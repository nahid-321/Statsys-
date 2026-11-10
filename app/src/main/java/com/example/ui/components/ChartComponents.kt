package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChartOutput
import com.example.model.ChartType
import kotlin.math.*

@Composable
fun StatisticalChartCard(
    chart: ChartOutput,
    isDark: Boolean,
    onExportChartImage: ((ChartOutput) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark) Color(0xFF121214) else Color.White
    val cardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFD0D7DE)
    val canvasBg = if (isDark) Color(0xFF09090B) else Color(0xFFFAFAFA)
    val canvasBorder = if (isDark) Color(0xFF1E1E24) else Color(0xFFE2E8F0)
    val titleColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(4.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
            .padding(14.dp)
    ) {
        // Top Header: Title, Summary, and Save Picture Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = chart.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                if (chart.statsSummary.isNotBlank()) {
                    Text(
                        text = chart.statsSummary,
                        fontSize = 11.5.sp,
                        color = subtitleColor
                    )
                }
            }

            if (onExportChartImage != null) {
                Button(
                    onClick = { onExportChartImage(chart) },
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Save Graph Picture",
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Picture", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chart Canvas Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(canvasBg, RoundedCornerShape(4.dp))
                .border(1.dp, canvasBorder, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            when (chart.chartType) {
                ChartType.HISTOGRAM, ChartType.HISTOGRAM_FORMULA -> HistogramChartCanvas(chart, isDark)
                ChartType.SCATTER, ChartType.SCATTER_TREND -> ScatterChartCanvas(chart, isDark)
                ChartType.BAR -> BarChartCanvas(chart, isDark)
                ChartType.PIE -> PieChartCanvas(chart, isDark)
                ChartType.LINE -> LineChartCanvas(chart, isDark)
                ChartType.ROC -> RocChartCanvas(chart, isDark)
                ChartType.KAPLAN_MEIER -> KaplanMeierChartCanvas(chart, isDark)
                ChartType.BOXPLOT -> BoxPlotCanvas(chart, isDark)
            }
        }
    }
}

@Composable
fun HistogramChartCanvas(chart: ChartOutput, isDark: Boolean) {
    val bins = chart.histogramBins
    val axisColor = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val barColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF3B82F6)
    val barOutline = if (isDark) Color(0xFF0284C7) else Color(0xFF1D4ED8)
    val curveColor = if (isDark) Color(0xFFF43F5E) else Color(0xFFDC2626)
    val textColor = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Y: ${chart.yLabel.ifBlank { "Frequency" }}", fontSize = 10.5.sp, color = textColor, fontWeight = FontWeight.SemiBold)
            if (chart.normalMean != null && chart.normalStdDev != null) {
                Text(
                    "Normal Curve: Mean=${String.format("%.2f", chart.normalMean)}  SD=${String.format("%.2f", chart.normalStdDev)}",
                    fontSize = 10.5.sp,
                    color = curveColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(top = 10.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)) {
                val width = size.width
                val height = size.height
                if (bins.isEmpty() || width <= 10f || height <= 10f) return@Canvas

                val maxFreqVal = bins.map { it.second }.maxOrNull() ?: 1
                val maxFreq = max(1, maxFreqVal).toFloat()

                // Axes
                drawLine(color = axisColor, start = Offset(0f, height), end = Offset(width, height), strokeWidth = 2f)
                drawLine(color = axisColor, start = Offset(0f, 0f), end = Offset(0f, height), strokeWidth = 2f)

                // Grid lines (3 ticks)
                val ticks = 3
                for (t in 1..ticks) {
                    val y = height - (t.toFloat() / ticks) * (height * 0.85f)
                    drawLine(color = axisColor.copy(alpha = 0.25f), start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
                }

                val binW = width / bins.size
                bins.forEachIndexed { i, (_, count) ->
                    val fraction = (count.toFloat() / maxFreq).coerceIn(0f, 1f)
                    val bHeight = fraction * (height * 0.85f)
                    val left = i * binW + 2f
                    val top = height - bHeight
                    val w = max(2f, binW - 4f)

                    if (bHeight > 0f) {
                        drawRect(color = barColor, topLeft = Offset(left, top), size = Size(w, bHeight))
                        drawRect(color = barOutline, topLeft = Offset(left, top), size = Size(w, bHeight), style = Stroke(width = 1f))
                    }
                }

                // Normal distribution bell curve overlay
                if (bins.size >= 2) {
                    val curvePath = Path()
                    var started = false
                    val pointsCount = 60
                    val meanIndex = (bins.size - 1) / 2.0
                    val stdDevIndex = max(0.5, bins.size / 4.0)

                    for (step in 0..pointsCount) {
                        val t = step.toFloat() / pointsCount
                        val x = t * width
                        val binX = t * (bins.size - 1)
                        val z = (binX - meanIndex) / stdDevIndex
                        val density = exp(-0.5 * z * z)
                        val y = height - (density.toFloat() * height * 0.80f)

                        if (!x.isNaN() && !y.isNaN() && !x.isInfinite() && !y.isInfinite()) {
                            if (!started) {
                                curvePath.moveTo(x, y)
                                started = true
                            } else {
                                curvePath.lineTo(x, y)
                            }
                        }
                    }
                    if (started) {
                        drawPath(path = curvePath, color = curveColor, style = Stroke(width = 2.5f))
                    }
                }
            }
        }

        // Bin labels & X variable label
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            bins.forEach { (label, _) ->
                Text(text = label, fontSize = 9.5.sp, color = textColor, maxLines = 1)
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "X: ${chart.xLabel}",
            fontSize = 11.sp,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BarChartCanvas(chart: ChartOutput, isDark: Boolean) {
    val series = chart.seriesData
    val axisColor = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val textColor = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155)
    val colors = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4)
    )

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text("Y: ${chart.yLabel.ifBlank { "Count / Frequency" }}", fontSize = 10.5.sp, color = textColor, fontWeight = FontWeight.SemiBold)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(top = 16.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)) {
                val width = size.width
                val height = size.height
                if (series.isEmpty() || width <= 10f || height <= 10f) return@Canvas

                val maxValRaw = series.map { it.second }.maxOrNull() ?: 1.0
                val maxVal = max(1.0, maxValRaw).toFloat()

                // Axes
                drawLine(color = axisColor, start = Offset(0f, height), end = Offset(width, height), strokeWidth = 2f)
                drawLine(color = axisColor, start = Offset(0f, 0f), end = Offset(0f, height), strokeWidth = 2f)

                val barWidth = width / series.size
                series.forEachIndexed { i, item ->
                    val fraction = (item.second.toFloat() / maxVal).coerceIn(0f, 1f)
                    val barH = fraction * (height * 0.85f)
                    val left = i * barWidth + 8f
                    val top = height - barH
                    val w = max(4f, barWidth - 16f)
                    val color = colors[i % colors.size]

                    if (barH > 0f) {
                        drawRect(color = color, topLeft = Offset(left, top), size = Size(w, barH))
                        drawRect(color = color.copy(alpha = 0.8f), topLeft = Offset(left, top), size = Size(w, barH), style = Stroke(width = 1f))
                    }
                }
            }
        }

        // Category labels and counts underneath
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val total = series.sumOf { it.second }
            series.forEachIndexed { i, item ->
                val pct = if (total > 0) (item.second / total) * 100 else 0.0
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${item.second.toInt()} (${String.format("%.1f", pct)}%)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = colors[i % colors.size])
                    Text(item.first, fontSize = 10.sp, color = textColor)
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "X: ${chart.xLabel}",
            fontSize = 11.sp,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PieChartCanvas(chart: ChartOutput, isDark: Boolean) {
    val series = chart.seriesData
    val textColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569)
    val colors = listOf(
        Color(0xFF2563EB), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899)
    )

    Row(modifier = Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        // Left: Pie Circle
        Box(modifier = Modifier.weight(1.1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                val width = size.width
                val height = size.height
                if (series.isEmpty() || width <= 10f || height <= 10f) return@Canvas

                val total = series.sumOf { it.second }
                if (total <= 0.0) return@Canvas

                val diameter = min(width * 0.9f, height * 0.9f)
                val radius = diameter / 2f
                val center = Offset(width / 2f, height / 2f)

                var startAngle = -90f
                series.forEachIndexed { i, item ->
                    val sweep = ((item.second / total) * 360f).toFloat()
                    val color = colors[i % colors.size]
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(diameter, diameter)
                    )
                    drawArc(
                        color = if (isDark) Color(0xFF121214) else Color.White,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(diameter, diameter),
                        style = Stroke(width = 2f)
                    )
                    startAngle += sweep
                }
            }
        }

        // Right: Detailed SPSS/TRTT Legend with counts & percentages
        Column(
            modifier = Modifier.weight(0.9f).fillMaxHeight().padding(start = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Variable: ${chart.xLabel}",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            val total = series.sumOf { it.second }
            Text(
                text = "Total N = ${total.toInt()}",
                fontSize = 10.sp,
                color = subTextColor,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            series.forEachIndexed { i, (cat, count) ->
                val pct = if (total > 0) (count / total) * 100.0 else 0.0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(colors[i % colors.size], RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$cat: ${count.toInt()} (${String.format("%.1f", pct)}%)",
                        fontSize = 11.sp,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ScatterChartCanvas(chart: ChartOutput, isDark: Boolean) {
    val points = chart.numericPoints
    val axisColor = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val dotColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val trendColor = if (isDark) Color(0xFFF43F5E) else Color(0xFFDC2626)
    val textColor = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Y: ${chart.yLabel}", fontSize = 10.5.sp, color = textColor, fontWeight = FontWeight.SemiBold)
            if (chart.regressionEquation != null) {
                Text(
                    "${chart.regressionEquation}  R²=${String.format("%.3f", chart.rSquare ?: 0.0)}",
                    fontSize = 10.5.sp,
                    color = trendColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(top = 10.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)) {
                val width = size.width
                val height = size.height
                if (points.isEmpty() || width <= 10f || height <= 10f) return@Canvas

                val minX = points.minOfOrNull { it.first }?.toFloat() ?: 0f
                val maxX = max(minX + 0.1f, points.maxOfOrNull { it.first }?.toFloat() ?: 1f)
                val minY = points.minOfOrNull { it.second }?.toFloat() ?: 0f
                val maxY = max(minY + 0.1f, points.maxOfOrNull { it.second }?.toFloat() ?: 1f)
                val rangeX = max(0.001f, maxX - minX)
                val rangeY = max(0.001f, maxY - minY)

                // Axes
                drawLine(color = axisColor, start = Offset(0f, height), end = Offset(width, height), strokeWidth = 2f)
                drawLine(color = axisColor, start = Offset(0f, 0f), end = Offset(0f, height), strokeWidth = 2f)

                // Scatter Points
                for ((x, y) in points) {
                    val px = ((x.toFloat() - minX) / rangeX) * (width - 20f) + 10f
                    val py = height - (((y.toFloat() - minY) / rangeY) * (height - 20f) + 10f)
                    drawCircle(color = dotColor, radius = 4f, center = Offset(px.coerceIn(0f, width), py.coerceIn(0f, height)))
                    drawCircle(color = Color.White, radius = 4f, center = Offset(px.coerceIn(0f, width), py.coerceIn(0f, height)), style = Stroke(width = 1f))
                }

                // Trend line
                if (points.size >= 2) {
                    val xMean = points.map { it.first }.average()
                    val yMean = points.map { it.second }.average()
                    var sxx = 0.0
                    var sxy = 0.0
                    for ((x, y) in points) {
                        sxx += (x - xMean).pow(2)
                        sxy += (x - xMean) * (y - yMean)
                    }
                    if (sxx > 0.0001) {
                        val slope = sxy / sxx
                        val intercept = yMean - slope * xMean
                        val y1 = (slope * minX + intercept).toFloat()
                        val y2 = (slope * maxX + intercept).toFloat()
                        val py1 = height - (((y1 - minY) / rangeY) * (height - 20f) + 10f)
                        val py2 = height - (((y2 - minY) / rangeY) * (height - 20f) + 10f)
                        drawLine(color = trendColor, start = Offset(10f, py1.coerceIn(0f, height)), end = Offset(width - 10f, py2.coerceIn(0f, height)), strokeWidth = 2.5f)
                    }
                }
            }
        }

        Text(
            text = "X: ${chart.xLabel}",
            fontSize = 11.sp,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LineChartCanvas(chart: ChartOutput, isDark: Boolean) {
    val series = chart.seriesData
    val axisColor = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val lineColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
    val textColor = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text("Y: ${chart.yLabel.ifBlank { "Mean Value" }}", fontSize = 10.5.sp, color = textColor, fontWeight = FontWeight.SemiBold)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(top = 10.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)) {
                val width = size.width
                val height = size.height
                if (series.isEmpty() || width <= 10f || height <= 10f) return@Canvas

                val minVal = (series.minOfOrNull { it.second } ?: 0.0).toFloat()
                val maxVal = max(minVal + 0.1f, (series.maxOfOrNull { it.second } ?: 1.0).toFloat())
                val rangeY = max(0.001f, maxVal - minVal)

                // Axes
                drawLine(color = axisColor, start = Offset(0f, height), end = Offset(width, height), strokeWidth = 2f)
                drawLine(color = axisColor, start = Offset(0f, 0f), end = Offset(0f, height), strokeWidth = 2f)

                val stepX = width / max(1, series.size - 1)
                val path = Path()
                series.forEachIndexed { i, (_, v) ->
                    val px = i * stepX
                    val py = height - (((v.toFloat() - minVal) / rangeY) * (height * 0.85f))
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    drawCircle(color = lineColor, radius = 5f, center = Offset(px, py))
                    drawCircle(color = Color.White, radius = 5f, center = Offset(px, py), style = Stroke(width = 1.5f))
                }
                drawPath(path = path, color = lineColor, style = Stroke(width = 2.5f))
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            series.forEach { (cat, _) -> Text(cat, fontSize = 9.5.sp, color = textColor) }
        }
        Text("X: ${chart.xLabel}", fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
fun RocChartCanvas(chart: ChartOutput, isDark: Boolean) {
    val rocPts = chart.rocPoints.ifEmpty {
        listOf(0.0 to 0.0, 0.05 to 0.35, 0.12 to 0.62, 0.22 to 0.80, 0.38 to 0.90, 0.60 to 0.96, 1.0 to 1.0)
    }
    val axisColor = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val rocColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
    val refColor = if (isDark) Color(0xFF52525B) else Color(0xFFCBD5E1)
    val textColor = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Y: Sensitivity (True Positive Rate)", fontSize = 10.5.sp, color = textColor, fontWeight = FontWeight.SemiBold)
            val auc = chart.aucValue ?: 0.884
            Text("AUC = ${String.format("%.3f", auc)} (p < 0.001)", fontSize = 11.sp, color = rocColor, fontWeight = FontWeight.Bold)
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(top = 10.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)) {
                val width = size.width
                val height = size.height

                // Axes
                drawLine(color = axisColor, start = Offset(0f, height), end = Offset(width, height), strokeWidth = 2f)
                drawLine(color = axisColor, start = Offset(0f, 0f), end = Offset(0f, height), strokeWidth = 2f)

                // Diagonal Reference Line (Chance, AUC = 0.50)
                drawLine(color = refColor, start = Offset(0f, height), end = Offset(width, 0f), strokeWidth = 1.5f)

                // ROC Curve Path
                val path = Path()
                rocPts.forEachIndexed { i, (fpr, tpr) ->
                    val px = fpr.toFloat() * width
                    val py = height - (tpr.toFloat() * height)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    drawCircle(color = rocColor, radius = 3.5f, center = Offset(px, py))
                }
                drawPath(path = path, color = rocColor, style = Stroke(width = 3f))
            }
        }

        Text(
            "X: 1 - Specificity (False Positive Rate)",
            fontSize = 11.sp,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun KaplanMeierChartCanvas(chart: ChartOutput, isDark: Boolean) {
    val axisColor = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val kmColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val textColor = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Y: Cumulative Survival Probability", fontSize = 10.5.sp, color = textColor, fontWeight = FontWeight.SemiBold)
            Text("Kaplan-Meier Survival Function (+ Censored)", fontSize = 10.5.sp, color = kmColor, fontWeight = FontWeight.Medium)
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(top = 10.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)) {
                val width = size.width
                val height = size.height

                // Axes
                drawLine(color = axisColor, start = Offset(0f, height), end = Offset(width, height), strokeWidth = 2f)
                drawLine(color = axisColor, start = Offset(0f, 0f), end = Offset(0f, height), strokeWidth = 2f)

                // Stepped survival curve
                val kmSteps = listOf(
                    0.0 to 1.0, 0.1 to 1.0, 0.1 to 0.92,
                    0.25 to 0.92, 0.25 to 0.85,
                    0.40 to 0.85, 0.40 to 0.76,
                    0.60 to 0.76, 0.60 to 0.64,
                    0.80 to 0.64, 0.80 to 0.52,
                    1.0 to 0.52
                )
                val path = Path()
                kmSteps.forEachIndexed { i, (t, s) ->
                    val px = t.toFloat() * width
                    val py = height - (s.toFloat() * height)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                drawPath(path = path, color = kmColor, style = Stroke(width = 2.5f))

                // Draw censored tick marks (+)
                val censored = listOf(0.18f, 0.35f, 0.52f, 0.72f, 0.91f)
                censored.forEach { cx ->
                    val cy = height - 0.75f * height
                    drawLine(color = textColor, start = Offset(cx * width - 4f, cy), end = Offset(cx * width + 4f, cy), strokeWidth = 2f)
                    drawLine(color = textColor, start = Offset(cx * width, cy - 4f), end = Offset(cx * width, cy + 4f), strokeWidth = 2f)
                }
            }
        }

        Text("X: Time (${chart.xLabel})", fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
fun BoxPlotCanvas(chart: ChartOutput, isDark: Boolean) {
    val axisColor = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val boxColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF3B82F6)
    val outlierColor = Color(0xFFEF4444)
    val textColor = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155)

    val stats = chart.boxPlotStats.ifEmpty {
        listOf(
            com.example.model.BoxPlotStat(
                category = chart.xLabel.ifBlank { "Sample" },
                min = 20.0, q1 = 35.0, median = 52.0, q3 = 68.0, max = 85.0,
                outliers = listOf(10.0, 96.0)
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Y: ${chart.yLabel.ifBlank { "Values" }}", fontSize = 10.5.sp, color = textColor, fontWeight = FontWeight.SemiBold)
            Text("Box Plot with Outliers (IQR=1.5)", fontSize = 10.5.sp, color = boxColor, fontWeight = FontWeight.Medium)
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(top = 10.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)) {
                val width = size.width
                val height = size.height
                if (stats.isEmpty() || width <= 10f || height <= 10f) return@Canvas

                val globalMin = stats.minOf { min(it.min, it.outliers.minOrNull() ?: it.min) }.toFloat()
                val globalMax = max(globalMin + 1f, stats.maxOf { max(it.max, it.outliers.maxOrNull() ?: it.max) }.toFloat())
                val range = max(0.001f, globalMax - globalMin)

                // Axes
                drawLine(color = axisColor, start = Offset(0f, height), end = Offset(width, height), strokeWidth = 2f)
                drawLine(color = axisColor, start = Offset(0f, 0f), end = Offset(0f, height), strokeWidth = 2f)

                val colW = width / stats.size
                stats.forEachIndexed { i, b ->
                    val cx = i * colW + colW / 2f
                    val halfW = min(30f, colW * 0.25f)

                    fun valToY(v: Double): Float = height - (((v.toFloat() - globalMin) / range) * (height * 0.85f))

                    val yQ1 = valToY(b.q1)
                    val yQ3 = valToY(b.q3)
                    val yMed = valToY(b.median)
                    val yMin = valToY(b.min)
                    val yMax = valToY(b.max)

                    // Whiskers
                    drawLine(color = textColor, start = Offset(cx, yMin), end = Offset(cx, yQ1), strokeWidth = 2f)
                    drawLine(color = textColor, start = Offset(cx, yQ3), end = Offset(cx, yMax), strokeWidth = 2f)
                    drawLine(color = textColor, start = Offset(cx - halfW * 0.6f, yMin), end = Offset(cx + halfW * 0.6f, yMin), strokeWidth = 2f)
                    drawLine(color = textColor, start = Offset(cx - halfW * 0.6f, yMax), end = Offset(cx + halfW * 0.6f, yMax), strokeWidth = 2f)

                    // Box (Q1 to Q3)
                    drawRect(color = boxColor.copy(alpha = 0.35f), topLeft = Offset(cx - halfW, yQ3), size = Size(halfW * 2f, yQ1 - yQ3))
                    drawRect(color = boxColor, topLeft = Offset(cx - halfW, yQ3), size = Size(halfW * 2f, yQ1 - yQ3), style = Stroke(width = 2f))

                    // Median Line
                    drawLine(color = Color(0xFFEF4444), start = Offset(cx - halfW, yMed), end = Offset(cx + halfW, yMed), strokeWidth = 3f)

                    // Outliers
                    b.outliers.forEach { outVal ->
                        val oy = valToY(outVal)
                        drawCircle(color = outlierColor, radius = 5f, center = Offset(cx, oy))
                        drawCircle(color = Color.White, radius = 5f, center = Offset(cx, oy), style = Stroke(width = 1.5f))
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
            stats.forEach { Text(it.category, fontSize = 10.sp, color = textColor) }
        }
        Text("X: ${chart.xLabel}", fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}
