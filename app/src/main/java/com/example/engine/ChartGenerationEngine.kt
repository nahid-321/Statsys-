package com.example.engine

import com.example.model.BoxPlotStat
import com.example.model.ChartOutput
import com.example.model.ChartType
import com.example.model.Dataset
import java.util.UUID
import kotlin.math.*

object ChartGenerationEngine {

    fun generateHistogram(
        dataset: Dataset,
        variableName: String,
        withFormulaCurve: Boolean = false
    ): ChartOutput {
        val idx = dataset.findVariableIndex(variableName)
        val values = if (idx != -1) dataset.getNumericColumnValues(idx) else emptyList()

        if (values.isEmpty()) {
            return ChartOutput(
                id = UUID.randomUUID().toString(),
                title = if (withFormulaCurve) "Histogram with Normal Curve: $variableName" else "Histogram: $variableName",
                commandSyntax = "GRAPH /HISTOGRAM${if (withFormulaCurve) "(NORMAL)" else ""}=$variableName.",
                chartType = if (withFormulaCurve) ChartType.HISTOGRAM_FORMULA else ChartType.HISTOGRAM,
                xLabel = variableName,
                yLabel = "Frequency",
                seriesData = emptyList(),
                statsSummary = "N = 0"
            )
        }

        val minVal = values.minOrNull() ?: 0.0
        val maxVal = values.maxOrNull() ?: 1.0
        val binCount = 8
        val binWidth = if (maxVal > minVal) (maxVal - minVal) / binCount else 1.0
        val bins = IntArray(binCount)

        for (v in values) {
            val binIdx = min(binCount - 1, max(0, ((v - minVal) / binWidth).toInt()))
            bins[binIdx]++
        }

        val histogramBins = bins.mapIndexed { i, count ->
            val start = minVal + i * binWidth
            val end = start + binWidth
            Pair(String.format("%.1f-%.1f", start, end), count)
        }

        val mean = values.average()
        val variance = values.sumOf { (it - mean).pow(2) } / max(1, values.size - 1)
        val stdDev = sqrt(variance)

        return ChartOutput(
            id = UUID.randomUUID().toString(),
            title = if (withFormulaCurve) "Histogram with Normal Curve: $variableName" else "Histogram: $variableName",
            commandSyntax = "GRAPH /HISTOGRAM${if (withFormulaCurve) "(NORMAL)" else ""}=$variableName.",
            chartType = if (withFormulaCurve) ChartType.HISTOGRAM_FORMULA else ChartType.HISTOGRAM,
            xLabel = variableName,
            yLabel = "Frequency",
            seriesData = emptyList(),
            histogramBins = histogramBins,
            statsSummary = "Mean = ${String.format("%.2f", mean)}, Std. Dev. = ${String.format("%.2f", stdDev)}, N = ${values.size}",
            normalMean = mean,
            normalStdDev = stdDev
        )
    }

    fun generateScatter(
        dataset: Dataset,
        xVarName: String,
        yVarName: String,
        withTrendLine: Boolean = false
    ): ChartOutput {
        val xIdx = dataset.findVariableIndex(xVarName)
        val yIdx = dataset.findVariableIndex(yVarName)
        val points = mutableListOf<Pair<Double, Double>>()

        if (xIdx != -1 && yIdx != -1) {
            for (row in dataset.rows) {
                val x = if (xIdx < row.size) row[xIdx].toDoubleOrNull() else null
                val y = if (yIdx < row.size) row[yIdx].toDoubleOrNull() else null
                if (x != null && y != null) {
                    points.add(Pair(x, y))
                }
            }
        }

        var eq: String? = null
        var r2: Double? = null
        if (points.size >= 2) {
            val xMean = points.map { it.first }.average()
            val yMean = points.map { it.second }.average()
            var sxx = 0.0
            var sxy = 0.0
            var syy = 0.0
            for ((x, y) in points) {
                sxx += (x - xMean).pow(2)
                sxy += (x - xMean) * (y - yMean)
                syy += (y - yMean).pow(2)
            }
            if (sxx > 0.0001) {
                val slope = sxy / sxx
                val intercept = yMean - slope * xMean
                eq = "Y = ${String.format("%.2f", intercept)} + ${String.format("%.2f", slope)}*X"
                r2 = if (syy > 0) (sxy.pow(2) / (sxx * syy)).coerceIn(0.0, 1.0) else 0.0
            }
        }

        return ChartOutput(
            id = UUID.randomUUID().toString(),
            title = if (withTrendLine) "Scatter Plot with Trend Line: $yVarName by $xVarName" else "Scatter Plot of $yVarName by $xVarName",
            commandSyntax = "GRAPH /SCATTERPLOT(BIVAR)=$xVarName WITH $yVarName${if (withTrendLine) " /FITLINE=TOTAL" else ""}.",
            chartType = if (withTrendLine) ChartType.SCATTER_TREND else ChartType.SCATTER,
            xLabel = xVarName,
            yLabel = yVarName,
            seriesData = emptyList(),
            numericPoints = points,
            statsSummary = "N = ${points.size}${if (r2 != null) ", R² = ${String.format("%.3f", r2)}" else ""}",
            regressionEquation = eq,
            rSquare = r2
        )
    }

    fun generateBar(
        dataset: Dataset,
        categoryVarName: String
    ): ChartOutput {
        val idx = dataset.findVariableIndex(categoryVarName)
        val rawValues = if (idx != -1) dataset.getColumnValues(idx).filter { it.isNotBlank() } else emptyList()
        val counts = rawValues.groupingBy { it }.eachCount()
        val series = counts.entries.sortedByDescending { it.value }.map { Pair(it.key, it.value.toDouble()) }

        return ChartOutput(
            id = UUID.randomUUID().toString(),
            title = "Bar Chart: $categoryVarName",
            commandSyntax = "GRAPH /BAR(SIMPLE)=COUNT BY $categoryVarName.",
            chartType = ChartType.BAR,
            xLabel = categoryVarName,
            yLabel = "Count / Frequency",
            seriesData = series,
            statsSummary = "Total Cases = ${rawValues.size}"
        )
    }

    fun generatePie(
        dataset: Dataset,
        categoryVarName: String
    ): ChartOutput {
        val idx = dataset.findVariableIndex(categoryVarName)
        val rawValues = if (idx != -1) dataset.getColumnValues(idx).filter { it.isNotBlank() } else emptyList()
        val counts = rawValues.groupingBy { it }.eachCount()
        val series = counts.entries.sortedByDescending { it.value }.map { Pair(it.key, it.value.toDouble()) }

        return ChartOutput(
            id = UUID.randomUUID().toString(),
            title = "Pie Chart: $categoryVarName",
            commandSyntax = "GRAPH /PIE=COUNT BY $categoryVarName.",
            chartType = ChartType.PIE,
            xLabel = categoryVarName,
            yLabel = "Proportion",
            seriesData = series,
            statsSummary = "N = ${rawValues.size} Cases"
        )
    }

    fun generateLineDiagram(
        dataset: Dataset,
        xVarName: String,
        yVarName: String
    ): ChartOutput {
        val xIdx = dataset.findVariableIndex(xVarName)
        val yIdx = dataset.findVariableIndex(yVarName)
        val series = mutableListOf<Pair<String, Double>>()

        if (xIdx != -1 && yIdx != -1) {
            val grouped = mutableMapOf<String, MutableList<Double>>()
            for (row in dataset.rows) {
                val xVal = if (xIdx < row.size) row[xIdx] else ""
                val yVal = if (yIdx < row.size) row[yIdx].toDoubleOrNull() else null
                if (xVal.isNotBlank() && yVal != null) {
                    grouped.getOrPut(xVal) { mutableListOf() }.add(yVal)
                }
            }
            grouped.forEach { (cat, list) ->
                series.add(Pair(cat, list.average()))
            }
        }

        return ChartOutput(
            id = UUID.randomUUID().toString(),
            title = "Line Diagram: Mean $yVarName by $xVarName",
            commandSyntax = "GRAPH /LINE(SIMPLE)=MEAN($yVarName) BY $xVarName.",
            chartType = ChartType.LINE,
            xLabel = xVarName,
            yLabel = "Mean $yVarName",
            seriesData = series,
            statsSummary = "Categories = ${series.size}"
        )
    }

    fun generateRocCurve(
        dataset: Dataset,
        testVarName: String,
        stateVarName: String
    ): ChartOutput {
        val tIdx = dataset.findVariableIndex(testVarName)
        val sIdx = dataset.findVariableIndex(stateVarName)
        val pairs = mutableListOf<Pair<Double, Int>>()

        if (tIdx != -1 && sIdx != -1) {
            for (row in dataset.rows) {
                val score = if (tIdx < row.size) row[tIdx].toDoubleOrNull() else null
                val state = if (sIdx < row.size) row[sIdx].toIntOrNull() else null
                if (score != null && state != null) {
                    pairs.add(score to if (state > 0) 1 else 0)
                }
            }
        }

        // Sort descending by test score
        pairs.sortByDescending { it.first }
        val numPos = max(1, pairs.count { it.second == 1 })
        val numNeg = max(1, pairs.count { it.second == 0 })

        var tp = 0
        var fp = 0
        val points = mutableListOf<Pair<Double, Double>>()
        points.add(0.0 to 0.0)

        for (p in pairs) {
            if (p.second == 1) tp++ else fp++
            val tpr = tp.toDouble() / numPos
            val fpr = fp.toDouble() / numNeg
            points.add(fpr to tpr)
        }
        points.add(1.0 to 1.0)

        // Trapezoidal AUC calculation
        var auc = 0.0
        for (i in 1 until points.size) {
            val (x0, y0) = points[i - 1]
            val (x1, y1) = points[i]
            auc += (x1 - x0) * (y0 + y1) / 2.0
        }
        auc = max(0.50, min(1.0, auc))

        return ChartOutput(
            id = UUID.randomUUID().toString(),
            title = "ROC Curve: $testVarName (State: $stateVarName=1)",
            commandSyntax = "ROC $testVarName BY $stateVarName (1) /PLOT=CURVE(REFERENCE) /PRINT=SE.",
            chartType = ChartType.ROC,
            xLabel = "1 - Specificity",
            yLabel = "Sensitivity",
            rocPoints = points,
            aucValue = auc,
            statsSummary = "AUC = ${String.format("%.3f", auc)}, N Pos = $numPos, N Neg = $numNeg"
        )
    }

    fun generateKaplanMeierCurve(
        dataset: Dataset,
        timeVarName: String,
        statusVarName: String
    ): ChartOutput {
        val tIdx = dataset.findVariableIndex(timeVarName)
        val sIdx = dataset.findVariableIndex(statusVarName)
        var totalCases = 0
        var totalEvents = 0

        if (tIdx != -1 && sIdx != -1) {
            for (row in dataset.rows) {
                val time = if (tIdx < row.size) row[tIdx].toDoubleOrNull() else null
                val status = if (sIdx < row.size) row[sIdx].toIntOrNull() else null
                if (time != null && status != null) {
                    totalCases++
                    if (status == 1) totalEvents++
                }
            }
        }

        return ChartOutput(
            id = UUID.randomUUID().toString(),
            title = "Kaplan-Meier Survival Function: $timeVarName",
            commandSyntax = "KM $timeVarName BY $statusVarName (1) /STATUS=$statusVarName EVENT (1) /PRINT=TABLE /PLOT SURVIVAL.",
            chartType = ChartType.KAPLAN_MEIER,
            xLabel = timeVarName,
            yLabel = "Cumulative Survival",
            statsSummary = "Total N = $totalCases, Events = $totalEvents, Censored = ${totalCases - totalEvents}"
        )
    }

    fun generateBoxPlot(
        dataset: Dataset,
        varName: String,
        groupVarName: String? = null
    ): ChartOutput {
        val vIdx = dataset.findVariableIndex(varName)
        val gIdx = if (groupVarName != null) dataset.findVariableIndex(groupVarName) else -1
        val boxStats = mutableListOf<BoxPlotStat>()

        if (vIdx != -1) {
            if (gIdx != -1 && groupVarName != null) {
                val groups = mutableMapOf<String, MutableList<Double>>()
                for (row in dataset.rows) {
                    val g = if (gIdx < row.size) row[gIdx] else ""
                    val v = if (vIdx < row.size) row[vIdx].toDoubleOrNull() else null
                    if (g.isNotBlank() && v != null) {
                        groups.getOrPut(g) { mutableListOf() }.add(v)
                    }
                }
                groups.forEach { (cat, vals) ->
                    boxStats.add(computeBoxStats(cat, vals))
                }
            } else {
                val vals = dataset.getNumericColumnValues(vIdx)
                boxStats.add(computeBoxStats(varName, vals))
            }
        }

        return ChartOutput(
            id = UUID.randomUUID().toString(),
            title = if (groupVarName != null) "Box Plot of $varName by $groupVarName" else "Box Plot of $varName",
            commandSyntax = "EXAMINE VARIABLES=$varName ${if (groupVarName != null) "BY $groupVarName " else ""}/PLOT=BOXPLOT.",
            chartType = ChartType.BOXPLOT,
            xLabel = groupVarName ?: varName,
            yLabel = varName,
            boxPlotStats = boxStats,
            statsSummary = "IQR Multiplier = 1.5, Outliers detected"
        )
    }

    private fun computeBoxStats(category: String, vals: List<Double>): BoxPlotStat {
        if (vals.isEmpty()) {
            return BoxPlotStat(category, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList())
        }
        val sorted = vals.sorted()
        val n = sorted.size
        val median = percentile(sorted, 0.50)
        val q1 = percentile(sorted, 0.25)
        val q3 = percentile(sorted, 0.75)
        val iqr = q3 - q1
        val lowerFence = q1 - 1.5 * iqr
        val upperFence = q3 + 1.5 * iqr

        val inside = sorted.filter { it in lowerFence..upperFence }
        val minVal = inside.minOrNull() ?: q1
        val maxVal = inside.maxOrNull() ?: q3
        val outliers = sorted.filter { it < lowerFence || it > upperFence }

        return BoxPlotStat(
            category = category,
            min = minVal,
            q1 = q1,
            median = median,
            q3 = q3,
            max = maxVal,
            outliers = outliers
        )
    }

    private fun percentile(sorted: List<Double>, p: Double): Double {
        if (sorted.isEmpty()) return 0.0
        val pos = p * (sorted.size - 1)
        val low = pos.toInt()
        val high = min(sorted.size - 1, low + 1)
        val weight = pos - low
        return sorted[low] * (1.0 - weight) + sorted[high] * weight
    }
}
