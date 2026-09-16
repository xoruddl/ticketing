# AGENTS.md

AI 에이전트용 안내 문서. **작업 전에 먼저 읽는다.**

## 프로젝트

좌석 예매 시스템을 **모듈러 모놀리식**으로 만드는 **학습용 프로젝트**. 목표는 두 가지다.

1. **동시성** — 좌석 경합, 이중 예약, 인기 공연의 처리량 상한
2. **모듈 경계 강제** — 배포는 하나, 경계는 컴파일러와 테스트가 지킨다

## 모듈 경계 규칙

지금은 단일 모듈이다. Step 4에서 나누고, Step 5에서 아래 규칙을 ArchUnit으로 옮긴다.

```
app  →  reservation · payment · ticket  →  user  →  shared-kernel
조립      업무 모듈 (서로 안 본다)          지원      식별자·값 객체만
```

| # | 규칙 |
|---|---|
| ① | 업무 모듈끼리 의존 금지 |
| ② | **`user` → 업무 모듈 의존 금지** (가장 중요. `user.canReserve()` 같은 게 생기면 순환이 생긴다) |
| ③ | `shared-kernel`은 아무것도 의존하지 않는다 |
| ④ | 모듈의 `internal` 패키지는 밖에서 못 본다 |
| ⑤ | 모듈은 자기 테이블만 읽는다 (다른 모듈 테이블 JOIN·FK 금지) |

- **인증**은 `app`의 필터, **`UserId`**는 `shared-kernel`의 값 객체, **프로필**은 `user` 모듈(조회만).
  도메인 모듈은 `UserId` 값만 받고 인증을 모른다.
- 경계는 먼저 **Gradle 멀티모듈 + ArchUnit**으로 직접 만들고, 다 배운 뒤 **Spring Modulith**로 바꾼다. 바꾸는 단계를 빼먹지 않는다.

## 작업 방식

1. `ai_docs/PROBLEMS.md`의 **순서대로** 진행하고, 완료하면 체크박스를 갱신한다.
2. **재현 먼저.** 해결책을 넣기 전에 실패하는 테스트로 문제를 드러낸다.
3. **Step마다 멈춘다.** 재현 → 고침 → 설명 → 커밋. 한 번에 한 Step만 한다.
4. 검증은 **테스트 코드로** 한다.
5. 새 테스트는 **코드를 되돌려서 실패하는지** 확인한다.
6. Step이 끝나면 `ai_docs/PROGRESS.md`에 목표·한 일과 이유·만난 문제·커밋 해시를 남긴다.
7. `main`에 직접 커밋하지 않는다. Step마다 브랜치를 딴다.
8. 요청받지 않은 푸시는 하지 않는다.

## 실행 · 테스트

```bash
./gradlew test    # Testcontainers가 MySQL을 띄운다 (Docker 필요)
# 앱 실행: src/test/java/com/ticketing/TestTicketingApplication의 main()
```

- **H2 금지.** 행 락·`FOR UPDATE`·격리 수준이 MySQL과 다르게 동작한다.
- MySQL 이미지는 `mysql:8.0`으로 고정한다.

## 환경

- **Spring Boot 4.1 / Java 21 toolchain / Gradle 9.7.1.** 설치된 JDK는 26이지만 `languageVersion`은 올리지 않는다.
- Boot 3.x와 스타터 이름이 다르다: `starter-web` → `starter-webmvc`, `starter-test` → `starter-{webmvc,data-jpa,validation}-test`.
  새 의존성은 기억에 의존하지 말고 `./gradlew dependencies --configuration testCompileClasspath`로 확인한다.

## 문서

| 문서 | 역할 |
|---|---|
| `ai_docs/FEATURES.md` | 만들 기능의 범위 |
| `ai_docs/PROBLEMS.md` | 풀어야 할 문제 (Step 0~13) |
| `ai_docs/PROGRESS.md` | 실제로 한 일과 이유 |
| `ai_docs/DECISIONS.md` | 기술 결정 기록 |

기술 결정 양식 (**포기한 것은 비워두지 않는다**):

```text
문제 / 대안 / 선택 이유 / 포기한 것 / 검증(확인한 범위와 못 한 범위)
```

## 언어

문서·커밋 메시지·테스트 메서드명은 한글, 코드 식별자는 영문.
