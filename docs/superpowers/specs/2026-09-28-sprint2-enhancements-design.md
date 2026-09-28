# Sprint 2 Enhancements Design

## Goal and scope

Complete the documented Sprint 2 gaps across claim forwarding, image-based search, camera access audit, claim/publishing risk review, and credit certificates. Preserve the existing Vue 3, Spring Boot 3, MyBatis-Plus, MySQL, JWT, and Element Plus stack. Keep external credentials optional; the application must remain usable without a cloud AI account.

## Existing constraints

- `claim_apply` is the review and pickup workflow. Its `(item_id, claimer_id)` unique key prevents duplicate claims, and `answer` is required.
- `lost_notice/{id}/matches/v2` already produces ranked candidates. The notice owner is the only user allowed to view matches.
- Uploaded images are stored locally and represented by `/uploads/...` URLs. Seed data includes dead `example.com` URLs that must be skipped.
- `camera_log` already exists in `01_schema.sql` and is used by lost-item disputes. Its status meanings are 0 pending, 1 approved, 2 rejected, 3 completed. Preserve them.
- No push notification, OCR/NLP provider, cloud vision credentials, or PDF library is configured.
- The current work directory does not contain Git metadata; design/implementation commits cannot be created here.

## Decisions

### Claim invitations

Implement `POST /api/lost-notices/{id}/forward` as a claim application initiated from an owned, active lost notice. The request identifies an eligible public found item and includes the answer/details to its claim question. It calls the existing claim application service so deduplication, sensitive-content checks, lock rules, and status transitions remain authoritative. Add nullable `source_notice_id` to `claim_apply` to retain provenance. The existing item publisher's claim queue is the recipient inbox; no push transport is invented. The response and claim queue identify the originating notice.

### Image search and duplicate detection

Use a local 64-bit difference hash (dHash), requiring no external credentials or network. Add `item_image_fingerprint` with one row per found-item image, hash version, digest, source URL, and timestamp. Fingerprints are generated when found items are published. Legacy rows are backfilled lazily when searched or when they are encountered for duplicate review. Duplicate candidates use Hamming distance and remain review warnings; publication is not automatically blocked.

Add multipart `POST /api/lost-notices/search-by-image` accepting `image` and optional `category`; return up to ten public found items ordered by similarity, with the similarity score. Reject invalid/empty images and cap input size consistently with current upload limits. Search is local perceptual similarity, not semantic object recognition; label the UI accordingly.

### Claim confidence and new-account markers

Add an explainable local text heuristic for application confidence using answer informativeness and character n-gram overlap with the item's title/description. Persist the score and reason on `claim_apply`; it only adds a low-confidence review marker and never approves/rejects an application. Display the marker in the claim review queue.

Expose the publisher's account age/new-account flag on found-item view models using `user.created_at`; mark publishers registered less than seven days before publication. Use the same result in item detail/list and admin risk review. Do not add redundant stored state.

### Camera log management

Add SYS_ADMIN-protected paginated GET, POST, and PUT endpoints under `/api/admin/camera-logs`, matching the existing schema and statuses. Creation records the authenticated admin as applicant and validates the drop point, optional item, and time range. Processing records handler and handled time, requires a result note, and permits only pending requests to move to approved or rejected; dispute automation may continue to set completed. Add a Camera Logs tab to the admin console with filtering, application, and review actions.

### Credit certificate PDF

Implement authenticated `GET /api/credits/certificate` as a server-generated downloadable PDF containing the authenticated user's name/student number, current credit score, issue timestamp, and a unique certificate identifier. Use Apache PDFBox and bundled/available CJK font support where possible; if no suitable font is present, the endpoint must still return a valid PDF and report the font limitation without corrupting output. Replace the browser-print export with an API download while retaining the on-page preview.

## API and data changes

- `POST /api/lost-notices/{id}/forward`: `{ itemId, answer }`; creates an ordinary pending claim application with `source_notice_id`.
- `POST /api/lost-notices/search-by-image`: multipart `image`, optional `category`; returns ranked matches with similarity.
- `GET /api/admin/camera-logs`: paginated filters by status/drop point/item.
- `POST /api/admin/camera-logs`: create a pending application.
- `PUT /api/admin/camera-logs/{id}`: approve/reject with required result note.
- `GET /api/credits/certificate`: `application/pdf` attachment.
- Add migration `sql/05_sprint2_enhancements.sql` for fingerprint storage, claim provenance/confidence, and indexes. The existing camera_log table needs no schema change.

## Failure behavior and security

- Only the owner of an open notice (`status=0`, as implemented by `LostNoticeServiceImpl.close`) may forward it; only public items may receive a forwarded claim; regular claim ownership/deduplication rules still apply.
- Only SYS_ADMIN may query/manage camera logs. Validate identifiers and time ranges, and do not expose credentials or image file paths.
- Image decoding failures return a validation response; missing or unreadable legacy photos are skipped and logged. Never fetch arbitrary user-provided URLs from the server.
- Similarity and confidence scores are advisory. They do not mutate item/claim status.
- PDF responses use `Content-Disposition: attachment`, require a configured Unicode font, and never accept user-supplied identity fields. Missing/invalid fonts produce a clear service error instead of a corrupt certificate.

## Verification

- Unit tests: image hash equality/near-match/unrelated images; search ordering/threshold and unreadable inputs; forward ownership/status/deduplication; confidence classification without automatic rejection; camera-log validation/transitions/authorization; PDF content type/headers and user data isolation.
- Backend: `mvn test` and `mvn package`.
- Frontend: `npm run build` and focused manual checks of image upload search, claim provenance/confidence marker, admin camera tab, and certificate download.
- SQL: apply the migration against a schema created from `01_schema.sql` plus `03_sprint2_tables.sql`; verify idempotency or document one-time execution requirements.

## Out of scope

- Cloud semantic image recognition, push notifications, automatic rejection/approval, video retrieval/storage, and changes to existing credit rules.
