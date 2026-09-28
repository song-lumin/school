# Sprint 2 Credit Certificate Design

This sub-project implements the certificate portion of the [umbrella design](2026-09-28-sprint2-enhancements-design.md).

## Contract

- Authenticated `GET /api/credits/certificate` returns an `application/pdf` attachment for only the authenticated user.
- The document includes the real name, student/staff number, current credit score, issue timestamp, issuing system, and a unique certificate identifier. No identity/score values are accepted from the request.
- The frontend retains the current preview and replaces browser print with direct download from the backend.

## Rendering and compatibility

- Use Apache PDFBox 3.0.8 (official current 3.0.x release) with the Java 17 backend.
- Embed a configured Unicode font through `CERTIFICATE_FONT_PATH` / `certificate.font-path`. If the configured font is absent or invalid, return a clear server error rather than produce a PDF with corrupted identity text.
- Set attachment headers and a deterministic safe filename; keep document generation in a focused certificate service.

## Tests

Test PDF signature/parseability, required fields from the authenticated user's record, content-disposition and MIME type, missing user handling, and that one user's identity cannot be requested by changing query parameters. Render the resulting PDF and inspect layout with the PDF skill workflow.

## Reference

Apache's official download page lists PDFBox 3.0.8 as the current 3.0.x release: https://pdfbox.apache.org/download. The PDFBox 3.0 getting-started guide documents the Maven dependency and PDF creation support: https://pdfbox.apache.org/3.0/getting-started.html.
