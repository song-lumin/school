# Sprint 2 Risk and Camera Audit Design

This sub-project implements the risk/audit portions of the [umbrella design](2026-09-28-sprint2-enhancements-design.md).

## Scope

- Hash found-item images at publish time and expose duplicate-image candidates to SYS_ADMIN as review warnings.
- Mark found items published by an account less than seven days old, using account creation time and item publication time.
- Manage camera-log requests in the existing SYS_ADMIN console.

## Decisions and contract

- Store per-image dHash values in `item_image_fingerprint` with item id, image URL, algorithm version, digest, and creation time. Near-duplicates use a documented Hamming-distance threshold. Duplicate warnings are advisory and never block publication.
- Backfill hashes lazily for existing local upload URLs; ignore unreadable or external seed-example URLs. Never perform server-side requests to arbitrary remote image URLs.
- New-account state is derived as `publishedAt < user.createdAt + 7 days`; no duplicate database flag is stored. Return the boolean and registration date on item VOs used by list/detail and admin views.
- Keep current `camera_log` table and status meanings: 0 pending, 1 approved, 2 rejected, 3 completed. Existing dispute logic continues to write status 3.
- `GET /api/admin/camera-logs` returns paginated/filterable results; `POST` creates status 0 with the authenticated SYS_ADMIN as applicant; `PUT /{id}` permits pending-to-approved/rejected transitions with result note and records authenticated handler/timestamp. Completed records are read-only through this management API.
- Add fingerprint duplicate scan results to risk-control warnings. Camera management includes status/drop point filters, application form, and handling form in a dedicated admin tab.

## Tests

Test dHash stability and image decoding; fingerprint publish and legacy backfill; duplicate threshold, warning fields, and non-blocking behavior; account-age boundary at seven days; camera filters, creation fields, transition validation, result-note requirement, and non-admin authorization.

## Non-goals

No video playback/download, AI image classification, automatic duplicate rejection, or changes to dispute-driven completion.
