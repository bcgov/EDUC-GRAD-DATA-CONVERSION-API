package ca.bc.gov.educ.api.dataconversion.listener;

import ca.bc.gov.educ.api.dataconversion.model.ConversionCourseSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;

@Component
public class CourseRestrictionDataConversionJobCompletionNotificationListener implements JobExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourseRestrictionDataConversionJobCompletionNotificationListener.class);
    
    @Override
    public void afterJob(JobExecution jobExecution) {
		if (jobExecution.getStatus() == BatchStatus.COMPLETED ||
			jobExecution.getStatus() == BatchStatus.FAILED ||
			jobExecution.getStatus() == BatchStatus.UNKNOWN) {
			long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
			LOGGER.info("=======================================================================================");
	    	LOGGER.info("Data Conversion - Course Restriction Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus().toString());

			ExecutionContext jobContext = jobExecution.getExecutionContext();
			ConversionCourseSummaryDTO summaryDTO = (ConversionCourseSummaryDTO)jobContext.get("courseRestrictionSummaryDTO");

			LOGGER.info(" Records read:		{}", summaryDTO.getReadCount());
			LOGGER.info(" Processed count:	{}", summaryDTO.getProcessedCount());
			LOGGER.info(" Created count:	{}", summaryDTO.getAddedCountForCourseRestriction());
			LOGGER.info(" Updated count:	{}", summaryDTO.getUpdatedCountForCourseRestriction());
			LOGGER.info(" --------------------------------------------------------------------------------------");
			LOGGER.info(" Not good:			{}", summaryDTO.getErrors().size());
			summaryDTO.getErrors().forEach(e ->
				LOGGER.info("	{} - Course Restriction: {}, Reason: {}", e.getLevel(), e.getItem(), e.getReason())
			);
			LOGGER.info("=======================================================================================");
		}
    }
}
