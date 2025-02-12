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
import static ca.bc.gov.educ.api.dataconversion.constant.EventType.UPD_GRAD;

@Service
@Slf4j
public class StudentGraduationUpdateEventService extends StudentGraduationUpdateBaseService implements EventService {

    private final EventRepository eventRepository;

    private final StudentProcess studentProcess;
    private final RestUtils restUtils;

    private final EducGradDataConversionApiConstants constants;

    @Autowired
    public StudentGraduationUpdateEventService(EventRepository eventRepository,
                                               StudentProcess studentProcess,
                                               RestUtils restUtils,
                                               EducGradDataConversionApiConstants constants) {
        super(restUtils);
        this.eventRepository = eventRepository;
        this.studentProcess = studentProcess;
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
            StudentGradDTO currentStudent = studentProcess.loadStudentData(updateGrad.getPen(), accessToken);
            if (currentStudent != null) {
                processStudent(updateGrad, currentStudent, accessToken);
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

    public void processStudent(TraxGraduationUpdateDTO updateGrad, StudentGradDTO currentStudent, String accessToken) {
        boolean isChanged = false;
        log.info(" Process Student : studentID = {}, pen = {}", currentStudent.getStudentID(), updateGrad.getPen());

        // Student Status - This step is moved to top to determine the update fields based on new status.
        String newStudentStatus = getGradStudentStatus(updateGrad.getStudentStatus(), updateGrad.getArchiveFlag());
        if (newStudentStatus != null) {
            processStudentStatus(currentStudent, newStudentStatus);
            log.info(" => student status : current = {}, request = {}", currentStudent.getStudentStatus(), currentStudent.getNewStudentStatus());
            isChanged = true;
        }
        // Processing order is important for the first 3 fields below.
        // 1. School of Record Guid
        if (updateGrad.getSchoolOfRecordId() != null) {
            isChanged = isChanged || processSchoolOfRecordId(currentStudent, updateGrad.getSchoolOfRecordId());
            log.info(" => school of record id : current = {}, request = {}", currentStudent.getSchoolOfRecordId(), currentStudent.getNewSchoolOfRecordId());
        }
        // 2. Grad Program
        String gradProgram = getGradProgram(updateGrad.getGraduationRequirementYear(), currentStudent.getUpToDateSchoolOfRecordId(), null);
        if (gradProgram != null) {
            isChanged = isChanged || processGraduationProgram(currentStudent, updateGrad.getPen(), gradProgram, accessToken);
            if (isChanged && StringUtils.isNotBlank(currentStudent.getNewProgram())) {
                log.info(" => grad program : current = {}, request = {}", currentStudent.getProgram(), currentStudent.getNewProgram());
            } else {
                log.info(" => grad program : current = {}, request = {} => no change(undo completion is required instead)", currentStudent.getProgram(), gradProgram);
            }
        }
        // 3. SLP Date
        String slpDate = updateGrad.getSlpDateWithDefaultFormat();
        if (slpDate != null && "SCCP".equalsIgnoreCase(currentStudent.getUpToDateGradProgram())) {
            isChanged = isChanged || processSlpDate(currentStudent, slpDate);
            if (isChanged) {
                log.info(" => slp date : current = {}, request = {}", currentStudent.getGradDate(), slpDate);
            } else {
                log.info(" => slp date : current = {}, request = {} => no change(undo completion is required instead)", currentStudent.getGradDate(), slpDate);
            }
        }
        // 4. Student Grade
        if (StringUtils.isNotBlank(updateGrad.getStudentGrade())) {
            isChanged = isChanged || processStudentGrade(currentStudent, updateGrad.getStudentGrade());
            log.info(" => student grade : current = {}, request = {}", currentStudent.getStudentGrade(), currentStudent.getNewStudentGrade());
        }
        // 5. Citizenship
        if (StringUtils.isNotBlank(updateGrad.getCitizenship())) {
            isChanged = isChanged || processCitizenship(currentStudent, updateGrad.getCitizenship());
            log.info(" => student citizenship : current = {}, request = {}", currentStudent.getCitizenship(), currentStudent.getNewCitizenship());
        }

        if (isChanged) {
            log.info(" Save Student : studentID = {}, pen = {}", currentStudent.getStudentID(), updateGrad.getPen());
            studentProcess.saveGraduationStudent(updateGrad.getPen(), currentStudent, UPD_GRAD, accessToken);
        }
    }

    @Override
    public String getEventType() {
        return UPD_GRAD.toString();
    }

    @Override
    public boolean hasAnyFrenchImmersionCourse(String gradProgramCode, String pen, String accessToken) {
        return studentProcess.hasAnyFrenchImmersionCourse(gradProgramCode, pen, accessToken);
    }
}
