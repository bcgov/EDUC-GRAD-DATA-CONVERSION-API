package ca.bc.gov.educ.api.dataconversion.reader;

import ca.bc.gov.educ.api.dataconversion.entity.conv.GraduationCourseEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConversionBaseSummaryDTO;
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

public class DataConversionCourseRequirementReader implements ItemReader<GraduationCourseEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionCourseRequirementReader.class);

    private final DataConversionService dataConversionService;
    private final RestUtils restUtils;

    private int indexForCourseRequirement;
    private List<GraduationCourseEntity> courseRequirementList;
    private ConversionBaseSummaryDTO summaryDTO;

    public DataConversionCourseRequirementReader(DataConversionService dataConversionService, RestUtils restUtils) {
        this.dataConversionService = dataConversionService;
        this.restUtils = restUtils;

        indexForCourseRequirement = 0;
    }

    @BeforeStep
    public void initializeSummaryDto(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        summaryDTO = new ConversionBaseSummaryDTO();
        summaryDTO.setTableName("COURSE_REQUIREMENT");
        jobContext.put("courseRequirementSummaryDTO", summaryDTO);
    }

    @Override
    public GraduationCourseEntity read() {
        LOGGER.info("Reading the information of the next course requirement");

        if (courseRequirementDataIsNotInitialized()) {
            courseRequirementList = loadCourseRequirementData();
        	summaryDTO.setReadCount(courseRequirementList.size());
        }

        if (indexForCourseRequirement % 300 == 0) {
            fetchAccessToken();
        }

        GraduationCourseEntity nextCourseRequirement = null;
        
        if (indexForCourseRequirement < courseRequirementList.size()) {
            nextCourseRequirement = courseRequirementList.get(indexForCourseRequirement);
            indexForCourseRequirement++;
            LOGGER.info("Found course requirement: {} in total {}", indexForCourseRequirement, summaryDTO.getReadCount());
        }
        else {
            indexForCourseRequirement = 0;
            courseRequirementList = null;
        }
        return nextCourseRequirement;
    }

    private boolean courseRequirementDataIsNotInitialized() {
        return this.courseRequirementList == null;
    }

    private List<GraduationCourseEntity> loadCourseRequirementData() {
        LOGGER.info("Fetching Course Requirement List that need Data Conversion Processing");
        return dataConversionService.loadInitialGradCourseRequirementsData();
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
