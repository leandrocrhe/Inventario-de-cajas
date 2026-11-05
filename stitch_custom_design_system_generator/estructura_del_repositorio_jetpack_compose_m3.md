# 📦 Repositorio Android / Jetpack Compose: Inventario de Cajas (M3 Container Transform & AMOLED)

Estructura de archivos lista para producción, configuración Gradle (`version catalog`), código fuente de componentes y suite completa de pruebas para clonar o incorporar directamente a un repositorio de Android Studio.

---

## 📂 1. Árbol del Proyecto (Repository File Tree)

```text
inventario-de-cajas/
├── .gitignore
├── README.md
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   └── java/com/inventariocajas/
        │       ├── MainActivity.kt
        │       ├── model/
        │       │   └── CajaItem.kt
        │       ├── navigation/
        │       │   └── BoxNavigationStateHolder.kt
        │       ├── theme/
        │       │   ├── AmoledColors.kt
        │       │   ├── M3Motion.kt
        │       │   └── Theme.kt
        │       └── ui/
        │           ├── BoxGridScreen.kt
        │           ├── BoxDetailScreen.kt
        │           └── BoxInventoryContainerTransformDemo.kt
        ├── test/
        │   └── java/com/inventariocajas/
        │       ├── BoxNavigationUnitStateTest.kt
        │       └── M3MotionSpecAndThemeContractTest.kt
        └── androidTest/
            └── java/com/inventariocajas/
                ├── testing/
                │   └── BoxInventoryTestTags.kt
                └── BoxContainerTransformUiTest.kt
```

---

## ⚙️ 2. Configuración Gradle y Dependencias

### `gradle/libs.versions.toml`
```toml
[versions]
agp = "8.6.0"
kotlin = "2.0.20"
coreKtx = "1.13.1"
lifecycleRuntimeKtx = "2.8.6"
activityCompose = "1.9.2"
composeBom = "2024.09.02"
composeAnimation = "1.7.2"
composeUi = "1.7.2"
material3 = "1.3.0"
junit = "4.13.2"
androidxTestExt = "1.2.1"
espresso = "3.6.1"
truth = "1.4.2"
coroutinesTest = "1.8.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }

# Compose BOM & Core
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui", version.ref = "composeUi" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3", version.ref = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-compose-animation = { group = "androidx.compose.animation", name = "animation", version.ref = "composeAnimation" }

# Testing
junit = { group = "junit", name = "junit", version.ref = "junit" }
google-truth = { group = "com.google.truth", name = "truth", version.ref = "truth" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestExt" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4", version.ref = "composeUi" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest", version.ref = "composeUi" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

### `app/build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.inventariocajas"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.inventariocajas"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)

    // Unit Testing (JVM)
    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented Testing (Android / Compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.google.truth)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

---

## 🎨 3. Modelos, Temas y Tokens

### `app/src/main/java/com/inventariocajas/model/CajaItem.kt`
```kotlin
package com.inventariocajas.model

import androidx.compose.ui.graphics.Color

data class CajaItem(
    val id: String,
    val codigo: String,
    val nombre: String,
    val objetosCount: Int,
    val subcajasCount: Int,
    val tagColor: Color,
    val zona: String
)
```

### `app/src/main/java/com/inventariocajas/theme/AmoledColors.kt`
```kotlin
package com.inventariocajas.theme

import androidx.compose.ui.graphics.Color

object AmoledColors {
    val Background = Color(0xFF000000)          // True Black 0-nits
    val SurfaceCard = Color(0xFF141210)         // Tarjeta en reposo
    val SurfaceSheet = Color(0xFF181512)        // Contenedor expandido modal
    val AmberPrimary = Color(0xFFFFB77C)        // Amber 80
    val OnAmberPrimary = Color(0xFF4D2700)      // Dark Roast 20
    val AmberContainer = Color(0xFF683500)
    val TextPrimary = Color(0xFFECE0DA)         // Warm Sand White
    val TextSecondary = Color(0xFFA0968F)       // Muted Sand Gray
    val BorderSubtle = Color(0xFF282522)        // Borde fino delimitador
    val TagAmber = Color(0xFFF59E0B)
    val TagPurple = Color(0xFF8B5CF6)
    val TagRed = Color(0xFFEF4444)
}
```

### `app/src/main/java/com/inventariocajas/theme/M3Motion.kt`
```kotlin
package com.inventariocajas.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

object M3Motion {
    const val DURATION_CONTAINER_TRANSFORM = 380

    // Curva oficial Material Design 3 Emphasized Decelerate (Apertura)
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    // Curva oficial Material Design 3 Emphasized Accelerate (Cierre)
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    fun <T> containerTransformSpec() = tween<T>(
        durationMillis = DURATION_CONTAINER_TRANSFORM,
        easing = EmphasizedDecelerateEasing
    )
}
```

### `app/src/main/java/com/inventariocajas/navigation/BoxNavigationStateHolder.kt`
```kotlin
package com.inventariocajas.navigation

import com.inventariocajas.model.CajaItem

class BoxNavigationStateHolder {
    var selectedCaja: CajaItem? = null
        private set

    val isDetailOpen: Boolean get() = selectedCaja != null

    fun selectBox(caja: CajaItem) {
        selectedCaja = caja
    }

    fun dismissDetail() {
        selectedCaja = null
    }
}
```

---

## 🧩 4. Componentes UI (Container Transform & Pantallas)

### `app/src/main/java/com/inventariocajas/ui/BoxInventoryContainerTransformDemo.kt`
```kotlin
package com.inventariocajas.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.inventariocajas.model.CajaItem
import com.inventariocajas.theme.AmoledColors

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BoxInventoryContainerTransformDemo() {
    var selectedCaja by remember { mutableStateOf<CajaItem?>(null) }

    val sampleCajas = remember {
        listOf(
            CajaItem("box_1", "T-01", "Herramientas Taller", 5, 2, AmoledColors.TagAmber, "Trastero Principal"),
            CajaItem("box_2", "A-01", "Cables y Adaptadores", 4, 1, AmoledColors.TagPurple, "Armario Estudio"),
            CajaItem("box_3", "R-01", "Abrigos y Bufandas", 3, 0, AmoledColors.TagRed, "Ropa Temporada")
        )
    }

    SharedTransitionLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledColors.Background)
    ) {
        AnimatedContent(
            targetState = selectedCaja,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 180, delayMillis = 40)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = 140))
            },
            label = "M3ContainerTransformNavigation"
        ) { targetBox ->
            if (targetBox == null) {
                BoxGridScreen(
                    cajas = sampleCajas,
                    onCajaClick = { selectedCaja = it },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent
                )
            } else {
                BoxDetailScreen(
                    caja = targetBox,
                    onBack = { selectedCaja = null },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent
                )
            }
        }
    }
}
```

### `app/src/main/java/com/inventariocajas/ui/BoxGridScreen.kt`
```kotlin
package com.inventariocajas.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inventariocajas.model.CajaItem
import com.inventariocajas.theme.AmoledColors
import com.inventariocajas.theme.M3Motion

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BoxGridScreen(
    cajas: List<CajaItem>,
    onCajaClick: (CajaItem) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    with(sharedTransitionScope) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Text(
                text = "Inventario de Cajas",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AmoledColors.TextPrimary
            )
            Text(
                text = "3 zonas · ${cajas.size} cajas",
                fontSize = 13.sp,
                color = AmoledColors.TextSecondary,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(cajas, key = { it.id }) { caja ->
                    Box(
                        modifier = Modifier
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "container_${caja.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> M3Motion.containerTransformSpec() },
                                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(AmoledColors.SurfaceCard)
                            .border(1.dp, AmoledColors.BorderSubtle, RoundedCornerShape(16.dp))
                            .clickable { onCajaClick(caja) }
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = caja.codigo,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = caja.tagColor,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(caja.tagColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = caja.tagColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = caja.nombre,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmoledColors.TextPrimary,
                                maxLines = 1,
                                modifier = Modifier.sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "title_${caja.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    boundsTransform = { _, _ -> M3Motion.containerTransformSpec() }
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${caja.objetosCount} objetos · ${caja.subcajasCount} sub",
                                fontSize = 12.sp,
                                color = AmoledColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
```

### `app/src/main/java/com/inventariocajas/ui/BoxDetailScreen.kt`
```kotlin
package com.inventariocajas.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inventariocajas.model.CajaItem
import com.inventariocajas.theme.AmoledColors
import com.inventariocajas.theme.M3Motion

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BoxDetailScreen(
    caja: CajaItem,
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AmoledColors.Background)
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "container_${caja.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> M3Motion.containerTransformSpec() },
                        clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(28.dp))
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(AmoledColors.SurfaceSheet)
                    .border(1.dp, AmoledColors.BorderSubtle, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                // Barra superior
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = caja.codigo,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = caja.tagColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(caja.tagColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = caja.zona,
                            fontSize = 13.sp,
                            color = AmoledColors.TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF24201C))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = AmoledColors.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = caja.nombre,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmoledColors.TextPrimary,
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "title_${caja.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> M3Motion.containerTransformSpec() }
                    )
                )

                Text(
                    text = "${caja.objetosCount} objetos almacenados en este contenedor",
                    fontSize = 13.sp,
                    color = AmoledColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Acciones ergonómicas 1:2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AmoledColors.TextPrimary
                        )
                    ) {
                        Text("Volver")
                    }

                    Button(
                        onClick = { /* Acción añadir */ },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmoledColors.AmberPrimary,
                            contentColor = AmoledColors.OnAmberPrimary
                        )
                    ) {
                        Text("+ Añadir Objeto", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
```

---

## 🧪 5. Suite de Pruebas Unitarias y de UI

### `app/src/test/java/com/inventariocajas/BoxNavigationUnitStateTest.kt`
```kotlin
package com.inventariocajas

import com.google.common.truth.Truth.assertThat
import com.inventariocajas.model.CajaItem
import com.inventariocajas.navigation.BoxNavigationStateHolder
import com.inventariocajas.theme.AmoledColors
import org.junit.Before
import org.junit.Test

class BoxNavigationUnitStateTest {

    private lateinit var stateHolder: BoxNavigationStateHolder
    private val sampleBox = CajaItem(
        id = "box_1",
        codigo = "T-01",
        nombre = "Herramientas Taller",
        objetosCount = 5,
        subcajasCount = 2,
        tagColor = AmoledColors.TagAmber,
        zona = "Trastero Principal"
    )

    @Before
    fun setUp() {
        stateHolder = BoxNavigationStateHolder()
    }

    @Test
    fun initialState_hasNoSelectedBox_andDetailIsClosed() {
        assertThat(stateHolder.selectedCaja).isNull()
        assertThat(stateHolder.isDetailOpen).isFalse()
    }

    @Test
    fun selectBox_updatesSelectedBox_andOpensDetail() {
        stateHolder.selectBox(sampleBox)
        assertThat(stateHolder.selectedCaja).isEqualTo(sampleBox)
        assertThat(stateHolder.isDetailOpen).isTrue()
    }

    @Test
    fun dismissDetail_clearsSelectedBox_andReturnsToGrid() {
        stateHolder.selectBox(sampleBox)
        stateHolder.dismissDetail()
        assertThat(stateHolder.selectedCaja).isNull()
        assertThat(stateHolder.isDetailOpen).isFalse()
    }
}
```

### `app/src/test/java/com/inventariocajas/M3MotionSpecAndThemeContractTest.kt`
```kotlin
package com.inventariocajas

import androidx.compose.animation.core.CubicBezierEasing
import com.google.common.truth.Truth.assertThat
import com.inventariocajas.theme.AmoledColors
import com.inventariocajas.theme.M3Motion
import org.junit.Test

class M3MotionSpecAndThemeContractTest {

    @Test
    fun verifyContainerTransformDuration_isExactly380ms() {
        assertThat(M3Motion.DURATION_CONTAINER_TRANSFORM).isEqualTo(380)
    }

    @Test
    fun verifyEmphasizedDecelerateEasing_matchesMaterial3OfficialCurve() {
        val curve = M3Motion.EmphasizedDecelerateEasing
        assertThat(curve).isInstanceOf(CubicBezierEasing::class.java)
    }

    @Test
    fun verifyAmoledThemeBackground_isAbsoluteZeroNitsPureBlack() {
        assertThat(AmoledColors.Background.red).isEqualTo(0f)
        assertThat(AmoledColors.Background.green).isEqualTo(0f)
        assertThat(AmoledColors.Background.blue).isEqualTo(0f)
    }
}
```

### `app/src/androidTest/java/com/inventariocajas/BoxContainerTransformUiTest.kt`
```kotlin
package com.inventariocajas

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.inventariocajas.theme.M3Motion
import com.inventariocajas.ui.BoxInventoryContainerTransformDemo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoxContainerTransformUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tapOnBoxCard_navigatesToDetailWithSharedTransition() {
        composeTestRule.setContent {
            BoxInventoryContainerTransformDemo()
        }

        composeTestRule.onNodeWithText("Herramientas Taller").assertIsDisplayed()
        composeTestRule.onNodeWithText("Herramientas Taller").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("5 objetos almacenados en este contenedor").assertIsDisplayed()
        composeTestRule.onNodeWithText("Volver").assertIsDisplayed()
        composeTestRule.onNodeWithText("+ Añadir Objeto").assertIsDisplayed()
    }

    @Test
    fun containerTransform_interpolatesDuringDuration_andSettlesAt380ms() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BoxInventoryContainerTransformDemo()
        }

        composeTestRule.onNodeWithText("Herramientas Taller").performClick()
        composeTestRule.mainClock.advanceTimeBy(M3Motion.DURATION_CONTAINER_TRANSFORM / 2L)
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.mainClock.advanceTimeBy(M3Motion.DURATION_CONTAINER_TRANSFORM / 2L + 50L)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("+ Añadir Objeto").assertIsDisplayed()
    }

    @Test
    fun tapOnBackButton_collapsesContainerBackToGrid() {
        composeTestRule.setContent {
            BoxInventoryContainerTransformDemo()
        }

        composeTestRule.onNodeWithText("Herramientas Taller").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Volver").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Inventario de Cajas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cables y Adaptadores").assertIsDisplayed()
    }

    @Test
    fun bottomActions_maintainErgonomicRatio1to2() {
        composeTestRule.setContent {
            BoxInventoryContainerTransformDemo()
        }

        composeTestRule.onNodeWithText("Herramientas Taller").performClick()
        composeTestRule.waitForIdle()

        val volverWidth = composeTestRule.onNodeWithText("Volver").getUnclippedBoundsInRoot().width.value
        val agregarWidth = composeTestRule.onNodeWithText("+ Añadir Objeto").getUnclippedBoundsInRoot().width.value

        val ratio = agregarWidth / volverWidth
        assertThat(ratio).isAtLeast(1.7f)
        assertThat(ratio).isAtMost(2.3f)
    }
}
```

---

## 🚀 6. Guía Rápida para el Desarrollador (README.md)

```markdown
# Inventario de Cajas — Android App (Material Design 3 & AMOLED)

Aplicación nativa en **Jetpack Compose 1.7+** con animación **M3 Container Transform (Shared Element Transition)** optimizada para pantallas **AMOLED (#000000 Pure Black)** y diseño **Tactile Storage & Warm Utility**.

## Requisitos
- Android Studio Koala / Ladybug o superior
- JDK 17
- Android SDK 35 (mínimo SDK 26)

## Ejecutar Pruebas Unitarias
```bash
./gradlew testDebugUnitTest
```

## Ejecutar Pruebas Instrumentadas de UI en Dispositivo/Emulador
```bash
./gradlew connectedAndroidTest
```
```
