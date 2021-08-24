package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.ConversionSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.GradCourseRestriction;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;
import ca.bc.gov.educ.api.dataconversion.service.course.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

public class DataConversionCourseRestrictionProcessor implements ItemProcessor<GradCourseRestriction, GradCourseRestriction> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionCourseRestrictionProcessor.class);

    @Autowired
	private CourseService courseService;

	private ConversionSummaryDTO summaryDTO;

	@BeforeStep
	public void retrieveSummaryDto(StepExecution stepExecution) {
		JobExecution jobExecution = stepExecution.getJobExecution();
		ExecutionContext jobContext = jobExecution.getExecutionContext();
		summaryDTO = (ConversionSummaryDTO)jobContext.get("courseRestrictionSummaryDTO");
	}

	@Override
	public GradCourseRestriction process(GradCourseRestriction gradCourseRestriction) throws Exception {
		return courseService.convertCourseRestriction(gradCourseRestriction, summaryDTO);
	}
}
