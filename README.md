# BabyGuess

BabyGuess is a self-hosted guessing game for friends of expectant parents. Participants privately predict the
baby's name and, when enabled, sex, birth date, and birth weight. After the baby arrives, the administrator
publishes a scored leaderboard.

The agreed product behavior and acceptance criteria are documented in [REQUIREMENTS.md](REQUIREMENTS.md).

## Current implementation

The current implementation includes:

- Spring Boot 4.1, Thymeleaf, Spring Security, JPA, Flyway, and a Gradle wrapper.
- A persistent local H2 database, an H2-based single-instance production Compose deployment, and PostgreSQL
  driver support for larger deployments.
- The initial versioned schema for event settings, participants, immutable submission versions, ordered name
  guesses, and accepted name variants.
- Exact/equivalent name matching, including the required `Sara`/`Sarah` and `Rebecca`/`Rebeka` variants.
- High-precision category scoring and effective-submission selection at a deadline.
- English and German UI scaffolding, a protected admin route, and a responsive public landing page.
- An authenticated event-settings form for the deadline, timezone, prediction categories, weights, and
  tolerances, with server-side validation and stale-update protection.
- A deadline-aware public submission form, case-insensitive participant identity, immutable submission versions,
  private edit links, and tracked link-email delivery attempts.
- An authenticated participant ledger showing the effective prediction, full immutable history, delivery status,
  and safe retries for failed edit-link emails.
- A closed-event reveal workspace for actual details, retained close-name decisions, deterministic scoring,
  competition-style places, and a private leaderboard preview.
- An explicit, one-time publication boundary that opens a sanitized public leaderboard and remains idempotent
  across repeated requests.
- Localized participant reveal emails with actual details and per-category results (without overall place or
  total), plus durable delivery status and safe retries that never automatically resend successful messages.
- Protected runtime SMTP administration for the server, port, transport security, sender, authentication, and
  write-only encrypted password, including a sanitized test-email result.
- Protected logo and background uploads with decoded-content validation, generated storage names, fixed public
  asset URLs, safe replacement, and the Public title reused as the site-wide wordmark.
- A Jib container build targeting port `8080` and persistent `/app/data` storage.

## Run locally

Java 21 or newer is required. No system Gradle installation is needed.

```powershell
.\gradlew.bat bootRun
```

Open `http://localhost:8080`. Local data is stored in `./data` and is excluded from Git.

The development-only admin credentials are `admin` / `admin12345678`. Override both through
`BABYGUESS_ADMIN_USERNAME` and `BABYGUESS_ADMIN_PASSWORD`; passwords shorter than 12 characters are rejected.

## Configure event branding

Sign in and open `http://localhost:8080/admin/branding`. You can upload one logo and one background as PNG or
JPEG. Images are limited to 5 MB, 8,000 pixels per side, and 40 million total pixels. The server verifies decoded
image content rather than trusting the filename or browser content type. Replacing an image removes the previous
active asset after the database update commits.

Branding files are stored in `./data/branding` by default, inside the container's persistent `/app/data` volume.
Set `BABYGUESS_BRANDING_DIRECTORY` only when the deployment uses a different persistent location. The top-left
wordmark always uses the configured Public title; an uploaded logo appears beside it.

## Configure outgoing email

Sign in and open `http://localhost:8080/admin/email`. The protected page stores the SMTP server, port, transport
security, sender address, username, and an optional password. A submitted password is encrypted before database
storage, is never returned to the browser, and can be retained by leaving the password field blank.

Authenticated SMTP configured through the page requires a stable 32-byte encryption key. Generate one once in
PowerShell and provide the resulting Base64 value as `BABYGUESS_SECRET_ENCRYPTION_KEY`:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Keep that key in the deployment secret store and retain it across restarts. Losing or changing it makes the
stored SMTP password unreadable; enter a new password after restoring the key. The page's test-delivery section
reports only safe result categories and does not display provider error messages.

Existing deployments may continue to configure `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`,
`SPRING_MAIL_PASSWORD`, Spring Mail SMTP properties, and `BABYGUESS_MAIL_FROM`. Those environment settings are
used when the saved SMTP configuration is disabled.

Run the test suite with:

```powershell
.\gradlew.bat test
```

## Production profile

The `prod` profile requires these environment variables:

- `BABYGUESS_DATABASE_URL` - a JDBC URL. The production Compose file supplies a persistent H2 file URL;
  PostgreSQL URLs are also supported.
- `BABYGUESS_DATABASE_USERNAME` and `BABYGUESS_DATABASE_PASSWORD`.
- `BABYGUESS_ADMIN_USERNAME` and a non-default `BABYGUESS_ADMIN_PASSWORD`.
- `BABYGUESS_TOKEN_SECRET` - at least 32 characters; changing it invalidates existing participant edit links.
- `BABYGUESS_PUBLIC_BASE_URL` - the canonical HTTPS URL used in participant emails.
- `BABYGUESS_SECRET_ENCRYPTION_KEY` - a Base64-encoded 32-byte key when authenticated SMTP is stored through the
  admin page; retain it for as long as the stored credential is needed.
- `BABYGUESS_BRANDING_DIRECTORY` - optional branding storage path; defaults to the persistent
  `./data/branding` directory.

Secure session cookies are enabled by default in production. Build and push `shatterhand/babyguess` to Docker
Hub without a Docker daemon using:

```powershell
.\gradlew.bat jib
```

Jib publishes both the Gradle project version and `latest` tags configured in `build.gradle`.

## Continuous integration

`.github/workflows/ci.yml` builds and tests every push to `main` and every pull request against it.

Pushing a tag that starts with `v` additionally publishes the container image to the GitHub Container
Registry at `ghcr.io/<owner>/<repository>`, using the workflow's own `GITHUB_TOKEN`. No registry secret has
to be configured. The tag name without its leading `v` becomes the image version, so `v1.2.0` publishes
`:1.2.0` and moves `:latest`. A pre-release tag such as `v1.2.0-rc1` publishes only `:1.2.0-rc1` and leaves
`:latest` untouched.

```powershell
git tag v1.2.0
git push origin v1.2.0
```

The first publication creates a private package. Make it public through the package settings on GitHub if
deployments should pull it without credentials.

## Docker Compose

The production deployment uses file-backed H2 and is intentionally limited to one BabyGuess container. Copy the
environment template, replace every placeholder, and start it behind a reverse proxy running on the Docker host:

```powershell
Copy-Item .env.production.example .env.production
docker compose --env-file .env.production -f compose.production.yml config
docker compose --env-file .env.production -f compose.production.yml up -d
```

Production publishes BabyGuess only on `127.0.0.1:8080`. Point the host reverse proxy there, terminate TLS at the
proxy, and forward the original host plus `X-Forwarded-Proto: https`. Set `BABYGUESS_PUBLIC_BASE_URL` to the
canonical external URL, including `https://`. If the reverse proxy is another container, attach it to the Compose
network and route to `babyguess:8080` instead of using the host-only port.

For development or staging, build or pull the desired tag and use the second Compose file:

```powershell
Copy-Item .env.dev-staging.example .env.dev-staging
docker compose --env-file .env.dev-staging -f compose.dev-staging.yml up -d
```

The development defaults are deliberately non-production. For staging, set `SPRING_PROFILES_ACTIVE=prod`, use
stable random secrets, configure the HTTPS public URL, set `BABYGUESS_SECURE_COOKIES=true`, and normally bind
`BABYGUESS_HOST_ADDRESS=127.0.0.1`. Both deployments keep the H2 database and branding files in a named volume.
Keep the same database password, token secret, and encryption key across restarts and upgrades, and back up the
volume regularly. Do not scale the H2-backed service beyond one container.
