FROM bellsoft/liberica-openjdk-alpine:11

COPY target/scala-2.13/hub-assembly-0.1.jar /app/app.jar

WORKDIR /app

EXPOSE 60001
EXPOSE 60002

CMD java -jar app.jar
