# 선물 개봉(이벤트 오픈) API 명세서

> Base URL: `{서버주소}/events`

---

# 이미지 업로드 API 명세서

> Base URL: `{서버주소}/images`

## 공통 응답 형식

모든 응답은 `ApiResponse<T>` 래퍼로 감싸서 반환됩니다.

```json
{
  "code": "SUCCESS",
  "message": "성공 메시지",
  "data": { ... }
}
```

### 에러 응답

```json
{
  "code": "INVALID_FILE_TYPE",
  "message": "허용되지 않는 파일 형식입니다. JPEG, PNG만 업로드 가능합니다."
}
```

| 에러 코드 | HTTP 상태 | 설명 |
|-----------|----------|------|
| `EMPTY_FILE` | 400 | 빈 파일 업로드 시도 |
| `INVALID_FILE_TYPE` | 400 | JPEG, PNG 외 파일 형식 |
| `FILE_SIZE_EXCEEDED` | 400 | 파일 크기 20MB 초과 |
| `STORAGE_UPLOAD_FAILED` | 500 | S3 업로드 실패 |

---

## 1. 이미지 업로드

이미지를 AWS Lightsail Object Storage(S3 호환)에 업로드하고 CDN URL을 반환합니다.
`type`에 따라 저장 경로가 분리됩니다 (`memories/`, `gifts/`, `quizzes/`).

### Request

```http
POST /images
Content-Type: multipart/form-data
```

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| `file` | form-data | MultipartFile | O | 업로드할 이미지 파일 (JPEG, PNG) |
| `type` | query | String | O | 이미지 용도 — `MEMORY` / `GIFT` / `QUIZ` |

### Response (200 OK)

```json
{
  "code": "SUCCESS",
  "message": "이미지 업로드 성공",
  "data": {
    "imageUrl": "https://cdn.example.com/gifts/550e8400-e29b-41d4-a716-446655440000.jpg"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `imageUrl` | String | 업로드된 이미지의 CDN URL |

### 참고

- 업로드된 이미지는 최대 1920×1920으로 자동 리사이즈되며 JPEG quality 0.8로 압축됩니다.
- 반환된 `imageUrl`을 선물 포장(이벤트 생성) 요청의 이미지 필드에 사용합니다.

---

## 공통 응답 형식

모든 응답은 `ApiResponse<T>` 래퍼로 감싸서 반환됩니다.

```json
{
  "code": "SUCCESS",
  "message": "성공 메시지",
  "data": { ... }
}
```

### 에러 응답

```json
{
  "code": "EVENT_NOT_FOUND",
  "message": "이벤트를 찾을 수 없습니다."
}
```

| 에러 코드 | HTTP 상태 | 설명 |
|-----------|----------|------|
| `EVENT_NOT_FOUND` | 404 | 이벤트 URL이 존재하지 않음 |
| `EVENT_EXPIRED` | 410 | 만료된 이벤트 |
| `EVENT_DELETED` | 404 | 삭제된 이벤트 |
| `CAPSULE_NOT_FOUND` | 404 | 캡슐 이벤트가 없음 |
| `CAPSULE_DRAW_LIMIT_EXCEEDED` | 400 | 뽑기 횟수 초과 |
| `CAPSULE_ALL_DRAWN` | 400 | 모든 캡슐을 이미 뽑음 |
| `CAPSULE_DRAW_NOT_FOUND` | 404 | 해당 캡슐 뽑기 이력을 찾을 수 없음 |
| `QUIZ_NOT_FOUND` | 404 | 퀴즈 이벤트가 없음 |
| `QUIZ_ALREADY_ANSWERED` | 400 | 이미 답변한 문제 |
| `QUIZ_NOT_ALL_ANSWERED` | 400 | 모든 문제를 아직 풀지 않음 |
| `QUIZ_QUESTION_NOT_FOUND` | 404 | 해당 퀴즈 문제를 찾을 수 없음 |
| `QUIZ_NO_ATTEMPTS_LEFT` | 400 | 시도 횟수 소진 |
| `INVALID_ARGUMENT` | 400 | 잘못된 요청 |

---

## 1. 이벤트 전체 콘텐츠 조회

URL 접속 시 갤러리 + 이벤트 콘텐츠를 한 번에 조회합니다.

### Request

```http
GET /events/{eventUrl}
```

| 파라미터 | 위치 | 타입 | 설명 |
|---------|------|------|------|
| `eventUrl` | path | String | 이벤트 고유 URL (8자리) |

### Response (200 OK)

```json
{
  "code": "SUCCESS",
  "message": "이벤트 조회 성공",
  "data": {
    "user": "김철수",
    "subTitle": "살 떨리는",
    "bgm": "https://example.com/bgm.mp3",
    "gallery": [
      {
        "title": "첫 만남",
        "imageUrl": "https://example.com/photo1.jpg",
        "description": "강남역에서 처음 만났던 날"
      }
    ],
    "content": {
      "gacha": { ... },
      "quiz": null,
      "unboxing": null
    }
  }
}
```

> **content** 안에 `gacha`, `quiz`, `unboxing` 중 **하나만 non-null**입니다.

---

### content.gacha (캡슐 뽑기인 경우)

```json
{
  "gacha": {
    "playCount": 5,
    "remainingDrawCount": 3,
    "selected": false,
    "list": [
      {
        "itemName": "에어팟 프로",
        "imageUrl": "https://example.com/airpods.jpg",
        "percent": 0.1,
        "percentOpen": true
      },
      {
        "itemName": "양말 세트",
        "imageUrl": "https://example.com/socks.jpg",
        "percent": 0.7,
        "percentOpen": true
      },
      {
        "itemName": "스타벅스 기프티콘",
        "imageUrl": "https://example.com/starbucks.jpg",
        "percent": 0.2,
        "percentOpen": false
      }
    ],
    "drawHistory": [
      {
        "capsuleId": 3,
        "giftName": "양말 세트",
        "giftImageUrl": "https://example.com/socks.jpg",
        "description": "따뜻한 양말 세트!",
        "selected": false
      },
      {
        "capsuleId": 7,
        "giftName": "에어팟 프로",
        "giftImageUrl": "https://example.com/airpods.jpg",
        "description": "축하해요!",
        "selected": false
      }
    ]
  },
  "quiz": null,
  "unboxing": null
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `playCount` | int | 총 뽑기 횟수 (이벤트 생성 시 설정) |
| `remainingDrawCount` | int | 남은 뽑기 횟수 |
| `selected` | boolean | 선택 완료 여부 (선택 후에도 뽑기 가능, 선택 변경도 가능) |
| `list` | Array | 남은 캡슐 목록 (이미 뽑힌 캡슐은 제외됨) |
| `list[].itemName` | String | 선물 이름 |
| `list[].imageUrl` | String | 선물 이미지 URL |
| `list[].percent` | double | 당첨 확률 (0.0~1.0, 남은 캡슐 기준 재계산) |
| `list[].percentOpen` | boolean | 확률 공개 여부 (`false`면 "비공개" 표시) |
| `drawHistory` | Array | 뽑기 이력 (재접속 시 히스토리 복원용, 뽑은 순서대로) |
| `drawHistory[].capsuleId` | Long | 캡슐 PK (선택 시 사용) |
| `drawHistory[].giftName` | String | 뽑힌 선물 이름 |
| `drawHistory[].giftImageUrl` | String | 뽑힌 선물 이미지 URL |
| `drawHistory[].description` | String | 선물 설명 |
| `drawHistory[].selected` | boolean | 최종 선택 여부 |

---

### content.quiz (퀴즈인 경우)

```json
{
  "gacha": null,
  "quiz": {
    "currentQuizIndex": 2,
    "remainingAttempts": null,
    "successReward": {
      "requiredCount": 2,
      "itemName": "에어팟 프로",
      "imageUrl": "https://example.com/airpods.jpg"
    },
    "failReward": {
      "requiredCount": null,
      "itemName": "양말 세트",
      "imageUrl": "https://example.com/socks.jpg"
    },
    "list": [
      {
        "quizId": 1,
        "type": "multiple_choice",
        "title": "내가 제일 좋아하는 음식은?",
        "imageUrl": null,
        "description": null,
        "hint": "어제 저녁에도 먹었어요!",
        "options": ["치킨", "마라탕", "초밥", "삼겹살", "파스타"],
        "playLimit": 3
      },
      {
        "quizId": 2,
        "type": "ox",
        "title": "나는 고양이를 키운다",
        "imageUrl": null,
        "description": null,
        "hint": "반려동물이 있긴 해요",
        "options": ["O", "X"],
        "playLimit": 2
      },
      {
        "quizId": 3,
        "type": "text",
        "title": "우리가 처음 만난 장소는?",
        "imageUrl": null,
        "description": null,
        "hint": "강남역 근처의 유명한 카페 브랜드입니다.",
        "options": [],
        "playLimit": 3
      }
    ],
    "answerHistory": [
      { "quizId": 1, "correct": true },
      { "quizId": 2, "correct": false }
    ]
  },
  "unboxing": null
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `currentQuizIndex` | int | 현재 풀어야 할 문제 인덱스 (list 기준, 0부터 시작) |
| `remainingAttempts` | Integer | 현재 문제의 남은 시도 횟수 (`null`이면 아직 해당 문제 시작 안 함, 재접속 시 이어하기용) |
| `successReward` | Object | 성공 보상 선물 |
| `successReward.requiredCount` | Integer | 성공에 필요한 최소 정답 수 |
| `failReward` | Object | 실패 보상 선물 |
| `failReward.requiredCount` | Integer | 항상 `null` |
| `list` | Array | 퀴즈 문항 목록 (sortOrder 순) |
| `list[].quizId` | Long | 퀴즈 PK |
| `list[].type` | String | `"multiple_choice"` / `"ox"` / `"text"` |
| `list[].title` | String | 문제 텍스트 |
| `list[].imageUrl` | String | 문제 이미지 URL (없으면 `null`) |
| `list[].description` | String | 문제 설명 (없으면 `null`) |
| `list[].hint` | String | 힌트 텍스트 |
| `list[].options` | Array\<String\> | 선택지 목록 (`text` 타입은 빈 배열) |
| `list[].playLimit` | int | 문항별 시도 제한 횟수 |
| `answerHistory` | Array | 이미 푼 문제의 정답/오답 기록 (재접속 시 복원용) |
| `answerHistory[].quizId` | Long | 문제 PK |
| `answerHistory[].correct` | boolean | 정답 여부 |

**퀴즈 채점 플로우 (서버에서 처리):**
1. `currentQuizIndex`에 해당하는 문제를 표시
2. `remainingAttempts`가 있으면 해당 횟수부터 이어서 시도, `null`이면 `playLimit`부터 시작
3. 사용자가 답안을 선택/입력하면 `POST /quiz/answer { quizId, selectedAnswer }` 호출
4. 서버가 채점 → 정답이면 `correct: true, remainingAttempts: 0` 반환, 다음 문항으로
5. 오답이면 서버가 `remainingAttempts` 차감 → 0이면 해당 문항 종료(correct: false), 다음 문항으로
6. 모든 문항 완료 → `POST /quiz/result` → 서버가 정답 수 계산하여 성공/실패 보상 반환

> **참고:** 정답은 서버에서만 보유하며 클라이언트에 노출하지 않습니다.

---

### content.unboxing (확정 선물인 경우)

```json
{
  "gacha": null,
  "quiz": null,
  "unboxing": {
    "beforeOpen": {
      "imageUrl": "https://example.com/box.jpg",
      "description": "생일 축하해! 이 상자를 열어봐!"
    },
    "afterOpen": {
      "itemName": "에어팟 프로",
      "imageUrl": "https://example.com/airpods.jpg"
    }
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `beforeOpen.imageUrl` | String | 개봉 전 이미지 |
| `beforeOpen.description` | String | 개봉 전 메시지 |
| `afterOpen.itemName` | String | 선물 이름 |
| `afterOpen.imageUrl` | String | 선물 이미지 URL |

> 별도 API 호출 없이 프론트에서 "선물 받기" 버튼으로 UI 전환만 처리합니다.

---

## 2. 캡슐 뽑기

캡슐을 1회 뽑습니다. 가중치 기반 랜덤 추첨이며, 한 번 뽑힌 캡슐은 풀에서 제거됩니다. 결과는 DB에 저장되며, 프론트에서 로컬 히스토리 배열에 push하여 화면에 표시합니다.

### Request

```http
POST /events/{eventUrl}/capsules/draw
```

| 파라미터 | 위치 | 타입 | 설명 |
|---------|------|------|------|
| `eventUrl` | path | String | 이벤트 고유 URL |

> Body 없음

### Response (200 OK)

```json
{
  "code": "SUCCESS",
  "message": "뽑기 성공",
  "data": {
    "capsuleId": 3,
    "giftName": "에어팟 프로",
    "giftImageUrl": "https://example.com/airpods.jpg",
    "description": "축하해요! 에어팟 프로를 뽑았습니다!"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `capsuleId` | Long | 캡슐 PK (선택 시 사용) |
| `giftName` | String | 뽑힌 선물 이름 |
| `giftImageUrl` | String | 뽑힌 선물 이미지 URL |
| `description` | String | 선물 설명 |

### 에러 케이스

| 상황 | 에러 코드 | HTTP |
|------|----------|------|
| 뽑기 횟수 초과 | `CAPSULE_DRAW_LIMIT_EXCEEDED` | 400 |
| 모든 캡슐 소진 | `CAPSULE_ALL_DRAWN` | 400 |
| 캡슐 이벤트 없음 | `CAPSULE_NOT_FOUND` | 404 |

### 참고

- 프론트는 뽑기 결과를 로컬 배열에 push하여 히스토리를 표시합니다.
- 재접속 시에는 `GET /events/{eventUrl}`의 `drawHistory`에서 복원합니다.
- 뽑기 후 다시 `GET /events/{eventUrl}`을 호출하면 `remainingDrawCount`가 감소하고, 뽑힌 캡슐은 `list`에서 제거됩니다.
- 남은 캡슐의 확률(`percent`)은 자동으로 재계산됩니다.
- **선택(select) 후에도 남은 횟수가 있으면 뽑기를 계속할 수 있습니다.**

---

## 3. 캡슐 선택

뽑힌 캡슐 중 1개를 최종 선물로 선택합니다. 선택 후에도 다른 뽑힌 선물로 언제든 변경할 수 있습니다.

### Request

```http
POST /events/{eventUrl}/capsules/select
```

| 파라미터 | 위치 | 타입 | 설명 |
|---------|------|------|------|
| `eventUrl` | path | String | 이벤트 고유 URL |

**Body:**

```json
{
  "capsuleId": 3
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `capsuleId` | Long | O | 선택할 캡슐 PK (뽑기 응답의 `capsuleId`) |

### Response (200 OK)

```json
{
  "code": "SUCCESS",
  "message": "선물 선택 성공",
  "data": {
    "giftName": "에어팟 프로",
    "giftImageUrl": "https://example.com/airpods.jpg",
    "description": "축하해요! 에어팟 프로를 뽑았습니다!"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `giftName` | String | 선택한 선물 이름 |
| `giftImageUrl` | String | 선택한 선물 이미지 URL |
| `description` | String | 선물 설명 |

### 에러 케이스

| 상황 | 에러 코드 | HTTP |
|------|----------|------|
| 뽑기 이력 없음 | `CAPSULE_DRAW_NOT_FOUND` | 404 |
| 캡슐 이벤트 없음 | `CAPSULE_NOT_FOUND` | 404 |
| capsuleId 누락 | `VALIDATION_ERROR` | 400 |

---

## 4. 퀴즈 답안 제출 (서버 채점)

매 시도마다 호출합니다. 서버가 채점하고 `remainingAttempts`를 관리합니다.
- 정답 → `QuizAnswer` 저장 + 다음 문제로
- 오답 + 횟수 남음 → `remainingAttempts` 차감
- 오답 + 횟수 소진 → `QuizAnswer(correct=false)` 저장 + 다음 문제로

### Request

```http
POST /events/{eventUrl}/quiz/answer
```

| 파라미터 | 위치 | 타입 | 설명 |
|---------|------|------|------|
| `eventUrl` | path | String | 이벤트 고유 URL |

**Body:**

```json
{
  "quizId": 1,
  "selectedAnswer": "치킨"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `quizId` | Long | O | 문제 PK |
| `selectedAnswer` | String | O | 사용자가 선택/입력한 답안 |

### Response (200 OK)

```json
{
  "code": "SUCCESS",
  "message": "답안 제출 성공",
  "data": {
    "quizId": 1,
    "correct": true,
    "remainingAttempts": 0,
    "currentQuizIndex": 1
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `quizId` | Long | 제출한 문제 PK |
| `correct` | boolean | 서버 채점 결과 |
| `remainingAttempts` | int | 남은 시도 횟수 (0이면 해당 문제 종료 — 정답이든 횟수 소진이든) |
| `currentQuizIndex` | int | 다음에 풀어야 할 문제 인덱스 (`remainingAttempts > 0`이면 현재 문제 유지) |

### 에러 케이스

| 상황 | 에러 코드 | HTTP |
|------|----------|------|
| 이미 답변한 문제 | `QUIZ_ALREADY_ANSWERED` | 400 |
| 시도 횟수 소진 | `QUIZ_NO_ATTEMPTS_LEFT` | 400 |
| 퀴즈 이벤트 없음 | `QUIZ_NOT_FOUND` | 404 |
| 문제가 없음 | `QUIZ_QUESTION_NOT_FOUND` | 404 |
| quizId/selectedAnswer 누락 | `VALIDATION_ERROR` | 400 |

### 참고

- 서버가 `selectedAnswer`와 DB의 정답을 대소문자 무시 + trim 비교로 채점합니다.
- `remainingAttempts`는 서버가 관리합니다. 첫 시도 시 `playLimit`으로 초기화됩니다.
- 재접속 시 `GET /events/{eventUrl}`의 `remainingAttempts`로 중간 상태가 복원됩니다.

---

## 5. 퀴즈 최종 결과 저장

모든 문제를 풀고 나서 최종 보상을 판정합니다. 서버가 DB에서 정답 수를 직접 계산합니다.

### Request

```http
POST /events/{eventUrl}/quiz/result
```

| 파라미터 | 위치 | 타입 | 설명 |
|---------|------|------|------|
| `eventUrl` | path | String | 이벤트 고유 URL |

**Body:**

```json
{
  "correctCount": 2
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `correctCount` | int | O | 프론트에서 채점한 정답 수 (서버 검증용 참고값, 실제 판정은 서버 DB 기준) |

### Response (200 OK)

```json
{
  "code": "SUCCESS",
  "message": "퀴즈 결과 저장 성공",
  "data": {
    "correctCount": 2,
    "success": true
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `correctCount` | int | 서버가 계산한 정답 수 |
| `success` | boolean | 성공 여부 (`correctCount >= requiredCount`) |

---

## 6. 진행 데이터 리셋

재접속 시 "처음부터 다시 하기" 선택 시 기존 진행 데이터를 초기화합니다.

### Request

```http
DELETE /events/{eventUrl}/progress
```

| 파라미터 | 위치 | 타입 | 설명 |
|---------|------|------|------|
| `eventUrl` | path | String | 이벤트 고유 URL |

### Response (200 OK)

```json
{
  "code": "SUCCESS",
  "message": "진행 데이터가 초기화되었습니다.",
  "data": null
}
```

### 초기화 대상

| 이벤트 타입 | 초기화 내용 |
|------------|-----------|
| 캡슐 뽑기 | `capsule_draw` 기록 전체 삭제 (선택 포함) → 뽑기 횟수/캡슐 풀 복원 |
| 퀴즈 | `quiz_answer` 전체 삭제 + `totalAttempt`, `lastCorrectCount`, `lastSuccess`, `currentQuizRemainingAttempts` 초기화 |
| 언박싱 | 서버 측 초기화 대상 없음 |

---

---

# BGM API 명세서

> Base URL: `{서버주소}/bgm`

## 공통 에러 코드

| 에러 코드 | HTTP 상태 | 설명 |
|-----------|----------|------|
| `EMPTY_FILE` | 400 | 빈 파일 업로드 시도 |
| `INVALID_AUDIO_FILE_TYPE` | 400 | MP3, WAV, OGG, AAC, M4A 외 파일 형식 |
| `FILE_SIZE_EXCEEDED` | 400 | 파일 크기 20MB 초과 |
| `BGM_UPLOAD_LIMIT_EXCEEDED` | 400 | 업로드된 BGM URL이 3개 초과 |
| `STORAGE_UPLOAD_FAILED` | 500 | S3 업로드 실패 |

---

## 1. 프리셋 BGM 목록 조회

앱에서 제공하는 고정 프리셋 BGM 3개의 목록을 반환합니다.

### Request

```http
GET /bgm/presets
```

### Response (200 OK)

```json
{
  "code": "SUCCESS",
  "message": "프리셋 BGM 조회 성공",
  "data": [
    { "id": "exciting",   "name": "신나는", "url": "https://cdn.example.com/bgm/preset/exciting.mp3" },
    { "id": "calm",       "name": "잔잔한", "url": "https://cdn.example.com/bgm/preset/calm.mp3" },
    { "id": "nostalgic",  "name": "추억",   "url": "https://cdn.example.com/bgm/preset/nostalgic.mp3" }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | String | 프리셋 식별자 |
| `name` | String | 프리셋 표시 이름 |
| `url` | String | CDN URL |

---

## 2. BGM 업로드

사용자 커스텀 BGM 파일을 업로드하고 CDN URL을 반환합니다.
이벤트 생성 전에 호출하며, 반환된 URL을 이벤트 생성 요청의 `bgm` 또는 `uploaded_bgm_urls`에 사용합니다.

### Request

```http
POST /bgm/upload
Content-Type: multipart/form-data
```

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| `file` | form-data | MultipartFile | O | 업로드할 오디오 파일 (MP3, WAV, OGG, AAC, M4A) |

### Response (200 OK)

```json
{
  "code": "SUCCESS",
  "message": "BGM 업로드 성공",
  "data": {
    "bgmUrl": "https://cdn.example.com/bgm/550e8400-e29b-41d4-a716-446655440000.mp3"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `bgmUrl` | String | 업로드된 BGM의 CDN URL |

### 참고

- 최대 파일 크기: **20MB**
- 이벤트 생성 시 선택되지 않은 업로드 BGM은 서버에서 삭제를 시도합니다(실패 시 무시될 수 있음).

---

## BGM 선택 플로우

```text
1. GET /bgm/presets
   → 프리셋 3개(신나는/잔잔한/추억) URL 수신

2. (선택) POST /bgm/upload × N (최대 3회)
   → 커스텀 BGM 업로드 → bgmUrl 수신

3. 이벤트 생성 요청 (POST /events)에 포함:
   - bgm: 최종 선택한 BGM URL (프리셋 또는 업로드된 URL 중 하나)
   - uploaded_bgm_urls: 업로드한 모든 BGM URL 목록 (미선택 파일 삭제용)
```

**이벤트 생성 요청 예시:**

```json
{
  "bgm": "https://cdn.example.com/bgm/aaaa.mp3",
  "uploaded_bgm_urls": [
    "https://cdn.example.com/bgm/aaaa.mp3",
    "https://cdn.example.com/bgm/bbbb.mp3"
  ],
  ...
}
```

> 프리셋을 선택한 경우 `uploaded_bgm_urls`는 생략하거나 빈 배열로 전달합니다.

---

## 전체 플로우 요약

```text
1. GET /events/{eventUrl}
   → 갤러리 + 콘텐츠(gacha/quiz/unboxing) 데이터 수신
   → 캡슐: drawHistory로 이전 뽑기 이력 복원
   → 퀴즈: answerHistory + currentQuizIndex + remainingAttempts로 풀이 이력 복원

2-A. 캡슐 뽑기 플로우
   → POST /events/{eventUrl}/capsules/draw (1회 뽑기)
   → 결과를 프론트 로컬 히스토리에 push, 오른쪽에 "~~를 뽑았습니다" 표시
   → POST /events/{eventUrl}/capsules/select로 선물 선택 (언제든 변경 가능)
   → 선택 후에도 남은 횟수가 있으면 계속 뽑기 가능
   → 뽑기와 선택을 자유롭게 반복 (선택이 뽑기를 차단하지 않음)

2-B. 퀴즈 플로우
   → 매 시도마다 POST /events/{eventUrl}/quiz/answer { quizId, selectedAnswer }
   → 서버가 채점 + remainingAttempts 관리
   → 정답 또는 playLimit 소진 시 자동으로 다음 문항 이동
   → 모든 문항 완료 후 POST /events/{eventUrl}/quiz/result
   → 서버가 정답 수 계산 → 성공/실패 보상 반환

2-C. 언박싱 플로우
   → "선물 받기" 버튼 → afterOpen 데이터로 UI 전환 (서버 호출 없음)

3. 중간 이탈 후 재접속
   → GET /events/{eventUrl}로 진행 이력 복원 → 이어서 진행
   → DELETE /events/{eventUrl}/progress → 처음부터 다시 시작
```
