package ca.bc.gov.educ.api.dataconversion.listener;

import ca.bc.gov.educ.api.dataconversion.model.ConversionSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class CourseRestrictionDataConversionJobCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourseRestrictionDataConversionJobCompletionNotificationListener.class);
    
    @Override
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
	    	long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().getTime();
			LOGGER.info("=======================================================================================");
	    	LOGGER.info("Data Conversion Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus().toString());

			ExecutionContext jobContext = jobExecution.getExecutionContext();
			ConversionSummaryDTO summaryDTO = (ConversionSummaryDTO)jobContext.get("courseRestrictionSummaryDTO");

			LOGGER.info(" Records read:		{}", summaryDTO.getReadCount());
			LOGGER.info(" Processed count:	{}", summaryDTO.getProcessedCount());
			LOGGER.info(" Created count:	{}", summaryDTO.getAddedCount());
			LOGGER.info(" Updated count:	{}", summaryDTO.getUpdatedCount());
			LOGGER.info(" --------------------------------------------------------------------------------------");
			LOGGER.info(" Errors: 			{}", summaryDTO.getErrors().size());
			summaryDTO.getErrors().forEach(e ->
				LOGGER.info("	Course Restriction: {}, Reason: {}", e.getItem(), e.getReason())
			);
			LOGGER.info("=======================================================================================");
		}
    }
}
