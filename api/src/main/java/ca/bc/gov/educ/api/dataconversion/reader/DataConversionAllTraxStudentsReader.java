package ca.bc.gov.educ.api.dataconversion.reader;

import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.ResponseObj;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;

import java.util.List;

public class DataConversionAllTraxStudentsReader implements ItemReader<ConvGradStudent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionStudentReader.class);

    private final DataConversionService dataConversionService;
    private final RestUtils restUtils;

    private int indexForStudent;
    private List<ConvGradStudent> studentList;
    private ConversionStudentSummaryDTO summaryDTO;

    public DataConversionAllTraxStudentsReader(DataConversionService dataConversionService, RestUtils restUtils) {
        this.dataConversionService = dataConversionService;
        this.restUtils = restUtils;

        indexForStudent = 1;
    }

    @BeforeStep
    public void initializeSummaryDto(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        summaryDTO = new ConversionStudentSummaryDTO();
        summaryDTO.setTableName("GRAD_STUDENT");
        jobContext.put("studentSummaryDTO", summaryDTO);
    }

    @Override
    public ConvGradStudent read() {
        LOGGER.info("Reading the information of the next student");

        if (studentDataIsNotInitialized()) {
            studentList = loadAllTraxStudents();
            summaryDTO.setReadCount(studentList.size());
        }

        if (indexForStudent % 1000 == 0) {
            fetchAccessToken();
        }

        ConvGradStudent nextStudent = null;

        if (indexForStudent <= studentList.size()) {
            nextStudent = studentList.get(indexForStudent);
            indexForStudent++;
            LOGGER.info("Found student[{}] - PEN: {} in total {}", indexForStudent, nextStudent.getPen(), summaryDTO.getReadCount());
        }
        else {
            indexForStudent = 1;
            studentList = null;
        }
        return nextStudent;
    }

    private boolean studentDataIsNotInitialized() {
        return this.studentList == null;
    }

    private List<ConvGradStudent> loadAllTraxStudents() {
        LOGGER.info("Fetching Student List that need Add Missing Students Processing");
        return dataConversionService.loadAllTraxStudentsForPenUpdate();
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
