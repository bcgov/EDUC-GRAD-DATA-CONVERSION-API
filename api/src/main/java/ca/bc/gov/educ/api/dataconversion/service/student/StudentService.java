package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.entity.program.CareerProgramEntity;
import ca.bc.gov.educ.api.dataconversion.entity.student.GraduationStatusEntity;
import ca.bc.gov.educ.api.dataconversion.entity.student.StudentCareerProgramEntity;
import ca.bc.gov.educ.api.dataconversion.entity.student.StudentOptionalProgramEntity;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.student.GraduationStatusRepository;

import ca.bc.gov.educ.api.dataconversion.repository.student.StudentCareerProgramRepository;
import ca.bc.gov.educ.api.dataconversion.repository.student.StudentOptionalProgramRepository;
import ca.bc.gov.educ.api.dataconversion.service.course.CourseService;
import ca.bc.gov.educ.api.dataconversion.service.program.ProgramService;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class StudentService {

    private static final List<String> OPTIONAL_PROGRAM_CODES = Arrays.asList("FI", "AD", "BC", "BD");

    private final GraduationStatusRepository graduationStatusRepository;
    private final StudentOptionalProgramRepository studentOptionalProgramRepository;
    private final StudentCareerProgramRepository studentCareerProgramRepository;

    private final RestUtils restUtils;
    private final CourseService courseService;
    private final ProgramService programService;

    @Autowired
    public StudentService(GraduationStatusRepository graduationStatusRepository,
                          StudentOptionalProgramRepository studentOptionalProgramRepository,
                          StudentCareerProgramRepository studentCareerProgramRepository,
                          RestUtils restUtils,
                          CourseService courseService,
                          ProgramService programService) {
        this.graduationStatusRepository = graduationStatusRepository;
        this.studentOptionalProgramRepository = studentOptionalProgramRepository;
        this.studentCareerProgramRepository = studentCareerProgramRepository;
        this.restUtils = restUtils;
        this.courseService = courseService;
        this.programService = programService;
    }

    @Transactional(transactionManager = "studentTransactionManager")
    public ConvGradStudent convertStudent(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary) {
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        try {
            String accessToken = summary.getAccessToken();

            List<Student> students;
            try {
                // Call PEN Student API
                students = restUtils.getStudentsByPen(convGradStudent.getPen(), accessToken);
            } catch (Exception e) {
                ConversionAlert error = new ConversionAlert();
                error.setItem(convGradStudent.getPen());
                error.setReason("PEN Student API is failed: " + e.getLocalizedMessage());
                summary.getErrors().add(error);
                return null;
            }
            if (students == null || students.isEmpty()) {
                ConversionAlert error = new ConversionAlert();
                error.setItem(convGradStudent.getPen());
                error.setReason("PEN does not exist: PEN Student API returns empty response.");
                summary.getErrors().add(error);
                return null;
            }

            students.forEach(st -> {
                UUID studentID = UUID.fromString(st.getStudentID());
                Optional<GraduationStatusEntity> stuOptional = graduationStatusRepository.findById(studentID);
                GraduationStatusEntity gradStudentEntity;
                if (stuOptional.isPresent()) {
                    gradStudentEntity = stuOptional.get();
                    gradStudentEntity.setPen(st.getPen());
                    convertStudentData(convGradStudent, gradStudentEntity, summary);
                    gradStudentEntity.setUpdateDate(new Date());
                    gradStudentEntity = graduationStatusRepository.save(gradStudentEntity);
                    summary.setUpdatedCount(summary.getUpdatedCount() + 1L);
                } else {
                    gradStudentEntity = new GraduationStatusEntity();
                    gradStudentEntity.setPen(st.getPen());
                    gradStudentEntity.setStudentID(studentID);
                    convertStudentData(convGradStudent, gradStudentEntity, summary);
                    // if year "1950" without "AD" or "AN", then program is null
                    if (StringUtils.isNotBlank(gradStudentEntity.getProgram())) {
                        graduationStatusRepository.save(gradStudentEntity);
                        summary.setAddedCount(summary.getAddedCount() + 1L);
                    }
                }
                // process dependencies
                processSpecialPrograms(gradStudentEntity, convGradStudent.getProgramCodes(), accessToken, summary);
                processProgramCodes(gradStudentEntity, convGradStudent.getProgramCodes(), accessToken, summary);
            });
            return convGradStudent;
        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setItem(convGradStudent.getPen());
            error.setReason("Unexpected Exception is occurred: " + e.getLocalizedMessage());
            summary.getErrors().add(error);
            return null;
        }
    }

    private void convertStudentData(ConvGradStudent student, GraduationStatusEntity studentEntity, ConversionStudentSummaryDTO summary) {
        if (determineProgram(student, summary)) {
            studentEntity.setProgram(student.getProgram());
        }

        // for testing purpose
        // always set to NULL, even for graduated students.  => GRAD grad algorithm will be run at some point
        studentEntity.setGpa(null);
        studentEntity.setHonoursStanding(null);
        studentEntity.setProgramCompletionDate(null);
        studentEntity.setStudentGradData(null);
//        studentEntity.setGpa(student.getGpa());
//        studentEntity.setHonoursStanding(student.getHonoursStanding());
//        studentEntity.setProgramCompletionDate(student.getProgramCompletionDate());
//        studentEntity.setStudentGradData(student.getStudentGradData());

        studentEntity.setSchoolOfRecord(student.getSchoolOfRecord());
        studentEntity.setSchoolAtGrad(student.getSchoolAtGrad());
        studentEntity.setRecalculateGradStatus(student.getRecalculateGradStatus());

        studentEntity.setStudentGrade(student.getStudentGrade());
        studentEntity.setStudentStatus(student.getStudentStatus());
    }

    private void processSpecialPrograms(GraduationStatusEntity student, List<String> programCodes, String accessToken, ConversionStudentSummaryDTO summary) {
        if (StringUtils.isBlank(student.getProgram())) {
            return;
        }

        // Dual Dogwood for yyyy-PF
        if (student.getProgram().endsWith("-PF")) {
            createStudentOptionalProgram("DD", student, accessToken, summary);
        }

        // French Immersion for 2018-EN, 2004-EN
        if (student.getProgram().endsWith("-EN")) {
            if (courseService.isFrenchImmersionCourse(student.getPen())) {
                createStudentOptionalProgram("FI", student, accessToken, summary);
            }
        }
    }

    private void processProgramCodes(GraduationStatusEntity student, List<String> programCodes, String accessToken, ConversionStudentSummaryDTO summary) {
        if (StringUtils.isNotBlank(student.getProgram()) && !programCodes.isEmpty()) {
            for (String programCode : programCodes) {
                if (isOptionalProgramCode(programCode)) {
                    createStudentOptionalProgram(programCode, student, accessToken, summary);
                } else {
                    if (StringUtils.equals(student.getProgram(), "SCCP")) {
                        ConversionAlert error = new ConversionAlert();
                        error.setLevel(ConversionAlert.AlertLevelEnum.WARNING);
                        error.setItem(student.getPen());
                        error.setReason(" [ SCCP | CP ] is found => skip both student career program and optional program creation process.");
                        summary.getErrors().add(error);
                    } else if (createStudentCareerProgram(programCode, student, summary)) {
                        createStudentOptionalProgram("CP", student, accessToken, summary);
                    }
                }
            }
        }
    }

    private boolean isOptionalProgramCode(String code) {
       return OPTIONAL_PROGRAM_CODES.contains(code);
    }

    private boolean createStudentOptionalProgram(String optionalProgramCode, GraduationStatusEntity student, String accessToken, ConversionStudentSummaryDTO summary) {
        StudentOptionalProgramEntity entity = new StudentOptionalProgramEntity();
        entity.setPen(student.getPen());
        entity.setStudentID(student.getStudentID());

        GradSpecialProgram gradSpecialProgram;
        // Call Grad Program API
        try {
            gradSpecialProgram = restUtils.getGradSpecialProgram(student.getProgram(), optionalProgramCode, accessToken);
        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setLevel(ConversionAlert.AlertLevelEnum.WARNING);
            error.setItem(student.getPen());
            error.setReason("Grad Program API is failed: " + e.getLocalizedMessage());
            summary.getErrors().add(error);
            return false;
        }
        if (gradSpecialProgram != null && gradSpecialProgram.getOptionalProgramID() != null) {
            entity.setOptionalProgramID(gradSpecialProgram.getOptionalProgramID());
            Optional<StudentOptionalProgramEntity> stdSpecialProgramOptional = studentOptionalProgramRepository.findByStudentIDAndOptionalProgramID(student.getStudentID(), gradSpecialProgram.getOptionalProgramID());
            if (stdSpecialProgramOptional.isPresent()) {
                StudentOptionalProgramEntity currentEntity = stdSpecialProgramOptional.get();
                studentOptionalProgramRepository.save(currentEntity);  // touch: update_user will be updated only.
            } else {
                entity.setId(UUID.randomUUID());
                studentOptionalProgramRepository.save(entity);
            }
            summary.incrementOptionalProgram(optionalProgramCode);
        }
        return true;
    }

    private boolean createStudentCareerProgram(String careerProgramCode, GraduationStatusEntity student, ConversionStudentSummaryDTO summary) {
        StudentCareerProgramEntity entity = new StudentCareerProgramEntity();
        entity.setStudentID(student.getStudentID());

        CareerProgramEntity cpEntity = programService.getCareerProgramCode(careerProgramCode);
        if (cpEntity != null) {
            entity.setCareerProgramCode(careerProgramCode);
            Optional<StudentCareerProgramEntity> stdCareerProgramOptional = studentCareerProgramRepository.findByStudentIDAndCareerProgramCode(student.getStudentID(), careerProgramCode);
            if (stdCareerProgramOptional.isPresent()) {
                StudentCareerProgramEntity currentEntity = stdCareerProgramOptional.get();
                studentCareerProgramRepository.save(currentEntity);  // touch: update_user will be updated only.
            } else {
                entity.setId(UUID.randomUUID());
                studentCareerProgramRepository.save(entity);
            }
            summary.incrementCareerProgram(careerProgramCode);
            return true;
        } else {
            ConversionAlert error = new ConversionAlert();
            error.setLevel(ConversionAlert.AlertLevelEnum.WARNING);
            error.setItem(student.getPen());
            error.setReason("Career Program Code does not exist: " + careerProgramCode);
            summary.getErrors().add(error);
            return false;
        }
    }

    private boolean determineProgram(ConvGradStudent student, ConversionStudentSummaryDTO summary) {
        switch(student.getGraduationRequestYear()) {
            case "2018":
                if (student.getSchoolOfRecord().startsWith("093")) {
                    student.setProgram("2018-PF");
                    summary.increment("2018-PF", student.isGraduated());
                } else {
                    student.setProgram("2018-EN");
                    summary.increment("2018-EN", student.isGraduated());
                }
                break;
            case "2004":
                if (student.getSchoolOfRecord().startsWith("093")) {
                    student.setProgram("2004-PF");
                    summary.increment("2004-PF", student.isGraduated());
                } else {
                    student.setProgram("2004-EN");
                    summary.increment("2004-EN", student.isGraduated());
                }
                break;
            case "1996":
                student.setProgram("1996");
                summary.increment("1996", student.isGraduated());
                break;
            case "1986":
                student.setProgram("1986");
                summary.increment("1986", student.isGraduated());
                break;
            case "1950":
                if (StringUtils.equals(student.getStudentGrade(), "AD")) {
                    student.setProgram("1950");
                    summary.increment("1950", student.isGraduated());
                } else if (StringUtils.equals(student.getStudentGrade(), "AN")) {
                    student.setProgram("NOPROG");
                    summary.increment("NOPROG", student.isGraduated());
                } else {
                    // error
                    ConversionAlert error = new ConversionAlert();
                    error.setItem(student.getPen());
                    error.setReason("Program is not found for year 1950 / grade " + student.getStudentGrade());
                    summary.getErrors().add(error);
                    return false;
                }
                break;
            case "SCCP":
                student.setProgram("SCCP");
                summary.increment("SCCP", false);
                break;
            default:
                return false;
        }
        return true;
    }

}
