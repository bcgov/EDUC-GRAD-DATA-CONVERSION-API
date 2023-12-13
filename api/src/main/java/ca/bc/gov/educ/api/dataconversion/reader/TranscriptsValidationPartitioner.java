package ca.bc.gov.educ.api.dataconversion.reader;

import ca.bc.gov.educ.api.dataconversion.model.ConversionSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.GradStudentTranscriptValidation;
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

    private static final int GRID_SIZE = 1000;

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

    private final RestUtils restUtils;

    public TranscriptsValidationPartitioner(RestUtils restUtils) {
        this.restUtils = restUtils;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Integer total = restUtils.getStudentTranscriptValidationCount(restUtils.getAccessToken());
        gridSize = GRID_SIZE;
        Integer pageSize = 5;//(total / gridSize) + 1;
        LOGGER.info("Partition setup: total number of records = {}, partition size = {}, page size = {}", total, gridSize, pageSize);

        Map<String, ExecutionContext> map = new HashMap<>(gridSize);
        for (int i = 0; i < 1; i++) {
            ExecutionContext executionContext = new ExecutionContext();
            ConversionSummaryDTO summaryDTO = new ConversionSummaryDTO();
            List<GradStudentTranscriptValidation> data = restUtils.getStudentTranscriptValidation(i, pageSize, restUtils.getAccessToken());
            executionContext.put("data", data);
            summaryDTO.setReadCount(data.size());
            summaryDTO.setBatchId(jobExecution.getId());
            executionContext.put("summary", summaryDTO);
            executionContext.put("index", i);
            String key = "partition" + i;
            map.put(key, executionContext);
        }
        return map;
    }
}
