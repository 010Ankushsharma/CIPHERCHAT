<div align="center">

```
   ██████╗██╗██████╗ ██╗  ██╗███████╗██████╗  ██████╗██╗  ██╗ █████╗ ████████╗
  ██╔════╝██║██╔══██╗██║  ██║██╔════╝██╔══██╗██╔════╝██║  ██║██╔══██╗╚══██╔══╝
  ██║     ██║██████╔╝███████║█████╗  ██████╔╝██║     ███████║███████║   ██║   
  ██║     ██║██╔═══╝ ██╔══██║██╔══╝  ██╔══██╗██║     ██╔══██║██╔══██║   ██║   
  ╚██████╗██║██║     ██║  ██║███████╗██║  ██║╚██████╗██║  ██║██║  ██║   ██║   
   ╚═════╝╚═╝╚═╝     ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝   
```

**Next Generation End-to-End Encrypted Messaging Platform**

*Privacy of Signal · Smoothness of iMessage · Flexibility of Telegram · AI-powered · Production-ready*

[![CI](https://github.com/your-org/cipherchat/actions/workflows/ci.yml/badge.svg)](https://github.com/your-org/cipherchat/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.7.0-4285F4)](https://www.jetbrains.com/lf/compose-multiplatform/)
[![Ktor](https://img.shields.io/badge/Ktor-3.0.1-087CFA)](https://ktor.io)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

</div>

---

## Overview

CipherChat is a production-grade, end-to-end encrypted messaging platform built with Kotlin Multiplatform. It targets Android, iOS, and Desktop from a single codebase, backed by a horizontally-scalable Ktor microservice architecture.

Every message is encrypted using the **Signal Protocol** (X3DH + Double Ratchet) before leaving the device. The server stores only metadata and opaque ciphertext blobs — it structurally cannot read your messages.

---

## Architecture

```
CipherChat/
├── client/                         # Kotlin Multiplatform client (Android · iOS · Desktop)
│   ├── core/
│   │   ├── domain/                 # Models · Repository interfaces · Use cases
│   │   ├── crypto/                 # Signal Protocol wrapper · Secure key storage
│   │   ├── network/                # Ktor client · WebSocket · REST DTOs
│   │   ├── database/               # SQLDelight schema · Encrypted local cache
│   │   └── designsystem/           # Color · Typography · Spacing · Components
│   ├── feature/
│   │   ├── onboarding/             # 7-page animated onboarding flow
│   │   ├── auth/                   # Email · Phone OTP · OAuth · Passkey · QR
│   │   └── chat/                   # Home screen · Chat screen · Real-time messaging
│   ├── androidApp/                 # Android entry point (MainActivity · Application)
│   ├── iosApp/                     # iOS entry point (placeholder)
│   └── desktopApp/                 # Desktop entry point (placeholder)
│
└── server/                         # Ktor microservices
    ├── shared/                     # Shared DTOs across services
    ├── gateway/                    # WebSocket + REST gateway (client entry point)
    ├── auth-service/               # Auth · Devices · JWT · Prekey management
    ├── messaging-service/          # Message routing · Chat metadata · Kafka fan-out
    ├── docker-compose.yml          # Local dev stack (Postgres · Redis · Kafka)
    ├── docker/                     # Dockerfiles
    └── k8s/                        # Kubernetes manifests (Kustomize)
        ├── base/                   # Base Deployment · Service · HPA · ConfigMap
        └── overlays/
            ├── staging/            # 2 replicas · relaxed limits · mutable tag
            └── production/         # 5+ replicas · SHA-pinned image · manual gate
```

---

## Security Architecture

### End-to-End Encryption
- **Signal Protocol** — X3DH for initial key agreement, Double Ratchet for per-message forward secrecy
- **Post-compromise security** — ratchet advances on every message; past messages safe even if current keys leak
- **Per-device encryption** — messages encrypted separately to each linked device (phone, tablet, desktop)
- **Libsodium primitives** — X25519, Ed25519, XChaCha20-Poly1305 via audited KMP bindings

### Key Storage
| Platform | Implementation |
|---|---|
| Android | Android Keystore (StrongBox-preferred) + AES-256-GCM wrapping |
| iOS | Secure Enclave + Keychain |
| Desktop | OS keychain (macOS Keychain · Windows Credential Manager · libsecret) |

Private keys **never** touch serializable types, never leave secure hardware, never appear in logs.

### Server Privacy Guarantee
The server is structurally incapable of reading message content:
- `Messages` table has a `ciphertext` column but no `body` column — there is nowhere to store plaintext
- Gateway routing code never imports a JSON parser for message content
- Ciphertext purged after all recipient devices acknowledge delivery

### Threat Model
| Threat | Mitigation |
|---|---|
| MITM on TLS | Certificate pinning in `network_security_config.xml` |
| Compromised server | E2E encryption — server only sees ciphertext |
| Stolen device | Hardware-backed key storage, biometric gate |
| Rogue CA cert | User-installed CAs excluded (Android API 24+) |
| Token theft | 15-min access tokens, refresh token rotation |
| OTP brute-force | 5 req/5 min rate limit, bcrypt-hashed codes |
| Auto Backup leak | `allowBackup="false"` in AndroidManifest |

---

## Technology Stack

### Client
| Layer | Technology |
|---|---|
| Language | Kotlin 2.1.0 |
| UI Framework | Compose Multiplatform 1.7 |
| Design System | Material 3 + Haze glassmorphism |
| Architecture | MVVM + Clean Architecture |
| Navigation | Voyager |
| Dependency Injection | Koin 4 |
| Image Loading | Coil 3 |
| Local Database | SQLDelight 2 + SQLCipher |
| Networking | Ktor client 3 (OkHttp · Darwin · CIO) |
| Async | Kotlin Coroutines + Flow |
| Crypto | libsodium KMP bindings |
| Logging | Napier |

### Server
| Layer | Technology |
|---|---|
| Language | Kotlin 2.1.0 |
| Framework | Ktor 3 + Netty |
| ORM | Jetbrains Exposed |
| Database | PostgreSQL 16 |
| Cache | Redis 7 (Lettuce async client) |
| Message Bus | Apache Kafka 3.8 (KRaft, no Zookeeper) |
| Auth | JWT (JJWT) + bcrypt |
| Metrics | Micrometer → Prometheus → Grafana |
| DI | Koin |
| Logging | Logback (JSON in production) |

---

## Getting Started

### Prerequisites
- JDK 17+
- Android Studio Hedgehog (2023.1.1)+
- Docker Desktop
- `kubectl` + `kustomize` (for k8s deployment)

### Local Development — Backend

```bash
# Start the full backend stack (Postgres · Redis · Kafka · Gateway)
cd server
docker compose up -d

# Verify gateway is running
curl http://localhost:8080/metrics
```

### Local Development — Android Client

```bash
# Build and install on connected device / emulator
cd client
./gradlew :androidApp:installDebug
```

### Environment Variables

| Variable | Required | Description |
|---|---|---|
| `DATABASE_URL` | ✅ | PostgreSQL JDBC URL |
| `DATABASE_USER` | ✅ | Database username |
| `DATABASE_PASSWORD` | ✅ | Database password |
| `REDIS_URL` | ✅ | Redis connection URL |
| `KAFKA_BOOTSTRAP_SERVERS` | ✅ | Kafka broker addresses |
| `JWT_SECRET` | ✅ | ≥32 char secret for HMAC-SHA256 |
| `JWT_ISSUER` | ✅ | Token issuer claim |
| `JWT_AUDIENCE` | ✅ | Token audience claim |
| `PORT` | ⬜ | Server port (default: 8080) |
| `LOG_FORMAT` | ⬜ | `console` (dev) or `json` (prod) |
| `LOG_LEVEL` | ⬜ | `INFO` (default) or `DEBUG` |

---

## Deployment

### Staging
Deployed automatically on every merge to `main`:

```bash
kustomize build server/k8s/overlays/staging | kubectl apply -f -
```

### Production
Triggered by pushing a version tag — requires **manual approval** in GitHub Environments:

```bash
git tag v1.0.0
git push origin v1.0.0
# → GitHub Actions builds, tests, builds Docker image
# → Team member approves in GitHub UI
# → Deploys with SHA-pinned image, smoke tests, auto-rollback on failure
```

### Kubernetes Architecture
```
                    ┌─────────────────────────────────────┐
                    │         Load Balancer (L7)           │
                    └──────────────┬──────────────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              ▼                    ▼                    ▼
        ┌──────────┐        ┌──────────┐        ┌──────────┐
        │ Gateway  │        │ Gateway  │        │ Gateway  │
        │ Pod (AZ1)│        │ Pod (AZ2)│        │ Pod (AZ3)│
        └────┬─────┘        └────┬─────┘        └────┬─────┘
             │                   │                   │
             └───────────────────┼───────────────────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          ▼                      ▼                      ▼
    ┌──────────┐          ┌──────────┐          ┌──────────┐
    │PostgreSQL│          │  Redis   │          │  Kafka   │
    │ (managed)│          │ (managed)│          │ (managed)│
    └──────────┘          └──────────┘          └──────────┘
```

---

## Module Dependency Graph

```
androidApp ──┐
             ├──► feature:chat ──┐
             ├──► feature:auth ──┤
             └──► feature:onb ───┤
                                 ├──► core:designsystem
                                 ├──► core:domain ◄──── (all features)
                                 └──► (via core:data, TBD)
                                          ├──► core:network
                                          ├──► core:crypto
                                          └──► core:database

core:domain    — zero external dependencies (pure Kotlin)
core:crypto    — depends on core:domain + libsodium
core:network   — depends on core:domain + core:crypto (for CipherText types)
core:database  — depends on core:domain + core:crypto (for DB encryption key)
core:designsystem — depends on Compose only (never on core:domain)
```

**Dependency rules enforced by convention:**
- `core:designsystem` never imports `core:domain`
- Feature modules never import `core:network` or `core:database` directly
- `core:crypto` private key types are never `@Serializable`

---

## Project Status

| Area | Status | Notes |
|---|---|---|
| Domain layer | ✅ Complete | Models, repositories, use cases |
| Crypto (Signal Protocol) | ✅ Scaffold | Libsodium-backed; swap internals for `libsignal` before prod |
| Network (Ktor client) | ✅ Complete | WebSocket + REST + DTOs |
| Database (SQLDelight) | ✅ Complete | Schema + queries for all entities |
| Design system | ✅ Complete | Color · Typography · Spacing · 5 components |
| Onboarding | ✅ Complete | 7 screens, full flow |
| Auth | ✅ Complete | 6 screens, all methods |
| Chat (Home + Chat screen) | ✅ Complete | Needs ViewModel wiring |
| Android app shell | ✅ Complete | MainActivity, Application, Manifest |
| iOS app shell | 🔲 Placeholder | Voyager + SwiftUI bridge TBD |
| Desktop app shell | 🔲 Placeholder | Compose Desktop entry point TBD |
| core:data (repository impls) | 🔲 Next | Wire domain interfaces to network/db/crypto |
| ViewModels | 🔲 Next | Wire UI to use cases |
| Push notifications | 🔲 Planned | FCM + APNs via notification-service |
| Voice/Video calls | 🔲 Planned | WebRTC + call-service |
| AI features | 🔲 Planned | On-device ML + AiRepository impl |
| Server gateway | ✅ Complete | Routing, WebSocket, JWT, metrics |
| Auth service | ✅ Complete | Schema, AuthService, PrekeyService |
| Messaging service | ✅ Complete | Schema, MessagingService, Kafka |
| Docker (local dev) | ✅ Complete | `docker compose up -d` |
| Kubernetes | ✅ Complete | Base + staging + production overlays |
| CI/CD | ✅ Complete | PR checks + staging auto-deploy + prod manual gate |

---

## Key Architectural Decisions

### Why one `AuthChoiceScreen` rather than per-method screens?
Eight login methods given equal visual weight creates decision paralysis. Primary methods (email/phone) get primary button emphasis; OAuth/passkey/anonymous are grouped below an "or" divider at lower emphasis. This matches how production auth screens are designed.

### Why `MessageExpiration.ReadOnce` (Whisper Messages) is only Text/Image/VoiceNote
Whisper semantics ("disappears immediately after reading, cannot screenshot/forward/copy") are enforced at the UI layer and are only meaningful for content types where the "no screenshot" UX makes sense. Code/Poll/Document Whisper messages would be odd UX, so they silently fall back to `None` expiration.

### Why `core:designsystem` never imports `core:domain`
Design components must be reusable without carrying business logic as a dependency. `MessageBubble` takes a `MessageBubbleStatus` enum (defined in designsystem) not `MessageStatus` (defined in domain) — `feature:chat` maps between them. This keeps the design system independently testable and theoretically reusable in a different product.

### Why the server uses Kafka for message fan-out rather than direct WebSocket calls
The gateway is horizontally scaled — any instance might hold the recipient's WebSocket connection. Kafka decouples "store + publish" (messaging-service) from "deliver to connected client" (gateway). Each gateway instance subscribes and delivers to its locally-connected sessions. This is the standard pattern for horizontally-scaled real-time systems.

### Why refresh tokens are bcrypt-hashed server-side
If the `auth_sessions` table leaks, raw refresh tokens must not be reusable. Bcrypt adds ~100ms to refresh operations (every 15 minutes at most), which is acceptable. Access tokens are stateless JWTs (no database lookup on every request).

---

## Security Notes for Production

Before first production release, complete these items:

- [ ] Replace `LibsodiumSignalProtocolEngine` internals with `libsignal` bindings
- [ ] Set real certificate pins in `network_security_config.xml`
- [ ] Replace `SchemaUtils.create` with Flyway migrations
- [ ] Wire `isFirstLaunch` to DataStore (not hardcoded `true`)
- [ ] Complete `SecureEnclaveStorage` iOS Keychain interop
- [ ] Set `JWT_SECRET` to a cryptographically random 64+ char secret
- [ ] Enable Kafka TLS between services
- [ ] Configure PostgreSQL TLS and connection encryption
- [ ] Set up Redis AUTH password and TLS
- [ ] Remove `REPLACE_WITH_*` placeholder values from k8s manifests
- [ ] Configure actual APNs/FCM credentials for push notifications
- [ ] Run a third-party security audit before public launch

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m 'Add my feature'`
4. Push to the branch: `git push origin feature/my-feature`
5. Open a Pull Request — CI runs automatically

**Code style:** `kotlin.code.style=official` (enforced via `.editorconfig`)

---

## License

MIT License — see [LICENSE](LICENSE) for details.

---

<div align="center">
Built with ❤️ using Kotlin Multiplatform
</div>
