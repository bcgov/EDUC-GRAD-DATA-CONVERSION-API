package ca.bc.gov.educ.api.dataconversion.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

public class EducGradDataConversionApiUtils {

    private EducGradDataConversionApiUtils() {}

    private static final Logger logger = LoggerFactory.getLogger(EducGradDataConversionApiUtils.class);
    private static final String ERROR_MSG  = "Error {}";

    public static HttpHeaders getHeaders (String accessToken)
    {
		HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/json");
        httpHeaders.setBearerAuth(accessToken);
        return httpHeaders;
    }
    
    public static HttpHeaders getHeaders (String username,String password)
    {
		HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.setBasicAuth(username, password);
        return httpHeaders;
    }

    public static String getSimpleDateFormat(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat(EducGradDataConversionApiConstants.DEFAULT_DATE_FORMAT);
        return formatter.format(date);
    }

    public static Date parseDate (String dateString, String dateFormat) {
        if (dateString == null || "".compareTo(dateString) == 0)
            return null;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        Date date = new Date();

        try {
            date = simpleDateFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return date;
    }

    public static Date parseDate(String dateString) {
        if (dateString == null || "".compareTo(dateString) == 0)
            return null;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(EducGradDataConversionApiConstants.DEFAULT_DATE_FORMAT);
        Date date = new Date();

        try {
            date = simpleDateFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return date;
    }

    public static String parsingNFormating(String inDate) {
        String actualDate = inDate + "/01";
        String sDates = null;
        Date temp = EducGradDataConversionApiUtils.parseDate(actualDate, EducGradDataConversionApiConstants.SECONDARY_DATE_FORMAT);
        sDates = EducGradDataConversionApiUtils.formatDate(temp, EducGradDataConversionApiConstants.DEFAULT_DATE_FORMAT);
        return sDates;
    }

    public static String parsingDateForCertificate(String sessionDate) {
        String actualSessionDate = sessionDate + "/01";
        String sDates = null;
        Date temp = parseDate(actualSessionDate, EducGradDataConversionApiConstants.SECONDARY_DATE_FORMAT);
        sDates = formatDate(temp, EducGradDataConversionApiConstants.DEFAULT_DATE_FORMAT);
        return sDates;
    }

    public static String formatDateForReportJasper(String updatedTimestamp) {
        SimpleDateFormat fromUser = new SimpleDateFormat(EducGradDataConversionApiConstants.DEFAULT_DATE_FORMAT);
        SimpleDateFormat myFormat = new SimpleDateFormat(EducGradDataConversionApiConstants.DEFAULT_DATE_FORMAT);
        try {
            return myFormat.format(fromUser.parse(updatedTimestamp));
        } catch (ParseException e) {
            logger.debug(ERROR_MSG,e.getLocalizedMessage());
        }
        return updatedTimestamp;

    }

    public static String formatDate (Date date) {
        if (date == null)
            return null;
        return getSimpleDateFormat(date);
    }

    public static String formatDate (Date date, String dateFormat) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        return simpleDateFormat.format(date);
    }

    public static Date formatIssueDateForReportJasper(String updatedTimestamp) {
        SimpleDateFormat fromUser = new SimpleDateFormat(EducGradDataConversionApiConstants.DEFAULT_DATE_FORMAT);
        SimpleDateFormat myFormat = new SimpleDateFormat(EducGradDataConversionApiConstants.DEFAULT_DATE_FORMAT);
        try {
            return new SimpleDateFormat(EducGradDataConversionApiConstants.DEFAULT_DATE_FORMAT).parse(myFormat.format(fromUser.parse(updatedTimestamp)));
        } catch (ParseException e) {
            logger.debug(ERROR_MSG,e.getLocalizedMessage());
        }
        return null;

    }

    public static int getDifferenceInMonths(String date1, String date2) {
        Period diff = Period.between(
                LocalDate.parse(date1).withDayOfMonth(1),
                LocalDate.parse(date2).withDayOfMonth(1));
        int monthsYear = diff.getYears() * 12;
        int months = diff.getMonths();



        return monthsYear + months;
    }

    private static String parseDateByFormat(final String sessionDate, final String dateFormat) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        try {
            Date date = simpleDateFormat.parse(sessionDate);
            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return localDate.getYear() +"/"+ String.format("%02d", localDate.getMonthValue());
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Integer getNumberOfCredits(final String credits) {
        String digits = StringUtils.getDigits(credits);
        if (StringUtils.isNotBlank(digits) && NumberUtils.isCreatable(digits.trim())) {
            return Integer.valueOf(digits.trim());
        }
        return null;
    }

    public static Double getPercentage(final String percentage) {
        if (StringUtils.isNotBlank(percentage) && NumberUtils.isCreatable(percentage.trim())) {
            return Double.valueOf(percentage.trim());
        }
        return null;
    }
}
