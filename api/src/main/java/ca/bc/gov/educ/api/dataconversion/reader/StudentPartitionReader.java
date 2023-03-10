package ca.bc.gov.educ.api.dataconversion.reader;

import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.ResponseObj;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

public class StudentPartitionReader implements ItemReader<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentPartitionReader.class);

    private final RestUtils restUtils;

    @Value("#{stepExecutionContext['index']}")
    private int indexForStudent;

    @Value("#{stepExecutionContext['data']}")
    private List<String> studentList;

    @Value("#{stepExecutionContext['summary']}")
    private ConversionStudentSummaryDTO summaryDTO;

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

    public StudentPartitionReader(RestUtils restUtils) {
        this.restUtils = restUtils;
    }

    public void aggregate() {
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        ConversionStudentSummaryDTO totalSummaryDTO = (ConversionStudentSummaryDTO)jobContext.get("studentSummaryDTO");
        if (totalSummaryDTO == null) {
            totalSummaryDTO = new ConversionStudentSummaryDTO();
            totalSummaryDTO.setTableName("LOAD_STUDENT");
            jobContext.put("studentSummaryDTO", totalSummaryDTO);
        }

        // merge
        totalSummaryDTO.setReadCount(totalSummaryDTO.getReadCount() + summaryDTO.getReadCount());
        totalSummaryDTO.setProcessedCount(totalSummaryDTO.getProcessedCount() + summaryDTO.getProcessedCount());
        totalSummaryDTO.setAddedCount(totalSummaryDTO.getAddedCount() + summaryDTO.getAddedCount());
        totalSummaryDTO.setUpdatedCount(totalSummaryDTO.getUpdatedCount() + summaryDTO.getUpdatedCount());
        totalSummaryDTO.setErroredCount(totalSummaryDTO.getErroredCount() + summaryDTO.getErroredCount());

        mergeMapCounts(totalSummaryDTO.getProgramCountMap(), summaryDTO.getProgramCountMap());
        mergeMapCounts(totalSummaryDTO.getOptionalProgramCountMap(), summaryDTO.getOptionalProgramCountMap());
        mergeMapCounts(totalSummaryDTO.getCareerProgramCountMap(), summaryDTO.getCareerProgramCountMap());
    }

    @Override
    public String read() {
        LOGGER.debug("Reading the information of the next student");

        fetchAccessToken();
        String nextStudent = null;

        if (indexForStudent < studentList.size()) {
            nextStudent = studentList.get(indexForStudent);
            indexForStudent++;
            LOGGER.debug("Found student[{}] - PEN: {} in total {}", indexForStudent, nextStudent, summaryDTO.getReadCount());
        } else {
            aggregate();
        }
        return nextStudent;
    }

    private void fetchAccessToken() {
        ResponseObj res = restUtils.getTokenResponseObject();
        if (res != null) {
            summaryDTO.setAccessToken(res.getAccess_token());
            LOGGER.debug("Setting the new access token in summaryDTO.");
        }
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
