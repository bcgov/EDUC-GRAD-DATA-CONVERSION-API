package ca.bc.gov.educ.api.dataconversion.controller;

import java.util.List;

import ca.bc.gov.educ.api.dataconversion.model.ConversionSummaryDTO;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.*;

import ca.bc.gov.educ.api.dataconversion.model.LoadStudentData;
import ca.bc.gov.educ.api.dataconversion.service.GradStudentService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.PermissionsContants;

@RestController
@RequestMapping(EducGradBatchGraduationApiConstants.GRAD_BATCH_API_ROOT_MAPPING)
@CrossOrigin
@EnableResourceServer
public class JobLauncherController {

    private static final Logger logger = LoggerFactory.getLogger(JobLauncherController.class);
    private static final String TIME="time";
    private static final String JOB_PARAM="job";

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;
    private final GradStudentService gradStudentService;

    public JobLauncherController(JobLauncher jobLauncher, JobRegistry jobRegistry, GradStudentService gradStudentService) {
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
        this.gradStudentService = gradStudentService;
    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_BATCH_JOB)
    public void launchJob( ) {
    	logger.info("Inside Launch Job");
    	JobParametersBuilder builder = new JobParametersBuilder();
    	builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
    	builder.addString(JOB_PARAM, "GraduationBatchJob");
    	try {
        jobLauncher.run(jobRegistry.getJob("GraduationBatchJob"), builder.toJobParameters());
      } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
          | JobParametersInvalidException | NoSuchJobException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    	
    }

    @PostMapping(EducGradBatchGraduationApiConstants.LOAD_STUDENT_IDS)
    @PreAuthorize(PermissionsContants.LOAD_STUDENT_IDS)
    public void loadStudentIDs(@RequestBody List<LoadStudentData> loadStudentData) {
    	logger.info("Inside loadStudentIDs");
    	OAuth2AuthenticationDetails auth = (OAuth2AuthenticationDetails) SecurityContextHolder.getContext().getAuthentication().getDetails(); 
    	String accessToken = auth.getTokenValue();
    	gradStudentService.getStudentByPenFromStudentAPI(loadStudentData,accessToken);
    	
    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_DATA_CONVERSION_BATCH_JOB)
    public ResponseEntity<ConversionSummaryDTO> launchDataConversionJob( ) {
        logger.info("Inside Launch Data Conversion Job");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_PARAM, "dataConversionBatchJob");
        try {
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("dataConversionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            ConversionSummaryDTO summaryDTO = (ConversionSummaryDTO)jobContext.get("summaryDTO");
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            e.printStackTrace();
            ConversionSummaryDTO summaryDTO = new ConversionSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }
    }
}
