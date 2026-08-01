# Contributing to CipherChat

## Development Setup

### Requirements
- JDK 17+ (`sdk install java 17.0.9-tem`)
- Android Studio Hedgehog 2023.1.1+
- Docker Desktop 4.x+
- Xcode 15+ (iOS development only)

### Clone and build

```bash
git clone https://github.com/your-org/cipherchat.git
cd cipherchat

# Start backend infrastructure
cd server && docker compose up -d && cd ..

# Build client modules
cd client && ./gradlew build
```

### Running tests

```bash
# Client — all common tests (JVM target)
cd client && ./gradlew allTests

# Server — all tests (requires docker compose up -d)
cd server && ./gradlew test
```

## Code Style

- Kotlin official code style enforced via `.editorconfig`
- No wildcard imports
- Max line length: 120 chars
- Document any non-obvious architectural decision with a KDoc comment
- Security-relevant code must always explain *why*, not just *what*

## Pull Request Process

1. Open an issue first for significant changes
2. Branch from `develop`, not `main`
3. Keep PRs focused — one concern per PR
4. All tests must pass
5. Update documentation if behaviour changes
6. Two approvals required for merge to `main`

## Architecture Rules (Enforced by Code Review)

- `core:designsystem` **never** imports `core:domain`
- Feature modules **never** import `core:network` or `core:database`
- Private key types are **never** `@Serializable`
- No crypto primitive implementations from scratch — use libsodium
- Every security decision must have a comment explaining the threat it mitigates

## Commit Message Format

```
type(scope): subject

body (optional)
```

Types: `feat`, `fix`, `security`, `refactor`, `test`, `docs`, `ci`, `chore`

Examples:
```
feat(auth): add passkey registration flow
security(crypto): switch OTP codes to bcrypt hashing
fix(chat): correct message bubble tail corner radius
```
