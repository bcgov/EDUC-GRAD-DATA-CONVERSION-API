package ca.bc.gov.educ.api.dataconversion.reader;

import ca.bc.gov.educ.api.dataconversion.model.ConversionSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.GradStudentTranscriptValidation;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

public class TranscriptsValidationPartitioner extends SimplePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranscriptsValidationPartitioner.class);

    private static final int PAGE_SIZE = 1500;
    private static final String NUMBER_OF_RECORDS_TO_PROCEED ="numberOfRecords";

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

    private final RestUtils restUtils;

    public TranscriptsValidationPartitioner(RestUtils restUtils) {
        this.restUtils = restUtils;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        JobParameters jobParameters = jobExecution.getJobParameters();
        Integer total = jobParameters.getLong(NUMBER_OF_RECORDS_TO_PROCEED, 0L).intValue();
        if(total == 0) {
            total = restUtils.getStudentTranscriptValidationCount(restUtils.getAccessToken());
        }
        LOGGER.info("Partition setup: total number of records = {}, partition size = {}, page size = {}", total, gridSize, PAGE_SIZE);
        Map<String, ExecutionContext> map = new HashMap<>(gridSize);
        List<Integer> range = IntStream.range(0, gridSize).boxed().toList();
        List<CompletableFuture<Map<String, ExecutionContext>>> futures = range.stream()
                .map(i -> CompletableFuture.supplyAsync(() -> populatePartitions(i)))
                .toList();
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        CompletableFuture<List<Map<String, ExecutionContext>>> result = allFutures.thenApply(v -> futures.stream()
                .map(CompletableFuture::join).toList());
        List<Map<String, ExecutionContext>> finalList = result.join();
        for(Map<String, ExecutionContext> m: finalList) {
            map.putAll(m);
        }
        return map;
    }

    private Map<String, ExecutionContext> populatePartitions(Integer pageNumber) {
        Map<String, ExecutionContext> map = new HashMap<>();
        ExecutionContext executionContext = new ExecutionContext();
        ConversionSummaryDTO summaryDTO = new ConversionSummaryDTO();
        List<GradStudentTranscriptValidation> data = restUtils.getStudentTranscriptValidation(pageNumber, PAGE_SIZE, restUtils.getAccessToken());
        executionContext.put("data", data);
        summaryDTO.setReadCount(data.size());
        summaryDTO.setBatchId(jobExecution.getId());
        executionContext.put("summary", summaryDTO);
        executionContext.put("index", pageNumber);
        String key = "partition" + pageNumber;
        map.put(key, executionContext);
        return map;
    }
}
