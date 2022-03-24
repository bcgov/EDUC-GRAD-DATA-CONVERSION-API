package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.entity.trax.TraxStudentEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConversionAlert;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

public class PenUpdatesPartitionHandlerCreator extends BasePartitionHandlerCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PenUpdatesPartitionHandlerCreator.class);

    @Autowired
    DataConversionService dataConversionService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        LOGGER.info("=======> start partition : read count = " + partitionData.size());
        // Process partitioned data in parallel asynchronously
        partitionData.stream().forEach(pen -> {
            if (summaryDTO.getProcessedCount() % 500 == 0) {
                summaryDTO.setAccessToken(fetchAccessToken());
            }
            LOGGER.info(" ==> pen = " + pen);
            TraxStudentEntity st = new TraxStudentEntity();
            st.setStudNo(pen);
            try {
                dataConversionService.readTraxStudentAndAddNewPen(st, summaryDTO);
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
}