package ca.bc.gov.educ.api.dataconversion.model.tsw.report;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;

public class OrderType implements Serializable {

    private static final long serialVersionUID = 2L;

    private String name;
    private CertificateType certificateType;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    @JsonDeserialize(as = CertificateType.class)
    public CertificateType getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(CertificateType value) {
        this.certificateType = value;
    }
}
