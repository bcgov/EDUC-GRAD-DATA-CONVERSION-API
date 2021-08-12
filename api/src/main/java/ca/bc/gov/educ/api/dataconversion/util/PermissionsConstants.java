package ca.bc.gov.educ.api.dataconversion.util;

public interface PermissionsConstants {
	String _PREFIX = "#oauth2.hasAnyScope('";
	String _SUFFIX = "')";

	String LOAD_STUDENT_IDS = _PREFIX + "LOAD_STUDENT_IDS" + _SUFFIX;
}
