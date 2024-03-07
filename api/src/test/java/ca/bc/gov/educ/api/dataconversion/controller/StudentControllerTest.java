package ca.bc.gov.educ.api.dataconversion.controller;

import ca.bc.gov.educ.api.dataconversion.model.Student;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentService;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
public class StudentControllerTest {

    @Mock
    private StudentService studentService;

    @Test
    public void testGetGradStudentByPenFromStudentAPI() {
        // ID
        UUID studentID = UUID.randomUUID();

        String pen = "123456789";
        String accessToken = "Bearer accesstoken";

        Student student = new Student();

        Mockito.when(studentService.getStudentByPen(pen,accessToken)).thenReturn(student);
        studentService.getStudentByPen(pen,accessToken.replaceAll("Bearer ", accessToken));
        Mockito.verify(studentService).getStudentByPen(pen,accessToken);

    }
}
