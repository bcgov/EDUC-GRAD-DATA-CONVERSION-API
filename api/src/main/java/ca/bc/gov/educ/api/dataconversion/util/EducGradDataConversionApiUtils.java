package ca.bc.gov.educ.api.dataconversion.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.Date;

public class EducGradDataConversionApiUtils {

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

    public static String formatDate (Date date) {
        if (date == null)
            return null;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(EducGradDataConversionApiConstants.DEFAULT_DATE_FORMAT);
        return simpleDateFormat.format(date);
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
	
}
