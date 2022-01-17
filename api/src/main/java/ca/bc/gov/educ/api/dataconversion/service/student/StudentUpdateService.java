package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.entity.trax.Event;
import ca.bc.gov.educ.api.dataconversion.repository.student.*;
import ca.bc.gov.educ.api.dataconversion.service.EventService;
import ca.bc.gov.educ.api.dataconversion.service.assessment.AssessmentService;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;
import ca.bc.gov.educ.api.dataconversion.service.course.CourseService;
import ca.bc.gov.educ.api.dataconversion.service.program.ProgramService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static ca.bc.gov.educ.api.dataconversion.constant.EventType.UPDATE_TRAX_STUDENT_MASTER;

@Service
public class StudentUpdateService extends StudentBaseService implements EventService {

    private final GraduationStudentRecordRepository graduationStudentRecordRepository;
    private final StudentOptionalProgramRepository studentOptionalProgramRepository;
    private final StudentCareerProgramRepository studentCareerProgramRepository;
    private final GraduationStudentRecordHistoryRepository graduationStudentRecordHistoryRepository;
    private final StudentOptionalProgramHistoryRepository studentOptionalProgramHistoryRepository;

    private final AssessmentService assessmentService;
    private final CourseService courseService;
    private final ProgramService programService;

    @Autowired
    public StudentUpdateService(GraduationStudentRecordRepository graduationStudentRecordRepository,
                                StudentOptionalProgramRepository studentOptionalProgramRepository,
                                StudentCareerProgramRepository studentCareerProgramRepository,
                                GraduationStudentRecordHistoryRepository graduationStudentRecordHistoryRepository,
                                StudentOptionalProgramHistoryRepository studentOptionalProgramHistoryRepository,
                                AssessmentService assessmentService,
                                CourseService courseService,
                                ProgramService programService) {
        this.graduationStudentRecordRepository = graduationStudentRecordRepository;
        this.studentOptionalProgramRepository = studentOptionalProgramRepository;
        this.studentCareerProgramRepository = studentCareerProgramRepository;
        this.graduationStudentRecordHistoryRepository = graduationStudentRecordHistoryRepository;
        this.studentOptionalProgramHistoryRepository = studentOptionalProgramHistoryRepository;
        this.assessmentService = assessmentService;
        this.courseService = courseService;
        this.programService = programService;
    }

    @Override
    public <T extends Object> void processEvent(T request, Event event) {

    }

    @Override
    public String getEventType() {
        return UPDATE_TRAX_STUDENT_MASTER.toString();
    }
}
