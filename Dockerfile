FROM openjdk:14-alpine
COPY build/libs/finance-*-all.jar finance.jar
EXPOSE 8080
CMD ["java", "-Dcom.sun.management.jmxremote", "-Xmx128m", "-jar", "finance.jar"]