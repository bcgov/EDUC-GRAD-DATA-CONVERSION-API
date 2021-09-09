package ca.bc.gov.educ.api.dataconversion.listener;

import ca.bc.gov.educ.api.dataconversion.model.ConversionCourseSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class CourseRequirementDataConversionJobCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourseRequirementDataConversionJobCompletionNotificationListener.class);
    
    @Override
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
	    	long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().getTime();
			LOGGER.info("=======================================================================================");
	    	LOGGER.info("Data Conversion - Course Requirement Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus().toString());

			ExecutionContext jobContext = jobExecution.getExecutionContext();
			ConversionCourseSummaryDTO summaryDTO = (ConversionCourseSummaryDTO)jobContext.get("courseRequirementSummaryDTO");

			LOGGER.info(" Records read:		{}", summaryDTO.getReadCount());
			LOGGER.info(" Processed count:	{}", summaryDTO.getProcessedCount());
			LOGGER.info(" [Course Requirement]-------------------------------------------------------------------");
			LOGGER.info("  Created count:	{}", summaryDTO.getAddedCountForCourseRequirement());
			LOGGER.info("  Updated count:	{}", summaryDTO.getUpdatedCountForCourseRequirement());
			LOGGER.info(" [Assessment Requirement]---------------------------------------------------------------");
			LOGGER.info("  Created count:	{}", summaryDTO.getAddedCountForAssessmentRequirement());
			LOGGER.info("  Updated count:	{}", summaryDTO.getUpdatedCountForAssessmentRequirement());
			LOGGER.info(" --------------------------------------------------------------------------------------");
			LOGGER.info(" Not good:			{}", summaryDTO.getErrors().size());
			summaryDTO.getErrors().forEach(e ->
				LOGGER.info("	{} - Course Requirement: {}, Reason: {}", e.getLevel(), e.getItem(), e.getReason())
			);
			LOGGER.info("=======================================================================================");
		}
    }
}
