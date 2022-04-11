package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.entity.conv.Event;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.conv.EventRepository;
import ca.bc.gov.educ.api.dataconversion.service.EventService;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static ca.bc.gov.educ.api.dataconversion.constant.EventStatus.PROCESSED;
import static ca.bc.gov.educ.api.dataconversion.constant.EventType.UPDATE_TRAX_STUDENT_MASTER;

@Service
@Slf4j
public class OngoingUpdateService extends StudentBaseService implements EventService {

    private final EventRepository eventRepository;

    private final StudentService studentService;
    private final DataConversionService dataConversionService;
    private final RestUtils restUtils;

    private final EducGradDataConversionApiConstants constants;

    @Autowired
    public OngoingUpdateService(EventRepository eventRepository,
                                StudentService studentService,
                                DataConversionService dataConversionService,
                                RestUtils restUtils,
                                EducGradDataConversionApiConstants constants) {
        this.eventRepository = eventRepository;
        this.studentService = studentService;
        this.dataConversionService = dataConversionService;
        this.restUtils = restUtils;
        this.constants = constants;
    }

    @Override
    public <T extends Object> void processEvent(T request, Event event) {
        TraxUpdateInGrad traxUpdateInGrad = (TraxUpdateInGrad) request;

        ConvGradStudent requestStudent = null;
            List<ConvGradStudent> traxStudents = dataConversionService.loadGradStudentDataFromTrax(traxUpdateInGrad.getPen());
        if (!traxStudents.isEmpty()) {
            requestStudent = traxStudents.get(0);
            log.info("=== Ongoing update: request pen = {}, update type = {} ===> TRAX load is done.", traxUpdateInGrad.getPen(), traxUpdateInGrad.getUpdateType());
        } else {
            log.info("=== Ongoing update: request pen = {}, update type = {} ===> TRAX load is failed.", traxUpdateInGrad.getPen(), traxUpdateInGrad.getUpdateType());
        }

        if (requestStudent != null && constants.isGradUpdateEnabled()) {
            // Get Access Token
            ResponseObj res = restUtils.getTokenResponseObject();
            String accessToken = null;
            if (res != null) {
                accessToken = res.getAccess_token();
            }
            if (StringUtils.equals(traxUpdateInGrad.getUpdateType(), "NEWSTUDENT")) {
                ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
                summary.setAccessToken(accessToken);
                studentService.convertStudent(requestStudent, new ConversionStudentSummaryDTO());
            } else {
                // get Grad Program & Student Status for trax student
                requestStudent.setProgram(getGradProgram(requestStudent.getGraduationRequestYear(), requestStudent.getSchoolOfRecord(), requestStudent.getStudentGrade()));
                requestStudent.setStudentStatus(getGradStudentStatus(requestStudent.getStudentStatus(), requestStudent.getArchiveFlag()));

                // Load grad student
                StudentGradDTO currentStudent = studentService.loadStudentData(requestStudent.getPen(), accessToken);
                log.info(" Get graduation data : studentID = {}  ===> GRAD load is done.", currentStudent.getStudentID(), traxUpdateInGrad.getUpdateType());
                switch (traxUpdateInGrad.getUpdateType()) {
                    case "STUDENT":
                        processStudent(requestStudent, currentStudent, accessToken);
                        break;
                    case "XPROGRAM":
                        processOptionalAndCareerPrograms(requestStudent, currentStudent, accessToken);
                        break;
                    case "FI10ADD":
                    case "FI11ADD":
                        processFrenchImmersion(requestStudent, currentStudent, accessToken, false);
                        break;
                    case "FI10DELETE":
                    case "FI11DELETE":
                        processFrenchImmersion(requestStudent, currentStudent, accessToken, true);
                        break;
                    case "COURSE":
                    case "ASSESSMENT":
                        studentService.triggerGraduationBatchRun(currentStudent.getStudentID());
                        break;
                    default:
                        break;
                }
            }
        }

        var existingEvent = eventRepository.findByEventId(event.getEventId());
        existingEvent.ifPresent(eventRecord -> {
            eventRecord.setEventStatus(PROCESSED.toString());
            eventRecord.setUpdateDate(LocalDateTime.now());
            eventRepository.save(eventRecord);
        });
    }

    public void processStudent(ConvGradStudent requestStudent, StudentGradDTO currentStudent, String accessToken) {
        boolean isChanged = false;

        log.info(" Process Student : studentID = {}, pen = {}", currentStudent.getStudentID(), requestStudent.getPen());
        // Grad Program
        if (!StringUtils.equals(requestStudent.getProgram(), currentStudent.getProgram())) {
            handleProgramChange(requestStudent, currentStudent);
            log.info(" => grad program : current = {}, request = {}", currentStudent.getProgram(), currentStudent.getNewProgram());
            isChanged = true;
        }
        // School of record
        if (!StringUtils.equals(requestStudent.getSchoolOfRecord(), currentStudent.getSchoolOfRecord())) {
            currentStudent.setNewSchoolOfRecord(requestStudent.getSchoolOfRecord());
            log.info(" => school of record : current = {}, request = {}", currentStudent.getSchoolOfRecord(), currentStudent.getNewSchoolOfRecord());
            isChanged = true;
        }
        // School at grad
        if (!StringUtils.equals(requestStudent.getSchoolAtGrad(), currentStudent.getSchoolAtGrad())) {
            currentStudent.setNewSchoolAtGrad(requestStudent.getSchoolAtGrad());
            log.info(" => school at grad : current = {}, request = {}", currentStudent.getSchoolAtGrad(), currentStudent.getNewSchoolAtGrad());
            isChanged = true;
        }
        // Student Status
        if (!StringUtils.equals(requestStudent.getStudentStatus(), currentStudent.getStudentStatus())) {
            currentStudent.setNewStudentStatus(requestStudent.getStudentStatus());
            log.info(" => student status : current = {}, request = {}", currentStudent.getStudentStatus(), currentStudent.getNewStudentStatus());
            isChanged = true;
        }
        // Student Grade
        if (!StringUtils.equals(requestStudent.getStudentGrade(), currentStudent.getStudentGrade())) {
            currentStudent.setNewStudentGrade(requestStudent.getStudentGrade());
            log.info(" => student grade : current = {}, request = {}", currentStudent.getStudentGrade(), currentStudent.getNewStudentGrade());
            isChanged = true;
        }
        // TODO (jsung): SLP Date - future ticket - can ignore for now

        if (isChanged) {
            log.info(" Save Student : studentID = {}, pen = {}", currentStudent.getStudentID(), requestStudent.getPen());
            studentService.saveGraduationStudent(currentStudent, accessToken);
            studentService.triggerGraduationBatchRun(currentStudent.getStudentID());
        }
    }

    public void processOptionalAndCareerPrograms(ConvGradStudent requestStudent, StudentGradDTO currentStudent, String accessToken) {
        log.info(" Process Optional & Career Programs : studentID = {}, pen = {}", currentStudent.getStudentID(), requestStudent.getPen());

        handleOptionalAndCareerProgramChange(requestStudent.getProgramCodes(), currentStudent.getProgramCodes(), currentStudent);

        if (currentStudent.getRemovedProgramCodes().isEmpty() && currentStudent.getAddedProgramCodes().isEmpty()) {
            return;
        }

        currentStudent.getRemovedProgramCodes().forEach(p -> {
            OptionalProgram optionalProgram = restUtils.getOptionalProgram(currentStudent.getProgram(),p, accessToken);
            if (optionalProgram != null) {
                log.info(" => removed optional program code : {}", p);
                studentService.removeStudentOptionalProgram(optionalProgram.getOptionalProgramID(), currentStudent);
            } else {
                log.info(" => removed career program code : {}", p);
                studentService.removeStudentCareerProgram(p, currentStudent);
            }
        });

        currentStudent.getAddedProgramCodes().forEach(p -> {
            OptionalProgram optionalProgram = restUtils.getOptionalProgram(currentStudent.getProgram(),p, accessToken);
            if (optionalProgram != null) {
                log.info(" => new optional program code : {}", p);
                studentService.addStudentOptionalProgram(optionalProgram.getOptionalProgramID(), currentStudent);
            } else {
                log.info(" => new career program code : {}", p);
                studentService.addStudentCareerProgram(p, currentStudent);
            }
        });

        // No Career Program?  then remove CP optional program
        if (studentService.existsCareerProgram(currentStudent.getStudentID())) {
            log.info(" => [CP] optional program will be added if not exist.");
            studentService.addStudentOptionalProgram("CP", currentStudent, accessToken);
        } else {
            log.info(" => [CP] optional program will be removed if exist.");
            studentService.removeStudentOptionalProgram("CP", currentStudent, accessToken);
        }
        studentService.triggerGraduationBatchRun(currentStudent.getStudentID());
    }

    public void processFrenchImmersion(ConvGradStudent requestStudent, StudentGradDTO currentStudent, String accessToken, boolean isDelete) {
        log.info(" Process French Immersion : studentID = {}, pen = {} ", currentStudent.getStudentID(), isDelete? "DELETE" : "ADD");
        if (isDelete && !studentService.hasAnyFrenchImmersionCourse(currentStudent.getProgram(), requestStudent.getPen(), requestStudent.getFrenchCert(), accessToken)) {
            log.info(" => remove FI optional program");
            studentService.removeStudentOptionalProgram("FI", currentStudent, accessToken);
            studentService.triggerGraduationBatchRun(currentStudent.getStudentID());
        } else if (!isDelete && studentService.hasAnyFrenchImmersionCourse(currentStudent.getProgram(), requestStudent.getPen(), requestStudent.getFrenchCert(), accessToken)) {
            log.info(" => create FI optional program");
            studentService.addStudentOptionalProgram("FI", currentStudent, accessToken);
            studentService.triggerGraduationBatchRun(currentStudent.getStudentID());
        }
    }

    private void handleProgramChange(ConvGradStudent requestStudent, StudentGradDTO currentStudent) {
        boolean addDualDogwood = false;
        boolean deleteDualDogwood = false;

        if (requestStudent.getProgram().endsWith("-PF") && !currentStudent.getProgram().endsWith("-PF")) {
            addDualDogwood = true;
        } else if (!requestStudent.getProgram().endsWith("-PF") && currentStudent.getProgram().endsWith("-PF")) {
            deleteDualDogwood = true;
        }

        currentStudent.setAddDualDogwood(addDualDogwood);
        currentStudent.setDeleteDualDogwood(deleteDualDogwood);
        currentStudent.setNewProgram(requestStudent.getProgram());
    }

    private void handleOptionalAndCareerProgramChange(List<String> reqProgramCodes,  List<String> curProgramCodes, StudentGradDTO currentStudent) {
        // Added Program?
        reqProgramCodes.forEach(p -> {
            if (!curProgramCodes.contains(p)) {
                currentStudent.getAddedProgramCodes().add(p);
            }
        });
        // Removed Program?
        curProgramCodes.forEach(p -> {
            if (!StringUtils.equals(p, "DD") && !StringUtils.equals(p, "FI") && !StringUtils.equals(p,"CP")) {
                if (!reqProgramCodes.contains(p)) {
                    currentStudent.getRemovedProgramCodes().add(p);
                }
            }
        });
    }

    @Override
    public String getEventType() {
        return UPDATE_TRAX_STUDENT_MASTER.toString();
    }
}
