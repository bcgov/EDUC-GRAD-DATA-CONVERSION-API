package ca.bc.gov.educ.api.dataconversion.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BusinessException extends Exception {

  @Getter
  private final ca.bc.gov.educ.api.dataconversion.exception.BusinessError businessError;

  public BusinessException(ca.bc.gov.educ.api.dataconversion.exception.BusinessError businessError, String... messageArgs) {
    super(businessError.getCode());
    this.businessError = businessError;
    var finalLogMessage = businessError.getCode();
    if (messageArgs != null) {
      finalLogMessage = getFormattedMessage(finalLogMessage, messageArgs);
    }
    log.error(finalLogMessage);
  }

  /**
   * Gets formatted message.
   *
   * @param msg           the msg
   * @param substitutions the substitutions
   * @return the formatted message
   */
  private static String getFormattedMessage(String msg, String... substitutions) {
    final String format = msg.replace("$?", "%s");
    return String.format(format, (Object[]) substitutions);
  }
}

