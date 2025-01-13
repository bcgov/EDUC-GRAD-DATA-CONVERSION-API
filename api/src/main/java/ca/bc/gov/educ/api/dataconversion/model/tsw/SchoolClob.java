package ca.bc.gov.educ.api.dataconversion.model.tsw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchoolClob implements Comparable<SchoolClob> {

	private String minCode;
	private String schoolId;
	private String schoolName;
	private String districtName;
	private String transcriptEligibility;
	private String certificateEligibility;
	private String address1;
	private String address2;
	private String city;
	private String provCode;
	private String countryCode;
	private String postal;
	private String openFlag;
	private String schoolCategoryCode;
	private String schoolCategoryLegacyCode;

	public String getSchoolName() {
		return  schoolName != null ? schoolName.trim(): null;
	}

	public String getDistrictName() {
		return districtName != null ? districtName.trim(): null;
	}

	public String getAddress1() {
		return address1 != null ? address1.trim(): null;
	}

	public String getAddress2() {
		return address2 != null ? address2.trim(): null;
	}

	public String getCity() {
		return city != null ? city.trim(): null;
	}

	public String getPostal() {
		return postal != null ? postal.trim(): null;
	}

	public String getOpenFlag() {
		return openFlag != null ? openFlag.trim(): null;
	}

	@Override
	public String toString() {
		return "SchoolClob [minCode=" + minCode + ", schoolId=" + schoolId + ", schoolCategoryCode=" + schoolCategoryCode + ", schoolCategoryLegacyCode=" + schoolCategoryLegacyCode
				+ ", schoolName=" + schoolName + ", districtName=" + districtName + ", transcriptEligibility=" + transcriptEligibility + ", certificateEligibility=" + certificateEligibility
				+ ", address1=" + address1 + ", address2=" + address2 + ", city=" + city + ", provCode=" + provCode + ", countryCode=" + countryCode + ", postal=" + postal + ", openFlag=" + openFlag
				+ "]";
	}

	@Override
	public int compareTo(SchoolClob o) {
		int result = 0;
		{
			if (result == 0) {
				result = getMinCode().compareToIgnoreCase(o.getMinCode());
			}
			if (result == 0) {
				result = getSchoolName().compareToIgnoreCase(o.getSchoolName());
			}
		}
		return result;
	}
}
