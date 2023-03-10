package ca.bc.gov.educ.api.dataconversion.writer;

import ca.bc.gov.educ.api.dataconversion.model.ConversionCourseSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.CourseRestriction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class DataConversionCourseRestrictionWriter implements ItemWriter<CourseRestriction> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionCourseRestrictionWriter.class);

    private ConversionCourseSummaryDTO summaryDTO;

    @BeforeStep
    public void retrieveSummaryDto(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        summaryDTO = (ConversionCourseSummaryDTO)jobContext.get("courseRestrictionSummaryDTO");
    }

    @Override
    public void write(Chunk<? extends CourseRestriction> chunk) throws Exception {
        if (!chunk.isEmpty()) {
            CourseRestriction courseRestriction = chunk.getItems().get(0);
            LOGGER.info("Processed course restriction: {} in total {}", summaryDTO.getProcessedCount(), summaryDTO.getReadCount());
            LOGGER.info("-------------------------------------------------------");
        }
    }
}
