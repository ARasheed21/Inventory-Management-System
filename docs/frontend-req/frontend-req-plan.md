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

1. Confirm backend contract and version of API base path
   - Decide if frontend should use `/api` prefix explicitly
   - Standardize URLs and document them as `API_BASE_URL` and `WS_BASE_URL`

2. Implement required auth support
   - Add `POST /auth/login` returning JWT + refresh token
   - Add `POST /auth/refresh`
   - Add `GET /auth/me`
   - Switch security to JWT bearer tokens
   - Ensure token payload includes `userId`, `email`, `roles`
   - Prefer RS256 if possible; otherwise HS256
   - Provide public key PEM if using RS256

3. Add missing cart endpoints
   - Support cart CRUD operations
   - Keep request/response payloads consistent and documented

4. Add real-time order status support
   - Add `/orders/status/{orderId}` or WebSocket STOMP `/ws/orders`
   - If using STOMP:
     - support `CONNECT` with JWT header
     - `/user/queue/orders` for customer
     - `/topic/orders` for admin
     - JSON payload with `orderId` and `status`

5. Add CORS configuration
   - Allow web dashboard origin
   - Allow credentials and methods GET, POST, PUT, DELETE, OPTIONS

6. Add standard error response schema
   - Consistent JSON format for errors
   - Example: `{ "timestamp": "...", "status": 401, "message": "...", "path": "/cart" }`

7. Publish API spec
   - Ensure OpenAPI/Swagger spec is available and up to date
   - Optionally provide Postman collection

8. Prepare deployment secrets and certs
   - Generate/publish SSL public key hashes
   - Create environment secrets for `API_BASE_URL`, `WS_BASE_URL`
   - Register hosting and distribution services if not already done

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
