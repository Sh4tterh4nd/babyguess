# BabyGuess

BabyGuess is a self-hosted guessing game for friends of expectant parents. Participants privately predict the
baby's name and, when enabled, sex, birth date, and birth weight. After the baby arrives, the administrator
publishes a scored leaderboard.

The agreed product behavior and acceptance criteria are documented in [REQUIREMENTS.md](REQUIREMENTS.md).

## Current implementation

The current implementation includes:

- Spring Boot 4.1, Thymeleaf, Spring Security, JPA, Flyway, and a Gradle wrapper.
- A persistent local H2 database and a production PostgreSQL profile.
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
- A Jib container build targeting port `8088` and persistent `/app/data` storage.

## Run locally

Java 21 or newer is required. No system Gradle installation is needed.

```powershell
.\gradlew.bat bootRun
```

Open `http://localhost:8088`. Local data is stored in `./data` and is excluded from Git.

The development-only admin credentials are `admin` / `admin12345678`. Override both through
`BABYGUESS_ADMIN_USERNAME` and `BABYGUESS_ADMIN_PASSWORD`; passwords shorter than 12 characters are rejected.

## Configure event branding

Sign in and open `http://localhost:8088/admin/branding`. You can upload one logo and one background as PNG or
JPEG. Images are limited to 5 MB, 8,000 pixels per side, and 40 million total pixels. The server verifies decoded
image content rather than trusting the filename or browser content type. Replacing an image removes the previous
active asset after the database update commits.

Branding files are stored in `./data/branding` by default, inside the container's persistent `/app/data` volume.
Set `BABYGUESS_BRANDING_DIRECTORY` only when the deployment uses a different persistent location. The top-left
wordmark always uses the configured Public title; an uploaded logo appears beside it.

## Configure outgoing email

Sign in and open `http://localhost:8088/admin/email`. The protected page stores the SMTP server, port, transport
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

- `BABYGUESS_DATABASE_URL` — a PostgreSQL JDBC URL.
- `BABYGUESS_DATABASE_USERNAME` and `BABYGUESS_DATABASE_PASSWORD`.
- `BABYGUESS_ADMIN_USERNAME` and a non-default `BABYGUESS_ADMIN_PASSWORD`.
- `BABYGUESS_TOKEN_SECRET` - at least 32 characters; changing it invalidates existing participant edit links.
- `BABYGUESS_PUBLIC_BASE_URL` - the canonical HTTPS URL used in participant emails.
- `BABYGUESS_SECRET_ENCRYPTION_KEY` - a Base64-encoded 32-byte key when authenticated SMTP is stored through the
  admin page; retain it for as long as the stored credential is needed.
- `BABYGUESS_BRANDING_DIRECTORY` - optional branding storage path; defaults to the persistent
  `./data/branding` directory.

Secure session cookies are enabled by default in production. Build an OCI image without a Docker daemon using:

```powershell
.\gradlew.bat jib -Pimage=registry.example/babyguess:tag
```
