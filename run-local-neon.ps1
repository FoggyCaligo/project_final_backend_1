# Neon DB로 로컬 서버 실행
$env:DB_URL = "jdbc:postgresql://ep-wispy-art-amny04jt-pooler.c-5.us-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "FoggyCaligo123!"

./gradlew bootRun
