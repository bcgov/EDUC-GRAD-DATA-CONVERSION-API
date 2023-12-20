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

import static ca.bc.gov.educ.api.dataconversion.constant.EventStatus.PROCESSED;
import static ca.bc.gov.educ.api.dataconversion.constant.EventType.UPD_DEMOG;

@Service
@Slf4j
public class StudentDemographicsUpdateEventService extends StudentBaseService implements EventService {

    private final EventRepository eventRepository;

    private final StudentProcess studentProcess;
    private final RestUtils restUtils;

    private final EducGradDataConversionApiConstants constants;

    @Autowired
    public StudentDemographicsUpdateEventService(EventRepository eventRepository,
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
        TraxDemographicsUpdateDTO updateDemog  = (TraxDemographicsUpdateDTO) request;
        if (updateDemog != null && constants.isGradUpdateEnabled()) {
            // Get Access Token
            ResponseObj res = restUtils.getTokenResponseObject();
            String accessToken = null;
            if (res != null) {
                accessToken = res.getAccess_token();
            }
            // Load grad student
            StudentGradDTO currentStudent = studentProcess.loadStudentData(updateDemog.getPen(), accessToken);
            if (currentStudent != null) {
                processStudentDemographics(updateDemog, currentStudent, accessToken);
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

    public void processStudentDemographics(TraxDemographicsUpdateDTO updateDemog, StudentGradDTO currentStudent, String accessToken) {
        boolean isChanged = false;
        // Last Name
        if (!StringUtils.equals(updateDemog.getLastName(), currentStudent.getLastName())) {
            populateNewBatchFlags(currentStudent);
            isChanged = true;
            log.info(" => student last name : current = {}, request = {}", currentStudent.getLastName(), updateDemog.getLastName());
        }
        // First Name
        if (!StringUtils.equals(updateDemog.getFirstName(), currentStudent.getFirstName())) {
            populateNewBatchFlags(currentStudent);
            isChanged = true;
            log.info(" => student first name : current = {}, request = {}", currentStudent.getFirstName(), updateDemog.getFirstName());
        }
        // Middle Names
        if (!StringUtils.equals(updateDemog.getMiddleNames(), currentStudent.getMiddleName())) {
            populateNewBatchFlags(currentStudent);
            isChanged = true;
            log.info(" => student middle name : current = {}, request = {}", currentStudent.getMiddleName(), updateDemog.getMiddleNames());
        }
        // Date of Birth
        if (!StringUtils.equals(updateDemog.getBirthday(), currentStudent.getBirthday())) {
            populateNewBatchFlags(currentStudent);
            isChanged = true;
            log.info(" => student dob : current = {}, request = {}", currentStudent.getBirthday(), updateDemog.getBirthday());
        }

        if (isChanged) {
            log.info(" Save Student : studentID = {}, pen = {}", currentStudent.getStudentID(), updateDemog.getPen());
            studentProcess.saveGraduationStudent(updateDemog.getPen(), currentStudent, UPD_DEMOG, accessToken);
        }
    }

    private void populateNewBatchFlags(StudentGradDTO currentStudent) {
        if ("ARC".equalsIgnoreCase(currentStudent.getStudentStatus())) {
            // Transcript
            currentStudent.setNewRecalculateGradStatus("Y");
        } else {
            // Transcript
            currentStudent.setNewRecalculateGradStatus("Y");
            // TVR
            currentStudent.setNewRecalculateProjectedGrad("Y");
        }
    }

    @Override
    public String getEventType() {
        return UPD_DEMOG.toString();
    }
}
