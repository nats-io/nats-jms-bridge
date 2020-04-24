FROM openjdk:8-jre-alpine
ADD build/libs/natsjmsbridge-*.jar natsjmsbridge.jar
EXPOSE 8080
CMD java -Xmx2g -Xmx2g -jar /natsjmsbridge.jar
