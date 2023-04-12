package ca.bc.gov.educ.api.dataconversion.process;

import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.EventRepository;
import ca.bc.gov.educ.api.dataconversion.process.CourseProcess;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class CourseProcessTest {

    @Autowired
    CourseProcess courseProcess;

    @MockBean
    EventRepository eventRepository;

    @MockBean
    RestUtils restUtils;

    @MockBean
    WebClient webClient;

    @Autowired
    private EducGradDataConversionApiConstants constants;

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

    @Test
    public void testConvertCourseRestriction() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        CourseRestriction courseRestriction = new CourseRestriction(
               null, "main", "12", "rest", "12", null, null
        );

        CourseRestriction savedCourseRestriction = new CourseRestriction();
        savedCourseRestriction.setCourseRestrictionId(UUID.randomUUID());
        savedCourseRestriction.setMainCourse("main");
        savedCourseRestriction.setMainCourseLevel("12");
        savedCourseRestriction.setRestrictedCourse("rest");
        savedCourseRestriction.setRestrictedCourseLevel("12");

        when(this.restUtils.getCourseRestriction("main", "12", "rest", "12", "123")).thenReturn(null);
        when(this.restUtils.saveCourseRestriction(courseRestriction, "123")).thenReturn(savedCourseRestriction);

        courseProcess.convertCourseRestriction(courseRestriction, summary);
        assertThat(summary.getAddedCountForCourseRestriction()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRestriction_whenGivenRecordExists() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        CourseRestriction courseRestriction = new CourseRestriction(
                null, "main", "12", "rest", "12", null, null
        );

        CourseRestriction savedCourseRestriction = new CourseRestriction();
        savedCourseRestriction.setCourseRestrictionId(UUID.randomUUID());
        savedCourseRestriction.setMainCourse("main");
        savedCourseRestriction.setMainCourseLevel("12");
        savedCourseRestriction.setRestrictedCourse("rest");
        savedCourseRestriction.setRestrictedCourseLevel("12");

        when(this.restUtils.getCourseRestriction("main", "12", "rest", "12", "123")).thenReturn(savedCourseRestriction);
        when(this.restUtils.saveCourseRestriction(courseRestriction, "123")).thenReturn(savedCourseRestriction);

        courseProcess.convertCourseRestriction(courseRestriction, summary);
        assertThat(summary.getUpdatedCountForCourseRestriction()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRestriction_whenInvalidCourseIsProvided_throwsError() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        CourseRestriction courseRestriction = new CourseRestriction(
                null, "CLEA", "12", "CLEB", "12", null, null
        );

        courseProcess.convertCourseRestriction(courseRestriction, summary);
        assertThat(summary.getErrors()).hasSize(1);
    }

    // English 10
    @Test
    public void testConvertCourseRequirement_whenDataExists_forEnglish10_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "ENG", "10", "101", null,true);
        traxCourse.setEnglish10("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "10", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "10", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getUpdatedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish10_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "ENG", "10", "101", null, false);
        traxCourse.setEnglish10("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "10", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "10", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish10_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "ENG", "10", "701", null,false);
        traxCourse.setEnglish10("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "10", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "10", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish10_2018_withFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "ENG", "10", "302", "F",false);
        traxCourse.setEnglish10("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "10", "123")).thenReturn(true);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "10", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish10_2004_withFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "ENG", "10", "815", "F",false);
        traxCourse.setEnglish10("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "10", "123")).thenReturn(true);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "10", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish10_2018_withBlankLanguage() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "ENG", "10", "400", "F",false);
        traxCourse.setEnglish10("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "10", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "10", "123")).thenReturn(true);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish10_2004_withBlankLanguage() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "ENG", "10", "850", "F",false);
        traxCourse.setEnglish10("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "10", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "10", "123")).thenReturn(true);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    // English 11
    @Test
    public void testConvertCourseRequirement_whenDataExists_forEnglish11_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "ENG", "11", "102", null,true);
        traxCourse.setEnglish11("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "11", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "11", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getUpdatedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "ENG", "11", "102", null, false);
        traxCourse.setEnglish11("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "11", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "11", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "ENG", "11", "702", null,false);
        traxCourse.setEnglish11("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "11", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "11", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1996", "ENG", "11", "721", null,false);
        traxCourse.setEnglish11("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "11", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "11", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_1986() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1986", "ENG", "11", "740", null,false);
        traxCourse.setEnglish11("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "11", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "11", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_2018_withFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "ENG", "11", "301", "F",false);
        traxCourse.setEnglish11("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "11", "123")).thenReturn(true);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "11", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_2004_withFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "ENG", "11", "816", "F",false);
        traxCourse.setEnglish11("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "11", "123")).thenReturn(true);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "11", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_1996_withFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1996", "ENG", "11", "818", "F",false);
        traxCourse.setEnglish11("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "11", "123")).thenReturn(true);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "11", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_2018_withBlankLanguage() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "ENG", "11", "401", "F",false);
        traxCourse.setEnglish11("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "11", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "11", "123")).thenReturn(true);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_2004_withBlankLanguage() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "ENG", "11", "851", "F",false);
        traxCourse.setEnglish11("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "11", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "11", "123")).thenReturn(true);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    // English 12
    @Test
    public void testConvertCourseRequirement_whenDataExists_forEnglish12_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "ENG", "12", "103", null,true);
        traxCourse.setEnglish12("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "12", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "12", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getUpdatedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "ENG", "12", "103", null, false);
        traxCourse.setEnglish12("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "12", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "12", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "ENG", "12", "703", null,false);
        traxCourse.setEnglish12("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "12", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "12", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_1950() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1950", "ENG", "12", "500", null,false);
        traxCourse.setEnglish12("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "12", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "12", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1996", "ENG", "12", "722", null,false);
        traxCourse.setEnglish12("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "12", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "12", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_1986() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1986", "ENG", "12", "741", null,false);
        traxCourse.setEnglish12("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "12", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "12", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_2018_withFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "ENG", "12", "300", "F",false);
        traxCourse.setEnglish12("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "12", "123")).thenReturn(true);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "12", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_2004_withFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "ENG", "12", "817", "F",false);
        traxCourse.setEnglish12("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "12", "123")).thenReturn(true);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "12", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_1996_withFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1996", "ENG", "12", "819", "F",false);
        traxCourse.setEnglish12("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "12", "123")).thenReturn(true);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "12", "123")).thenReturn(false);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_2018_withBlankLanguage() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "ENG", "12", "402", "F",false);
        traxCourse.setEnglish12("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "12", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "12", "123")).thenReturn(true);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_2004_withBlankLanguage() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "ENG", "12", "852", "F",false);
        traxCourse.setEnglish12("Y");

        when(this.restUtils.checkFrenchLanguageCourse("ENG", "12", "123")).thenReturn(false);
        when(this.restUtils.checkBlankLanguageCourse("ENG", "12", "123")).thenReturn(true);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    // Socials
    @Test
    public void testConvertCourseRequirement_forSocials10_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "SOC", "10", "104", null,false);
        traxCourse.setSocials10("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forSocials10_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "SOC", "10", "704", null,false);
        traxCourse.setSocials10("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forSocials_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "SOC", "11", "105", null,false);
        traxCourse.setSocials("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forSocials_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "SOC", "11", "705", null,false);
        traxCourse.setSocials("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forSocials_1950() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1950", "SOC", "11", "502", null,false);
        traxCourse.setSocials("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forSocials_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1996", "SOC", "11", "723", null,false);
        traxCourse.setSocials("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forSocials_1986() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1986", "SOC", "11", "742", null,false);
        traxCourse.setSocials("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // Math
    @Test
    public void testConvertCourseRequirement_forMath10_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "MAT", "10", "106", null,false);
        traxCourse.setMath10("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forMath10_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "MAT", "10", "706", null,false);
        traxCourse.setMath10("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forMath_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "MAT", "11", "107", null,false);
        traxCourse.setMath("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forMath_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "MAT", "11", "707", null,false);
        traxCourse.setMath("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forMath_1950() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1950", "MAT", "11", "501", null,false);
        traxCourse.setMath("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forMath_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1996", "MAT", "11", "724", null,false);
        traxCourse.setMath("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forMath_1986() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1986", "MAT", "11", "743", null,false);
        traxCourse.setMath("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // Science
    @Test
    public void testConvertCourseRequirement_forScience10_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "PHY", "10", "108", null,false);
        traxCourse.setScience10("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forScience10_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "PHY", "10", "708", null,false);
        traxCourse.setScience10("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forScience_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "PHY", "11", "109", null,false);
        traxCourse.setScience("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forScience_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "PHY", "11", "709", null,false);
        traxCourse.setScience("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forScience_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1996", "PHY", "11", "725", null,false);
        traxCourse.setScience("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forScience_1986() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1986", "PHY", "11", "744", null,false);
        traxCourse.setScience("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // Career Personal
    @Test
    public void testConvertCourseRequirement_forCareerPersonal10_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "CPP", "10", "112", null,false);
        traxCourse.setCareerPersonal10("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forCareerPersonal10_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "CPP", "10", "710", null,false);
        traxCourse.setCareerPersonal10("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forCareerPersonal11_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1996", "CPP", "11", "728", null,false);
        traxCourse.setCareerPersonal11("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forCareerPersonal12_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1996", "CPP", "12", "729", null,false);
        traxCourse.setCareerPersonal12("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // PhysEd
    @Test
    public void testConvertCourseRequirement_forPhysEd10_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "PHE", "10", "110", null,false);
        traxCourse.setPhysEd10("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forPhysEd10_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "PHE", "10", "711", null,false);
        traxCourse.setPhysEd10("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // Applied Skills
    @Test
    public void testConvertCourseRequirement_forAppliedSkills_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "ASK", "10", "111", null,false);
        traxCourse.setAppliedSkills("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forAppliedSkills_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "ASK", "10", "712", null,false);
        traxCourse.setAppliedSkills("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forAppliedSkills_1996_withFineArts() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1996", "ASK", "10", "732", null,false);
        traxCourse.setAppliedSkills("Y");
        traxCourse.setFineArts("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forAppliedSkills_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1996", "ASK", "10", "727", null,false);
        traxCourse.setAppliedSkills("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // PortFolio
    @Test
    public void testConvertCourseRequirement_forPortFolio_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2004", "PFL", "11", "713", null,false);
        traxCourse.setPortfolio("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // ConsEd
    @Test
    public void testConvertCourseRequirement_forConsEd_1986() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1986", "CED", "11", "745", null,false);
        traxCourse.setConsEd("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // Career Life Connections
    @Test
    public void testConvertCourseRequirement_forCareerLifeConnections_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "CLC", "11", "113", null,false);
        traxCourse.setCareerLifeConnections("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // Fine Arts
    @Test
    public void testConvertCourseRequirement_forFineArts_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1996", "ART", "11", "726", null,false);
        traxCourse.setFineArts("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // Fine Arts
    @Test
    public void testConvertCourseRequirement_inRemovalList_forFrenchLanguage_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("2018", "LTSTF", "10", "302", null,false);
        traxCourse.setEnglish10("Y");

        when(this.restUtils.checkFrenchLanguageCourse("LTSTF", "10", "123")).thenReturn(true);

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // Hard-Coded Course Requirements
    @Test
    public void testCreateCourseRequirementsForFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        courseProcess.createCourseRequirements(summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isGreaterThan(0L);
    }

    @Test
    public void testConvertCourseRequirement_whenStartSessionIsSameAsEndSession_then_skipLoading() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GradCourse traxCourse = prepareCourseRequirementData("1996", "ASK", "10", "732", null,false);
        traxCourse.setEndSession(traxCourse.getStartSession());
        traxCourse.setAppliedSkills("Y");
        traxCourse.setFineArts("Y");

        courseProcess.convertCourseRequirement(traxCourse, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isZero();
    }


    private GradCourse prepareCourseRequirementData(String reqtYear, String courseCode, String courseLevel, String ruleCode, String lang, boolean isUpdateMode) {
        GradCourse traxCourse = new GradCourse();
        traxCourse.setCourseCode(courseCode);
        traxCourse.setCourseLevel(courseLevel);
        traxCourse.setGradReqtYear(reqtYear);
        traxCourse.setStartSession("200107");
        traxCourse.setEndSession("208001");

        CourseRequirementCodeDTO ruleCodeEntity = new CourseRequirementCodeDTO();
        ruleCodeEntity.setCourseRequirementCode(ruleCode);
        ruleCodeEntity.setLabel(courseCode + " " + courseLevel);
        ruleCodeEntity.setDescription("Credits must be earned in " + courseCode + " " + courseLevel);

        CourseRequirement entity = new CourseRequirement();
        entity.setCourseRequirementId(UUID.randomUUID());
        entity.setCourseCode(courseCode);
        entity.setCourseLevel(courseLevel);
        entity.setRuleCode(ruleCodeEntity);

        CourseRequirements courseRequirements = new CourseRequirements();
        courseRequirements.setCourseRequirementList(Arrays.asList(entity));

//        when(this.courseRequirementCodeRepository.findById(ruleCode)).thenReturn(Optional.of(ruleCodeEntity));
        when(this.restUtils.checkCourseRequirementExists(courseCode, courseLevel, ruleCode,"123")).thenReturn(isUpdateMode? true : false);
        when(this.restUtils.saveCourseRequirement(entity, "123")).thenReturn(entity);

        if (lang != null) {
            if (lang == " ") {
                when(this.restUtils.checkBlankLanguageCourse(courseCode, courseLevel, "123")).thenReturn(true);
            }
            if (lang == "F") {
                when(this.restUtils.checkFrenchLanguageCourse(courseCode, courseLevel, "123")).thenReturn(true);
            }
        }

        return traxCourse;
    }

}
