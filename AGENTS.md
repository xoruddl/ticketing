# AGENTS.md

AI 에이전트용 안내 문서. **작업 전에 먼저 읽는다.**

## 프로젝트

좌석 예매 시스템을 **모듈러 모놀리식**으로 만드는 **학습용 프로젝트**. 목표는 두 가지다.

1. **동시성** — 좌석 경합, 이중 예약, 인기 공연의 처리량 상한
2. **모듈 경계 강제** — 배포는 하나, 경계는 테스트가 지킨다 (Spring Modulith)

## 모듈 경계 규칙

지금은 단일 패키지다. Step 4에서 패키지로 모듈을 나누고, Step 5에서 아래 규칙을 Spring Modulith 테스트로 강제한다.

```
app  →  reservation · payment · ticket  →  user  →  shared-kernel
조립      업무 모듈 (서로 안 본다)          지원      식별자·값 객체만
```

| # | 규칙 | 강제 수단 |
|---|---|---|
| ① | 업무 모듈끼리 의존 금지 | Modulith 테스트 |
| ② | **`user` → 업무 모듈 의존 금지** (가장 중요. `user.canReserve()` 같은 게 생기면 순환이 생긴다) | Modulith 테스트 |
| ③ | `shared-kernel`은 아무것도 의존하지 않는다 | Modulith 테스트 |
| ④ | 모듈의 하위 패키지는 밖에서 못 본다 (공개 API는 모듈 최상위 패키지에만) | Modulith 테스트 |
| ⑤ | 모듈은 자기 테이블만 읽는다 (다른 모듈 테이블 JOIN·FK 금지) | **Modulith가 못 잡는다.** Step 5에서 정한다 |

- **모듈은 `com.ticketing` 바로 아래 패키지다.** `app`의 조립 역할은 루트 패키지(`TicketingApplication`)가 맡는다. `shared-kernel`은 패키지명으로 `sharedkernel`을 쓴다.
- **인증**은 `app`의 필터, **`UserId`**는 `shared-kernel`의 값 객체, **프로필**은 `user` 모듈(조회만).
  도메인 모듈은 `UserId` 값만 받고 인증을 모른다.
- 경계는 **Spring Modulith**(`ApplicationModules.verify()`)로 강제한다. **위반은 컴파일이 아니라 테스트에서 잡힌다** — Gradle 멀티모듈 대신 이걸 고른 대가다.
- Modulith의 **이벤트 기능**(`@ApplicationModuleListener`, Event Publication Registry)은 미리 쓰지 않는다. Step 6·8에서 대안으로 비교할 때 처음 쓴다.

## 작업 방식

1. `ai_docs/PROBLEMS.md`의 **순서대로** 진행하고, 완료하면 체크박스를 갱신한다.
2. **재현 먼저.** 해결책을 넣기 전에 실패하는 테스트로 문제를 드러낸다.
3. **Step마다 멈춘다.** 재현 → 고침 → 설명 → 커밋. 한 번에 한 Step만 한다.
   Step 안에서도 **조금씩 바꾼다** — 작은 단위로 고치고, 테스트를 돌리고, 커밋한다 (`ai_docs/CONTRIBUTING.md`의 "변경 단위").
4. 검증은 **테스트 코드로** 한다.
5. 새 테스트는 **코드를 되돌려서 실패하는지** 확인한다.
6. Step이 끝나면 `ai_docs/PROGRESS.md`에 목표·한 일과 이유·만난 문제·커밋 해시를 남긴다.
7. 브랜치·커밋·PR은 `ai_docs/CONTRIBUTING.md`를 따른다 (git flow, `feature/step-{N}-{주제}` → `develop`). `main`에 직접 커밋하지 않는다.
8. 코드는 `ai_docs/CLEAN_CODE.md`를 따르고, 커밋 전에 `./gradlew spotlessApply`를 돌린다.
9. 아직 못 고친 문제의 재현 테스트에는 `@Tag("reproduction")`을 붙인다 (기본 `test`와 CI에서 제외).
10. 요청받지 않은 푸시는 하지 않는다.

## 실행 · 테스트

```bash
./gradlew build              # 포맷 검사 + 테스트. Testcontainers가 MySQL을 띄운다 (Docker 필요)
./gradlew spotlessApply      # 포맷 자동 수정
./gradlew reproductionTest   # 재현 테스트만 (실패가 정상)
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
| `ai_docs/ARCHITECTURE.md` | C4 세 층으로 본 시스템 구조와 앞으로 생길 것 |
| `ai_docs/FEATURES.md` | 만들 기능의 범위 |
| `ai_docs/CONTRIBUTING.md` | 브랜치 전략, 커밋 컨벤션, PR, 포맷, CI |
| `ai_docs/CLEAN_CODE.md` | 클린코드 체크리스트, SOLID |
| `ai_docs/PROBLEMS.md` | 풀어야 할 문제 (Step 0~13) |
| `ai_docs/PROGRESS.md` | 실제로 한 일과 이유 |
| `ai_docs/DECISIONS.md` | 기술 결정 기록 |

기술 결정 양식 (**포기한 것은 비워두지 않는다**):

```text
문제 / 대안 / 선택 이유 / 포기한 것 / 검증(확인한 범위와 못 한 범위)
```

## 언어

문서·커밋 메시지·테스트 메서드명은 한글, 코드 식별자는 영문.
