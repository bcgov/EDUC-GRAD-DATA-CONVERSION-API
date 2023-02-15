package ca.bc.gov.educ.api.dataconversion.util;

import ca.bc.gov.educ.api.dataconversion.mappers.ConvGradStudentMapper;

//@Component
//@Profile("test")
public class GradConversionTestUtils {
//    @Autowired
//    GraduationStudentRecordRepository graduationStudentRecordRepository;

//    @Autowired
    private ConvGradStudentMapper mapper;

//    public List<GraduationStudentRecord> createGradStudents(final String jsonFileName) throws IOException {
//        final File file = new File(
//                Objects.requireNonNull(GradConversionTestUtils.class.getClassLoader().getResource(jsonFileName)).getFile()
//        );
//        final List<ConvGradStudent> models = new ObjectMapper().readValue(file, new TypeReference<>() {
//        });
//        final var entities = models.stream().map(mapper::toEntity).collect(toList());
//
////        graduationStudentRecordRepository.saveAll(entities);
//        return entities;
//    }


}
