<!-- 아래 이미지 경로를 프로젝트 로고 경로로 교체하세요. -->
<p align="center">
  <img src="docs/assets/compose-hig.svg" width="128" height="128" alt="Compose-HIG 로고 자리">
</p>

# Compose-HIG

Compose Multiplatform에서 Apple HIG 스타일 컴포넌트, Liquid Glass 효과, 선택적 UIKit 연동을 제공하는 UI 라이브러리입니다.

[![Maven Central](https://img.shields.io/maven-central/v/zone.ien.hig/hig?style=flat)](https://central.sonatype.com/artifact/zone.ien.hig/hig)
[![빌드](https://github.com/ienground/compose-hig/actions/workflows/gradle.yml/badge.svg)](https://github.com/ienground/compose-hig/actions/workflows/gradle.yml)
[![라이선스](https://img.shields.io/badge/license-Apache%202.0-blue?style=flat)](LICENSE.txt)

[English](README.md) | **한국어**

<a id="overview"></a>

## 개요

Compose-HIG는 [Apple Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines/) 스타일을 공통 [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) UI에서 사용할 수 있게 합니다. 모든 대상에서 Cupertino 디자인을 직접 사용하거나, iOS에서 UIKit 기반 컨트롤을 선택하거나, Cupertino와 Material 3를 전환하는 적응형 컴포넌트를 사용할 수 있습니다.

[alexzhirkevich/compose-cupertino](https://github.com/alexzhirkevich/compose-cupertino)와 [slanos/compose-cupertino](https://github.com/slanos/compose-cupertino)를 바탕으로 배경 레이어 기반 Liquid Glass 렌더링, 추가 컨트롤, 적응형 내비게이션을 확장했습니다. 대부분의 UI는 Compose로 렌더링하며, iOS 네이티브 컨트롤은 `hig-native`에서 별도로 제공합니다.

이 문서는 현재 프로젝트 API를 기준으로 하며, 설치 예제에는 버전 placeholder를 사용합니다. [설치](#installation)와 [호환성](#compatibility)을 참고하세요.

<a id="contents"></a>

## 목차

- [개요](#overview)
- [목차](#contents)
- [미리보기](#preview)
  - [iPhone Duo](#duo-preview)
- [주요 기능](#features)
- [플랫폼 지원](#platforms)
- [설치](#installation)
- [빠른 시작](#quick-start)
- [사용 예제](#examples)
  - [Liquid Glass 배경](#glass)
  - [세그먼트 컨트롤](#segments)
  - [게이지](#gauges)
  - [페이지 탭 뷰](#page-tab-view)
  - [스와이프 액션](#swipe)
  - [당겨서 새로고침](#pull-to-refresh)
  - [컨텍스트 메뉴](#context-menu)
  - [검색 토큰](#search-tokens)
  - [그룹 목록](#sections)
  - [레이블](#labels)
  - [적응형 컴포넌트](#adaptive)
  - [사이드바](#sidebar)
  - [iPhone Duo 레이아웃](#duo)
  - [iOS 네이티브 대화상자](#native)
  - [색상 피커](#color-picker)
  - [공유 시트](#share)
  - [웹 뷰](#web-view)
  - [전체 화면 커버](#cover)
- [API 레퍼런스](#api)
  - [컴포넌트 구성](#component-api)
  - [테마 및 상태](#theme-api)
  - [Liquid 버튼 매개변수](#glass-api)
- [호환성 및 제한 사항](#compatibility)
  - [iOS 네이티브 UI 깜빡임](#flickering)
- [개발](#development)
- [라이선스 및 출처](#license)

<a id="preview"></a>

## 미리보기

[샘플 앱](example/composeApp/)으로 제작한 **[WasmJS 인터랙티브 데모](https://hig.ien.zone)**를 확인하세요.

[![Compose-HIG iOS 샘플: 스와이프 액션, 세그먼트 선택, 그룹 목록](docs/assets/compose-hig-ios-demo.gif)](docs/assets/compose-hig-ios-demo.mp4)

[MP4 시연 영상 재생 및 다운로드](docs/assets/compose-hig-ios-demo.mp4). 현재 소스를 빌드해 iOS 27.0의 iPhone 18 Pro 시뮬레이터에서 촬영했습니다. 스와이프 액션, 섹션 화면 이동, Lazy/Default 세그먼트 선택, 스크롤, 스위치 조작을 보여줍니다.

<details>
<summary>이전 시연 영상</summary>

https://github.com/user-attachments/assets/4cc67248-f666-4ba1-9f32-988356b96b2f

</details>

배포된 웹 데모와 이전 영상은 현재 체크아웃의 구현과 다를 수 있습니다.

<a id="duo-preview"></a>

### iPhone Duo

[![iPhone Duo: 접힌·펼친 레이아웃, 세그먼트 컨트롤, 하단 시트](docs/assets/compose-hig-duo-demo.gif)](docs/assets/compose-hig-duo-demo.mp4)

[iPhone Duo MP4 시연 영상 재생 및 다운로드](docs/assets/compose-hig-duo-demo.mp4). 현재 소스를 빌드해 iOS 27.1의 iPhone Duo 시뮬레이터에서 촬영했습니다. 접힌 상태와 펼친 상태를 각각 촬영해 컷으로 연결했습니다. 펼친 상태에서는 Compose-HIG를 Compose Adaptive(Compose Multiplatform Material 3 Adaptive) 및 Navigation 3와 조합한 목록·상세 내비게이션과 모달 하단 시트를 보여줍니다.

| 접힌 상태: 오른쪽 컨트롤 | 펼친 상태: 목록과 상세 |
|--------------------------|------------------------|
| [<img src="docs/assets/compose-hig-duo-closed.png" width="240" alt="오른쪽에 컨트롤을 세로로 배치한 접힌 iPhone Duo">](docs/assets/compose-hig-duo-closed.png) | [<img src="docs/assets/compose-hig-duo-open.png" width="420" alt="내비게이션 목록과 섹션 상세를 함께 표시하는 펼친 iPhone Duo">](docs/assets/compose-hig-duo-open.png) |

<details>
<summary>iPhone Duo의 모달 하단 시트</summary>

[<img src="docs/assets/compose-hig-duo-sheet.png" width="640" alt="가로형 상단 바와 Done 액션이 있는 iPhone Duo 모달 시트">](docs/assets/compose-hig-duo-sheet.png)

</details>

통합 방법과 플랫폼 요구 사항은 [iPhone Duo 레이아웃](#duo)을 참고하세요.

<a id="features"></a>

## 주요 기능

- **Liquid Glass 표면** — 버튼, 아이콘 버튼, 대화상자, 세그먼트 컨트롤, 내비게이션 표면에 배경 렌더링을 활용한 블러, 굴절, 하이라이트, 그림자, 배경에 맞춘 색상을 적용합니다. [배경 레이어를 연결](#glass)하면 유리 너머로 콘텐츠를 표시할 수 있습니다.
- **상호작용 컨트롤** — 세그먼트 선택은 드래그와 스프링 애니메이션을 지원하며, 스와이프 행은 양쪽 액션 표시와 전체 스와이프 액션 실행을 지원합니다. [세그먼트 컨트롤](#segments)과 [스와이프 액션](#swipe)을 참고하세요.
- **게이지** — `CupertinoGauge`는 SwiftUI `Gauge`의 네 가지 스타일(선형 용량, 액세서리 선형, 액세서리 원형, 원형 용량)로 범위 안의 값을 표시하며, 단색·그라디언트 색조와 범위·백분율 접근성 시맨틱을 지원합니다. [게이지](#gauges)를 참고하세요.
- **당겨서 새로고침** — `CupertinoPullToRefreshBox`는 세로 스크롤 콘텐츠에 iOS 러버밴드 당김, 점진적으로 나타나는 스피너, 임계값 햅틱, 접근성 새로고침 액션을 제공합니다. [당겨서 새로고침](#pull-to-refresh)을 참고하세요.
- **컨텍스트 메뉴** — `CupertinoContextMenuBox`는 길게 누르거나 보조 클릭하면 콘텐츠를 들어 올리고 배경을 블러 처리한 뒤 `CupertinoDropdownMenu`와 같은 메뉴 항목을 표시합니다. [컨텍스트 메뉴](#context-menu)를 참고하세요.
- **페이지 탭 뷰** — `CupertinoPageTabView`는 SwiftUI `TabView`의 `.tabViewStyle(.page)`처럼 페이지를 넘기며, 자동·항상·숨김 페이지 인덱스와 상호작용형 캡슐 배경을 지원합니다. [페이지 탭 뷰](#page-tab-view)를 참고하세요.
- **그룹 및 접기 가능한 목록** — `CupertinoSection`, `section`, `stickySection`으로 그룹, 인셋, 일반, 사이드바 레이아웃을 구성하고 스타일과 섹션 상태를 공유합니다. 디스클로저 그룹(`CupertinoDisclosureGroup`, `SectionDisclosureGroup`, `disclosureGroup`)으로 하위 행을 펼치거나 접을 수 있습니다.
- **iPhone Duo 레이아웃** — 힌지를 반영한 시스템 영역 처리로 세로 바 레이아웃이 활성화되면 오른쪽에 컨트롤을 배치합니다. 적응형 목록·상세 헬퍼는 실제 오른쪽 패널에 이 처리를 한정합니다. [스크린샷·영상](#duo-preview)과 [통합 방법](#duo)을 참고하세요.
- **화면 구조와 내비게이션** — Scaffold, 상단·하단 앱 바, 내비게이션 바, 모달·고정형 하단 시트, 네 가지 크기의 플로팅 액션 버튼으로 화면을 구성합니다.
- **전체 화면 커버** — `CupertinoFullScreenCover`는 스프링 애니메이션으로 올라와 창 전체를 덮는 모달 흐름을 `isPresented` 또는 `item` API로 표시하며, Android 뒤로 가기는 `onDismissRequest`로 전달됩니다. [전체 화면 커버](#cover)를 참고하세요.
- **입력·선택·피커** — 일반·테두리·검색 입력창, 스위치, 체크박스, 슬라이더와 범위 슬라이더, 활동 표시기, 휠·날짜·시간·날짜시간 피커로 일반적인 상호작용을 구현합니다.
- **검색 토큰** — `CupertinoSearchTextField`는 `UISearchTextField`, SwiftUI `.searchable(text:tokens:)`처럼 커서 앞에 틴트된 캡슐 칩으로 표시되는 `tokens`를 지원합니다. [검색 토큰](#search-tokens)을 참고하세요.
- **색상 피커** — `CupertinoColorPicker`(SwiftUI `ColorPicker` 형태의 행), `CupertinoColorWell`, `CupertinoColorPickerSheet`는 그리드·스펙트럼·RGB 슬라이더 보기와 불투명도, 16진수 입력, 호이스팅된 최근 색상을 제공합니다. [색상 피커](#color-picker)를 참고하세요.
- **선택적 iOS 네이티브 컨트롤** — `hig-native`는 UIKit 기반 대화상자, 액션 시트, 메뉴, 날짜·시간 피커, 내비게이션 바를 제공합니다. 다른 대상에서는 공통 네이티브 API에 Compose 대체 구현을 사용합니다.
- **시스템 공유 시트** — `CupertinoShareLink`와 `rememberCupertinoShareSheetState`로 iOS의 `UIActivityViewController`와 Android 공유 시트를 통해 텍스트·링크·파일을 공유하며, 데스크톱과 웹에서는 클립보드에 복사합니다. [공유 시트](#share)를 참고하세요.
- **웹 콘텐츠 내장** — `hig-native`의 `CupertinoWebView`는 iOS에서 `WKWebView`, Android에서 `android.webkit.WebView`를 사용하며 로딩 진행률, 제목, 방문 기록, 오류를 관찰 가능한 상태와 내비게이터로 제공합니다. 다른 대상에서는 브라우저로 여는 대체 화면을 표시합니다. [웹 뷰](#web-view)를 참고하세요.
- **적응형 디자인 시스템** — `hig-adaptive`는 기본적으로 iOS에서 Cupertino, 나머지 플랫폼에서 Material 3를 선택합니다. 컴포넌트별 설정과 Navigation 3 목록·상세 헬퍼도 제공합니다. [테마 선택 및 재정의](#adaptive)를 참고하세요.
- **사이드바 내비게이션** — `CupertinoSidebarScaffold`는 태블릿과 폴더블에서 토글 가능한 고정 사이드바 열을 표시하고, 휴대폰에서는 상세 화면을 푸시하는 목록으로 접힙니다. [사이드바](#sidebar)를 참고하세요.
- **공통 테마와 플랫폼 연동** — `hig-core`에서 색상 체계, 타이포그래피, 연속 곡선 도형, iOS 햅틱, 접근성 설정을 제공합니다. 플랫폼별 동작은 [지원 표](#platforms)에 설명되어 있습니다.
- **Cupertino 벡터 아이콘** — `CupertinoIcons.Outlined`와 `CupertinoIcons.Filled`는 Compose `ImageVector` 아이콘을 제공하며, `Default`는 `Outlined`의 별칭입니다. 선택적 확장 모듈로 더 많은 아이콘을 사용할 수 있습니다.
- **레이블** — `CupertinoLabel`은 SwiftUI의 `Label`처럼 아이콘과 제목을 함께 표시하며, `TitleAndIcon`, `TitleOnly`, `IconOnly` 스타일을 컨테이너에서 `ProvideCupertinoLabelStyle`로 지정할 수 있습니다. [레이블](#labels)을 참고하세요.

<a id="platforms"></a>

## 플랫폼 지원

[공통 타깃 설정](buildSrc/src/main/kotlin/multiplatform-module-convention.gradle.kts)에 선언된 라이브러리 대상을 정리했습니다. ‘설정됨’은 소스·빌드 대상이 존재한다는 의미이며, 모든 효과와 기기에서 검증했다는 뜻은 아닙니다.

| 플랫폼 | 상태 / 타깃 | 네이티브 API 동작 | 기본 적응형 테마 |
|--------|-------------|------------------|------------------|
| Android | 설정됨: Android 라이브러리, 최소 API 29 | Compose 대체 구현 | Material 3 |
| iOS | 설정됨: `iosArm64`, `iosSimulatorArm64` | UIKit 구현 | Cupertino |
| Desktop JVM | 설정됨: `desktop` | Compose 대체 구현 | Material 3 |
| macOS Native | 실험적 Compose 타깃: `macosArm64` | UIKit이 아닌 Compose 대체 구현 | Material 3 |
| Web JavaScript | 설정됨: `js(IR)` 브라우저 | Compose 대체 구현 | Material 3 |
| WebAssembly | 설정됨: `wasmJs` 브라우저, 샘플 및 웹 데모 제공 | Compose 대체 구현 | Material 3 |

Intel Apple 타깃(`iosX64`, `macosX64`), tvOS, watchOS는 선언되어 있지 않습니다. 샘플 앱은 Android, iOS, Desktop JVM, WasmJS를 대상으로 하며 JS와 macOS Native 전용 실행 진입점은 제공하지 않습니다.

iOS 햅틱은 UIKit 생성기를 사용합니다. 다른 대상은 Compose의 `LocalHapticFeedback`에 위임하므로 기기 지원에 따라 결과가 달라집니다. 투명도 줄이기 설정은 iOS에서 읽으며, 비iOS 구현은 `false`를 반환합니다. 고대비 관찰은 플랫폼별 구현을 사용하므로 모든 대상에서 동일한 시스템 연동을 제공하지는 않습니다.

<a id="installation"></a>

## 설치

`settings.gradle.kts`에 저장소를 설정하세요.

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

`{current version}`을 사용할 버전으로 바꾼 뒤 `commonMain`에 기본 UI 의존성을 추가하세요.

```kotlin
val hig = "{current version}"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("zone.ien.hig:hig:${hig}")
        }
    }
}
```

또는 [Gradle Version Catalog](https://docs.gradle.org/current/userguide/version_catalogs.html)를 사용하세요. 필요한 모듈만 선택하면 됩니다.

```toml
[versions]
hig = "{current version}"

[libraries]
hig = { module = "zone.ien.hig:hig", version.ref = "hig" }
hig-adaptive = { module = "zone.ien.hig:hig-adaptive", version.ref = "hig" }
hig-native = { module = "zone.ien.hig:hig-native", version.ref = "hig" }
hig-icons-extended = { module = "zone.ien.hig:hig-icons-extended", version.ref = "hig" }
```

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.hig)
        }
    }
}
```

| 아티팩트 | 용도 |
|----------|------|
| [`zone.ien.hig:hig`](https://central.sonatype.com/artifact/zone.ien.hig/hig) | Compose Cupertino UI, `hig-core`를 전이 의존성으로 포함 |
| [`zone.ien.hig:hig-core`](https://central.sonatype.com/artifact/zone.ien.hig/hig-core) | 테마 기반, 색상, 도형, 접근성, 햅틱 |
| [`zone.ien.hig:hig-native`](https://central.sonatype.com/artifact/zone.ien.hig/hig-native) | iOS 네이티브 래퍼와 비iOS 대체 구현, `hig` 포함 |
| [`zone.ien.hig:hig-adaptive`](https://central.sonatype.com/artifact/zone.ien.hig/hig-adaptive) | Cupertino/Material 3 전환, `hig`와 `hig-native` 포함 |
| [`zone.ien.hig:hig-icons-extended`](https://central.sonatype.com/artifact/zone.ien.hig/hig-icons-extended) | 추가 Cupertino 벡터 아이콘 |

로컬 개발에서는 [샘플처럼](example/composeApp/build.gradle.kts) 프로젝트 의존성을 사용하거나 [Maven Local 안내](#development)를 참고하세요.

<a id="quick-start"></a>

## 빠른 시작

UI를 `CupertinoTheme`으로 감싸고 실험적 Cupertino API 사용을 선언하세요. 변경 가능한 값은 호출자가 관리합니다. 다음 컴포저블은 스위치가 있는 테마 화면을 구성합니다.

```kotlin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import zone.ien.hig.CupertinoScaffold
import zone.ien.hig.CupertinoSwitch
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.theme.CupertinoTheme

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun SettingsScreen() {
    CupertinoTheme {
        var notificationsEnabled by rememberSaveable { mutableStateOf(true) }

        CupertinoScaffold { contentPadding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(contentPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CupertinoText("Notifications")
                CupertinoSwitch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                )
            }
        }
    }
}
```

플랫폼별 Compose 호스트에서 `SettingsScreen()`을 호출하세요. Scaffold가 전달한 `PaddingValues`를 적용하면 바 및 인셋 영역과 콘텐츠가 겹치는 것을 피할 수 있습니다. 전체 통합 예제는 [Cupertino 쇼케이스](example/composeApp/src/commonMain/kotlin/cupertino/CupertinoWidgetsScreen.kt)를 참고하세요.

<a id="examples"></a>

## 사용 예제

다음 컴포저블은 `AdaptiveTheme`을 직접 구성하는 경우를 제외하면 상위에 `CupertinoTheme`이 있다고 가정합니다. 예제는 현재 프로젝트 API를 사용합니다.

<a id="glass"></a>

### Liquid Glass 배경

Backdrop에는 유리 뒤의 콘텐츠를 기록해야 합니다. 배경 형제 요소에 `Modifier.layerBackdrop`을 적용하고 같은 backdrop을 버튼에 전달하세요. 유리 컨트롤은 기록 대상 레이어 밖에 배치합니다.

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import zone.ien.hig.CupertinoLiquidButton
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.utils.rememberDefaultBackdrop

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun GlassButton(onClick: () -> Unit) {
    val backdrop = rememberDefaultBackdrop()
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .layerBackdrop(backdrop)
                .background(Brush.linearGradient(listOf(Color.Blue, Color.Magenta))),
        )
        CupertinoLiquidButton(
            onClick = onClick,
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.Center).padding(16.dp),
        ) {
            CupertinoText("Continue")
        }
    }
}
```

`rememberDefaultBackdrop()`은 테마 배경으로 초기화한 `LayerBackdrop`을 생성합니다. 생성만으로 주변 콘텐츠가 캡처되지는 않습니다. `isBackgroundAdaptive`와 `isInteractive`의 기본값은 `true`입니다. [버튼 매개변수](#glass-api)와 [Backdrop 구현](hig/src/commonMain/kotlin/zone/ien/hig/utils/Backdrop.kt)을 참고하세요.

<a id="segments"></a>

### 세그먼트 컨트롤

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import zone.ien.hig.CupertinoSegmentedControl
import zone.ien.hig.CupertinoSegmentedControlTab
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun FilterControl() {
    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    CupertinoSegmentedControl(selectedTabIndex = selectedIndex) {
        listOf("All", "Favorites").forEachIndexed { index, label ->
            CupertinoSegmentedControlTab(
                isSelected = selectedIndex == index,
                onClick = { selectedIndex = index },
            ) {
                CupertinoText(label)
            }
        }
    }
}
```

`selectedTabIndex`와 각 탭의 `isSelected`를 일치시켜야 합니다. 선택 콜백은 각 탭에 전달하며, 부모 컨트롤에는 `onSelectedTabChange` 매개변수가 없습니다. 주변 콘텐츠를 배경으로 사용하려면 외부 `LayerBackdrop`을 전달할 수 있습니다.

<a id="gauges"></a>

### 게이지

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import zone.ien.hig.CupertinoGauge
import zone.ien.hig.CupertinoGaugeDefaults
import zone.ien.hig.CupertinoGaugeStyle
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun HeartRate(bpm: Float) {
    Column {
        CupertinoGauge(
            value = bpm,
            range = 50f..180f,
            label = { CupertinoText("Heart rate") },
            currentValueLabel = { CupertinoText("${bpm.toInt()} BPM") },
            minimumValueLabel = { CupertinoText("50") },
            maximumValueLabel = { CupertinoText("180") },
        )
        Row {
            CupertinoGauge(
                value = bpm,
                range = 50f..180f,
                style = CupertinoGaugeStyle.AccessoryCircular,
                colors = CupertinoGaugeDefaults.colors(gradient = listOf(Color.Green, Color.Yellow, Color.Red)),
                currentValueLabel = { CupertinoText("${bpm.toInt()}") },
            )
            CupertinoGauge(value = 0.7f, style = CupertinoGaugeStyle.AccessoryCircularCapacity)
        }
    }
}
```

범위를 벗어난 값은 범위 안으로 제한되며, `NaN`이나 폭이 0인 범위는 빈 게이지로 표시됩니다. 선형 스타일은 오른쪽에서 왼쪽 레이아웃에서도 시작 가장자리부터 채워지고, 원형 스타일은 시계 방향으로 진행합니다. 임의의 `Brush`를 사용하려면 `CupertinoGaugeDefaults.colors(tint = brush)`를 전달하세요.

<a id="page-tab-view"></a>

### 페이지 탭 뷰

```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import zone.ien.hig.CupertinoPageIndexBackgroundDisplayMode
import zone.ien.hig.CupertinoPageIndexDisplayMode
import zone.ien.hig.CupertinoPageTabView
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun Onboarding() {
    val pages = listOf("Welcome", "Stay in the loop", "Ready to go")
    val pagerState = rememberPagerState { pages.size }
    CupertinoPageTabView(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().height(320.dp),
        indexDisplayMode = CupertinoPageIndexDisplayMode.Automatic,
        backgroundDisplayMode = CupertinoPageIndexBackgroundDisplayMode.Interactive,
    ) { page ->
        CupertinoText(pages[page])
    }
}
```

`CupertinoPageTabView`는 SwiftUI `TabView`의 `.tabViewStyle(.page)`에 해당하며, `HorizontalPager` 하단에 `CupertinoPageControl` 인덱스를 표시합니다. `indexDisplayMode`의 `Automatic`은 페이지가 하나일 때 인덱스를 숨기고, `Always`/`Never`로 강제할 수 있습니다. `backgroundDisplayMode`는 `.indexViewStyle(.page(backgroundDisplayMode:))`에 대응하며, `Interactive`(및 기본값 `Automatic`)는 인덱스를 터치하는 동안 캡슐 배경을 표시하고 2초 뒤 숨깁니다. 점을 탭하거나 인덱스를 가로로 드래그해 페이지를 바꿀 수 있습니다. `pageCount` 오버로드는 `PagerState`를 직접 기억합니다.

<a id="swipe"></a>

### 스와이프 액션

```kotlin
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import zone.ien.hig.CupertinoSwipeBox
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.swipebox.CupertinoSwipeBoxItem
import zone.ien.hig.theme.CupertinoColors
import zone.ien.hig.theme.systemRed

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun DeletableRow(onDelete: () -> Unit) {
    CupertinoSwipeBox(
        actionItemBuilder = {
            end(onClick = onDelete) {
                CupertinoSwipeBoxItem(
                    color = CupertinoColors.systemRed,
                    label = "Delete",
                    onClick = onDelete,
                )
            }
        },
    ) {
        CupertinoText("Swipe to delete", modifier = Modifier.padding(16.dp))
    }
}
```

빌더의 `end(onClick = ...)` 콜백은 전체 스와이프 실행을, 아이템의 `onClick`은 액션 탭을 처리합니다. 두 제스처로 같은 작업을 실행하려면 콜백을 모두 전달하세요. 반대쪽에는 `start { ... }`를 사용합니다. `startToEndFullSwipeEnabled` 또는 `endToStartFullSwipeEnabled`로 방향별 전체 스와이프를 끌 수 있으며 둘 다 기본값은 `true`입니다. `CupertinoSwipeBoxItem.restoreOnClick`의 기본값도 `true`입니다.

<a id="pull-to-refresh"></a>

### 당겨서 새로고침

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import zone.ien.hig.CupertinoPullToRefreshBox
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun RefreshableInbox(messages: List<String>, reload: suspend () -> Unit) {
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    CupertinoPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            scope.launch {
                try {
                    reload()
                } finally {
                    isRefreshing = false
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(messages) { CupertinoText(it) }
        }
    }
}
```

목록 맨 위에서 당기면 콘텐츠가 iOS 러버밴드 저항과 함께 아래로 이동하고, 활동 표시기의 살이 하나씩 나타납니다. 임계값을 넘으면 중간 강도 햅틱이 한 번 재생되고, 넘은 상태에서 손을 떼면 `onRefresh`가 호출됩니다. `isRefreshing`이 `true`인 동안 스피너가 유지되고 `false`가 되면 스프링 애니메이션으로 돌아갑니다. 코드에서 `isRefreshing = true`로 설정해도 표시됩니다. 스크린 리더는 박스 맨 위에서 "Refresh" 요소를 찾을 수 있으며(활성화하거나 사용자 지정 액션 사용), 로딩 중에는 이 요소가 "Refreshing"을 알립니다. 마우스 휠과 트랙패드로 당긴 경우에는 스크롤이 멈추면 놓은 것으로 처리됩니다. 목록 첫 항목의 `CupertinoNavigationTitle`은 iOS처럼 당기는 동안 커지며, 반투명 상단 바가 있는 `CupertinoScaffold` 안에서는 표시기가 바 아래에 보이도록 Scaffold 패딩을 박스에 적용하세요.

<a id="context-menu"></a>

### 컨텍스트 메뉴

```kotlin
import androidx.compose.runtime.Composable
import zone.ien.hig.CupertinoContextMenuBox
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.MenuAction
import zone.ien.hig.MenuDivider
import zone.ien.hig.theme.CupertinoColors
import zone.ien.hig.theme.systemRed

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun NoteWithContextMenu(onShare: () -> Unit, onDelete: () -> Unit) {
    CupertinoContextMenuBox(
        menuContent = {
            MenuAction(onClick = onShare) { CupertinoText("Share") }
            MenuDivider()
            MenuAction(onClick = onDelete, contentColor = CupertinoColors.systemRed) {
                CupertinoText("Delete")
            }
        },
    ) {
        CupertinoText("Long press me")
    }
}
```

길게 누르거나 마우스로 보조 클릭하면 콘텐츠 스냅샷이 떠오르고 배경이 블러·어둡게 처리되며, 메뉴는 그 위나 아래에 배치됩니다. 화면보다 큰 콘텐츠는 보이도록 축소됩니다. 액션은 `onClick` 실행 후 메뉴를 닫으며, 바깥 탭, 뒤로 가기, Escape로도 닫힙니다. 스크린 리더에는 "Show menu" 사용자 지정 액션이 제공되고, `rememberCupertinoContextMenuState()`로 메뉴를 코드에서 열 수 있으며, `preview` 슬롯으로 스냅샷 대신 다른 미리보기를 표시할 수 있습니다. 배경 블러는 가장 가까운 `CupertinoScaffold`를 읽습니다. iOS를 포함한 모든 플랫폼에서 Compose로 그려집니다. `UIContextMenuInteraction`은 Compose 콘텐츠를 미리보기로 사용할 수 없기 때문입니다.

<a id="search-tokens"></a>

### 검색 토큰

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import zone.ien.hig.CupertinoButton
import zone.ien.hig.CupertinoSearchTextField
import zone.ien.hig.CupertinoSearchToken
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.icons.CupertinoIcons
import zone.ien.hig.icons.filled.Heart

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun LibrarySearch() {
    var query by remember { mutableStateOf("") }
    var tokens by remember { mutableStateOf(listOf(CupertinoSearchToken("Photos"))) }

    Column {
        CupertinoSearchTextField(
            value = query,
            onValueChange = { query = it },
            tokens = tokens,
            onTokensChange = { tokens = it },
        )
        CupertinoButton(onClick = { tokens = tokens + CupertinoSearchToken("Favorites", CupertinoIcons.Filled.Heart) }) {
            CupertinoText("Favorites")
        }
    }
}
```

토큰이 넘치면 가로로 스크롤됩니다. 커서가 검색어 맨 앞에 있을 때(예: 검색어가 비어 있을 때) 첫 번째 백스페이스는 마지막 토큰을 선택하고 두 번째 백스페이스는 삭제합니다. 입력하면 선택이 해제되고, 토큰을 탭하면 선택되면서 필드에 포커스가 가며, 지우기·취소 버튼은 텍스트와 모든 토큰을 제거합니다. 백스페이스는 하드웨어 키와 소프트 키보드 모두에서 동작합니다. 토큰이 있는 동안 필드는 검색어 앞에 보이지 않는 폭 없는 공백(U+200B)을 유지하고, 이를 지우면 백스페이스로 처리합니다. `onValueChange`에는 이 문자가 전달되지 않습니다. 이 경로는 데스크톱에서만 테스트되었으므로 iOS 키보드와 대상 Android IME는 실제 기기에서 확인하세요. 읽기 전용이거나 비활성화된 필드는 토큰을 표시하지만 사용자가 선택하거나 제거할 수 없습니다. 각 토큰은 "<text>, token"으로 읽히며 "Remove <text>" 접근성 동작을 제공합니다. 이 문자열은 `tokenContentDescription`, `tokenRemoveActionLabel`, `tokenSelectActionLabel`로 바꿀 수 있습니다. 칩 스타일은 `tokenColors` 또는 `token` 슬롯으로 바꿀 수 있습니다.

<a id="sections"></a>

### 그룹 목록

```kotlin
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.section.section

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun AccountList() {
    LazyColumn {
        section(
            title = { CupertinoText("Account") },
            caption = { CupertinoText("Manage your account settings.") },
        ) {
            item { CupertinoText("Profile", Modifier.padding(16.dp)) }
            item { CupertinoText("Privacy", Modifier.padding(16.dp)) }
        }
    }
}
```

고정 헤더는 `stickySection`, 지연 로딩이 필요 없는 콘텐츠는 `CupertinoSection`을 사용하세요. `ProvideSectionStyle`로 `Sidebar`, `InsetGrouped`(기본값), `Grouped`, `Inset`, `Plain` 스타일을 공유할 수 있습니다. 접기 가능한 사이드바 섹션에는 `rememberSectionState()`를 전달하세요. [섹션 쇼케이스](example/composeApp/src/commonMain/kotlin/sections/SectionsScreen.kt)에서 각 변형을 확인할 수 있습니다.

디스클로저 그룹은 `section`/`stickySection`(지연 목록)과 `CupertinoSection` 안에서 하위 행을 그 자리에서 펼칩니다.

```kotlin
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.section.disclosureGroup
import zone.ien.hig.section.section

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun NetworkSettings() {
    var expanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn {
        section {
            disclosureGroup(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                title = { CupertinoText("Network") },
            ) {
                item { CupertinoText("Wi-Fi", Modifier.padding(it)) }
                item { CupertinoText("Bluetooth", Modifier.padding(it)) }
            }
        }
    }
}
```

`CupertinoSection` 안에서는 `SectionDisclosureGroup`을, 그 외에는 `CupertinoDisclosureGroup`을 사용하세요.

<a id="labels"></a>

### 레이블

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import zone.ien.hig.CupertinoLabel
import zone.ien.hig.CupertinoLabelStyle
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.ProvideCupertinoLabelStyle
import zone.ien.hig.icons.CupertinoIcons
import zone.ien.hig.icons.outlined.Heart
import zone.ien.hig.icons.outlined.SquareAndArrowUp

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun Labels() {
    Column {
        CupertinoLabel(text = "Favorites", icon = CupertinoIcons.Default.Heart)

        // SwiftUI의 .labelStyle(.iconOnly)처럼 내부의 Automatic 레이블은 아이콘만 표시하고,
        // 제목은 아이콘의 접근성 레이블로 유지됩니다.
        ProvideCupertinoLabelStyle(CupertinoLabelStyle.IconOnly) {
            Row {
                CupertinoLabel(text = "Share", icon = CupertinoIcons.Default.SquareAndArrowUp)
                CupertinoLabel(text = "Favorite", icon = CupertinoIcons.Default.Heart)
            }
        }
    }
}
```

명시적인 `style`이 컨테이너 스타일보다 우선하며, `Automatic`은 기본적으로 `TitleAndIcon`이 됩니다. 아이콘은 현재 텍스트 스타일에 맞춰 크기가 정해지고 제목의 첫 줄 중앙에 정렬되며, 오른쪽에서 왼쪽 레이아웃에서는 위치가 반전되고(글리프 자체는 `autoMirror = true`인 `ImageVector`만 반전됩니다) `LocalContentColor`를 따릅니다. 사용자 정의 콘텐츠에는 슬롯 오버로드 `CupertinoLabel(title = { ... }, icon = { ... })`를 사용하세요.

<a id="adaptive"></a>

### 적응형 컴포넌트

`hig-adaptive`가 필요합니다. 사용 중단되지 않은 테마 명세 오버로드를 명시적으로 사용하세요.

```kotlin
import androidx.compose.runtime.Composable
import zone.ien.hig.CupertinoText
import zone.ien.hig.adaptive.AdaptiveButton
import zone.ien.hig.adaptive.AdaptiveTheme
import zone.ien.hig.adaptive.CupertinoThemeSpec
import zone.ien.hig.adaptive.ExperimentalAdaptiveApi
import zone.ien.hig.adaptive.MaterialThemeSpec

@OptIn(ExperimentalAdaptiveApi::class)
@Composable
fun AdaptiveAction(onClick: () -> Unit) {
    AdaptiveTheme(
        material = MaterialThemeSpec.Default(),
        cupertino = CupertinoThemeSpec.Default(),
    ) {
        AdaptiveButton(onClick = onClick) {
            CupertinoText("Continue")
        }
    }
}
```

기본 테마는 iOS에서 Cupertino, 나머지 플랫폼에서 Material 3입니다. `target = Theme.Cupertino` 또는 `Theme.Material3`로 재정의할 수 있습니다. `MaterialThemeSpec`과 `CupertinoThemeSpec`으로 색상, 도형, 타이포그래피를 설정하고, 위젯의 `adaptation` 블록 안에서 `cupertino { ... }`와 `material { ... }`로 각각 조정합니다.

Navigation 3용 `rememberAdaptiveListDetailSceneStrategy<T>()`는 기본적으로 중간 너비부터 두 패널을 사용하며, `BackNavigationBehavior.PopLatest`와 `shouldHandleSinglePaneLayout = true`를 적용합니다. `AdaptiveListDetailPane(role)`로 오른쪽 시스템 영역 처리를 해당 패널에 한정하고, `isAdaptiveListDetailExpanded`로 목표 레이아웃을 확인하며, `adaptivePaneAnimation()`으로 160ms 페이드와 240ms 위치·크기 애니메이션을 적용합니다. 이 헬퍼에는 `ExperimentalMaterial3AdaptiveApi` 사용 선언이 필요합니다. 역할, 선호 너비, 미선택 안내 콘텐츠는 호출자가 entry 메타데이터로 제공해야 합니다. [구현](hig-adaptive/src/commonMain/kotlin/zone/ien/hig/adaptive/AdaptiveListDetail.kt)과 [샘플 내비게이션](example/composeApp/src/commonMain/kotlin/RootNavigationGraph.kt)을 참고하세요.

<a id="sidebar"></a>

### 사이드바

`hig-adaptive`가 필요합니다. `CupertinoSidebarScaffold`는 SwiftUI 2열 `NavigationSplitView`에 대응합니다. 자체 너비가 600dp 이상이면 사이드바가 유리 사이드바 재질의 고정 앞쪽 열로 표시되고, `CupertinoSidebarToggleButton`이 스프링 애니메이션으로 숨기거나 표시합니다. 더 좁으면 사이드바 목록이 루트 화면이 되고 `destination`을 선택하면 상세 화면이 푸시됩니다. 시스템 뒤로가기(Android 예측 뒤로가기 포함)나 `state.navigateBack()`으로 목록에 돌아갑니다. 행은 `sidebarItem`을 재사용하므로 선택 상태가 접근성 서비스에 노출되며, RTL에서는 사이드바가 오른쪽에 배치됩니다. `sidebarTopBar`를 지정하면 상단 윈도 인셋은 상단 바가 처리하고, 지정하지 않으면 목록에 상단 인셋이 적용됩니다.

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import zone.ien.hig.CupertinoNavigateBackLiquidButton
import zone.ien.hig.CupertinoText
import zone.ien.hig.CupertinoTopAppBar
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.adaptive.CupertinoSidebarScaffold
import zone.ien.hig.adaptive.CupertinoSidebarToggleButton
import zone.ien.hig.adaptive.CupertinoSidebarWidthClass
import zone.ien.hig.adaptive.rememberCupertinoSidebarState
import zone.ien.hig.section.SectionStyle
import zone.ien.hig.section.section
import zone.ien.hig.utils.rememberDefaultBackdrop

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun Mail() {
    val state = rememberCupertinoSidebarState()
    var mailbox by rememberSaveable { mutableStateOf("inbox") }
    CupertinoSidebarScaffold(
        modifier = Modifier.fillMaxSize(),
        state = state,
        selectedKey = mailbox,
        onSelect = { mailbox = it as String },
        sidebarTopBar = { CupertinoTopAppBar(title = { CupertinoText("Mailboxes") }) },
        sidebar = {
            section(style = SectionStyle.Sidebar) {
                destination(key = "inbox") { CupertinoText("Inbox") }
                destination(key = "sent") { CupertinoText("Sent") }
            }
        },
        detail = {
            CupertinoTopAppBar(
                title = { CupertinoText(mailbox) },
                navigationIcon = {
                    if (state.widthClass == CupertinoSidebarWidthClass.Compact) {
                        CupertinoNavigateBackLiquidButton(onClick = state::showSidebar, backdrop = rememberDefaultBackdrop())
                    } else {
                        CupertinoSidebarToggleButton(state)
                    }
                },
            )
        },
    )
}
```

<a id="duo"></a>

### iPhone Duo 레이아웃

샘플은 Compose-HIG를 [Compose Adaptive](hig-adaptive/build.gradle.kts)(`org.jetbrains.compose.material3.adaptive`) 및 Navigation 3와 조합합니다. Compose Adaptive는 창 크기에 따른 directive와 목록·상세 Scene 전략을 제공하고, Compose-HIG는 Cupertino 스타일과 패널별 시스템 영역 처리를 적용합니다.

`CupertinoScaffold`는 iOS 상태 영역, 카메라 가림 영역, safe-area 인셋, 힌지 상태를 관찰해 바 배치를 결정합니다. Duo에서는 시스템 예약 영역에 액션이 겹치지 않도록 콘텐츠와 컨트롤을 배치합니다.

| 레이아웃 | 동작 |
|----------|------|
| 접힌 Duo | 촬영한 좁은 화면에서는 콘텐츠를 단일 패널에 표시하고 상단 바 액션과 내비게이션 탭을 오른쪽에 세로로 배치합니다. |
| 펼친 Duo | 샘플의 넓어진 창에서 내비게이션 목록과 선택한 상세를 함께 표시합니다. `AdaptiveListDetailPane`은 실제 오른쪽에 놓인 패널에 오른쪽 바와 인셋을 적용합니다. |
| 모달 하단 시트 | `CupertinoBottomSheetContent`는 Duo에서도 시트 상단에 가로형 상단 바를 유지합니다. |

이 배치를 사용하려면 `CupertinoScaffold` 안에 `CupertinoTopAppBar`와 `CupertinoNavigationBar`를 구성하세요. 상단 바 제목은 위쪽에 유지되고, 뒤로가기 버튼과 액션은 오른쪽 컨트롤 영역으로 이동할 수 있습니다. 세로 내비게이션이 활성화되면 탭을 아이콘으로 표시하며, 탭을 0.5초 동안 누르면 바와 라벨을 펼칩니다. 드래그 선택은 손을 뗄 때 확정합니다.

목록·상세 화면은 [적응형 컴포넌트](#adaptive)의 설명처럼 `rememberAdaptiveListDetailSceneStrategy<T>()`에 호출자가 제공한 entry 역할을 연결하고 각 패널을 `AdaptiveListDetailPane(role)`로 감싸세요. 패널 수는 창 크기에 따른 적응형 directive가 결정하며, 힌지 감지는 바 배치 정보를 제공합니다. 각 Scaffold가 전달하는 `PaddingValues`도 콘텐츠에 적용해야 합니다.

힌지 관찰은 **iOS 27.1 이상**과 `UIHingeInteraction`을 제공하는 빌드 SDK에서 활성화되도록 보호되어 있습니다. 관찰자는 부분적으로 펼친 힌지와 완전히 펼친 힌지를 모두 열린 상태로 처리합니다. 위 미디어는 접힌·펼친 시뮬레이터 구성을 확인한 결과이며, 실기기나 모든 중간 힌지 각도를 검증한 결과는 아닙니다.

구현 참고: [iOS 배치 처리](hig/src/iosMain/kotlin/zone/ien/hig/CupertinoBarPlacement.ios.kt), [버전 보호가 적용된 UIKit 브리지](hig/src/nativeInterop/cinterop/cupertinoBarPlacement.def), [패널별 영역 처리](hig-adaptive/src/commonMain/kotlin/zone/ien/hig/adaptive/AdaptiveListDetail.kt), [샘플 내비게이션](example/composeApp/src/commonMain/kotlin/RootNavigationGraph.kt).

<a id="native"></a>

### iOS 네이티브 대화상자

`hig-native`가 필요합니다. 호출자가 대화상자의 표시 여부를 관리하고 닫기 요청이나 액션 실행 시 상태를 해제합니다.

```kotlin
import androidx.compose.runtime.Composable
import zone.ien.hig.CupertinoAlertDialogNative
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.cancel
import zone.ien.hig.destructive

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun DeleteConfirmation(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (visible) {
        CupertinoAlertDialogNative(
            onDismissRequest = onDismiss,
            title = "Delete item?",
            message = "This action cannot be undone.",
        ) {
            cancel(onClick = onDismiss, title = "Cancel")
            destructive(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                title = "Delete",
            )
        }
    }
}
```

iOS에서는 `UIAlertController`를 사용하며, 다른 대상에서는 Compose Cupertino 대화상자에 위임합니다. `NativeAlertDialogActionsScope`는 `action`, `default`, `cancel`, `destructive`를 제공하고 액션의 `enabled` 기본값은 `true`입니다. 공통 네이티브 API에는 액션 시트, 메뉴, 내비게이션 바, 날짜·시간 피커도 포함됩니다. `CupertinoColorPickerNative`는 iOS에서 `UIColorPickerViewController`를, 다른 대상에서는 Compose [색상 피커 시트](#color-picker)를 표시합니다. [네이티브 소스 세트](hig-native/src/)와 [깜빡임 대응 방법](#flickering)을 참고하세요.

<a id="color-picker"></a>

### 색상 피커

레이블과 색상 웰로 구성된 행입니다. 탭하면 그리드, 스펙트럼, 슬라이더 보기를 갖춘 "Colors" 시트가 표시됩니다.

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import zone.ien.hig.CupertinoColorPicker
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun TintRow() {
    var tint by remember { mutableStateOf(Color(0xFF007AFF)) }
    var recent by remember { mutableStateOf(emptyList<Color>()) }
    CupertinoColorPicker(
        selection = tint,
        onSelectionChange = { tint = it },
        supportsOpacity = true,
        recentColors = recent,
        onRecentColorsChange = { recent = it },
    ) {
        CupertinoText("Tint")
    }
}
```

웰만 필요하면 `CupertinoColorWell`을, 시트를 직접 표시하려면 `CupertinoColorPickerSheet`를 사용하세요. `supportsOpacity = false`이면 불투명도 슬라이더가 숨겨지고 선택한 색상은 항상 불투명합니다. 최근 색상은 호출자가 상태를 관리하며 저장되지 않습니다. iOS 시스템 피커가 필요하면 `hig-native`의 `CupertinoColorPickerNative`를 사용하세요. 다른 대상에서는 이 Compose 시트를 표시합니다.

<a id="share"></a>

### 공유 시트

`hig-native`가 필요합니다. `CupertinoShareLink`는 SwiftUI `ShareLink`에 해당하며, 직접 만든 트리거에는 `rememberCupertinoShareSheetState()`의 `share(items, subject)`와 `Modifier.cupertinoShareSheetAnchor(state)`를 사용합니다.

```kotlin
import androidx.compose.runtime.Composable
import zone.ien.hig.CupertinoShareItem
import zone.ien.hig.CupertinoShareLink
import zone.ien.hig.CupertinoShareResult
import zone.ien.hig.ExperimentalCupertinoApi

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun ShareArticle(url: String, onShared: () -> Unit) {
    CupertinoShareLink(
        items = listOf(
            CupertinoShareItem.Text("Worth a read"),
            CupertinoShareItem.Url(url),
        ),
        subject = "Article",
        onResult = { result ->
            if (result == CupertinoShareResult.Shared || result == CupertinoShareResult.Presented) onShared()
        },
    )
}
```

| 플랫폼 | 동작 | 결과 |
|--------|------|------|
| iOS | 최상위 뷰 컨트롤러에서 `UIActivityViewController`를 표시하며, iPad에서는 버튼에 고정된 팝오버로 표시 | `Shared`, `Dismissed`, `CopiedToClipboard`, `Unavailable` |
| Android | `ACTION_SEND` / `ACTION_SEND_MULTIPLE`, `EXTRA_SUBJECT`를 담은 `Intent.createChooser`. 파일에는 읽기 URI 권한과 `ClipData`를 부여 | `Presented`(선택기가 결과를 알려 주지 않음), `Unavailable` |
| 데스크톱, macOS, 웹 | 텍스트와 링크를 클립보드에 복사 | `CopiedToClipboard`, 파일은 `Unavailable` |

Android에서 `CupertinoShareItem.File`은 매니페스트에 선언한 `androidx.core.content.FileProvider` 등이 만든 `content://` URI여야 합니다([샘플 앱 매니페스트](example/androidApp/src/main/AndroidManifest.xml) 참고). `file://` URI와 일반 경로는 `Unavailable`을 반환합니다. iOS에서는 절대 경로나 `file://` URL을 전달합니다.

<a id="web-view"></a>

### 웹 뷰

`hig-native`가 필요합니다. `CupertinoWebView`는 플랫폼 웹 엔진을 내장하고, 직접 만든 주소창·툴바에서 사용할 수 있도록 상태를 노출합니다.

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import zone.ien.hig.CupertinoProgressIndicator
import zone.ien.hig.CupertinoWebView
import zone.ien.hig.CupertinoWebViewDefaults
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.rememberCupertinoWebViewState

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun DocumentationScreen(modifier: Modifier = Modifier) {
    val state = rememberCupertinoWebViewState("https://developer.apple.com/design/")
    Column(modifier) {
        if (state.isLoading) {
            CupertinoProgressIndicator(progress = state.progress, modifier = Modifier.fillMaxWidth())
        }
        CupertinoWebView(
            state = state,
            modifier = Modifier.weight(1f),
            settings = CupertinoWebViewDefaults.settings(javaScriptEnabled = true),
        )
    }
    // Android: BackHandler(enabled = state.canGoBack) { state.navigator.goBack() } (androidx.activity)
    // state.title, state.canGoBack, state.lastError, ...
    // state.navigator.goBack() / goForward() / reload() / stopLoading()
    // CupertinoWebViewDefaults.normalizeUrl("example.com") == "https://example.com"
}
```

iOS에서는 `WKWebView`, Android에서는 `android.webkit.WebView`를 사용하며 Android 앱 매니페스트에 `<uses-permission android:name="android.permission.INTERNET" />`이 필요합니다. 데스크톱, macOS, JS, Wasm에는 내장 가능한 웹 엔진이 없으므로 URL과 `LocalUriHandler`를 사용하는 **Open in Browser** 버튼이 있는 테마 적용 대체 화면을 표시합니다. Android에서는 내비게이션·시트와 순서가 맞도록 앱의 뒤로 가기 처리에 연결하세요. 예: androidx.activity의 `BackHandler(enabled = state.canGoBack) { state.navigator.goBack() }`. `handleBackPress = true`는 단일 화면 호스트용 옵트인입니다. 창에 직접 등록되므로 다른 뒤로 가기 핸들러와의 순서가 보장되지 않습니다. 상태는 `rememberSaveable`로 저장되므로(URL과 제목, Android에서는 웹 방문 기록까지) 구성 변경과 프로세스 종료 후에도 페이지가 유지됩니다. `onCreated`는 쿠키나 JavaScript 브리지 설정을 위해 네이티브 뷰를 전달합니다.

<a id="cover"></a>

### 전체 화면 커버

`CupertinoFullScreenCover`는 SwiftUI의 `fullScreenCover`에 대응합니다. 창 전체를 덮으며 아래에서 올라오고, 콘텐츠 안의 버튼으로 닫습니다.

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import zone.ien.hig.CupertinoButton
import zone.ien.hig.CupertinoFullScreenCover
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun Onboarding() {
    var presented by rememberSaveable { mutableStateOf(false) }

    CupertinoButton(onClick = { presented = true }) { CupertinoText(text = "Get started") }

    CupertinoFullScreenCover(
        isPresented = presented,
        onDismissRequest = { presented = false }, // Android back
    ) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            CupertinoButton(
                onClick = { presented = false },
                modifier = Modifier.align(Alignment.TopEnd),
            ) { CupertinoText(text = "Close") }
        }
    }
}
```

커버는 창 전체 크기의 대화상자에 호스팅되므로 어디에서나 선언할 수 있고, 입력과 접근성 측면에서 모달로 동작합니다. 콘텐츠는 edge-to-edge로 그려지므로 `safeDrawingPadding()`을 적용하거나 `CupertinoTopAppBar`를 사용해 안전 영역을 지키세요. Android 뒤로 가기(데스크톱의 Escape)는 `CupertinoFullScreenCoverProperties(dismissOnBackPress = false)`가 아니면 `onDismissRequest`를 호출합니다. 스와이프로 닫기는 기본적으로 꺼져 있으며 `interactiveDismissEnabled = true`로 켤 수 있습니다. `item` 오버로드는 항목이 null이 아닐 때 표시되고, 닫히는 애니메이션 동안 마지막 항목을 유지하며, 항목이 바뀌면 콘텐츠를 교체합니다. `onDismiss`는 닫힘 애니메이션이 끝난 뒤 호출됩니다. 상태 바와 내비게이션 바 아이콘은 커버의 `containerColor` 밝기를 따르므로, 라이트 모드에서도 검은 커버에는 밝은 아이콘이 표시됩니다.

<a id="api"></a>

## API 레퍼런스

공개 컴포넌트 함수는 주로 `zone.ien.hig`, 테마는 `zone.ien.hig.theme`, 섹션은 `zone.ien.hig.section`, 스와이프 아이템·상태는 `zone.ien.hig.swipebox`, 적응형 API는 `zone.ien.hig.adaptive`에 있습니다. 전체 시그니처와 KDoc은 아래 소스 링크에서 확인할 수 있습니다.

<a id="component-api"></a>

### 컴포넌트 구성

| API / 소스 | 용도 |
|------------|------|
| [`CupertinoButton`, `CupertinoIconButton`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoButton.kt) | 일반 버튼, 크기, 색상, 활성 상태, 콘텐츠 슬롯. |
| [`CupertinoLiquidButton`, `CupertinoLiquidIconButton`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoLiquidButton.kt) | 배경을 반영하는 버튼. [매개변수](#glass-api) 참고. |
| [`CupertinoAlertDialog`, `CupertinoActionSheet`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoDialogs.kt) | Compose 대화상자와 액션 빌더. [Liquid Glass 변형](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoLiquidDialogs.kt)도 제공. |
| [`CupertinoDropdownMenu`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoDropdownMenu.kt) | 액션, 섹션, 구분선이 있는 팝업 메뉴. |
| [`CupertinoDisclosureGroup`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoDisclosureGroup.kt), [`SectionDisclosureGroup`, `disclosureGroup`](hig/src/commonMain/kotlin/zone/ien/hig/section/SectionDisclosureGroup.kt) | 애니메이션 쉐브론이 있는 펼침 가능한 행. 단독 또는 섹션 안에서 사용. |
| [`CupertinoContextMenuBox`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoContextMenu.kt) | 떠오르는 미리보기, 블러 배경, `CupertinoMenuScope` 항목을 갖춘 길게 누르기 컨텍스트 메뉴. |
| [`CupertinoTextField`, `CupertinoBorderedTextField`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoTextField.kt) | 텍스트 입력. [검색 입력창](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoSearchTextField.kt)은 검색 전용 UI 제공. |
| [`CupertinoSearchToken`, `CupertinoSearchTokenDefaults`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoSearchToken.kt) | `CupertinoSearchTextField`의 `tokens` 오버로드용 검색 토큰. [검색 토큰](#search-tokens) 참고. |
| [`CupertinoCheckBox`, `CupertinoTriStateCheckBox`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoCheckbox.kt) | 2상태·3상태 선택. [스위치](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoSwitch.kt)로 토글 제공. |
| [`CupertinoSlider`, `CupertinoRangeSlider`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoSlider.kt) | 연속 값 및 범위 입력. |
| [`CupertinoColorPicker`, `CupertinoColorWell`, `CupertinoColorPickerSheet`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoColorPicker.kt) | 그리드, 스펙트럼, RGB 슬라이더, 16진수 입력, 불투명도, 최근 색상을 지원하는 색상 선택. |
| [`CupertinoPicker`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoPicker.kt) | 휠 선택. 전용 [날짜](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoDatePicker.kt)·[시간](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoTimePicker.kt)·[날짜시간](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoDateTimePicker.kt) API 제공. |
| [`CupertinoSegmentedControl`, `CupertinoSegmentedControlTab`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoSegmentedControl.kt) | 상태 기반 선택, 드래그 가능한 인디케이터, 탭 콜백. |
| [`CupertinoSwipeBox`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoSwipeBox.kt) | 시작·끝 액션, 기억되는 스와이프 상태, 선택적 전체 스와이프 실행. |
| [`CupertinoPageTabView`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoPageTabView.kt) | 페이지 인덱스가 있는 페이지 콘텐츠. 인덱스·배경 표시 모드, 스크럽, 조절 가능한 접근성. |
| [`section`, `stickySection`](hig/src/commonMain/kotlin/zone/ien/hig/section/LazyListSection.kt) | 지연 로딩 섹션. [CupertinoSection](hig/src/commonMain/kotlin/zone/ien/hig/section/Section.kt)은 비지연 콘텐츠 처리. |
| [`CupertinoScaffold`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoScaffold.kt) | 상단·하단 바, snackbar, FAB, 인셋, 콘텐츠 슬롯. |
| [`CupertinoSidebarScaffold`, `CupertinoSidebarToggleButton`](hig-adaptive/src/commonMain/kotlin/zone/ien/hig/adaptive/CupertinoSidebar.kt) | `CupertinoSidebarState`(표시 상태, 좁은 화면의 열), 단일 선택, 토글, 좁은 화면 푸시 내비게이션을 갖춘 사이드바·상세 분할 화면. |
| [`CupertinoTopAppBar`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoTopBars.kt) | 상단 앱 바. [내비게이션 바](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoNavigationBar.kt)와 [하단 앱 바](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoBottomAppBar.kt)도 제공. |
| [`CupertinoBottomSheet`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoBottomSheet.kt) | 모달 시트. [CupertinoBottomSheetScaffold](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoBottomSheetScaffold.kt)로 고정형 시트 구조 제공. |
| [`CupertinoShareLink`, `rememberCupertinoShareSheetState`](hig-native/src/commonMain/kotlin/zone/ien/hig/CupertinoShareSheet.kt) | 텍스트·링크·파일을 위한 시스템 공유 시트(`UIActivityViewController` / Android 선택기 / 클립보드 대체). [공유 시트](#share) 참고. |
| [`CupertinoLabel`, `ProvideCupertinoLabelStyle`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoLabel.kt) | `CupertinoLabelStyle`과 컨테이너 제공 스타일을 지원하는 아이콘·제목 레이블. |
| [`CupertinoFullScreenCover`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoFullScreenCover.kt) | 창 전체를 덮는 모달 커버. `isPresented`·`item` 오버로드, `CupertinoFullScreenCoverProperties`, `CupertinoFullScreenCoverDefaults.colors()` 제공. |
| [`CupertinoSmallFloatingActionButton`, `CupertinoMediumFloatingActionButton`, `CupertinoLargeFloatingActionButton`, `CupertinoExtraLargeFloatingActionButton`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoFloatingActionButton.kt) | 네 가지 크기의 플로팅 액션 버튼. |
| [`CupertinoGauge`, `CupertinoGaugeStyle`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoGauge.kt) | 범위 안의 읽기 전용 값 표시. 선형 용량, 액세서리 선형, 액세서리 원형, 원형 용량 스타일과 단색·그라디언트 색조를 지원합니다. |
| [`CupertinoActivityIndicator`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoActivityIndicator.kt) | 작업·로딩 상태 표시. |
| [`CupertinoPullToRefreshBox`, `rememberCupertinoPullToRefreshState`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoPullToRefresh.kt) | 러버밴드 저항, 햅틱, 접근성 새로고침 액션을 갖춘 iOS 당겨서 새로고침. |
| [`CupertinoWebView`, `rememberCupertinoWebViewState`](hig-native/src/commonMain/kotlin/zone/ien/hig/CupertinoWebView.kt) | 관찰 가능한 상태, 내비게이터, 설정, URL 정규화를 갖춘 네이티브 웹 콘텐츠(`WKWebView` / Android `WebView`). 다른 대상에서는 대체 화면을 표시합니다. |
| [`CupertinoText`](hig/src/commonMain/kotlin/zone/ien/hig/Text.kt) | 테마를 반영하는 텍스트. [CupertinoIcon](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoIcon.kt)으로 벡터 또는 painter 렌더링. |

<a id="theme-api"></a>

### 테마 및 상태

- [`CupertinoTheme`](hig/src/commonMain/kotlin/zone/ien/hig/theme/CupertinoTheme.kt)은 `colorScheme`(기본값은 시스템 밝은/어두운 테마), `shapes = Shapes()`, `typography = Typography()`, 필수 컴포저블 `content`를 받습니다. `CupertinoTheme.colorScheme`, `.shapes`, `.typography`로 값을 읽습니다.
- [`ColorScheme`](hig-core/src/commonMain/kotlin/zone/ien/hig/theme/ColorScheme.kt), `lightColorScheme()`, `darkColorScheme()`, [`CupertinoColors`](hig-core/src/commonMain/kotlin/zone/ien/hig/theme/CupertinoColors.kt)는 의미 기반 색상과 시스템 색상을 제공합니다. `systemRed`를 비롯한 여러 `CupertinoColors` 값은 확장 속성이므로 해당 import가 필요합니다.
- [`SectionState`](hig/src/commonMain/kotlin/zone/ien/hig/section/SectionState.kt)는 `isCollapsed`, 변경 가능한 `canCollapse`, `toggle()`, `collapse()`, `expand()`를 제공합니다. `rememberSectionState(initiallyCollapsed = false, canCollapse = true)`로 섹션 상태를 저장합니다.
- [`rememberCupertinoSwipeBoxState`](hig/src/commonMain/kotlin/zone/ien/hig/swipebox/CupertinoSwipeBoxState.kt)는 `CupertinoSwipeBox`가 받는 `AnchoredDraggableState<SwipeBoxStates>`를 생성합니다.
- [`CupertinoIcons`](hig/src/commonMain/kotlin/zone/ien/hig/icons/CupertinoIcons.kt)는 벡터를 `Outlined`와 `Filled`로 구분합니다. 각 아이콘 확장 속성을 해당 패키지에서 import하세요. [아이콘 샘플](example/composeApp/src/commonMain/kotlin/icons/IconsScreen.kt)을 참고하세요.
- [`LocalCupertinoRightSystemArea`](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoBarPlacement.kt)는 기본값이 `true`인 `CompositionLocal<Boolean>`입니다. 현재 패널이 오른쪽 시스템 영역을 담당하는지 나타내며, `AdaptiveListDetailPane`이 목록·상세 레이아웃에 맞게 범위를 지정합니다. [Duo 통합 방법](#duo)을 참고하세요.
- [`Accessibility`](hig-core/src/commonMain/kotlin/zone/ien/hig/Accessibility.kt)는 `isHighContrastEnabled`, `isReduceTransparencyEnabled`를 제공하며, [`rememberCupertinoHapticFeedback()`](hig-core/src/commonMain/kotlin/zone/ien/hig/CupertinoHapticFeedback.kt)은 Compose `HapticFeedback`을 반환합니다.

<a id="glass-api"></a>

### Liquid 버튼 매개변수

`CupertinoLiquidButton`의 주요 매개변수입니다. 전체 시그니처는 [소스](hig/src/commonMain/kotlin/zone/ien/hig/CupertinoLiquidButton.kt)를 참고하세요.

| 매개변수 | 타입 | 기본값 | 동작 |
|----------|------|--------|------|
| `onClick` | `() -> Unit` | 필수 | 클릭 콜백 |
| `backdrop` | `Backdrop` | 필수 | 유리 렌더링에 사용할 배경 소스 |
| `modifier` | `Modifier` | `Modifier` | 레이아웃 및 시맨틱 modifier |
| `enabled` | `Boolean` | `true` | 상호작용 활성화 |
| `size` | `CupertinoButtonSize` | `Regular` | `Small`, `Regular`, `Large`, `ExtraLarge` |
| `colors` | `CupertinoLiquidButtonColors` | `glassButtonColors()` | 틴트·표면·콘텐츠 색상. 다른 프리셋으로 `glassProminentButtonColors()` 제공 |
| `shape` | `Shape` | `size.shape(CupertinoTheme.shapes)` | 크기와 테마에서 가져온 도형 |
| `contentPadding` | `PaddingValues` | `size.contentPadding` | 내부 여백 |
| `interactionSource` | `MutableInteractionSource` | 기억된 인스턴스 | 누름 및 상호작용 추적 |
| `isBackgroundAdaptive` | `Boolean` | `true` | 배경을 반영하는 외형 |
| `isInteractive` | `Boolean` | `true` | 상호작용에 따른 시각적 반응 |
| `content` | `@Composable RowScope.() -> Unit` | 필수 | 버튼 콘텐츠 |

<a id="compatibility"></a>

## 호환성 및 제한 사항

다음은 **현재 소스의 빌드 설정**이며, 모든 배포 버전이나 소비 프로젝트의 호환성을 보장하는 표는 아닙니다.

| 설정 | 저장소 값 / 요구 사항 |
|------|----------------------|
| Kotlin / Compose compiler plugin | `2.4.21` |
| Compose Multiplatform | `1.12.2` |
| Material 3 의존성 | `1.12.0-alpha03` |
| Android Gradle Plugin | `9.3.1` |
| Gradle wrapper | `9.7.0` |
| Android SDK | 최소 `29`, compile `37` 및 minor API `1`, 샘플 target `37` |
| JVM 바이트코드 | Android 및 Desktop 라이브러리 코드의 타깃은 `17` |
| 빌드 JDK | 배포 workflow는 JDK `21`, 테스트·데모 배포 workflow는 현재 JDK `17` 선언 |
| iOS 샘플 deployment target | Xcode 프로젝트에 `15.3` 설정. 모든 라이브러리 의존성의 최소 버전을 검증한 값은 아님 |
| Apple 빌드 | macOS 및 적절한 Xcode/SDK 설치 필요 |
| 브라우저 테스트 | Karma headless 실행에 Chrome/Chromium 필요 |

근거: [Version Catalog](gradle/libs.versions.toml), [Gradle 속성](gradle.properties), [타깃 설정](buildSrc/src/main/kotlin/multiplatform-module-convention.gradle.kts), [wrapper](gradle/wrapper/gradle-wrapper.properties), [workflows](.github/workflows/).

- iPhone Duo 힌지 관찰에는 iOS 27.1 이상과 해당 SDK API가 필요합니다. [Duo 미리보기](#duo-preview)는 시뮬레이터 촬영본이며, 이번 문서 작업에서 실기기 및 중간 힌지 각도의 동작은 테스트하지 않았습니다.
- 많은 Cupertino API에는 `@OptIn(ExperimentalCupertinoApi::class)`이, 적응형 API에는 `ExperimentalAdaptiveApi` 사용 선언이 필요합니다. 내비게이션 헬퍼에는 상위 라이브러리의 추가 실험적 API 사용 선언이 필요할 수 있습니다. Alpha 버전에서는 API가 변경될 수 있습니다.
- UIKit 기반 컴포넌트가 네이티브로 동작하는 플랫폼은 iOS입니다. OS가 제어하는 레이아웃·외형·설정 범위는 Compose 대체 구현과 다를 수 있습니다.
- Liquid Glass 외형은 렌더링 백엔드, OS, 접근성 설정, 캡처한 배경에 영향을 받습니다. 빌드 대상이 설정되어 있다는 사실만으로 모든 플랫폼의 광학 효과가 같다고 볼 수는 없습니다.
- `CupertinoTheme`은 접근성 관찰과 햅틱·테마 기본값을 설치하지만 Compose 컴포넌트를 UIKit 뷰로 바꾸지는 않습니다. iOS의 투명도 줄이기 설정은 유리 렌더링에 반영됩니다.
- 저장소는 WasmJS 타깃 때문에 Gradle configuration cache를 비활성화합니다. 기여 시 저작권 헤더를 유지하세요. 기능별 테스트는 각 모듈의 테스트 소스 세트에 있습니다.

<a id="flickering"></a>

### iOS 네이티브 UI 깜빡임

네이티브 메뉴, alert, action sheet를 표시하는 UIKit 전환 중 Compose 콘텐츠가 깜빡이면, 메인 스레드에서 첫 `ComposeUIViewController`를 만들기 전에 `enableCupertinoParallelRenderingFix()`를 호출하세요.

```kotlin
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import zone.ien.hig.enableCupertinoParallelRenderingFix

fun MainViewController(): UIViewController {
    check(enableCupertinoParallelRenderingFix()) { "Compose 병렬 렌더링 보정을 적용하지 못했습니다" }
    return ComposeUIViewController {
        App()
    }
}
```

여기서 `App()`은 애플리케이션의 루트 컴포저블입니다. 이 라이브러리의 iOS 전용 보정 함수는 Compose 1.12.2의 내부 `CMPMetalLayer` 버퍼 획득 경로에 프로세스 전체 보정을 한 번 적용하며, 반복 호출해도 중복 적용하지 않습니다. 합성 중이거나 GPU 작업이 끝나지 않은 버퍼를 재사용하지 않도록 하면서 병렬 렌더링과 기존 opaque·블러 설정은 유지합니다. 이 보정을 적용할 때는 `parallelRendering = false`를 설정할 필요가 없습니다.

사용 가능한 버퍼를 우선 재사용하고 부족할 때만 최대 5개까지 늘리므로 메모리 사용량이 증가할 수 있습니다. 모든 버퍼가 사용 중이면 렌더링 스레드가 기다리거나 프레임을 건너뛸 수 있으며, 메인 스레드에서는 버퍼가 빌 때까지 기다리지 않습니다. 모든 깜빡임의 해결이나 성능 영향이 없음을 보장하는 설정은 아닙니다.

메인 스레드가 아니거나 Compose 내부 구조가 호환되지 않으면 `false`를 반환하므로 위 예제의 `check`가 실패합니다. Compose 공식 API가 아닌 내부 구현 보정이므로 Compose 버전을 변경할 때 호환성과 보정 필요 여부를 다시 확인하세요. [iOS 예제](example/composeApp/src/iosMain/kotlin/main.ios.kt)를 참고하세요.

<a id="development"></a>

## 개발

저장소 루트에서 포함된 Gradle wrapper로 실행하세요. [샘플 빌드](example/composeApp/build.gradle.kts)는 로컬 모듈 의존성을 사용하므로 배포 바이너리가 아닌 현재 체크아웃을 사용합니다.

```shell
./gradlew :hig:tasks :hig-adaptive:tasks --all
./gradlew :hig:assemble :hig-core:assemble :hig-native:assemble :hig-adaptive:assemble :hig-icons-extended:assemble
./gradlew :hig:desktopTest :hig-adaptive:desktopTest
./gradlew :hig:jsBrowserTest :hig-adaptive:jsBrowserTest
```

샘플 실행 및 빌드 명령입니다.

```shell
./gradlew :example:composeApp:run
./gradlew :example:composeApp:wasmJsBrowserDevelopmentRun
./gradlew :example:composeApp:wasmJsBrowserDistribution
./gradlew :example:androidApp:assembleDebug
```

iOS는 macOS에서 [Xcode 프로젝트](example/iosApp/iosApp.xcodeproj/)를 열고 ARM64 시뮬레이터나 기기에서 `iosApp` 스킴을 실행하세요. Android는 Android Studio의 `example:androidApp` 실행 구성으로 실행할 수 있습니다. 샘플 빌드에는 Desktop용 DMG, MSI, DEB 패키징이 설정되어 있습니다.

소스 버전 아티팩트를 로컬에 설치하려면 다음 명령을 사용하세요.

```shell
./gradlew :hig-core:publishToMavenLocal :hig:publishToMavenLocal :hig-native:publishToMavenLocal :hig-adaptive:publishToMavenLocal :hig-icons-extended:publishToMavenLocal
```

전체 멀티플랫폼 빌드와 로컬 배포에는 해당 플랫폼 도구가 필요합니다. 위 빌드·테스트 명령은 사용 가능한 작업을 안내하며, 문서 정비 과정에서 모든 플랫폼을 실행했다는 의미는 아닙니다. [CI workflow](.github/workflows/gradle.yml)는 Desktop, JS, iOS 시뮬레이터, macOS Native 테스트 작업을 정의하며, [데모 배포](.github/workflows/deploy.yml)는 WasmJS 배포본을 빌드합니다.

주요 예제: [앱 테마](example/composeApp/src/commonMain/kotlin/App.kt), [Cupertino 컨트롤](example/composeApp/src/commonMain/kotlin/cupertino/CupertinoWidgetsScreen.kt), [적응형 컨트롤](example/composeApp/src/commonMain/kotlin/adaptive/AdaptiveWidgetsScreen.kt), [섹션](example/composeApp/src/commonMain/kotlin/sections/SectionsScreen.kt), [햅틱](example/composeApp/src/commonMain/kotlin/haptic/HapticFeedbackScreen.kt).

<a id="license"></a>

## 라이선스 및 출처

[Apache License 2.0](LICENSE.txt)에 따라 배포됩니다. 라이선스 파일의 JetBrains 저작권 고지도 유지합니다.

Compose-HIG는 [alexzhirkevich/compose-cupertino](https://github.com/alexzhirkevich/compose-cupertino)와 [slanos/compose-cupertino](https://github.com/slanos/compose-cupertino)를 포크하여 확장한 프로젝트입니다. 이 저장소에서는 Liquid Glass 컨트롤, 스와이프 액션, 확장된 컴포넌트 API, 적응형 내비게이션 작업을 추가했습니다. 렌더링에는 [Backdrop](https://github.com/Kyant0/AndroidLiquidGlass)을 사용하며, 의존성 좌표와 버전은 [Version Catalog](gradle/libs.versions.toml)에 기록되어 있습니다.

기존 프로젝트 저작권 고지를 유지합니다.

```text
Copyright (c) 2023-2024. Compose Cupertino project and open source contributors.
Copyright (c) 2025. Scott Lanoue.
Copyright (c) 2026. IENGROUND of IENLAB.
```
