package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.entity.trax.GraduationCourseEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConversionCourseSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.service.course.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

public class DataConversionCourseRequirementProcessor implements ItemProcessor<GraduationCourseEntity, GraduationCourseEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionCourseRequirementProcessor.class);

    @Autowired
	private CourseService courseService;

	private ConversionCourseSummaryDTO summaryDTO;

	@BeforeStep
	public void retrieveSummaryDto(StepExecution stepExecution) {
		JobExecution jobExecution = stepExecution.getJobExecution();
		ExecutionContext jobContext = jobExecution.getExecutionContext();
		summaryDTO = (ConversionCourseSummaryDTO)jobContext.get("courseRequirementSummaryDTO");
	}

	@Override
	public GraduationCourseEntity process(GraduationCourseEntity graduationCourseEntity) throws Exception {
		return courseService.convertCourseRequirement(graduationCourseEntity, summaryDTO);
	}
}
