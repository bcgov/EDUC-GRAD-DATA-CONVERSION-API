package ca.bc.gov.educ.api.dataconversion.util;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateConversionUtils {
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
}
