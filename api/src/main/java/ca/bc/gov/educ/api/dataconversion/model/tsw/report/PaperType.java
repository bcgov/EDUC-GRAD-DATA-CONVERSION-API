package ca.bc.gov.educ.api.dataconversion.model.tsw.report;

import java.io.Serializable;

public class PaperType implements Serializable {

    private static final long serialVersionUID = 2L;

    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String value) {
        this.code = value;
    }
}
