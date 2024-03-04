package ca.bc.gov.educ.api.dataconversion.controller;

import ca.bc.gov.educ.api.dataconversion.constant.BatchStatusEnum;
import ca.bc.gov.educ.api.dataconversion.model.BatchJobResponse;
import ca.bc.gov.educ.api.dataconversion.model.ConversionBaseSummaryDTO;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
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
import org.springframework.web.bind.annotation.*;

import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;

import java.util.Date;

@RestController
@RequestMapping(EducGradDataConversionApiConstants.GRAD_DATA_CONVERSION_API_ROOT_MAPPING)
@CrossOrigin
@OpenAPIDefinition(info = @Info(title = "API for Data Conversion & Ongoing Updates.",
        description = "This API is for Reading TRAX data and Persisting GRAD data.", version = "1"))
public class JobLauncherController {

    private static final Logger logger = LoggerFactory.getLogger(JobLauncherController.class);
    private static final String TIME="time";
    private static final String JOB_PARAM="job";
    private static final String RELOAD_PARAM="reload";

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    public JobLauncherController(JobLauncher jobLauncher, JobRegistry jobRegistry) {
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
    }

    @GetMapping(EducGradDataConversionApiConstants.GRAD_COURSE_RESTRICTION_DATA_CONVERSION_BATCH_JOB)
    @Operation(summary = "Initial Load of Course Restrictions", description = "Loading Course Restrictions from TRAX into GRAD", tags = { "Courses" })
    public ResponseEntity<BatchJobResponse> launchCourseRestrictionDataConversionJob( ) {
        logger.info("Inside Launch Course Restriction Data Conversion Job");
        BatchJobResponse response = new BatchJobResponse();
        response.setJobType("courseRestrictionDataConversionBatchJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_PARAM, "courseRestrictionDataConversionBatchJob");
        try {
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("courseRestrictionDataConversionBatchJob"), builder.toJobParameters());
            response.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping(EducGradDataConversionApiConstants.GRAD_COURSE_REQUIREMENT_DATA_CONVERSION_BATCH_JOB)
    @Operation(summary = "Initial Load of Course Requirements", description = "Loading Course Requirements from TRAX into GRAD", tags = { "Courses" })
    public ResponseEntity<BatchJobResponse> launchCourseRequirementDataConversionJob( ) {
        logger.info("Inside Launch Course Requirement Data Conversion Job");
        BatchJobResponse response = new BatchJobResponse();
        response.setJobType("courseRequirementDataConversionBatchJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_PARAM, "courseRequirementDataConversionBatchJob");
        try {
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("courseRequirementDataConversionBatchJob"), builder.toJobParameters());
            response.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping(EducGradDataConversionApiConstants.GRAD_STUDENT_PARALLEL_DATA_CONVERSION_BATCH_JOB)
    @Operation(summary = "Initial Load of Students in Async Parallel Processing", description = "Loading students from TRAX into GRAD in Parallel using the partitions that are getting the paginated list from TRAX_STUDENT_NO table", tags = { "Students" })
    public ResponseEntity<BatchJobResponse> launchStudentDataConversionPartitionJob(
            @RequestParam(value = "reload", required = false, defaultValue = "false") boolean isReload) {
        logger.info("Inside Launch Student Data Conversion Partition Job - Parallel Processing");
        BatchJobResponse response = new BatchJobResponse();
        response.setJobType("studentLoadJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_PARAM, "studentLoadJob");
        builder.addString(RELOAD_PARAM, isReload? "Y" : "N");
        try {
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("studentLoadJob"), builder.toJobParameters());
            response.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping(EducGradDataConversionApiConstants.PEN_UPDATES_PARALLEL_BATCH_JOB)
    @Operation(summary = "Pen Updates in Async Parallel Processing", description = "Add missing students into PEN in Parallel using the partitions that are getting the paginated list from TRAX_STUDENT_NO table", tags = { "Utils" })
    public ResponseEntity<BatchJobResponse> launchPenUpdatesJob() {
        logger.info("Inside Launch PEN Updates Job");
        BatchJobResponse response = new BatchJobResponse();
        response.setJobType("penUpdatesJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_PARAM, "penUpdatesJob");
        try {
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("penUpdatesJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            response.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private ConversionBaseSummaryDTO handleSuccess(ExecutionContext jobContext , String summaryDtoName) throws JobParametersInvalidException {
        return (ConversionBaseSummaryDTO)jobContext.get(summaryDtoName);
    }

    private ConversionBaseSummaryDTO handleException(Exception e) {
        e.printStackTrace();
        ConversionBaseSummaryDTO summaryDTO = new ConversionBaseSummaryDTO();
        summaryDTO.setException(e.getLocalizedMessage());
        return summaryDTO;
    }
}
