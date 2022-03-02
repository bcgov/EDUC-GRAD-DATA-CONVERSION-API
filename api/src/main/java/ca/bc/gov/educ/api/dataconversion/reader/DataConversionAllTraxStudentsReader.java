package ca.bc.gov.educ.api.dataconversion.reader;

import ca.bc.gov.educ.api.dataconversion.entity.trax.TraxStudentEntity;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public class DataConversionAllTraxStudentsReader implements ItemReader<TraxStudentEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionAllTraxStudentsReader.class);
    private static final int PAGE_SIZE = 1000;

    private final DataConversionService dataConversionService;
    private final RestUtils restUtils;

    private int indexForStudent;
    private int page;
    private List<TraxStudentEntity> studentList;
    private ConversionStudentSummaryDTO summaryDTO;

    public DataConversionAllTraxStudentsReader(DataConversionService dataConversionService, RestUtils restUtils) {
        this.dataConversionService = dataConversionService;
        this.restUtils = restUtils;

        indexForStudent = 0;
        page = 0;
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
    public TraxStudentEntity read() {
        LOGGER.debug("Reading the information of the next student");

        if (indexForStudent % PAGE_SIZE == 0) {
            // next page
            Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("studNo").ascending());
            studentList = loadAllTraxStudents(pageable);
            summaryDTO.setReadCount(summaryDTO.getReadCount() + studentList.size());

            page++;
            indexForStudent = 0;
            fetchAccessToken();
        }

        TraxStudentEntity nextStudent = null;
        if (indexForStudent < studentList.size()) {
            nextStudent = studentList.get(indexForStudent);
            indexForStudent++;
            LOGGER.debug("Found student[{}] - PEN: {} in total {}",((page - 1)*PAGE_SIZE) + indexForStudent, nextStudent.getStudNo(), summaryDTO.getReadCount());
        } else {
            indexForStudent = 0;
            studentList = null;
        }
        return nextStudent;
    }

    private List<TraxStudentEntity> loadAllTraxStudents(Pageable pageable) {
        LOGGER.info("Fetching Student List that need Add Missing Students Processing - page:{}, size:{}", pageable.getPageNumber(), pageable.getPageSize());
        return dataConversionService.loadAllTraxStudentsForPenUpdate(pageable);
    }

    private void fetchAccessToken() {
        LOGGER.debug("Fetching the access token from KeyCloak API");
        ResponseObj res = restUtils.getTokenResponseObject();
        if (res != null) {
            summaryDTO.setAccessToken(res.getAccess_token());
            LOGGER.debug("Setting the new access token in summaryDTO.");
        }
    }
}
