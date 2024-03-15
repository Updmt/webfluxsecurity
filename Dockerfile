FROM openjdk:17

WORKDIR /app

COPY build/libs/webfluxsecurity-1.0.0.jar /app

ENTRYPOINT ["java", "-jar", "webfluxsecurity-1.0.0.jar"]