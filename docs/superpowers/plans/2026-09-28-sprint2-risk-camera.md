# Sprint 2 Risk and Camera Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add image-duplicate and new-account risk signals, and a secure camera-log management workflow for administrators.

**Architecture:** Reuse the dHash fingerprint service from the claim/discovery plan, derive new-account state from user creation and item publication timestamps, and expose camera-log operations through SYS_ADMIN-only endpoints and a new admin tab.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus, MySQL, JUnit 5/Mockito, Vue 3, Axios, Element Plus.

**Spec:** `docs/superpowers/specs/2026-09-28-sprint2-risk-camera-design.md`

## Global Constraints

- Fingerprint table/service are delivered by Task 1 of the claim/discovery plan; do not duplicate them.
- Duplicate signals are advisory, and publication is never blocked by a duplicate.
- New-account is derived from registration time less than seven days before publication.
- Preserve camera-log schema/statuses: 0 pending, 1 approved, 2 rejected, 3 completed; dispute automation writes 3.
- All `/api/admin/**` routes require `SYS_ADMIN`.

---

### Task 1: New-Account and Duplicate Risk Signals

**Files:**
- Modify: `backend/src/main/java/com/school/lostfound/vo/FoundItemVO.java`
- Modify: `backend/src/main/java/com/school/lostfound/service/impl/FoundItemServiceImpl.java`
- Modify: `backend/src/main/java/com/school/lostfound/service/RiskControlService.java`
- Modify: `backend/src/main/java/com/school/lostfound/service/impl/RiskControlServiceImpl.java`
- Modify: `backend/src/main/java/com/school/lostfound/controller/RiskControlController.java`
- Modify: `backend/src/main/java/com/school/lostfound/vo/RiskWarningVO.java`
- Test: `backend/src/test/java/com/school/lostfound/service/RiskControlServiceTest.java`

**Interfaces:**
- `FoundItemVO` adds `publisherNewAccount: boolean` and `publisherCreatedAt`.
- `RiskControlService.detectDuplicateImages()` returns `RiskWarningVO` entries with both item IDs, titles, and similarity.
- `GET /api/admin/risk-control/duplicates` exposes duplicate candidates.

- [ ] **Step 1: Write failing tests.** Cover exactly 6 days 23 hours (new), exactly 7 days (not new), duplicate hashes inside the configured Hamming threshold, and unrelated hashes not returned.
- [ ] **Step 2: Run `mvn -Dtest=RiskControlServiceTest test`.** Expect missing method/VO property failures.
- [ ] **Step 3: Populate publisher age on FoundItemVO.** Resolve the founder from UserMapper in existing list/detail conversion, compare `publishedAt` to `createdAt.plusDays(7)`, and test missing users as not marked.
- [ ] **Step 4: Detect duplicate fingerprints.** Compare distinct item IDs sharing hashes within distance threshold 8; group results so each pair is returned once; ignore non-public/voided items; include readable item titles and score in warning detail.
- [ ] **Step 5: Expose the duplicates route and combine it into warnings.** Keep current warning types and avoid logging image paths or user-private data.
- [ ] **Step 6: Run `mvn -Dtest=RiskControlServiceTest test`.** Expect boundary and duplicate tests to pass.

### Task 2: Camera-Log API

**Files:**
- Create: `backend/src/main/java/com/school/lostfound/dto/CameraLogCreateRequest.java`
- Create: `backend/src/main/java/com/school/lostfound/dto/CameraLogHandleRequest.java`
- Create: `backend/src/main/java/com/school/lostfound/vo/CameraLogVO.java`
- Create: `backend/src/main/java/com/school/lostfound/service/CameraLogService.java`
- Create: `backend/src/main/java/com/school/lostfound/service/impl/CameraLogServiceImpl.java`
- Create: `backend/src/main/java/com/school/lostfound/controller/CameraLogAdminController.java`
- Create: `backend/src/test/java/com/school/lostfound/service/CameraLogServiceTest.java`
- Modify: `backend/src/main/java/com/school/lostfound/config/SecurityConfig.java` only if existing `/api/admin/**` authorization does not cover the mappings.

**Interfaces:**
- `GET /api/admin/camera-logs?status=&dropPointId=&itemId=&page=&size=` returns `IPage<CameraLogVO>`.
- `POST /api/admin/camera-logs` body has `dropPointId`, optional `itemId`, `applyReason`, `timeRangeStart`, `timeRangeEnd`.
- `PUT /api/admin/camera-logs/{id}` body has `status` (1 or 2) and `resultNote`.

- [ ] **Step 1: Write service tests.** Verify invalid time order, inactive/non-camera point, missing item, status outside 1/2, blank result, repeated handling, authenticated applicant/handler IDs and timestamps.
- [ ] **Step 2: Run `mvn -Dtest=CameraLogServiceTest test`.** Expect compile failure for the absent service.
- [ ] **Step 3: Implement DTO validation and VO conversion.** Map names for applicant, handler, drop point, and item title without exposing camera credentials.
- [ ] **Step 4: Implement paginated filtering, create and handle.** Create status 0 with principal as applicant. Only pending rows can move to status 1 or 2; set handler principal, result note and `handledAt`.
- [ ] **Step 5: Add controller under `/api/admin/camera-logs`.** Rely on existing SYS_ADMIN matcher, annotate consistently with other admin controllers, and use standard `Result` wrappers.
- [ ] **Step 6: Run `mvn -Dtest=CameraLogServiceTest,DisputeServiceTest test`.** Verify camera management does not break dispute behavior that marks records status 3.

### Task 3: Admin UI and Item Risk Markers

**Files:**
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/views/AdminView.vue`
- Modify: `frontend/src/views/ItemListView.vue`
- Modify: `frontend/src/views/ItemDetailView.vue`

- [ ] **Step 1: Add typed camera API methods and VO types.** Include paginated list, create and handle methods with exact backend fields.
- [ ] **Step 2: Add a Camera Logs tab.** Provide status/drop point filters, paginated rows, a request dialog, and an approve/reject dialog requiring a result note. Disable actions for non-pending rows.
- [ ] **Step 3: Render item new-account marker and duplicate-risk warning.** Show the marker on list/detail only when server data says true; show risk detail in admin warnings without blocking user actions.
- [ ] **Step 4: Run `npm run build` in `frontend`.** Expect type-check and Vite build to pass.

## Plan Self-Review

- Spec coverage: duplicate hash warnings, 7-day account marker, camera management endpoints, status transitions, and admin UI are covered.
- No schema update is needed for camera logs; the schema already contains the table and current status use.
- Fingerprint persistence is owned by the claim/discovery plan to avoid competing migrations.
