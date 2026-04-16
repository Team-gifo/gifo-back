# Gifo Backend

생일 이벤트 선물 증정 플랫폼 **Gifo**의 백엔드 API 서버입니다.

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.2 |
| Build | Gradle |
| Database | PostgreSQL (운영), H2 (테스트) |
| ORM | Spring Data JPA / Hibernate |
| AI | Azure OpenAI (Spring AI) |
| Storage | AWS Lightsail Object Storage (S3 호환) |
| API 문서 | SpringDoc OpenAPI (Swagger UI) |
| 기타 | MapStruct, Lombok, Thumbnailator |

## 주요 기능

- **이벤트 관리**: 생일 이벤트 생성 및 URL 기반 조회
- **캡슐 뽑기(Gacha)**: 가중치 기반 랜덤 추첨, 뽑기 이력 관리, 선물 선택
- **퀴즈**: 객관식/OX/주관식, 문항별 시도 횟수 관리, 성공/실패 보상 판정
- **언박싱(Unboxing)**: 확정 선물 개봉
- **이미지 업로드**: CDN 연동, 자동 리사이즈(최대 1920×1920, JPEG 80%) 및 WEBP 감지
- **BGM**: 프리셋 제공 및 커스텀 파일 업로드
- **AI**: Azure OpenAI 기반 기능

## 시작하기

### 사전 요구사항

- JDK 21+
- PostgreSQL (로컬 실행 시)

### 환경변수 설정

프로젝트 루트에 `.env` 파일을 생성합니다. `.env.example`을 참고하세요.

```properties
# PostgreSQL
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/gifo
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password

# Azure OpenAI
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com
AZURE_OPENAI_API_KEY=your_api_key
AZURE_OPENAI_DEPLOYMENT_NAME=your_deployment_name

# AWS Lightsail Object Storage
LIGHTSAIL_ACCESS_KEY_ID=your_access_key
LIGHTSAIL_SECRET_ACCESS_KEY=your_secret_key
LIGHTSAIL_BUCKET_NAME=your_bucket_name
LIGHTSAIL_BUCKET_REGION=ap-northeast-2
LIGHTSAIL_ENDPOINT_URL=https://your-bucket.us-east-1.cs.amazonlightsail.com
LIGHTSAIL_CDN_DOMAIN=https://your-cdn-domain.com

# Swagger 서버 URL
API_SERVER_URL=https://your-api-server.com
```

### 로컬 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

### 테스트 실행

```bash
# 전체 테스트 (H2 인메모리 DB 사용)
./gradlew test

# 단일 클래스 테스트
./gradlew test --tests "com.gifo.backend.BackendApplicationTests"
```

### Docker 실행

```bash
# 이미지 빌드
docker build -t gifo-backend:latest .

# 컨테이너 실행
docker run -p 8080:8080 --env-file .env gifo-backend:latest
```

## 로컬 주요 URL

| 서비스 | URL |
|-------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| API Docs | http://localhost:8080/api-docs |
| Health Check | http://localhost:8080/health |

## API 개요

전체 API 명세는 [docs/api-spec-open.md](docs/api-spec-open.md)를 참고하세요.

### 주요 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/events/{eventUrl}` | 이벤트 전체 콘텐츠 조회 |
| `POST` | `/events/{eventUrl}/capsules/draw` | 캡슐 1회 뽑기 |
| `POST` | `/events/{eventUrl}/capsules/select` | 최종 선물 선택 |
| `POST` | `/events/{eventUrl}/quiz/answer` | 퀴즈 답안 제출 |
| `POST` | `/events/{eventUrl}/quiz/result` | 퀴즈 최종 결과 조회 |
| `DELETE` | `/events/{eventUrl}/progress` | 진행 데이터 리셋 |
| `POST` | `/images` | 이미지 업로드 |
| `GET` | `/bgm/presets` | 프리셋 BGM 목록 조회 |
| `POST` | `/bgm/upload` | 커스텀 BGM 업로드 |

### 공통 응답 형식

```json
{
  "code": "SUCCESS",
  "message": "성공 메시지",
  "data": { ... }
}
```

## 패키지 구조

```
com.gifo.backend/
├── entity/          # JPA 엔티티
├── repository/      # Spring Data JPA 레포지토리
├── service/         # 비즈니스 로직
├── controller/      # REST 컨트롤러
├── dto/             # DTO (Request / Response)
├── mapper/          # MapStruct 매퍼
├── global/          # 공통 응답, 예외 처리, 유틸리티
├── ai/              # Azure OpenAI 설정 및 컨트롤러
├── config/          # Swagger, Storage, Web 설정
└── healthcheck/     # 헬스 체크 엔드포인트
```

## CI/CD

`main` 브랜치 push 시 GitHub Actions가 `Team-gifo/gifo-deploy` 레포에 `repository_dispatch` 이벤트를 전송하여 배포를 자동 트리거합니다.
