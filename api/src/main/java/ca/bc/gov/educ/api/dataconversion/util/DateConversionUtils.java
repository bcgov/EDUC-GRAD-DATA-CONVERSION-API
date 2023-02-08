package ca.bc.gov.educ.api.dataconversion.util;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class DateConversionUtils {
  private DateConversionUtils() {}

  public static Date convertStringToDate(String dateStr) {
    if (StringUtils.isNotBlank(dateStr)) {
      SimpleDateFormat formatter = null;
      try {
        if (dateStr.trim().length() == 6) {
          formatter = new SimpleDateFormat("yyyyMM");
        } else if (dateStr.trim().length() == 7) {
          formatter = new SimpleDateFormat("yyyy/MM");
        } else if (dateStr.trim().length() == 8) {
          formatter = new SimpleDateFormat("yyyyMMdd");
        }
        return formatter.parse(dateStr);
      } catch (ParseException pe) {
        return null;
      }
    }
    return null;
  }

  public static LocalDate convertToLocalDate(Date dateToConvert) {
      return LocalDate.ofInstant(dateToConvert.toInstant(), ZoneId.systemDefault());
  }

  public static Date convertToDate(LocalDate dateToConvert) {
    return java.util.Date.from(dateToConvert.atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant());
  }

  public static Date getLastDayOfMonth(Date currentDate) {
    LocalDate temp = convertToLocalDate(currentDate);
    LocalDate lastDayOfMonth = temp.withDayOfMonth(temp.getMonth().length(temp.isLeapYear()));
    return convertToDate(lastDayOfMonth);
  }
}
