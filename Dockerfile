# ---------- build ---------- #
# wrapper로 실행해서 로컬-ci-docker 빌드를 일치시킴
FROM bellsoft/liberica-openjdk-alpine:17 AS builder

ARG MODULE
WORKDIR /workspace

# 의존성 해석에 필요한 파일만 먼저 복사해 레이어 캐시를 활용함
COPY gradlew ./
COPY gradle gradle
COPY settings.gradle build.gradle ./
COPY common/build.gradle common/
COPY eureka-server/build.gradle eureka-server/
COPY gateway/build.gradle gateway/
COPY user-service/build.gradle user-service/
COPY company-service/build.gradle company-service/
COPY hub-service/build.gradle hub-service/
COPY order-service/build.gradle order-service/
COPY delivery-service/build.gradle delivery-service/
COPY message-service/build.gradle message-service/

RUN chmod +x gradlew && ./gradlew :${MODULE}:dependencies --no-daemon || true

COPY . .

# 실행 권한 없이 커밋된 환경에서도 빌드되도록 다시 부여한다.
# 테스트는 제외
RUN chmod +x gradlew && ./gradlew :${MODULE}:bootJar --no-daemon -x test

# bootJar만 골라낸다
RUN cp $(ls /workspace/${MODULE}/build/libs/*.jar | grep -v plain) /workspace/app.jar


# ---------- runtime ---------- #
# eclipse-temurin의 alpine 태그는 arm64 이미지가 없어 liberica alpine을 사용합니다, 심지어 크기도 작음
FROM bellsoft/liberica-openjre-alpine:17

# 헬스체크용
RUN apk add --no-cache curl

# root로 실행하지 않함
RUN addgroup -S bonae && adduser -S bonae -G bonae
USER bonae

WORKDIR /app
COPY --from=builder --chown=bonae:bonae /workspace/app.jar app.jar

# 컨테이너 메모리 비율로 잡음
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"
ENV TZ=Asia/Seoul

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

