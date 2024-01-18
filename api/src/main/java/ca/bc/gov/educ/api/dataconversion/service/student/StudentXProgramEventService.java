package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.entity.Event;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.process.StudentProcess;
import ca.bc.gov.educ.api.dataconversion.repository.EventRepository;
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

    private final StudentProcess studentProcess;
    private final RestUtils restUtils;

    private final EducGradDataConversionApiConstants constants;

    @Autowired
    public StudentXProgramEventService(EventRepository eventRepository,
                                       StudentProcess studentProcess,
                                       RestUtils restUtils,
                                       EducGradDataConversionApiConstants constants) {
        this.eventRepository = eventRepository;
        this.studentProcess = studentProcess;
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
            StudentGradDTO currentStudent = studentProcess.loadStudentData(xprogram.getPen(), accessToken);
            if (currentStudent != null) {
                processOptionalAndCareerPrograms(xprogram, currentStudent, accessToken);
            } else {
                return;
            }
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
            if (isOptionalProgramCode(p)) {
                log.info(" => [{}] optional program will be removed if exist for {}.", p, currentStudent.getProgram());
                studentProcess.removeStudentOptionalProgram(p, currentStudent, accessToken);
            } else {
                log.info(" => [{}] career program will be removed if exist for {}.", p, currentStudent.getProgram());
                studentProcess.removeStudentCareerProgram(p, currentStudent, accessToken);
            }
        });

        currentStudent.getAddedProgramCodes().forEach(p -> {
            if (isOptionalProgramCode(p)) {
                log.info(" => [{}] optional program will be added if not exist for {}.", p, currentStudent.getProgram());
                studentProcess.addStudentOptionalProgram(p, currentStudent, false, accessToken);
            } else {
                log.info(" => [{}] career program will be added if not exist for {}.", p, currentStudent.getProgram());
                studentProcess.addStudentCareerProgram(p, currentStudent.getStudentID(), accessToken);
            }
        });

        // No Career Program?  then remove CP optional program
        if (studentProcess.existsCareerProgram(currentStudent.getStudentID(), accessToken)) {
            log.info(" => [CP] optional program will be added if not exist for {}.", currentStudent.getProgram());
            studentProcess.addStudentOptionalProgram("CP", currentStudent, false, accessToken);
        } else {
            log.info(" => [CP] optional program will be removed if exist for {}.", currentStudent.getProgram());
            studentProcess.removeStudentOptionalProgram("CP", currentStudent, accessToken);
        }

        // Transcript & TVR
        populateNewBatchFlags(currentStudent);

        studentProcess.triggerGraduationBatchRun(XPROGRAM, currentStudent.getStudentID(), xprogram.getPen(), currentStudent.getNewRecalculateGradStatus(), currentStudent.getNewRecalculateProjectedGrad(), accessToken);
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
