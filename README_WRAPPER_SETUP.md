# Gradle Wrapper 안내

이 프로젝트에는 아래 파일이 포함되어 있습니다.

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.properties`

다만 현재 패키지에는 `gradle/wrapper/gradle-wrapper.jar` 는 포함되어 있지 않습니다.
이 JAR는 일반적으로 `gradle wrapper` 명령으로 생성됩니다.

## wrapper 완성 방법

### 방법 1: 로컬에 Gradle이 설치되어 있는 경우
```bash
gradle wrapper
```

### 방법 2: 다른 Gradle 프로젝트에서 복사
아래 파일을 같은 위치에 복사합니다.

```text
gradle/wrapper/gradle-wrapper.jar
```

## 이후 실행
Windows:
```bat
gradlew.bat bootRun
```

macOS / Linux / Git Bash:
```bash
./gradlew bootRun
```
