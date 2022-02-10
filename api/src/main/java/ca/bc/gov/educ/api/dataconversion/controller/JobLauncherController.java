package ca.bc.gov.educ.api.dataconversion.controller;

import ca.bc.gov.educ.api.dataconversion.model.ConversionBaseSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.*;

import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;

@RestController
@RequestMapping(EducGradDataConversionApiConstants.GRAD_BATCH_API_ROOT_MAPPING)
@CrossOrigin
@EnableResourceServer
public class JobLauncherController {

    private static final Logger logger = LoggerFactory.getLogger(JobLauncherController.class);
    private static final String TIME="time";
    private static final String JOB_PARAM="job";

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    public JobLauncherController(JobLauncher jobLauncher, JobRegistry jobRegistry) {
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
    }

    @GetMapping(EducGradDataConversionApiConstants.GRAD_STUDENT_DATA_CONVERSION_BATCH_JOB)
    public ResponseEntity<ConversionBaseSummaryDTO> launchStudentDataConversionJob( ) {
        logger.info("Inside Launch Student Data Conversion Job");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_PARAM, "studentDataConversionBatchJob");
        try {
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("studentDataConversionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            ConversionBaseSummaryDTO summaryDTO = (ConversionBaseSummaryDTO)jobContext.get("studentSummaryDTO");
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            e.printStackTrace();
            ConversionBaseSummaryDTO summaryDTO = new ConversionBaseSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }
    }

    @GetMapping(EducGradDataConversionApiConstants.GRAD_COURSE_RESTRICTION_DATA_CONVERSION_BATCH_JOB)
    public ResponseEntity<ConversionBaseSummaryDTO> launchCourseRestrictionDataConversionJob( ) {
        logger.info("Inside Launch Course Restriction Data Conversion Job");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_PARAM, "courseRestrictionDataConversionBatchJob");
        try {
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("courseRestrictionDataConversionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            ConversionBaseSummaryDTO summaryDTO = (ConversionBaseSummaryDTO)jobContext.get("courseRestrictionSummaryDTO");
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            e.printStackTrace();
            ConversionBaseSummaryDTO summaryDTO = new ConversionBaseSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }
    }

    @GetMapping(EducGradDataConversionApiConstants.GRAD_COURSE_REQUIREMENT_DATA_CONVERSION_BATCH_JOB)
    public ResponseEntity<ConversionBaseSummaryDTO> launchCourseRequirementDataConversionJob( ) {
        logger.info("Inside Launch Course Requirement Data Conversion Job");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_PARAM, "courseRequirementDataConversionBatchJob");
        try {
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("courseRequirementDataConversionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            ConversionBaseSummaryDTO summaryDTO = (ConversionBaseSummaryDTO)jobContext.get("courseRequirementSummaryDTO");
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            e.printStackTrace();
            ConversionBaseSummaryDTO summaryDTO = new ConversionBaseSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }
    }

    @GetMapping(EducGradDataConversionApiConstants.READ_TRAX_AND_ADD_NEW_PEN_BATCH_JOB)
    public ResponseEntity<ConversionBaseSummaryDTO> launchReadTraxAndAddNewPenIfNotExistsJob() {
        logger.info("Inside Launch Read Trax Student & Add a New PEN Job");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_PARAM, "readTraxAndAddNewPenBatchJob");
        try {
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("readTraxAndAddNewPenBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            ConversionBaseSummaryDTO summaryDTO = (ConversionBaseSummaryDTO)jobContext.get("studentSummaryDTO");
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            e.printStackTrace();
            ConversionBaseSummaryDTO summaryDTO = new ConversionBaseSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }
    }
}
