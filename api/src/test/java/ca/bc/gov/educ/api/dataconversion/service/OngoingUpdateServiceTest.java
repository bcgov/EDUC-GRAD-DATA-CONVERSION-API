package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.constant.EventStatus;
import ca.bc.gov.educ.api.dataconversion.constant.EventType;
import ca.bc.gov.educ.api.dataconversion.entity.conv.Event;
import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.conv.EventRepository;
import ca.bc.gov.educ.api.dataconversion.service.student.OngoingUpdateService;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class OngoingUpdateServiceTest {

    @Autowired
    OngoingUpdateService ongoingUpdateService;

    @MockBean
    EventRepository eventRepository;

    @MockBean
    StudentService studentService;

    @MockBean
    RestUtils restUtils;

    // NATS
    @MockBean
    private NatsConnection natsConnection;
    @MockBean
    private Subscriber subscriber;

    @Autowired
    private EducGradDataConversionApiConstants constants;

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_STUDENT_returnsAPICallFailure() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "UPD_GRAD";

        // TraxUpdateInGrad
        TraxUpdateInGrad traxUpdateInGrad = new TraxUpdateInGrad();
        traxUpdateInGrad.setPen(pen);
        traxUpdateInGrad.setUpdateType(updateType);

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPDATE_TRAX_STUDENT_MASTER.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("A");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");
        currentStudent.getProgramCodes().add("FI");
        // Courses
        StudentCourse course1 = new StudentCourse();
        course1.setCourseCode("Test");
        course1.setCourseLevel("12");
        course1.setCourseName("Test Course");
        course1.setCreditsUsedForGrad(Integer.valueOf("4"));
        course1.setCompletedCoursePercentage(Double.valueOf("92.00"));
        course1.setCredits(Integer.valueOf("4"));
        course1.setPen(pen);
        currentStudent.getCourses().add(course1);

        when(this.studentService.loadStudentData(pen, "123")).thenReturn(currentStudent);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("12")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .schoolAtGrad(mincode)
                .graduationRequestYear("2018")
                .programCodes(Arrays.asList("XC","FI")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(pen, summary.getAccessToken())).thenReturn(Arrays.asList(traxStudent));

        ResponseObj res = new ResponseObj();
        res.setAccess_token("accessToken");
        res.setRefresh_token("refreshToken");
        when(this.restUtils.getTokenResponseObject()).thenReturn(res);
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        ongoingUpdateService.processEvent(traxUpdateInGrad, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenNew_STUDENT_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "NEWSTUDENT";

        // TraxUpdateInGrad
        TraxUpdateInGrad traxUpdateInGrad = new TraxUpdateInGrad();
        traxUpdateInGrad.setPen(pen);
        traxUpdateInGrad.setUpdateType(updateType);

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPDATE_TRAX_STUDENT_MASTER.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("11")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .schoolAtGrad(mincode)
                .graduationRequestYear("2018")
                .programCodes(Arrays.asList("XC","FI")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        ongoingUpdateService.processEvent(traxUpdateInGrad, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_STUDENT_whenGradeAndSchoolAreChanged_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String newMincode = "333444";

        String updateType = "UPD_GRAD";

        // TraxUpdateInGrad
        TraxUpdateInGrad traxUpdateInGrad = new TraxUpdateInGrad();
        traxUpdateInGrad.setPen(pen);
        traxUpdateInGrad.setUpdateType(updateType);

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPDATE_TRAX_STUDENT_MASTER.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("A");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");
        currentStudent.getProgramCodes().add("FI");
        // Courses
        StudentCourse course1 = new StudentCourse();
        course1.setCourseCode("Test");
        course1.setCourseLevel("12");
        course1.setCourseName("Test Course");
        course1.setCreditsUsedForGrad(Integer.valueOf("4"));
        course1.setCompletedCoursePercentage(Double.valueOf("92.00"));
        course1.setCredits(Integer.valueOf("4"));
        course1.setPen(pen);
        currentStudent.getCourses().add(course1);

        when(this.studentService.loadStudentData(eq(pen), any())).thenReturn(currentStudent);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("11")
                .studentStatus("A")
                .schoolOfRecord(newMincode)
                .schoolAtGrad(newMincode)
                .graduationRequestYear("2018")
                .programCodes(Arrays.asList("XC","FI")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        ongoingUpdateService.processEvent(traxUpdateInGrad, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_STUDENT_whenProgramIsChanged_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-PF";
        String mincode = "222333";

        String newProgram = "2018-EN";
        String newMincode = "333444";

        String updateType = "UPD_GRAD";

        // TraxUpdateInGrad
        TraxUpdateInGrad traxUpdateInGrad = new TraxUpdateInGrad();
        traxUpdateInGrad.setPen(pen);
        traxUpdateInGrad.setUpdateType(updateType);

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPDATE_TRAX_STUDENT_MASTER.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("A");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");
        currentStudent.getProgramCodes().add("DD");
        // Courses
        StudentCourse course1 = new StudentCourse();
        course1.setCourseCode("Test");
        course1.setCourseLevel("12");
        course1.setCourseName("Test Course");
        course1.setCreditsUsedForGrad(Integer.valueOf("4"));
        course1.setCompletedCoursePercentage(Double.valueOf("92.00"));
        course1.setCredits(Integer.valueOf("4"));
        course1.setPen(pen);
        currentStudent.getCourses().add(course1);

        when(this.studentService.loadStudentData(eq(pen), any())).thenReturn(currentStudent);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(newProgram)
                .studentGrade("12")
                .studentStatus("A")
                .schoolOfRecord(newMincode)
                .schoolAtGrad(newMincode)
                .graduationRequestYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        ongoingUpdateService.processEvent(traxUpdateInGrad, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_XPROGRAM_whenCourseIsSame_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "XPROGRAM";

        // TraxUpdateInGrad
        TraxUpdateInGrad traxUpdateInGrad = new TraxUpdateInGrad();
        traxUpdateInGrad.setPen(pen);
        traxUpdateInGrad.setUpdateType(updateType);

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPDATE_TRAX_STUDENT_MASTER.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("A");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");
        currentStudent.getProgramCodes().add("FI");
        // Courses
        StudentCourse course1 = new StudentCourse();
        course1.setCourseCode("Test");
        course1.setCourseLevel("12");
        course1.setCourseName("Test Course");
        course1.setCreditsUsedForGrad(Integer.valueOf("4"));
        course1.setCompletedCoursePercentage(Double.valueOf("92.00"));
        course1.setCredits(Integer.valueOf("4"));
        course1.setPen(pen);
        currentStudent.getCourses().add(course1);

        when(this.studentService.loadStudentData(eq(pen), any())).thenReturn(currentStudent);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("12")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .graduationRequestYear("2018")
                .programCodes(Arrays.asList("XC","FI")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        ongoingUpdateService.processEvent(traxUpdateInGrad, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_XPROGRAM_whenNewCareerProgramIsRemoved_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "XPROGRAM";

        // TraxUpdateInGrad
        TraxUpdateInGrad traxUpdateInGrad = new TraxUpdateInGrad();
        traxUpdateInGrad.setPen(pen);
        traxUpdateInGrad.setUpdateType(updateType);

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPDATE_TRAX_STUDENT_MASTER.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("A");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");

        // Courses
        StudentCourse course1 = new StudentCourse();
        course1.setCourseCode("Test");
        course1.setCourseLevel("12");
        course1.setCourseName("Test Course");
        course1.setCreditsUsedForGrad(Integer.valueOf("4"));
        course1.setCompletedCoursePercentage(Double.valueOf("92.00"));
        course1.setCredits(Integer.valueOf("4"));
        course1.setPen(pen);
        currentStudent.getCourses().add(course1);

        when(this.studentService.loadStudentData(eq(pen), any())).thenReturn(currentStudent);

        when(this.restUtils.getOptionalProgram(eq(program), eq("XC"), any())).thenReturn(null);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("12")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .graduationRequestYear("2018")
                .programCodes(new ArrayList<>()).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        ongoingUpdateService.processEvent(traxUpdateInGrad, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_XPROGRAM_whenNewCareerProgramIsAdded_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "XPROGRAM";

        // TraxUpdateInGrad
        TraxUpdateInGrad traxUpdateInGrad = new TraxUpdateInGrad();
        traxUpdateInGrad.setPen(pen);
        traxUpdateInGrad.setUpdateType(updateType);

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPDATE_TRAX_STUDENT_MASTER.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("A");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        // None

        // Courses
        StudentCourse course1 = new StudentCourse();
        course1.setCourseCode("Test");
        course1.setCourseLevel("12");
        course1.setCourseName("Test Course");
        course1.setCreditsUsedForGrad(Integer.valueOf("4"));
        course1.setCompletedCoursePercentage(Double.valueOf("92.00"));
        course1.setCredits(Integer.valueOf("4"));
        course1.setPen(pen);
        currentStudent.getCourses().add(course1);

        when(this.studentService.loadStudentData(eq(pen), any())).thenReturn(currentStudent);

        when(this.restUtils.getOptionalProgram(eq(program), eq("XC"), any())).thenReturn(null);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("12")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .graduationRequestYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        ongoingUpdateService.processEvent(traxUpdateInGrad, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_XPROGRAM_whenCourseIsRemoved_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "XPROGRAM";

        // TraxUpdateInGrad
        TraxUpdateInGrad traxUpdateInGrad = new TraxUpdateInGrad();
        traxUpdateInGrad.setPen(pen);
        traxUpdateInGrad.setUpdateType(updateType);

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPDATE_TRAX_STUDENT_MASTER.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("A");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        currentStudent.getProgramCodes().add("ID");

        // Courses
        StudentCourse course1 = new StudentCourse();
        course1.setCourseCode("Test");
        course1.setCourseLevel("12");
        course1.setCourseName("Test Course");
        course1.setCreditsUsedForGrad(Integer.valueOf("4"));
        course1.setCompletedCoursePercentage(Double.valueOf("92.00"));
        course1.setCredits(Integer.valueOf("4"));
        course1.setPen(pen);
        currentStudent.getCourses().add(course1);

        when(this.studentService.loadStudentData(eq(pen), any())).thenReturn(currentStudent);

        when(this.restUtils.getOptionalProgram(eq(program), eq("XC"), any())).thenReturn(null);

        OptionalProgram optionalProgram = new OptionalProgram();
        optionalProgram.setOptionalProgramID(UUID.randomUUID());
        optionalProgram.setOptProgramCode("ID");
        optionalProgram.setOptionalProgramName("IB Diploma");
        optionalProgram.setGraduationProgramCode(program);
        when(this.restUtils.getOptionalProgram(eq(program), eq("ID"), any())).thenReturn(optionalProgram);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("12")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .graduationRequestYear("2018")
                .programCodes(Arrays.asList("ID")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        ongoingUpdateService.processEvent(traxUpdateInGrad, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_XPROGRAM_whenNewCourseIsAddedAndRemoved_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "XPROGRAM";

        // TraxUpdateInGrad
        TraxUpdateInGrad traxUpdateInGrad = new TraxUpdateInGrad();
        traxUpdateInGrad.setPen(pen);
        traxUpdateInGrad.setUpdateType(updateType);

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPDATE_TRAX_STUDENT_MASTER.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("A");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");
        currentStudent.getProgramCodes().add("ID");
        currentStudent.getProgramCodes().add("FI");
        // Courses
        StudentCourse course1 = new StudentCourse();
        course1.setCourseCode("Test");
        course1.setCourseLevel("12");
        course1.setCourseName("Test Course");
        course1.setCreditsUsedForGrad(Integer.valueOf("4"));
        course1.setCompletedCoursePercentage(Double.valueOf("92.00"));
        course1.setCredits(Integer.valueOf("4"));
        course1.setPen(pen);
        currentStudent.getCourses().add(course1);

        when(this.studentService.loadStudentData(eq(pen), any())).thenReturn(currentStudent);

        when(this.restUtils.getOptionalProgram(eq(program), eq("XC"), any())).thenReturn(null);

        OptionalProgram optionalProgram1 = new OptionalProgram();
        optionalProgram1.setOptionalProgramID(UUID.randomUUID());
        optionalProgram1.setOptProgramCode("FI");
        optionalProgram1.setOptionalProgramName("French Immersion");
        optionalProgram1.setGraduationProgramCode(program);
        when(this.restUtils.getOptionalProgram(eq(program), eq("FI"), any())).thenReturn(optionalProgram1);

        OptionalProgram optionalProgram2 = new OptionalProgram();
        optionalProgram2.setOptionalProgramID(UUID.randomUUID());
        optionalProgram2.setOptProgramCode("AD");
        optionalProgram2.setOptionalProgramName("Advance Placement");
        optionalProgram2.setGraduationProgramCode(program);
        when(this.restUtils.getOptionalProgram(eq(program), eq("AD"), any())).thenReturn(optionalProgram2);

        OptionalProgram optionalProgram3 = new OptionalProgram();
        optionalProgram3.setOptionalProgramID(UUID.randomUUID());
        optionalProgram3.setOptProgramCode("ID");
        optionalProgram3.setOptionalProgramName("IB Diploma");
        optionalProgram3.setGraduationProgramCode(program);
        when(this.restUtils.getOptionalProgram(eq(program), eq("ID"), any())).thenReturn(optionalProgram3);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("12")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .graduationRequestYear("2018")
                .programCodes(Arrays.asList("XC","AD")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        ongoingUpdateService.processEvent(traxUpdateInGrad, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_FI11ADD_whenNewCourseIsAdded_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "FI11ADD";

        // TraxUpdateInGrad
        TraxUpdateInGrad traxUpdateInGrad = new TraxUpdateInGrad();
        traxUpdateInGrad.setPen(pen);
        traxUpdateInGrad.setUpdateType(updateType);

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPDATE_TRAX_STUDENT_MASTER.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("A");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");
        currentStudent.getProgramCodes().add("ID");

        // Courses
        StudentCourse course1 = new StudentCourse();
        course1.setCourseCode("Test");
        course1.setCourseLevel("12");
        course1.setCourseName("Test Course");
        course1.setCreditsUsedForGrad(Integer.valueOf("4"));
        course1.setCompletedCoursePercentage(Double.valueOf("92.00"));
        course1.setCredits(Integer.valueOf("4"));
        course1.setPen(pen);
        currentStudent.getCourses().add(course1);

        when(this.studentService.loadStudentData(eq(pen), any())).thenReturn(currentStudent);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("12")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .graduationRequestYear("2018")
                .programCodes(Arrays.asList("XC","FI")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.studentService.hasAnyFrenchImmersionCourse(any(), any(), any(), any())).thenReturn(true);
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        ongoingUpdateService.processEvent(traxUpdateInGrad, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_FI11DELETE_whenCourseIsRemoved_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "FI11DELETE";

        // TraxUpdateInGrad
        TraxUpdateInGrad traxUpdateInGrad = new TraxUpdateInGrad();
        traxUpdateInGrad.setPen(pen);
        traxUpdateInGrad.setUpdateType(updateType);

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPDATE_TRAX_STUDENT_MASTER.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("A");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);
        // Optional Program Codes
        currentStudent.getProgramCodes().add("XC");
        currentStudent.getProgramCodes().add("FI");

        // Courses
        StudentCourse course1 = new StudentCourse();
        course1.setCourseCode("Test");
        course1.setCourseLevel("12");
        course1.setCourseName("Test Course");
        course1.setCreditsUsedForGrad(Integer.valueOf("4"));
        course1.setCompletedCoursePercentage(Double.valueOf("92.00"));
        course1.setCredits(Integer.valueOf("4"));
        course1.setPen(pen);
        currentStudent.getCourses().add(course1);

        when(this.studentService.loadStudentData(eq(pen), any())).thenReturn(currentStudent);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("12")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .graduationRequestYear("2018")
                .programCodes(Arrays.asList("XC")).build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.studentService.hasAnyFrenchImmersionCourse(any(), any(), any(), any())).thenReturn(false);
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        ongoingUpdateService.processEvent(traxUpdateInGrad, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentForGrad2018ENProgram_givenUpdated_UPD_DEMOG_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "UPD_DEMOG";

        // TraxUpdateInGrad
        TraxUpdateInGrad traxUpdateInGrad = new TraxUpdateInGrad();
        traxUpdateInGrad.setPen(pen);
        traxUpdateInGrad.setUpdateType(updateType);

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPDATE_TRAX_STUDENT_MASTER.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("A");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);

        when(this.studentService.loadStudentData(eq(pen), any())).thenReturn(currentStudent);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("12")
                .studentStatus("A")
                .schoolOfRecord(mincode)
                .graduationRequestYear("2018").build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        ongoingUpdateService.processEvent(traxUpdateInGrad, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

}
