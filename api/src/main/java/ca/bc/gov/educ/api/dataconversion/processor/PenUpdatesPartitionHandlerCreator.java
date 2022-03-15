package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.entity.trax.TraxStudentEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.ResponseObj;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

public class PenUpdatesPartitionHandlerCreator implements Tasklet {

    private static final Logger LOGGER = LoggerFactory.getLogger(PenUpdatesPartitionHandlerCreator.class);

    @Autowired
    DataConversionService dataConversionService;

    @Autowired
    RestUtils restUtils;

    @Value("#{stepExecutionContext['data']}")
    List<String> partitionData;

    @Value("#{stepExecutionContext['summary']}")
    ConversionStudentSummaryDTO summaryDTO;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        System.out.println("=======> " + Thread.currentThread().getName() + " start partition : read count = " + partitionData.size());
        // Process partitioned data in parallel asynchronously
        partitionData.stream().forEach(d -> {
            if (summaryDTO.getProcessedCount() % 500 == 0) {
                summaryDTO.setAccessToken(fetchAccessToken());
            }
            System.out.println("  ==> [" + Thread.currentThread().getName() + "] processing partitionData = " + d);
            TraxStudentEntity st = new TraxStudentEntity();
            st.setStudNo(d);
            try {
                dataConversionService.readTraxStudentAndAddNewPen(st, summaryDTO);
            } catch (Exception e) {
                LOGGER.error("unknown exception: " + e.getLocalizedMessage());
            }
        });
        System.out.println("=======> " +Thread.currentThread().getName() + " end partition : processed count = " + summaryDTO.getProcessedCount());

        // Aggregate summary
        aggregate(contribution);
        return RepeatStatus.FINISHED;
    }

    private void aggregate(StepContribution contribution) {
        ConversionStudentSummaryDTO totalSummaryDTO = (ConversionStudentSummaryDTO)contribution.getStepExecution().getJobExecution().getExecutionContext().get("penUpdatesSummaryDTO");
        if (totalSummaryDTO == null) {
            totalSummaryDTO = new ConversionStudentSummaryDTO();
            contribution.getStepExecution().getJobExecution().getExecutionContext().put("penUpdatesSummaryDTO", totalSummaryDTO);
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

    private String fetchAccessToken() {
        LOGGER.info("Fetching the access token from KeyCloak API");
        ResponseObj res = restUtils.getTokenResponseObject();
        if (res != null) {
            return res.getAccess_token();
        }
        return null;
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