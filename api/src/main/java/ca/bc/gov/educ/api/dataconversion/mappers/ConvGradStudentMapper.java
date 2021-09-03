package ca.bc.gov.educ.api.dataconversion.mappers;

import ca.bc.gov.educ.api.dataconversion.entity.student.GraduationStatusEntity;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConvGradStudentMapper {
    @Autowired
    ModelMapper modelMapper;

    public ConvGradStudent toModel(GraduationStatusEntity gradStudentEntity) {
        ConvGradStudent convGradStudent = modelMapper.map(gradStudentEntity, ConvGradStudent.class);
        return convGradStudent;
    }

    public GraduationStatusEntity toEntity(ConvGradStudent convGradStudent) {
        GraduationStatusEntity gradStudentEntity = modelMapper.map(convGradStudent, GraduationStatusEntity.class);
        return gradStudentEntity;
    }
}
