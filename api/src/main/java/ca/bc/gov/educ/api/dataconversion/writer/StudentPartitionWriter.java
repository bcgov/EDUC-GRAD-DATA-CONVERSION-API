package ca.bc.gov.educ.api.dataconversion.writer;

import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class StudentPartitionWriter implements ItemWriter<ConvGradStudent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentPartitionWriter.class);

    @Value("#{stepExecutionContext['summary']}")
    private ConversionStudentSummaryDTO summaryDTO;

    @Override
    public void write(List<? extends ConvGradStudent> list) {
        if (!list.isEmpty()) {
            ConvGradStudent gradStudent = list.get(0);
            LOGGER.info("Processed student[{}] - PEN: {} in total {}", summaryDTO.getProcessedCount(), gradStudent.getPen(), summaryDTO.getReadCount());
        }
    }
}
