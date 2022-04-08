package ca.bc.gov.educ.api.dataconversion.config;

import ca.bc.gov.educ.api.dataconversion.entity.trax.GraduationCourseEntity;
import ca.bc.gov.educ.api.dataconversion.listener.*;
import ca.bc.gov.educ.api.dataconversion.model.CourseRestriction;
import ca.bc.gov.educ.api.dataconversion.processor.*;
import ca.bc.gov.educ.api.dataconversion.reader.*;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;

import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.writer.*;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableBatchProcessing
public class BatchJobConfig {

    @Autowired
    JobRegistry jobRegistry;

    @Bean
    public ItemReader<ConvGradStudent> studentReader(DataConversionService dataConversionService, RestUtils restUtils) {
        return new DataConversionStudentReader(dataConversionService, restUtils);
    }

    @Bean
    public ItemReader<CourseRestriction> courseRestrictionReader(DataConversionService dataConversionService, RestUtils restUtils) {
        return new DataConversionCourseRestrictionReader(dataConversionService, restUtils);
    }

    @Bean
    public ItemReader<GraduationCourseEntity> courseRequirementReader(DataConversionService dataConversionService, RestUtils restUtils) {
        return new DataConversionCourseRequirementReader(dataConversionService, restUtils);
    }

    @Bean
    public ItemWriter<ConvGradStudent> studentWriter() {
        return new DataConversionStudentWriter();
    }

    @Bean
    public ItemWriter<CourseRestriction> courseRestrictionWriter() {
        return new DataConversionCourseRestrictionWriter();
    }

    @Bean
    public ItemWriter<GraduationCourseEntity> courseRequirementWriter() {
        return new DataConversionCourseRequirementWriter();
    }

    @Bean
    public ItemProcessor<ConvGradStudent,ConvGradStudent> studentProcessor() {
        return new DataConversionStudentProcessor();
    }

    @Bean
    public ItemProcessor<CourseRestriction, CourseRestriction> courseRestrictionProcessor() {
        return new DataConversionCourseRestrictionProcessor();
    }

    @Bean
    public ItemProcessor<GraduationCourseEntity,GraduationCourseEntity> courseRequirementProcessor() {
        return new DataConversionCourseRequirementProcessor();
    }

    @Bean
    public AssessmentRequirementCreator assessmentRequirementCreator() {
        return new AssessmentRequirementCreator();
    }

    @Bean
    public CourseRequirementCreator courseRequirementCreator() {
        return new CourseRequirementCreator();
    }

    /**
    * Creates a bean that represents the only steps of our batch job.
    */
    @Bean
    public Step studentDataConversionJobStep(ItemReader<ConvGradStudent> studentReader,
                                        ItemProcessor<? super ConvGradStudent, ? extends ConvGradStudent> studentProcessor,
                                        ItemWriter<ConvGradStudent> studentWriter,
                                        StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("studentDataConversionJobStep")
            .<ConvGradStudent, ConvGradStudent>chunk(1)
            .reader(studentReader)
            .processor(studentProcessor)
            .writer(studentWriter)
            .build();
    }

    /**
    * Creates a bean that represents our batch job.
    */
    @Bean
    public Job studentDataConversionBatchJob(Step studentDataConversionJobStep,
                                        StudentDataConversionJobCompletionNotificationListener listener,
                                        JobBuilderFactory jobBuilderFactory) {
    return jobBuilderFactory.get("studentDataConversionBatchJob")
            .incrementer(new RunIdIncrementer())
            .listener(listener)
            .flow(studentDataConversionJobStep)
            .end()
            .build();
    }

    /**
     * Creates a bean that represents the only steps of our batch job.
     */
    @Bean
    public Step courseRestrictionDataConversionJobStep(ItemReader<CourseRestriction> courseRestrictionReader,
                                                    ItemProcessor<? super CourseRestriction, ? extends CourseRestriction> courseRestrictionProcessor,
                                                    ItemWriter<CourseRestriction> courseRestrictionWriter,
                                                    StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("courseRestrictionDataConversionJobStep")
                .<CourseRestriction, CourseRestriction>chunk(1)
                .reader(courseRestrictionReader)
                .processor(courseRestrictionProcessor)
                .writer(courseRestrictionWriter)
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean
    public Job courseRestrictionDataConversionBatchJob(Step courseRestrictionDataConversionJobStep,
                                                       CourseRestrictionDataConversionJobCompletionNotificationListener listener,
                                                       JobBuilderFactory jobBuilderFactory) {
        return jobBuilderFactory.get("courseRestrictionDataConversionBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(courseRestrictionDataConversionJobStep)
                .end()
                .build();
    }

    /**
     * Creates a bean that represents the only steps of our batch job.
     */
    @Bean
    public Step courseRequirementDataConversionJobStep(ItemReader<GraduationCourseEntity> courseRequirementReader,
                                                       ItemProcessor<? super GraduationCourseEntity, ? extends GraduationCourseEntity> courseRequirementProcessor,
                                                       ItemWriter<GraduationCourseEntity> courseRequirementWriter,
                                                       StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("courseRequirementDataConversionJobStep")
                .<GraduationCourseEntity, GraduationCourseEntity>chunk(1)
                .reader(courseRequirementReader)
                .processor(courseRequirementProcessor)
                .writer(courseRequirementWriter)
                .build();
    }

    @Bean
    public Step createAssessmentRequirementsJobStep(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("createAssessmentRequirementsJobStep")
                .tasklet(assessmentRequirementCreator())
                .build();
    }

    @Bean
    public Step createCourseRequirementsJobStep(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("createCourseRequirementsJobStep")
                .tasklet(courseRequirementCreator())
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean
    public Job courseRequirementDataConversionBatchJob(Step courseRequirementDataConversionJobStep,
                                                       Step createAssessmentRequirementsJobStep,
                                                       Step createCourseRequirementsJobStep,
                                                       CourseRequirementDataConversionJobCompletionNotificationListener listener,
                                                       JobBuilderFactory jobBuilderFactory) {
        return jobBuilderFactory.get("courseRequirementDataConversionBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(courseRequirementDataConversionJobStep)
                .next(createAssessmentRequirementsJobStep)
                .next(createCourseRequirementsJobStep)
                .build();
    }

    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor() {
        JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
        postProcessor.setJobRegistry(jobRegistry);
        return postProcessor;
    }

    // Partitioning for pen updates ---------------------------------------------------------------------------
    @Bean
    public Step masterStepForPenUpdates(StepBuilderFactory stepBuilderFactory, DataConversionService dataConversionService, EducGradDataConversionApiConstants constants) {
        return stepBuilderFactory.get("masterStepForPenUpdates")
                .partitioner(slaveStepForPenUpdates(stepBuilderFactory).getName(), partitioner(dataConversionService))
                .step(slaveStepForPenUpdates(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public StudentLoadPartitioner partitioner(DataConversionService dataConversionService) {
        // Reader to feed input data for each partition
        return new StudentLoadPartitioner(dataConversionService);
    }

    @Bean
    public Step slaveStepForPenUpdates(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("slaveStepForPenUpdates")
                .tasklet(penUpdatesPartitionHandler())
                .build();
    }

    @Bean
    @StepScope
    public PenUpdatesPartitionHandlerCreator penUpdatesPartitionHandler() {
        // Processor for each partition
        return new PenUpdatesPartitionHandlerCreator();
    }

    @Bean
    public Job penUpdatesJob(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory,
                             DataConversionService dataConversionService,
                             EducGradDataConversionApiConstants constants,
                             PenUpdatesJobCompletionNotificationListener listener) {
        return jobBuilderFactory.get("penUpdatesJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepForPenUpdates(stepBuilderFactory, dataConversionService, constants))
                .end()
                .build();
    }

    // Partitioning for student load ---------------------------------------------------------------------------
    @Bean
    public Step masterStepForStudent(StepBuilderFactory stepBuilderFactory, DataConversionService dataConversionService, RestUtils restUtils, EducGradDataConversionApiConstants constants) {
        return stepBuilderFactory.get("masterStepForStudent")
                .partitioner(slaveStepForStudent(stepBuilderFactory, restUtils).getName(), partitioner(dataConversionService))
                .step(slaveStepForStudent(stepBuilderFactory, restUtils))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step slaveStepForStudent(StepBuilderFactory stepBuilderFactory, RestUtils restUtils) {
        return stepBuilderFactory.get("slaveStepForStudent")
                .<String, ConvGradStudent>chunk(1)
                .reader(studentPartitionReader(restUtils))
                .processor(studentPartitionProcessor())
                .writer(studentPartitionWriter())
                .build();
    }

    @Bean
    @StepScope
    public StudentPartitionReader studentPartitionReader(RestUtils restUtils) {
        return new StudentPartitionReader(restUtils);
    }

    @Bean
    @StepScope
    public StudentPartitionProcessor studentPartitionProcessor() {
        return new StudentPartitionProcessor();
    }

    @Bean
    @StepScope
    public StudentPartitionWriter studentPartitionWriter() {
        return new StudentPartitionWriter();
    }

    @Bean
    public Job studentLoadJob(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory,
                              DataConversionService dataConversionService, RestUtils restUtils,
                              EducGradDataConversionApiConstants constants,
                              StudentDataConversionJobCompletionNotificationListener listener) {
        return jobBuilderFactory.get("studentLoadJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepForStudent(stepBuilderFactory, dataConversionService, restUtils,constants))
                .end()
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor(EducGradDataConversionApiConstants constants) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(constants.getNumberOfPartitions());
        executor.setMaxPoolSize(constants.getNumberOfPartitions());
        executor.setThreadNamePrefix("task_thread-");
        executor.initialize();

        return executor;
    }

}
