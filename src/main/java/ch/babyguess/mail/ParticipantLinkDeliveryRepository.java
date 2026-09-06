package ch.babyguess.mail;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantLinkDeliveryRepository extends JpaRepository<ParticipantLinkDelivery, UUID> {

    List<ParticipantLinkDelivery> findByStatusOrderByCreatedAt(LinkDeliveryStatus status);

    @EntityGraph(attributePaths = "participant")
    List<ParticipantLinkDelivery> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "participant")
    Optional<ParticipantLinkDelivery> findFirstByParticipantIdOrderByCreatedAtDesc(UUID participantId);
}
