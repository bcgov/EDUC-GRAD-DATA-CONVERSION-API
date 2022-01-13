package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
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

    // Optional Programs
    public List<String> getAddedOptionalPrograms() {
        List<String> newOptionalPrograms = new ArrayList<>();

        return newOptionalPrograms;
    }

    public List<String> getRemovedOptionalPrograms() {
        List<String> removedOptionalPrograms = new ArrayList<>();

        return removedOptionalPrograms;
    }

    // Career Programs
    public List<String> getAddedCareerPrograms() {
        List<String> newCareerPrograms = new ArrayList<>();

        return newCareerPrograms;
    }

    public List<String> getRemovedCareerPrograms() {
        List<String> removedCareerPrograms = new ArrayList<>();

        return removedCareerPrograms;
    }
}
