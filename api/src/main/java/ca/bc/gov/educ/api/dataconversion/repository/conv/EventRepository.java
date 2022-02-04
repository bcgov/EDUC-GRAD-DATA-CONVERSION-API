package ca.bc.gov.educ.api.dataconversion.repository.conv;

import ca.bc.gov.educ.api.dataconversion.entity.conv.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    Optional<Event> findByEventId(UUID eventId);

    List<Event> findAllByEventStatusOrderByCreateDate(String eventStatus);
}
