# BabyGuess Requirements

## 1. Purpose

BabyGuess is a small, self-hosted web application for friends to predict details about one expected baby. It
collects private guesses before a deadline, preserves every submitted version, and publishes a scored
leaderboard after an administrator reveals the actual results.

One deployed application represents exactly one baby-guessing event. Multi-event support, independent friend
groups, and participant accounts are outside the initial scope.

## 2. Technical constraints

- BabyGuess is an independent Spring Boot web application.
- The server-rendered frontend uses Thymeleaf, HTML, CSS, and JavaScript.
- BabyGuess has its own Gradle build, application port, configuration, tests, and release lifecycle.
- A production container image is built with Jib.
- The application must be runnable and deployable without Plex Manager or any other repository project.
- Persistent application data and uploaded branding assets must survive application and container restarts.
- Persistent-data schema changes must use repeatable, version-controlled migrations.

Exact framework versions, database engine, storage layout, and deployment configuration will be selected during
technical design.

## 3. Roles and access

### 3.1 Administrator

There is one administrator. The administrator can:

- Sign in to a protected administration area.
- Configure the event, prediction categories, scoring, deadline, branding, and outgoing email.
- View participants, their private email addresses, and the complete immutable submission history.
- Enter and correct the baby's actual details.
- Review and approve additional close-name matches.
- Publish the reveal and leaderboard.

The administrator username and password are supplied through application configuration. There is no admin
registration, initial-setup wizard, or password-recovery workflow.

### 3.2 Participant

Participants do not create accounts or passwords. A participant initially supplies:

- A public display name.
- A private email address.
- Between one and the configured maximum number of ordered name guesses.
- Any enabled optional predictions they choose to provide.

The email address identifies a participant uniquely, case-insensitively. Submitting an email address that
already belongs to a participant must not create a duplicate participant or expose the existing submission.
The participant receives a private, unguessable edit link by email.

Possession of the edit link authorizes access to that participant's submission. The raw token must not be
stored in recoverable form. Tokens must have enough entropy to resist guessing and must not appear in
application logs.

### 3.3 Public visitor

Before the reveal, public visitors can access the event page and submission form, but cannot see anyone else's
guesses. After the reveal, they can see the published leaderboard. Participant email addresses are never
public.

## 4. Event configuration

The administrator can configure:

- A public event title.
- One logo image.
- One background image.
- A submission deadline.
- The event timezone, defaulting to `Europe/Zurich`.
- The maximum number of name guesses per participant, with a minimum of one.
- Whether name-guess ranking affects scoring.
- Whether sex, birth date, and birth weight predictions are enabled independently.
- A scoring weight for every enabled category, defaulting to `1`.
- The accepted birth-date deviation in whole days.
- The accepted birth-weight deviation in whole grams.

Uploaded images must be validated as supported image content, stored outside packaged application resources,
and served safely without allowing arbitrary file access. Replacing an image replaces the currently active
branding asset.

Configuration changes are validated server-side. Existing submission history must never be deleted or
rewritten because the administrator changes the current configuration.

## 5. Event lifecycle

The user-visible lifecycle is:

1. **Open:** the current time is before the configured deadline. New submissions and token-authorized edits are
   accepted.
2. **Closed:** the current time is at or after the deadline. Submission and editing are rejected server-side,
   even if an old browser page remains open.
3. **Match review:** the administrator enters the actual details and reviews suggested additional name matches.
   Guesses remain private.
4. **Revealed:** the administrator publishes the results. The leaderboard becomes public and reveal emails are
   sent.

The administrator may change the deadline. Moving it into the future can reopen submissions. Moving it into
the past changes which historical submission version qualifies, as described below.

## 6. Submission and history

### 6.1 Name guesses

- Every participant must submit at least one nonblank name guess.
- A participant may submit no more than the currently configured maximum.
- Name guesses have an explicit order: guess 1, guess 2, and so on.
- Duplicate guesses within one submission are rejected after name normalization.
- Only names are repeatable; every enabled optional category has at most one value per participant.

### 6.2 Optional predictions

The administrator can independently expose these optional fields:

- Sex: `Girl` or `Boy`.
- Birth date: a calendar date.
- Birth weight: a positive whole number of grams.

An enabled optional field is available but is not mandatory for a participant. A missing prediction earns no
points for that category.

### 6.3 Immutable versions

Every successful initial submission and edit creates a new immutable, timestamped snapshot containing the
participant's complete set of guesses at that moment. Editing never overwrites an earlier snapshot.

For scoring and reveal, the effective submission is the newest snapshot whose server-recorded submission time
is at or before the current deadline:

- Snapshots after the deadline remain in the admin-visible audit history but do not count.
- A participant with no snapshot at or before the deadline is excluded from scoring and the leaderboard.
- Moving the deadline backwards automatically selects an earlier qualifying snapshot without deleting data.
- Moving the deadline forwards automatically selects the newest snapshot that qualifies under the new
  deadline.

All deadline enforcement and snapshot selection use server time in the configured event timezone. Client
timestamps are never authoritative.

## 7. Participant email links

- A successful initial submission sends an email containing the participant's private edit URL.
- Following the URL displays the participant's current editable values while the event is open.
- Saving through the URL creates a new immutable submission version and retains the same participant token.
- Once the event is closed, the URL may show the participant's effective submission but cannot modify it.
- Requests involving an existing or unknown email address must use non-enumerating responses where practical.
- The admin can resend a participant's edit email without changing their token or submission.

## 8. Name comparison

Name matching produces one of three outcomes:

1. **Exact spelling:** equal after cosmetic normalization only, such as Unicode normalization, trimming,
   case-folding, and consistent internal whitespace.
2. **Equivalent spelling:** accepted as the same name through conservative automatic spelling rules or an
   administrator-approved variant.
3. **No match.**

Exact and equivalent spellings earn the same category score. The distinction is retained for leaderboard
tie-breaking and display.

### 8.1 Automatic spelling equivalence

Automatic matching must recognize conservative, explainable variants, especially:

- Single versus doubled letters.
- `c` versus `k` where they represent the same sound.
- `f` versus `ph`.
- Common silent-letter variants such as `Sara` and `Sarah`.

For example, `Sara`/`Sarah` and `Rebecca`/`Rebeka` must match automatically. These deterministic automatic
matches do not require admin confirmation.

Phonetic and fuzzy algorithms must not independently create an automatic match. Soundex is not suitable as
the sole matcher for the application's multilingual names. A stronger phonetic comparison such as Double
Metaphone, combined with a conservative edit-distance threshold, may identify additional candidates for admin
review. This candidate mechanism must not affect scoring until the administrator approves the candidate.

### 8.2 Admin-approved variants

After entering the actual name and before publishing:

- The admin sees unmatched guesses that the application considers plausible close matches.
- The admin can approve or reject each suggestion.
- The admin can manually add an accepted spelling variant that was not suggested.
- Approved variants count as equivalent spellings, not exact spellings.
- Match decisions are retained so a later recalculation produces the same result unless the admin changes the
  decision.

## 9. Scoring

Scores are calculated from the effective historical submission and the actual revealed values. Each category
has an admin-configurable weight `W`, defaulting to `1`. Disabled categories and enabled categories without an
actual result do not contribute points.

Calculations retain sufficient decimal precision to avoid ordering changes caused by display rounding.

### 9.1 Name score

Let:

- `N` be the configured maximum number of name guesses.
- `r` be the one-based position of the highest-ranked matching guess.
- `W_name` be the configured name-category weight.

If no submitted name matches, the score is zero. If multiple guesses match, only the highest-ranked matching
guess counts.

When ranked scoring is disabled, any matching guess scores:

```text
name score = W_name
```

When ranked scoring is enabled:

```text
rank factor = (N - r + 1) / N
name score  = W_name * rank factor
```

For three allowed guesses, positions 1, 2, and 3 therefore have factors `1`, `2/3`, and `1/3`. For ten
allowed guesses, position 3 has factor `0.8`.

### 9.2 Sex score

```text
sex score = W_sex when the prediction is correct
sex score = 0 otherwise
```

### 9.3 Birth-date score

Let `D` be the absolute difference in calendar days and `T_days` the configured tolerance:

```text
date factor = max(0, 1 - D / T_days)
date score  = W_date * date factor
```

At the exact date the factor is `1`; at or beyond the tolerance it is `0`. If the tolerance is zero, only an
exact prediction receives the full category weight.

### 9.4 Birth-weight score

Let `G` be the absolute difference in grams and `T_grams` the configured tolerance:

```text
weight factor = max(0, 1 - G / T_grams)
weight score  = W_weight * weight factor
```

At the exact weight the factor is `1`; at or beyond the tolerance it is `0`. If the tolerance is zero, only
an exact prediction receives the full category weight.

### 9.5 Total score

```text
total score = name score + sex score + date score + weight score
```

Only enabled and scoreable categories are included. A participant who omitted an enabled optional prediction
receives zero for that category.

## 10. Leaderboard and tie-breaking

The revealed page displays a ranked leaderboard rather than participant email addresses. It includes:

- Place.
- Participant display name.
- Name prediction result and whether it was an exact or equivalent match.
- Sex result when enabled.
- Birth-date result when enabled.
- Birth-weight result when enabled.
- Category scores and total score.

An exact name, sex, date, or weight result displays a checkmark. A non-exact date or weight result displays its
absolute difference and earned percentage, for example `1 day off (80%)` or `10 g off (98%)`. A wrong sex or
unmatched name is visibly marked as incorrect.

Leaderboard ordering is:

1. Higher total score.
2. Better name-match class: exact spelling, then equivalent spelling, then no name match.
3. Earlier position of the matching name guess, even when ranked name scoring is disabled.

Participants still tied after these rules share the same competition-style place number (`1, 2, 2, 4`) and
are displayed alphabetically within that place. Alphabetical ordering is only presentation and does not break
the tie.

## 11. Reveal workflow

The administrator can enter:

- The actual baby name, which is required to reveal.
- Actual sex, birth date, and birth weight for enabled categories.

An enabled optional category without an actual value is shown as awaiting a result and is excluded from
scoring until supplied.

Before the first reveal, the application calculates results and presents additional close-name candidates for
admin approval. Publishing makes the actual details and leaderboard public.

The admin may correct actual details or accepted variants after publication. Such corrections recalculate the
public leaderboard but do not send another automatic reveal email.

## 12. Email

### 12.1 SMTP administration

The admin interface can configure outgoing SMTP settings, including server, port, transport security,
authentication, sender address, username, and password. It provides a test-email action with a clear success
or failure result.

SMTP credentials must never be returned to the browser after being stored, written to logs, or exposed in
error messages. The implementation must define secure secret-at-rest handling during technical design.

### 12.2 Reveal email

On the first successful publication, every participant with an effective submission receives one reveal
email. The email includes:

- The baby's revealed details.
- Which of that participant's guesses were correct or close.
- That participant's points by category.
- A link to the public leaderboard.

The email does not include the participant's overall total score or leaderboard place. Correcting a published
result does not automatically send another email. Delivery results are tracked so the admin can identify and
retry failed messages without resending successful ones.

## 13. Localization

- Initial user-interface languages are English and German.
- Portuguese and Spanish are planned, so user-facing text must not be hardcoded into templates or JavaScript.
- On first visit, the application chooses the best supported language from the browser's language preferences.
- Unsupported browser languages fall back to English.
- A visible language selector is available on public, participant, and admin pages.
- A manual selection is stored in a cookie and takes precedence on later visits.
- The participant's selected locale is retained for transactional emails.
- Dates, numbers, units, validation messages, and emails are localized consistently.

## 14. Privacy and security

- Participant emails and submission histories are visible only to the administrator and their owner through
  the private token URL.
- Public pages never reveal email addresses or historical submission versions.
- Admin pages use authenticated sessions, CSRF protection, and secure cookie settings appropriate to the
  deployment.
- Participant edit tokens, SMTP secrets, and admin credentials must not be logged.
- All submitted text is validated and safely escaped when rendered.
- Image upload handling verifies actual content and prevents path traversal and executable uploads.
- Sensitive actions such as reveal publication, deadline changes, and result corrections require an
  authenticated administrator.
- Application logs contain enough context to diagnose mail and scoring failures without exposing secrets or
  private guesses unnecessarily.

## 15. Reliability and usability

- All authorization, deadline, uniqueness, and scoring rules are enforced server-side.
- Submission and edit forms are responsive and usable on current mobile and desktop browsers.
- A successful submission clearly tells the participant to check their email for the private edit link.
- If email delivery fails after a submission is saved, the guess remains saved and the admin can retry the
  message.
- Repeated reveal or email actions must be idempotent and must not create duplicate history or duplicate
  successful emails.
- Scoring is deterministic: the same configuration, actual values, match decisions, deadline, and submission
  history always produce the same leaderboard.
- Administrative changes that affect scoring trigger a complete recalculation from immutable source data.

## 16. Initial acceptance criteria

The first releasable version is complete when:

1. An admin can log in using configured credentials and configure event branding, deadline, name-guess count,
   optional categories, tolerances, and category weights.
2. A visitor can submit their display name, email, ordered name guesses, and any enabled optional predictions
   before the deadline.
3. The participant receives a secure edit link and can use it to create later submission versions before the
   deadline.
4. Neither initial submissions nor edits are accepted at or after the current deadline.
5. Every submission version is retained, and moving the deadline backwards selects the latest version at or
   before that deadline without deleting later versions.
6. Guesses remain hidden from other participants until the reveal.
7. The automatic matcher treats `Sara`/`Sarah` and `Rebecca`/`Rebeka` as equivalent while preserving exact
   spelling as a tie-break attribute.
8. The admin can approve additional close spelling matches before publishing.
9. Name, sex, birth-date, and birth-weight scores follow the configured category weights and formulas in this
   document.
10. The published leaderboard applies the defined tie-breaking and shared-place rules and never exposes email
    addresses.
11. The first reveal sends each qualifying participant one localized result email without their total score or
    place.
12. SMTP can be configured and tested through the protected admin interface.
13. English and German are complete, browser language is detected, and a cookie preserves a manual language
    choice.
14. The application builds and runs independently and can produce its own Jib container image.

## 17. Explicitly out of scope for the initial release

- Multiple baby events in one deployment.
- Multiple friend groups or tenants.
- Participant accounts or passwords.
- Social login.
- Public participant email addresses.
- Native mobile applications.
- Portuguese and Spanish translations, although the localization design must allow them to be added later.
