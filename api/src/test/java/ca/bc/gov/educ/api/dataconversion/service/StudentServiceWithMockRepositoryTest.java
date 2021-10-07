package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.entity.program.CareerProgramEntity;
import ca.bc.gov.educ.api.dataconversion.entity.student.*;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.student.*;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class StudentServiceWithMockRepositoryTest {

    @Autowired
    StudentService studentService;

    @MockBean
    GraduationStudentRecordRepository graduationStudentRecordRepository;

    @MockBean
    StudentOptionalProgramRepository studentOptionalProgramRepository;

    @MockBean
    StudentCareerProgramRepository studentCareerProgramRepository;

    @MockBean
    GraduationStudentRecordHistoryRepository graduationStudentRecordHistoryRepository;

    @MockBean
    StudentOptionalProgramHistoryRepository studentOptionalProgramHistoryRepository;

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
        graduationStudentRecordRepository.deleteAll();
    }

    @Test
    public void convertStudent_forExistingGradStudent_whenGivenData_withFrechImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);

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

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.of(gradStudentEntity));
        when(this.graduationStudentRecordRepository.save(gradStudentEntity)).thenReturn(gradStudentEntity);
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

        GraduationStudentRecordEntity gradStudentEntity = new GraduationStudentRecordEntity();
        gradStudentEntity.setStudentID(studentID);
        gradStudentEntity.setPen(pen);

        GraduationStudentRecordHistoryEntity gradStudentHistoryEntity = new GraduationStudentRecordHistoryEntity();
        BeanUtils.copyProperties(gradStudentEntity, gradStudentHistoryEntity);
//        gradStudentHistoryEntity.setHistoryID(UUID.randomUUID());
        gradStudentHistoryEntity.setActivityCode("DATACONVERT");

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

        StudentOptionalProgramHistoryEntity specialProgramHistoryEntity = new StudentOptionalProgramHistoryEntity();
        BeanUtils.copyProperties(specialProgramEntity, specialProgramHistoryEntity);
        specialProgramHistoryEntity.setStudentOptionalProgramID(specialProgramEntity.getId());
        specialProgramHistoryEntity.setActivityCode("DATACONVERT");

        CareerProgramEntity careerProgramEntity = new CareerProgramEntity();
        careerProgramEntity.setCode("XC");
        careerProgramEntity.setDescription("XC Test");
        careerProgramEntity.setStartDate(new Date(System.currentTimeMillis() - 100000L));
        careerProgramEntity.setEndDate(new Date(System.currentTimeMillis() + 100000L));

        StudentCareerProgramEntity studentCareerProgramEntity = new StudentCareerProgramEntity();
        studentCareerProgramEntity.setId(UUID.randomUUID());
        studentCareerProgramEntity.setStudentID(studentID);
        studentCareerProgramEntity.setCareerProgramCode("XC");

        when(this.graduationStudentRecordRepository.findById(studentID)).thenReturn(Optional.empty());
        when(this.graduationStudentRecordRepository.save(any(GraduationStudentRecordEntity.class))).thenReturn(gradStudentEntity);
        when(this.courseService.isFrenchImmersionCourse(pen)).thenReturn(true);
        when(this.programService.getCareerProgramCode("XC")).thenReturn(careerProgramEntity);
        when(this.studentOptionalProgramRepository.save(any(StudentOptionalProgramEntity.class))).thenReturn(specialProgramEntity);
        when(this.studentCareerProgramRepository.save(studentCareerProgramEntity)).thenReturn(studentCareerProgramEntity);
        when(this.graduationStudentRecordHistoryRepository.save(gradStudentHistoryEntity)).thenReturn(gradStudentHistoryEntity);
        when(this.studentOptionalProgramHistoryRepository.save(specialProgramHistoryEntity)).thenReturn(specialProgramHistoryEntity);
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
