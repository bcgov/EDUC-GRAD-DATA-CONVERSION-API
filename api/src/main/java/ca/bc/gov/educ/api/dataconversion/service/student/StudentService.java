package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.entity.conv.ConvGradStudentSpecialProgramEntity;
import ca.bc.gov.educ.api.dataconversion.entity.student.GraduationStatusEntity;
import ca.bc.gov.educ.api.dataconversion.entity.student.StudentOptionalProgramEntity;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.conv.ConvGradStudentSpecialProgramRepository;
import ca.bc.gov.educ.api.dataconversion.repository.student.GraduationStatusRepository;

import ca.bc.gov.educ.api.dataconversion.repository.student.StudentOptionalProgramRepository;
import ca.bc.gov.educ.api.dataconversion.service.course.CourseService;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StudentService {

    private final GraduationStatusRepository graduationStatusRepository;
    private final StudentOptionalProgramRepository studentOptionalProgramRepository;

    private final RestUtils restUtils;
    private final CourseService courseService;

    @Autowired
    public StudentService(GraduationStatusRepository graduationStatusRepository,
                          StudentOptionalProgramRepository studentOptionalProgramRepository,
                          RestUtils restUtils,
                          CourseService courseService) {
        this.graduationStatusRepository = graduationStatusRepository;
        this.studentOptionalProgramRepository = studentOptionalProgramRepository;
        this.restUtils = restUtils;
        this.courseService = courseService;
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public ConvGradStudent convertStudent(ConvGradStudent convGradStudent, ConversionSummaryDTO summary) {
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        try {
            String accessToken = summary.getAccessToken();

            List<Student> students;
            try {
                // Call PEN Student API
                students = restUtils.getStudentsByPen(convGradStudent.getPen(), accessToken);
            } catch (Exception e) {
                ConversionError error = new ConversionError();
                error.setItem(convGradStudent.getPen());
                error.setReason("PEN Student API is failed: " + e.getLocalizedMessage());
                summary.getErrors().add(error);
                return null;
            }
            if (students == null || students.isEmpty()) {
                ConversionError error = new ConversionError();
                error.setItem(convGradStudent.getPen());
                error.setReason("PEN does not exist: PEN Student API returns empty response.");
                summary.getErrors().add(error);
                return null;
            }

            students.forEach(st -> {
                UUID studentID = UUID.fromString(st.getStudentID());
                Optional<GraduationStatusEntity> stuOptional = graduationStatusRepository.findById(studentID);
                GraduationStatusEntity gradStudentEntity = null;
                if (stuOptional.isPresent()) {
                    gradStudentEntity = stuOptional.get();
                    convertStudentData(convGradStudent, gradStudentEntity, summary);
                    gradStudentEntity.setUpdateDate(new Date());
                    gradStudentEntity = graduationStatusRepository.save(gradStudentEntity);
                    summary.setUpdatedCount(summary.getUpdatedCount() + 1L);
                    // process dependencies
                    try {
                        processSpecialPrograms(gradStudentEntity, accessToken);
                    } catch (Exception e) {
                        ConversionError error = new ConversionError();
                        error.setItem(convGradStudent.getPen());
                        error.setReason("Grad Program Management API is failed: " + e.getLocalizedMessage());
                        summary.getErrors().add(error);
                    }
                } else {
                    gradStudentEntity = new GraduationStatusEntity();
                    gradStudentEntity.setPen(st.getPen());
                    gradStudentEntity.setStudentID(studentID);
                    convertStudentData(convGradStudent, gradStudentEntity, summary);
                    graduationStatusRepository.save(gradStudentEntity);
                    summary.setAddedCount(summary.getAddedCount() + 1L);
                    // process dependencies
                    try {
                        processSpecialPrograms(gradStudentEntity, accessToken);
                    } catch (Exception e) {
                        ConversionError error = new ConversionError();
                        error.setItem(convGradStudent.getPen());
                        error.setReason("Grad Program Management API is failed: " + e.getLocalizedMessage());
                        summary.getErrors().add(error);
                    }
                }
            });
            return convGradStudent;
        } catch (Exception e) {
            ConversionError error = new ConversionError();
            error.setItem(convGradStudent.getPen());
            error.setReason("Unexpected Exception is occurred: " + e.getLocalizedMessage());
            summary.getErrors().add(error);
            return null;
        }
    }

    private void convertStudentData(ConvGradStudent student, GraduationStatusEntity studentEntity, ConversionSummaryDTO summary) {
        studentEntity.setGpa(student.getGpa());
        studentEntity.setHonoursStanding(student.getHonoursStanding());
        determineProgram(student, summary);
        studentEntity.setProgram(student.getProgram());
        studentEntity.setProgramCompletionDate(student.getProgramCompletionDate());
        studentEntity.setSchoolOfRecord(student.getSchoolOfRecord());
        studentEntity.setSchoolAtGrad(student.getSchoolAtGrad());
        studentEntity.setRecalculateGradStatus(student.getRecalculateGradStatus());
        studentEntity.setStudentGradData(student.getStudentGradData());
        studentEntity.setStudentGrade(student.getStudentGrade());
        studentEntity.setStudentStatus(student.getStudentStatus());
    }

    private void processSpecialPrograms(GraduationStatusEntity student, String accessToken) {
        // French Immersion for 2018-EN
        if (StringUtils.equals(student.getProgram(), "2018-EN")) {
            if (courseService.isFrenchImmersionCourse(student.getPen())) {
                StudentOptionalProgramEntity entity = new StudentOptionalProgramEntity();
                entity.setPen(student.getPen());
                entity.setStudentID(student.getStudentID());
                // Call Grad Program Management API
                GradSpecialProgram gradSpecialProgram = restUtils.getGradSpecialProgram("2018-EN", "FI", accessToken);
                if (gradSpecialProgram != null && gradSpecialProgram.getOptionalProgramID() != null) {
                    entity.setOptionalProgramID(gradSpecialProgram.getOptionalProgramID());
                    Optional<StudentOptionalProgramEntity> stdSpecialProgramOptional = studentOptionalProgramRepository.findByStudentIDAndOptionalProgramID(student.getStudentID(), gradSpecialProgram.getOptionalProgramID());
                    if (!stdSpecialProgramOptional.isPresent()) {
                        studentOptionalProgramRepository.save(entity);
                    }
                }
            }
        }
    }

    private void determineProgram(ConvGradStudent student, ConversionSummaryDTO summary) {
        switch(student.getGraduationRequestYear()) {
            case "2018":
                if (student.getSchoolOfRecord().startsWith("093")) {
                    student.setProgram("2018-PF");
                    summary.increment("2018-PF");
                } else {
                    student.setProgram("2018-EN");
                    summary.increment("2018-EN");
                }
                break;
            case "2004":
                student.setProgram("2004");
                summary.increment("2004");
                break;
            case "1996":
                student.setProgram("1996");
                summary.increment("1996");
                break;
            case "1986":
                student.setProgram("1986");
                summary.increment("1986");
                break;
            case "1950":
                if (StringUtils.equals(student.getStudentGrade(), "AD")) {
                    student.setProgram("1950");
                    summary.increment("1950");
                } else if (StringUtils.equals(student.getStudentGrade(), "AN")) {
                    student.setProgram("NOPROG");
                    summary.increment("NOPROG");
                } else {
                    // error
                    ConversionError error = new ConversionError();
                    error.setItem(student.getPen());
                    error.setReason("Program is not found for 1950 / " + student.getStudentGrade());
                    summary.getErrors().add(error);
                }
                break;
            case "SCCP":
                student.setProgram("SCCP");
                summary.increment("SCCP");
                break;
            default:
                break;
        }
    }

}
