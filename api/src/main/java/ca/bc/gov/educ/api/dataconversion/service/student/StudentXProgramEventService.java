package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.entity.conv.Event;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.repository.conv.EventRepository;
import ca.bc.gov.educ.api.dataconversion.service.EventService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static ca.bc.gov.educ.api.dataconversion.constant.EventStatus.PROCESSED;
import static ca.bc.gov.educ.api.dataconversion.constant.EventType.XPROGRAM;

@Service
@Slf4j
public class StudentXProgramEventService extends StudentBaseService implements EventService {

    private final EventRepository eventRepository;

    private final StudentService studentService;
    private final RestUtils restUtils;

    private final EducGradDataConversionApiConstants constants;

    @Autowired
    public StudentXProgramEventService(EventRepository eventRepository,
                                       StudentService studentService,
                                       RestUtils restUtils,
                                       EducGradDataConversionApiConstants constants) {
        this.eventRepository = eventRepository;
        this.studentService = studentService;
        this.restUtils = restUtils;
        this.constants = constants;
    }

    @Override
    public <T extends Object> void processEvent(T request, Event event) {
        TraxXProgramDTO xprogram = (TraxXProgramDTO) request;
        if (xprogram != null && constants.isGradUpdateEnabled()) {
            // Get Access Token
            ResponseObj res = restUtils.getTokenResponseObject();
            String accessToken = null;
            if (res != null) {
                accessToken = res.getAccess_token();
            }
            // Load grad student
            StudentGradDTO currentStudent = studentService.loadStudentData(xprogram.getPen(), accessToken);
            processOptionalAndCareerPrograms(xprogram, currentStudent, accessToken);
        }

        var existingEvent = eventRepository.findByEventId(event.getEventId());
        existingEvent.ifPresent(eventRecord -> {
            eventRecord.setEventStatus(PROCESSED.toString());
            eventRecord.setUpdateDate(LocalDateTime.now());
            eventRepository.save(eventRecord);
        });
    }

    public void processOptionalAndCareerPrograms(TraxXProgramDTO xprogram, StudentGradDTO currentStudent, String accessToken) {
        log.info(" Process Optional & Career Programs : studentID = {}, pen = {}", currentStudent.getStudentID(), xprogram.getPen());

        handleOptionalAndCareerProgramChange(xprogram.getProgramList(), currentStudent.getProgramCodes(), currentStudent);

        if (currentStudent.getRemovedProgramCodes().isEmpty() && currentStudent.getAddedProgramCodes().isEmpty()) {
            return;
        }

        currentStudent.getRemovedProgramCodes().forEach(p -> {
            OptionalProgram optionalProgram = restUtils.getOptionalProgram(currentStudent.getProgram(),p, accessToken);
            if (optionalProgram != null) {
                log.info(" => removed optional program code : {}", p);
                studentService.removeStudentOptionalProgram(optionalProgram.getOptionalProgramID(), currentStudent, accessToken);
            } else {
                log.info(" => removed career program code : {}", p);
                studentService.removeStudentCareerProgram(p, currentStudent, accessToken);
            }
        });

        currentStudent.getAddedProgramCodes().forEach(p -> {
            OptionalProgram optionalProgram = restUtils.getOptionalProgram(currentStudent.getProgram(),p, accessToken);
            if (optionalProgram != null) {
                log.info(" => new optional program code : {}", p);
                studentService.addStudentOptionalProgram(optionalProgram.getOptProgramCode(), currentStudent, accessToken);
            } else {
                log.info(" => new career program code : {}", p);
                studentService.addStudentCareerProgram(p, currentStudent.getStudentID(), accessToken);
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

        // Transcript
        currentStudent.setNewRecalculateGradStatus("Y");
        // TVR
        currentStudent.setNewRecalculateProjectedGrad("Y");
        studentService.triggerGraduationBatchRun(currentStudent.getStudentID(), currentStudent.getNewRecalculateGradStatus(), currentStudent.getNewRecalculateProjectedGrad(), accessToken);
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
            if (!StringUtils.equals(p, "DD") && !StringUtils.equals(p, "FI") && !StringUtils.equals(p,"CP") && !reqProgramCodes.contains(p)) {
                currentStudent.getRemovedProgramCodes().add(p);
            }
        });
    }

    @Override
    public String getEventType() {
        return XPROGRAM.toString();
    }
}
