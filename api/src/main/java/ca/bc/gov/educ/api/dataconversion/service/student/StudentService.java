package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class StudentService {
    private static final Logger logger = LoggerFactory.getLogger(StudentService.class);

    private final RestUtils restUtils;
    final WebClient webClient;

    @Autowired
    public StudentService(RestUtils restUtils, WebClient webClient) {
        this.restUtils = restUtils;
        this.webClient = webClient;
    }

    @Transactional
    @Retry(name = "searchbypen")
    public Student getStudentByPen(String pen, String accessToken) {
        logger.debug("Get Student by PEN [Service]");

        Student student;
        List<Student> gradStudentList = new ArrayList<>();

        try {
            gradStudentList = restUtils.getStudentsByPen(pen, accessToken);
            student = gradStudentList.stream().filter(s -> s.getPen().compareTo(pen) == 0).findAny().orElse(null);
        } catch (Exception e) {
            log.error("Failed to retrieve PEN [{}] : {} ", pen, e.getLocalizedMessage());
            return null;
        }

        if (student == null) {
            log.error("PEN NOT FOUND [{}]", pen);
            return null;
        }
        return student;
    }

    @Transactional
    public String cascadeDeleteStudent(String pen, String accessToken) {
        logger.debug("Cascade Delete a Student [Service]");

        //GET Student by PEN
        Student student = getStudentByPen(pen, accessToken);
        logger.debug("After GET student");
        String studentID;

        if (student != null) {
            studentID = student.getStudentID();
            logger.debug("Student ID: [{}]", studentID);

            /*
                Delete All student related data ({STUDENT_API}/api/v1/student/conv/studentid/{studentID})
                This will delete student data from the following tables:
                STUDENT_RECORD_NOTE, STUDENT_CAREER_PROGRAMS, STUDENT_OPTIONAL_PROGRAM_HISTORY,
                STUDENT_OPTIONAL_PROGRAM, GRADUATION_STUDENT_RECORD_HISTORY, GRADUATION_STUDENT_RECORD
             */
            restUtils.removeAllStudentRelatedData(UUID.fromString(studentID), accessToken);

            /*
                Delete all Student certificates, transcripts and reports from API_GRAD_REPORT schema
                Tables: STUDENT_CERTIFICATE, STUDENT_TRANSCRIPT and STUDENT_REPORT
             */
            restUtils.removeAllStudentAchievements(UUID.fromString(studentID), accessToken);

            /*
                Update TRAX_STUDENT_NO status to NULL
             */
            restUtils.updateTraxStudentNo(pen, accessToken);
        }
        return pen;
    }
}
