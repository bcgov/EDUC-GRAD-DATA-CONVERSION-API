package ca.bc.gov.educ.api.dataconversion.writer;

import ca.bc.gov.educ.api.dataconversion.entity.trax.GraduationCourseEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConversionCourseSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class DataConversionCourseRequirementWriter implements ItemWriter<GraduationCourseEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionCourseRequirementWriter.class);

    private ConversionCourseSummaryDTO summaryDTO;

    @BeforeStep
    public void retrieveSummaryDto(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        summaryDTO = (ConversionCourseSummaryDTO)jobContext.get("courseRequirementSummaryDTO");
    }
    
    @Override
    public void write(List<? extends GraduationCourseEntity> list) {
        if (!list.isEmpty()) {
            GraduationCourseEntity graduationCourseEntity = list.get(0);
            LOGGER.info("Processed course requirement: {} in total {}", summaryDTO.getProcessedCount(), summaryDTO.getReadCount());
            LOGGER.info("-------------------------------------------------------");
        }
    }
}
