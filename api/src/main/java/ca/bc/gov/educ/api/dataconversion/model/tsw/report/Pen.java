package ca.bc.gov.educ.api.dataconversion.model.tsw.report;

import java.io.Serializable;

public class Pen implements Serializable {

    private static final long serialVersionUID = 2L;

    private String pen;
    private Object entityID;

    public String getPen() {
        return pen;
    }

    public void setPen(String value) {
        this.pen = value;
    }

    public Object getEntityID() {
        return entityID;
    }

    public void setEntityID(Object value) {
        this.entityID = value;
    }
}
