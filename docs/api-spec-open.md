# 선물 개봉(이벤트 오픈) API 명세서

> Base URL: `{서버주소}/events`

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
| `QUIZ_NOT_FOUND` | 404 | 퀴즈 이벤트가 없음 |
| `INVALID_ARGUMENT` | 400 | 잘못된 요청 (예: correctCount가 음수) |

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
| `list` | Array | 남은 캡슐 목록 (이미 뽑힌 캡슐은 제외됨) |
| `list[].itemName` | String | 선물 이름 |
| `list[].imageUrl` | String | 선물 이미지 URL |
| `list[].percent` | double | 당첨 확률 (0.0~1.0, 남은 캡슐 기준 재계산) |
| `list[].percentOpen` | boolean | 확률 공개 여부 (`false`면 "비공개" 표시) |

---

### content.quiz (퀴즈인 경우)

```json
{
  "gacha": null,
  "quiz": {
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
        "answer": ["치킨"],
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
        "answer": ["O"],
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
        "answer": ["스타벅스", "스벅"],
        "playLimit": 3
      }
    ]
  },
  "unboxing": null
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
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
| `list[].answer` | Array\<String\> | 정답 목록 (주관식은 허용 답안 여러 개 가능) |
| `list[].playLimit` | int | 문항별 시도 제한 횟수 |

**퀴즈 채점 플로우 (프론트에서 처리):**
1. 문항을 1개씩 표시, `playLimit`만큼 재시도 가능
2. `answer` 배열과 대소문자 무시 비교로 정답 판별
3. 정답 → `correctCount++`, 다음 문항으로
4. 오답 → 남은 기회 차감, 0이면 다음 문항으로 (오답 처리)
5. 모든 문항 완료 → `correctCount >= successReward.requiredCount`이면 성공 보상, 아니면 실패 보상

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

캡슐을 1회 뽑습니다. 가중치 기반 랜덤 추첨이며, 한 번 뽑힌 캡슐은 풀에서 제거됩니다.

### Request

```http
POST /events/{eventUrl}/capsule/draw
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
    "giftName": "에어팟 프로",
    "giftImageUrl": "https://example.com/airpods.jpg",
    "description": "축하해요! 에어팟 프로를 뽑았습니다!"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
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

- 뽑기 후 다시 `GET /events/{eventUrl}`을 호출하면 `remainingDrawCount`가 감소하고, 뽑힌 캡슐은 `list`에서 제거됩니다.
- 남은 캡슐의 확률(`percent`)은 자동으로 재계산됩니다.

---

## 3. 퀴즈 결과 저장

프론트에서 채점 완료 후 최종 정답 수를 서버에 저장합니다.

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
| `correctCount` | int | O | 프론트에서 채점한 최종 정답 수 |

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
| `correctCount` | int | 저장된 정답 수 |
| `success` | boolean | 성공 여부 (`correctCount >= requiredCount`) |

---

## 4. 진행 데이터 리셋

중간에 이탈했다가 재접속 시 "다시 시작하겠습니까?" → 기존 진행 데이터를 초기화합니다.

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
| 캡슐 뽑기 | `capsule_draw` 기록 전체 삭제 → 뽑기 횟수/캡슐 풀 복원 |
| 퀴즈 | `totalAttempt` 0으로 초기화 |
| 언박싱 | 서버 측 초기화 대상 없음 |

---

## 전체 플로우 요약

```text
1. GET /events/{eventUrl}
   → 갤러리 + 콘텐츠(gacha/quiz/unboxing) 데이터 수신

2-A. 캡슐 뽑기 플로우
   → POST /events/{eventUrl}/capsule/draw (뽑기 1회)
   → 결과 화면 표시
   → 남은 횟수 있으면 반복
   → 횟수 소진 시 종료

2-B. 퀴즈 플로우
   → 프론트에서 문항별 채점 (answer 비교, playLimit 재시도)
   → 모든 문항 완료 후 POST /events/{eventUrl}/quiz/result
   → correctCount >= requiredCount → 성공 보상 / 아니면 실패 보상

2-C. 언박싱 플로우
   → "선물 받기" 버튼 → afterOpen 데이터로 UI 전환 (서버 호출 없음)

3. 중간 이탈 후 재접속
   → DELETE /events/{eventUrl}/progress → 처음부터 다시 시작
```
