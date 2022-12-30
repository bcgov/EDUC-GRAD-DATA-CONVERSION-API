package ca.bc.gov.educ.api.dataconversion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraxFrenchImmersionUpdateDTO extends TraxStudentUpdateDTO {

    // GRAD_REQT_YEAR
    private String graduationRequirementYear;
    // French Immersion Course
    private String courseCode;
    // French Immersion Level
    private String courseLevel;

}
