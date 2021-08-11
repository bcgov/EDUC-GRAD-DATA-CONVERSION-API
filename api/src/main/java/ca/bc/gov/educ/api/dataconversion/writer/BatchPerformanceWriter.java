package ca.bc.gov.educ.api.dataconversion.writer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;

import ca.bc.gov.educ.api.dataconversion.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.GraduationStatus;

public class BatchPerformanceWriter implements ItemWriter<GraduationStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchPerformanceWriter.class);
    
    private AlgorithmSummaryDTO summaryDTO;
    
    @BeforeStep
    public void retrieveSummaryDto(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        summaryDTO = (AlgorithmSummaryDTO)jobContext.get("summaryDTO");
    }
    
    @Override
    public void write(List<? extends GraduationStatus> list) throws Exception {
        LOGGER.info("Recording Algorithm Processed Data");
        if(!list.isEmpty()) {
	        GraduationStatus gradStatus = list.get(0);
	        summaryDTO.increment(gradStatus.getProgram());
	        LOGGER.info("Processed student[{}] - PEN: {} in total {}", summaryDTO.getProcessedCount(), gradStatus.getPen(), summaryDTO.getReadCount());
            LOGGER.info("-------------------------------------------------------");
        }
    }

}
