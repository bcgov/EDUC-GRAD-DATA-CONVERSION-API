package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.constant.EventStatus;
import ca.bc.gov.educ.api.dataconversion.constant.EventType;
import ca.bc.gov.educ.api.dataconversion.entity.Event;
import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.EventRepository;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentDemographicsUpdateEventService;
import ca.bc.gov.educ.api.dataconversion.process.StudentProcess;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class StudentDemographicsUpdateEventServiceTest {

    @Autowired
    StudentDemographicsUpdateEventService studentDemographicsUpdateEventService;

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
    public void testProcessStudentDemographicsForGrad2018ENProgram_givenUpdated_UPD_DEMOG_then_returnsAPICallSuccess() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        // Program & School
        String program = "2018-EN";
        String mincode = "222333";

        String updateType = "UPD_DEMOG";

        // TraxUpdateInGrad
        TraxDemographicsUpdateDTO traxDemogUpdate = new TraxDemographicsUpdateDTO();
        traxDemogUpdate.setPen(pen);
        traxDemogUpdate.setFirstName("Test");
        traxDemogUpdate.setMiddleNames("and");
        traxDemogUpdate.setLastName("QA");
        traxDemogUpdate.setBirthday("19960601");

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_DEMOG.name());
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

        when(this.studentProcess.loadStudentData(eq(pen), any())).thenReturn(currentStudent);
        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));

        studentDemographicsUpdateEventService.processEvent(traxDemogUpdate, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }

    @Test
    public void testProcessStudentDemographics_whenException_isThrown_returnsAPICallError() throws Exception {
        // ID
        String pen = "111222333";
        String updateType = "UPD_DEMOG";

        TraxDemographicsUpdateDTO traxDemog = TraxDemographicsUpdateDTO.builder()
                .lastName("Test")
                .firstName("QA")
                .build();
        traxDemog.setPen(pen);

        // Event
        Event event = new Event();
        event.setEventType(EventType.UPD_DEMOG.name());
        event.setEventStatus(EventStatus.DB_COMMITTED.name());
        event.setActivityCode(updateType);
        event.setEventId(UUID.randomUUID());

        when(this.eventRepository.findByEventId(event.getEventId())).thenReturn(Optional.of(event));
        when(this.studentProcess.convertStudent(any(), any(), eq(false))).thenThrow(new RuntimeException("Test Exception is thrown!"));

        studentDemographicsUpdateEventService.processEvent(traxDemog, event);

        assertThat(event).isNotNull();
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.PROCESSED.name());
    }
}
