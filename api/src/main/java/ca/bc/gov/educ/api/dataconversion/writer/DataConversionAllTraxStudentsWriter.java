package ca.bc.gov.educ.api.dataconversion.writer;

import ca.bc.gov.educ.api.dataconversion.entity.trax.TraxStudentEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class DataConversionAllTraxStudentsWriter implements ItemWriter<TraxStudentEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionAllTraxStudentsWriter.class);

    private ConversionStudentSummaryDTO summaryDTO;

    @BeforeStep
    public void retrieveSummaryDto(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        summaryDTO = (ConversionStudentSummaryDTO)jobContext.get("studentSummaryDTO");
    }
    
    @Override
    public void write(List<? extends TraxStudentEntity> list) {
        if (!list.isEmpty()) {
            TraxStudentEntity gradStudent = list.get(0);
            LOGGER.info("Processed student[{}] - PEN: {} in total {}", summaryDTO.getProcessedCount(), gradStudent.getStudNo(), summaryDTO.getReadCount());
        }
    }
}
