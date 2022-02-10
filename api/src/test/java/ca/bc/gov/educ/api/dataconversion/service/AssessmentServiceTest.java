package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.model.ConversionCourseSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.repository.assessment.AssessmentRequirementCodeRepository;
import ca.bc.gov.educ.api.dataconversion.repository.assessment.AssessmentRequirementRepository;
import ca.bc.gov.educ.api.dataconversion.service.assessment.AssessmentService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.GradConversionTestUtils;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class AssessmentServiceTest {

    @Autowired
    AssessmentService assessmentService;

    @MockBean
    AssessmentRequirementRepository assessmentRequirementRepository;

    @MockBean
    AssessmentRequirementCodeRepository assessmentRequirementCodeRepository;

    @MockBean
    RestUtils restUtils;

    @Autowired
    private EducGradDataConversionApiConstants constants;

    @Autowired
    GradConversionTestUtils gradConversionTestUtils;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {
        assessmentRequirementRepository.deleteAll();
        assessmentRequirementCodeRepository.deleteAll();
    }

    // Hard-Coded Assessment Requirements
    @Test
    public void testCreateAssessmentRequirements() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        assessmentService.createAssessmentRequirements(summary);
        assertThat(summary.getAddedCountForAssessmentRequirement()).isGreaterThan(0L);
    }

}
