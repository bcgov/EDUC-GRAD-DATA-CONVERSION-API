package ca.bc.gov.educ.api.dataconversion.reader;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;

import ca.bc.gov.educ.api.dataconversion.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.GraduationStatus;
import ca.bc.gov.educ.api.dataconversion.model.ResponseObj;
import ca.bc.gov.educ.api.dataconversion.rest.RestUtils;

public class RecalculateStudentReader implements ItemReader<GraduationStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecalculateStudentReader.class);
    
    private final RestUtils restUtils;
    
    private AlgorithmSummaryDTO summaryDTO;

    private int nxtStudentForProcessing;
    private List<GraduationStatus> studentList;

    public RecalculateStudentReader(RestUtils restUtils) {
        nxtStudentForProcessing = 0;
        this.restUtils = restUtils;
    }
    
    @BeforeStep
    public void initializeSummaryDto(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        summaryDTO = new AlgorithmSummaryDTO();
        jobContext.put("summaryDTO", summaryDTO);
    }

    @Override
    public GraduationStatus read() throws Exception {
        LOGGER.info("Reading the information of the next student");

        if (studentDataIsNotInitialized()) {
        	studentList = fetchStudentDataFromAPI();
        	summaryDTO.setReadCount(studentList.size());
        }

        GraduationStatus nextStudent = null;
        
        if (nxtStudentForProcessing < studentList.size()) {
            nextStudent = studentList.get(nxtStudentForProcessing);
            LOGGER.info("Found student[{}] - PEN: {} in total {}", nxtStudentForProcessing + 1, nextStudent.getPen(), summaryDTO.getReadCount());
            nxtStudentForProcessing++;
        }
        else {
        	nxtStudentForProcessing = 0;
            studentList = null;
        }
        return nextStudent;
    }

    private boolean studentDataIsNotInitialized() {
        return this.studentList == null;
    }

    private List<GraduationStatus> fetchStudentDataFromAPI() {
        LOGGER.info("Fetching Student List that need Processing");
        fetchAccessToken();			
		return restUtils.getStudentsForAlgorithm(summaryDTO.getAccessToken());
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
