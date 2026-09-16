# CONTRIBUTING

브랜치 전략, 변경 단위, 커밋 컨벤션, PR 작성, 코드 포맷 규칙이다. 코드 설계 규칙은 `ai_docs/CLEAN_CODE.md`에 있다.

---

## 브랜치 전략

[git flow](https://nvie.com/posts/a-successful-git-branching-model/)를 따른다. git flow의 `master` 역할은 `main`이 맡는다.

| 브랜치 | 역할 | 분기 원본 | 머지 대상 |
|---|---|---|---|
| `main` | Step이 완료된 상태만 들어온다 | — | — |
| `develop` | 다음 Step을 개발하는 통합 브랜치 | `main` | — |
| `feature/*` | 작업 단위 개발 | `develop` | `develop` |
| `release/*` | Step 출시 준비 | `develop` | `main` + `develop` |
| `hotfix/*` | `main`에서 발견한 버그 수정 | `main` | `main` + `develop` |

### 이름 규칙

| 브랜치 | 형식 | 예 |
|---|---|---|
| feature | `feature/step-{N}-{주제}` | `feature/step-1-double-hold-reproduction` |
| Step에 속하지 않는 작업 | `feature/{주제}` | `feature/project-setup` |
| release | `release/step-{N}` | `release/step-1` |
| hotfix | `hotfix/{요약}` | `hotfix/expired-hold-payment` |

### 흐름

```
feature/step-N-* ─PR─▶ develop ─▶ release/step-N ─PR─▶ main (+ 태그 step-N-complete) ─▶ develop 역머지
```

- **PR은 작업 단위로 잘게, 출시는 Step 단위로.** Step 하나가 끝나는 그날 `release/step-N`을 낸다. 모아서 내지 않는다
- `main`에 머지하면 `step-N-complete` annotated 태그를 단다
- **`release/*`에서는 새 커밋을 만들지 않는다.** 문서 갱신은 `develop`에서 먼저 한다
- **역머지는 반드시 한다.** 끝나고 확인한다

  ```bash
  git rev-list --count develop..main   # 0 이어야 한다
  ```

### 규칙

- **`main`에 직접 커밋하지 않는다.** 저장소 보호 규칙으로 강제된다 — PR 필수, CI 통과 필수, force push·삭제 금지
- `main`과 `develop`은 항상 CI가 통과하는 상태를 유지한다
- **아직 못 고친 문제를 재현하는 실패 테스트**에는 `@Tag("reproduction")`을 붙인다.
  기본 `test`에서 빠지므로 CI를 깨지 않는다. 고친 Step에서 태그를 뗀다

  ```bash
  ./gradlew reproductionTest   # 재현 테스트만 실행 (실패가 정상)
  ```

---

## 변경 단위

**조금씩 바꾼다.** 한 번에 크게 바꾸면 리뷰할 수 없고, 깨졌을 때 어느 변경 때문인지 찾을 수 없다.

### 커밋

- **한 커밋에 파일 하나 정도만 담는다.** 엔티티 하나, 서비스 하나, 테스트 하나가 각각 커밋 하나다
  - 예외: 따로 떼면 빌드가 깨지는 **최소 묶음** (새 메서드와 그걸 처음 호출하는 곳 등)
  - 예외: 이름 일괄 변경·포맷 같은 **기계적 변경**은 파일 수와 관계없이 커밋 하나로 묶는다
- **한 커밋에 한 가지 변경.** 동작 변경과 리팩터링·이름 변경·포맷 변경을 한 커밋에 섞지 않는다
- **모든 커밋은 빌드가 통과하는 상태**다. 재현 테스트는 `reproduction` 태그로 예외 처리한다
- 순서는 **테스트 → 구현 → 정리**로 쪼갠다

  ```
  test: 같은 좌석 동시 선점을 재현한다
  fix: 좌석 선점에 유니크 제약을 건다
  refactor: 선점 거절 예외를 도메인 예외로 바꾼다
  ```

- 구현 중에 고칠 거리를 발견하면 지금 커밋에 끼우지 않는다. 메모해 두고 **별도 커밋**으로 한다

### PR

- **PR 하나의 변경은 300줄 안팎**을 목표로 한다 (도구가 일괄 처리한 변경 제외). 넘으면 쪼갤 수 있는지 먼저 본다
- **Step 하나를 PR 여러 개로 나눈다.** 예: 재현 테스트 PR → 보호 장치 PR → 문서 PR
- 기능과 무관한 리팩터링은 **선행 PR로 먼저** 머지한다
- 대량 기계적 변경(포맷, 패키지 이동, 이름 일괄 변경)은 **그것만 담은 PR**로 낸다

### AI 에이전트 작업 시

- 파일 여러 개를 한 번에 크게 고치지 않는다. **파일 하나를 고치고 → 빌드·테스트를 돌리고 → 커밋**한다
- **커밋 하나를 만들면 멈춘다.** 무엇을 바꿨는지 보고하고, 사용자가 피드백을 주거나 진행하라고 할 때까지 다음 커밋을 시작하지 않는다
- 빌드 확인은 **그 커밋에 들어가는 파일만 있는 상태**로 한다. 아직 커밋하지 않은 파일 덕분에 통과하면 안 된다
- 요청 범위를 넘는 변경은 먼저 묻는다

---

## 커밋 컨벤션

[Conventional Commits](https://www.conventionalcommits.org/) 형식을 따른다.

```
<type>: <제목>

<본문 (선택)>
```

### type

| type | 용도 |
|---|---|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `test` | 테스트 추가·수정 (재현 테스트 포함) |
| `refactor` | 동작 변경 없는 구조 개선 |
| `perf` | 성능 개선 |
| `style` | 포맷 등 의미 없는 변경 |
| `docs` | 문서만 변경 |
| `build` | Gradle 등 빌드 설정 |
| `ci` | CI 설정 |
| `chore` | 그 밖의 잡무 (`.gitignore` 등) |

### 규칙

- **제목은 한 줄, 50자 안팎.** 한글, 현재형(`~한다`), 마침표 없음. 무엇을 했는지만 적는다
- **한 커밋에 한 관심사.** type이 둘 섞이면 커밋을 나눈다
- **본문은 불릿, 한 줄에 한 가지.** "왜"는 코드만 봐서 알 수 없을 때만 한두 줄
- 볼드·이모지를 쓰지 않는다
- **제목에 서사를 담지 않는다.** 발견 경위나 사연은 `ai_docs/PROGRESS.md`에 적는다

```
✅ test: 같은 좌석 동시 선점을 재현한다
❌ test: 동시에 때려보니 좌석 하나에 예약이 3개 생겼다

✅ fix: 만료된 선점으로 결제하면 거절한다
❌ fix: 만료 체크를 빼먹어서 결제가 통과하고 있었다
```

### 예시

```
feat: 좌석 선점과 결제 확정을 구현한다

- POST /reservations 좌석 선점
- POST /reservations/{id}/payment 결제 후 확정·발권
- 락 없음 (Step 1에서 재현할 대상)
```

---

## PR 작성

리뷰어가 훑어서 필요한 것만 집어가는 문서다. 템플릿은 `.github/pull_request_template.md`.

```markdown
## 개요
- 관련 Step, 목적. 한두 줄

## 작업 내용
- 무엇을 했는지 명사형 단문

## 리뷰 참고 사항
- 검증 방법과 결과
- 재현 테스트 여부
```

- **명사형 단문 불릿.** `~추가`, `~분리`, `~제거`
- 볼드는 라벨에만, 이모지는 쓰지 않는다
- 코드·diff는 판단이 갈리는 부분만 보여준다
- 도구가 일괄 처리한 변경(`spotlessApply` 등)과 직접 고친 것을 나눠 적는다

---

## 코드 포맷

**Spotless + google-java-format**으로 강제한다. 들여쓰기 2칸, 한 줄 100자.

```bash
./gradlew spotlessApply   # 고친다
./gradlew spotlessCheck   # 검사만 한다 (build에 포함, CI의 format job)
```

- 커밋 전에 `spotlessApply`를 돌린다
- IDE 설정은 `.editorconfig`가 맞춘다. 어긋나면 `spotlessApply` 결과가 기준이다
- 포맷 변경만 대량으로 생기면 `style:` 커밋으로 분리한다

## 주석

**주석에 HTML 태그를 쓰지 않는다** (`<p>`, `<b>`, `<ul>` 등). javadoc HTML을 생성하지 않으므로 소스 그대로 읽힌다.
빈 줄과 `-` 목록으로 구분한다. `{@link}`, `{@code}`는 IDE에서 동작하므로 쓴다.

---

## CI

`.github/workflows/build.yml` — `main`·`develop`으로의 push와 PR에서 돈다.

| job | 하는 일 |
|---|---|
| `lint-workflows` | 워크플로 파일 검사 (actionlint) |
| `format` | `spotlessCheck` |
| `build` | 컴파일 + 테스트 (Testcontainers MySQL) |
