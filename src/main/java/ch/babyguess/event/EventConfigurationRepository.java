package ch.babyguess.event;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventConfigurationRepository extends JpaRepository<EventConfiguration, Short> {
}
