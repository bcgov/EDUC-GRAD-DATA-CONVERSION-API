package ca.bc.gov.educ.api.dataconversion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraxGraduationUpdateDTO extends TraxStudentUpdateDTO {

    // GRAD_REQT_YEAR
    private String graduationRequirementYear;
    // STUD_GRAD
    private String studentGrade;
    // MINCODE
    private String schoolOfRecord;
    // SLP_DATE
    private String slpDate;
    // STUD_CITIZ
    private String citizenship;

    public String getSlpDateWithDefaultFormat() {
        if (StringUtils.isBlank(slpDate)) {
            return null;
        }
        return slpDate.substring(0,4) + "/" + slpDate.substring(4,6);
    }

}
