package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ConversionAlert implements Serializable {
  private AlertLevelEnum level = AlertLevelEnum.ERROR;
  private String item;
  private String reason;

  public enum AlertLevelEnum {
    WARNING, ERROR
  }
}
