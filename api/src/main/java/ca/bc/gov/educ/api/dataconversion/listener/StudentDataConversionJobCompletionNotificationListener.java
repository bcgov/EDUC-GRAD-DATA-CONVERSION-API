package ca.bc.gov.educ.api.dataconversion.listener;

import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class StudentDataConversionJobCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentDataConversionJobCompletionNotificationListener.class);
    
    @Override
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED || jobExecution.getStatus() == BatchStatus.FAILED) {
	    	long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().getTime();
			LOGGER.info("=======================================================================================");
	    	LOGGER.info("Data Conversion - Student Load Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus().toString());

			ExecutionContext jobContext = jobExecution.getExecutionContext();
			ConversionStudentSummaryDTO summaryDTO = (ConversionStudentSummaryDTO)jobContext.get("studentSummaryDTO");

			LOGGER.info(" Records read:		{}", summaryDTO.getReadCount());
			LOGGER.info(" Processed count:	{}", summaryDTO.getProcessedCount());
			LOGGER.info(" Created count:	{}", summaryDTO.getAddedCount());
			LOGGER.info(" Updated count:	{}", summaryDTO.getUpdatedCount());
			LOGGER.info(" Not Good:			{}", summaryDTO.getErroredCount());
			LOGGER.info(" --------------------------------------------------------------------------------------");
			summaryDTO.getProgramCountMap().entrySet().stream().forEach(e -> {
				String key = e.getKey();
				LOGGER.info(" {} count:	{}", key, summaryDTO.getProgramCountMap().get(key));
			});
			LOGGER.info(" --------------------------------------------------------------------------------------");
			LOGGER.info(" Optional Program Subtotals:");
			summaryDTO.getOptionalProgramCountMap().entrySet().stream().forEach(e -> {
				String key = e.getKey();
				LOGGER.info("	{} count: {}", key, summaryDTO.getOptionalProgramCountMap().get(key));
			});
			LOGGER.info(" --------------------------------------------------------------------------------------");
			LOGGER.info(" Career Program Subtotals:");
			summaryDTO.getCareerProgramCountMap().entrySet().stream().forEach(e -> {
				String key = e.getKey();
				LOGGER.info("	{} count: {}", key, summaryDTO.getCareerProgramCountMap().get(key));
			});
			LOGGER.info("=======================================================================================");
		}
    }
}
