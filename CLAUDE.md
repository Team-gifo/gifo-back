# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Gifo 백엔드 — Spring Boot 4.0.2 REST API, Java 21, Gradle, PostgreSQL, Azure OpenAI 통합.

## 명령어

```bash
# 빌드
./gradlew build

# 로컬 실행 (루트의 .env 파일에 DB 및 Azure 자격증명 필요)
./gradlew bootRun

# 테스트 실행 — test 프로파일로 H2 인메모리 DB 사용
# PostgreSQL 연결 시도를 막으려면 테스트 클래스에 @ActiveProfiles("test") 필요
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests "com.gifo.backend.BackendApplicationTests"

# 테스트 없이 JAR 빌드
./gradlew bootJar -x test

# Docker 이미지 빌드
docker build -t gifo-backend:latest .
```

## 환경 설정

앱은 프로젝트 루트의 `.env` 파일에서 자격증명을 읽습니다 (`spring.config.import: optional:file:.env[.properties]`로 자동 로드). 필수 환경 변수:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` — PostgreSQL
- `AZURE_OPENAI_ENDPOINT`, `AZURE_OPENAI_API_KEY`, `AZURE_OPENAI_DEPLOYMENT_NAME` — Azure OpenAI
- `API_SERVER_URL` — Swagger 서버 목록에 사용되는 HTTPS 서버 URL (없으면 Swagger에 잘못된 서버 항목이 표시됨)

테스트는 `test` 프로파일(`application-test.yml`)로 H2 인메모리 DB에서 실행됩니다.

## 알려진 문제

- `AiProperties.java`의 `@ConfigurationProperties(prefix = "azure.openai")`가 실제 yml 경로 `spring.ai.azure.openai`와 다름. 현재 dead code이므로 사용하지 말 것 — AI 설정은 `AiConfig`의 `ChatClient` 빈을 사용.
- `application.yml`의 `springdoc` 설정이 `spring:` 하위에 중첩되어 있음 (`spring.springdoc.xxx`). 올바른 위치는 루트 레벨 (`springdoc.xxx`). Swagger 경로 커스터마이징이 무시될 수 있음.

## 아키텍처

### 패키지 구조

도메인별 패키지 구조를 사용합니다. 새 기능은 `com.gifo.backend.{domain}/` 하위에 구성합니다.

```
com.gifo.backend/
├── global/                    # 공통 관심사
│   ├── ApiResponse            # 공통 응답 래퍼 {code, message, data<T>}
│   ├── ErrorCode              # 에러 코드 enum
│   ├── ErrorResponse          # 에러 응답 바디
│   └── exception/
│       ├── CustomException              # 기본 커스텀 예외
│       ├── GlobalExceptionHandler       # @RestControllerAdvice
│       └── {domain}/                   # 도메인별 예외 패키지
│           └── {Domain}Exception.java  # CustomException 확장
├── util/
│   └── EntityFinder           # DB 단순 조회 예외 처리 유틸리티
├── ai/                        # Azure OpenAI 도메인
│   ├── config/                # AiConfig (ChatClient 빈)
│   └── controller/
├── config/                    # SwaggerConfig
├── healthcheck/
└── {domain}/                  # 새 도메인 (예: member, gift ...)
    ├── controller/
    ├── service/
    ├── repository/
    ├── entity/
    ├── dto/                   # 도메인별 DTO
    └── mapper/                # MapStruct 매퍼 인터페이스
```

### 핵심 패턴

#### API 응답
모든 컨트롤러 응답은 `ApiResponse<T>` 래퍼를 사용합니다.

```java
// 데이터 포함 응답
return ResponseEntity.ok(ApiResponse.success("조회 성공", data));

// 데이터 없는 응답
return ResponseEntity.ok(ApiResponse.success("삭제 성공"));
```

#### 예외 처리
**1단계** — `ErrorCode` enum에 도메인별 에러 코드를 추가합니다.

```java
// ErrorCode.java 에 도메인 섹션 추가
// Member 도메인
MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 회원입니다."),
```

**2단계** — `global/exception/{domain}/` 패키지에 `CustomException`을 확장하는 도메인 예외 클래스를 정의합니다.

```java
// global/exception/member/MemberNotFoundException.java
public class MemberNotFoundException extends CustomException {
    public MemberNotFoundException() {
        super(ErrorCode.MEMBER_NOT_FOUND);
    }
}
```

**3단계** — `global/util/EntityFinder`에 도메인 Repository를 주입하고 `get{Entity}OrThrow` 메서드를 추가합니다.

```java
// global/util/EntityFinder.java 에 추가
@Component
@RequiredArgsConstructor
public class EntityFinder {

    private final MemberRepository memberRepository;

    public Member getMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
```

서비스에서는 `EntityFinder`를 주입받아 호출합니다.

```java
@RequiredArgsConstructor
public class MemberService {
    private final EntityFinder entityFinder;

    public MemberResponse getMember(Long id) {
        Member member = entityFinder.getMemberOrThrow(id);
        // ...
    }
}
```

`GlobalExceptionHandler`가 `CustomException`을 catch하므로 별도 핸들러 추가 없이 자동 처리됩니다.

#### DTO
도메인별 `dto/` 패키지에 Request/Response DTO를 정의합니다.

```
member/dto/MemberCreateRequest.java
member/dto/MemberResponse.java
```

#### MapStruct
엔티티 ↔ DTO 변환이 필요한 경우 도메인별 `mapper/` 패키지에 MapStruct 인터페이스를 정의합니다.

```java
// member/mapper/MemberMapper.java
@Mapper(componentModel = "spring")
public interface MemberMapper {
    MemberResponse toResponse(Member member);
    Member toEntity(MemberCreateRequest request);
}
```

서비스에서 `@RequiredArgsConstructor`로 주입받아 사용합니다. `componentModel = "spring"` 필수.

#### AI
`AiConfig`가 생성한 `ChatClient` 빈을 서비스/컨트롤러에 주입해 사용합니다.

### 로컬 주요 URL

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API 문서: `http://localhost:8080/api-docs`
- 헬스 체크: `http://localhost:8080/health`

## 커밋 메시지 컨벤션

`[chore]`, `[feat]`, `[fix]`, `[refactor]` 접두어 사용 (한국어 설명).

## CI/CD

`main` 브랜치 push 시 GitHub Actions가 `Team-gifo/gifo-deploy` 레포에 `repository_dispatch` 이벤트를 전송해 배포를 트리거합니다. `ACCESS_TOKEN` secret이 필요합니다.
