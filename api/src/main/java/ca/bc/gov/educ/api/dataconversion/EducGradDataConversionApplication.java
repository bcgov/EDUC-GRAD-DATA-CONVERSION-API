package ca.bc.gov.educ.api.dataconversion;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableRetry
@EnableSchedulerLock(defaultLockAtMostFor = "1s")
public class EducGradDataConversionApplication {

    public static void main(String[] args) {
        SpringApplication.run(EducGradDataConversionApplication.class, args);
    }

}
