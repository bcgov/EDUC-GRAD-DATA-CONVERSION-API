package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.constant.EventStatus;
import ca.bc.gov.educ.api.dataconversion.constant.EventType;
import ca.bc.gov.educ.api.dataconversion.entity.Event;
import ca.bc.gov.educ.api.dataconversion.messaging.NatsConnection;
import ca.bc.gov.educ.api.dataconversion.messaging.jetstream.Subscriber;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.Student;
import ca.bc.gov.educ.api.dataconversion.process.StudentProcess;
import ca.bc.gov.educ.api.dataconversion.repository.EventRepository;
import ca.bc.gov.educ.api.dataconversion.service.student.NewStudentEventService;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class StudentServiceTest {

    @Autowired
    StudentService studentService;

    @MockBean
    RestUtils restUtils;

    @MockBean
    WebClient webClient;

    // NATS
    @MockBean
    private NatsConnection natsConnection;
    @MockBean
    private Subscriber subscriber;

    @Test
    public void testGetStudentByPen_given_PEN_returnsAPICallSuccess() {
        String pen = "123456789";
        String accessToken = "Bearer accesstoken";

        Student student = new Student();
        student.setPen(pen);
        List<Student> penList = List.of(student);

        when(this.restUtils.getStudentsByPen(pen, accessToken)).thenReturn(penList);
        studentService.getStudentByPen(pen, accessToken);

        assertThat(penList).isNotNull();
    }

    @Test
    public void testGetStudentByPen_given_PEN_returnsNULL() {
        String pen = "123456789";
        String accessToken = "Bearer accesstoken";

        Student student = new Student();
        student.setPen(pen);
        List<Student> penList = List.of(student);

        when(this.restUtils.getStudentsByPen(pen, accessToken)).thenReturn(new ArrayList<Student>());
        Student s = studentService.getStudentByPen(pen, accessToken);

        assertThat(s).isNull();
    }

    @Test(expected = Exception.class)
    public void testGetStudentByPen_given_PEN_throws_exception() {
        String pen = "123456789";
        String accessToken = "Bearer accesstoken";

        Student student = new Student();
        student.setPen(pen);
        List<Student> penList = List.of(student);

        when(this.restUtils.getStudentsByPen(pen, accessToken)).thenThrow(Exception.class);
        Student s = studentService.getStudentByPen(pen, accessToken);
    }

    @Test
    public void testCascadeDeleteStudentBy_given_PEN_returnsAPICallSuccess() {
        String pen = "123456789";
        String studentID = "0a614e84-7e27-1815-817e-fad384090090";
        String accessToken = "Bearer accesstoken";

        Student student = new Student();
        student.setPen(pen);
        student.setStudentID(studentID);
        List<Student> penList = List.of(student);

        when(this.restUtils.getStudentsByPen(pen, accessToken)).thenReturn(penList);
        studentService.cascadeDeleteStudent(pen, accessToken);
        Mockito.verify(restUtils).removeAllStudentRelatedData(UUID.fromString(studentID), accessToken);
    }
}
