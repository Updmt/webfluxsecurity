FROM openjdk:17

WORKDIR /app

COPY wait-for-it.sh /wait-for-it.sh

COPY build/libs/webfluxsecurity-1.0.0.jar /app

ENTRYPOINT ["/wait-for-it.sh", "db:3306", "--", "java", "-jar", "webfluxsecurity-1.0.0.jar"]