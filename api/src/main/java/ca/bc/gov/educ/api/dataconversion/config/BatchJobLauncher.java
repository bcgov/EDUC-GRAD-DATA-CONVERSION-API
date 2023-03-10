package ca.bc.gov.educ.api.dataconversion.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This bean schedules and runs our Spring Batch job.
 */
@Component
public class BatchJobLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchJobLauncher.class);

    private final Job job;
    private final JobLauncher jobLauncher;

    @Autowired
    public BatchJobLauncher(Job studentLoadJob, JobLauncher jobLauncher) {
        this.job = studentLoadJob;
        this.jobLauncher = jobLauncher;
    }

    public void runSpringBatchExampleJob() throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
        LOGGER.info("Batch Job was started");
        jobLauncher.run(job, newExecution());
        LOGGER.info("Batch Job was stopped");
    }

    private JobParameters newExecution() {
        Map<String, JobParameter<?>> parameters = new HashMap<>();
        JobParameter<?> parameter = new JobParameter(new Date(), Date.class);
        parameters.put("currentTime", parameter);
        return new JobParameters(parameters);
    }
}
