package ch.babyguess.web.admin;

import ch.babyguess.event.EventConfiguration;
import ch.babyguess.event.EventConfigurationService;
import ch.babyguess.event.RevealNotReadyException;
import ch.babyguess.name.NameCandidateSuggester;
import ch.babyguess.name.NameMatchDecision;
import ch.babyguess.name.NameMatchDecisionRepository;
import ch.babyguess.name.NameMatchDecisionType;
import ch.babyguess.name.NameMatchType;
import ch.babyguess.name.NameMatcher;
import ch.babyguess.scoring.ActualBaby;
import ch.babyguess.scoring.LeaderboardRanker;
import ch.babyguess.scoring.ParticipantPrediction;
import ch.babyguess.scoring.RankablePrediction;
import ch.babyguess.scoring.ScoringConfiguration;
import ch.babyguess.scoring.ToleratedCategory;
import ch.babyguess.scoring.WeightedCategory;
import ch.babyguess.submission.NameGuess;
import ch.babyguess.submission.SubmissionVersion;
import ch.babyguess.submission.SubmissionVersionRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminRevealService {

    private static final MathContext PRECISION = MathContext.DECIMAL128;

    private final EventConfigurationService eventService;
    private final SubmissionVersionRepository submissionRepository;
    private final NameMatchDecisionRepository decisionRepository;
    private final NameMatcher nameMatcher;
    private final NameCandidateSuggester candidateSuggester;
    private final LeaderboardRanker leaderboardRanker;
    private final Clock clock;

    public AdminRevealService(
            EventConfigurationService eventService,
            SubmissionVersionRepository submissionRepository,
            NameMatchDecisionRepository decisionRepository,
            NameMatcher nameMatcher,
            NameCandidateSuggester candidateSuggester,
            LeaderboardRanker leaderboardRanker,
            Clock clock) {
        this.eventService = eventService;
        this.submissionRepository = submissionRepository;
        this.decisionRepository = decisionRepository;
        this.nameMatcher = nameMatcher;
        this.candidateSuggester = candidateSuggester;
        this.leaderboardRanker = leaderboardRanker;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminRevealView view(Locale locale) {
        var event = eventService.get();
        var effective = effectiveSubmissions(event.getSubmissionDeadline());
        var closed = event.getSubmissionDeadline() != null
                && !clock.instant().isBefore(event.getSubmissionDeadline());
        var formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale)
                .withZone(ZoneId.of(event.getEventTimezone()));
        var deadline = event.getSubmissionDeadline() == null
                ? null
                : formatter.format(event.getSubmissionDeadline());
        if (event.getActualName() == null || event.getActualName().isBlank()) {
            return new AdminRevealView(
                    closed,
                    deadline,
                    false,
                    null,
                    null,
                    null,
                    null,
                    event.isSexEnabled(),
                    event.isBirthDateEnabled(),
                    event.isBirthWeightEnabled(),
                    effective.size(),
                    List.of(),
                    List.of(),
                    List.of());
        }

        var actualCosmetic = nameMatcher.cosmeticForm(event.getActualName());
        var decisions = decisionRepository.findByActualCosmeticNameOrderByGuessedDisplayNameAsc(actualCosmetic);
        var accepted = decisions.stream()
                .filter(decision -> decision.getDecision() == NameMatchDecisionType.ACCEPTED)
                .map(NameMatchDecision::getGuessedCosmeticName)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        var actual = new ActualBaby(
                event.getActualName(),
                event.getActualSex(),
                event.getActualBirthDate(),
                event.getActualBirthWeightGrams());
        var rankable = effective.values().stream().map(this::toRankable).toList();
        var ranked = leaderboardRanker.rank(scoringConfiguration(event), actual, accepted, rankable);
        var leaderboard = ranked.stream()
                .map(entry -> toView(entry, event, actual, locale))
                .toList();

        return new AdminRevealView(
                closed,
                deadline,
                true,
                event.getActualName(),
                event.getActualSex(),
                event.getActualBirthDate(),
                event.getActualBirthWeightGrams(),
                event.isSexEnabled(),
                event.isBirthDateEnabled(),
                event.isBirthWeightEnabled(),
                effective.size(),
                candidateViews(event.getActualName(), effective, decisions),
                decisions.stream()
                        .map(decision -> new AdminRevealView.NameDecision(
                                decision.getGuessedDisplayName(), decision.getDecision()))
                        .toList(),
                leaderboard);
    }

    @Transactional
    public void reviewCandidate(String displayName, NameMatchDecisionType decision) {
        var context = decisionContext();
        var normalized = requireVariant(displayName);
        boolean isCurrentCandidate = candidateViews(
                        context.event().getActualName(),
                        context.effectiveSubmissions(),
                        context.decisions()).stream()
                .anyMatch(candidate -> nameMatcher.cosmeticForm(candidate.displayName())
                        .equals(nameMatcher.cosmeticForm(normalized)));
        if (!isCurrentCandidate) {
            throw new IllegalArgumentException("The name is not an unresolved review candidate");
        }
        saveDecision(context.event().getActualName(), normalized, decision);
    }

    @Transactional
    public void acceptManualVariant(String displayName) {
        var context = decisionContext();
        saveDecision(context.event().getActualName(), requireVariant(displayName), NameMatchDecisionType.ACCEPTED);
    }

    @Transactional
    public void changeDecision(String displayName, NameMatchDecisionType decision) {
        var context = decisionContext();
        var variant = requireVariant(displayName);
        var actual = nameMatcher.cosmeticForm(context.event().getActualName());
        var cosmetic = nameMatcher.cosmeticForm(variant);
        if (decisionRepository.findByActualCosmeticNameAndGuessedCosmeticName(actual, cosmetic).isEmpty()) {
            throw new IllegalArgumentException("The name decision does not exist");
        }
        saveDecision(context.event().getActualName(), variant, decision);
    }

    private DecisionContext decisionContext() {
        var event = eventService.get();
        if (event.getSubmissionDeadline() == null || clock.instant().isBefore(event.getSubmissionDeadline())) {
            throw new RevealNotReadyException();
        }
        if (event.getActualName() == null || event.getActualName().isBlank()) {
            throw new IllegalArgumentException("Actual details must be saved first");
        }
        var actual = nameMatcher.cosmeticForm(event.getActualName());
        return new DecisionContext(
                event,
                effectiveSubmissions(event.getSubmissionDeadline()),
                decisionRepository.findByActualCosmeticNameOrderByGuessedDisplayNameAsc(actual));
    }

    private void saveDecision(String actualName, String displayName, NameMatchDecisionType decision) {
        var actual = nameMatcher.cosmeticForm(actualName);
        var cosmetic = nameMatcher.cosmeticForm(displayName);
        var now = clock.instant();
        var entity = decisionRepository.findByActualCosmeticNameAndGuessedCosmeticName(actual, cosmetic)
                .orElseGet(() -> new NameMatchDecision(actual, cosmetic, displayName, decision, now));
        if (entity.getId() != null) {
            entity.update(displayName, decision, now);
        }
        decisionRepository.save(entity);
    }

    private Map<UUID, SubmissionVersion> effectiveSubmissions(Instant deadline) {
        var effective = new LinkedHashMap<UUID, SubmissionVersion>();
        if (deadline == null) {
            return effective;
        }
        for (var version : submissionRepository.findAllByOrderBySubmittedAtDesc()) {
            if (!version.getSubmittedAt().isAfter(deadline)) {
                effective.putIfAbsent(version.getParticipant().getId(), version);
            }
        }
        return effective;
    }

    private List<AdminRevealView.NameCandidate> candidateViews(
            String actualName,
            Map<UUID, SubmissionVersion> effective,
            List<NameMatchDecision> decisions) {
        var decided = decisions.stream()
                .map(NameMatchDecision::getGuessedCosmeticName)
                .collect(java.util.stream.Collectors.toSet());
        var candidates = new LinkedHashMap<String, CandidateAccumulator>();
        for (var entry : effective.entrySet()) {
            var participantVariants = new HashSet<String>();
            for (var guess : entry.getValue().getNameGuesses()) {
                var cosmetic = guess.getCosmeticName();
                if (!decided.contains(cosmetic)
                        && participantVariants.add(cosmetic)
                        && candidateSuggester.isPlausible(actualName, guess.getGuessedName())) {
                    candidates.compute(cosmetic, (ignored, current) -> current == null
                            ? new CandidateAccumulator(guess.getGuessedName(), 1)
                            : current.incremented());
                }
            }
        }
        return candidates.values().stream()
                .map(candidate -> new AdminRevealView.NameCandidate(
                        candidate.displayName(), candidate.participantCount()))
                .sorted(java.util.Comparator.comparing(
                        AdminRevealView.NameCandidate::displayName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private RankablePrediction toRankable(SubmissionVersion version) {
        return new RankablePrediction(
                version.getParticipant().getId(),
                version.getParticipant().getDisplayName(),
                new ParticipantPrediction(
                        version.getNameGuesses().stream().map(NameGuess::getGuessedName).toList(),
                        version.getPredictedSex(),
                        version.getPredictedBirthDate(),
                        version.getPredictedBirthWeightGrams()));
    }

    private ScoringConfiguration scoringConfiguration(EventConfiguration event) {
        return new ScoringConfiguration(
                event.getMaximumNameGuesses(),
                event.isRankedNameScoring(),
                event.getNameWeight(),
                new WeightedCategory(event.isSexEnabled(), event.getSexWeight()),
                new ToleratedCategory(
                        event.isBirthDateEnabled(),
                        event.getBirthDateWeight(),
                        event.getBirthDateToleranceDays()),
                new ToleratedCategory(
                        event.isBirthWeightEnabled(),
                        event.getBirthWeightWeight(),
                        event.getBirthWeightToleranceGrams()));
    }

    private AdminRevealView.LeaderboardEntry toView(
            ch.babyguess.scoring.RankedPrediction ranked,
            EventConfiguration event,
            ActualBaby actual,
            Locale locale) {
        var prediction = ranked.participant().prediction();
        var score = ranked.score();
        var matchedName = score.matchedNamePosition() == null
                ? null
                : prediction.nameGuesses().get(score.matchedNamePosition() - 1);
        return new AdminRevealView.LeaderboardEntry(
                ranked.place(),
                ranked.participant().participantId(),
                ranked.participant().displayName(),
                prediction.nameGuesses(),
                matchedName,
                score.nameMatchType(),
                formatScore(score.nameScore(), locale),
                formatScore(score.totalScore(), locale),
                sexResult(event, actual, prediction, score.sexScore(), locale),
                toleratedResult(
                        event.isBirthDateEnabled(),
                        actual.birthDate() != null,
                        prediction.birthDate() == null ? null : prediction.birthDate().toString(),
                        score.birthDateScore(),
                        score.birthDateDifferenceDays(),
                        event.getBirthDateToleranceDays(),
                        locale),
                toleratedResult(
                        event.isBirthWeightEnabled(),
                        actual.birthWeightGrams() != null,
                        prediction.birthWeightGrams() == null ? null : prediction.birthWeightGrams() + " g",
                        score.birthWeightScore(),
                        score.birthWeightDifferenceGrams(),
                        event.getBirthWeightToleranceGrams(),
                        locale));
    }

    private AdminRevealView.CategoryResult sexResult(
            EventConfiguration event,
            ActualBaby actual,
            ParticipantPrediction prediction,
            BigDecimal score,
            Locale locale) {
        if (!event.isSexEnabled() || actual.sex() == null) {
            return categoryResult(AdminRevealView.ResultOutcome.AWAITING, null, score, null, null, locale);
        }
        if (prediction.sex() == null) {
            return categoryResult(AdminRevealView.ResultOutcome.MISSING, null, score, null, null, locale);
        }
        var outcome = prediction.sex() == actual.sex()
                ? AdminRevealView.ResultOutcome.EXACT
                : AdminRevealView.ResultOutcome.WRONG;
        return categoryResult(outcome, prediction.sex().name(), score, null, null, locale);
    }

    private AdminRevealView.CategoryResult toleratedResult(
            boolean enabled,
            boolean actualKnown,
            String prediction,
            BigDecimal score,
            Integer difference,
            int tolerance,
            Locale locale) {
        if (!enabled || !actualKnown) {
            return categoryResult(AdminRevealView.ResultOutcome.AWAITING, prediction, score, null, null, locale);
        }
        if (prediction == null || difference == null) {
            return categoryResult(AdminRevealView.ResultOutcome.MISSING, null, score, null, null, locale);
        }
        if (difference == 0) {
            return categoryResult(AdminRevealView.ResultOutcome.EXACT, prediction, score, 0, 100, locale);
        }
        if (tolerance > 0 && difference < tolerance) {
            var percentage = BigDecimal.ONE.subtract(
                            BigDecimal.valueOf(difference).divide(BigDecimal.valueOf(tolerance), PRECISION))
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValueExact();
            return categoryResult(
                    AdminRevealView.ResultOutcome.CLOSE,
                    prediction,
                    score,
                    difference,
                    percentage,
                    locale);
        }
        return categoryResult(AdminRevealView.ResultOutcome.WRONG, prediction, score, difference, 0, locale);
    }

    private AdminRevealView.CategoryResult categoryResult(
            AdminRevealView.ResultOutcome outcome,
            String prediction,
            BigDecimal score,
            Integer difference,
            Integer percentage,
            Locale locale) {
        return new AdminRevealView.CategoryResult(
                outcome, prediction, formatScore(score, locale), difference, percentage);
    }

    private String formatScore(BigDecimal value, Locale locale) {
        var formatter = NumberFormat.getNumberInstance(locale);
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(2);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        return formatter.format(value);
    }

    private String requireVariant(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A name variant is required");
        }
        var stripped = value.strip();
        if (stripped.length() > 160) {
            throw new IllegalArgumentException("The name variant is too long");
        }
        return stripped;
    }

    private record CandidateAccumulator(String displayName, int participantCount) {
        CandidateAccumulator incremented() {
            return new CandidateAccumulator(displayName, participantCount + 1);
        }
    }

    private record DecisionContext(
            EventConfiguration event,
            Map<UUID, SubmissionVersion> effectiveSubmissions,
            List<NameMatchDecision> decisions) {
    }
}
