package ca.bc.gov.educ.api.dataconversion.model;

import ca.bc.gov.educ.api.dataconversion.constant.FieldName;
import ca.bc.gov.educ.api.dataconversion.constant.FieldType;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Builder
@Data
public class OngoingUpdateFieldDTO {
    private FieldType type;
    private FieldName name;
    private Object value;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OngoingUpdateFieldDTO that = (OngoingUpdateFieldDTO) o;
        return getName() == that.getName();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    @Override
    public String toString() {
        return "OngoingUpdateField{" +
                "type=" + type +
                ", name=" + name +
                ", value=" + value +
                '}';
    }

}
