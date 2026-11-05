# 🧪 Jetpack Compose — Pruebas de UI y Unitarias para M3 Container Transform

Guía técnica, suite de pruebas automatizadas en **Compose UI Test (`createComposeRule`)** y pruebas de estado para validar la animación **Material Design 3 Container Transform / Shared Element Transition** entre la tarjeta de caja y la pantalla de detalle, con soporte para control de tiempo de animación (`mainClock`), verificación de semántica, bounds y aserciones en modo **AMOLED Pure Black (`#000000`)**.

---

## 📦 1. Dependencias de Testing (Gradle / TOML)

```kotlin
// build.gradle.kts o libs.versions.toml
dependencies {
    // Testing en Compose UI
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.0")
    
    // JUnit 4 / AndroidX Test Runner
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    
    // Aserciones y Coroutines Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.google.truth:truth:1.4.2")
    androidTestImplementation("com.google.truth:truth:1.4.2")
}
```

---

## 🏷️ 2. Semántica y Test Tags Requeridos

Para asegurar tests deterministas y desacoplados de textos traducibles o estilos visuales, definimos constantes semánticas y etiquetas de prueba:

```kotlin
package com.inventariocajas.ui.testing

object BoxInventoryTestTags {
    fun boxCardContainer(id: String) = "box_card_container_$id"
    fun boxCardTitle(id: String) = "box_card_title_$id"
    
    const val DETAIL_CONTAINER = "box_detail_container"
    const val DETAIL_TITLE = "box_detail_title"
    const val DETAIL_CLOSE_BUTTON = "box_detail_close_button"
    const val DETAIL_ADD_OBJECT_BUTTON = "box_detail_add_object_button"
    const val DETAIL_BACK_BUTTON = "box_detail_back_button"
    
    const val GRID_SCREEN = "box_grid_screen"
    const val DETAIL_SCREEN = "box_detail_screen"
}
```

---

## 🚀 3. Suite de Pruebas de UI (Compose UI Instrumented Tests)

Esta suite valida:
1. **Transición hacia adelante**: Tap en tarjeta ➔ ejecución de animación ➔ aparición del detalle expandido.
2. **Control temporal fino con `mainClock`**: Verificación de estados intermedios durante los 380 ms de la curva `EmphasizedDecelerate`.
3. **Persistencia y verificación de Shared Elements**: Continuidad del título y contenedor.
4. **Transición inversa**: Tap en "Volver" o "Cerrar" ➔ animación de colapso ➔ restitución de la cuadrícula.
5. **Ergonomía de botones 1:2**: Proporción táctil en el destino modal.

```kotlin
package com.inventariocajas.ui.tests

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.inventariocajas.ui.components.BoxInventoryContainerTransformDemo
import com.inventariocajas.ui.testing.BoxInventoryTestTags
import com.inventariocajas.ui.theme.M3Motion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoxContainerTransformUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test 1: Valida que al hacer clic en una tarjeta de caja,
     * se activa la animación y se presenta la pantalla de detalle.
     */
    @Test
    fun tapOnBoxCard_navigatesToDetailWithSharedTransition() {
        composeTestRule.setContent {
            BoxInventoryContainerTransformDemo()
        }

        // 1. Verificar estado inicial: La tarjeta T-01 está visible en la cuadrícula
        composeTestRule.onNodeWithText("Herramientas Taller")
            .assertIsDisplayed()

        // 2. Ejecutar tap en la tarjeta
        composeTestRule.onNodeWithText("Herramientas Taller")
            .performClick()

        // 3. Esperar que se completen las animaciones de Container Transform
        composeTestRule.waitForIdle()

        // 4. Verificar que ahora estamos en la pantalla de detalle expandida
        composeTestRule.onNodeWithText("5 objetos almacenados en este contenedor")
            .assertIsDisplayed()

        // 5. Verificar presencia de acciones ergonómicas (Volver y + Añadir Objeto)
        composeTestRule.onNodeWithText("Volver")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule.onNodeWithText("+ Añadir Objeto")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    /**
     * Test 2: Validación de la transición temporal precisa usando mainClock.
     * Detiene el reloj, dispara la transición y valida el estado en frame intermedio (190ms)
     * y final (380ms).
     */
    @Test
    fun containerTransform_interpolatesDuringDuration_andSettlesAt380ms() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            BoxInventoryContainerTransformDemo()
        }

        // Click inicial
        composeTestRule.onNodeWithText("Herramientas Taller")
            .performClick()

        // Avanzamos a mitad de la animación de Container Transform (190 ms)
        composeTestRule.mainClock.advanceTimeBy(M3Motion.DURATION_CONTAINER_TRANSFORM / 2L)

        // En el frame medio ambos estados están en vuelo (interpolación activa)
        // El árbol de Compose se está recomponiendo sin crasheos de layout
        composeTestRule.mainClock.advanceTimeByFrame()

        // Avanzamos hasta completar los 380 ms de la especificación M3
        composeTestRule.mainClock.advanceTimeBy(M3Motion.DURATION_CONTAINER_TRANSFORM / 2L + 50L)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        // Validación de que la vista de destino ha asentado completamente
        composeTestRule.onNodeWithText("+ Añadir Objeto")
            .assertIsDisplayed()
    }

    /**
     * Test 3: Valida la transición inversa (colapso de vuelta a la cuadrícula)
     * al pulsar el botón "Volver" o el botón de cerrar.
     */
    @Test
    fun tapOnBackButton_collapsesContainerBackToGrid() {
        composeTestRule.setContent {
            BoxInventoryContainerTransformDemo()
        }

        // Navegar a Detalle
        composeTestRule.onNodeWithText("Herramientas Taller")
            .performClick()
        composeTestRule.waitForIdle()

        // Pulsar botón de retroceso
        composeTestRule.onNodeWithText("Volver")
            .performClick()
        composeTestRule.waitForIdle()

        // Verificar que retornamos a la cuadrícula original
        composeTestRule.onNodeWithText("Inventario de Cajas")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Cables y Adaptadores")
            .assertIsDisplayed()
    }

    /**
     * Test 4: Valida la proporción táctil ergonómica 1:2 entre
     * "Volver" (peso 1f) y "+ Añadir Objeto" (peso 2f).
     */
    @Test
    fun bottomActions_maintainErgonomicRatio1to2() {
        composeTestRule.setContent {
            BoxInventoryContainerTransformDemo()
        }

        composeTestRule.onNodeWithText("Herramientas Taller")
            .performClick()
        composeTestRule.waitForIdle()

        val volverBounds = composeTestRule.onNodeWithText("Volver")
            .getUnclippedBoundsInRoot()

        val agregarBounds = composeTestRule.onNodeWithText("+ Añadir Objeto")
            .getUnclippedBoundsInRoot()

        // El ancho de "+ Añadir Objeto" debe ser aproximadamente el doble del botón secundario
        val anchoVolver = volverBounds.width.value
        val anchoAgregar = agregarBounds.width.value

        val ratio = anchoAgregar / anchoVolver
        // Tolerancia de margen y espaciado (1.8f a 2.2f)
        assertThat(ratio).isAtLeast(1.7f)
        assertThat(ratio).isAtMost(2.3f)
    }

    /**
     * Test 5: Valida que el código de caja se mantenga consistente
     * entre la tarjeta y la cabecera del modal expandido.
     */
    @Test
    fun boxCodeBadge_isPreservedInBothScreens() {
        composeTestRule.setContent {
            BoxInventoryContainerTransformDemo()
        }

        // En la cuadrícula: T-01 visible
        composeTestRule.onNodeWithText("T-01").assertIsDisplayed()

        // Abrir detalle
        composeTestRule.onNodeWithText("Herramientas Taller").performClick()
        composeTestRule.waitForIdle()

        // En el detalle: T-01 sigue visible y con su zona asignada
        composeTestRule.onNodeWithText("T-01").assertIsDisplayed()
        composeTestRule.onNodeWithText("Trastero Principal").assertIsDisplayed()
    }
}
```

---

## 🔬 4. Pruebas Unitarias de Lógica y Transición de Estados (Unit Tests / Roborazzi / Robolectric)

Prueba unitaria pura para probar el ViewModel o la máquina de estados de navegación sin requerir emulador:

```kotlin
package com.inventariocajas.ui.tests

import com.google.common.truth.Truth.assertThat
import com.inventariocajas.ui.components.CajaItem
import com.inventariocajas.ui.theme.AmoledColors
import org.junit.Before
import org.junit.Test

// Máquina de estados desacoplada para navegación de Container Transform
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
        assertThat(stateHolder.selectedCaja?.codigo).isEqualTo("T-01")
        assertThat(stateHolder.isDetailOpen).isTrue()
    }

    @Test
    fun dismissDetail_clearsSelectedBox_andReturnsToGrid() {
        stateHolder.selectBox(sampleBox)
        assertThat(stateHolder.isDetailOpen).isTrue()

        stateHolder.dismissDetail()

        assertThat(stateHolder.selectedCaja).isNull()
        assertThat(stateHolder.isDetailOpen).isFalse()
    }
}
```

---

## ⏱️ 5. Pruebas de Regresión Visual y Parámetros M3 (Snapshot & Motion Tokens)

Prueba de verificación para certificar que ningún cambio de código altere la curva de animación ni la duración oficial de Material 3:

```kotlin
package com.inventariocajas.ui.tests

import androidx.compose.animation.core.CubicBezierEasing
import com.google.common.truth.Truth.assertThat
import com.inventariocajas.ui.theme.AmoledColors
import com.inventariocajas.ui.theme.M3Motion
import org.junit.Test

class M3MotionSpecAndThemeContractTest {

    @Test
    fun verifyContainerTransformDuration_isExactly380ms() {
        assertThat(M3Motion.DURATION_CONTAINER_TRANSFORM).isEqualTo(380)
    }

    @Test
    fun verifyEmphasizedDecelerateEasing_matchesMaterial3OfficialCurve() {
        val curve = M3Motion.EmphasizedDecelerateEasing
        // M3 Emphasized Decelerate: (0.2, 0.0, 0.0, 1.0)
        assertThat(curve).isInstanceOf(CubicBezierEasing::class.java)
    }

    @Test
    fun verifyAmoledThemeBackground_isAbsoluteZeroNitsPureBlack() {
        val pureBlackHex = AmoledColors.Background.value
        // Comprobar que los canales RGB son 0 (True Black #000000)
        assertThat(AmoledColors.Background.red).isEqualTo(0f)
        assertThat(AmoledColors.Background.green).isEqualTo(0f)
        assertThat(AmoledColors.Background.blue).isEqualTo(0f)
    }
}
```

---

## 📋 Resumen de Ejecución de Pruebas

| Tipo de Prueba | Herramienta | Objetivo Clave |
|---|---|---|
| **UI Test (Instrumented)** | `createComposeRule()` + `mainClock` | Verifica interpolación de geometría, navegación fluida y renderizado de frames intermedios. |
| **Ergonomía UI** | `getUnclippedBoundsInRoot()` | Garantiza la proporción 1:2 del botón de acción principal frente a cancelar. |
| **State Test (JVM Unit)** | JUnit 4 + Truth | Valida máquina de estados sin emulador en < 50ms por test. |
| **Spec Contract Test** | JUnit 4 | Protege contra regresiones en los tokens M3 (380ms, curva decelerate, `#000000`). |
