# 회원제 커뮤니티 + 실시간 메시지 (Spring Boot + React)

회원가입/로그인 기반 커뮤니티 기능(게시글, 댓글, 좋아요, 검색/페이징)과
회원 간 쪽지, 실시간 알림(SSE + Polling Fallback), 감사로그(삭제/차단 이력)를 제공하는 샘플 프로젝트입니다.

## 스택

- Backend: Spring Boot 3.3.1, Spring Security, JWT, Spring Data JPA, MariaDB, Gradle
- Frontend: React + Vite, React Router, Axios

## 실행 포트

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`
- Base API URL: `http://localhost:8080/api`

## Swagger UI

- Docs: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- JWT 테스트 시 `Authorize` 버튼에서 아래 값 입력
  - `Type`: `http`
  - `Bearer Token`: `your-jwt`

---

## 1) 설치/실행

### 1-0. 통합 실행/종료 (PowerShell)

루트에서 한 번에 개발 서버를 올리고 내릴 수 있습니다.

```powershell
.\start-dev.ps1
```

```powershell
.\stop-dev.ps1
```

- `start-dev.ps1`: backend `bootJar` 빌드 후 `8080`, frontend dev server를 `5173`으로 실행
- `stop-dev.ps1`: `.run\backend.pid`, `.run\frontend.pid` 기준으로 둘 다 종료
- 로그 파일: `.run\backend.out.log`, `.run\backend.err.log`, `.run\frontend.out.log`, `.run\frontend.err.log`

### 1-0. 백엔드 실행 체크 (PowerShell/Bash 1줄 템플릿)

`JAVA_HOME_17`이 안 잡히면 먼저 아래 3줄만 맞추면 됩니다.  
1) PowerShell: `$env:JAVA_HOME_17="C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot"`  
2) 또는 영구: `setx JAVA_HOME_17 "C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot"`  
3) 아니면 `JAVA_HOME`을 JDK 17 경로로 직접 지정

```powershell
cd .\backend; .\gradlew-check.ps1
```

```bash
cd ./backend && bash ./gradlew-check.sh
```

`Bash`에서도 동일하게 미설정이면 먼저 아래 3줄이면 됩니다.  
1) 임시: `export JAVA_HOME_17=/usr/lib/jvm/jdk-17`  
2) 영구: `echo 'export JAVA_HOME_17=/usr/lib/jvm/jdk-17' >> ~/.bashrc && source ~/.bashrc`  
3) 대체: `export JAVA_HOME=/usr/lib/jvm/jdk-17`  

`gradlew-check.*`는 실행 전 JDK major 버전(17+)을 fail-fast로 점검한 뒤 `[diag] ...` 1줄 진단을 출력합니다.
인자 없이 실행하면 `bootRun`을 수행하고, 인자로 gradlew 옵션/태스크를 넘길 수 있습니다.

### 공통 요구사항

- Java 17+
- MariaDB 10.5+
- Node.js 20+ / npm
- Windows PowerShell/Bash(Linux/macOS) 공용

### 1-1. Backend 실행

`JAVA_HOME_17` 미설정 시 아래 3줄부터 맞춘 뒤 실행하세요.
1) PowerShell: `$env:JAVA_HOME_17 = 'C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot'`
2) PowerShell 영구: `setx JAVA_HOME_17 "C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot"`
3) 대체: `$env:JAVA_HOME = $env:JAVA_HOME_17` (혹은 기존 JAVA_HOME이 JDK 17이면 생략)

```powershell
cd .\backend
if(-not $env:SPRING_PROFILES_ACTIVE){$env:SPRING_PROFILES_ACTIVE='dev'}
.\gradlew-check.ps1 bootRun
```

```bash
cd ./backend
if [ -z "${SPRING_PROFILES_ACTIVE:-}" ]; then export SPRING_PROFILES_ACTIVE=dev; fi
./gradlew-check.sh bootRun
```

> PowerShell은 `.\gradlew-check.ps1`, Bash는 `./gradlew-check.sh`를 사용하세요.

### 1-2. Frontend 실행

```powershell
cd .\frontend
npm install
npm run dev
```

```bash
cd ./frontend
npm install
npm run dev
```

### 1-3. 초기 환경 설정

- `backend/src/main/resources/application.yml`
- 기본 DB: `community`
- DB 사용자: `root`
- DB 비밀번호: `1111`
- 서버 포트: `8080`

### 1-4. Prod Docker 배포 (한 번에 실행)

1. 루트에서 `.env.example`를 복사
```powershell
Copy-Item .env.example .env
```
```bash
cp .env.example .env
```
2. `.env`에서 서비스 도메인/비밀번호/키를 실제 값으로 수정
- `DOMAIN_NAME`, `ENABLE_HTTPS`, `LETSENCRYPT_ENABLED`, `LETSENCRYPT_EMAIL`, `LETSENCRYPT_STAGING`, `LETSENCRYPT_RENEW_INTERVAL_HOURS`, `LETSENCRYPT_LOG_FORMAT`, `APP_ALLOWED_ORIGINS`, `PROXY_HTTP_PORT`, `PROXY_HTTPS_PORT`를 운영 정책에 맞게 설정
3. 아래 명령으로 빌드+실행
```powershell
docker compose -f docker-compose.prod.yml --env-file .env up --build
```
```bash
docker compose -f docker-compose.prod.yml --env-file .env up --build
```
4. 도메인 기반 운영 접속은 `https://도메인` 또는 `http://도메인`(ENABLE_HTTPS=false)
5. HTTPS 모드
   - `LETSENCRYPT_ENABLED=true`면 `proxy` 컨테이너가 `DOMAIN_NAME` 기준으로 최초 인증서를 발급하고 갱신을 계속 수행합니다.
   - 발급/갱신 전제: 도메인 DNS가 이 서버로 향하고, 80/443 포트가 외부에 열려 있어야 합니다.
   - `LETSENCRYPT_ENABLED=false`면 `SSL_CERT_DIR` 또는 `SSL_CERT_PATH`에 인증서를 직접 넣고 `proxy` 시작 시 `SSL_CERT_PATH`, `SSL_KEY_PATH`에 유효한 인증서가 있어야 합니다.
5-1. 모니터링 로그 포맷 (운영)
- `LETSENCRYPT_LOG_FORMAT=json` (권장): 구조화 JSON 이벤트
- `LETSENCRYPT_LOG_FORMAT=kv` : `key=value` 이벤트 라인
  - `container` 이벤트 예시
    - `{"ts":"2026-03-04T00:00:00Z","level":"info","service":"community-proxy","event":"certbot_issue_start","domain":"your.domain","staging":false,"renew_interval_hours":12,"msg":"command starts"}`
    - `ts=... level=info service=community-proxy event=certbot_renew_ok ...`
- 운영 점검
  - 스테이징 전환 점검: `LETSENCRYPT_STAGING=true` (발급은 Let’s Encrypt staging endpoint 사용)
  - 갱신 주기 점검: `LETSENCRYPT_RENEW_INTERVAL_HOURS=12`를 기준으로 주기 로그(`certbot_renew_cycle`, `certbot_renew_wait`)가 남습니다.
  - 최근 이벤트 추적:
    ```powershell
    docker logs community-proxy --since 24h | Select-String "certbot"
    ```
    ```bash
    docker logs community-proxy --since 24h | grep "certbot"
    ```
6. 배포 후 점검(운영: 상태 확인/실시간 로그/restart)
```powershell
docker compose -f .\docker-compose.prod.yml ps
```
```bash
docker compose -f docker-compose.prod.yml ps
```
```powershell
docker compose -f .\docker-compose.prod.yml logs -f --since 24h community-backend
```
```bash
docker compose -f docker-compose.prod.yml logs -f --since 24h community-backend
```
```powershell
docker compose -f .\docker-compose.prod.yml restart community-backend
```
```bash
docker compose -f docker-compose.prod.yml restart community-backend
```

- 6-1) 전체 서비스 일괄 재시작
```powershell
docker compose -f .\docker-compose.prod.yml restart
```
```bash
docker compose -f docker-compose.prod.yml restart
```

- 6-2) 컨테이너 리소스/디스크 사용량 확인
```powershell
docker stats --no-stream
docker volume ls
```
```bash
docker stats --no-stream
docker volume ls
```

- 6-3) 빠른 Healthcheck 확인
```powershell
docker inspect --format "{{.Name}}={{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}" community-backend community-frontend community-proxy community-db
```
```bash
docker inspect --format '{{.Name}}={{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' community-backend community-frontend community-proxy community-db
```


생성 파일
- `docker-compose.prod.yml` : db + backend + frontend + nginx reverse-proxy(도메인/HTTPS) 템플릿
- `backend/Dockerfile` : Spring Boot 이미지 생성
- `frontend/Dockerfile` : Vite 빌드 + Nginx 정적 서빙
- `frontend/nginx.conf` : SPA 라우팅용 Nginx 설정
- `nginx/proxy/Dockerfile` : 리버스 프록시 Nginx 이미지
- `nginx/proxy/default-http.conf.template` : HTTP 프록시 템플릿
- `nginx/proxy/default-https.conf.template` : HTTPS 프록시 템플릿
- `.env.example` : 배포용 환경변수 템플릿

---

## 2) API (Swagger 스타일 정리)

### 인증/보안

- 인증 헤더: `Authorization: Bearer {JWT}`
- `/api/auth/**`는 허용, 그 외는 JWT 필요

| 분류 | Method | Path | 요약 | 권한 | Request | Response | 에러 |
|---|---|---|---|---|---|---|---|
| Auth | `POST` | `/api/auth/register` | 회원가입 | 전체 허용 | `AuthRequest` | `AuthResponse` | `400`, `409` |
| Auth | `POST` | `/api/auth/login` | 로그인 | 전체 허용 | `AuthRequest` | `AuthResponse` | `401`, `400` |
| Auth | `POST` | `/api/auth/me` | 현재 사용자 조회 | 로그인 필요 | 없음 | `AuthResponse`(간단 정보) | `401` |

### 게시글 API

| Method | Path | 요약 | 권한 | Query/Path | Request | Response | 에러 |
|---|---|---|---|---|---|---|---|
| `GET` | `/api/posts` | 게시글 목록/검색/정렬/페이징 | 로그인 필요 | `q`, `page`, `size`, `sort` | 없음 | `Page<PostItemResponse>` | `200` |
| `GET` | `/api/posts/{id}` | 게시글 상세 | 로그인 필요 | `id` | 없음 | `PostDetailResponse` | `404` |
| `POST` | `/api/posts` | 게시글 생성 | 로그인 필요 | none | `PostRequest` | `PostItemResponse` | `400`, `401` |
| `PUT` | `/api/posts/{id}` | 게시글 수정 (소유자만) | 본인 | `id` | `PostRequest` | `PostItemResponse` | `403`, `404` |
| `DELETE` | `/api/posts/{id}` | 게시글 삭제 (소유자만) | 본인 | `id` | 없음 | 없음(`204`) | `403`, `404` |
| `POST` | `/api/posts/{id}/likes` | 좋아요 토글 | 로그인 필요 | `id` | 없음 | `LikeResponse` | `404` |
| `GET` | `/api/posts/{id}/likes/me` | 본인 좋아요 상태 조회 | 로그인 필요 | `id` | 없음 | `LikeResponse` | `404` |

### 댓글 API

| Method | Path | 요약 | 권한 | Path/Query | Request | Response | 에러 |
|---|---|---|---|---|---|---|---|
| `GET` | `/api/posts/{id}/comments` | 댓글 목록 | 로그인 필요 | `id`, `page`, `size` | 없음 | `Page<CommentResponse>` | `404` |
| `POST` | `/api/posts/{id}/comments` | 댓글 작성 | 로그인 필요 | `id` | `CommentRequest` | `CommentResponse` | `400`, `404` |
| `PUT` | `/api/posts/{id}/comments/{commentId}` | 댓글 수정 (본인만) | 본인 | `id`, `commentId` | `CommentRequest` | `CommentResponse` | `403`, `404` |
| `DELETE` | `/api/posts/{id}/comments/{commentId}` | 댓글 삭제 (본인만) | 본인 | `id`, `commentId` | 없음 | 없음(`204`) | `403`, `404` |

### 메시지 API

| Method | Path | 요약 | 권한 | Query/Path | Request | Response | 에러 |
|---|---|---|---|---|---|---|---|
| `POST` | `/api/messages/send` | 쪽지 발송 | 로그인 필요 | none | `MessageRequest` | `MessageResponse` | `400`, `403` |
| `GET` | `/api/messages/received` | 받은 쪽지 목록 | 로그인 필요 | `page`, `size`, `onlyUnread` | 없음 | `Page<MessageResponse>` | `200` |
| `GET` | `/api/messages/sent` | 보낸 쪽지 목록 | 로그인 필요 | `page`, `size` | 없음 | `Page<MessageResponse>` | `200` |
| `GET` | `/api/messages/unread-count` | 미읽음 카운트 | 로그인 필요 | none | none | `number` | `200` |
| `POST` | `/api/messages/{id}/read` | 쪽지 읽음 처리 | 로그인 필요 | `id` | 없음 | `MessageResponse` | `403`, `404` |
| `DELETE` | `/api/messages/{id}` | 쪽지 삭제 (송신/수신 본인만) | 본인 | `id` | none | none(`204`) | `403`, `404` |
| `GET` | `/api/messages/blocks` | 차단 목록 | 로그인 필요 | none | none | `string[]` | `200` |
| `POST` | `/api/messages/block/{username}` | 사용자 차단 | 로그인 필요 | `username` | none | none(`204`) | `400`, `409` |
| `DELETE` | `/api/messages/block/{username}` | 차단 해제 | 로그인 필요 | `username` | none | none(`204`) | `400` |
| `GET` | `/api/messages/stream` | SSE 스트림 | 토큰 필요 | `token`(쿼리) | none | `text/event-stream` | `401`, `503` |

### 운영 로그 API

| Method | Path | 요약 | 권한 | Query | Response |
|---|---|---|---|---|---|
| `GET` | `/api/audit-logs` | 삭제/차단 로그 조회 | 로그인 필요 | `page`, `size` | `Page<OperationLogResponse>` |

### 공통 에러 포맷

`application/json` 응답:

```json
{
  "timestamp": "2026-03-04T00:00:00",
  "status": 403,
  "error": "Forbidden",
  "path": "/api/...",
  "message": "권한이 없습니다."
}
```

---

## 3) DB 스키마 (SHOW CREATE TABLE 기준)

> 아래는 현재 엔티티 기준으로 만든 **예상 DDL 예시**입니다.  
> 실제 운영 DB 스키마는 배포 환경에서 `SHOW CREATE TABLE` 결과로 확정합니다.

### 3-1. DB 생성 예시

```sql
CREATE DATABASE IF NOT EXISTS community
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE community;
```

### 3-2. 각 테이블 조회 예시

```sql
SHOW CREATE TABLE members\G
SHOW CREATE TABLE posts\G
SHOW CREATE TABLE comments\G
SHOW CREATE TABLE post_likes\G
SHOW CREATE TABLE messages\G
SHOW CREATE TABLE message_blocks\G
SHOW CREATE TABLE operation_logs\G
```

### 3-3. 참고 DDL (핵심 구조)

#### `members`

```sql
CREATE TABLE `members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `nickname` varchar(50),
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_username` (`username`),
  UNIQUE KEY `UK_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### `posts`

```sql
CREATE TABLE `posts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(180) NOT NULL,
  `content` longtext NOT NULL,
  `author_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_posts_author` (`author_id`),
  CONSTRAINT `FK_posts_author` FOREIGN KEY (`author_id`) REFERENCES `members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### `comments`

```sql
CREATE TABLE `comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `author_id` bigint NOT NULL,
  `content` longtext NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_comments_post` (`post_id`),
  KEY `FK_comments_author` (`author_id`),
  CONSTRAINT `FK_comments_post` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`),
  CONSTRAINT `FK_comments_author` FOREIGN KEY (`author_id`) REFERENCES `members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### `post_likes`

```sql
CREATE TABLE `post_likes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_post_member` (`post_id`, `member_id`),
  KEY `FK_post_likes_member` (`member_id`),
  CONSTRAINT `FK_post_likes_post` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`),
  CONSTRAINT `FK_post_likes_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### `messages`

```sql
CREATE TABLE `messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_id` bigint NOT NULL,
  `receiver_id` bigint NOT NULL,
  `title` varchar(180) NOT NULL,
  `content` longtext NOT NULL,
  `is_read` bit(1) NOT NULL,
  `read_at` datetime(6),
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_messages_sender` (`sender_id`),
  KEY `FK_messages_receiver` (`receiver_id`),
  CONSTRAINT `FK_messages_sender` FOREIGN KEY (`sender_id`) REFERENCES `members` (`id`),
  CONSTRAINT `FK_messages_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### `message_blocks`

```sql
CREATE TABLE `message_blocks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `owner_id` bigint NOT NULL,
  `blocked_member_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_owner_blocked` (`owner_id`, `blocked_member_id`),
  KEY `FK_blocks_owner` (`owner_id`),
  KEY `FK_blocks_blocked` (`blocked_member_id`),
  CONSTRAINT `FK_blocks_owner` FOREIGN KEY (`owner_id`) REFERENCES `members` (`id`),
  CONSTRAINT `FK_blocks_blocked` FOREIGN KEY (`blocked_member_id`) REFERENCES `members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### `operation_logs`

```sql
CREATE TABLE `operation_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor` varchar(120) NOT NULL,
  `action` varchar(60) NOT NULL,
  `target_type` varchar(40) NOT NULL,
  `target_id` bigint,
  `target_ref` varchar(255),
  `detail` longtext,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_oplog_created_at` (`created_at`),
  KEY `idx_oplog_actor` (`actor`),
  KEY `idx_oplog_action` (`action`),
  KEY `idx_oplog_target_type` (`target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 4) 배포형 운영 체크리스트 (개발/운영 분리 + 환경 변수)

### 4-1. 환경 분리

- 개발(Dev)과 운영(Prod) 프로필 분리
  - `application-dev.yml`, `application-prod.yml` 분리 권장
  - 실행 시 `SPRING_PROFILES_ACTIVE`로 선택
- 공통 보안값 분리
  - `app.jwt.secret`
  - DB 접속정보
  - 로깅/디버그 레벨

### 4-2. 환경 변수 예시

```bash
export SPRING_PROFILES_ACTIVE=prod
export APP_JWT_SECRET=change-this-with-strong-random-string
export SPRING_DATASOURCE_URL=jdbc:mariadb://db-host:3306/community?useSSL=false&serverTimezone=Asia/Seoul
export SPRING_DATASOURCE_USERNAME=community_user
export SPRING_DATASOURCE_PASSWORD=strong-password
export APP_JWT_EXPIRATION_MS=86400000
```

### 4-3. application.yml 권장 반영

`backend/src/main/resources/application.yml`

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
app:
  jwt:
    secret: ${APP_JWT_SECRET}
    expiration-ms: ${APP_JWT_EXPIRATION_MS:86400000}
```

`application-prod.yml`에서 다음 값 제어:

- `server.port` (예: 8080)
- `spring.datasource.url` / `username` / `password`
- `spring.jpa.hibernate.ddl-auto` (`validate` 권장)
- `spring.jpa.show-sql` (`false`)
- 로그 레벨: `org.hibernate.SQL` WARN 이하

### 4-4. 포트/도메인 정리

- Backend 도메인: `https://api.your-domain.com` (예: Cloud Run/EC2/VM)
- Frontend 도메인: `https://app.your-domain.com` (Nginx/Vercel/Netlify)
- CORS 허용 도메인에 실제 Frontend Origin 등록
- SSE 엔드포인트 확인
  - `https://api.your-domain.com/api/messages/stream`
- 프론트에서 `API_BASE_URL`을 운영 API URL로 변경 또는 `.env` 주입

### 4-5. 배포 전 점검

- [ ] 운영 DB 마이그레이션 및 백업 정책 적용
- [ ] JWT Secret 최소 64바이트 이상, 운영 비밀정보 암호화 저장
- [ ] `ddl-auto: validate` 또는 migration 사용
- [ ] HTTPS 적용 + HSTS 설정
- [ ] JWT 갱신 정책/재로그인 처리 점검
- [ ] 프론트 `API_BASE_URL`을 운영 API URL로 변경
- [ ] 로그 보관 주기 설정(운영 로그 TTL or Archiving)
- [ ] SSE 연결 타임아웃/재연결 정책 점검
- [ ] 장애 시 Polling fallback 동작 확인

---

## 5) 트러블슈팅

- `./gradlew`가 동작하지 않을 때
  - PowerShell: `.\gradlew-check.ps1`로 실행(Gradle wrapper 직접 호출 대신 점검 포함 실행)
  - Bash: `./gradlew-check.sh`로 실행
- MariaDB `read` 키워드 충돌
  - `Message` 엔티티의 `read`를 `is_read` 컬럼으로 매핑 적용
- 토큰 만료/권한 오류
  - `Authorization` 헤더와 로그인 상태 확인
- SSE가 열리지 않을 때
  - 프록시/방화벽 타임아웃, CORS, 토큰 전달 여부 점검
- `Unsupported class file major version 69`(BuildScript/Gradle 실행 오류)
  - 현재 JVM/Gradle/JDK 버전 호환성 문제입니다. 현재 프로젝트는 JDK 17 기준(`sourceCompatibility: 17`)입니다.
  - `JAVA_HOME`을 JDK 17 경로로 지정한 뒤 실행하세요. `backend/gradle.properties` 가이드와 동일하게 `JAVA_HOME_17` 우선 적용이 가능합니다.
  - PowerShell 진단/복구 1줄:
    ```powershell
    $env:JAVA_HOME_17="<JDK17_PATH>"; $env:JAVA_HOME=if($env:JAVA_HOME_17){$env:JAVA_HOME_17}else{$env:JAVA_HOME}; if(-not $env:JAVA_HOME){ throw "JAVA_HOME missing" }; $env:Path="$env:JAVA_HOME\bin;$env:Path"; java -version; .\gradlew.bat --version; .\gradlew.bat --stop; cd backend; .\gradlew.bat bootRun
    ```
  - Bash 진단/복구 1줄:
    ```bash
    JAVA_HOME="${JAVA_HOME_17:-$JAVA_HOME}"; [ -z "$JAVA_HOME" ] && { echo 'JAVA_HOME missing'; exit 1; }; export JAVA_HOME; export PATH="$JAVA_HOME/bin:$PATH"; java -version; ./gradlew --version; ./gradlew --stop; cd backend && ./gradlew bootRun
    ```
  - 영구 적용:
    - 시스템 환경변수 `JAVA_HOME` 또는 `JAVA_HOME_17`을 JDK 17 경로로 고정
    - `backend\gradle.properties`에 `org.gradle.java.home=<JDK17경로>` 추가

## 6) 핵심 파일

- [backend/src/main/java/com/example/community/domain/Message.java](backend/src/main/java/com/example/community/domain/Message.java)
- [backend/src/main/java/com/example/community/domain/OperationLog.java](backend/src/main/java/com/example/community/domain/OperationLog.java)
- [backend/src/main/java/com/example/community/service/MessageService.java](backend/src/main/java/com/example/community/service/MessageService.java)
- [backend/src/main/java/com/example/community/service/PostService.java](backend/src/main/java/com/example/community/service/PostService.java)
- [backend/src/main/java/com/example/community/service/OperationLogService.java](backend/src/main/java/com/example/community/service/OperationLogService.java)
- [backend/src/main/java/com/example/community/service/NotificationService.java](backend/src/main/java/com/example/community/service/NotificationService.java)
- [backend/src/main/java/com/example/community/controller/OperationLogController.java](backend/src/main/java/com/example/community/controller/OperationLogController.java)
- [frontend/src/App.jsx](frontend/src/App.jsx)
- [frontend/src/pages/AuditLogPage.jsx](frontend/src/pages/AuditLogPage.jsx)
- [frontend/src/pages/MessagePage.jsx](frontend/src/pages/MessagePage.jsx)
- [backend/src/main/resources/application-dev.yml](backend/src/main/resources/application-dev.yml)
- [backend/src/main/resources/application-prod.yml](backend/src/main/resources/application-prod.yml)
- [backend/src/main/java/com/example/community/config/OpenApiConfig.java](backend/src/main/java/com/example/community/config/OpenApiConfig.java)
