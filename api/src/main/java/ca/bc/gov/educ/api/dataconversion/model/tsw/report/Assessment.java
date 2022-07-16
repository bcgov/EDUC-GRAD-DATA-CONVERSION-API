package ca.bc.gov.educ.api.dataconversion.model.tsw.report;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class Assessment implements Serializable {

    private static final long serialVersionUID = 2L;

    private Date issueDate;
    private List<AssessmentResult> results;

    @JsonFormat(pattern="yyyy-MM-dd")
    public Date getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Date value) {
        this.issueDate = value;
    }

    @JsonProperty("results")
    public List<AssessmentResult> getResults() {
        return results;
    }

    public void setResults(List<AssessmentResult> value) {
        this.results = value;
    }
}
