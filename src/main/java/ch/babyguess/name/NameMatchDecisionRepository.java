package ch.babyguess.name;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NameMatchDecisionRepository extends JpaRepository<NameMatchDecision, Long> {

    List<NameMatchDecision> findByActualCosmeticNameOrderByGuessedDisplayNameAsc(String actualCosmeticName);

    Optional<NameMatchDecision> findByActualCosmeticNameAndGuessedCosmeticName(
            String actualCosmeticName,
            String guessedCosmeticName);
}
