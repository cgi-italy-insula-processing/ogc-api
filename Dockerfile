ARG BASE_IMAGE
FROM ${BASE_IMAGE}
VOLUME /tmp
ARG JAR_FILE
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Xmx4096m","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]