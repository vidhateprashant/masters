FROM openjdk:11
COPY target/masters-0.0.1-SNAPSHOT.jar masters.jar
ENTRYPOINT ["java","-jar","masters.jar"]