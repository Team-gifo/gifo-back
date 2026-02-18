# Gradle 8.11 + JDK 21 이미지를 빌드 환경으로 사용
FROM gradle:8.11-jdk21 AS build

# 컨테이너 안의 작업 디렉토리 설정
WORKDIR /app

# 프로젝트 전체 파일을 컨테이너로 복사
COPY . .

# Gradle로 Spring Boot JAR 파일 빌드 (테스트는 건너뜀, 데몬 비활성화)
RUN gradle bootJar -x test --no-daemon

# 실행에는 JRE만 있으면 됨 (JDK보다 이미지 크기가 작음)
FROM eclipse-temurin:21-jre

# 작업 디렉토리 설정
WORKDIR /app

# 빌드 단계에서 만든 JAR 파일만 복사 (소스 코드는 포함하지 않음)
COPY --from=build /app/build/libs/*.jar app.jar

# 컨테이너 시작 시 실행할 명령어 (Spring Boot 앱 실행)
ENTRYPOINT ["java", "-jar", "app.jar"]