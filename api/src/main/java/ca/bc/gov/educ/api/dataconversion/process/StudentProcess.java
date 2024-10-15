package ca.bc.gov.educ.api.dataconversion.process;

import ca.bc.gov.educ.api.dataconversion.constant.*;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.model.StudentAssessment;
import ca.bc.gov.educ.api.dataconversion.model.StudentCourse;

import ca.bc.gov.educ.api.dataconversion.service.student.StudentBaseService;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import ca.bc.gov.educ.api.dataconversion.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants.DEFAULT_CREATED_BY;
import static ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants.DEFAULT_UPDATED_BY;

@Component
@Slf4j
public class StudentProcess extends StudentBaseService {

    private static final String GRAD_STUDENT_API_ERROR_MSG = "Grad Student API is failed for ";
    private static final String EXCEPTION_MSG = "Exception occurred: ";

    private final RestUtils restUtils;
    private final AssessmentProcess assessmentProcess;
    private final CourseProcess courseProcess;

    @Autowired
    public StudentProcess(RestUtils restUtils,
                          AssessmentProcess assessmentProcess,
                          CourseProcess courseProcess) {
        this.restUtils = restUtils;
        this.assessmentProcess = assessmentProcess;
        this.courseProcess = courseProcess;
    }

    public ConvGradStudent convertStudent(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary, boolean reload, boolean ongoingUpdate) throws Exception {
        long startTime = System.currentTimeMillis();
        summary.setProcessedCount(summary.getProcessedCount() + 1L);

        // PEN Student
        List<Student> students = getStudentsFromPEN(convGradStudent, summary);
        if (ConversionResultType.FAILURE == convGradStudent.getResult()) { // PEN Student API is failed
            return convGradStudent;
        } else if (students == null || students.isEmpty()) {
            handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, "PEN does not exist: PEN Student API returns empty response.");
            return convGradStudent;
        }

        // Program Completion for graduated student
        if (!validateProgramCompletionDate(convGradStudent, summary)) {
            return convGradStudent;
        }

        // Student conversion process
        process(convGradStudent, students, summary, reload, ongoingUpdate);

        long diff = (System.currentTimeMillis() - startTime) / 1000L;
        log.info("** PEN: {} - {} secs", convGradStudent.getPen(), diff);
        return convGradStudent;
    }

    private List<Student> getStudentsFromPEN(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary) {
        List<Student> students = null;
        try {
            // Call PEN Student API
            students = restUtils.getStudentsByPen(convGradStudent.getPen(), summary.getAccessToken());
        } catch (Exception e) {
            log.error(EXCEPTION_MSG, e);
            handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, "PEN Student API is failed: " + e.getLocalizedMessage());
        }
        return students;
    }

    private void process(ConvGradStudent convGradStudent, List<Student> students, ConversionStudentSummaryDTO summary, boolean reload, boolean ongoingUpdate) {
        log.debug("Process Non-Graduated Student for pen# : " + convGradStudent.getPen());
        students.forEach(st -> {
            if (reload) {
                restUtils.removeAllStudentRelatedData(UUID.fromString(st.getStudentID()), summary.getAccessToken());
            }
            GraduationStudentRecord gradStudent = processStudent(convGradStudent, st, ongoingUpdate, summary);
            if (gradStudent != null) {
                processDependencies(convGradStudent, gradStudent, summary, ongoingUpdate);
            }

            if (convGradStudent.getResult() == null) {
                convGradStudent.setResult(ConversionResultType.SUCCESS);
            }
        });
    }

    private GraduationStudentRecord processStudent(ConvGradStudent convGradStudent, Student penStudent, boolean ongoingUpdate, ConversionStudentSummaryDTO summary) {
        UUID studentID = UUID.fromString(penStudent.getStudentID());
        GraduationStudentRecord gradStudent = null;
        try {
            gradStudent = restUtils.getStudentGradStatus(penStudent.getStudentID(), summary.getAccessToken());
        } catch (Exception e) {
            log.error(EXCEPTION_MSG, e);
            handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, GRAD_STUDENT_API_ERROR_MSG + "getting a GraduationStudentRecord : " + e.getLocalizedMessage());
            return null;
        }
        if (gradStudent != null) { // update
            gradStudent.setPen(penStudent.getPen());
            convertStudentData(convGradStudent, penStudent, gradStudent, summary);
            if (ConversionResultType.FAILURE != convGradStudent.getResult()) {
                gradStudent.setUpdateDate(null);
                gradStudent.setUpdateUser(null);
                try {
                    gradStudent = restUtils.saveStudentGradStatus(penStudent.getStudentID(), gradStudent, ongoingUpdate, summary.getAccessToken());
                } catch (Exception e) {
                    log.error(EXCEPTION_MSG, e);
                    handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, GRAD_STUDENT_API_ERROR_MSG + "saving a GraduationStudentRecord : " + e.getLocalizedMessage());
                    return null;
                }
                summary.setUpdatedCount(summary.getUpdatedCount() + 1L);
            }
        } else { // create
            gradStudent = new GraduationStudentRecord();
            gradStudent.setPen(penStudent.getPen());
            gradStudent.setStudentID(studentID);
            convertStudentData(convGradStudent, penStudent, gradStudent, summary);
            if (ConversionResultType.FAILURE != convGradStudent.getResult()) {
                try {
                    gradStudent = restUtils.saveStudentGradStatus(penStudent.getStudentID(), gradStudent, ongoingUpdate, summary.getAccessToken());
                } catch (Exception e) {
                    log.error(EXCEPTION_MSG, e);
                    handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, GRAD_STUDENT_API_ERROR_MSG + "saving a GraduationStudentRecord : " + e.getLocalizedMessage());
                    return null;
                }
                summary.setAddedCount(summary.getAddedCount() + 1L);
            }
        }
        if (ConversionResultType.FAILURE != convGradStudent.getResult()) {
            return gradStudent;
        } else {
            return null;
        }
    }

    private void processDependencies(ConvGradStudent convGradStudent,
                                     GraduationStudentRecord gradStudent,
                                     ConversionStudentSummaryDTO summary,
                                     boolean ongoingUpdate) {
        ConversionResultType result;

        // process dependencies
        gradStudent.setPen(convGradStudent.getPen());

        result = processOptionalPrograms(gradStudent, summary, ongoingUpdate);
        if (ConversionResultType.FAILURE != result) {
            result = processProgramCodes(gradStudent, convGradStudent.getProgramCodes(), summary);
        }
        if (ConversionResultType.FAILURE != result) {
            result = processSccpFrenchCertificates(gradStudent, summary);
        }

        convGradStudent.setResult(result);
    }

    private void convertStudentData(ConvGradStudent student, Student penStudent, GraduationStudentRecord gradStudent, ConversionStudentSummaryDTO summary) {
        if (determineProgram(student, summary)) {
            gradStudent.setProgram(student.getProgram());
        } else {
            return;
        }

        gradStudent.setGpa(null);
        gradStudent.setHonoursStanding(null);
        if ("SCCP".equalsIgnoreCase(gradStudent.getProgram())) {
            if (!validateAndSetSlpDate(student, gradStudent, summary)) {
                return;
            }
        } else {
            gradStudent.setProgramCompletionDate(null);
        }
        gradStudent.setStudentGradData(null);
        gradStudent.setSchoolAtGrad(null);

        gradStudent.setSchoolOfRecord(StringUtils.isNotBlank(student.getSchoolOfRecord())? student.getSchoolOfRecord() : null);
        gradStudent.setStudentGrade(student.getStudentGrade());
        gradStudent.setStudentStatus(getGradStudentStatus(student.getStudentStatus(), student.getArchiveFlag()));

        handleAdultStartRule(penStudent, gradStudent);

        // flags
        if (StringUtils.equalsIgnoreCase(gradStudent.getStudentStatus(), STUDENT_STATUS_MERGED)) {
            gradStudent.setRecalculateGradStatus(null);
            gradStudent.setRecalculateProjectedGrad(null);
        } else if (StringUtils.equalsIgnoreCase(gradStudent.getStudentStatus(), STUDENT_STATUS_ARCHIVED)) {
            gradStudent.setRecalculateGradStatus("Y");
            gradStudent.setRecalculateProjectedGrad(null);
        } else {
            gradStudent.setRecalculateGradStatus("Y");
            gradStudent.setRecalculateProjectedGrad("Y");
        }

        // Mappings with Student_Master
        gradStudent.setConsumerEducationRequirementMet(student.getConsumerEducationRequirementMet());
        gradStudent.setStudentCitizenship(StringUtils.isBlank(student.getStudentCitizenship())? "U" : student.getStudentCitizenship());

        // extra
        gradStudent.setCreateUser(DEFAULT_CREATED_BY);
        gradStudent.setUpdateUser(DEFAULT_UPDATED_BY);
    }

    private ConversionResultType processOptionalPrograms(GraduationStudentRecord student, ConversionStudentSummaryDTO summary, boolean ongoingUpdate) {
        if (StringUtils.isBlank(student.getProgram())) {
            return ConversionResultType.SUCCESS;
        }

        // Dual Dogwood for yyyy-PF
        if (student.getProgram().endsWith("-PF")) {
            return createStudentOptionalProgram("DD", student, summary);
        }

        // French Immersion for 2018-EN, 2004-EN, 1996-EN, 1986-EN
        if (!ongoingUpdate && hasAnyFrenchImmersionCourse(student.getProgram(), student.getPen(), summary.getAccessToken())) {
            return createStudentOptionalProgram("FI", student, summary);
        }

        return ConversionResultType.SUCCESS;
    }

    public boolean hasAnyFrenchImmersionCourse(String program, String pen, String accessToken) {
        boolean frenchImmersion = false;
        switch (program) {
            case "2023-EN", "2018-EN", "2004-EN" -> {
                if (courseProcess.isFrenchImmersionCourse(pen, "10", accessToken)) { // FRAL 10 or FRALP 10
                    frenchImmersion = true;
                }
            }
            case "1996-EN" -> {
                if (courseProcess.isFrenchImmersionCourse(pen, "11", accessToken)) { // FRAL 11 or FRALP 11
                    frenchImmersion = true;
                }
            }
            case "1986-EN" -> {
                if (courseProcess.isFrenchImmersionCourseForEN(pen, "11", accessToken)) { // FRAL 11
                    frenchImmersion = true;
                }
            }
        }
        return frenchImmersion;
    }

    private ConversionResultType processProgramCodes(GraduationStudentRecord student, List<String> programCodes, ConversionStudentSummaryDTO summary) {
        ConversionResultType resultType = ConversionResultType.SUCCESS;
        Boolean isCareerProgramCreated = Boolean.FALSE;
        if (StringUtils.isNotBlank(student.getProgram()) && !programCodes.isEmpty()) {
            for (String programCode : programCodes) {
                Pair<ConversionResultType, Boolean> res = handleProgramCode(programCode, student, summary);
                if (Boolean.TRUE.equals(res.getRight())) {
                    isCareerProgramCreated = Boolean.TRUE;
                }
                resultType = res.getLeft();
                if (ConversionResultType.FAILURE == resultType) {
                    break;
                }
            }
            if (Boolean.TRUE.equals(isCareerProgramCreated)) {
                resultType = createStudentOptionalProgram("CP", student, summary);
            }
        }
        return resultType;
    }

    private Pair<ConversionResultType, Boolean> handleProgramCode(String programCode, GraduationStudentRecord student, ConversionStudentSummaryDTO summary) {
        ConversionResultType resultType;
        boolean isCareerProgramCreated = false;
        if (isOptionalProgramCode(programCode)) {
            resultType = createStudentOptionalProgram(programCode, student, summary);
        } else {
            resultType = createStudentCareerProgram(programCode, student, summary);
            if (ConversionResultType.SUCCESS == resultType) {
                isCareerProgramCreated = true;
            }
        }
        return Pair.of(resultType, isCareerProgramCreated);
    }

    private ConversionResultType processSccpFrenchCertificates(GraduationStudentRecord student, ConversionStudentSummaryDTO summary) {
        if (StringUtils.equals(student.getProgram(), "SCCP")
            && ( StringUtils.isNotBlank(student.getSchoolOfRecord())
                 && student.getSchoolOfRecord().startsWith("093") )
        ) {
            return createStudentOptionalProgram("FR", student, summary);
        }
        return ConversionResultType.SUCCESS;
    }

    private ConversionResultType createStudentOptionalProgram(String optionalProgramCode, GraduationStudentRecord student, ConversionStudentSummaryDTO summary) {
        StudentOptionalProgramRequestDTO object = new StudentOptionalProgramRequestDTO();
        object.setPen(student.getPen());
        object.setStudentID(student.getStudentID());
        object.setMainProgramCode(student.getProgram());
        object.setOptionalProgramCode(optionalProgramCode);
        object.setOptionalProgramCompletionDate(null);

        try {
            restUtils.saveStudentOptionalProgram(object, summary.getAccessToken());
        } catch (Exception e) {
            handleException(null, summary, student.getPen(), ConversionResultType.FAILURE, GRAD_STUDENT_API_ERROR_MSG + "saving a StudentOptionalProgram : " + e.getLocalizedMessage());
            return ConversionResultType.FAILURE;
        }
        summary.incrementOptionalProgram(optionalProgramCode);
        return ConversionResultType.SUCCESS;
    }

    private ConversionResultType createStudentCareerProgram(String careerProgramCode, GraduationStudentRecord student, ConversionStudentSummaryDTO summary) {
        StudentCareerProgram object = new StudentCareerProgram();
        object.setStudentID(student.getStudentID());
        object.setCareerProgramCode(careerProgramCode);

        try {
            restUtils.saveStudentCareerProgram(object, summary.getAccessToken());
        } catch (Exception e) {
            handleException(null, summary, student.getPen(), ConversionResultType.FAILURE, GRAD_STUDENT_API_ERROR_MSG + "saving a StudentCareerProgram : " + e.getLocalizedMessage());
            return ConversionResultType.FAILURE;
        }
        summary.incrementCareerProgram(careerProgramCode);
        return ConversionResultType.SUCCESS;
    }

    /**
     * Load Student Data in GRAD
     *      for ongoing updates from TRAX to GRAD
     *
     * @param pen
     * @param accessToken
     * @return
     */
    public StudentGradDTO loadStudentData(String pen, String accessToken) {
        Student penStudent;
        // PEN Student
        try {
            // Call PEN Student API
            List<Student> students = restUtils.getStudentsByPen(pen, accessToken);
            penStudent = students.stream().filter(s -> s.getPen().compareTo(pen) == 0).findAny().orElse(null);
        } catch (Exception e) {
            log.error("PEN Student API is failed for pen[{}] : {} ", pen, e.getLocalizedMessage());
            return null;
        }

        if (penStudent == null) {
            log.error("Pen# [{}] is not found in PEN StudentAPI.", pen);
            return null;
        }

        UUID studentID = UUID.fromString(penStudent.getStudentID());
        StudentGradDTO studentData = new StudentGradDTO();
        studentData.setStudentID(studentID);
        // populate Demographic data
        studentData.setLastName(penStudent.getLegalLastName());
        studentData.setFirstName(penStudent.getLegalFirstName());
        studentData.setMiddleName(penStudent.getLegalMiddleNames());
        studentData.setBirthday(penStudent.getDob());

        GraduationStudentRecord gradStudent = null;
        try {
            gradStudent = restUtils.getStudentGradStatus(studentID.toString(), accessToken);
        } catch (Exception e) {
            log.error(GRAD_STUDENT_API_ERROR_MSG + "getting a GraduationStudentRecord : " + e.getLocalizedMessage());
            return null;
        }
        if (gradStudent != null) {
            studentData.setProgram(gradStudent.getProgram());
            studentData.setGradDate(gradStudent.getProgramCompletionDate());
            studentData.setStudentGrade(gradStudent.getStudentGrade());
            studentData.setStudentStatus(gradStudent.getStudentStatus());
            studentData.setSchoolOfRecord(gradStudent.getSchoolOfRecord());
            studentData.setSchoolAtGrad(gradStudent.getSchoolAtGrad());
            studentData.setCitizenship(gradStudent.getStudentCitizenship());
            studentData.setAdultStartDate(gradStudent.getAdultStartDate());
        } else {
            log.error("GraduationStudentRecord is not found for pen# [{}], studentID [{}]", pen, studentID);
            return null;
        }

        // optional programs
        List<StudentOptionalProgram> optionalPrograms = new ArrayList<>();
        try {
            optionalPrograms = restUtils.getStudentOptionalPrograms(studentID.toString(), accessToken);
        } catch (Exception e) {
            log.error(GRAD_STUDENT_API_ERROR_MSG + "getting StudentOptionalPrograms : " + e.getLocalizedMessage());
        }
        studentData.getProgramCodes().addAll(getOptionalProgramCodes(optionalPrograms));

        // career programs
        List<StudentCareerProgram> careerPrograms = new ArrayList<>();
        try {
            careerPrograms = restUtils.getStudentCareerPrograms(studentID.toString(), accessToken);
        } catch (Exception e) {
            log.error(GRAD_STUDENT_API_ERROR_MSG + "getting StudentCareerPrograms : " + e.getLocalizedMessage());
        }
        studentData.getProgramCodes().addAll(getCareerProgramCodes(careerPrograms));

        // courses
        List<StudentCourse> courses = courseProcess.getStudentCourses(pen, accessToken);
        if (courses != null && !courses.isEmpty()) {
            studentData.getCourses().addAll(courses);
        }
        // assessments
        List<StudentAssessment> assessments = assessmentProcess.getStudentAssessments(pen, accessToken);
        if (assessments != null && !assessments.isEmpty()) {
            studentData.getAssessments().addAll(assessments);
        }

        return studentData;
    }

    private List<String> getOptionalProgramCodes(List<StudentOptionalProgram> studentOptionalPrograms) {
        List<String> codes = new ArrayList<>();
        if (studentOptionalPrograms != null && !studentOptionalPrograms.isEmpty()) {
            studentOptionalPrograms.forEach(e -> codes.add(e.getOptionalProgramCode()));
        }
        return codes;
    }

    private List<String> getCareerProgramCodes(List<StudentCareerProgram> studentCareerPrograms) {
        List<String> codes = new ArrayList<>();
        if (studentCareerPrograms != null && !studentCareerPrograms.isEmpty()) {
            studentCareerPrograms.forEach(e -> codes.add(e.getCareerProgramCode()));
        }
        return codes;
    }

    /**
     * Save Graduation Student Record
     *      for Ongoing Updates from TRAX to GRAD
     */
    public void saveGraduationStudent(String pen, StudentGradDTO gradStudent, EventType eventType, String accessToken) {
        OngoingUpdateRequestDTO requestDTO = new OngoingUpdateRequestDTO();
        requestDTO.setStudentID(gradStudent.getStudentID().toString());
        requestDTO.setPen(pen);
        requestDTO.setEventType(eventType);
        // UPD_GRAD ====================================================
        if (eventType == EventType.UPD_GRAD) {
            // Student Status
            if (StringUtils.isNotBlank(gradStudent.getNewStudentStatus())) {
                OngoingUpdateFieldDTO field = OngoingUpdateFieldDTO.builder()
                        .type(FieldType.STRING).name(FieldName.STUDENT_STATUS).value(gradStudent.getNewStudentStatus())
                        .build();
                requestDTO.getUpdateFields().add(field);
            }
            // School of Record
            if (StringUtils.isNotBlank(gradStudent.getNewSchoolOfRecord())) {
                OngoingUpdateFieldDTO field = OngoingUpdateFieldDTO.builder()
                        .type(FieldType.STRING).name(FieldName.SCHOOL_OF_RECORD).value(gradStudent.getNewSchoolOfRecord())
                        .build();
                requestDTO.getUpdateFields().add(field);
            }
            // GRAD Program
            if (StringUtils.isNotBlank(gradStudent.getNewProgram())) {
                OngoingUpdateFieldDTO field = OngoingUpdateFieldDTO.builder()
                        .type(FieldType.STRING).name(FieldName.GRAD_PROGRAM).value(gradStudent.getNewProgram())
                        .build();
                requestDTO.getUpdateFields().add(field);
            }
            // Adult Start Date when GRAD program is changed
            if (StringUtils.isNotBlank(gradStudent.getNewAdultStartDate())) {
                OngoingUpdateFieldDTO field = OngoingUpdateFieldDTO.builder()
                        .type(FieldType.DATE).name(FieldName.ADULT_START_DATE).value(gradStudent.getNewAdultStartDate())
                        .build();
                requestDTO.getUpdateFields().add(field);
            }
            // SLP Date
            if (StringUtils.isNotBlank(gradStudent.getNewGradDate())) {
                OngoingUpdateFieldDTO field = OngoingUpdateFieldDTO.builder()
                        .type(FieldType.DATE).name(FieldName.SLP_DATE).value(gradStudent.getNewGradDate())
                        .build();
                requestDTO.getUpdateFields().add(field);
            }
            // Student Grade
            if (StringUtils.isNotBlank(gradStudent.getNewStudentGrade())) {
                OngoingUpdateFieldDTO field = OngoingUpdateFieldDTO.builder()
                        .type(FieldType.STRING).name(FieldName.STUDENT_GRADE).value(gradStudent.getNewStudentGrade())
                        .build();
                requestDTO.getUpdateFields().add(field);
            }
            // Citizenship
            if (StringUtils.isNotBlank(gradStudent.getNewCitizenship())) {
                OngoingUpdateFieldDTO field = OngoingUpdateFieldDTO.builder()
                        .type(FieldType.STRING).name(FieldName.CITIZENSHIP).value(gradStudent.getNewCitizenship())
                        .build();
                requestDTO.getUpdateFields().add(field);
            }
        }

        // Others ======================================================
        // Batch Flags
        if (StringUtils.isNotBlank(gradStudent.getNewRecalculateGradStatus())) {
            OngoingUpdateFieldDTO field = OngoingUpdateFieldDTO.builder()
                    .type(FieldType.STRING).name(FieldName.RECALC_GRAD_ALG).value(gradStudent.getNewRecalculateGradStatus())
                    .build();
            requestDTO.getUpdateFields().add(field);
        }
        if (StringUtils.isNotBlank(gradStudent.getNewRecalculateProjectedGrad())) {
            OngoingUpdateFieldDTO field = OngoingUpdateFieldDTO.builder()
                    .type(FieldType.STRING).name(FieldName.RECALC_TVR).value(gradStudent.getNewRecalculateProjectedGrad())
                    .build();
            requestDTO.getUpdateFields().add(field);
        }

        restUtils.updateStudentGradStatusByFields(requestDTO, accessToken);

        if (StringUtils.isNotBlank(gradStudent.getNewProgram())) {
            removeAndReCreateOptionalPrograms(gradStudent, accessToken);
            handleFIorDDOptionalProgram(gradStudent, accessToken);
        }
    }

    private void removeAndReCreateOptionalPrograms(StudentGradDTO gradStudent, String accessToken) {
        // Remove all optional programs
        List<StudentOptionalProgram> studentOptionalPrograms = restUtils.getStudentOptionalPrograms(gradStudent.getStudentID().toString(), accessToken);
        if (studentOptionalPrograms != null && !studentOptionalPrograms.isEmpty()) {
            List<String> optionalProgramCodes = new ArrayList<>();

            studentOptionalPrograms.forEach(op -> {
                log.info(" => [{}] optional program will be removed if exist for {}.", op.getOptionalProgramCode(), gradStudent.getProgram());
                removeStudentOptionalProgram(op.getOptionalProgramID(), gradStudent, accessToken);
                if (isOptionalProgramRecreationRequired(op.getOptionalProgramCode(), gradStudent.getNewProgram())) {
                    optionalProgramCodes.add(op.getOptionalProgramCode());
                }
            });

            // Recreate nonFI & nonDD optional programs
            optionalProgramCodes.forEach(opc -> {
                log.info(" => [{}] optional program will be re-created for {}.", opc, gradStudent.getNewProgram());
                addStudentOptionalProgram(opc, gradStudent, true, accessToken);
            });
        }
    }

    private void handleFIorDDOptionalProgram(StudentGradDTO gradStudent, String accessToken) {
        if (gradStudent.isAddDualDogwood()) {
            log.info(" => [DD] optional program will be added if not exist for {}.", gradStudent.getNewProgram());
            // new grad program has to be used
            addStudentOptionalProgram("DD", gradStudent, true, accessToken);
        } else if (gradStudent.isAddFrenchImmersion()) {
            log.info(" => [FI] optional program will be added if not exist for {}.", gradStudent.getNewProgram());
            // new grad program has to be used
            addStudentOptionalProgram("FI", gradStudent, true, accessToken);
        }
    }

    public void removeStudentOptionalProgram(String optionalProgramCode, StudentGradDTO gradStudent, String accessToken) {
        OptionalProgram optionalProgram = restUtils.getOptionalProgram(gradStudent.getProgram(), optionalProgramCode, accessToken);
        if (optionalProgram != null) {
            removeStudentOptionalProgram(optionalProgram.getOptionalProgramID(), gradStudent, accessToken);
        }
    }

    public void removeStudentOptionalProgram(UUID optionalProgramID, StudentGradDTO gradStudent, String accessToken) {
        restUtils.removeStudentOptionalProgram(optionalProgramID, gradStudent.getStudentID(), accessToken);
    }

    public void addStudentOptionalProgram(String optionalProgramCode, StudentGradDTO gradStudent, boolean isNewGradProgram, String accessToken) {
        StudentOptionalProgramRequestDTO object = new StudentOptionalProgramRequestDTO();
        object.setStudentID(gradStudent.getStudentID());
        object.setMainProgramCode(isNewGradProgram && gradStudent.getNewProgram() != null? gradStudent.getNewProgram() : gradStudent.getProgram());
        object.setOptionalProgramCode(optionalProgramCode);
        restUtils.saveStudentOptionalProgram(object, accessToken);
    }

    public void addStudentCareerProgram(String careerProgramCode, UUID studentID, String accessToken) {
        StudentCareerProgram object = new StudentCareerProgram();
        object.setStudentID(studentID);
        object.setCareerProgramCode(careerProgramCode);
        restUtils.saveStudentCareerProgram(object, accessToken);
    }

    public void removeStudentCareerProgram(String careerProgramCode, StudentGradDTO gradStudent, String accessToken) {
        restUtils.removeStudentCareerProgram(careerProgramCode, gradStudent.getStudentID(), accessToken);
    }

    public boolean existsCareerProgram(UUID studentID, String accessToken) {
        List<StudentCareerProgram> list = restUtils.getStudentCareerPrograms(studentID.toString(), accessToken);
        return list != null && !list.isEmpty();
    }

    public void triggerGraduationBatchRun(EventType eventType, UUID studentID, String pen, String recalculateGradStatus, String recalculateProjectedGrad, String accessToken) {
        GraduationStudentRecord object = restUtils.getStudentGradStatus(studentID.toString(), accessToken);
        if (object != null) {
            OngoingUpdateRequestDTO requestDTO = new OngoingUpdateRequestDTO();
            requestDTO.setStudentID(studentID.toString());
            requestDTO.setPen(pen);
            requestDTO.setEventType(eventType);

            boolean isMerged = StringUtils.equals(object.getStudentStatus(), STUDENT_STATUS_MERGED);
            // Batch Flags
            if (StringUtils.isNotBlank(recalculateGradStatus)) {
                OngoingUpdateFieldDTO field = OngoingUpdateFieldDTO.builder()
                        .type(FieldType.STRING).name(FieldName.RECALC_GRAD_ALG).value(isMerged ? null : recalculateGradStatus)
                        .build();
                requestDTO.getUpdateFields().add(field);
            }
            if (StringUtils.isNotBlank(recalculateProjectedGrad)) {
                OngoingUpdateFieldDTO field = OngoingUpdateFieldDTO.builder()
                        .type(FieldType.STRING).name(FieldName.RECALC_TVR).value(isMerged ? null : recalculateProjectedGrad)
                        .build();
                requestDTO.getUpdateFields().add(field);
            }

            restUtils.updateStudentGradStatusByFields(requestDTO, accessToken);
        }
    }

    private void handleAdultStartRule(Student penStudent, GraduationStudentRecord gradStudent) {
        if ("1950".equalsIgnoreCase(gradStudent.getProgram())) {
            Date dob = EducGradDataConversionApiUtils.parseDate(penStudent.getDob());
            Date adultStartDate = DateUtils.addYears(dob, 18);
            gradStudent.setAdultStartDate(EducGradDataConversionApiUtils.formatDate(adultStartDate)); // yyyy-MM-dd
        }
    }

    /**
     *
     * @return true             Valid
     *         false            Bad data (programCompletionDate is null)
     */
    private boolean validateProgramCompletionDate(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary) {
        if ("SCCP".equalsIgnoreCase(convGradStudent.getGraduationRequirementYear()) &&
                StringUtils.isBlank(convGradStudent.getSlpDate())) {
            handleException(convGradStudent, summary, convGradStudent.getPen(), ConversionResultType.FAILURE, "Bad data: slp_date is null for SCCP");
            return false;
        }
        return true;
    }

    /**
     *
     * @return true             Valid
     *         false            Bad data (slp_date format is not right)
     */
    private boolean validateAndSetSlpDate(ConvGradStudent student, GraduationStudentRecord gradStudent, ConversionStudentSummaryDTO summary) {
        if (StringUtils.isNotBlank(student.getSlpDate()) && StringUtils.length(student.getSlpDate().trim()) != 8) {
            handleException(student, summary, student.getPen(), ConversionResultType.FAILURE, "Bad data : slp_date format is not yyyyMMdd");
            return false;
        } else {
            Date pcd = EducGradDataConversionApiUtils.parseDate(student.getSlpDate(), EducGradDataConversionApiConstants.TRAX_SLP_DATE_FORMAT);
            gradStudent.setProgramCompletionDate(EducGradDataConversionApiUtils.formatDate(pcd));
            return true;
        }
    }

}
