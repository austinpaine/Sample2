FROM openjdk:8-alpine

COPY target/uberjar/sample2.jar /sample2/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/sample2/app.jar"]
