package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.model.ConversionAlert;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.ConversionSummaryDTO;
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

    @Value("#{stepExecutionContext['summary']}")
    ConversionSummaryDTO conversionSummaryDTO;

    @Autowired
    DataConversionProcess dataConversionProcess;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LOGGER.info("=======> start partition : read count = {}", gradStudentTranscriptValidationPartitionData.size());
        // Process partitioned data in parallel asynchronously
        gradStudentTranscriptValidationPartitionData.forEach(gradStudentTranscriptValidation -> {
            conversionSummaryDTO.setAccessToken(fetchAccessToken());
            LOGGER.debug(" ==> GradStudentTranscriptValidation = {}", gradStudentTranscriptValidation.getStudentTranscriptValidationKey().getStudentID());
            try {
                dataConversionProcess.processStudentTranscriptValidations(gradStudentTranscriptValidation, conversionSummaryDTO);
            } catch (Exception e) {
                ConversionAlert error = new ConversionAlert();
                error.setItem(gradStudentTranscriptValidation.toString());
                error.setReason("Unexpected Exception is occurred: " + e.getLocalizedMessage());
                conversionSummaryDTO.getErrors().add(error);
                LOGGER.error("unknown exception: " + e.getLocalizedMessage());
            }
        });
        LOGGER.info("=======> end partition : processed count = " + conversionSummaryDTO.getProcessedCount());

        // Aggregate summary
        aggregate(contribution, "TRANSCRIPTS_VALIDATION", "transcriptsValidationSummaryDTO");
        return RepeatStatus.FINISHED;
    }

    protected void aggregate(StepContribution contribution, String tableName, String summaryContextName) {
        ConversionSummaryDTO totalSummaryDTO = (ConversionSummaryDTO)contribution.getStepExecution().getJobExecution().getExecutionContext().get(summaryContextName);
        if (totalSummaryDTO == null) {
            totalSummaryDTO = new ConversionStudentSummaryDTO();
            totalSummaryDTO.setTableName(tableName);
            contribution.getStepExecution().getJobExecution().getExecutionContext().put(summaryContextName, totalSummaryDTO);
        }

        totalSummaryDTO.setReadCount(totalSummaryDTO.getReadCount() + conversionSummaryDTO.getReadCount());
        totalSummaryDTO.setProcessedCount(totalSummaryDTO.getProcessedCount() + conversionSummaryDTO.getProcessedCount());
        totalSummaryDTO.setAddedCount(totalSummaryDTO.getAddedCount() + conversionSummaryDTO.getAddedCount());
        totalSummaryDTO.setUpdatedCount(totalSummaryDTO.getUpdatedCount() + conversionSummaryDTO.getUpdatedCount());
        totalSummaryDTO.getErrors().addAll(conversionSummaryDTO.getErrors());
    }
}