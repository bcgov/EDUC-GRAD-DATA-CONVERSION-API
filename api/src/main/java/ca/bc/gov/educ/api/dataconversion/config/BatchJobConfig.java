package ca.bc.gov.educ.api.dataconversion.config;

import ca.bc.gov.educ.api.dataconversion.listener.*;
import ca.bc.gov.educ.api.dataconversion.model.CourseRestriction;
import ca.bc.gov.educ.api.dataconversion.model.GradCourse;
import ca.bc.gov.educ.api.dataconversion.processor.*;
import ca.bc.gov.educ.api.dataconversion.reader.*;

import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.writer.*;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;

@Configuration
@EnableBatchProcessing
public class BatchJobConfig {

    @Bean
    public ItemReader<CourseRestriction> courseRestrictionReader(RestUtils restUtils) {
        return new DataConversionCourseRestrictionReader(restUtils);
    }

    @Bean
    public ItemReader<GradCourse> courseRequirementReader(RestUtils restUtils) {
        return new DataConversionCourseRequirementReader(restUtils);
    }

    @Bean
    public ItemWriter<CourseRestriction> courseRestrictionWriter() {
        return new DataConversionCourseRestrictionWriter();
    }

    @Bean
    public ItemWriter<GradCourse> courseRequirementWriter() {
        return new DataConversionCourseRequirementWriter();
    }

    @Bean
    public ItemProcessor<CourseRestriction, CourseRestriction> courseRestrictionProcessor() {
        return new DataConversionCourseRestrictionProcessor();
    }

    @Bean
    public ItemProcessor<GradCourse,GradCourse> courseRequirementProcessor() {
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
    public Step courseRestrictionDataConversionJobStep(
            JobRepository jobRepository, PlatformTransactionManager transactionManager,
            ItemReader<CourseRestriction> courseRestrictionReader,
            ItemProcessor<? super CourseRestriction, ? extends CourseRestriction> courseRestrictionProcessor,
            ItemWriter<CourseRestriction> courseRestrictionWriter) {
        return new StepBuilder("courseRestrictionDataConversionJobStep", jobRepository)
                .<CourseRestriction, CourseRestriction>chunk(1, transactionManager)
                .reader(courseRestrictionReader)
                .processor(courseRestrictionProcessor)
                .writer(courseRestrictionWriter)
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean
    public Job courseRestrictionDataConversionBatchJob(
            JobRepository jobRepository,
            Step courseRestrictionDataConversionJobStep,
            CourseRestrictionDataConversionJobCompletionNotificationListener listener) {
        return new JobBuilder("courseRestrictionDataConversionBatchJob", jobRepository)
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
    public Step courseRequirementDataConversionJobStep(
            JobRepository jobRepository, PlatformTransactionManager transactionManager,
            ItemReader<GradCourse> courseRequirementReader,
            ItemProcessor<? super GradCourse, ? extends GradCourse> courseRequirementProcessor,
            ItemWriter<GradCourse> courseRequirementWriter) {
        return new StepBuilder("courseRequirementDataConversionJobStep", jobRepository)
                .<GradCourse, GradCourse>chunk(1, transactionManager)
                .reader(courseRequirementReader)
                .processor(courseRequirementProcessor)
                .writer(courseRequirementWriter)
                .build();
    }

    @Bean
    public Step createAssessmentRequirementsJobStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("createAssessmentRequirementsJobStep", jobRepository)
                .tasklet(assessmentRequirementCreator(), transactionManager)
                .build();
    }

    @Bean
    public Step createCourseRequirementsJobStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("createCourseRequirementsJobStep", jobRepository)
                .tasklet(courseRequirementCreator(), transactionManager)
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean
    public Job courseRequirementDataConversionBatchJob(
            JobRepository jobRepository,
            Step courseRequirementDataConversionJobStep,
            Step createAssessmentRequirementsJobStep,
            Step createCourseRequirementsJobStep,
            CourseRequirementDataConversionJobCompletionNotificationListener listener) {
        return new JobBuilder("courseRequirementDataConversionBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(courseRequirementDataConversionJobStep)
                .next(createAssessmentRequirementsJobStep)
                .next(createCourseRequirementsJobStep)
                .build();
    }

    // Partitioning for pen updates ---------------------------------------------------------------------------
    @Bean
    public Step masterStepForPenUpdates(
            JobRepository jobRepository, PlatformTransactionManager transactionManager,
            RestUtils restUtils, EducGradDataConversionApiConstants constants) {
        return new StepBuilder("masterStepForPenUpdates", jobRepository)
                .partitioner(slaveStepForPenUpdates(jobRepository, transactionManager).getName(), partitioner(restUtils))
                .step(slaveStepForPenUpdates(jobRepository, transactionManager))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    @StepScope
    public StudentLoadPartitioner partitioner(RestUtils restUtils) {
        // Reader to feed input data for each partition
        return new StudentLoadPartitioner(restUtils);
    }

    @Bean
    public Step slaveStepForPenUpdates(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("slaveStepForPenUpdates", jobRepository)
                .tasklet(penUpdatesPartitionHandler(), transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public PenUpdatesPartitionHandlerCreator penUpdatesPartitionHandler() {
        // Processor for each partition
        return new PenUpdatesPartitionHandlerCreator();
    }

    @Bean
    public Job penUpdatesJob(
            JobRepository jobRepository, PlatformTransactionManager transactionManager,
            RestUtils restUtils,
            EducGradDataConversionApiConstants constants,
            PenUpdatesJobCompletionNotificationListener listener) {
        return new JobBuilder("penUpdatesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepForPenUpdates(jobRepository, transactionManager, restUtils, constants))
                .end()
                .build();
    }

    // Partitioning for student load ---------------------------------------------------------------------------
    @Bean
    public Step masterStepForStudent(JobRepository jobRepository, PlatformTransactionManager transactionManager, RestUtils restUtils, EducGradDataConversionApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepForStudent", jobRepository)
                .partitioner(slaveStepForStudent(jobRepository, transactionManager, restUtils, skipListener).getName(), partitioner(restUtils))
                .step(slaveStepForStudent(jobRepository, transactionManager, restUtils, skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step slaveStepForStudent(JobRepository jobRepository, PlatformTransactionManager transactionManager, RestUtils restUtils, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("slaveStepForStudent", jobRepository)
                .<String, ConvGradStudent>chunk(1, transactionManager)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionSystemException.class)
                .skip(IOException.class)
                .skip(SocketTimeoutException.class)
                .skipLimit(100)
                .reader(studentPartitionReader(restUtils))
                .processor(studentPartitionProcessor())
                .writer(studentPartitionWriter())
                .listener(skipListener)
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
    public Job studentLoadJob(
            JobRepository jobRepository, PlatformTransactionManager transactionManager,
            RestUtils restUtils,
            EducGradDataConversionApiConstants constants,
            StudentDataConversionJobCompletionNotificationListener listener,
            SkipSQLTransactionExceptionsListener skipListener) {
        return new JobBuilder("studentLoadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepForStudent(jobRepository, transactionManager, restUtils, constants, skipListener))
                .on("*")
                .end().build()
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor(EducGradDataConversionApiConstants constants) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(constants.getNumberOfPartitions());
        executor.setMaxPoolSize(constants.getNumberOfPartitions());
        executor.setThreadNamePrefix("partition-");
        executor.initialize();

        return executor;
    }

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(JobRegistry jobRegistry) {
        JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
        postProcessor.setJobRegistry(jobRegistry);
        return postProcessor;
    }
}
