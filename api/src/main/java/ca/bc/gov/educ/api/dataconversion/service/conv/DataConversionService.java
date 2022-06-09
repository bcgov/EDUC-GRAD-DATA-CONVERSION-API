package ca.bc.gov.educ.api.dataconversion.service.conv;

import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class DataConversionService {
    private final RestUtils restUtils;

    @Autowired
    public DataConversionService(RestUtils restUtils) {
        this.restUtils = restUtils;
    }

    public List<ConvGradStudent> getStudentMasterDataFromTrax(String pen, String accessToken) {
        return restUtils.getTraxStudentMasterDataByPen(pen, accessToken);
    }

    public List<Student> getStudentDemographicsDataFromTrax(String pen, String accessToken) {
        return restUtils.getTraxStudentDemographicsDataByPen(pen, accessToken);
    }

    public TraxStudentNo readTraxStudentAndAddNewPen(TraxStudentNo traxStudentNo, ConversionStudentSummaryDTO summary) {
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        try {
            String accessToken = summary.getAccessToken();
            Student penStudent = getPenStudent(traxStudentNo.getStudNo(), accessToken, summary);
            if (penStudent == null) {
                Student traxStudent = readTraxStudent(traxStudentNo.getStudNo(), accessToken);
                if (traxStudent != null) {
                    if (StringUtils.equals(traxStudent.getStatusCode(), "M") && StringUtils.isNotBlank(traxStudent.getTruePen())) {
                        log.debug("Merged student is skipped: pen# {}", traxStudent.getPen());
                        return traxStudentNo;
//                        // MergedToStudent
//                        Student penMergedToStudent = getPenStudent(traxStudent.getTruePen(), accessToken, summary);
//                        if (penMergedToStudent == null) {
//                            // Create MergedToStudent
//                            penMergedToStudent = readTraxStudent(traxStudent.getTruePen());
//                            if (penMergedToStudent != null) {
//                                penMergedToStudent.setDemogCode("A");
//                                penMergedToStudent = createNewPen(penMergedToStudent, accessToken, summary);
//                            }
//                        }
//                        // TrueStudentID
//                        traxStudent.setTrueStudentID(penMergedToStudent != null? penMergedToStudent.getStudentID() : null);
//                        traxStudent.setDemogCode("A");
                    }
                    // MergedFromStudent
                    createNewPen(traxStudent, accessToken, summary);
                    saveTraxStudent(traxStudentNo.getStudNo(), "C", accessToken);
                }
            } else {
                log.info("Student already exists : pen# {} => studentID {}", traxStudentNo.getStudNo(), penStudent.getStudentID());
                saveTraxStudent(traxStudentNo.getStudNo(), "Y", accessToken);
            }
            return traxStudentNo;
        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setItem(traxStudentNo.getStudNo());
            error.setReason("Unexpected Exception is occurred: " + e.getLocalizedMessage());
            summary.getErrors().add(error);
            return null;
        }
    }

    private Student readTraxStudent(String pen, String accessToken) {
        List<Student> students = getStudentDemographicsDataFromTrax(pen, accessToken);
        if (students != null && !students.isEmpty()) {
            return students.get(0);
        }
        return null;
    }

    private Student getPenStudent(String pen, String accessToken, ConversionStudentSummaryDTO summary) {
        Student student = null;
        try {
            // Call PEN Student API
            List<Student> students = restUtils.getStudentsByPen(pen, accessToken);
            if (students != null && !students.isEmpty()) {
                student = students.get(0);
            }
        } catch (Exception e) {
            ConversionAlert error = new ConversionAlert();
            error.setItem(pen);
            error.setReason("PEN Student API is failed: " + e.getLocalizedMessage());
            summary.getErrors().add(error);
        }
        return student;
    }

    private Student createNewPen(Student student, String accessToken, ConversionStudentSummaryDTO summary) {
        if (StringUtils.isBlank(student.getHistoryActivityCode())) {
            student.setHistoryActivityCode("REQNEW");
        }
        if (StringUtils.isBlank(student.getDemogCode())) {
            student.setDemogCode("A");
        }
        Student newStudent = restUtils.addNewPen(student, accessToken);
        if (newStudent != null) {
            log.info("Add missing student: pen# {} => studentID {}", student.getPen(), newStudent.getStudentID());
            summary.setAddedCount(summary.getAddedCount() + 1L);
        }
        return newStudent;
    }

    public void saveTraxStudent(String studNo, String status, String accessToken) {
        TraxStudentNo traxStudentNo = new TraxStudentNo();
        traxStudentNo.setStudNo(studNo);
        traxStudentNo.setStatus(status);
        restUtils.saveTraxStudentNo(traxStudentNo, accessToken);
    }

    public List<CourseRestriction> loadGradCourseRestrictionsDataFromTrax(String accessToken) {
        return restUtils.getTraxCourseRestrictions(accessToken);
    }

    public List<GradCourse> loadGradCourseRequirementsDataFromTrax(String accessToken) {
        return restUtils.getTraxCourseRequirements(accessToken);  // .subList(0,1)
    }
}
