package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.entity.assessment.AssessmentRequirementCodeEntity;
import ca.bc.gov.educ.api.dataconversion.entity.assessment.AssessmentRequirementEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConversionCourseSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.repository.assessment.AssessmentRequirementCodeRepository;
import ca.bc.gov.educ.api.dataconversion.repository.assessment.AssessmentRequirementRepository;
import ca.bc.gov.educ.api.dataconversion.repository.conv.EventRepository;
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

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
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
    EventRepository eventRepository;

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

    // Hard-Coded Assessment Requirements
    @Test
    public void testCreateAssessmentRequirements_whenDataAlreadyExists() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        AssessmentRequirementCodeEntity ruleCode = new AssessmentRequirementCodeEntity();
        ruleCode.setAssmtRequirementCode("116");
        ruleCode.setLabel("Test 116");
        ruleCode.setDescription("Test 116 description");
        ruleCode.setEffectiveDate(new java.sql.Date(System.currentTimeMillis() - 100000L));

        AssessmentRequirementEntity assessmentRequirementEntity = new AssessmentRequirementEntity();
        assessmentRequirementEntity.setAssessmentRequirementId(UUID.randomUUID());
        assessmentRequirementEntity.setAssessmentCode("NME");
        assessmentRequirementEntity.setRuleCode(ruleCode);

        when(this.assessmentRequirementCodeRepository.findById(ruleCode.getAssmtRequirementCode())).thenReturn(Optional.of(ruleCode));
        when(this.assessmentRequirementRepository.findByAssessmentCodeAndRuleCode(assessmentRequirementEntity.getAssessmentCode(), assessmentRequirementEntity.getRuleCode())).thenReturn(assessmentRequirementEntity);

        assessmentService.createAssessmentRequirements(summary);
        assertThat(summary.getAddedCountForAssessmentRequirement()).isGreaterThan(0L);
        assertThat(summary.getUpdatedCountForAssessmentRequirement()).isGreaterThan(0L);
    }

}
