package ca.bc.gov.educ.api.dataconversion.mappers;

import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.GraduationStudentRecord;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConvGradStudentMapper {
    @Autowired
    ModelMapper modelMapper;

    public ConvGradStudent toModel(GraduationStudentRecord gradStudent) {
        ConvGradStudent convGradStudent = modelMapper.map(gradStudent, ConvGradStudent.class);
        return convGradStudent;
    }

    public GraduationStudentRecord toObject(ConvGradStudent convGradStudent) {
        GraduationStudentRecord gradStudent = modelMapper.map(convGradStudent, GraduationStudentRecord.class);
        return gradStudent;
    }
}
