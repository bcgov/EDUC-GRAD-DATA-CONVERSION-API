package ca.bc.gov.educ.api.dataconversion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConvGradStudent {
  private String pen;
  private String program;
  private Date programCompletionDate;
  private String gpa;
  private String honoursStanding;
  private String recalculateGradStatus;
  private String studentGradData;
  private String schoolOfRecord;
  private String schoolAtGrad;
  private String studentGrade;
  private String studentStatus;

  // extra
  private String graduationRequestYear;
}
