## Frontend preparation plan

### 1. Current repo assessment

From Frontend-Requirements.md and the repository:

- This is currently a Spring Boot backend project.
- Existing API endpoints:
  - `GET /api/products`
  - `GET /api/inventory`
  - `POST /api/inventory/products`
  - `PUT /api/inventory/products/{id}`
  - `POST /api/orders`
  - `GET /api/orders`
  - `GET /api/orders/{id}`
  - `POST /api/orders/{id}/payment`
  - `POST /api/orders/{id}/cancel`
- Security currently uses basic auth via SecurityConfig.java
- OpenAPI/Swagger is present and exposed via `/swagger-ui.html` and `/v3/api-docs`

### 2. Gap analysis for frontend requirements

The backend does not yet satisfy several frontend contract expectations:

- Missing frontend auth contract:
  - `/auth/login`
  - `/auth/refresh`
  - `/auth/me`
- No JWT-based auth implementation
- No cart endpoints:
  - `GET /cart`
  - `POST /cart`
  - `PUT /cart/{id}`
  - `DELETE /cart/{id}`
- No websocket/STOMP support for order updates
- No `/orders/status/{orderId}`
- No explicit CORS configuration for web dashboard origin
- No certificate pinning data from production TLS cert
- Current security is HTTP Basic, not bearer token based

### 3. Preparation plan

#### Phase 1: Backend readiness before frontend work

1. Confirm backend contract and version of API base path (completed)
   - Frontend should use `/api` prefix where controllers expose it. API base: `API_BASE_URL=/api`. WS base: `WS_BASE_URL=/ws`.
   - Example env file: `.env.example` at the project root (contains `API_BASE_URL` and `WS_BASE_URL`).

2. Implement required auth support (completed)
   - Endpoints added: `POST /auth/login`, `POST /auth/refresh`, `GET /auth/me` (`src/main/java/com/example/inventory/web/controllers/AuthController.java`).
   - Security switched to JWT bearer tokens; `JwtService` includes `userId`, `email`, and `roles` in tokens (`src/main/java/com/example/inventory/infrastructure/security/JwtService.java`).
   - Current implementation uses HS256 (shared secret). RS256 is recommended for production; see notes below if you want RS256 support.

3. Add missing cart endpoints (completed)
   - Cart CRUD implemented under `/api/cart` (`CartController`, `CartHandler`, `JpaCartRepository`).

4. Add real-time order status support (completed)
   - `GET /orders/status/{orderId}` exists (`OrderController`).
   - STOMP/WebSocket `/ws` endpoint added with JWT handshake support and a simple in-memory broker. Test publish endpoint: `POST /api/orders/{id}/notify-test`.
   - STOMP JWT support accepts `Authorization` header, `sec-websocket-protocol` subprotocol, or handshake attribute; see `StompJwtChannelInterceptor` and `WebSocketHandshakeInterceptor`.

5. Add CORS configuration (completed)
   - CORS configured in `SecurityConfig` via `cors.allowed-origins` property; see `application.yml` and `SecurityConfig`.

6. Add standard error response schema (completed)
   - Centralized `ApiExceptionHandler` returns consistent JSON error payloads for validation and other errors.

7. Publish API spec (available)
   - OpenAPI/Swagger available at `/v3/api-docs` and `/swagger-ui.html` via Springdoc. Ensure controllers' DTOs and request/response models are documented.

8. Prepare deployment secrets and certs (partially completed)
   - `.env.example` added at repo root for environment values.
   - Certificate pinning helper: `docs/frontend-req/cert-pinning.md` (commands to extract public key SHA256).
   - Hosting and distribution registration remain operational tasks for your ops team.

Notes and remaining recommendations:
- RS256: Consider adding RS256 support and publishing the public key PEM for frontend verification; current implementation uses HS256.
- Broker: In-memory STOMP broker is for development; use RabbitMQ/Redis STOMP relay in production for scalability.
- API base path: Most controllers use `/api` prefix (check any custom endpoints). Consider enforcing a global prefix via `spring.mvc.servlet.path` or a common `@RequestMapping` base if you want strict uniformity.

#### Phase 2: Frontend skeleton and environment

1. Decide frontend stack
   - Requirements suggest Flutter/Jaspr monorepo
   - Confirm whether web will be Jaspr static build and mobile is Flutter
   - Create initial frontend project structure if absent

2. Configure environment and build settings
   - Add `.env` or `dart-define` support for:
     - `API_BASE_URL`
     - `WS_BASE_URL`
     - optionally `SENTRY_DSN`
   - Document required environment values

3. Choose HTTP client and auth storage
   - Use Dio or equivalent for HTTP requests
   - Implement centralized request/response interceptor behavior
   - Add token persistence:
     - secure storage for mobile
     - local storage/session storage for web

#### Phase 3: Frontend implementation plan

1. Authentication/UI
   - Login screen using `/auth/login`
   - Store JWT + refresh token
   - Auto-refresh token on expiration
   - `GET /auth/me` for current user profile

2. Product browsing
   - Product list with:
     - pagination
     - search
     - category filters
   - Product details screen using `GET /products/{id}`

3. Cart flow
   - Add/remove items
   - Update cart item quantities
   - Cart review page
   - Sync cart with backend via cart endpoints

4. Order flow
   - Checkout flow calling `POST /orders`
   - Order history page using `GET /orders`
   - Order detail view using `GET /orders/{id}`
   - Show order status updates

5. Real-time updates
   - Connect to WebSocket/STOMP if available
   - Display order status changes in customer UI
   - Admin view for `/topic/orders` if admin dashboard exists

6. Admin interface
   - Inventory management page
   - Create product screen
   - Edit product screen
   - Use `/admin` or `/inventory/products` endpoints with admin auth

#### Phase 4: Deployment and verification

1. Pick hosting service for web
   - Cloudflare Pages, Vercel, Netlify, or serve static build from backend
2. Pick mobile distribution method
   - App Center, GitHub Releases, Google Play internal track
3. Setup CI/CD pipeline
   - Build frontend output
   - Inject environment variables
   - Publish to chosen host
4. Validate integration
   - Test against backend dev environment
   - Verify CORS and auth behavior
   - Confirm WebSocket connection and order update flow
5. QA checks
   - Confirm error handling works with backend error format
   - Confirm token refresh works
   - Confirm admin pages enforce roles

### 4. Recommended first actions

- Confirm whether frontend will be built in this repo or as a separate monorepo
- Update backend requirements document with:
  - exact endpoint list
  - chosen auth flow
  - WebSocket path and message schema
  - CORS allowed origins
- Implement backend readiness items before starting frontend screens

### 5. High-level priority order

1. Backend contract + auth + missing endpoints
2. Environment and API base URL setup
3. Frontend skeleton + auth flow
4. Product/cart/order UX
5. Real-time order status
6. Deployment and secrets
