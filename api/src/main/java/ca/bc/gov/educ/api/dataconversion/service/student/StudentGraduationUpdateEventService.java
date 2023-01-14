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
import static ca.bc.gov.educ.api.dataconversion.constant.EventType.UPD_GRAD;

@Service
@Slf4j
public class StudentGraduationUpdateEventService extends StudentBaseService implements EventService {

    private final EventRepository eventRepository;

    private final StudentService studentService;
    private final RestUtils restUtils;

    private final EducGradDataConversionApiConstants constants;

    @Autowired
    public StudentGraduationUpdateEventService(EventRepository eventRepository,
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
        TraxGraduationUpdateDTO updateGrad = (TraxGraduationUpdateDTO) request;
        if (updateGrad != null && constants.isGradUpdateEnabled()) {
            // Get Access Token
            ResponseObj res = restUtils.getTokenResponseObject();
            String accessToken = null;
            if (res != null) {
                accessToken = res.getAccess_token();
            }
            // Load grad student
            StudentGradDTO currentStudent = studentService.loadStudentData(updateGrad.getPen(), accessToken);
            processStudent(updateGrad, currentStudent, accessToken);
        }

        var existingEvent = eventRepository.findByEventId(event.getEventId());
        existingEvent.ifPresent(eventRecord -> {
            eventRecord.setEventStatus(PROCESSED.toString());
            eventRecord.setUpdateDate(LocalDateTime.now());
            eventRepository.save(eventRecord);
        });
    }

    public void processStudent(TraxGraduationUpdateDTO updateGrad, StudentGradDTO currentStudent, String accessToken) {
        boolean isChanged = false;

        log.info(" Process Student : studentID = {}, pen = {}", currentStudent.getStudentID(), updateGrad.getPen());
        // Grad Program
        String gradProgram = getGradProgram(updateGrad.getGraduationRequirementYear(), updateGrad.getSchoolOfRecord(), null);
        if (!StringUtils.equals(gradProgram, currentStudent.getProgram())) {
            handleProgramChange(gradProgram, currentStudent);
            if (StringUtils.isBlank(currentStudent.getGradDate())) { // non grad
                // Transcript
                currentStudent.setNewRecalculateGradStatus("Y");
                // TVR
                currentStudent.setNewRecalculateProjectedGrad("Y");
            }
            log.info(" => grad program : current = {}, request = {}", currentStudent.getProgram(), currentStudent.getNewProgram());
            isChanged = true;
        }
        // School of record
        if (!StringUtils.equals(updateGrad.getSchoolOfRecord(), currentStudent.getSchoolOfRecord())) {
            currentStudent.setNewSchoolOfRecord(updateGrad.getSchoolOfRecord());
            // Transcript
            currentStudent.setNewRecalculateGradStatus("Y");
            // TVR
            currentStudent.setNewRecalculateProjectedGrad("Y");
            log.info(" => school of record : current = {}, request = {}", currentStudent.getSchoolOfRecord(), currentStudent.getNewSchoolOfRecord());
            isChanged = true;
        }
        // Student Grade
        if (!StringUtils.equals(updateGrad.getStudentGrade(), currentStudent.getStudentGrade())) {
            currentStudent.setNewStudentGrade(updateGrad.getStudentGrade());
            if (StringUtils.isBlank(currentStudent.getGradDate())) { // non grad
                // Transcript
                currentStudent.setNewRecalculateGradStatus("Y");
                // TVR
                currentStudent.setNewRecalculateProjectedGrad("Y");
            } else {
                // TVR
                currentStudent.setNewRecalculateProjectedGrad("Y");
            }
            log.info(" => student grade : current = {}, request = {}", currentStudent.getStudentGrade(), currentStudent.getNewStudentGrade());
            isChanged = true;
        }
        // SLP Date
        if (!StringUtils.equals(updateGrad.getSlpDate(), currentStudent.getGradDate())) {
            if (StringUtils.isBlank(currentStudent.getGradDate())) { // non grad
                // Transcript
                currentStudent.setNewRecalculateGradStatus("Y");
                // TVR
                currentStudent.setNewRecalculateProjectedGrad("Y");
            }
            log.info(" => student slp date : current = {}, request = {}", currentStudent.getStudentGrade(), currentStudent.getNewStudentGrade());
            isChanged = true;
        }

        // citizenship
        if (!StringUtils.equals(updateGrad.getCitizenship(), currentStudent.getCitizenship())) {
            currentStudent.setCitizenship(updateGrad.getCitizenship());
            if (StringUtils.isBlank(currentStudent.getGradDate())) { // non grad
                // Transcript
                currentStudent.setNewRecalculateGradStatus("Y");
                // TVR
                currentStudent.setNewRecalculateProjectedGrad("Y");
            } else {
                // TVR
                currentStudent.setNewRecalculateProjectedGrad("Y");
            }
            log.info(" => student citizenship : current = {}, request = {}", currentStudent.getStudentGrade(), currentStudent.getNewStudentGrade());
            isChanged = true;
        }

        if (isChanged) {
            log.info(" Save Student : studentID = {}, pen = {}", currentStudent.getStudentID(), updateGrad.getPen());
            studentService.saveGraduationStudent(currentStudent, accessToken);
        }
    }

    private void handleProgramChange(String newGradProgram, StudentGradDTO currentStudent) {
        boolean addDualDogwood = false;
        boolean deleteDualDogwood = false;

        if (newGradProgram.endsWith("-PF") && !currentStudent.getProgram().endsWith("-PF")) {
            addDualDogwood = true;
        } else if (!newGradProgram.endsWith("-PF") && currentStudent.getProgram().endsWith("-PF")) {
            deleteDualDogwood = true;
        }

        currentStudent.setAddDualDogwood(addDualDogwood);
        currentStudent.setDeleteDualDogwood(deleteDualDogwood);
        currentStudent.setNewProgram(newGradProgram);
    }

    @Override
    public String getEventType() {
        return UPD_GRAD.toString();
    }
}
