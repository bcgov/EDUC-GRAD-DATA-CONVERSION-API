package ca.bc.gov.educ.api.dataconversion.repository;

import ca.bc.gov.educ.api.dataconversion.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    Optional<Event> findByEventId(UUID eventId);

    List<Event> findAllByEventStatusOrderByCreateDate(String eventStatus);

    @Transactional
    @Modifying
    @Query("delete from Event where createDate <= :createDate")
    void deleteByCreateDateBefore(LocalDateTime createDate);
}
