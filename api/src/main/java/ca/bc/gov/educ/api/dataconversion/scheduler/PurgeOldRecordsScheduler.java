package ca.bc.gov.educ.api.dataconversion.scheduler;

import ca.bc.gov.educ.api.dataconversion.repository.EventRepository;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class PurgeOldRecordsScheduler {
    private final EventRepository eventRepository;
    private final EducGradDataConversionApiConstants constants;

    public PurgeOldRecordsScheduler(final EventRepository eventRepository,
                                    final EducGradDataConversionApiConstants constants) {
        this.eventRepository = eventRepository;
        this.constants = constants;
    }

    @Scheduled(cron = "${cron.scheduled.process.purge-old-records.run}")
    @SchedulerLock(name = "PurgeOldRecordsLock",
            lockAtLeastFor = "PT1H", lockAtMostFor = "PT1H") //midnight job so lock for an hour
    @Transactional
    public void purgeOldRecords() {
        LockAssert.assertLocked();
        final LocalDateTime createDateToCompare = this.calculateCreateDateBasedOnStaleEventInDays();
        this.eventRepository.deleteByCreateDateBefore(createDateToCompare);
    }


    private LocalDateTime calculateCreateDateBasedOnStaleEventInDays() {
        final LocalDateTime currentTime = LocalDateTime.now();
        return currentTime.minusDays(this.constants.getRecordsStaleInDays());
    }
}
