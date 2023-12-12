package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.model.ConversionAlert;
import ca.bc.gov.educ.api.dataconversion.model.GradStudentTranscriptValidation;
import ca.bc.gov.educ.api.dataconversion.process.DataConversionProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class TranscriptsValidationPartitionHandlerCreator extends BasePartitionHandlerCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranscriptsValidationPartitionHandlerCreator.class);

    @Value("#{stepExecutionContext['data']}")
    List<GradStudentTranscriptValidation> gradStudentTranscriptValidationPartitionData;

    @Autowired
    DataConversionProcess dataConversionProcess;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LOGGER.info("=======> start partition : read count = {}", partitionData.size());
        // Process partitioned data in parallel asynchronously
        gradStudentTranscriptValidationPartitionData.forEach(gradStudentTranscriptValidation -> {
            if (summaryDTO.getProcessedCount() % 500 == 0) {
                summaryDTO.setAccessToken(fetchAccessToken());
            }
            LOGGER.info(" ==> GradStudentTranscriptValidation = " + gradStudentTranscriptValidation);
            try {
                dataConversionProcess.processStudentTranscriptValidations(gradStudentTranscriptValidation, summaryDTO);
            } catch (Exception e) {
                ConversionAlert error = new ConversionAlert();
                error.setItem(gradStudentTranscriptValidation.toString());
                error.setReason("Unexpected Exception is occurred: " + e.getLocalizedMessage());
                summaryDTO.getErrors().add(error);
                LOGGER.error("unknown exception: " + e.getLocalizedMessage());
            }
        });
        LOGGER.info("=======> end partition : processed count = " + summaryDTO.getProcessedCount());

        // Aggregate summary
        aggregate(contribution, "TRANSCRIPTS_VALIDATION", "transcriptsValidationSummaryDTO");
        return RepeatStatus.FINISHED;
    }
}