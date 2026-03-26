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
- `LIGHTSAIL_ACCESS_KEY_ID`, `LIGHTSAIL_SECRET_ACCESS_KEY`, `LIGHTSAIL_BUCKET_NAME`, `LIGHTSAIL_BUCKET_REGION`, `LIGHTSAIL_ENDPOINT_URL`, `LIGHTSAIL_CDN_DOMAIN` — AWS Lightsail Object Storage
- `API_SERVER_URL` — Swagger 서버 목록에 사용되는 HTTPS 서버 URL (없으면 Swagger에 잘못된 서버 항목이 표시됨)

> **환경변수 추가 시 규칙**: `application.yml`에 새 환경변수를 추가할 때는 반드시 `.env.example`에도 해당 변수를 (빈 값으로) 추가해야 합니다.

테스트는 `test` 프로파일(`application-test.yml`)로 H2 인메모리 DB에서 실행됩니다.

## 알려진 문제

- `AiProperties.java`의 `@ConfigurationProperties(prefix = "azure.openai")`가 실제 yml 경로 `spring.ai.azure.openai`와 다름. 현재 dead code이므로 사용하지 말 것 — AI 설정은 `AiConfig`의 `ChatClient` 빈을 사용.

## 아키텍처

### 패키지 구조

레이어별로 중앙화하고, 각 레이어 안에서 도메인 서브패키지로 나눕니다.

```
com.gifo.backend/
├── entity/                    # JPA 엔티티
│   ├── event/                 # BirthdayEvent, Memory, EventStatus
│   ├── gift/                  # Gift
│   ├── capsule/               # CapsuleEvent, Capsule, CapsuleDraw
│   ├── direct/                # DirectEvent
│   └── quiz/                  # QuizEvent, Quiz, QuizChoice, QuizAttempt, QuizRewardRule, QuizType
├── repository/                # Spring Data JPA 레포지토리
│   ├── event/                 # BirthdayEventRepository, MemoryRepository
│   ├── gift/                  # GiftRepository
│   ├── capsule/               # CapsuleEventRepository, CapsuleRepository, CapsuleDrawRepository
│   ├── direct/                # DirectEventRepository
│   └── quiz/                  # QuizEventRepository, QuizRepository, ...
├── service/                   # 비즈니스 로직
│   ├── event/                 # EventService
│   ├── gift/                  # GiftService
│   ├── capsule/               # CapsuleService
│   ├── direct/                # DirectService
│   └── quiz/                  # QuizService
├── controller/                # REST 컨트롤러
│   ├── event/
│   ├── gift/
│   ├── capsule/
│   ├── direct/
│   └── quiz/
├── dto/                       # DTO (Request / Response)
│   ├── event/
│   ├── gift/
│   ├── capsule/
│   ├── direct/
│   └── quiz/
├── mapper/                    # MapStruct 매퍼
│   ├── event/
│   └── ...
├── global/                    # 공통 관심사
│   ├── ApiResponse            # 공통 응답 래퍼 {code, message, data<T>}
│   ├── ErrorCode              # 에러 코드 enum
│   ├── ErrorResponse          # 에러 응답 바디
│   ├── util/
│   │   └── EntityFinder       # DB 단순 조회 예외 처리 유틸리티
│   └── exception/
│       ├── CustomException              # 기본 커스텀 예외
│       ├── GlobalExceptionHandler       # @RestControllerAdvice
│       └── {domain}/                   # 도메인별 예외 패키지
│           └── {Domain}Exception.java  # CustomException 확장
├── ai/                        # Azure OpenAI
│   ├── config/
│   └── controller/
├── config/                    # SwaggerConfig
└── healthcheck/
```

### 새 도메인 추가 체크리스트

도메인 `{domain}` (예: `member`)을 추가할 때 아래 순서로 파일을 생성합니다.

**1. 엔티티** — `entity/{domain}/`
```
패키지: com.gifo.backend.entity.member
파일:   Member.java
```

**2. 레포지토리** — `repository/{domain}/`
```java
// 패키지: com.gifo.backend.repository.member
public interface MemberRepository extends JpaRepository<Member, Long> {
}
```

**3. 도메인 예외** — `global/exception/{domain}/`
```java
// 패키지: com.gifo.backend.global.exception.member
public class MemberException extends CustomException {
    public MemberException(ErrorCode errorCode) {
        super(errorCode);
    }
}
```

**4. 에러 코드** — `global/ErrorCode.java`에 도메인 섹션 추가
```java
// Member 도메인
MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
```

**5. EntityFinder** — `global/util/EntityFinder.java`에 레포지토리 주입 및 메서드 추가
```java
private final MemberRepository memberRepository;

public Member getMemberOrThrow(Long memberId) {
    return memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));
}
```

**6. DTO** — `dto/{domain}/`
```
패키지: com.gifo.backend.dto.member
파일:   MemberCreateRequest.java, MemberResponse.java
```

**7. Mapper (필요 시)** — `mapper/{domain}/`
```java
// 패키지: com.gifo.backend.mapper.member
@Mapper(componentModel = "spring")
public interface MemberMapper {
    MemberResponse toResponse(Member member);
}
```

**8. 서비스** — `service/{domain}/`
```java
// 패키지: com.gifo.backend.service.member
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final EntityFinder entityFinder;
    private final MemberMapper memberMapper;

    public MemberResponse getMember(Long id) {
        Member member = entityFinder.getMemberOrThrow(id);
        return memberMapper.toResponse(member);
    }
}
```

**9. 컨트롤러** — `controller/{domain}/`
```java
// 패키지: com.gifo.backend.controller.member
@RestController
@RequestMapping("/members")
@Tag(name = "Member API", description = "회원 관련 API")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> getMember(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("조회 성공", memberService.getMember(id)));
    }
}
```

### 핵심 패턴

#### API 응답
모든 컨트롤러 응답은 `ApiResponse<T>` 래퍼를 사용합니다.

```java
return ResponseEntity.ok(ApiResponse.success("조회 성공", data));  // 데이터 포함
return ResponseEntity.ok(ApiResponse.success("삭제 성공"));         // 데이터 없음
```

#### 예외 처리
`GlobalExceptionHandler`가 `CustomException`을 catch하므로 도메인 예외만 throw하면 자동 처리됩니다. 위 체크리스트의 3~5단계를 따릅니다. `componentModel = "spring"` 필수.

#### AI
`AiConfig`가 생성한 `ChatClient` 빈을 서비스/컨트롤러에 주입해 사용합니다.

### 로컬 주요 URL

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API 문서: `http://localhost:8080/api-docs`
- 헬스 체크: `http://localhost:8080/health`

## 커밋 메시지 컨벤션

형식: `[type] 설명` (한국어 설명)

| type | 의미 | 사용 예시 |
|------|------|-----------|
| `feat` | 새로운 기능 추가 | feat: 게시글 등록 API 구현 |
| `fix` | 버그 수정 | fix: JWT 검증 오류 수정 |
| `refactor` | 기능 변화 없는 코드 리팩토링 | refactor: 서비스 로직 구조 개선 |
| `style` | 코드 스타일 변경 (포맷, 공백 등) | style: 코드 포맷 정리 |
| `docs` | 문서 수정 | docs: README 업데이트 |
| `test` | 테스트 코드 추가/수정 | test: 회원가입 서비스 테스트 추가 |
| `chore` | 설정, 빌드, 의존성, 환경파일 변경 | chore: application.yml 설정 수정 |
| `deploy` | 배포 관련 작업 | deploy: 운영 배포 설정 수정 |
| `remove` | 코드/파일 삭제 | remove: 사용하지 않는 DTO 삭제 |
| `hotfix` | 긴급 수정 | hotfix: 운영 서버 로그인 오류 수정 |
| `perf` | 성능 개선 | perf: DB 조회 쿼리 최적화 |
| `init` | 초기 프로젝트 생성 | init: Spring Boot 프로젝트 생성 |

## PR 작성 규칙

- 작업 단위가 다르면 브랜치를 분리하고 PR을 새로 생성합니다. 기존 PR에 관련 없는 변경사항을 추가하지 않습니다.
- PR 본문은 반드시 `docs/pull_request_template.md` 양식을 따릅니다.

```
## 📌 작업 개요
## ✨ 주요 변경 사항
## 🖼️ 기능 살펴 보기
## ✅ 작업 체크리스트
## 📂 테스트 방법
## 💬 기타 참고 사항
## 📎 관련 이슈 / 문서
```

`gh` CLI로 PR 생성 시:
```bash
gh pr create --title "제목" --body "$(cat docs/pull_request_template.md)"
```

## CI/CD

`main` 브랜치 push 시 GitHub Actions가 `Team-gifo/gifo-deploy` 레포에 `repository_dispatch` 이벤트를 전송해 배포를 트리거합니다. `ACCESS_TOKEN` secret이 필요합니다.
