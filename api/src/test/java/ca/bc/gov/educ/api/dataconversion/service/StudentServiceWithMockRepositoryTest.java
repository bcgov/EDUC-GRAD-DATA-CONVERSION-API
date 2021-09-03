package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.entity.program.CareerProgramEntity;
import ca.bc.gov.educ.api.dataconversion.entity.student.GraduationStatusEntity;
import ca.bc.gov.educ.api.dataconversion.entity.student.StudentCareerProgramEntity;
import ca.bc.gov.educ.api.dataconversion.entity.student.StudentOptionalProgramEntity;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.student.GraduationStatusRepository;
import ca.bc.gov.educ.api.dataconversion.repository.student.StudentCareerProgramRepository;
import ca.bc.gov.educ.api.dataconversion.repository.student.StudentOptionalProgramRepository;
import ca.bc.gov.educ.api.dataconversion.service.course.CourseService;
import ca.bc.gov.educ.api.dataconversion.service.program.ProgramService;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentService;
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

// TODO (jsung) : needs to work on Unit Test for ProgramService & CareerProgramRepository
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class StudentServiceWithMockRepositoryTest {

    @Autowired
    StudentService studentService;

    @MockBean
    GraduationStatusRepository graduationStatusRepository;

    @MockBean
    StudentOptionalProgramRepository studentOptionalProgramRepository;

    @MockBean
    StudentCareerProgramRepository studentCareerProgramRepository;

    @MockBean
    ProgramService programService;

    @MockBean
    CourseService courseService;

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
        graduationStatusRepository.deleteAll();
    }

    @Test
    public void convertStudent_forExistingGradStudent_whenGivenData_withFrechImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStatusEntity penStudentEntity = new GraduationStatusEntity();
        penStudentEntity.setStudentID(studentID);
        penStudentEntity.setPen(pen);

        GradSpecialProgram specialProgram = new GradSpecialProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("2018-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgramEntity specialProgramEntity = new StudentOptionalProgramEntity();
        specialProgramEntity.setId(UUID.randomUUID());
        specialProgramEntity.setStudentID(studentID);
        specialProgramEntity.setOptionalProgramID(specialProgram.getOptionalProgramID());
        specialProgramEntity.setPen(pen);

        StudentCareerProgramEntity careerProgramEntity = new StudentCareerProgramEntity();
        careerProgramEntity.setId(UUID.randomUUID());
        careerProgramEntity.setStudentID(studentID);
        careerProgramEntity.setCareerProgramCode("XC");

        when(this.graduationStatusRepository.findById(studentID)).thenReturn(Optional.of(penStudentEntity));
        when(this.graduationStatusRepository.save(penStudentEntity)).thenReturn(penStudentEntity);
        when(this.courseService.isFrenchImmersionCourse(pen)).thenReturn(true);
        when(this.programService.getCareerProgramCode("XC")).thenReturn(null);
        when(this.studentOptionalProgramRepository.save(specialProgramEntity)).thenReturn(specialProgramEntity);
        when(this.studentCareerProgramRepository.save(careerProgramEntity)).thenReturn(careerProgramEntity);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getGradSpecialProgram("2018-EN", "FI", "123")).thenReturn(specialProgram);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN").recalculateGradStatus("Y")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

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

        GraduationStatusEntity penStudentEntity = new GraduationStatusEntity();
        penStudentEntity.setStudentID(studentID);
        penStudentEntity.setPen(pen);

        GradSpecialProgram specialProgram = new GradSpecialProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("2018-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgramEntity specialProgramEntity = new StudentOptionalProgramEntity();
        specialProgramEntity.setId(UUID.randomUUID());
        specialProgramEntity.setStudentID(studentID);
        specialProgramEntity.setOptionalProgramID(specialProgram.getOptionalProgramID());
        specialProgramEntity.setPen(pen);

        CareerProgramEntity careerProgramEntity = new CareerProgramEntity();
        careerProgramEntity.setCode("XC");
        careerProgramEntity.setDescription("XC Test");
        careerProgramEntity.setStartDate(new Date(System.currentTimeMillis() - 100000L));
        careerProgramEntity.setEndDate(new Date(System.currentTimeMillis() + 100000L));

        StudentCareerProgramEntity studentCareerProgramEntity = new StudentCareerProgramEntity();
        studentCareerProgramEntity.setId(UUID.randomUUID());
        studentCareerProgramEntity.setStudentID(studentID);
        studentCareerProgramEntity.setCareerProgramCode("XC");

        when(this.graduationStatusRepository.findById(studentID)).thenReturn(Optional.empty());
        when(this.graduationStatusRepository.save(penStudentEntity)).thenReturn(penStudentEntity);
        when(this.courseService.isFrenchImmersionCourse(pen)).thenReturn(true);
        when(this.programService.getCareerProgramCode("XC")).thenReturn(careerProgramEntity);
        when(this.studentOptionalProgramRepository.save(specialProgramEntity)).thenReturn(specialProgramEntity);
        when(this.studentCareerProgramRepository.save(studentCareerProgramEntity)).thenReturn(studentCareerProgramEntity);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getGradSpecialProgram("2018-EN", "FI", "123")).thenReturn(specialProgram);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN").recalculateGradStatus("Y")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getRecalculateGradStatus()).isEqualTo("Y");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

}
