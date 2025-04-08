# Base Image
FROM openjdk:21

# Directory
WORKDIR /app

# 빌드 경로
ARG JAR_PATH=build/libs

# 빌드 파일 복사
COPY ${JAR_PATH}/*.jar saboteur.jar

# 실행
ENTRYPOINT ["java", "-jar", "saboteur.jar"]
