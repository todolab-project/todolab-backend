# ToDoLab Environment Integration

Last updated: 2026-07-14

이 문서는 모바일 real mode가 백엔드에 붙을 때 사용하는 환경별 URL, CORS origin, 문서 UI 공개 기준을 정리한다.

## 1. 환경별 API URL

| 환경 | API URL | 상태 | 비고 |
| --- | --- | --- | --- |
| local | `http://localhost:8080` | 확정 | 백엔드 로컬 실행 기준 |
| local, Android Emulator | `http://10.0.2.2:8080` | 확정 | Android Emulator에서 호스트 머신 접근 시 사용 |
| local, 실제 기기 | `http://<dev-machine-lan-ip>:8080` | 확인 필요 | 같은 네트워크에서 개발 머신 IP로 접근 |
| staging | 미정 | 확인 필요 | 배포 URL 확정 후 갱신 |
| production | 미정 | 확인 필요 | 운영 도메인 확정 후 갱신 |

모바일 기본 base path는 모든 환경에서 `/api/v1`이다.

## 2. CORS Origin

현재 코드 기준 CORS 설정은 `app.cors.allowed-origins` 값을 사용한다.

| 환경 | Origin | 상태 | 설정 위치 |
| --- | --- | --- | --- |
| local, Expo Web | `http://localhost:8081` | 확인됨 | `application.yml`, `application-local.yml` |
| local, Expo Web 대체 포트 | `http://localhost:8090` | 확인됨 | `application.yml`, `application-local.yml` |
| staging | 미정 | 확인 필요 | `TODOLAB_ALLOWED_ORIGINS` |
| production | 미정 | 확인 필요 | `TODOLAB_ALLOWED_ORIGINS` |

허용 request header:

- `Content-Type`
- `Accept`
- `Authorization`

허용 method:

- `GET`
- `POST`
- `PUT`
- `PATCH`
- `DELETE`
- `OPTIONS`

## 3. 환경변수 운영 방식

운영 프로필은 아래 환경변수로 CORS origin을 주입한다.

```bash
TODOLAB_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
```

원칙:

- staging과 production origin은 코드에 하드코딩하지 않는다.
- 여러 origin은 쉼표로 구분한다.
- secret 값은 문서에 기록하지 않는다.
- origin을 추가한 뒤 `Authorization` header가 포함된 preflight를 확인한다.

## 4. 문서 UI 공개 기준

| 환경 | Swagger UI `/swagger-ui` | Scalar `/scalar.html` | 비고 |
| --- | --- | --- | --- |
| local | 공개 | 공개 | 개발 확인용 |
| staging | 제한 공개 권장 | 제한 공개 권장 | 접근 제어 또는 네트워크 제한 필요 |
| production | 비공개 또는 제한 공개 권장 | 비공개 또는 제한 공개 권장 | 운영 공개 범위 확정 필요 |

현재 백엔드는 문서 UI 경로를 인증 없이 열어 둔다. staging/prod 배포 전 공개 범위를 별도로 결정해야 한다.
