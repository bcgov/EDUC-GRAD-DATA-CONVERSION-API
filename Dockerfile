FROM artifacts.developer.gov.bc.ca/docker-remote/maven:3-jdk-11 as build
WORKDIR /workspace/app

COPY api/pom.xml .
COPY api/src src
RUN mvn package -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM artifacts.developer.gov.bc.ca/docker-remote/openjdk:11-jdk
RUN useradd -ms /bin/bash spring && mkdir -p /logs && chown -R spring:spring /logs && chmod 755 /logs
USER spring
VOLUME /tmp
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-Duser.name=EDUC_GRAD_DATA_CONVERSION_API","-Xms1500m","-Xmx1500m","-noverify","-XX:TieredStopAtLevel=1",\
"-XX:+UseParallelGC","-XX:MinHeapFreeRatio=20","-XX:MaxHeapFreeRatio=40","-XX:GCTimeRatio=4",\
"-XX:AdaptiveSizePolicyWeight=90","-XX:MaxMetaspaceSize=300m","-XX:ParallelGCThreads=1",\
"-Djava.util.concurrent.ForkJoinPool.common.parallelism=1","-XX:CICompilerCount=2",\
"-XX:+ExitOnOutOfMemoryError","-Djava.security.egd=file:/dev/./urandom",\
"-Dspring.backgroundpreinitializer.ignore=true","-cp","app:app/lib/*",\
"ca.bc.gov.educ.api.dataconversion.EducGradDataConversionApplication"]