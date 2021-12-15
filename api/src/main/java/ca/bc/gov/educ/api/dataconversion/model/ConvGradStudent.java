package ca.bc.gov.educ.api.dataconversion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConvGradStudent {
  private String pen;
  private String program;
  private Date programCompletionDate;
  private String slpDate;
  private String gpa;
  private String honoursStanding;
  private String recalculateGradStatus;
  private String studentGradData;
  private String schoolOfRecord;
  private String schoolAtGrad;
  private String studentGrade;
  private String studentStatus;
  private String archiveFlag;

  // extra
  private String graduationRequestYear;

  // program codes for optional / career program
  private List<String> programCodes;

  // grad or non-grad
  private boolean graduated;
}
