FROM 745222113226.dkr.ecr.us-east-1.amazonaws.com/interop/api:develop
# openjdk:11.0.7-jdk-slim
ADD reference-api-webapp/target/hspc-reference-api-webapp-*.jar app.jar
# RUN apt-get update -y && apt-get dist-upgrade -y
# RUN apt-get install -y default-mysql-client
ENV JAVA_OPTS="-Xmx1536m -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar app.jar" ]