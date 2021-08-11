package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.entity.ConvGradStudentEntity;
import ca.bc.gov.educ.api.dataconversion.entity.ConvGradStudentSpecialProgramEntity;
import ca.bc.gov.educ.api.dataconversion.entity.GradCourseRestrictionEntity;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.ConvGradStudentRepository;
import ca.bc.gov.educ.api.dataconversion.repository.ConvGradStudentSpecialProgramRepository;
import ca.bc.gov.educ.api.dataconversion.repository.GradCourseRestrictionRepository;
import ca.bc.gov.educ.api.dataconversion.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.GradBatchTestUtils;
import ca.bc.gov.educ.api.dataconversion.rest.RestUtils;
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
public class DataConversionServiceWithMockRepositoryTest {

    @Autowired
    DataConversionService dataConversionService;

    @MockBean
    ConvGradStudentRepository convGradStudentRepository;

    @MockBean
    ConvGradStudentSpecialProgramRepository convGradStudentSpecialProgramRepository;

    @MockBean
    GradCourseRestrictionRepository gradCourseRestrictionRepository;

    @MockBean
    RestUtils restUtils;

    @Autowired
    private EducGradBatchGraduationApiConstants constants;

    @Autowired
    GradBatchTestUtils gradBatchTestUtils;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {
        convGradStudentRepository.deleteAll();
    }

    @Test
    public void convertStudent_forExistingGradStudent_whenGivenData_withFrechImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        ConvGradStudentEntity penStudentEntity = new ConvGradStudentEntity();
        penStudentEntity.setStudentID(studentID);
        penStudentEntity.setPen(pen);

        GradSpecialProgram specialProgram = new GradSpecialProgram();
        specialProgram.setId(UUID.randomUUID());
        specialProgram.setProgramCode("2018-EN");
        specialProgram.setSpecialProgramCode("FI");
        specialProgram.setSpecialProgramName("French Immersion");

        ConvGradStudentSpecialProgramEntity specialProgramEntity = new ConvGradStudentSpecialProgramEntity();
        specialProgramEntity.setId(UUID.randomUUID());
        specialProgramEntity.setStudentID(studentID);
        specialProgramEntity.setSpecialProgramID(specialProgram.getId());
        specialProgramEntity.setPen(pen);

        when(this.convGradStudentRepository.findByPen(pen)).thenReturn(Optional.of(penStudentEntity));
        when(this.convGradStudentRepository.save(penStudentEntity)).thenReturn(penStudentEntity);
        when(this.convGradStudentRepository.countFrenchImmersionCourses(pen)).thenReturn(1L);
        when(this.convGradStudentSpecialProgramRepository.save(specialProgramEntity)).thenReturn(specialProgramEntity);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getGradSpecialProgram("2018-EN", "FI", "123")).thenReturn(specialProgram);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN").recalculateGradStatus("Y")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("2018").build();
        ConversionSummaryDTO summary = new ConversionSummaryDTO();
        summary.setAccessToken("123");
        var result = dataConversionService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getRecalculateGradStatus()).isEqualTo("Y");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getProgramCode());

    }

    @Test
    public void convertStudent_whenGivenData_withFrechImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        ConvGradStudentEntity penStudentEntity = new ConvGradStudentEntity();
        penStudentEntity.setStudentID(studentID);
        penStudentEntity.setPen(pen);

        GradSpecialProgram specialProgram = new GradSpecialProgram();
        specialProgram.setId(UUID.randomUUID());
        specialProgram.setProgramCode("2018-EN");
        specialProgram.setSpecialProgramCode("FI");
        specialProgram.setSpecialProgramName("French Immersion");

        ConvGradStudentSpecialProgramEntity specialProgramEntity = new ConvGradStudentSpecialProgramEntity();
        specialProgramEntity.setId(UUID.randomUUID());
        specialProgramEntity.setStudentID(studentID);
        specialProgramEntity.setSpecialProgramID(specialProgram.getId());
        specialProgramEntity.setPen(pen);

        when(this.convGradStudentRepository.findByPen(pen)).thenReturn(Optional.empty());
        when(this.convGradStudentRepository.save(penStudentEntity)).thenReturn(penStudentEntity);
        when(this.convGradStudentRepository.countFrenchImmersionCourses(pen)).thenReturn(1L);
        when(this.convGradStudentSpecialProgramRepository.save(specialProgramEntity)).thenReturn(specialProgramEntity);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getGradSpecialProgram("2018-EN", "FI", "123")).thenReturn(specialProgram);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN").recalculateGradStatus("Y")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("2018").build();
        ConversionSummaryDTO summary = new ConversionSummaryDTO();
        summary.setAccessToken("123");
        var result = dataConversionService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getRecalculateGradStatus()).isEqualTo("Y");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getProgramCode());

    }

    @Test
    public void testLoadInitialRawGradStudentData() {
        Object[] obj = new Object[] {
               "123456789", "12345678", "12345678", "12", Character.valueOf('A'), "2020", Character.valueOf('Y')
        };
        List<Object[]> results = new ArrayList<>();
        results.add(obj);

        when(this.convGradStudentRepository.loadInitialRawData()).thenReturn(results);

        var result = dataConversionService.loadInitialRawGradStudentData(true);
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        ConvGradStudent responseStudent = result.get(0);
        assertThat(responseStudent.getPen()).isEqualTo(obj[0]);
    }

    @Test
    public void testConvertCourseRestriction() {
        ConversionSummaryDTO summary = new ConversionSummaryDTO();
        summary.setAccessToken("123");

        GradCourseRestriction courseRestriction = new GradCourseRestriction(
                "main", "12", "rest", "12", null, null
        );

        GradCourseRestrictionEntity gradCourseRestrictionEntity = new GradCourseRestrictionEntity();
        gradCourseRestrictionEntity.setCourseRestrictionId(UUID.randomUUID());
        gradCourseRestrictionEntity.setMainCourse("main");
        gradCourseRestrictionEntity.setMainCourseLevel("12");
        gradCourseRestrictionEntity.setRestrictedCourse("rest");
        gradCourseRestrictionEntity.setRestrictedCourseLevel("12");

        when(this.gradCourseRestrictionRepository.findByMainCourseAndMainCourseLevelAndRestrictedCourseAndRestrictedCourseLevel("main", "12", "rest", "12")).thenReturn(Optional.empty());
        when(this.gradCourseRestrictionRepository.save(gradCourseRestrictionEntity)).thenReturn(gradCourseRestrictionEntity);
        dataConversionService.convertCourseRestriction(courseRestriction, summary);
        assertThat(summary.getAddedCount()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRestriction_whenGivenRecordExists() {
        ConversionSummaryDTO summary = new ConversionSummaryDTO();
        summary.setAccessToken("123");

        GradCourseRestriction courseRestriction = new GradCourseRestriction(
                "main", "12", "rest", "12", null, null
        );

        GradCourseRestrictionEntity gradCourseRestrictionEntity = new GradCourseRestrictionEntity();
        gradCourseRestrictionEntity.setCourseRestrictionId(UUID.randomUUID());
        gradCourseRestrictionEntity.setMainCourse("main");
        gradCourseRestrictionEntity.setMainCourseLevel("12");
        gradCourseRestrictionEntity.setRestrictedCourse("rest");
        gradCourseRestrictionEntity.setRestrictedCourseLevel("12");

        when(this.gradCourseRestrictionRepository.findByMainCourseAndMainCourseLevelAndRestrictedCourseAndRestrictedCourseLevel("main", "12", "rest", "12")).thenReturn(Optional.of(gradCourseRestrictionEntity));
        when(this.gradCourseRestrictionRepository.save(gradCourseRestrictionEntity)).thenReturn(gradCourseRestrictionEntity);
        dataConversionService.convertCourseRestriction(courseRestriction, summary);
        assertThat(summary.getUpdatedCount()).isEqualTo(1L);
    }

    @Test
    public void testLoadInitialRawGradCourseRestrictionsData() {
        Object[] obj = new Object[] {
                "main", "12", "test", "12", null, null
        };
        List<Object[]> results = new ArrayList<>();
        results.add(obj);

        when(this.gradCourseRestrictionRepository.loadInitialRawData()).thenReturn(results);

        var result = dataConversionService.loadInitialRawGradCourseRestrictionsData(true);
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        GradCourseRestriction responseCourseRestriction = result.get(0);
        assertThat(responseCourseRestriction.getMainCourse()).isEqualTo("main");
    }
}
