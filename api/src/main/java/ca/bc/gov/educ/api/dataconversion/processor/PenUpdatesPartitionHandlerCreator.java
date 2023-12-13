package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.model.ConversionAlert;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.TraxStudentNo;
import ca.bc.gov.educ.api.dataconversion.process.DataConversionProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

public class PenUpdatesPartitionHandlerCreator extends BasePartitionHandlerCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PenUpdatesPartitionHandlerCreator.class);

    @Autowired
    DataConversionProcess dataConversionProcess;

    @Value("#{stepExecutionContext['data']}")
    List<String> partitionData;

    @Value("#{stepExecutionContext['summary']}")
    ConversionStudentSummaryDTO summaryDTO;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        LOGGER.info("=======> start partition : read count = " + partitionData.size());
        // Process partitioned data in parallel asynchronously
        partitionData.stream().forEach(pen -> {
            if (summaryDTO.getProcessedCount() % 500 == 0) {
                summaryDTO.setAccessToken(fetchAccessToken());
            }
            LOGGER.info(" ==> pen = " + pen);
            TraxStudentNo st = new TraxStudentNo();
            st.setStudNo(pen);
            try {
                dataConversionProcess.readTraxStudentAndAddNewPen(st, summaryDTO);
            } catch (Exception e) {
                ConversionAlert error = new ConversionAlert();
                error.setItem(pen);
                error.setReason("Unexpected Exception is occurred: " + e.getLocalizedMessage());
                summaryDTO.getErrors().add(error);
                LOGGER.error("unknown exception: " + e.getLocalizedMessage());
            }
        });
        LOGGER.info("=======> end partition : processed count = " + summaryDTO.getProcessedCount());

        // Aggregate summary
        aggregate(contribution, "PEN_UPDATES", "penUpdatesSummaryDTO");
        return RepeatStatus.FINISHED;
    }

    protected void aggregate(StepContribution contribution, String tableName, String summaryContextName) {
        ConversionStudentSummaryDTO totalSummaryDTO = (ConversionStudentSummaryDTO)contribution.getStepExecution().getJobExecution().getExecutionContext().get(summaryContextName);
        if (totalSummaryDTO == null) {
            totalSummaryDTO = new ConversionStudentSummaryDTO();
            totalSummaryDTO.setTableName(tableName);
            contribution.getStepExecution().getJobExecution().getExecutionContext().put(summaryContextName, totalSummaryDTO);
        }

        totalSummaryDTO.setReadCount(totalSummaryDTO.getReadCount() + summaryDTO.getReadCount());
        totalSummaryDTO.setProcessedCount(totalSummaryDTO.getProcessedCount() + summaryDTO.getProcessedCount());
        totalSummaryDTO.setAddedCount(totalSummaryDTO.getAddedCount() + summaryDTO.getAddedCount());
        totalSummaryDTO.setUpdatedCount(totalSummaryDTO.getUpdatedCount() + summaryDTO.getUpdatedCount());
        totalSummaryDTO.getErrors().addAll(summaryDTO.getErrors());

        mergeMapCounts(totalSummaryDTO.getProgramCountMap(), summaryDTO.getProgramCountMap());
        mergeMapCounts(totalSummaryDTO.getOptionalProgramCountMap(), summaryDTO.getOptionalProgramCountMap());
        mergeMapCounts(totalSummaryDTO.getCareerProgramCountMap(), summaryDTO.getCareerProgramCountMap());
    }

    private void mergeMapCounts(Map<String, Long> total, Map<String, Long> current) {
        current.forEach((k,v) -> {
            if (total.containsKey(k)) {
                total.put(k, total.get(k) + v);
            } else {
                total.put(k, v);
            }
        });
    }
}