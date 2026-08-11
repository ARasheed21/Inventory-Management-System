Here is a dedicated **external dependencies checklist** – everything required *outside* your Flutter/Jaspr monorepo. It covers backend requirements, cryptographic keys, and alternative deployment services if Firebase isn’t an option.

---

## 1. Backend Project Requirements (Spring Boot)

Your backend must provide the following **fully functional** endpoints and protocols *before* feature development starts. Provide the development and production URLs.

| # | Requirement | Specification / Contract |
|---|-------------|---------------------------|
| **1.1** | **Base URLs** | Provide `API_BASE_URL` (e.g., `https://api.dev.example.com`) and `WS_BASE_URL` (e.g., `wss://api.dev.example.com/ws`). |
| **1.2** | **REST Endpoints** | <ul><li>`POST /auth/login` – returns JWT + refresh token.</li><li>`POST /auth/refresh` – refreshes expired JWT.</li><li>`GET /auth/me` – returns current user profile.</li><li>`GET /products` – pagination, search, category filters.</li><li>`GET /products/{id}` – product details.</li><li>`GET /cart`, `POST /cart`, `PUT /cart/{id}`, `DELETE /cart/{id}`.</li><li>`POST /orders`, `GET /orders` (history), `GET /orders/{id}`.</li><li>`GET /orders/status/{orderId}` (optional, but WebSocket preferred).</li><li>`PUT /admin/inventory` (web only).</li></ul> |
| **1.3** | **WebSocket (STOMP)** | <ul><li>Endpoint: `/ws/orders` (or your chosen path).</li><li>Requires JWT in the STOMP `CONNECT` headers.</li><li>Destinations: `/user/queue/orders` (customer), `/topic/orders` (admin).</li><li>Message format: JSON with `orderId` and `status`.</li></ul> |
| **1.4** | **JWT Specification** | <ul><li>Algorithm: RS256 (preferred) or HS256. If RS256, provide the **public key** (for client-side expiry validation, though optional).</li><li>Header format: `Authorization: Bearer <token>`.</li><li>Token payload must contain `userId`, `email`, and `roles` (e.g., `ROLE_ADMIN`).</li><li>Refresh token mechanism: either a long-lived JWT or a separate opaque token via `/auth/refresh`.</li></ul> |
| **1.5** | **Error Response Format** | Standardised JSON structure so Dio interceptors can parse failures, e.g.: <br> `{ "timestamp": "...", "status": 401, "message": "Token expired", "path": "/cart" }` |
| **1.6** | **CORS Configuration** | Must allow the web dashboard origin (e.g., `https://dashboard.dev.example.com`). Allow credentials (`true`), methods: GET, POST, PUT, DELETE, OPTIONS. |
| **1.7** | **SSL/TLS Certificate** | Provide the **public key SHA-256 hashes** for certificate pinning (see Section 2). Must have valid certificates in production (Let’s Encrypt or paid). |
| **1.8** | **Postman / OpenAPI Spec** | Provide an OpenAPI (Swagger) spec or Postman collection for the mobile/web developers to test against. |

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

- [ ] Backend is running (dev environment) and Swagger/Postman tests pass for all endpoints.
- [ ] WebSocket server is accessible and STOMP handshake works with a test token.
- [ ] SSL certificate hashes are extracted and stored as environment secrets.
- [ ] `API_BASE_URL` and `WS_BASE_URL` are provided and accessible from the mobile emulator (e.g., `10.0.2.2` for Android) and web browser (CORS handled).
- [ ] Chosen deployment service (Cloudflare, Vercel, App Center, etc.) is configured with API keys stored in GitHub Secrets.
- [ ] `flutter_secure_storage` and web `localStorage`/cookies are tested with a dummy token.
- [ ] The team has access to the backend logs for debugging (e.g., ELK, Datadog, or plain log files).
- [ ] If using a custom domain, DNS records are pointed to the hosting provider.

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