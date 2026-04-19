#!/bin/sh

APP_HOME=$(cd "${0%/*}" && pwd -P)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -f "$WRAPPER_JAR" ]; then
  exec java -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
fi

echo "ERROR: gradle-wrapper.jar 가 없습니다."
echo "이 레포는 wrapper-ready 상태이며, 현재 환경에서는 Gradle 미설치로 jar를 생성하지 못했습니다."
echo "아래 중 하나를 실행해 wrapper를 완성하세요:"
echo "  1) 로컬에 Gradle이 있으면: gradle wrapper"
echo "  2) 다른 Spring/Gradle 프로젝트의 gradle/wrapper/gradle-wrapper.jar 를 복사"
exit 1
