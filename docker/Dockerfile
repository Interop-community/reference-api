FROM openjdk:11.0.7-jdk-slim

# mapped to host for filesystem mounting
VOLUME /tmp

ADD target/hspc-reference-api-webapp-*.jar app.jar

ADD pom.xml /pom.xml

RUN apt-get update -y && apt-get dist-upgrade -y

RUN apt-get install -y default-mysql-client

ENV JAVA_OPTS="-Xmx1536m -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar app.jar" ]
