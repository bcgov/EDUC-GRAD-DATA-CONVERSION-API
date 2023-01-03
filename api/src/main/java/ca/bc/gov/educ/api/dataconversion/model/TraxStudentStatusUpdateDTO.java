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
public class TraxStudentStatusUpdateDTO extends TraxStudentUpdateDTO {

    // STUD_STATUS
    private String studentStatus;
    // ARCHIVE_FLAG
    private String archiveFlag;

}
