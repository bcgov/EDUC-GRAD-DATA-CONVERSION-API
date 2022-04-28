package ca.bc.gov.educ.api.dataconversion.util;

public interface PermissionsConstants {
	String _PREFIX = "hasAuthority('";
	String _SUFFIX = "')";

	String LOAD_STUDENT_IDS = _PREFIX + "SCOPE_LOAD_STUDENT_IDS" + _SUFFIX;
}
