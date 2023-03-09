package ca.bc.gov.educ.api.dataconversion.reader;

import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.ResponseObj;
import ca.bc.gov.educ.api.dataconversion.model.TraxStudentNo;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StudentLoadPartitioner extends SimplePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentLoadPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

    private final RestUtils restUtils;

    public StudentLoadPartitioner(RestUtils restUtils) {
        this.restUtils = restUtils;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        ResponseObj res = restUtils.getTokenResponseObject();
        String accessToken = null;
        if (res != null) {
            accessToken = res.getAccess_token();
        }

        String reload = jobExecution.getJobParameters().getString("reload");
        Integer total = restUtils.getTotalNumberOfTraxStudentNoList(accessToken);
        Integer pageSize = (total / gridSize) + 1;
        LOGGER.info("Partition setup: total number of records = {}, partition size = {}, page size = {}", total, gridSize, pageSize);

        Map<String, ExecutionContext> map = new HashMap<>(gridSize);
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext executionContext = new ExecutionContext();
            ConversionStudentSummaryDTO summaryDTO = new ConversionStudentSummaryDTO();
            List<String> data = loadPenNumbers(i, pageSize, accessToken);
            executionContext.put("data", data);
            summaryDTO.setReadCount(data.size());
            executionContext.put("summary", summaryDTO);
            executionContext.put("index", Integer.valueOf(0));
            executionContext.put("reload", reload);
            String key = "partition" + i;
            map.put(key, executionContext);
        }
        return map;
    }

    private List<String> loadPenNumbers(int pageNumber, int pageSize, String accessToken) {
        List<TraxStudentNo> list = restUtils.getTraxStudentNoListByPage(pageNumber, pageSize, accessToken);
        if (list != null && !list.isEmpty()) {
            return list.stream().map(r -> r.getStudNo()).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
