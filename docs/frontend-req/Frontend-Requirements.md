Here is a dedicated **external dependencies checklist** – everything required *outside* your Flutter/Jaspr monorepo. It covers backend requirements, cryptographic keys, and alternative deployment services if Firebase isn’t an option.

---

## 1. Backend Project Requirements (Spring Boot)

Your backend must provide the following **fully functional** endpoints and protocols *before* feature development starts. Provide the development and production URLs.

| # | Requirement | Specification / Contract | Status |
|---|-------------|---------------------------|--------|
| **1.1** | **Base URLs** | Local: `http://localhost:8080/api` and `ws://localhost:8080/api/ws`. Production URLs pending deployment (see deployment guide). Context path `/api` is part of every URL. | ⏳ prod pending |
| **1.2** | **REST Endpoints** | Authoritative source: [`../../contracts/api/openapi.yaml`](../../contracts/api/openapi.yaml). Summary:<ul><li>`POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `GET /auth/me`.</li><li>`GET /products`, `GET /products/{id}` (pagination, search, category).</li><li>`GET /cart`, `POST /cart`, `PUT /cart/{itemId}`, `DELETE /cart/{itemId}`.</li><li>`POST /orders`, `GET /orders` (history; admin may filter by customer/status), `GET /orders/{id}`, `GET /orders/status/{orderId}`.</li><li>`POST /orders/{id}/payment`, `POST /orders/{id}/cancel`.</li><li>`POST /orders/{id}/ship`, `POST /orders/{id}/deliver` (WAREHOUSE/ADMIN).</li><li>`GET /fulfillment/orders?status=` (WAREHOUSE/ADMIN fulfillment queue).</li><li>`POST /inventory/products`, `PUT /inventory/products/{id}` (ADMIN catalog management).</li><li>`GET /admin/audit/products/{id}`, `GET /admin/audit/orders/{id}` (ADMIN revision history).</li><li>`GET /inventory/reserved` (WAREHOUSE/ADMIN stock-vs-reserved report).</li></ul> | ✅ ready |
| **1.3** | **WebSocket (STOMP)** | Endpoint: `/api/ws` (SockJS). JWT required as raw `Authorization: Bearer` header on the STOMP CONNECT frame. Destinations: `/user/queue/orders` (private per user), `/topic/orders/{orderId}` (broadcast). Payloads: `{orderId, status}` plus `RESERVATION_EXPIRED` and `PAYMENT_FAILED {reason}` events. Full contract: [`asyncapi-ws.md`](../../contracts/ws/asyncapi-ws.md). | ✅ ready |
| **1.4** | **JWT Specification** | Algorithm: HS256 default (RS256 optional via `jwt.use-rs256`). Header format: `Authorization: Bearer <token>`. Access-token payload contains `userId`, `email`, and `ROLE_`-prefixed `roles`. Refresh: exchange refresh token at `POST /auth/refresh` for a new pair. Access token lifetime: 900s by default. | ✅ ready |
| **1.5** | **Error Response Format** | Every non-2xx returns `{timestamp, status, error, message, path}` (`ApiError` schema in the OpenAPI document). | ✅ ready |
| **1.6** | **CORS Configuration** | Controlled by `cors.allowed-origins` (comma-separated). Add the dashboard origin when deploying. | ✅ ready |
| **1.7** | **SSL/TLS Certificate** | Requires a deployed server behind TLS (Caddy/nginx). Pinning hashes extracted after the certificate exists. | ⏳ prod pending |
| **1.8** | **OpenAPI Spec** | Committed at [`../../contracts/api/openapi.yaml`](../../contracts/api/openapi.yaml) — regenerate with `mvn test -Dtest=OpenApiContractExportTest`. Interactive docs at `/api/swagger-ui.html` (includes Bearer authorize). Generate typed clients from this file. | ✅ ready |

---

## 2. Keys, Secrets & Certificates to Prepare

These must be generated/obtained and safely stored (e.g., GitHub Secrets, `.env` files, or secure vault).

| # | Item | Purpose | How to obtain / format |
|---|------|---------|------------------------|
| **2.1** | **SSL Public Key Hash (Primary)** | Certificate pinning in production Dio client. | Run `openssl x509 -in server.crt -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | base64` on your server certificate. |
| **2.2** | **SSL Public Key Hash (Backup)** | Fallback pin for certificate rotation. | Same as above, for the backup/intermediate certificate or future certificate. |
| **2.3** | **JWT Signing Public Key** *(if RS256)* | Allows the client to decode and optionally validate `exp` claim locally (reduces API calls). | Provided by backend in PEM format. (Not mandatory, but helpful.) |
| **2.4** | **API Base URL** | Used in `dart-define` and `.env` files. | E.g., `https://api.prod.example.com` |
| **2.5** | **WebSocket Base URL** | Used in `dart-define` and `.env` files. | E.g., `wss://api.prod.example.com/ws` |
| **2.6** | **Sentry DSN** *(optional)* | For error tracking (if used instead of Firebase Crashlytics). | Get from Sentry.io project settings. |
| **2.7** | **Google Play / App Store Credentials** (later) | For final production release. | Not required for initial infrastructure, but keep in mind. |

---

## 3. Third-Party Services & Deployment (Firebase Alternatives)

Since you mentioned you **do not have an available free Firebase project** for deployment, here is a checklist of alternatives for Web Hosting and Mobile App Distribution.

### 3.1 Web Hosting Alternatives (for Jaspr static build)
The `web` package builds to static HTML, CSS, and JS – any static host works.

| Service | Free Tier | Setup Notes |
|---------|-----------|-------------|
| **Cloudflare Pages** | Unlimited personal projects, great bandwidth. | Connect your GitHub repo; build command: `melos run deploy:web` (outputs to `web/dist`). |
| **Vercel** | Generous hobby tier (100GB bandwidth). | Configure `vercel.json` to point to the build output directory. |
| **Netlify** | 100GB/month bandwidth. | Easy drag-and-drop or Git integration. |
| **Serve via Spring Boot** | Free (no extra cost). | Put the built `web/dist` folder inside `src/main/resources/static/dashboard` and serve it from the same backend (just add CORS for API). |
| **AWS S3 + CloudFront** | Pay-as-you-go (very cheap). | Requires AWS account, but no free project limit. |

**Action**: Pick one and register a domain/subdomain (e.g., `dashboard.example.com`). Set up the deployment pipeline (Melos script) to push there.

### 3.2 Mobile App Distribution Alternatives (for APK/IPA)
Instead of Firebase App Distribution for internal testing:

| Service | Free Tier | Setup Notes |
|---------|-----------|-------------|
| **Microsoft App Center** | Free unlimited private testers. | Supports Android and iOS. Upload APK/IPA via CLI (`appcenter distribute`). |
| **Google Play Internal / Closed Track** | Free (Play Console account one-time $25). | Upload APK directly to the internal test track and share the opt-in link. |
| **GitHub Releases** | Free (public repos) or private (with Pro). | Attach the built APK to a draft release; share the download URL. |
| **AWS S3 (pre-signed URLs)** | Pay-as-you-go. | Upload APK to S3, generate a temporary download link for testers. |

**Action**: Choose one, create an account/organisation, and set up the deployment script (e.g., `melos deploy:app` using App Center CLI or `gh release upload`).

### 3.3 Crash Reporting / Analytics (instead of Firebase Crashlytics)

| Service | Alternative |
|---------|-------------|
| **Sentry** | Free tier for 5k errors/month. Works with Flutter and Dart. |
| **BugSnag** | Free tier for 7,500 events/month. |
| **Datadog RUM** | Paid but has trial. |
| **Mixpanel / PostHog** | For analytics (if needed). |

---

## 4. Development Environment (Local Machine Setup)

Outside the project code, ensure each developer’s machine has the following installed:

| # | Tool | Minimum Version | Check command |
|---|------|-----------------|---------------|
| **4.1** | **Flutter SDK** | 3.22+ (stable) | `flutter --version` |
| **4.2** | **Dart SDK** | 3.4+ (bundled with Flutter) | `dart --version` |
| **4.3** | **Node.js & npm** | 18+ | `node --version` |
| **4.4** | **Melos** (global) | 6.0+ | `dart pub global activate melos` |
| **4.5** | **Git** | 2.40+ | `git --version` |
| **4.6** | **Tailwind CSS CLI** (used by Jaspr) | latest | Installed via npm (`tailwindcss`) |
| **4.7** | **Java / JDK** (only if running Spring Boot locally) | 17+ | `java --version` |
| **4.8** | **Android Studio / Xcode** | As required for Flutter builds | Only needed for local Flutter runs on emulators. |

---

## 5. Pre-Feature Development External Readiness Checklist

Before writing a single line of feature code (login, product list), confirm these are **done**:

**Backend readiness — ✅ COMPLETE (nothing left to derive):**
- [x] All endpoints implemented, contracted in `openapi.yaml`, and covered by integration tests.
- [x] WebSocket STOMP handshake with a real token proven by `WebSocketIntegrationTest` (and the manual Node script in README).
- [x] Error format, JWT claims, CORS property, and registration flow verified.

**Environment / deployment — ⏳ remaining before production use:**
- [ ] Backend deployed to a server with TLS; production `API_BASE_URL` / `WS_BASE_URL` obtained.
- [ ] SSL certificate hashes extracted and stored as environment secrets (requires the deployed cert).
- [ ] `cors.allowed-origins` on the server set to the dashboard origin.
- [ ] Chosen hosting/distribution service configured with API keys stored in GitHub Secrets.

**Frontend-side setup tasks:**
- [ ] Secure token storage (`flutter_secure_storage` mobile / web storage) tested with a dummy token.
- [ ] Access to backend logs for debugging during development.

---

## Summary of External Providers to Sign Up For (if not already)

| Provider | Purpose | Link |
|----------|---------|------|
| **Cloudflare Pages** (or Vercel/Netlify) | Web hosting | cloudflare.com |
| **Microsoft App Center** | Mobile APK distribution | appcenter.ms |
| **Sentry** (optional) | Error tracking | sentry.io |
| **GitHub** | Code repository + CI/CD | github.com |
| **PostHog / Mixpanel** (optional) | Analytics | posthog.com |

---

This checklist covers everything *outside* your codebase. Once these are satisfied, the infrastructure implementation plan (Epic 1) can proceed without blockers.
