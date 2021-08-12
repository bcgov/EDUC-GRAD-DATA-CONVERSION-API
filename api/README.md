# GRAD BATCH GRADUATION API
## Build Setup

- #### Download ojdbc8.jar oracle driver file
```
https://gww.svn.educ.gov.bc.ca/svn/repos/openshiftdevs_repos/drivers/ojdbc8.jar
```

- #### Install the ojdbc oracle driver in the local maven repository
```
mvn install:install-file -Dfile=.\ojdbc8.jar -DgroupId=com.oracle.ojdbc -DartifactId=ojdbc8 -Dversion=12.2.0.1 -Dpackaging=jar
```

- #### Run application with local properties
```
mvn clean install -Dspring.profiles.active=dev
```

- #### Run application with default properties
```
mvn clean install
```

## START and STOP API

- #### Start API
```
mvn spring-boot:start
```

- ####  Shutdown API
```
mvn spring-boot:stop
```

## Kill a running API on Windows
```
#Find the Process ID
netstat -ano | find "<port#>"

#Kill the process by Process ID
Taskkill /F /IM <Process-ID>
```