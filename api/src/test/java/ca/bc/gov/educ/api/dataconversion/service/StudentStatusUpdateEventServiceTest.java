package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.constant.EventStatus;
import ca.bc.gov.educ.api.dataconversion.constant.EventType;
import ca.bc.gov.educ.api.dataconversion.entity.Event;
import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.EventRepository;
import ca.bc.gov.educ.api.dataconversion.process.StudentProcess;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentStatusUpdateEventService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

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
public class StudentStatusUpdateEventServiceTest {

    @Autowired
    StudentStatusUpdateEventService studentStatusUpdateEventService;

    @MockBean
    EventRepository eventRepository;

    @MockBean
    StudentProcess studentProcess;

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
    public void testProcessStudentStatusForGrad2018ENProgram_givenUpdated_UPD_STD_STATUS_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "UPD_STD_STATUS";

        // TraxStudentStatusUpdateDTO
        TraxStudentStatusUpdateDTO traxStudentStatusUpdate = new TraxStudentStatusUpdateDTO();
        traxStudentStatusUpdate.setPen(pen);
        traxStudentStatusUpdate.setStudentStatus("T");
        traxStudentStatusUpdate.setArchiveFlag("I");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_STD_STATUS.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        // current GRAD Student
        StudentGradDTO currentStudent = new StudentGradDTO();
        // GraduationStudentRecord
        currentStudent.setStudentID(studentID);
        currentStudent.setProgram(program);
        currentStudent.setStudentGrade("12");
        currentStudent.setStudentStatus("CUR");
        currentStudent.setSchoolOfRecord(mincode);
        currentStudent.setSchoolAtGrad(mincode);

        when(this.studentProcess.loadStudentData(eq(pen), any())).thenReturn(currentStudent);

        // ConvGradStudent = traxStudent with the recent updated info
        ConvGradStudent traxStudent = ConvGradStudent.builder().pen(pen)
                .program(program)
                .studentGrade("12")
                .studentStatus("A")
                .archiveFlag("A")
                .schoolOfRecord(mincode)
                .graduationRequirementYear("2018").build();
        ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getTraxStudentMasterDataByPen(eq(pen), any())).thenReturn(Arrays.asList(traxStudent));
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentStatusUpdateEventService.processEvent(traxStudentStatusUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentStatus_whenException_isThrown_returnsAPICallError() throws Exception {
        // ID
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "UPD_STD_STATUS";

        TraxStudentStatusUpdateDTO traxStudentUpdate = TraxStudentStatusUpdateDTO.builder()
                .studentStatus("A")
                .archiveFlag("I")
                .build();
        traxStudentUpdate.setPen(pen);

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_STD_STATUS.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));
        when(this.studentProcess.convertStudent(any(), any(), eq(false), eq(true))).thenThrow(new RuntimeException("Test Exception is thrown!"));

        studentStatusUpdateEventService.processEvent(traxStudentUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }
}
