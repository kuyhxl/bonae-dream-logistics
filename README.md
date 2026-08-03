# 보내드림 물류 (bonae-dream-logistics)

MSA 기반 국내 물류 관리 및 배송 시스템

---

## 필수 준비물

| 항목 | 버전 | 확인 |
|---|---|---|
| Java | **17** | `java -version` |
| Docker Desktop | 최신 | `docker --version |
| Git | - | |

---

## 1. 클론 및 환경변수 설정

```bash
git clone https://github.com/kuyhxl/bonae-dream-logistics.git
cd bonae-dream-logistics

# 환경변수 파일 생성 (필수)
cp .env.template .env
```

> `.env`는 `.gitignore`에 등록되어 커밋되지 않습니다. **반드시 직접 생성**해주세요.

## 2. 인프라 기동

```bash
docker compose up -d
docker compose ps        # 3개 컨테이너가 Up 상태인지 확인
```

| 컨테이너 | 호스트 포트 | 용도 |
|---|---|---|
| bonae-postgres | **15432** | PostgreSQL 16 (스키마 6개) |
| bonae-redis | 6379 | 캐싱 |
| bonae-zipkin | 9411 | 분산 추적 |

## 3. 빌드

```bash
./gradlew clean build -x test
```

## 4. 실행

**Eureka를 가장 먼저 띄워주세요.** 이후 순서는 상관없습니다.

```bash
# 터미널 1 — 반드시 먼저
./gradlew :eureka-server:bootRun

# 터미널 2 — 본인 담당 서비스
./gradlew :user-service:bootRun

# 터미널 3 — 게이트웨이 경유 확인이 필요할 때
./gradlew :gateway:bootRun
```

IntelliJ에서는 각 `*Application` 클래스의 실행 버튼(▶)을 눌러서 실행시키는게 터미널 여러 개보다 편합니다.

## 5. 동작 확인

```bash
curl localhost:19001/api/users/ping     # 서비스 직접 호출
curl localhost:8080/api/users/ping      # 게이트웨이 경유
```

| 주소 | 용도 |
|---|---|
| http://localhost:8761 | Eureka 대시보드 (등록된 서비스 확인) |
| http://localhost:9411 | Zipkin UI (분산 추적) |

---

## 포트 목록

| 앱 | 포트 | 스키마 |
|---|---|---|
| Gateway | 8080 | - |
| Eureka | 8761 | - |
| user-service | 19001 | user_service |
| message-service | 19002 | message_service |
| hub-service | 19003 | hub_service |
| company-service | 19004 | company_service |
| order-service | 19005 | order_service |
| delivery-service | 19006 | delivery_service |

---

## 개발 규칙 요약

자세한 내용은 팀 노션의 [팀 컨벤션] 문서를 참고해주세요.

### 브랜치
```
main  <- dev  <- feature/{이슈번호}-{내용}
```
- `main`, `dev` 직접 push 금지
- PR 필수, **승인 1명** 후 머지
- feature → dev는 **Squash merge**

### 커밋
```
type(scope): subject

예) feat(hub): 허브 생성 API 구현
```
타입: `feat` `fix` `docs` `style` `refactor` `test` `chore`

### 엔티티 작성 시
```java
@Entity
@Table(name = "p_hubs")          // p_ 접두사 명시 필수
public class Hub extends BaseEntity {   // BaseEntity 상속 필수
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    // Audit 6필드는 BaseEntity가 제공하므로 직접 선언하지 않습니다
}
```
- 삭제는 물리 삭제 대신 `entity.delete(userId)` (논리 삭제)
- 조회 시 `deletedAt IS NULL` 조건 적용
- **타 서비스 테이블은 FK로 참조하지 않고 UUID 값만 저장**합니다

### API 규격
- 응답 래퍼 미사용 — 성공 시 데이터를 그대로, 실패 시 `{code, message, traceId}`
- 페이지 번호는 **1부터**
- `size`는 10/30/50만 허용, `sort`는 `createdAt`/`updatedAt`만 허용
- 서비스 간 내부 API는 `/api/internal/**` (게이트웨이에서 외부 차단)

### 공통 파일 변경 시
`common`, `gateway` 설정, `build.gradle`, `docker-compose.yml` 등을 수정할 때는
**사전 공지 -> 단독 PR -> 우선 리뷰 -> 머지 즉시 공지** 순으로 진행해주세요.