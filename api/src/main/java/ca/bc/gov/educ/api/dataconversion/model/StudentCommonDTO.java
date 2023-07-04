package ca.bc.gov.educ.api.dataconversion.model;

import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Data
public class StudentCommonDTO extends StudentDemographicDTO {
    // grad status
    private String program;
    private String gradDate;
    private String schoolOfRecord;
    private String schoolAtGrad;
    private String studentGrade;
    private String studentStatus;

    // citizenship
    private String citizenship;

    // adultStartDate
    private String adultStartDate;

    // all program codes
    private List<String> programCodes = new ArrayList<>();

    // courses
    private List<StudentCourse> courses = new ArrayList<>();

    // assessments
    private List<StudentAssessment> assessments = new ArrayList<>();

    public boolean isGraduated() {
        boolean graduated = StringUtils.isNotBlank(gradDate);
        if (graduated && "SCCP".equalsIgnoreCase(program)) {
            return isGradDatePast();
        }
        return graduated;
    }

    private boolean isGradDatePast() {
        String gradDateStr = gradDate.length() < 10? gradDate + "/01" : gradDate;
        log.debug("GRAD Student Current Data: Grad Date = {}", gradDateStr);
        SimpleDateFormat dateFormat = new SimpleDateFormat(gradDate.length() < 10? EducGradDataConversionApiConstants.SECONDARY_DATE_FORMAT : EducGradDataConversionApiConstants.DEFAULT_DATE_FORMAT);
        try {
            Date dt = dateFormat.parse(gradDateStr);
            Calendar calGradDate = Calendar.getInstance();
            calGradDate.setTime(dt);
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());
            return calGradDate.before(now);
        } catch (ParseException e) {
            log.error("Date Parse Exception: gradDate = {}. format = {}", gradDateStr, dateFormat.toPattern());
            return false;
        }
    }
}
