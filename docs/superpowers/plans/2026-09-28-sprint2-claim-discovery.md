# Sprint 2 Claim and Discovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add traceable claim forwarding, advisory claim confidence, and local perceptual image search to lost-notice and claim workflows.

**Architecture:** Reuse the existing `ClaimService.apply` checks and publisher review queue; store the source notice and advisory confidence on `claim_apply`. A shared local dHash service stores image fingerprints for found-item photos and ranks image-search candidates without cloud credentials.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus, MySQL, JUnit 5/Mockito, Vue 3, Axios, Element Plus.

**Spec:** `docs/superpowers/specs/2026-09-28-sprint2-claim-discovery-design.md`

## Global Constraints

- Forwarding is only allowed to the owner's open notice (`status=0`) and public found items.
- Keep the `(item_id, claimer_id)` unique key and existing sensitive-content, lock, and duplicate-claim behavior.
- Low-confidence and visual-similarity scores are advisory only.
- Image processing is local dHash; do not fetch arbitrary URLs or require an external vision credential.
- Existing uploaded file URLs use `/uploads/...`; skip external seed-example URLs.

---

### Task 1: Local Image Fingerprinting

**Files:**
- Create: `backend/src/main/java/com/school/lostfound/service/ImageFingerprintService.java`
- Create: `backend/src/main/java/com/school/lostfound/service/impl/ImageFingerprintServiceImpl.java`
- Create: `backend/src/main/java/com/school/lostfound/entity/ItemImageFingerprint.java`
- Create: `backend/src/main/java/com/school/lostfound/mapper/ItemImageFingerprintMapper.java`
- Create: `backend/src/test/java/com/school/lostfound/service/ImageFingerprintServiceTest.java`
- Create: `sql/05_sprint2_enhancements.sql`

**Interfaces:**
- Produce `String hash(BufferedImage image)`, `String hash(MultipartFile image)`, `int distance(String left, String right)`, and `void indexItemImages(FoundItem item)`.
- Persist one fingerprint per `(item_id, image_url)` with 16-character hex dHash and algorithm version `dhash-64-v1`.

- [ ] **Step 1: Add a failing dHash test.** Build small BufferedImages directly in the test; assert identical pixels hash identically, a one-pixel brightness change stays within Hamming distance 4, and a solid black image differs from solid white by 64 bits.
- [ ] **Step 2: Run `mvn -Dtest=ImageFingerprintServiceTest test` in `backend`.** Expect the test not to compile because the service does not exist.
- [ ] **Step 3: Implement 9x8 grayscale difference hashing.** Compare adjacent luminance values row-wise, encode 64 bits as fixed-width hex, and compute Hamming distance with `Long.bitCount(Long.parseUnsignedLong(hash, 16) ^ ...)`.
- [ ] **Step 4: Add persistence and URL resolution.** Resolve only `/uploads/{filename}` under configured `file.upload.path`; normalize the path and reject paths escaping that directory. Ignore non-local and unreadable existing image URLs with a warning log.
- [ ] **Step 5: Add schema migration.** Create `item_image_fingerprint` with FK to `found_item`, `image_url`, `hash_value CHAR(16)`, `algorithm_version VARCHAR(32)`, timestamps, unique `(item_id, image_url)`, and indexes on item and hash prefix. Add claim provenance/confidence columns in the same migration as specified in the claim plan.
- [ ] **Step 6: Run `mvn -Dtest=ImageFingerprintServiceTest test`.** Expect all three hash behavior assertions to pass.

### Task 2: Claim Confidence and Provenance

**Files:**
- Create: `backend/src/main/java/com/school/lostfound/service/ClaimConfidenceAnalyzer.java`
- Create: `backend/src/main/java/com/school/lostfound/dto/ForwardClaimRequest.java`
- Modify: `backend/src/main/java/com/school/lostfound/entity/ClaimApply.java`
- Modify: `backend/src/main/java/com/school/lostfound/vo/ClaimApplyVO.java`
- Modify: `backend/src/main/java/com/school/lostfound/service/ClaimService.java`
- Modify: `backend/src/main/java/com/school/lostfound/service/impl/ClaimServiceImpl.java`
- Modify: `backend/src/main/java/com/school/lostfound/service/LostNoticeService.java`
- Modify: `backend/src/main/java/com/school/lostfound/service/impl/LostNoticeServiceImpl.java`
- Modify: `backend/src/main/java/com/school/lostfound/controller/LostNoticeController.java`
- Test: `backend/src/test/java/com/school/lostfound/service/ClaimConfidenceAnalyzerTest.java`
- Test: `backend/src/test/java/com/school/lostfound/service/ClaimServiceTest.java`
- Test: `backend/src/test/java/com/school/lostfound/service/LostNoticeServiceTest.java`

**Interfaces:**
- `ClaimConfidenceAnalyzer.analyze(String answer, String itemText)` returns `{score: int, lowConfidence: boolean, reason: String}`.
- `ClaimService.applyForward(ForwardClaimRequest request, Long noticeId, Long currentUserId)` returns `ClaimApplyVO`.
- `LostNoticeService.forward(Long noticeId, ForwardClaimRequest request, Long currentUserId)` validates notice ownership/state then delegates to the shared claim service.
- `ClaimApplyVO` adds `sourceNoticeId`, `confidenceScore`, `lowConfidence`, `confidenceReason`.

- [ ] **Step 1: Write analyzer tests.** Assert a generic one-character answer is low confidence, an informative answer sharing 2-grams with item description scores higher, and empty inputs return a bounded low score with a reason.
- [ ] **Step 2: Run `mvn -Dtest=ClaimConfidenceAnalyzerTest test`.** Expect compile failure because the analyzer is absent.
- [ ] **Step 3: Implement deterministic Unicode-aware bigram overlap and informativeness scoring.** Normalize whitespace/case; return a 0-100 integer and concise Chinese reason; do not use randomness, remote APIs, or auto decisions.
- [ ] **Step 4: Extend ClaimApply and ClaimApplyVO mapping.** Add nullable `sourceNoticeId`, integer `confidenceScore`, boolean `lowConfidence`, and `confidenceReason`; include values in every VO conversion path.
- [ ] **Step 5: Refactor ClaimService.apply into one shared private creation path.** The regular path passes no notice ID; the forward path validates notice ownership/open state, validates item visibility and non-self claim, then uses all existing rate/lock/sensitive/duplicate checks. Set low-confidence metadata but always keep valid submissions pending.
- [ ] **Step 6: Add service tests.** Verify non-owner/closed notice/public-item failures do not insert; a valid forward inserts a pending claim with source notice ID; low confidence never changes pending status; duplicate claims still return 409.
- [ ] **Step 7: Run `mvn -Dtest=ClaimConfidenceAnalyzerTest,ClaimServiceTest,LostNoticeServiceTest test`.** Expect the new and existing claim tests to pass.

### Task 3: Image Search API and Found-Item Indexing

**Files:**
- Modify: `backend/src/main/java/com/school/lostfound/controller/LostNoticeController.java`
- Modify: `backend/src/main/java/com/school/lostfound/service/FoundItemService.java`
- Modify: `backend/src/main/java/com/school/lostfound/service/impl/FoundItemServiceImpl.java`
- Create: `backend/src/main/java/com/school/lostfound/vo/ImageSearchResultVO.java`
- Create: `backend/src/test/java/com/school/lostfound/service/ImageSearchServiceTest.java`
- Create: `backend/src/test/java/com/school/lostfound/service/FoundItemImageIndexTest.java`

**Interfaces:**
- Add `FoundItemService.searchByImage(MultipartFile image, String category)` returning at most ten `ImageSearchResultVO(item, similarity)` results.
- Replace the `Map<String,String>` stub with multipart fields `image` and optional `category`.

- [ ] **Step 1: Write ranking tests.** With a mock fingerprint repository and generated test images, assert lower Hamming distance ranks first, category excludes other categories, only public items are returned, result count is at most ten, and invalid image decoding raises a 400 `BusinessException`.
- [ ] **Step 2: Run `mvn -Dtest=ImageSearchServiceTest test`.** Expect compile failure because the ranked result type/search method is missing.
- [ ] **Step 3: Implement candidate lookup and lazy fingerprinting.** Load bounded public candidates, fingerprint only their local image URLs when no row exists, compare dHash, calculate `round((64-distance)/64*100)`, exclude scores below 30, sort descending, and limit ten.
- [ ] **Step 4: Add indexing after item persistence.** Invoke `indexItemImages` after found-item insert; image hash failure must not undo a valid publication, but must be logged for admin rescan.
- [ ] **Step 5: Expose authenticated multipart controller mapping.** Validate one image, enforce 10MB, accept optional category, and return the standard `Result` envelope.
- [ ] **Step 6: Add an indexing test.** Verify a valid publish indexes each local photo once and image decode/storage errors do not roll back the found-item insert.
- [ ] **Step 7: Run `mvn -Dtest=ImageSearchServiceTest,FoundItemImageIndexTest test`.** Expect deterministic score ordering and current publication behavior to remain intact.

### Task 4: Frontend Search, Forwarding, and Review Indicators

**Files:**
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/views/LostNoticeDetailView.vue`
- Modify: `frontend/src/views/ItemDetailView.vue`
- Modify: `frontend/src/views/ClaimListView.vue`

**Interfaces:**
- Add typed API methods `lostNoticeApi.searchByImage(file, category?)` and `lostNoticeApi.forward(id, {itemId, answer})`.
- Add similarity and claim provenance/confidence properties to frontend response types.

- [ ] **Step 1: Add frontend API and type contracts.** Use `FormData` for multipart search; do not set a JSON content-type header for the browser-generated boundary.
- [ ] **Step 2: Add image-search upload UI to the notice list.** Accept one JPEG/PNG/GIF image, optional category, show loading/errors, and render results with similarity percentage and a detail link.
- [ ] **Step 3: Add forward action to notice details.** Show it only for the notice owner and open status; require a public item and answer; submit, show success, then reload match/search state.
- [ ] **Step 4: Show source notice and confidence advisory in item claim queue.** Link to the originating notice; low-confidence label includes server reason and explicitly remains an advisory.
- [ ] **Step 5: Run `npm run build` in `frontend`.** Expect Vue type-check and Vite production build to pass.

## Plan Self-Review

- Spec coverage: forwarding, provenance, existing claim checks, confidence, local visual ranking, legacy fingerprinting, and frontend workflows are all assigned above.
- No external provider key or remote image fetch is introduced.
- This plan shares the migration file with the risk/camera plan; implementation must edit the same migration without duplicating table definitions.
