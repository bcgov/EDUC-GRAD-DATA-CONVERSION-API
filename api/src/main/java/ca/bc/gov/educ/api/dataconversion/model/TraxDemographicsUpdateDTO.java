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
public class TraxDemographicsUpdateDTO extends TraxStudentUpdateDTO {

    // STUD_SURNAME
    private String lastName;
    // STUD_GIVEN
    private String firstName;
    // STUD_MIDDLE
    private String middleNames;
    // STUD_BIRTH
    private String birthday;

}
