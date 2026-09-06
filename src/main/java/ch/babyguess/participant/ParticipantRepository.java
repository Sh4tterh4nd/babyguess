package ch.babyguess.participant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    Optional<Participant> findByNormalizedEmailAddress(String normalizedEmailAddress);

    Optional<Participant> findByEditTokenHash(String editTokenHash);

    List<Participant> findAllByOrderByCreatedAtAsc();
}
