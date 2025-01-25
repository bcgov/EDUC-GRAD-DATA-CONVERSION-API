package ca.bc.gov.educ.api.dataconversion.process;

import ca.bc.gov.educ.api.dataconversion.constant.EventType;
import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.model.tsw.SchoolClob;
import ca.bc.gov.educ.api.dataconversion.repository.EventRepository;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiUtils;
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
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class StudentProcessTest {

    @Autowired
    StudentProcess studentProcess;

    @MockBean
    EventRepository eventRepository;

    @MockBean
    CourseProcess courseProcess;

    @MockBean
    AssessmentProcess assessmentProcess;

    @MockBean
    RestUtils restUtils;

    @MockBean
    WebClient webClient;

    // NATS
    @MockBean
    private NatsConnection natsConnection;
    @MockBean
    private Subscriber subscriber;

    @Autowired
    private EducGradDataConversionApiConstants constants;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {
        //Placeholder method
    }

    @Test
    public void convertStudent_forPenStudentValidation_whenGivenPen_doesNotExist_thenReturnFailure() throws Exception {
        String pen = "111222333";
        String mincode = "222333";
        UUID schoolId = UUID.randomUUID();

        SchoolClob school = new SchoolClob();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(new ArrayList<>());

        ConvGradStudent student = ConvGradStudent.builder().pen(pen).program("SCCP")
                .archiveFlag("A").studentStatus("A").schoolOfRecordId(schoolId)
                .graduationRequirementYear("SCCP").programCodes(new ArrayList<>()).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("SCCP");
        assertThat(summary.getErrors()).isNotEmpty();
        assertThat(summary.getErrors().get(0).getReason()).startsWith("PEN does not exist:");
    }

    @Test
    public void convertStudent_forPenStudentValidation_whenPENAPI_isDown_thenReturnFailure() throws Exception {
        String pen = "111222333";
        String mincode = "222333";
        UUID schoolId = UUID.randomUUID();

        SchoolClob school = new SchoolClob();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        when(this.restUtils.getStudentsByPen(pen, "123")).thenThrow(new RuntimeException("Test"));

        ConvGradStudent student = ConvGradStudent.builder().pen(pen).program("SCCP")
                .studentStatus("A").schoolOfRecordId(schoolId)
                .graduationRequirementYear("SCCP")
                .programCodes(new ArrayList<>()).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("SCCP");
        assertThat(summary.getErrors()).isNotEmpty();
        assertThat(summary.getErrors().get(0).getReason()).startsWith("PEN Student API is failed:");
    }

    @Test
    public void convertStudent_forExistingGradStudent_whenGivenData_withSccpProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";
        UUID schoolId = UUID.randomUUID();

        SchoolClob school = new SchoolClob();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("SCCP");
        gradStudent.setStudentGrade("11");
        gradStudent.setStudentStatus("CUR");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("SCCP")
                .archiveFlag("A").studentStatus("A").schoolOfRecordId(schoolId)
                .graduationRequirementYear("SCCP").programCodes(new ArrayList<>()).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("SCCP");
    }

    @Test
    public void convertStudent_forExistingGradStudent_whenGivenData_withAdultProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";
        UUID schoolId = UUID.randomUUID();

        SchoolClob school = new SchoolClob();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setDob("2000-06-30");
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1950");
        gradStudent.setStudentGrade("AD");
        gradStudent.setStudentStatus("CUR");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1950")
                .studentGrade("AD").archiveFlag("A").studentStatus("A")
                .schoolOfRecordId(schoolId)
                .graduationRequirementYear("1950")
                .programCodes(new ArrayList<>()).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1950");
    }

    @Test
    public void convertStudent_forExistingGradStudent_whenGivenData_withFrenchImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";
        UUID schoolId = UUID.randomUUID();

        SchoolClob school = new SchoolClob();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2018-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("2018-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram = new StudentOptionalProgram();
        studentOptionalProgram.setId(UUID.randomUUID());
        studentOptionalProgram.setStudentID(studentID);
        studentOptionalProgram.setOptionalProgramID(specialProgram.getOptionalProgramID());
        studentOptionalProgram.setOptionalProgramName(specialProgram.getOptionalProgramName());
        studentOptionalProgram.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(specialProgram.getOptProgramCode());
        specialProgramReq.setPen(pen);

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram);
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("2018-EN", "FI", "123")).thenReturn(specialProgram);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN")
                .archiveFlag("A").studentStatus("A").schoolOfRecordId(schoolId)
                .graduationRequirementYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("2018");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertStudent_whenGivenData_withFrenchImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";
        UUID schoolId = UUID.randomUUID();

        SchoolClob school = new SchoolClob();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2018-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("2018-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram = new StudentOptionalProgram();
        studentOptionalProgram.setId(UUID.randomUUID());
        studentOptionalProgram.setStudentID(studentID);
        studentOptionalProgram.setOptionalProgramID(specialProgram.getOptionalProgramID());
        studentOptionalProgram.setOptionalProgramName(specialProgram.getOptionalProgramName());
        studentOptionalProgram.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(specialProgram.getOptProgramCode());
        specialProgramReq.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(GraduationStudentRecord.class), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourse(pen, "10", "123")).thenReturn(true);
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram);
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("2018-EN", "FI", "123")).thenReturn(specialProgram);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN")
                .archiveFlag("A").studentStatus("A").schoolOfRecordId(schoolId)
                .graduationRequirementYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("2018");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertStudent_whenGivenData_forMergedStatus_withFrenchImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";
        UUID schoolId = UUID.randomUUID();

        SchoolClob school = new SchoolClob();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1986-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("MER");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("1986-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram = new StudentOptionalProgram();
        studentOptionalProgram.setId(UUID.randomUUID());
        studentOptionalProgram.setStudentID(studentID);
        studentOptionalProgram.setOptionalProgramID(specialProgram.getOptionalProgramID());
        studentOptionalProgram.setOptionalProgramName(specialProgram.getOptionalProgramName());
        studentOptionalProgram.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(specialProgram.getOptProgramCode());
        specialProgramReq.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourseForEN(pen, "11", "123")).thenReturn(true);
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram);
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("1986-EN", "FI", "123")).thenReturn(specialProgram);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1986-EN")
                .studentStatus("M").schoolOfRecordId(schoolId)
                .graduationRequirementYear("1986")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1986");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertStudent_whenGiven1996Data_withFrenchImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";
        UUID schoolId = UUID.randomUUID();

        SchoolClob school = new SchoolClob();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1996-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("1996-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram = new StudentOptionalProgram();
        studentOptionalProgram.setId(UUID.randomUUID());
        studentOptionalProgram.setStudentID(studentID);
        studentOptionalProgram.setOptionalProgramID(specialProgram.getOptionalProgramID());
        studentOptionalProgram.setOptionalProgramName(specialProgram.getOptionalProgramName());
        studentOptionalProgram.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(specialProgram.getOptProgramCode());
        specialProgramReq.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourse(pen, "11", "123")).thenReturn(true);
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram);
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("1996-EN", "FI", "123")).thenReturn(specialProgram);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1996-EN")
                .archiveFlag("A").studentStatus("A").schoolOfRecordId(schoolId)
                .graduationRequirementYear("1996")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1996");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertStudent_whenGiven1986Data_withFrenchImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";
        UUID schoolId = UUID.randomUUID();

        SchoolClob school = new SchoolClob();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1986-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("1986-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram = new StudentOptionalProgram();
        studentOptionalProgram.setId(UUID.randomUUID());
        studentOptionalProgram.setStudentID(studentID);
        studentOptionalProgram.setOptionalProgramID(specialProgram.getOptionalProgramID());
        studentOptionalProgram.setOptionalProgramName(specialProgram.getOptionalProgramName());
        studentOptionalProgram.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(specialProgram.getOptProgramCode());
        specialProgramReq.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourseForEN(pen, "11", "123")).thenReturn(true);
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram);
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("1986-EN", "FI", "123")).thenReturn(specialProgram);
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1986-EN")
                .archiveFlag("A").studentStatus("A").schoolOfRecordId(schoolId)
                .graduationRequirementYear("1986")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1986");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertArchivedStudent_whenGiven1986Data_withFrenchImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        String mincode = "222333";
        UUID schoolId = UUID.randomUUID();

        SchoolClob school = new SchoolClob();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("1986-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("ARC");

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("1986-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram = new StudentOptionalProgram();
        studentOptionalProgram.setId(UUID.randomUUID());
        studentOptionalProgram.setStudentID(studentID);
        studentOptionalProgram.setOptionalProgramID(specialProgram.getOptionalProgramID());
        studentOptionalProgram.setOptionalProgramName(specialProgram.getOptionalProgramName());
        studentOptionalProgram.setPen(pen);

        StudentOptionalProgramRequestDTO specialProgramReq = new StudentOptionalProgramRequestDTO();
        specialProgramReq.setId(studentOptionalProgram.getId());
        specialProgramReq.setStudentID(studentID);
        specialProgramReq.setMainProgramCode(gradStudent.getProgram());
        specialProgramReq.setOptionalProgramCode(specialProgram.getOptProgramCode());
        specialProgramReq.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);
        when(this.restUtils.saveStudentGradStatus(eq(studentID.toString()), any(), eq(false), eq("123"))).thenReturn(gradStudent);
        when(this.courseProcess.isFrenchImmersionCourseForEN(pen, "11", "123")).thenReturn(true);
        when(this.restUtils.saveStudentOptionalProgram(specialProgramReq, "123")).thenReturn(studentOptionalProgram);
        when(this.restUtils.saveStudentCareerProgram(studentCareerProgram, "123")).thenReturn(studentCareerProgram);
        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getOptionalProgram("1986-EN", "FI", "123")).thenReturn(specialProgram);
        when(this.restUtils.addNewPen(penStudent, "123")).thenReturn(penStudent);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("1986-EN")
                .archiveFlag("I").studentStatus("A").schoolOfRecordId(schoolId)
                .graduationRequirementYear("1986")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");
        var result = studentProcess.convertStudent(student, summary, false, false);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getGraduationRequirementYear()).isEqualTo("1986");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void testAddStudentCareerProgram() {
        UUID studentID = UUID.randomUUID();
        String careerProgramCode = "XC";

        boolean exceptionIsThrown = false;
        try {
            studentProcess.addStudentCareerProgram(careerProgramCode, studentID, "123");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testRemoveStudentCareerProgram() {
        UUID studentID = UUID.randomUUID();
        String careerProgramCode = "XC";

        StudentGradDTO requestStudent = new StudentGradDTO();
        requestStudent.setStudentID(studentID);

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setCareerProgramCode(careerProgramCode);
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setId(UUID.randomUUID());

        boolean exceptionIsThrown = false;
        try {
            studentProcess.removeStudentCareerProgram(careerProgramCode, requestStudent, "123");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testExistCareerProgram() {
        UUID studentID = UUID.randomUUID();
        String careerProgramCode = "XC";

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setCareerProgramCode(careerProgramCode);
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setId(UUID.randomUUID());

        when(this.restUtils.getStudentCareerPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentCareerProgram));

        var result = studentProcess.existsCareerProgram(studentID, "123");
        assertThat(result).isTrue();
    }

    @Test
    public void testAddStudentOptionalProgram() {
        String program = "2018-EN";

        UUID studentID = UUID.randomUUID();
        String optionalProgramCode = "FI";

        OptionalProgram optionalProgram = new OptionalProgram();
        optionalProgram.setOptionalProgramID(UUID.randomUUID());
        optionalProgram.setOptProgramCode(optionalProgramCode);
        optionalProgram.setOptionalProgramName("French Immersion");
        optionalProgram.setGraduationProgramCode(program);

        StudentGradDTO requestStudent = new StudentGradDTO();
        requestStudent.setStudentID(studentID);
        requestStudent.setProgram(program);

        when(this.restUtils.getOptionalProgram(eq(program), eq(optionalProgramCode), any())).thenReturn(optionalProgram);

        boolean exceptionIsThrown = false;
        try {
            studentProcess.addStudentOptionalProgram(optionalProgramCode, requestStudent, true, "123");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testRemoveStudentOptionalProgram() {
        String program = "2018-EN";

        UUID studentID = UUID.randomUUID();
        String optionalProgramCode = "FI";

        OptionalProgram optionalProgram = new OptionalProgram();
        optionalProgram.setOptionalProgramID(UUID.randomUUID());
        optionalProgram.setOptProgramCode(optionalProgramCode);
        optionalProgram.setOptionalProgramName("French Immersion");
        optionalProgram.setGraduationProgramCode(program);

        StudentGradDTO requestStudent = new StudentGradDTO();
        requestStudent.setStudentID(studentID);
        requestStudent.setProgram(program);

        when(this.restUtils.getOptionalProgram(eq(program), eq(optionalProgramCode), any())).thenReturn(optionalProgram);

        boolean exceptionIsThrown = false;
        try {
            studentProcess.removeStudentOptionalProgram(optionalProgramCode, requestStudent, "123");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testSaveGraduationStudent_whenENisChangedToPF_then_returnAPICallSuccess() {
        String program = "2018-EN";
        String pen = "111222333";
        UUID studentID = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID newSchoolId = UUID.randomUUID();

        GraduationStudentRecord graduationStudentRecord = new GraduationStudentRecord();
        graduationStudentRecord.setStudentID(studentID);
        graduationStudentRecord.setProgram(program);
        graduationStudentRecord.setStudentGrade("11");
        graduationStudentRecord.setStudentStatus("CUR");
        graduationStudentRecord.setSchoolOfRecordId(schoolId);

        StudentOptionalProgram studentOptionalProgram1 = new StudentOptionalProgram();
        studentOptionalProgram1.setId(UUID.randomUUID());
        studentOptionalProgram1.setOptionalProgramID(UUID.randomUUID());
        studentOptionalProgram1.setStudentID(studentID);
        studentOptionalProgram1.setOptionalProgramCode("FI");

        StudentOptionalProgram studentOptionalProgram2 = new StudentOptionalProgram();
        studentOptionalProgram2.setId(UUID.randomUUID());
        studentOptionalProgram2.setOptionalProgramID(UUID.randomUUID());
        studentOptionalProgram2.setStudentID(studentID);
        studentOptionalProgram2.setOptionalProgramCode("BD");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(graduationStudentRecord);
        when(this.restUtils.getStudentOptionalPrograms(studentID.toString(), "123")).thenReturn(List.of(studentOptionalProgram1, studentOptionalProgram2));

        StudentGradDTO requestStudent = new StudentGradDTO();
        requestStudent.setStudentID(studentID);
        requestStudent.setProgram(program);
        requestStudent.setNewProgram("2018-PF");
        requestStudent.setNewStudentGrade("12");
        requestStudent.setNewSchoolOfRecordId(newSchoolId);
        requestStudent.setNewStudentStatus("ARC");
        requestStudent.setNewRecalculateGradStatus("Y");
        requestStudent.setNewRecalculateProjectedGrad("Y");
        requestStudent.setAddDualDogwood(true);

        boolean exceptionIsThrown = false;
        try {
            studentProcess.saveGraduationStudent(pen, requestStudent, EventType.UPD_GRAD, "123");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testSaveGraduationStudent_whenPFisChangedToEN_then_returnAPICallSuccess() {
        String program = "2018-PF";
        String pen = "111222333";
        UUID studentID = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID newSchoolId = UUID.randomUUID();

        GraduationStudentRecord graduationStudentRecord = new GraduationStudentRecord();
        graduationStudentRecord.setStudentID(studentID);
        graduationStudentRecord.setProgram(program);
        graduationStudentRecord.setStudentGrade("11");
        graduationStudentRecord.setStudentStatus("CUR");
        graduationStudentRecord.setSchoolOfRecordId(schoolId);

        StudentOptionalProgram studentOptionalProgram1 = new StudentOptionalProgram();
        studentOptionalProgram1.setId(UUID.randomUUID());
        studentOptionalProgram1.setOptionalProgramID(UUID.randomUUID());
        studentOptionalProgram1.setStudentID(studentID);
        studentOptionalProgram1.setOptionalProgramCode("DD");

        StudentOptionalProgram studentOptionalProgram2 = new StudentOptionalProgram();
        studentOptionalProgram2.setId(UUID.randomUUID());
        studentOptionalProgram2.setOptionalProgramID(UUID.randomUUID());
        studentOptionalProgram2.setStudentID(studentID);
        studentOptionalProgram2.setOptionalProgramCode("BD");

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(graduationStudentRecord);
        when(this.restUtils.getStudentOptionalPrograms(studentID.toString(), "123")).thenReturn(List.of(studentOptionalProgram1, studentOptionalProgram2));

        StudentGradDTO requestStudent = new StudentGradDTO();
        requestStudent.setStudentID(studentID);
        requestStudent.setProgram(program);
        requestStudent.setNewProgram("2018-EN");
        requestStudent.setNewStudentGrade("12");
        requestStudent.setNewSchoolOfRecordId(newSchoolId);
        requestStudent.setNewStudentStatus("ARC");
        requestStudent.setNewRecalculateGradStatus("Y");
        requestStudent.setNewRecalculateProjectedGrad("Y");
        requestStudent.setAddFrenchImmersion(true);

        boolean exceptionIsThrown = false;
        try {
            studentProcess.saveGraduationStudent(pen, requestStudent, EventType.UPD_GRAD, "123");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testSaveGraduationStudent_whenENisChangedTo1950Adult_then_returnAPICallSuccess() {
        String program = "2018-EN";
        String pen = "111222333";
        UUID studentID = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID newSchoolId = UUID.randomUUID();

        GraduationStudentRecord graduationStudentRecord = new GraduationStudentRecord();
        graduationStudentRecord.setStudentID(studentID);
        graduationStudentRecord.setProgram(program);
        graduationStudentRecord.setStudentGrade("11");
        graduationStudentRecord.setStudentStatus("CUR");
        graduationStudentRecord.setSchoolOfRecordId(schoolId);

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(graduationStudentRecord);

        StudentGradDTO requestStudent = new StudentGradDTO();
        requestStudent.setStudentID(studentID);
        requestStudent.setProgram(program);
        requestStudent.setNewProgram("1950");
        requestStudent.setNewStudentGrade("AD");
        requestStudent.setNewSchoolOfRecordId(newSchoolId);
        requestStudent.setNewStudentStatus("ARC");
        requestStudent.setNewRecalculateGradStatus("Y");
        requestStudent.setNewRecalculateProjectedGrad("Y");
        requestStudent.setNewAdultStartDate("2010-01-01");
        requestStudent.setNewCitizenship("C");
        requestStudent.setNewGradDate("2022/06/01");

        boolean exceptionIsThrown = false;
        try {
            studentProcess.saveGraduationStudent(pen, requestStudent, EventType.UPD_GRAD, "123");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testLoadStudentData_whenPENStudentAPIisDown_returnsNull() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        when(this.restUtils.getStudentsByPen(pen, "123")).thenThrow(IllegalArgumentException.class);

        var result = studentProcess.loadStudentData(pen, "123");
        assertThat(result).isNull();
    }

    @Test
    public void testLoadStudentData_whenPENStudentIsNotFound_returnsNull() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(new ArrayList<>());

        var result = studentProcess.loadStudentData(pen, "123");
        assertThat(result).isNull();
    }

    @Test
    public void testLoadStudentData_whenGraduationStudentIsNotFound_returnsNull() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(null);

        var result = studentProcess.loadStudentData(pen, "123");
        assertThat(result).isNull();
    }

    @Test
    public void testLoadStudentData_whenGradStudentAPIisDown_returnsNull() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenThrow(new RuntimeException("GRAD Student API is down!"));

        var result = studentProcess.loadStudentData(pen, "123");
        assertThat(result).isNull();
    }

    @Test
    public void testLoadStudentData_whenGradStudentAPIisDownForStudentOptionalPrograms_returnsSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        UUID schoolId = UUID.randomUUID();

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2018-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("Y");
        gradStudent.setSchoolOfRecordId(schoolId);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));

        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.getStudentOptionalPrograms(studentID.toString(), "123")).thenThrow(new RuntimeException("GRAD Student API is donw!"));

        var result = studentProcess.loadStudentData(pen, "123");
        assertThat(result).isNotNull();
    }

    @Test
    public void testLoadStudentData_whenGradStudentAPIisDownForStudentCareerPrograms_returnsSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        UUID schoolId = UUID.randomUUID();

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2018-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("Y");
        gradStudent.setSchoolOfRecordId(schoolId);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));

        OptionalProgram optionalProgram1 = new OptionalProgram();
        optionalProgram1.setOptionalProgramID(UUID.randomUUID());
        optionalProgram1.setGraduationProgramCode("2018-EN");
        optionalProgram1.setOptProgramCode("FI");
        optionalProgram1.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram1 = new StudentOptionalProgram();
        studentOptionalProgram1.setId(UUID.randomUUID());
        studentOptionalProgram1.setStudentID(studentID);
        studentOptionalProgram1.setOptionalProgramID(optionalProgram1.getOptionalProgramID());
        studentOptionalProgram1.setOptionalProgramName(optionalProgram1.getOptionalProgramName());
        studentOptionalProgram1.setOptionalProgramCode("FI");
        studentOptionalProgram1.setPen(pen);

        OptionalProgram optionalProgram2 = new OptionalProgram();
        optionalProgram2.setOptionalProgramID(UUID.randomUUID());
        optionalProgram2.setGraduationProgramCode("2018-EN");
        optionalProgram2.setOptProgramCode("CP");
        optionalProgram2.setOptionalProgramName("Career Program");

        StudentOptionalProgram studentOptionalProgram2 = new StudentOptionalProgram();
        studentOptionalProgram2.setId(UUID.randomUUID());
        studentOptionalProgram2.setStudentID(studentID);
        studentOptionalProgram2.setOptionalProgramID(optionalProgram2.getOptionalProgramID());
        studentOptionalProgram2.setOptionalProgramName(optionalProgram2.getOptionalProgramName());
        studentOptionalProgram2.setOptionalProgramCode("CP");
        studentOptionalProgram2.setPen(pen);

        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        when(this.restUtils.getStudentOptionalPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentOptionalProgram1, studentOptionalProgram2));
        when(this.restUtils.getStudentCareerPrograms(studentID.toString(), "123")).thenThrow(new RuntimeException("GRAD Student API is donw!"));

        var result = studentProcess.loadStudentData(pen, "123");
        assertThat(result).isNotNull();
    }

    @Test
    public void testLoadStudentData_withGivenData_returnsStudentGradDTO_withAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        UUID schoolId = UUID.randomUUID();

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2018-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("Y");
        gradStudent.setSchoolOfRecordId(schoolId);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));

        OptionalProgram optionalProgram1 = new OptionalProgram();
        optionalProgram1.setOptionalProgramID(UUID.randomUUID());
        optionalProgram1.setGraduationProgramCode("2018-EN");
        optionalProgram1.setOptProgramCode("FI");
        optionalProgram1.setOptionalProgramName("French Immersion");

        StudentOptionalProgram studentOptionalProgram1 = new StudentOptionalProgram();
        studentOptionalProgram1.setId(UUID.randomUUID());
        studentOptionalProgram1.setStudentID(studentID);
        studentOptionalProgram1.setOptionalProgramID(optionalProgram1.getOptionalProgramID());
        studentOptionalProgram1.setOptionalProgramName(optionalProgram1.getOptionalProgramName());
        studentOptionalProgram1.setOptionalProgramCode("FI");
        studentOptionalProgram1.setPen(pen);

        OptionalProgram optionalProgram2 = new OptionalProgram();
        optionalProgram2.setOptionalProgramID(UUID.randomUUID());
        optionalProgram2.setGraduationProgramCode("2018-EN");
        optionalProgram2.setOptProgramCode("CP");
        optionalProgram2.setOptionalProgramName("Career Program");

        StudentOptionalProgram studentOptionalProgram2 = new StudentOptionalProgram();
        studentOptionalProgram2.setId(UUID.randomUUID());
        studentOptionalProgram2.setStudentID(studentID);
        studentOptionalProgram2.setOptionalProgramID(optionalProgram2.getOptionalProgramID());
        studentOptionalProgram2.setOptionalProgramName(optionalProgram2.getOptionalProgramName());
        studentOptionalProgram2.setOptionalProgramCode("CP");
        studentOptionalProgram2.setPen(pen);

        CareerProgram careerProgram = new CareerProgram();
        careerProgram.setCode("XC");
        careerProgram.setDescription("XC Test");
        careerProgram.setStartDate(new Date(System.currentTimeMillis() - 100000L).toString());
        careerProgram.setEndDate(new Date(System.currentTimeMillis() + 100000L).toString());

        StudentCareerProgram studentCareerProgram = new StudentCareerProgram();
        studentCareerProgram.setId(UUID.randomUUID());
        studentCareerProgram.setStudentID(studentID);
        studentCareerProgram.setCareerProgramCode("XC");

        StudentCourse studentCourse = new StudentCourse();
        studentCourse.setPen(pen);
        studentCourse.setCourseCode("TestCourse");
        studentCourse.setCourseLevel("12");
        studentCourse.setCredits(Integer.valueOf("4"));

        StudentAssessment studentAssessment = new StudentAssessment();
        studentAssessment.setPen(pen);
        studentAssessment.setAssessmentCode("TestAssmt");
        studentAssessment.setAssessmentName("Test Assessment");

        when(this.restUtils.getStudentsByPen(pen, "123")).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);

        when(this.restUtils.getOptionalProgramByID(optionalProgram1.getOptionalProgramID(), "123")).thenReturn(optionalProgram1);
        when(this.restUtils.getStudentOptionalPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentOptionalProgram1, studentOptionalProgram2));
        when(this.restUtils.getStudentCareerPrograms(studentID.toString(), "123")).thenReturn(Arrays.asList(studentCareerProgram));
        when(this.courseProcess.getStudentCourses(pen, "123")).thenReturn(Arrays.asList(studentCourse));
        when(this.assessmentProcess.getStudentAssessments(pen, "123")).thenReturn(Arrays.asList(studentAssessment));

        var result = studentProcess.loadStudentData(pen, "123");
        assertThat(result).isNotNull();
        assertThat(result.getStudentID()).isEqualTo(studentID);
        assertThat(result.getProgramCodes()).isNotEmpty();
        assertThat(result.getProgramCodes()).hasSize(3);
        assertThat(result.getCourses()).isNotEmpty();
        assertThat(result.getCourses()).hasSize(1);
        assertThat(result.getAssessments()).isNotEmpty();
        assertThat(result.getAssessments()).hasSize(1);
    }

    @Test
    public void testTriggerGraduationBatchRun_withMergedStatus_returnsNoBatchRun() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        UUID schoolId = UUID.randomUUID();

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2018-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("MER");
        gradStudent.setHonoursStanding("Y");
        gradStudent.setSchoolOfRecordId(schoolId);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);
        boolean isExceptionThrown = false;
        try {
            studentProcess.triggerGraduationBatchRun(EventType.XPROGRAM, studentID, pen, "Y", "Y", "123");
        } catch (Exception e) {
            isExceptionThrown = true;
        }
        assertThat(isExceptionThrown).isFalse();
    }

    @Test
    public void testTriggerGraduationBatchRun_withCurrentStatus_returnsNoBatchRun() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";
        UUID schoolId = UUID.randomUUID();

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStudentRecord gradStudent = new GraduationStudentRecord();
        gradStudent.setStudentID(studentID);
        gradStudent.setPen(pen);
        gradStudent.setProgram("2018-EN");
        gradStudent.setStudentGrade("12");
        gradStudent.setStudentStatus("CUR");
        gradStudent.setHonoursStanding("Y");
        gradStudent.setSchoolOfRecordId(schoolId);
        gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(new Date(System.currentTimeMillis() - 600000L)));

        when(this.restUtils.getStudentGradStatus(studentID.toString(), "123")).thenReturn(gradStudent);

        boolean isExceptionThrown = false;
        try {
            studentProcess.triggerGraduationBatchRun(EventType.XPROGRAM, studentID, pen, "Y", "Y", "123");
        } catch (Exception e) {
            isExceptionThrown = true;
        }
        assertThat(isExceptionThrown).isFalse();
    }

}
