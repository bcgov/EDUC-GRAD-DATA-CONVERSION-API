package ca.bc.gov.educ.api.dataconversion.reader;

import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.ResponseObj;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;

import java.util.ArrayList;
import java.util.List;

public class DataConversionStudentReader implements ItemReader<ConvGradStudent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionStudentReader.class);

    private final RestUtils restUtils;

    private int indexForStudent;
    private List<ConvGradStudent> studentList;
    private ConversionStudentSummaryDTO summaryDTO;

    public DataConversionStudentReader(RestUtils restUtils) {
        this.restUtils = restUtils;

        indexForStudent = 0;
    }

    @BeforeStep
    public void initializeSummaryDto(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        summaryDTO = new ConversionStudentSummaryDTO();
        summaryDTO.setTableName("GRAD_STUDENT");
        jobContext.put("studentSummaryDTO", summaryDTO);

        // initialize
        studentList = null;
        indexForStudent = 0;
    }

    @Override
    public ConvGradStudent read() {
        LOGGER.info("Reading the information of the next student");

        if (indexForStudent % 100 == 0) {
            fetchAccessToken();
        }

        if (studentDataIsNotInitialized()) {
        	studentList = loadRawStudentData(summaryDTO.getAccessToken());
        	summaryDTO.setReadCount(studentList.size());
        }

        ConvGradStudent nextStudent = null;
        
        if (indexForStudent < studentList.size()) {
            nextStudent = studentList.get(indexForStudent);
            indexForStudent++;
            LOGGER.info("Found student[{}] - PEN: {} in total {}", indexForStudent, nextStudent.getPen(), summaryDTO.getReadCount());
        }
        else {
        	indexForStudent = 0;
            studentList = null;
        }
        return nextStudent;
    }

    private boolean studentDataIsNotInitialized() {
        return this.studentList == null;
    }

    private List<ConvGradStudent> loadRawStudentData(String accessToken) {
        LOGGER.info("Fetching Student List that need Data Conversion Processing");
//        return restUtils.getTraxStudentMasterDataByPen(accessToken);
        return new ArrayList<>();
    }

    private void fetchAccessToken() {
        LOGGER.info("Fetching the access token from KeyCloak API");
        ResponseObj res = restUtils.getTokenResponseObject();
        if (res != null) {
            summaryDTO.setAccessToken(res.getAccess_token());
            LOGGER.info("Setting the new access token in summaryDTO.");
        }
    }
}
