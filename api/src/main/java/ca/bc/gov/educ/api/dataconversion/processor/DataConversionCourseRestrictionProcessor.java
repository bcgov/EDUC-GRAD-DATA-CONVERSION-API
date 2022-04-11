package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.model.ConversionBaseSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.ConversionCourseSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.CourseRestriction;
import ca.bc.gov.educ.api.dataconversion.service.course.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

public class DataConversionCourseRestrictionProcessor implements ItemProcessor<CourseRestriction, CourseRestriction> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionCourseRestrictionProcessor.class);

    @Autowired
	private CourseService courseService;

	private ConversionCourseSummaryDTO summaryDTO;

	@BeforeStep
	public void retrieveSummaryDto(StepExecution stepExecution) {
		JobExecution jobExecution = stepExecution.getJobExecution();
		ExecutionContext jobContext = jobExecution.getExecutionContext();
		summaryDTO = (ConversionCourseSummaryDTO)jobContext.get("courseRestrictionSummaryDTO");
	}

	@Override
	public CourseRestriction process(CourseRestriction courseRestriction) throws Exception {
		return courseService.convertCourseRestriction(courseRestriction, summaryDTO);
	}
}
