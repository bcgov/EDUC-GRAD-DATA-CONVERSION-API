package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.entity.conv.GraduationCourseEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConversionBaseSummaryDTO;
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

	private ConversionBaseSummaryDTO summaryDTO;

	@BeforeStep
	public void retrieveSummaryDto(StepExecution stepExecution) {
		JobExecution jobExecution = stepExecution.getJobExecution();
		ExecutionContext jobContext = jobExecution.getExecutionContext();
		summaryDTO = (ConversionBaseSummaryDTO)jobContext.get("courseRequirementSummaryDTO");
	}

	@Override
	public GraduationCourseEntity process(GraduationCourseEntity graduationCourseEntity) throws Exception {
		return courseService.convertCourseRequirement(graduationCourseEntity, summaryDTO);
	}
}
