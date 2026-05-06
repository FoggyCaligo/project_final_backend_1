## postgre 실행 명령어
psql -h localhost -p 5432 -d today_fridge

## dev 서버 실행 명령어
export MAIL_USERNAME="your-real-gmail@gmail.com"
export MAIL_PASSWORD="16자리앱비밀번호"
export MAIL_FROM="your-real-gmail@gmail.com"
./gradlew bootRun

export DB_URL="jdbc:postgresql://neondb_owner:npg_xhHoOy7L2NWu@ep-wispy-art-amny04jt-pooler.c-5.us-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require"
export DB_USERNAME="neondb_owner"
export DB_PASSWORD="FoggyCaligo123!"
gradle bootrun

## 터미널에서 db 접속하는 명령어
psql "postgresql://neondb_owner:npg_xhHoOy7L2NWu@ep-wispy-art-amny04jt-pooler.c-5.us-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require"


## dbeaver 접속
Host: ep-wispy-art-amny04jt-pooler.c-5.us-east-1.aws.neon.tech
Port: 5432
Database: neondb
Username: postgres
Password: FoggyCaligo123!