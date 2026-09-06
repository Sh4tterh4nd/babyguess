package ch.babyguess.mail;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevealEmailDeliveryRepository extends JpaRepository<RevealEmailDelivery, UUID> {

    Optional<RevealEmailDelivery> findByParticipantId(UUID participantId);

    long countByStatus(LinkDeliveryStatus status);

    @EntityGraph(attributePaths = "participant")
    Optional<RevealEmailDelivery> findWithParticipantById(UUID id);

    @EntityGraph(attributePaths = "participant")
    List<RevealEmailDelivery> findAllByOrderByCreatedAtAsc();
}
