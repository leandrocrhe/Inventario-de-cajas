package com.example.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.BoxEntity
import com.example.data.repository.CategoryStat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Data slice representing a segment in the Storage Pie / Donut Chart.
 */
data class PieChartSlice(
    val id: String,
    val label: String,
    val value: Float,
    val countText: String,
    val color: Color,
    val percentage: Float,
    val secondaryText: String? = null
)

/**
 * Breakdown dimension mode for the storage visualization.
 */
enum class StorageChartMode(val label: String) {
    BY_CATEGORY("Categorías"),
    BY_BOX_TYPE("Tipos de Caja"),
    BY_ZONE("Zonas")
}

val CHART_PALETTE = listOf(
    Color(0xFFFFB77C), // Warm Amber
    Color(0xFF38BDF8), // Sky Blue
    Color(0xFFA78BFA), // Lavender / Purple
    Color(0xFF34D399), // Emerald Green
    Color(0xFFF43F5E), // Coral Rose
    Color(0xFFFBBF24), // Gold Yellow
    Color(0xFF818CF8), // Indigo
    Color(0xFF2DD4BF), // Teal
    Color(0xFFFB923C), // Tangerine
    Color(0xFFA3E635)  // Lime
)

/**
 * Storage Pie Chart Card for SettingsScreen.
 * Displays interactive donut visualization representing storage distribution
 * by Category, Box Type (Root vs Sub-box), or Zone.
 */
@Composable
fun StoragePieChartCard(
    categoryStats: List<CategoryStat> = emptyList(),
    allBoxes: List<BoxEntity> = emptyList(),
    totalItemsCount: Int = 0,
    totalBoxesCount: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(StorageChartMode.BY_CATEGORY) }
    var selectedSliceIndex by remember { mutableStateOf<Int?>(null) }

    // Prepare slices based on active breakdown mode
    val slices = remember(selectedMode, categoryStats, allBoxes, totalItemsCount, totalBoxesCount) {
        when (selectedMode) {
            StorageChartMode.BY_CATEGORY -> buildCategorySlices(categoryStats)
            StorageChartMode.BY_BOX_TYPE -> buildBoxTypeSlices(allBoxes)
            StorageChartMode.BY_ZONE -> buildZoneSlices(allBoxes)
        }
    }

    // Reset selection if slice count changes or active tab changes
    remember(selectedMode) {
        selectedSliceIndex = null
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14110E)),
        border = BorderStroke(1.dp, Color(0xFF2C2622)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("storage_pie_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with title and icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF231B15))
                            .border(1.dp, Color(0xFF2C2622), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = Color(0xFFFFB77C),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Desglose de Almacenamiento",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Distribución por categoría o tipo de caja",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA89F95)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Selector Tabs (Categorías, Tipos de Caja, Zonas)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF191512))
                    .border(1.dp, Color(0xFF2C2622), RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StorageChartMode.values().forEach { mode ->
                    val isSelected = selectedMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF2B211A) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFFFFB77C).copy(alpha = 0.5f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedMode = mode
                                selectedSliceIndex = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFFFFB77C) else Color(0xFFA89F95)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (slices.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inbox,
                            contentDescription = null,
                            tint = Color(0xFF6B5E55),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Sin registros suficientes para graficar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA89F95),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Agrega cajas y objetos para ver el desglose circular",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B5E55),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Interactive Donut Chart
                val activeTotalCount = when (selectedMode) {
                    StorageChartMode.BY_CATEGORY -> totalItemsCount
                    else -> allBoxes.size
                }
                val totalLabel = when (selectedMode) {
                    StorageChartMode.BY_CATEGORY -> "Artículos"
                    else -> "Cajas"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DonutPieChartCanvas(
                        slices = slices,
                        selectedSliceIndex = selectedSliceIndex,
                        onSliceSelected = { index ->
                            selectedSliceIndex = if (selectedSliceIndex == index) null else index
                        },
                        modifier = Modifier
                            .size(190.dp)
                            .testTag("storage_pie_chart_canvas")
                    )

                    // Center Content
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .clickable { selectedSliceIndex = null }
                    ) {
                        val currentSlice = selectedSliceIndex?.let { slices.getOrNull(it) }
                        if (currentSlice != null) {
                            Text(
                                text = currentSlice.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = currentSlice.color,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f%%", currentSlice.percentage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = currentSlice.countText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA89F95),
                                fontSize = 10.sp
                            )
                        } else {
                            Text(
                                text = "TOTAL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFA89F95),
                                fontSize = 10.sp
                            )
                            Text(
                                text = "$activeTotalCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = totalLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFB77C),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Breakdown Legend / Slices List
                Text(
                    text = "Detalle del desglose (toca para filtrar):",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA89F95),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    slices.forEachIndexed { index, slice ->
                        val isSelected = selectedSliceIndex == index
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF231B15) else Color(0xFF191512),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) slice.color else Color(0xFF2C2622)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedSliceIndex = if (isSelected) null else index
                                }
                                .testTag("pie_legend_item_$index")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(slice.color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = slice.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFFECE0DA),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = slice.countText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA89F95)
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = slice.color.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, slice.color.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%.1f%%", slice.percentage),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = slice.color,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Donut Pie Chart Canvas drawing arcs with smooth angular gaps and click detection.
 */
@Composable
private fun DonutPieChartCanvas(
    slices: List<PieChartSlice>,
    selectedSliceIndex: Int?,
    onSliceSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "donut_chart_animation"
    )

    Canvas(
        modifier = modifier.pointerInput(slices) {
            detectTapGestures { offset ->
                val canvasSize = size
                val centerX = canvasSize.width / 2f
                val centerY = canvasSize.height / 2f

                val dx = offset.x - centerX
                val dy = offset.y - centerY
                val dist = sqrt(dx * dx + dy * dy)

                val outerRadius = minOf(canvasSize.width, canvasSize.height) / 2f
                val innerRadius = outerRadius - 38.dp.toPx()

                if (dist in innerRadius..outerRadius) {
                    var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (angleDeg < 0) angleDeg += 360f

                    // Start angle is at -90 deg (270 deg)
                    val normalizedAngle = (angleDeg - 270f + 360f) % 360f

                    var cumulative = 0f
                    for (i in slices.indices) {
                        val sweep = slices[i].percentage * 3.6f
                        if (normalizedAngle >= cumulative && normalizedAngle <= cumulative + sweep) {
                            onSliceSelected(i)
                            return@detectTapGestures
                        }
                        cumulative += sweep
                    }
                }
            }
        }
    ) {
        val diameter = size.minDimension
        val strokeWidth = 26.dp.toPx()
        val selectedStrokeWidth = 34.dp.toPx()
        val gapDegrees = if (slices.size > 1) 2.5f else 0f

        var currentStartAngle = -90f

        slices.forEachIndexed { index, slice ->
            val totalSweep = slice.percentage * 3.6f
            val animatedSweep = totalSweep * animationProgress
            val effectiveSweep = (animatedSweep - gapDegrees).coerceAtLeast(0.5f)

            val isSelected = selectedSliceIndex == index
            val currentStroke = if (isSelected) selectedStrokeWidth else strokeWidth
            val arcPadding = (selectedStrokeWidth - strokeWidth) / 2f + 4.dp.toPx()

            val arcSize = Size(
                width = diameter - arcPadding * 2 - strokeWidth,
                height = diameter - arcPadding * 2 - strokeWidth
            )
            val arcOffset = Offset(
                x = (size.width - arcSize.width) / 2f,
                y = (size.height - arcSize.height) / 2f
            )

            drawArc(
                color = if (isSelected) slice.color else slice.color.copy(alpha = 0.9f),
                startAngle = currentStartAngle + (gapDegrees / 2f),
                sweepAngle = effectiveSweep,
                useCenter = false,
                topLeft = arcOffset,
                size = arcSize,
                style = Stroke(
                    width = currentStroke,
                    cap = StrokeCap.Butt
                )
            )

            currentStartAngle += totalSweep
        }
    }
}

/**
 * Builds pie slices from CategoryStat list.
 */
private fun buildCategorySlices(categoryStats: List<CategoryStat>): List<PieChartSlice> {
    if (categoryStats.isEmpty()) return emptyList()

    val totalUnits = categoryStats.sumOf { it.unitsCount }.coerceAtLeast(1)
    return categoryStats.mapIndexed { index, stat ->
        val color = parseColorSafe(
            stat.category.colorHex,
            fallback = CHART_PALETTE[index % CHART_PALETTE.size]
        )
        val calculatedPercent = (stat.unitsCount.toFloat() / totalUnits.toFloat()) * 100f

        PieChartSlice(
            id = stat.category.id,
            label = stat.category.name,
            value = stat.unitsCount.toFloat(),
            countText = "${stat.unitsCount} uds (${stat.itemsCount} obj)",
            color = color,
            percentage = calculatedPercent
        )
    }
}

/**
 * Builds pie slices grouping boxes by Box Type (Cajas Principales vs Subcajas Organizadoras).
 */
private fun buildBoxTypeSlices(boxes: List<BoxEntity>): List<PieChartSlice> {
    if (boxes.isEmpty()) return emptyList()

    val rootBoxes = boxes.filter { it.parentBoxId == null }
    val subBoxes = boxes.filter { it.parentBoxId != null }
    val total = boxes.size.coerceAtLeast(1)

    val slices = mutableListOf<PieChartSlice>()

    if (rootBoxes.isNotEmpty()) {
        val pct = (rootBoxes.size.toFloat() / total.toFloat()) * 100f
        slices.add(
            PieChartSlice(
                id = "root_boxes",
                label = "Cajas Principales",
                value = rootBoxes.size.toFloat(),
                countText = "${rootBoxes.size} cajas",
                color = Color(0xFFFFB77C),
                percentage = pct
            )
        )
    }

    if (subBoxes.isNotEmpty()) {
        val pct = (subBoxes.size.toFloat() / total.toFloat()) * 100f
        slices.add(
            PieChartSlice(
                id = "sub_boxes",
                label = "Subcajas Interiores",
                value = subBoxes.size.toFloat(),
                countText = "${subBoxes.size} subcajas",
                color = Color(0xFF38BDF8),
                percentage = pct
            )
        )
    }

    return slices
}

/**
 * Builds pie slices grouping boxes by Zones / Estancias.
 */
private fun buildZoneSlices(boxes: List<BoxEntity>): List<PieChartSlice> {
    if (boxes.isEmpty()) return emptyList()

    val total = boxes.size.coerceAtLeast(1)
    val grouped = boxes.groupBy { it.zone?.takeIf { z -> z.isNotBlank() } ?: "Sin Zona" }

    return grouped.entries.sortedByDescending { it.value.size }.mapIndexed { index, entry ->
        val pct = (entry.value.size.toFloat() / total.toFloat()) * 100f
        val color = CHART_PALETTE[index % CHART_PALETTE.size]
        PieChartSlice(
            id = "zone_${entry.key}",
            label = entry.key,
            value = entry.value.size.toFloat(),
            countText = "${entry.value.size} cajas",
            color = color,
            percentage = pct
        )
    }
}
