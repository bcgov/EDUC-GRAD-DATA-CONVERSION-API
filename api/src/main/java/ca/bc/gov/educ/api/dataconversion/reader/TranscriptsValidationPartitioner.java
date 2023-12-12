package ca.bc.gov.educ.api.dataconversion.reader;

import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.GradStudentTranscriptValidation;
import ca.bc.gov.educ.api.dataconversion.model.ResponseObj;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranscriptsValidationPartitioner extends SimplePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranscriptsValidationPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

    private final RestUtils restUtils;

    public TranscriptsValidationPartitioner(RestUtils restUtils) {
        this.restUtils = restUtils;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        ResponseObj res = restUtils.getTokenResponseObject();
        String accessToken = null;
        if (res != null) {
            accessToken = res.getAccess_token();
        }

        Integer total = restUtils.getStudentTranscriptValidationCount(accessToken);
        Integer pageSize = (total / gridSize) + 1;
        LOGGER.info("Partition setup: total number of records = {}, partition size = {}, page size = {}", total, gridSize, pageSize);

        Map<String, ExecutionContext> map = new HashMap<>(gridSize);
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext executionContext = new ExecutionContext();
            ConversionStudentSummaryDTO summaryDTO = new ConversionStudentSummaryDTO();
            List<GradStudentTranscriptValidation> data = restUtils.getStudentTranscriptValidation(i, pageSize, accessToken);
            executionContext.put("data", data);
            summaryDTO.setReadCount(data.size());
            executionContext.put("summary", summaryDTO);
            executionContext.put("index", Integer.valueOf(0));
            String key = "partition" + i;
            map.put(key, executionContext);
        }
        return map;
    }
}
