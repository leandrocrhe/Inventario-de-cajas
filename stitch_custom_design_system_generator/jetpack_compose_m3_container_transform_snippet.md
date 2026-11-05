# 🚀 Jetpack Compose — M3 Container Transform (Shared Element Transition)

Guía de implementación técnica y snippet completo en **Jetpack Compose** (Compose 1.7+ / Navigation Compose) para recrear la animación **Container Transform** de Material Design 3 entre la tarjeta de caja en la cuadrícula de inicio y el modal o pantalla de detalle de caja, adaptado a la paleta **Tactile Storage & Warm Utility** en modo **AMOLED Pure Black (`#000000`)**.

---

## 📦 Dependencias (Gradle / TOML)

```kotlin
// libs.versions.toml o build.gradle.kts
dependencies {
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.animation:animation:1.7.0")
    implementation("androidx.navigation:navigation-compose:2.8.0")
}
```

---

## 🎨 1. Tokens de Tema AMOLED & M3 Motion Spec

```kotlin
package com.inventariocajas.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Paleta AMOLED Puro (Tactile Storage & Warm Utility)
object AmoledColors {
    val Background = Color(0xFF000000)
    val SurfaceCard = Color(0xFF141210)
    val SurfaceSheet = Color(0xFF181512)
    val AmberPrimary = Color(0xFFFFB77C) // Amber 80
    val OnAmberPrimary = Color(0xFF4D2700) // Dark Roast 20
    val AmberContainer = Color(0xFF683500)
    val TextPrimary = Color(0xFFECE0DA) // Warm Sand White
    val TextSecondary = Color(0xFFA0968F)
    val BorderSubtle = Color(0xFF282522)
    val TagAmber = Color(0xFFF59E0B)
}

// Especificación de M3 Emphasized Motion
object M3Motion {
    const val DURATION_CONTAINER_TRANSFORM = 380
    
    // Curva oficial M3 Emphasized Decelerate
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    
    // Curva oficial M3 Emphasized Accelerate (Cierre)
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    fun <T> containerTransformSpec() = tween<T>(
        durationMillis = DURATION_CONTAINER_TRANSFORM,
        easing = EmphasizedDecelerateEasing
    )
}
```

---

## 🪄 2. Implementación con SharedTransitionLayout (Compose 1.7+)

Esta es la API oficial y moderna en Jetpack Compose para lograr el efecto **Material Container Transform** nativo entre composables.

```kotlin
package com.inventariocajas.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inventariocajas.ui.theme.AmoledColors
import com.inventariocajas.ui.theme.M3Motion

// Modelo de datos
data class CajaItem(
    val id: String,
    val codigo: String,
    val nombre: String,
    val objetosCount: Int,
    val subcajasCount: Int,
    val tagColor: Color,
    val zona: String
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BoxInventoryContainerTransformDemo() {
    var selectedCaja by remember { mutableStateOf<CajaItem?>(null) }

    val cajas = remember {
        listOf(
            CajaItem("box_1", "T-01", "Herramientas Taller", 5, 2, AmoledColors.TagAmber, "Trastero Principal"),
            CajaItem("box_2", "A-01", "Cables y Adaptadores", 4, 1, Color(0xFF8B5CF6), "Armario Estudio"),
            CajaItem("box_3", "R-01", "Abrigos y Bufandas", 3, 0, Color(0xFFEF4444), "Ropa Temporada")
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
                fadeIn(animationSpec = tween(durationMillis = 200, delayMillis = 60)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = 150))
            },
            label = "M3ContainerTransformNavigation"
        ) { targetBox ->
            if (targetBox == null) {
                // Estado 1: Origen (Cuadrícula de Cajas)
                BoxGridScreen(
                    cajas = cajas,
                    onCajaClick = { selectedCaja = it },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent
                )
            } else {
                // Estado 2: Destino (Ficha Detallada / Modal Expandido)
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

---

## 🗂️ 3. Origen: Tarjeta de Caja en la Cuadrícula (`BoxGridScreen`)

Aquí aplicamos `sharedBounds` con la curva **EmphasizedDecelerateEasing** y el radio de borde inicial (16dp).

```kotlin
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BoxGridScreen(
    cajas: List<CajaItem>,
    onCajaClick: (CajaItem) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope
) {
    with(sharedTransitionScope) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // Cabecera AMOLED
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
                    // Contenedor que experimenta la transformación elástica
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
                                // Código con Shared Element de texto
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

                            // Título de la caja con Shared Bounds de texto
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

---

## 📱 4. Destino: Detalle de Caja Expandido (`BoxDetailScreen`)

El contenedor morphing pasa de radio 16dp a 28dp, expandiéndose en pantalla completa o fondo de modal:

```kotlin
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BoxDetailScreen(
    caja: CajaItem,
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope
) {
    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AmoledColors.Background)
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            // Contenedor destino transformado (M3 Container Transform con 28dp radius)
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
                // Barra de herramientas superior del modal
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

                // Título transformado continuamente
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

                // Acciones ergonómicas inferiores con proporción 1:2
                Spacer(modifier = Modifier.weight(1f))

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
                        modifier = Modifier.weight(2f), // Proporción ergonómica 1:2
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

## ⚡ 5. Puntos Clave de la Arquitectura

1. **Curva `EmphasizedDecelerateEasing`**:
   - `CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)` proporciona la aceleración rápida inicial y desaceleración suave característica de Material Design 3.
2. **Transformación de Geometría y Radio (`OverlayClip`)**:
   - Al usar `clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))` y `RoundedCornerShape(28.dp)` en el destino, Compose interpola limpiamente el radio de los vértices durante la trayectoria de expansión.
3. **Persistencia de Jerarquía de Texto**:
   - Asignar una clave compartida (`key = "title_${caja.id}"`) al título evita saltos visuales (*layout pop-in*) al sincronizar posición y escala tipográfica.
4. **Optimización AMOLED 0-Nits**:
   - Fondo de escena `#000000` con superficies elevadas en `#141210` y `#181512` para mantener apagados los píxeles OLED circundantes durante toda la animación.
