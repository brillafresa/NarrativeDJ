# AI 내러티브 DJ 서비스 — 사전 컨설팅 및 아키텍처 설계 보고서

> **Source of Truth** for the NarrativeDJ project.  
> Derived from Gemini research (user-provided, 2026-07-20).  
> Do not reference external Downloads paths.

---

## 1. 시장 환경 및 경쟁 분석

### 1.1 기존 플랫폼 AI 서비스의 기술적·경험적 한계 분석

디지털 오디오 스트리밍 시장을 지배하는 빅테크 기업들의 인공지능 기반 서비스는 극단적인 개인화와 데이터 효율성을 추구하는 과정에서 사용자 경험의 단절과 감성적 소외라는 중대한 역설을 낳고 있다. 선도적 사례인 스포티파이의 'AI DJ (DJ X)' 서비스는 전문 라디오 호스트의 목소리를 인공지능으로 복제하여 고도화된 음성 합성 기술(TTS)을 선보였으나, 실제 사용자 환경에서는 심각한 기능적 결함과 피로감을 노출하고 있다.

가장 지배적인 한계는 **개인화 알고리즘의 데이터 루프 고착화 현상**이다. 스포티파이의 AI DJ는 유저의 방대한 보관함이나 새로운 음원 탐색 영역을 활용하지 못하고, 사용자가 최근 반복 청취한 약 25곡에서 50곡 내외의 협소한 범주 내에서만 음원을 순환 재생하는 심각한 편향성을 드러낸다.

**음향적·감성적 완결성의 훼손** 역시 치명적인 한계이다. 기계적인 오디오 크로스페이드 트랜지션은 곡의 고유한 전주(Intro)나 아웃트로(Outro)를 임의로 40초 이상 강제 삭제하는 현상을 유발한다.

인디 오디오 스트리밍 서비스인 **Trending.fm**은 무드 칩(Balanced, My Library, Throwback, Discover, Energy) 전환과 음성 명령 기반 스토리 아크를 제공하나, B2B 공간 확장에는 기술적 한계가 있다.

### 1.2 대형 플랫폼들의 비즈니스 모델적 장벽

- B2C 개인 요금제와 B2B 상업 라이선스의 엄격한 단절
- 중앙 집중식 데이터 처리 아키텍처의 한계와 '데이터의 역설'

| 비교 항목 | Spotify AI DJ | Trending.fm | 일반 B2B 스트리밍 | 제안 서비스 |
|-----------|---------------|-------------|-------------------|-------------|
| 선곡 제어 주권 | 일방향적 셔플 | 무드 칩·보이스 컨트롤 | 고정 채널 송출 | 자연어 공감 선곡·쿠션 트랜지션 |
| 음향 트랜지션 | 기계적 페이드아웃 | 일반 크로스페이드 | 물리적 크로스페이드 | Web Audio API 덕킹 |
| 공간 컨텍스트 | 미지원 | 개인 디바이스 전용 | 장르 필터링 | 공간 프로필·무인 자동 모드 |
| 저작권 구조 | B2C 전용 | 개인 전용 | B2B 고가 구독 | BYOK 클라이언트 프록시 |

---

## 2. 핵심 기능 상세 정의 및 알고리즘 메커니즘

### 2.1 쿠션(Cushion) 선곡 및 트랜지션 알고리즘

음원 라이브러리의 모든 곡 $S$는 고차원 특징 벡터 $\mathbf{v}$로 표현된다:

$$
\mathbf{v} = \begin{bmatrix} B \\ E \\ V \\ D \\ A \\ \mathbf{g} \end{bmatrix}
$$

- $B$: BPM, $E$: Energy, $V$: Valence, $D$: Danceability, $A$: Acousticness
- $\mathbf{g}$: 가사·텍스트 의미론 임베딩

종합 음향적 거리:

$$
D(S_i, S_j) = \sqrt{w_B (B_i - B_j)^2 + w_E (E_i - E_j)^2 + w_V (V_i - V_j)^2} + w_g \left(1 - \frac{\mathbf{g}_i \cdot \mathbf{g}_j}{\|\mathbf{g}_i\| \|\mathbf{g}_j\|}\right)
$$

가중치: $w_B=0.25$, $w_E=0.25$, $w_V=0.20$, $w_g=0.30$ (공간 프로필에 따라 유동 재계산 가능)

**실행 논리:**

1. $D(S_{current}, S_{target}) \leq \theta$ → 다이렉트 삽입 (`[]`)
2. $\theta$ 초과 시 1~2곡 Bridge Song 탐색
3. 경로 없음 → Drop (`None`)

**캐논 예시:** 몽중인 → California Dreamin' → Hotel California → Sweet Child O' Mine

#### Python 참조 구현 (하네스·앱 구현 기준)

> **주의:** 서술 수식은 유클리드+코사인이나, 아래 Python 코드는 L1 절대차+BPM/200 정규화이며 D/A 미포함. 구현은 이 코드를 따른다.

```python
import numpy as np

class CushionMusicScheduler:
    def __init__(self, track_vector_db, max_bridges=2, alpha_threshold=0.55):
        self.db = track_vector_db
        self.max_bridges = max_bridges
        self.threshold = alpha_threshold

    def get_distance(self, v1, v2):
        bpm_dist = np.abs(v1[0] - v2[0]) / 200.0
        energy_dist = np.abs(v1[1] - v2[1])
        valence_dist = np.abs(v1[2] - v2[2])
        emb1, emb2 = v1[3:], v2[3:]
        cos_sim = np.dot(emb1, emb2) / (np.linalg.norm(emb1) * np.linalg.norm(emb2))
        cosine_dist = 1.0 - cos_sim
        return (0.25 * bpm_dist) + (0.25 * energy_dist) + (0.20 * valence_dist) + (0.30 * cosine_dist)

    def calculate_cushion_route(self, current_id, target_id):
        v_curr = self.db[current_id]
        v_target = self.db[target_id]
        direct_dist = self.get_distance(v_curr, v_target)
        if direct_dist <= self.threshold:
            return []

        best_single_bridge = None
        min_deviation = float('inf')
        for s_id, v_cand in self.db.items():
            if s_id in [current_id, target_id]:
                continue
            d1 = self.get_distance(v_curr, v_cand)
            d2 = self.get_distance(v_cand, v_target)
            if d1 < self.threshold and d2 < self.threshold:
                if (d1 + d2) < min_deviation:
                    min_deviation = d1 + d2
                    best_single_bridge = [s_id]
        if best_single_bridge:
            return best_single_bridge

        best_double_bridge = None
        min_double_deviation = float('inf')
        for s1_id, v_cand1 in self.db.items():
            if s1_id in [current_id, target_id]:
                continue
            d1 = self.get_distance(v_curr, v_cand1)
            if d1 >= self.threshold:
                continue
            for s2_id, v_cand2 in self.db.items():
                if s2_id in [current_id, target_id, s1_id]:
                    continue
                d2 = self.get_distance(v_cand1, v_cand2)
                d3 = self.get_distance(v_cand2, v_target)
                if d2 < self.threshold and d3 < self.threshold:
                    if (d1 + d2 + d3) < min_double_deviation:
                        min_double_deviation = d1 + d2 + d3
                        best_double_bridge = [s1_id, s2_id]
        if best_double_bridge:
            return best_double_bridge
        return None
```

### 2.2 상황 인지 오프닝 및 공간 프로필 모드

| 공간 프로필 템플릿 | 주류 장르 | BPM | Energy | 감성 방향 |
|-------------------|----------|-----|--------|----------|
| 아늑한 브런치 카페 | 어쿠스틱 포크, 보사노바, 재즈 피아노 | 80~110 | 0.35~0.50 | 평온, 온화, 산뜻 |
| 아날로그 LP 바 | 7080 소울, 블루스, 포크 록 | 70~100 | 0.25~0.45 | 노스탤지어, 우수 |
| 조용한 서점 | 미니멀 클래식, 로파이, 앰비언트 | 60~90 | 0.10~0.30 | 차분, 집중 |

**감성적 초기화:** 날씨, 시간, 웹 트렌드 + 사용자 사연 결합.

### 2.3 AI DJ 페르소나 및 멀티스테이지 프롬프트 파이프라인

- **STAGE 1:** 감성 맥락 파싱 (사연, Weather, Time, Trend)
- **STAGE 2:** 라디오 스크립트 작성 (페르소나, 쿠션 멘트, SSML 호흡)
- **STAGE 3:** 멘트-오디오 제어 JSON (ducking_volume, ramp_duration, script, ssml)

---

## 3. 기술 아키텍처 및 구현 타당성 검토

### 3.1 인앱 WebView 주입식 제어

**Architecture A — BYOK, 서버 비용 Zero**

1. **DOM 변경 취약성** → SVD(셀렉터 자가 검증) 파이프라인, Fallback Dictionary
2. **CSP 우회** → `WebViewClient.shouldInterceptRequest` HTML 변조

```kotlin
override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
    val url = request?.url.toString()
    if (url.contains("music.youtube.com")) {
        // CSP meta 무력화, NativeAudioBridge 주입
    }
    return null
}
```

3. **Anti-Automation** → 실제 WebView 사용자 세션 (헤드리스 회피)

### 3.2 백그라운드 재생 유지

- Partial Wake Lock
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` + MediaSessionService
- Web Worker / HackTimer.js 타이머 보호

### 3.3 TTS/LLM 비동기 처리 및 오디오 Ducking

```
[사연 입력] → [LLM BYOK] → [SSML + 페이더 파라미터]
    → [TTS BYOK] → [Web Audio API]
        → Ducking In → TTS 재생 → Ducking Out
```

**노드 구조:** YouTube Music MediaElement → Music GainNode → destination  
TTS AudioBuffer → Speech GainNode → destination

---

## 4. 프로젝트 로드맵 및 리스크 관리

### 4.1 저작권(DMCA) 방어

- 클라이언트 프록시: 음원은 사용자 YT Music 세션에서 직접 렌더링
- API Key: AES-256-GCM 로컬 암호화 저장

### 4.2 단계별 로드맵

| Phase | 기간 | 목표 |
|-------|------|------|
| **Phase 1 MVP** | 1~3개월 | Android APK, WebView YT Music, 쿠션 경로 검증 |
| **Phase 2** | 4~6개월 | 백그라운드 방어, Ducking, 공간 프로필 |
| **Phase 3 B2B** | 7개월+ | B2B API 제휴, 어드민 콘솔, 공연권 계약 |

### 4.3 리스크 완화

- 상업 공간 GPS 가드 + B2B 라이선스 유도
- DOM 변경 핫픽스 배포 파이프라인

---

## 5. 결론 및 제언

1. DOM 파싱 SVD 모듈 최우선 확보 후 쿠션 알고리즘 착수
2. MediaSessionService 골조로 앱 프레임워크 개발
3. B2B 확장을 위한 Loose Coupling (BYOK ↔ B2B API 플러그인 교체 가능)
