package ca.bc.gov.educ.api.dataconversion.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
public class GradStudentTranscriptValidationKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private String pen;
    private UUID studentID;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GradStudentTranscriptValidationKey {");
        sb.append("pen=").append(pen).append(", ");
        sb.append("studentID=").append(studentID);
        sb.append('}');
        return sb.toString();
    }
}
