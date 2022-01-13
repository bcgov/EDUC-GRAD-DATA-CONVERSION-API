package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.UUID;

@Data
public class StudentContext {
    // Trax Student
    private StudentTraxDTO traxStudent;

    // Grad Student
    private StudentGradDTO gradStudent;

    // Workflow flags

    // Methods
    public void loadTraxStudent(String pen, StudentCommonDTO student) {
        traxStudent = new StudentTraxDTO();
        if (student != null) {
            BeanUtils.copyProperties(student, traxStudent);
        }
        traxStudent.setPen(pen);
    }

    public void loadGradStudent(UUID studentID, StudentCommonDTO student) {
        gradStudent = new StudentGradDTO();
        if (student != null) {
            BeanUtils.copyProperties(student, gradStudent);
        }
        gradStudent.setStudentID(studentID);
    }
}
