FROM openjdk:21 AS build

RUN microdnf install findutils

ENV APP_DIR /usr/src/app
WORKDIR $APP_DIR
RUN mkdir -p $APP_DIR
COPY . .

RUN ./gradlew clean build -x test

FROM openjdk:21

ENV APP_DIR /usr/src/app
WORKDIR $APP_DIR
COPY --from=build $APP_DIR/build/libs/*.jar .

ENTRYPOINT ["sh", "-c", "java -jar *-SNAPSHOT.jar"]
