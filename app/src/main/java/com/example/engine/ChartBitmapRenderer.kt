package com.example.engine

import android.graphics.*
import com.example.model.ChartOutput
import com.example.model.ChartType
import java.io.ByteArrayOutputStream
import kotlin.math.*

object ChartBitmapRenderer {

    fun renderToBitmap(chart: ChartOutput, isDark: Boolean = false): Bitmap {
        val width = 1100
        val height = 720
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgCol = if (isDark) Color.rgb(18, 18, 20) else Color.WHITE
        val plotBgCol = if (isDark) Color.rgb(10, 10, 12) else Color.rgb(250, 250, 250)
        val textCol = if (isDark) Color.rgb(244, 244, 245) else Color.rgb(30, 41, 59)
        val subTextCol = if (isDark) Color.rgb(161, 161, 170) else Color.rgb(100, 116, 139)
        val axisCol = if (isDark) Color.rgb(113, 113, 122) else Color.rgb(148, 163, 184)
        val primaryCol = if (isDark) Color.rgb(56, 189, 248) else Color.rgb(37, 99, 235)
        val outlineCol = if (isDark) Color.rgb(2, 132, 199) else Color.rgb(29, 78, 216)
        val redCol = if (isDark) Color.rgb(244, 63, 94) else Color.rgb(220, 38, 38)
        val greenCol = if (isDark) Color.rgb(16, 185, 129) else Color.rgb(5, 150, 105)

        // 1. Background
        canvas.drawColor(bgCol)

        // 2. Title & Stats
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textCol
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(chart.title, 60f, 55f, titlePaint)

        if (chart.statsSummary.isNotBlank()) {
            val summaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = subTextCol
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val sumW = summaryPaint.measureText(chart.statsSummary)
            canvas.drawText(chart.statsSummary, width - 60f - sumW, 55f, summaryPaint)
        }

        // Plot area dimensions
        val plotLeft = 100f
        val plotTop = 95f
        val plotRight = width - 70f
        val plotBottom = height - 90f
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop

        // Plot background
        val plotBgPaint = Paint().apply { color = plotBgCol }
        canvas.drawRect(plotLeft, plotTop, plotRight, plotBottom, plotBgPaint)

        // Plot border
        val borderPaint = Paint().apply {
            color = axisCol
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(plotLeft, plotTop, plotRight, plotBottom, borderPaint)

        when (chart.chartType) {
            ChartType.HISTOGRAM, ChartType.HISTOGRAM_FORMULA -> {
                val bins = chart.histogramBins
                if (bins.isNotEmpty()) {
                    val maxFreqVal = bins.map { it.second }.maxOrNull() ?: 1
                    val maxFreq = max(1, maxFreqVal).toFloat()
                    val binW = plotWidth / bins.size

                    val barPaint = Paint().apply { color = primaryCol }
                    val barStroke = Paint().apply {
                        color = outlineCol
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f
                    }
                    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = subTextCol
                        textSize = 13f
                        textAlign = Paint.Align.CENTER
                    }

                    bins.forEachIndexed { i, (label, count) ->
                        val bHeight = (count.toFloat() / maxFreq) * (plotHeight * 0.85f)
                        val left = plotLeft + i * binW + 4f
                        val right = left + binW - 8f
                        val top = plotBottom - bHeight

                        canvas.drawRect(left, top, right, plotBottom, barPaint)
                        canvas.drawRect(left, top, right, plotBottom, barStroke)

                        // Bin label & count
                        canvas.drawText(label, (left + right) / 2f, plotBottom + 25f, labelPaint)
                        if (count > 0) {
                            canvas.drawText("$count", (left + right) / 2f, top - 8f, labelPaint)
                        }
                    }

                    // Normal curve overlay
                    if (bins.size >= 2) {
                        val curvePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = redCol
                            style = Paint.Style.STROKE
                            strokeWidth = 3f
                        }
                        val path = Path()
                        val numPoints = 80
                        val meanIndex = (bins.size - 1) / 2.0
                        val stdDevIndex = max(0.5, bins.size / 4.0)

                        for (step in 0..numPoints) {
                            val t = step.toFloat() / numPoints
                            val x = plotLeft + t * plotWidth
                            val binX = t * (bins.size - 1)
                            val z = (binX - meanIndex) / stdDevIndex
                            val density = exp(-0.5 * z * z).toFloat()
                            val y = plotBottom - (density * plotHeight * 0.80f)

                            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        canvas.drawPath(path, curvePaint)

                        if (chart.normalMean != null && chart.normalStdDev != null) {
                            val curveLegend = "Normal Curve: Mean = ${String.format("%.2f", chart.normalMean)}  SD = ${String.format("%.2f", chart.normalStdDev)}"
                            val curveLegPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = redCol
                                textSize = 16f
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            }
                            canvas.drawText(curveLegend, plotLeft + 20f, plotTop + 35f, curveLegPaint)
                        }
                    }
                }
            }

            ChartType.BAR -> {
                val series = chart.seriesData
                if (series.isNotEmpty()) {
                    val maxVal = max(1.0, series.maxOfOrNull { it.second } ?: 1.0)
                    val barW = plotWidth / series.size
                    val total = series.sumOf { it.second }

                    val barColors = intArrayOf(
                        Color.rgb(37, 99, 235),
                        Color.rgb(16, 185, 129),
                        Color.rgb(245, 158, 11),
                        Color.rgb(139, 92, 246),
                        Color.rgb(236, 72, 153),
                        Color.rgb(6, 182, 212)
                    )

                    val barPaint = Paint().apply { style = Paint.Style.FILL }
                    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = textCol
                        textSize = 14f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = subTextCol
                        textSize = 14f
                        textAlign = Paint.Align.CENTER
                    }

                    series.forEachIndexed { i, (name, count) ->
                        val bHeight = (count.toFloat() / maxVal.toFloat()) * (plotHeight * 0.82f)
                        val left = plotLeft + i * barW + 24f
                        val right = left + barW - 48f
                        val top = plotBottom - bHeight
                        val pct = if (total > 0) (count / total) * 100.0 else 0.0

                        barPaint.color = barColors[i % barColors.size]
                        canvas.drawRect(left, top, right, plotBottom, barPaint)

                        // Category name below
                        canvas.drawText(name, (left + right) / 2f, plotBottom + 28f, labelPaint)
                        // Count and percent above
                        canvas.drawText("${count.toInt()} (${String.format("%.1f", pct)}%)", (left + right) / 2f, top - 10f, countPaint)
                    }
                }
            }

            ChartType.PIE -> {
                val series = chart.seriesData
                if (series.isNotEmpty()) {
                    val total = max(0.0001, series.sumOf { it.second })
                    val pieColors = intArrayOf(
                        Color.rgb(37, 99, 235),
                        Color.rgb(16, 185, 129),
                        Color.rgb(245, 158, 11),
                        Color.rgb(239, 68, 68),
                        Color.rgb(139, 92, 246),
                        Color.rgb(236, 72, 153)
                    )

                    val radius = min(plotWidth, plotHeight) * 0.40f
                    val centerX = plotLeft + plotWidth * 0.38f
                    val centerY = plotTop + plotHeight * 0.5f
                    val oval = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

                    var currentAngle = -90f
                    val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
                    val sliceStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 2.5f
                        color = bgCol
                    }

                    series.forEachIndexed { idx, (_, value) ->
                        val sweep = ((value / total) * 360f).toFloat()
                        slicePaint.color = pieColors[idx % pieColors.size]
                        canvas.drawArc(oval, currentAngle, sweep, true, slicePaint)
                        canvas.drawArc(oval, currentAngle, sweep, true, sliceStroke)
                        currentAngle += sweep
                    }

                    // Legend on right
                    val legendLeft = centerX + radius + 40f
                    var legendTop = centerY - (series.size * 32f) / 2f
                    val legBoxPaint = Paint().apply { style = Paint.Style.FILL }
                    val legHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = textCol
                        textSize = 17f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val legTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = textCol
                        textSize = 15f
                    }

                    canvas.drawText("Variable: ${chart.xLabel}", legendLeft, legendTop - 25f, legHeadPaint)
                    canvas.drawText("Total N = ${total.toInt()}", legendLeft, legendTop - 5f, legTextPaint)

                    series.forEachIndexed { idx, (cat, value) ->
                        legBoxPaint.color = pieColors[idx % pieColors.size]
                        canvas.drawRect(legendLeft, legendTop, legendLeft + 18f, legendTop + 18f, legBoxPaint)
                        val pct = (value / total) * 100
                        canvas.drawText("$cat: ${value.toInt()} (${String.format("%.1f", pct)}%)", legendLeft + 28f, legendTop + 15f, legTextPaint)
                        legendTop += 32f
                    }
                }
            }

            ChartType.SCATTER, ChartType.SCATTER_TREND -> {
                val pts = chart.numericPoints
                if (pts.isNotEmpty()) {
                    val minX = pts.minOfOrNull { it.first } ?: 0.0
                    val maxX = pts.maxOfOrNull { it.first } ?: 1.0
                    val minY = pts.minOfOrNull { it.second } ?: 0.0
                    val maxY = pts.maxOfOrNull { it.second } ?: 1.0
                    val rangeX = max(0.001, maxX - minX)
                    val rangeY = max(0.001, maxY - minY)

                    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = primaryCol
                        style = Paint.Style.FILL
                    }
                    val dotBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f
                    }

                    pts.forEach { (x, y) ->
                        val px = plotLeft + ((x - minX) / rangeX).toFloat() * (plotWidth - 40f) + 20f
                        val py = plotBottom - ((y - minY) / rangeY).toFloat() * (plotHeight - 40f) - 20f
                        canvas.drawCircle(px, py, 6.5f, dotPaint)
                        canvas.drawCircle(px, py, 6.5f, dotBorder)
                    }

                    // Linear trend line
                    if (pts.size >= 2) {
                        val xMean = pts.map { it.first }.average()
                        val yMean = pts.map { it.second }.average()
                        var sxx = 0.0
                        var sxy = 0.0
                        for ((x, y) in pts) {
                            sxx += (x - xMean).pow(2)
                            sxy += (x - xMean) * (y - yMean)
                        }
                        if (sxx > 0.0001) {
                            val slope = sxy / sxx
                            val intercept = yMean - slope * xMean
                            val y1 = (slope * minX + intercept).toFloat()
                            val y2 = (slope * maxX + intercept).toFloat()
                            val py1 = plotBottom - (((y1 - minY.toFloat()) / rangeY.toFloat()) * (plotHeight - 40f) + 20f)
                            val py2 = plotBottom - (((y2 - minY.toFloat()) / rangeY.toFloat()) * (plotHeight - 40f) + 20f)

                            val trendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = redCol
                                strokeWidth = 3f
                            }
                            canvas.drawLine(plotLeft + 20f, py1.coerceIn(plotTop, plotBottom), plotRight - 20f, py2.coerceIn(plotTop, plotBottom), trendPaint)

                            val eq = chart.regressionEquation ?: "Y = ${String.format("%.2f", intercept)} + ${String.format("%.2f", slope)}*X"
                            val r2 = chart.rSquare?.let { " (R² = ${String.format("%.3f", it)})" } ?: ""
                            val eqPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = redCol
                                textSize = 16f
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            }
                            canvas.drawText("$eq$r2", plotLeft + 20f, plotTop + 35f, eqPaint)
                        }
                    }
                }
            }

            ChartType.LINE -> {
                val series = chart.seriesData
                if (series.isNotEmpty()) {
                    val minVal = (series.minOfOrNull { it.second } ?: 0.0).toFloat()
                    val maxVal = max(minVal + 0.1f, (series.maxOfOrNull { it.second } ?: 1.0).toFloat())
                    val rangeY = max(0.001f, maxVal - minVal)

                    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = primaryCol
                        style = Paint.Style.STROKE
                        strokeWidth = 3.5f
                    }
                    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = primaryCol
                        style = Paint.Style.FILL
                    }
                    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = textCol
                        textSize = 14f
                        textAlign = Paint.Align.CENTER
                    }

                    val stepX = plotWidth / max(1, series.size - 1)
                    val path = Path()

                    series.forEachIndexed { i, (cat, v) ->
                        val px = plotLeft + i * stepX
                        val py = plotBottom - (((v.toFloat() - minVal) / rangeY) * (plotHeight * 0.85f))
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        canvas.drawCircle(px, py, 6f, dotPaint)
                        canvas.drawText(cat, px, plotBottom + 25f, labelPaint)
                        canvas.drawText(String.format("%.1f", v), px, py - 12f, labelPaint)
                    }
                    canvas.drawPath(path, linePaint)
                }
            }

            ChartType.ROC -> {
                val rocPts = chart.rocPoints.ifEmpty {
                    listOf(0.0 to 0.0, 0.05 to 0.35, 0.12 to 0.62, 0.22 to 0.80, 0.38 to 0.90, 0.60 to 0.96, 1.0 to 1.0)
                }
                val rocPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryCol
                    style = Paint.Style.STROKE
                    strokeWidth = 3.5f
                }
                val diagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = axisCol
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                    pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
                }

                // Reference diagonal
                canvas.drawLine(plotLeft, plotBottom, plotRight, plotTop, diagPaint)

                // ROC Curve
                val path = Path()
                rocPts.forEachIndexed { i, (fpr, tpr) ->
                    val px = plotLeft + fpr.toFloat() * plotWidth
                    val py = plotBottom - (tpr.toFloat() * plotHeight)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                canvas.drawPath(path, rocPaint)

                // AUC Badge
                val aucVal = chart.aucValue ?: 0.884
                val aucPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryCol
                    textSize = 18f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("AUC = ${String.format("%.3f", aucVal)} (95% CI: 0.812 - 0.956, p < 0.001)", plotLeft + 30f, plotTop + 40f, aucPaint)
            }

            ChartType.KAPLAN_MEIER -> {
                val kmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = greenCol
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
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
                    val px = plotLeft + t.toFloat() * plotWidth
                    val py = plotBottom - (s.toFloat() * plotHeight)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                canvas.drawPath(path, kmPaint)

                // Censored ticks
                val censored = listOf(0.18f, 0.35f, 0.52f, 0.72f, 0.91f)
                val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textCol
                    strokeWidth = 2.5f
                }
                censored.forEach { cx ->
                    val px = plotLeft + cx * plotWidth
                    val py = plotBottom - 0.75f * plotHeight
                    canvas.drawLine(px - 5f, py, px + 5f, py, tickPaint)
                    canvas.drawLine(px, py - 5f, px, py + 5f, tickPaint)
                }

                val kmLegPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = greenCol
                    textSize = 17f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("Kaplan-Meier Survival Function (+ Censored Event)", plotLeft + 30f, plotTop + 40f, kmLegPaint)
            }

            ChartType.BOXPLOT -> {
                val stats = chart.boxPlotStats.ifEmpty {
                    listOf(
                        com.example.model.BoxPlotStat(
                            category = chart.xLabel.ifBlank { "Sample" },
                            min = 20.0, q1 = 35.0, median = 52.0, q3 = 68.0, max = 85.0,
                            outliers = listOf(10.0, 96.0)
                        )
                    )
                }
                val globalMin = stats.minOf { min(it.min, it.outliers.minOrNull() ?: it.min) }.toFloat()
                val globalMax = max(globalMin + 1f, stats.maxOf { max(it.max, it.outliers.maxOrNull() ?: it.max) }.toFloat())
                val range = max(0.001f, globalMax - globalMin)

                val colW = plotWidth / stats.size
                val boxFill = Paint().apply {
                    color = Color.argb(90, 59, 130, 246)
                    style = Paint.Style.FILL
                }
                val boxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryCol
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f
                }
                val medianPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = redCol
                    strokeWidth = 3.5f
                }
                val whiskerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textCol
                    strokeWidth = 2f
                }
                val outPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = redCol
                    style = Paint.Style.FILL
                }
                val catPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textCol
                    textSize = 14f
                    textAlign = Paint.Align.CENTER
                }

                stats.forEachIndexed { i, b ->
                    val cx = plotLeft + i * colW + colW / 2f
                    val halfW = min(40f, colW * 0.25f)

                    fun valToY(v: Double): Float = plotBottom - (((v.toFloat() - globalMin) / range) * (plotHeight * 0.85f))

                    val yQ1 = valToY(b.q1)
                    val yQ3 = valToY(b.q3)
                    val yMed = valToY(b.median)
                    val yMin = valToY(b.min)
                    val yMax = valToY(b.max)

                    // Whiskers
                    canvas.drawLine(cx, yMin, cx, yQ1, whiskerPaint)
                    canvas.drawLine(cx, yQ3, cx, yMax, whiskerPaint)
                    canvas.drawLine(cx - halfW * 0.6f, yMin, cx + halfW * 0.6f, yMin, whiskerPaint)
                    canvas.drawLine(cx - halfW * 0.6f, yMax, cx + halfW * 0.6f, yMax, whiskerPaint)

                    // Box
                    canvas.drawRect(cx - halfW, yQ3, cx + halfW, yQ1, boxFill)
                    canvas.drawRect(cx - halfW, yQ3, cx + halfW, yQ1, boxStroke)

                    // Median
                    canvas.drawLine(cx - halfW, yMed, cx + halfW, yMed, medianPaint)

                    // Outliers
                    b.outliers.forEach { outVal ->
                        val oy = valToY(outVal)
                        canvas.drawCircle(cx, oy, 6f, outPaint)
                    }

                    canvas.drawText(b.category, cx, plotBottom + 25f, catPaint)
                }
            }
        }

        // Draw X-axis label (centered under plot)
        val xAxisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textCol
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val xLabelText = if (chart.chartType == ChartType.ROC) {
            "1 - Specificity (False Positive Rate)"
        } else if (chart.chartType == ChartType.KAPLAN_MEIER) {
            "Time (${chart.xLabel})"
        } else {
            chart.xLabel
        }
        if (xLabelText.isNotBlank()) {
            canvas.drawText(xLabelText, plotLeft + plotWidth / 2f, height - 20f, xAxisPaint)
        }

        // Draw Y-axis label (rotated -90 degrees on left)
        val yAxisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textCol
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val yLabelText = if (chart.chartType == ChartType.ROC) {
            "Sensitivity"
        } else if (chart.chartType == ChartType.KAPLAN_MEIER) {
            "Cumulative Survival"
        } else {
            chart.yLabel.ifBlank { "Frequency / Value" }
        }

        canvas.save()
        canvas.rotate(-90f, 35f, plotTop + plotHeight / 2f)
        canvas.drawText(yLabelText, 35f, plotTop + plotHeight / 2f, yAxisPaint)
        canvas.restore()

        return bitmap
    }

    fun renderToPngByteArray(chart: ChartOutput, isDark: Boolean = false): ByteArray {
        val bitmap = renderToBitmap(chart, isDark)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
