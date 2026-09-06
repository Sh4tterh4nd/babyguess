package ch.babyguess.submission;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionVersionRepository extends JpaRepository<SubmissionVersion, UUID> {

    List<SubmissionVersion> findByParticipantIdOrderBySubmittedAtDesc(UUID participantId);

    long countByParticipantId(UUID participantId);

    @EntityGraph(attributePaths = {"participant", "nameGuesses"})
    List<SubmissionVersion> findAllByOrderBySubmittedAtDesc();
}
