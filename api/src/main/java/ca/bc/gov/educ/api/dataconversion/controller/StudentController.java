package ca.bc.gov.educ.api.dataconversion.controller;

import ca.bc.gov.educ.api.dataconversion.model.Student;
import ca.bc.gov.educ.api.dataconversion.service.student.StudentService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(EducGradDataConversionApiConstants.GRAD_DATA_CONVERSION_API_ROOT_MAPPING)
@CrossOrigin
@OpenAPIDefinition(info = @Info(title = "API for Adhoc Student Operations",
        description = "This API is for running adhoc student operations invoking the endpoints manually.", version = "1"))
public class StudentController {

    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    @Autowired
    StudentService studentService;

    @GetMapping(EducGradDataConversionApiConstants.GRAD_STUDENT_BY_PEN_STUDENT_API)
    @PreAuthorize("hasAuthority('SCOPE_READ_GRAD_STUDENT_DATA')")
    @Operation(summary = "Search For Student by PEN", description = "Search for Student Demographics by PEN", tags = { "Students" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    public Student getGradStudentByPenFromStudentAPI(@PathVariable String pen, @RequestHeader(name="Authorization") String accessToken) {
        logger.debug("Get Student by PEN [Controller]");
        return studentService.getStudentByPen(pen,accessToken.replaceAll("Bearer ", ""));
    }

    @DeleteMapping(EducGradDataConversionApiConstants.GRAD_CASCADE_DELETE_STUDENT_BY_PEN)
    @PreAuthorize("hasAuthority('SCOPE_READ_GRAD_STUDENT_DATA')")
    @Operation(summary = "Delete a Student by PEN", description = "Delete a Student and all related data by PEN", tags = { "Students" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    public void cascadeDeleteStudent(@PathVariable String pen, @RequestHeader(name="Authorization") String accessToken) {
        logger.debug("Cascade Delete a Student [Controller]");
        studentService.cascadeDeleteStudent(pen,accessToken.replaceAll("Bearer ", ""));
    }
}
