package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.entity.course.CourseRequirementCodeEntity;
import ca.bc.gov.educ.api.dataconversion.entity.course.CourseRequirementEntity;
import ca.bc.gov.educ.api.dataconversion.entity.course.CourseRestrictionEntity;
import ca.bc.gov.educ.api.dataconversion.entity.trax.GraduationCourseEntity;
import ca.bc.gov.educ.api.dataconversion.entity.trax.GraduationCourseKey;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.conv.EventRepository;
import ca.bc.gov.educ.api.dataconversion.repository.course.CourseRequirementCodeRepository;
import ca.bc.gov.educ.api.dataconversion.repository.course.CourseRequirementRepository;
import ca.bc.gov.educ.api.dataconversion.repository.course.CourseRestrictionRepository;
import ca.bc.gov.educ.api.dataconversion.service.course.CourseService;
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

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class CourseServiceTest {

    @Autowired
    CourseService courseService;

    @MockBean
    CourseRestrictionRepository courseRestrictionRepository;

    @MockBean
    CourseRequirementRepository courseRequirementRepository;

    @MockBean
    CourseRequirementCodeRepository courseRequirementCodeRepository;

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
        courseRestrictionRepository.deleteAll();
        courseRequirementRepository.deleteAll();
        courseRequirementCodeRepository.deleteAll();
    }

    @Test
    public void testConvertCourseRestriction() {
        ConversionBaseSummaryDTO summary = new ConversionBaseSummaryDTO();
        summary.setAccessToken("123");

        GradCourseRestriction courseRestriction = new GradCourseRestriction(
                "main", "12", "rest", "12", null, null
        );

        CourseRestrictionEntity courseRestrictionEntity = new CourseRestrictionEntity();
        courseRestrictionEntity.setCourseRestrictionId(UUID.randomUUID());
        courseRestrictionEntity.setMainCourse("main");
        courseRestrictionEntity.setMainCourseLevel("12");
        courseRestrictionEntity.setRestrictedCourse("rest");
        courseRestrictionEntity.setRestrictedCourseLevel("12");

        when(this.courseRestrictionRepository.findByMainCourseAndMainCourseLevelAndRestrictedCourseAndRestrictedCourseLevel("main", "12", "rest", "12")).thenReturn(Optional.empty());
        when(this.courseRestrictionRepository.save(courseRestrictionEntity)).thenReturn(courseRestrictionEntity);
        courseService.convertCourseRestriction(courseRestriction, summary);
        assertThat(summary.getAddedCount()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRestriction_whenGivenRecordExists() {
        ConversionBaseSummaryDTO summary = new ConversionBaseSummaryDTO();
        summary.setAccessToken("123");

        GradCourseRestriction courseRestriction = new GradCourseRestriction(
                "main", "12", "rest", "12", null, null
        );

        CourseRestrictionEntity courseRestrictionEntity = new CourseRestrictionEntity();
        courseRestrictionEntity.setCourseRestrictionId(UUID.randomUUID());
        courseRestrictionEntity.setMainCourse("main");
        courseRestrictionEntity.setMainCourseLevel("12");
        courseRestrictionEntity.setRestrictedCourse("rest");
        courseRestrictionEntity.setRestrictedCourseLevel("12");

        when(this.courseRestrictionRepository.findByMainCourseAndMainCourseLevelAndRestrictedCourseAndRestrictedCourseLevel("main", "12", "rest", "12")).thenReturn(Optional.of(courseRestrictionEntity));
        when(this.courseRestrictionRepository.save(courseRestrictionEntity)).thenReturn(courseRestrictionEntity);
        courseService.convertCourseRestriction(courseRestriction, summary);
        assertThat(summary.getUpdatedCount()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRestriction_whenInvalidCourseIsProvided_throwsError() {
        ConversionBaseSummaryDTO summary = new ConversionBaseSummaryDTO();
        summary.setAccessToken("123");

        GradCourseRestriction courseRestriction = new GradCourseRestriction(
                "CLEA", "12", "CLEB", "12", null, null
        );

        courseService.convertCourseRestriction(courseRestriction, summary);
        assertThat(summary.getErrors()).hasSize(1);
    }

    // English 10
    @Test
    public void testConvertCourseRequirement_whenDataExists_forEnglish10_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "ENG", "10", "101", null,true);
        traxCourseEntity.setEnglish10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getUpdatedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish10_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "ENG", "10", "101", null, false);
        traxCourseEntity.setEnglish10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish10_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "ENG", "10", "701", null,false);
        traxCourseEntity.setEnglish10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish10_2018_withFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "ENG", "10", "302", "F",false);
        traxCourseEntity.setEnglish10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish10_2004_withFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "ENG", "10", "815", "F",false);
        traxCourseEntity.setEnglish10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish10_2018_withBlankLanguage() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "ENG", "10", "400", "F",false);
        traxCourseEntity.setEnglish10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish10_2004_withBlankLanguage() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "ENG", "10", "850", "F",false);
        traxCourseEntity.setEnglish10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    // English 11
    @Test
    public void testConvertCourseRequirement_whenDataExists_forEnglish11_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "ENG", "11", "102", null,true);
        traxCourseEntity.setEnglish11("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getUpdatedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "ENG", "11", "102", null, false);
        traxCourseEntity.setEnglish11("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "ENG", "11", "702", null,false);
        traxCourseEntity.setEnglish11("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1996", "ENG", "11", "721", null,false);
        traxCourseEntity.setEnglish11("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_1986() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1986", "ENG", "11", "740", null,false);
        traxCourseEntity.setEnglish11("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_2018_withFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "ENG", "11", "301", "F",false);
        traxCourseEntity.setEnglish11("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_2004_withFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "ENG", "11", "816", "F",false);
        traxCourseEntity.setEnglish11("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_1996_withFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1996", "ENG", "11", "818", "F",false);
        traxCourseEntity.setEnglish11("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_2018_withBlankLanguage() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "ENG", "11", "401", "F",false);
        traxCourseEntity.setEnglish11("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish11_2004_withBlankLanguage() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "ENG", "11", "851", "F",false);
        traxCourseEntity.setEnglish11("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    // English 12
    @Test
    public void testConvertCourseRequirement_whenDataExists_forEnglish12_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "ENG", "12", "103", null,true);
        traxCourseEntity.setEnglish12("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getUpdatedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "ENG", "12", "103", null, false);
        traxCourseEntity.setEnglish12("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "ENG", "12", "703", null,false);
        traxCourseEntity.setEnglish12("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_1950() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1950", "ENG", "12", "500", null,false);
        traxCourseEntity.setEnglish12("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1996", "ENG", "12", "722", null,false);
        traxCourseEntity.setEnglish12("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_1986() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1986", "ENG", "12", "741", null,false);
        traxCourseEntity.setEnglish12("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_2018_withFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "ENG", "12", "300", "F",false);
        traxCourseEntity.setEnglish12("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_2004_withFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "ENG", "12", "817", "F",false);
        traxCourseEntity.setEnglish12("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_1996_withFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1996", "ENG", "12", "819", "F",false);
        traxCourseEntity.setEnglish12("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_2018_withBlankLanguage() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "ENG", "12", "402", "F",false);
        traxCourseEntity.setEnglish12("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    @Test
    public void testConvertCourseRequirement_forEnglish12_2004_withBlankLanguage() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "ENG", "12", "852", "F",false);
        traxCourseEntity.setEnglish12("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(2L);
    }

    // Socials
    @Test
    public void testConvertCourseRequirement_forSocials10_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "SOC", "10", "104", null,false);
        traxCourseEntity.setSocials10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forSocials10_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "SOC", "10", "704", null,false);
        traxCourseEntity.setSocials10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forSocials_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "SOC", "11", "105", null,false);
        traxCourseEntity.setSocials("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forSocials_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "SOC", "11", "705", null,false);
        traxCourseEntity.setSocials("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forSocials_1950() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1950", "SOC", "11", "502", null,false);
        traxCourseEntity.setSocials("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forSocials_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1996", "SOC", "11", "723", null,false);
        traxCourseEntity.setSocials("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forSocials_1986() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1986", "SOC", "11", "742", null,false);
        traxCourseEntity.setSocials("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // Math
    @Test
    public void testConvertCourseRequirement_forMath10_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "MAT", "10", "106", null,false);
        traxCourseEntity.setMath10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forMath10_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "MAT", "10", "706", null,false);
        traxCourseEntity.setMath10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forMath_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "MAT", "11", "107", null,false);
        traxCourseEntity.setMath("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forMath_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "MAT", "11", "707", null,false);
        traxCourseEntity.setMath("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forMath_1950() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1950", "MAT", "11", "501", null,false);
        traxCourseEntity.setMath("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forMath_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1996", "MAT", "11", "724", null,false);
        traxCourseEntity.setMath("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forMath_1986() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1986", "MAT", "11", "743", null,false);
        traxCourseEntity.setMath("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // Science
    @Test
    public void testConvertCourseRequirement_forScience10_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "PHY", "10", "108", null,false);
        traxCourseEntity.setScience10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forScience10_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "PHY", "10", "708", null,false);
        traxCourseEntity.setScience10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forScience_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "PHY", "11", "109", null,false);
        traxCourseEntity.setScience("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forScience_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "PHY", "11", "709", null,false);
        traxCourseEntity.setScience("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forScience_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1996", "PHY", "11", "725", null,false);
        traxCourseEntity.setScience("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forScience_1986() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1986", "PHY", "11", "744", null,false);
        traxCourseEntity.setScience("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // Career Personal
    @Test
    public void testConvertCourseRequirement_forCareerPersonal10_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "CPP", "10", "112", null,false);
        traxCourseEntity.setCareerPersonal10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forCareerPersonal10_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "CPP", "10", "710", null,false);
        traxCourseEntity.setCareerPersonal10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forCareerPersonal11_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1996", "CPP", "11", "728", null,false);
        traxCourseEntity.setCareerPersonal11("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forCareerPersonal12_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1996", "CPP", "12", "729", null,false);
        traxCourseEntity.setCareerPersonal12("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // PhysEd
    @Test
    public void testConvertCourseRequirement_forPhysEd10_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "PHE", "10", "110", null,false);
        traxCourseEntity.setPhysEd10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forPhysEd10_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "PHE", "10", "711", null,false);
        traxCourseEntity.setPhysEd10("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // Applied Skills
    @Test
    public void testConvertCourseRequirement_forAppliedSkills_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "ASK", "10", "111", null,false);
        traxCourseEntity.setAppliedSkills("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forAppliedSkills_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "ASK", "10", "712", null,false);
        traxCourseEntity.setAppliedSkills("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forAppliedSkills_1996_withFineArts() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1996", "ASK", "10", "732", null,false);
        traxCourseEntity.setAppliedSkills("Y");
        traxCourseEntity.setFineArts("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRequirement_forAppliedSkills_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1996", "ASK", "10", "727", null,false);
        traxCourseEntity.setAppliedSkills("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // PortFolio
    @Test
    public void testConvertCourseRequirement_forPortFolio_2004() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2004", "PFL", "11", "713", null,false);
        traxCourseEntity.setPortfolio("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // ConsEd
    @Test
    public void testConvertCourseRequirement_forConsEd_1986() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1986", "CED", "11", "745", null,false);
        traxCourseEntity.setConsEd("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // Career Life Connections
    @Test
    public void testConvertCourseRequirement_forCareerLifeConnections_2018() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("2018", "CLC", "11", "113", null,false);
        traxCourseEntity.setCareerLifeConnections("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // Fine Arts
    @Test
    public void testConvertCourseRequirement_forFineArts_1996() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        GraduationCourseEntity traxCourseEntity = prepareCourseRequirementData("1996", "ART", "11", "726", null,false);
        traxCourseEntity.setFineArts("Y");

        courseService.convertCourseRequirement(traxCourseEntity, summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isEqualTo(1L);
    }

    // Hard-Coded Course Requirements
    @Test
    public void testCreateCourseRequirementsForFrenchImmersion() {
        ConversionCourseSummaryDTO summary = new ConversionCourseSummaryDTO();
        summary.setAccessToken("123");

        courseService.createCourseRequirementsForFrenchImmersion(summary);
        assertThat(summary.getAddedCountForCourseRequirement()).isGreaterThan(0L);
    }


    private GraduationCourseEntity prepareCourseRequirementData(String reqtYear, String courseCode, String courseLevel, String ruleCode, String lang, boolean isUpdateMode) {
        GraduationCourseKey courseKey = new GraduationCourseKey();
        courseKey.setCourseCode(courseCode);
        courseKey.setCourseLevel(courseLevel);
        courseKey.setGradReqtYear(reqtYear);

        GraduationCourseEntity traxCourseEntity = new GraduationCourseEntity();
        traxCourseEntity.setGraduationCourseKey(courseKey);

        CourseRequirementCodeEntity ruleCodeEntity = new CourseRequirementCodeEntity();
        ruleCodeEntity.setCourseRequirementCode(ruleCode);
        ruleCodeEntity.setLabel(courseCode + " " + courseLevel);
        ruleCodeEntity.setDescription("Credits must be earned in " + courseCode + " " + courseLevel);

        CourseRequirementEntity entity = new CourseRequirementEntity();
        entity.setCourseRequirementId(UUID.randomUUID());
        entity.setCourseCode(courseCode);
        entity.setCourseLevel(courseLevel);
        entity.setRuleCode(ruleCodeEntity);

        when(this.courseRequirementCodeRepository.findById(ruleCode)).thenReturn(Optional.of(ruleCodeEntity));
        when(this.courseRequirementRepository.findByCourseCodeAndCourseLevelAndRuleCode(courseCode, courseLevel, ruleCodeEntity)).thenReturn(isUpdateMode? entity : null);
        when(this.courseRequirementRepository.save(entity)).thenReturn(entity);

        if (lang != null && (lang == " " || lang == "F")) {
            when(this.courseRequirementRepository.countTabCourses(courseCode, courseLevel, lang)).thenReturn(1L);
        } else {
            when(this.courseRequirementRepository.countTabCourses(courseCode, courseLevel, lang)).thenReturn(0L);
        }

        return traxCourseEntity;
    }

}
