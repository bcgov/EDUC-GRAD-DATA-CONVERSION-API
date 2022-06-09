package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.AssessmentRequirement;
import ca.bc.gov.educ.api.dataconversion.model.AssessmentRequirementCode;
import ca.bc.gov.educ.api.dataconversion.model.ConversionCourseSummaryDTO;
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

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class AssessmentServiceTest {

    @Autowired
    AssessmentService assessmentService;

    @MockBean
    EventRepository eventRepository;

    @MockBean
    RestUtils restUtils;

    @Autowired
    private EducGradDataConversionApiConstants constants;

    @Autowired
    GradConversionTestUtils gradConversionTestUtils;

    // NATS
    @MockBean
    private NatsConnection natsConnection;
    @MockBean
    private Subscriber subscriber;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {
    }

    // Hard-Coded Assessment Requirements
    @Test
    public void testCreateAssessmentRequirements() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        AssessmentRequirementCode ruleCode = new AssessmentRequirementCode();
        ruleCode.setAssmtRequirementCode("116");
        ruleCode.setLabel("Test 116");
        ruleCode.setDescription("Test 116 description");
        ruleCode.setEffectiveDate(new java.sql.Date(System.currentTimeMillis() - 100000L));

        AssessmentRequirement assessmentRequirement1 = new AssessmentRequirement();
        assessmentRequirement1.setAssessmentRequirementId(UUID.randomUUID());
        assessmentRequirement1.setAssessmentCode("NME");
        assessmentRequirement1.setRuleCode(ruleCode);

        AssessmentRequirement assessmentRequirement2 = new AssessmentRequirement();
        assessmentRequirement2.setAssessmentRequirementId(UUID.randomUUID());
        assessmentRequirement2.setAssessmentCode("NME10");
        assessmentRequirement2.setRuleCode(ruleCode);


        when(this.restUtils.addAssessmentRequirement(assessmentRequirement1, "123")).thenReturn(assessmentRequirement1);
        when(this.restUtils.addAssessmentRequirement(assessmentRequirement2, "123")).thenReturn(assessmentRequirement2);

        assessmentService.createAssessmentRequirements(summary);
    }

}
