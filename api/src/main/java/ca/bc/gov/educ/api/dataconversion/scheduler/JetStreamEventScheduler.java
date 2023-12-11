package ca.bc.gov.educ.api.dataconversion.scheduler;

import ca.bc.gov.educ.api.dataconversion.choreographer.ChoreographEventHandler;
import ca.bc.gov.educ.api.dataconversion.entity.Event;
import ca.bc.gov.educ.api.dataconversion.repository.EventRepository;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static ca.bc.gov.educ.api.dataconversion.constant.EventStatus.DB_COMMITTED;

/**
 * This class is responsible to check the PEN_MATCH_EVENT table periodically and publish messages to Jet Stream, if some them are not yet published
 * this is a very edge case scenario which will occur.
 */
@Component
@Slf4j
public class JetStreamEventScheduler {

    /**
     * The Event repository.
     */
    private final EventRepository eventRepository;

    private final ChoreographEventHandler choreographer;

    private final EducGradDataConversionApiConstants constants;

    /**
     * Instantiates a new Stan event scheduler.
     *
     * @param eventRepository the event repository
     * @param choreographer   the choreographer
     */
    public JetStreamEventScheduler(final EventRepository eventRepository,
                                   final ChoreographEventHandler choreographer,
                                   final EducGradDataConversionApiConstants constants) {
        this.eventRepository = eventRepository;
        this.choreographer = choreographer;
        this.constants = constants;
    }

    @Scheduled(cron = "${cron.scheduled.process.events.stan.run}") // minimum = every 5 minute
    @SchedulerLock(name = "PROCESS_CHOREOGRAPHED_EVENTS_FROM_JET_STREAM", lockAtLeastFor = "${cron.scheduled.process.events.stan.lockAtLeastFor}", lockAtMostFor = "${cron.scheduled.process.events.stan.lockAtMostFor}")
    public void findAndProcessEvents() {
        LockAssert.assertLocked();
        log.debug("PROCESS_CHOREOGRAPHED_EVENTS_FROM_JET_STREAM: started - cron {}, lockAtMostFor {}", constants.getTraxToGradCronRun(), constants.getTraxToGradLockAtMostFor());
        final var results = this.eventRepository.findAllByEventStatusOrderByCreateDate(DB_COMMITTED.toString());
        if (!results.isEmpty()) {
            var filteredList = results.stream().filter(el -> el.getUpdateDate().isBefore(LocalDateTime.now().minusMinutes(5))).toList();
            int cnt = 0;
            for (Event e : filteredList) {
                if (cnt++ >= constants.getTraxToGradProcessingThreshold()) {
                    log.info(" ==> Reached the processing threshold of {}", constants.getTraxToGradProcessingThreshold());
                    break;
                }
                try {
                    choreographer.handleEvent(e);
                } catch (final Exception ex) {
                    log.error("Exception while trying to handle message", ex);
                }
            }
            log.debug("PROCESS_CHOREOGRAPHED_EVENTS_FROM_JET_STREAM: processing is completed");
        }
    }

}

