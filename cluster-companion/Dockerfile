FROM bellsoft/liberica-openjdk-alpine:11

COPY build /app/build

WORKDIR /app

EXPOSE 60001

CMD java -jar build/libs/cluster-companion-1.0.jar