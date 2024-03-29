package ca.bc.gov.educ.api.dataconversion.controller;

import ca.bc.gov.educ.api.dataconversion.model.Student;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentService;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
public class StudentControllerTest {

    @Mock
    private StudentService studentService;

    @Test
    public void testGetGradStudentByPenFromStudentAPI() {

        String pen = "123456789";
        String accessToken = "Bearer accesstoken";
        Student student = new Student();

        Mockito.when(studentService.getStudentByPen(pen,accessToken.replaceAll("Bearer ", accessToken))).thenReturn(student);
        studentService.getStudentByPen(pen,accessToken.replaceAll("Bearer ", accessToken));
        Mockito.verify(studentService).getStudentByPen(pen, accessToken.replaceAll("Bearer ", accessToken));
    }

    @Test
    public void testCascadeDeleteStudent() {

        String pen = "123456789";
        String accessToken = "Bearer accesstoken";
        Student student = new Student();

        Mockito.when(studentService.cascadeDeleteStudent(pen, accessToken.replaceAll("Bearer ", accessToken))).thenReturn(pen);
        studentService.cascadeDeleteStudent(pen,accessToken.replaceAll("Bearer ", accessToken));
        Mockito.verify(studentService).cascadeDeleteStudent(pen, accessToken.replaceAll("Bearer ", accessToken));

    }
}
