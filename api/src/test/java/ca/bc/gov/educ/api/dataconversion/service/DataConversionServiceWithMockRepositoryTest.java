package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.entity.conv.ConvGradStudentEntity;
import ca.bc.gov.educ.api.dataconversion.entity.conv.ConvGradStudentSpecialProgramEntity;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.conv.ConvGradStudentRepository;
import ca.bc.gov.educ.api.dataconversion.repository.conv.ConvGradStudentSpecialProgramRepository;
import ca.bc.gov.educ.api.dataconversion.repository.course.GradCourseRestrictionRepository;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;
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
    private EducGradDataConversionApiConstants constants;

    @Autowired
    GradConversionTestUtils gradConversionTestUtils;

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
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("2018-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        ConvGradStudentSpecialProgramEntity specialProgramEntity = new ConvGradStudentSpecialProgramEntity();
        specialProgramEntity.setId(UUID.randomUUID());
        specialProgramEntity.setStudentID(studentID);
        specialProgramEntity.setOptionalProgramID(specialProgram.getOptionalProgramID());
        specialProgramEntity.setPen(pen);

        when(this.convGradStudentRepository.findById(studentID)).thenReturn(Optional.of(penStudentEntity));
        when(this.convGradStudentRepository.save(penStudentEntity)).thenReturn(penStudentEntity);
        when(this.gradCourseRestrictionRepository.countFrenchImmersionCourses(pen)).thenReturn(1L);
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
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

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
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("2018-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        ConvGradStudentSpecialProgramEntity specialProgramEntity = new ConvGradStudentSpecialProgramEntity();
        specialProgramEntity.setId(UUID.randomUUID());
        specialProgramEntity.setStudentID(studentID);
        specialProgramEntity.setOptionalProgramID(specialProgram.getOptionalProgramID());
        specialProgramEntity.setPen(pen);

        when(this.convGradStudentRepository.findById(studentID)).thenReturn(Optional.empty());
        when(this.convGradStudentRepository.save(penStudentEntity)).thenReturn(penStudentEntity);
        when(this.gradCourseRestrictionRepository.countFrenchImmersionCourses(pen)).thenReturn(1L);
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
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

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

}
