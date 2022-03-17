package ca.bc.gov.educ.api.dataconversion.processor;

import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.ResponseObj;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

public abstract class BasePartitionHandlerCreator implements Tasklet {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePartitionHandlerCreator.class);

    @Autowired
    RestUtils restUtils;

    @Value("#{stepExecutionContext['data']}")
    List<String> partitionData;

    @Value("#{stepExecutionContext['summary']}")
    ConversionStudentSummaryDTO summaryDTO;

    protected void aggregate(StepContribution contribution, String tableName, String summaryContextName) {
        ConversionStudentSummaryDTO totalSummaryDTO = (ConversionStudentSummaryDTO)contribution.getStepExecution().getJobExecution().getExecutionContext().get("penUpdatesSummaryDTO");
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

    protected String fetchAccessToken() {
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