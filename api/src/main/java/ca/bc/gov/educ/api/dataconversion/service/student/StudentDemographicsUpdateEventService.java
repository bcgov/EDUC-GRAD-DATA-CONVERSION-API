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

import static ca.bc.gov.educ.api.dataconversion.constant.EventStatus.PROCESSED;
import static ca.bc.gov.educ.api.dataconversion.constant.EventType.UPD_DEMOG;

@Service
@Slf4j
public class StudentDemographicsUpdateEventService extends StudentBaseService implements EventService {

    private final EventRepository eventRepository;

    private final StudentService studentService;
    private final RestUtils restUtils;

    private final EducGradDataConversionApiConstants constants;

    @Autowired
    public StudentDemographicsUpdateEventService(EventRepository eventRepository,
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
        TraxDemographicsUpdateDTO updateDemog  = (TraxDemographicsUpdateDTO) request;
        if (updateDemog != null && constants.isGradUpdateEnabled()) {
            // Get Access Token
            ResponseObj res = restUtils.getTokenResponseObject();
            String accessToken = null;
            if (res != null) {
                accessToken = res.getAccess_token();
            }
            // Load grad student
            StudentGradDTO currentStudent = studentService.loadStudentData(updateDemog.getPen(), accessToken);
            processStudentDemographics(updateDemog, currentStudent, accessToken);
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
            // Transcript
            currentStudent.setNewRecalculateGradStatus("Y");
            // TVR
            currentStudent.setNewRecalculateProjectedGrad("Y");

            isChanged = true;
            log.info(" => student last name : current = {}, request = {}", currentStudent.getLastName(), updateDemog.getLastName());
        }
        // First Name
        if (!StringUtils.equals(updateDemog.getFirstName(), currentStudent.getFirstName())) {
            // Transcript
            currentStudent.setNewRecalculateGradStatus("Y");
            // TVR
            currentStudent.setNewRecalculateProjectedGrad("Y");

            isChanged = true;
            log.info(" => student first name : current = {}, request = {}", currentStudent.getFirstName(), updateDemog.getFirstName());
        }
        // Middle Names
        if (!StringUtils.equals(updateDemog.getMiddleNames(), currentStudent.getMiddleName())) {
            // Transcript
            currentStudent.setNewRecalculateGradStatus("Y");
            // TVR
            currentStudent.setNewRecalculateProjectedGrad("Y");

            isChanged = true;
            log.info(" => student middle name : current = {}, request = {}", currentStudent.getMiddleName(), updateDemog.getMiddleNames());
        }
        // Date of Birth
        if (!StringUtils.equals(updateDemog.getBirthday(), currentStudent.getBirthday())) {
            // Transcript
            currentStudent.setNewRecalculateGradStatus("Y");
            // TVR
            currentStudent.setNewRecalculateProjectedGrad("Y");

            isChanged = true;
            log.info(" => student dob : current = {}, request = {}", currentStudent.getBirthday(), updateDemog.getBirthday());
        }

        if (isChanged) {
            log.info(" Save Student : studentID = {}, pen = {}", currentStudent.getStudentID(), updateDemog.getPen());
            studentService.saveGraduationStudent(currentStudent, accessToken);
        }
    }

    @Override
    public String getEventType() {
        return UPD_DEMOG.toString();
    }
}