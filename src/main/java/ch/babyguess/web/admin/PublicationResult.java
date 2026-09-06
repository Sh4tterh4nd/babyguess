package ch.babyguess.web.admin;

import java.util.List;
import java.util.UUID;

public record PublicationResult(boolean firstPublication, List<UUID> deliveryIds) {
}
