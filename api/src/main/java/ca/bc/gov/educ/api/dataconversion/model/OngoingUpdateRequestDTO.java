package ca.bc.gov.educ.api.dataconversion.model;

import ca.bc.gov.educ.api.dataconversion.constant.EventType;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
public class OngoingUpdateRequestDTO {
    private String studentID;
    private String pen;
    private EventType eventType;
    private List<OngoingUpdateFieldDTO> updateFields = new ArrayList<>();
}
