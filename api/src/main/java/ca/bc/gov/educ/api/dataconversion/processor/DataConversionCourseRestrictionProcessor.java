package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.model.ConversionCourseSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.CourseRestriction;
import ca.bc.gov.educ.api.dataconversion.process.CourseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("ALL")
public class DataConversionCourseRestrictionProcessor implements ItemProcessor<CourseRestriction, CourseRestriction> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionCourseRestrictionProcessor.class);

    @Autowired
	private CourseProcess courseProcess;

	private ConversionCourseSummaryDTO summaryDTO;

	@BeforeStep
	public void retrieveSummaryDto(StepExecution stepExecution) {
		JobExecution jobExecution = stepExecution.getJobExecution();
		ExecutionContext jobContext = jobExecution.getExecutionContext();
		summaryDTO = (ConversionCourseSummaryDTO)jobContext.get("courseRestrictionSummaryDTO");
	}

	@Override
	public CourseRestriction process(CourseRestriction courseRestriction) throws Exception {
		return courseProcess.convertCourseRestriction(courseRestriction, summaryDTO);
	}
}
