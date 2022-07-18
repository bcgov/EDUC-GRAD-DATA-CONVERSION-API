package ca.bc.gov.educ.api.dataconversion.model.tsw.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonPropertyOrder({ "options", "data"})
public class ReportRequest implements Serializable {

	private static final long serialVersionUID = 2L;

	private ReportData data;
	private ReportOptions options;

	@JsonProperty("data")
	public ReportData getData() {
		return data;
	}
	public void setData(ReportData data) {
		this.data = data;
	}

	@JsonProperty("options")
	public ReportOptions getOptions() {
		return options;
	}
	public void setOptions(ReportOptions options) {
		this.options = options;
	}
}
