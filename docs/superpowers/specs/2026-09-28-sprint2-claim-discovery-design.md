# Sprint 2 Claim and Discovery Design

This sub-project implements the claim/discovery portions of the [umbrella design](2026-09-28-sprint2-enhancements-design.md).

## Scope

- The owned active lost notice owner can forward a claim to a public found item with an answer to its verification question.
- The forward endpoint reuses the normal claim service and queue, retains the source notice on the claim, and does not create a parallel review state or push system.
- Found item claims receive an explainable advisory confidence score based on answer informativeness and character n-gram overlap with item text. Low confidence is visible to the item publisher and does not change application status.
- A multipart image-search endpoint ranks public found items by local dHash similarity, optionally filtered by category.
- Frontend lost-notice detail supports image search, match list forwarding, answer entry, and source provenance in the existing claim review interface.

## Contract

- `POST /api/lost-notices/{noticeId}/forward`, authenticated body `{ itemId: number, answer: string }`.
- Forwarding requires current user ownership of an open notice (`status=0`) and a public item not owned by that user. Existing one-claim-per-item constraint, lock, sensitive content, and pending status rules remain in effect.
- `POST /api/lost-notices/search-by-image`, authenticated multipart fields `image` and optional `category`; responds with up to ten `{ item, similarity }` results.
- Similarity is normalized 0-100 from 64-bit dHash Hamming distance. It is visual similarity only, not object/semantic recognition.

## Data and implementation

- Add nullable `source_notice_id`, confidence score, low-confidence flag, and reason to `claim_apply`; keep its current unique key and required answer.
- Add one fingerprint row for each image on each found item. The shared image-fingerprint service computes/stores hashes during found-item publish and backfills legacy images on demand.
- The browser uses existing upload validation and displays errors from the API. Search results include similarity, item status and navigation to detail.
- Tests cover permission checks, state rules, duplicate application behavior, confidence advisory-only behavior, hash ranking, and unreadable image failures.

## Non-goals

No cloud vision SDK/credential, semantic image classification, push notification, or automatic claim decision.
