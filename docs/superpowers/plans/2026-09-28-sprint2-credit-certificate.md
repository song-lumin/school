# Sprint 2 Credit Certificate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate and download a server-issued PDF certificate for the authenticated user's current integrity score.

**Architecture:** A focused certificate service reads the current user from the authenticated principal, uses Apache PDFBox to create a single-page PDF with an embedded configured Unicode font, and returns an attachment. The frontend keeps its preview but downloads server output.

**Tech Stack:** Java 17, Spring Boot 3.2, Apache PDFBox 3.0.8, Vue 3, Axios, Element Plus.

**Spec:** `docs/superpowers/specs/2026-09-28-sprint2-credit-certificate-design.md`

## Global Constraints

- The document identity and score must come from the authenticated user's persisted record, never query/body values.
- The PDF embeds a configured Unicode TTF/OTF font; absent/invalid font returns a clear service error.
- Use Apache PDFBox 3.0.8; official docs identify this as current 3.0.x and document PDF creation support.
- Response content type is `application/pdf` and filename is a sanitized attachment name.

---

### Task 1: Server PDF Generator

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/school/lostfound/service/CreditCertificateService.java`
- Create: `backend/src/main/java/com/school/lostfound/service/impl/CreditCertificateServiceImpl.java`
- Create: `backend/src/main/java/com/school/lostfound/vo/CreditCertificateData.java`
- Test: `backend/src/test/java/com/school/lostfound/service/CreditCertificateServiceTest.java`

**Interfaces:**
- `byte[] generate(User user, Instant issuedAt, String certificateId)` creates one A4 PDF page.
- Config key `certificate.font-path`, sourced from `CERTIFICATE_FONT_PATH`, identifies an embeddable Unicode font.

- [ ] **Step 1: Add the PDFBox 3.0.8 dependency and config property.** Keep the font path blank by default and validate on request so non-certificate app flows start normally.
- [ ] **Step 2: Write failing tests.** With a valid TTF fixture, assert output starts `%PDF-`, PDFBox reopens it, and extracted text contains name, student ID, score, issue date, and certificate ID. Assert absent/invalid font produces a controlled business error.
- [ ] **Step 3: Run `mvn -Dtest=CreditCertificateServiceTest test`.** Expect compile failure because the service is absent.
- [ ] **Step 4: Implement one-page certificate rendering.** Use embedded `PDType0Font`; write every line with one content stream and wrap text to page bounds. Close document/resources with try-with-resources.
- [ ] **Step 5: Run `mvn -Dtest=CreditCertificateServiceTest test`.** Expect parseability, Unicode extraction, and failure-path assertions to pass.

### Task 2: Authenticated Download API

**Files:**
- Modify: `backend/src/main/java/com/school/lostfound/controller/CreditController.java`
- Modify: `backend/src/main/java/com/school/lostfound/service/CreditQueryService.java` only if current-user retrieval cannot be safely shared.
- Test: `backend/src/test/java/com/school/lostfound/controller/CreditCertificateControllerTest.java`

- [ ] **Step 1: Write controller tests.** Assert `application/pdf`, attachment disposition, PDF bytes, 404 for missing current user, and that no requested user ID is accepted.
- [ ] **Step 2: Run `mvn -Dtest=CreditCertificateControllerTest test`.** Expect route/service failures before implementation.
- [ ] **Step 3: Add `GET /api/credits/certificate`.** Read principal ID, resolve user from mapper/service, generate timestamp and UUID certificate number, return `ResponseEntity<byte[]>` with safe filename and no-cache headers.
- [ ] **Step 4: Run `mvn -Dtest=CreditCertificateControllerTest test`.** Expect endpoint contract tests to pass.

### Task 3: Frontend Download and PDF QA

**Files:**
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/CertificateView.vue`

- [ ] **Step 1: Add blob download API.** Request `/credits/certificate` with `responseType: 'blob'`.
- [ ] **Step 2: Replace `window.print()` flow.** Create a temporary object URL and anchor download, then revoke the URL; keep the existing certificate preview and loading/error feedback.
- [ ] **Step 3: Run `npm run build` in `frontend`.** Expect Vue type-check and Vite build to pass.
- [ ] **Step 4: Run backend `mvn -Dtest=CreditCertificateServiceTest,CreditCertificateControllerTest test`, generate a sample PDF with the API/service fixture, render it to PNG using the PDF skill tools, and inspect that Chinese identity text, score, and certificate number fit without clipping.**

## Plan Self-Review

- Spec coverage: authenticated identity, current score, issue time, unique identifier, MIME/disposition, Unicode font failure path, and frontend direct download are covered.
- PDFBox version is verified from the official Apache release page and dependency guide.
- Visual PDF QA is required; byte-signature validation alone is not treated as layout verification.
