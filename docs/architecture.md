# Initial technical design

## Runtime and build

- Java 21 is the source and container runtime baseline. The build also runs on newer supported JDKs.
- Spring Boot 4.1.1 provides Spring MVC, Thymeleaf, Security, Validation, Mail, and Data JPA integration.
- Gradle 9.7.1 is checked in through the wrapper. Jib 3.5.4 creates the container image.
- The application listens on port `8080` by default, overridable through `BABYGUESS_PORT`.

## Persistence

Flyway owns the database schema; Hibernate schema generation is disabled. A file-backed H2 database in
PostgreSQL compatibility mode is the default for both local development and the single-container production
deployment, so every environment runs the same migration scripts. The PostgreSQL driver is also supported for
deployments that outgrow a single instance; an H2-backed service must never be scaled beyond one container.

Submission versions are append-only at the application boundary. Each version has its own ordered name-guess
rows and optional category values. The effective version is computed as the latest server timestamp at or before
the current deadline, so deadline changes never rewrite history.

Logo and background uploads live outside packaged resources under `./data/branding` by default, which is inside
the container's persistent data volume. The browser filename and declared content type are ignored. The server
limits the byte count, uses ImageIO to identify and decode PNG/JPEG content, bounds dimensions and total pixels,
then activates a generated UUID filename through the singleton event configuration. Public requests expose only
the fixed `/branding/logo` and `/branding/background` routes; no user-controlled path reaches the filesystem.
Replacement files are coordinated with the configuration transaction so rollbacks remove the new file and a
successful commit removes the formerly active file.

The singleton event configuration uses an optimistic version column. Admin forms carry the version they read;
an older form is rejected if another request saved first. Deadlines are entered as local wall-clock times and
converted to instants with an IANA timezone. Times inside daylight-saving gaps or overlaps are rejected as
ambiguous rather than silently adjusted.

Edit links use an HMAC-SHA-256 token derived from a random participant UUID and a deployment secret. Only the
token's SHA-256 digest is stored in `participant.edit_token_hash`; the raw token exists transiently while composing
the email URL. This keeps tokens stable for resends without storing recoverable raw URLs. The production profile
rejects the development secret, and operators must retain their secret across restarts.

The singleton SMTP configuration also uses optimistic locking. Administrator-entered SMTP passwords are
write-only: the request value is encrypted with AES-256-GCM using a new random 96-bit nonce before persistence,
and neither the ciphertext nor plaintext is included in the admin view model. The deployment supplies the stable
32-byte encryption key separately through `BABYGUESS_SECRET_ENCRYPTION_KEY`; the key is never stored in the
database. Missing keys and damaged ciphertext fail closed. SMTP test failures are reduced to safe result codes,
while durable participant-delivery records retain only exception classes or credential-availability codes.
Saved, enabled SMTP settings take precedence over standard `spring.mail` deployment settings, which remain a
backward-compatible fallback.

Every initial link-email request has a delivery row. Sending happens after the submission transaction commits, so
mail failure cannot erase a guess. Failures retain only a safe exception code for later administrator retry; raw
tokens and mail exception messages are not logged or persisted. An authenticated retry reuses the failed delivery
row, increments its attempt count, and derives the stable token only while composing the new message. The retry is
refused if the current deployment secret no longer reproduces the participant's stored token digest.

## Domain boundaries

The name matcher, score calculator, and effective-submission selector are independent domain services. They do
not depend on controllers or persistence, making their deterministic behavior cheap to test and safe to reuse
for the public leaderboard and reveal emails.

Automatic name equivalence is intentionally narrow and explainable. Cosmetic normalization is checked first so
exact matches retain their stronger tie-break class. A second key handles repeated Latin letters, hard `c`/`k`,
`ph`/`f`, and a trailing silent `h`. Broader phonetic or edit-distance matches will only become review candidates
and will never award points without an administrator decision.

Review candidates use a deliberately conservative edit-distance filter and never change scoring by themselves.
Accept/reject decisions are stored against both the cosmetic actual name and cosmetic guessed name. Correcting
the baby's name therefore activates only decisions made for that actual name, while changing back reproduces the
earlier result. Manual accepted variants use the same retained decision path.

The private leaderboard is recalculated from immutable effective submissions whenever actual details, scoring
configuration, the deadline, or a name decision changes. Ranking retains full `BigDecimal` precision and applies
total score, name-match class, and matching position before assigning competition-style places. Alphabetical
ordering affects presentation only after the place-defining comparisons are tied.

## Security and localization

Only `/admin/**` requires the configured administrator session at present. CSRF protection remains enabled.
Session cookies are HTTP-only and become secure by default in the production profile.

All current UI text lives in English and German message bundles. A language query parameter writes a one-year
cookie; unsupported locales fall back to English. The locale used for a participant's first saved submission is
retained for future edit-link messages.
