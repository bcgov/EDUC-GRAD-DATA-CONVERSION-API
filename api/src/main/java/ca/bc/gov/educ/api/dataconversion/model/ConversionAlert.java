package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;

@Data
public class ConversionAlert {
  private AlertLevelEnum level = AlertLevelEnum.ERROR;
  private String item;
  private String reason;

  public enum AlertLevelEnum {
    WARNING, ERROR
  }
}
