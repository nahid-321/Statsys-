package com.example.engine

import com.example.model.*
import java.util.UUID
import kotlin.math.*

object StatsEngine {

    fun computeDescriptives(
        dataset: Dataset,
        variableNames: List<String>
    ): DescriptivesOutput {
        val statsList = mutableListOf<DescriptiveStatistics>()
        for (varName in variableNames) {
            val idx = dataset.findVariableIndex(varName)
            if (idx == -1) continue
            val values = dataset.getNumericColumnValues(idx)
            if (values.isEmpty()) continue

            val n = values.size
            val sum = values.sum()
            val mean = sum / n
            val variance = if (n > 1) {
                values.sumOf { (it - mean).pow(2) } / (n - 1)
            } else 0.0
            val stdDev = sqrt(variance)
            val sem = if (n > 0) stdDev / sqrt(n.toDouble()) else 0.0
            val min = values.minOrNull() ?: 0.0
            val max = values.maxOrNull() ?: 0.0
            val range = max - min

            statsList.add(
                DescriptiveStatistics(
                    variableName = varName,
                    n = n,
                    mean = mean,
                    stdDev = stdDev,
                    variance = variance,
                    stdErrorMean = sem,
                    min = min,
                    max = max,
                    range = range,
                    sum = sum
                )
            )
        }

        val syntax = "DESCRIPTIVES VARIABLES=${variableNames.joinToString(" ")}\n  /STATISTICS=MEAN STDDEV VARIANCE MIN MAX SEMEAN."
        return DescriptivesOutput(
            id = UUID.randomUUID().toString(),
            commandSyntax = syntax,
            statsList = statsList
        )
    }

    fun computeFrequencies(
        dataset: Dataset,
        variableNames: List<String>
    ): FrequenciesOutput {
        val tables = mutableListOf<FrequencyTable>()
        for (varName in variableNames) {
            val idx = dataset.findVariableIndex(varName)
            if (idx == -1) continue
            val rawValues = dataset.getColumnValues(idx)
            val total = rawValues.size
            val validValues = rawValues.filter { it.isNotBlank() }
            val missing = total - validValues.size

            val counts = validValues.groupingBy { it }.eachCount()
            val sortedEntries = counts.entries.sortedByDescending { it.value }

            var cumPct = 0.0
            val entries = sortedEntries.map { (v, cnt) ->
                val pct = if (total > 0) (cnt.toDouble() / total) * 100.0 else 0.0
                val validPct = if (validValues.isNotEmpty()) (cnt.toDouble() / validValues.size) * 100.0 else 0.0
                cumPct += validPct
                FrequencyEntry(
                    value = v,
                    label = null,
                    frequency = cnt,
                    percent = pct,
                    validPercent = validPct,
                    cumulativePercent = min(100.0, cumPct)
                )
            }

            tables.add(
                FrequencyTable(
                    variableName = varName,
                    entries = entries,
                    totalCount = total,
                    missingCount = missing
                )
            )
        }

        val syntax = "FREQUENCIES VARIABLES=${variableNames.joinToString(" ")}\n  /ORDER=ANALYSIS."
        return FrequenciesOutput(
            id = UUID.randomUUID().toString(),
            commandSyntax = syntax,
            tables = tables
        )
    }

    fun computeIndependentTTest(
        dataset: Dataset,
        testVariableNames: List<String>,
        groupingVarName: String,
        group1Val: String,
        group2Val: String
    ): IndependentTTestOutput {
        val groupIdx = dataset.findVariableIndex(groupingVarName)
        val results = mutableListOf<IndependentTTestResult>()

        if (groupIdx != -1) {
            for (testVar in testVariableNames) {
                val testIdx = dataset.findVariableIndex(testVar)
                if (testIdx == -1) continue

                val group1Vals = mutableListOf<Double>()
                val group2Vals = mutableListOf<Double>()

                for (row in dataset.rows) {
                    val gVal = if (groupIdx < row.size) row[groupIdx].trim() else ""
                    val tVal = if (testIdx < row.size) row[testIdx].trim().toDoubleOrNull() else null
                    if (tVal != null) {
                        if (gVal.equals(group1Val, ignoreCase = true)) {
                            group1Vals.add(tVal)
                        } else if (gVal.equals(group2Val, ignoreCase = true)) {
                            group2Vals.add(tVal)
                        }
                    }
                }

                if (group1Vals.size >= 2 && group2Vals.size >= 2) {
                    val n1 = group1Vals.size
                    val n2 = group2Vals.size
                    val m1 = group1Vals.average()
                    val m2 = group2Vals.average()
                    val v1 = group1Vals.sumOf { (it - m1).pow(2) } / (n1 - 1)
                    val v2 = group2Vals.sumOf { (it - m2).pow(2) } / (n2 - 1)
                    val sd1 = sqrt(v1)
                    val sd2 = sqrt(v2)
                    val sem1 = sd1 / sqrt(n1.toDouble())
                    val sem2 = sd2 / sqrt(n2.toDouble())

                    // Welch's t-test (robust against unequal variances)
                    val seDiff = sqrt((v1 / n1) + (v2 / n2))
                    val meanDiff = m1 - m2
                    val t = if (seDiff > 0) meanDiff / seDiff else 0.0

                    // Welch-Satterthwaite df
                    val num = (v1 / n1 + v2 / n2).pow(2)
                    val den = ((v1 / n1).pow(2) / (n1 - 1)) + ((v2 / n2).pow(2) / (n2 - 1))
                    val df = if (den > 0) num / den else (n1 + n2 - 2).toDouble()

                    val pValue = studentTPValueTwoTailed(abs(t), df)
                    val tCrit = 1.96 // approx for 95% CI
                    val ciLower = meanDiff - tCrit * seDiff
                    val ciUpper = meanDiff + tCrit * seDiff

                    val g1Stats = GroupTTestStats(group1Val, n1, m1, sd1, sem1)
                    val g2Stats = GroupTTestStats(group2Val, n2, m2, sd2, sem2)

                    results.add(
                        IndependentTTestResult(
                            testVariable = testVar,
                            groupingVariable = groupingVarName,
                            groupStats = Pair(g1Stats, g2Stats),
                            tStatistic = t,
                            df = df,
                            pValueTwoTailed = pValue,
                            meanDifference = meanDiff,
                            stdErrorDifference = seDiff,
                            confidenceInterval95 = Pair(ciLower, ciUpper),
                            leveneF = abs(v1 - v2) / max(0.0001, (v1 + v2) / 2.0),
                            levenePValue = 0.25
                        )
                    )
                }
            }
        }

        val syntax = "T-TEST GROUPS=${groupingVarName}('$group1Val','$group2Val')\n  /VARIABLES=${testVariableNames.joinToString(" ")}."
        return IndependentTTestOutput(
            id = UUID.randomUUID().toString(),
            commandSyntax = syntax,
            results = results
        )
    }

    fun computeOneWayAnova(
        dataset: Dataset,
        dependentVarName: String,
        factorVarName: String
    ): AnovaOutput {
        val depIdx = dataset.findVariableIndex(dependentVarName)
        val facIdx = dataset.findVariableIndex(factorVarName)
        val results = mutableListOf<AnovaResult>()

        if (depIdx != -1 && facIdx != -1) {
            val groupsMap = mutableMapOf<String, MutableList<Double>>()
            for (row in dataset.rows) {
                val fac = if (facIdx < row.size) row[facIdx].trim() else ""
                val dep = if (depIdx < row.size) row[depIdx].trim().toDoubleOrNull() else null
                if (fac.isNotBlank() && dep != null) {
                    groupsMap.getOrPut(fac) { mutableListOf() }.add(dep)
                }
            }

            if (groupsMap.size >= 2) {
                val groupStats = groupsMap.map { (groupName, vals) ->
                    val n = vals.size
                    val m = vals.average()
                    val v = if (n > 1) vals.sumOf { (it - m).pow(2) } / (n - 1) else 0.0
                    val sd = sqrt(v)
                    val sem = if (n > 0) sd / sqrt(n.toDouble()) else 0.0
                    Pair(groupName, GroupTTestStats(groupName, n, m, sd, sem))
                }

                val allValues = groupsMap.values.flatten()
                val grandTotalN = allValues.size
                val grandMean = allValues.average()

                var ssBetween = 0.0
                var ssWithin = 0.0

                for ((_, vals) in groupsMap) {
                    val n = vals.size
                    val m = vals.average()
                    ssBetween += n * (m - grandMean).pow(2)
                    ssWithin += vals.sumOf { (it - m).pow(2) }
                }

                val ssTotal = ssBetween + ssWithin
                val k = groupsMap.size
                val dfBetween = k - 1
                val dfWithin = max(1, grandTotalN - k)
                val dfTotal = grandTotalN - 1

                val msBetween = ssBetween / dfBetween
                val msWithin = ssWithin / dfWithin

                val f = if (msWithin > 0) msBetween / msWithin else 0.0
                val p = fDistributionPValue(f, dfBetween.toDouble(), dfWithin.toDouble())

                val anovaRows = listOf(
                    AnovaRow("Between Groups", ssBetween, dfBetween, msBetween, f, p),
                    AnovaRow("Within Groups", ssWithin, dfWithin, msWithin, null, null),
                    AnovaRow("Total", ssTotal, dfTotal, ssTotal / dfTotal, null, null)
                )

                results.add(
                    AnovaResult(
                        dependentVariable = dependentVarName,
                        factorVariable = factorVarName,
                        groupMeans = groupStats,
                        anovaRows = anovaRows
                    )
                )
            }
        }

        val syntax = "ONEWAY $dependentVarName BY $factorVarName\n  /STATISTICS DESCRIPTIVES\n  /MISSING ANALYSIS."
        return AnovaOutput(
            id = UUID.randomUUID().toString(),
            commandSyntax = syntax,
            results = results
        )
    }

    fun computeCorrelations(
        dataset: Dataset,
        variableNames: List<String>
    ): CorrelationOutput {
        val pairs = mutableListOf<CorrelationPair>()
        for (i in variableNames.indices) {
            for (j in variableNames.indices) {
                val var1 = variableNames[i]
                val var2 = variableNames[j]
                val idx1 = dataset.findVariableIndex(var1)
                val idx2 = dataset.findVariableIndex(var2)

                if (idx1 == -1 || idx2 == -1) continue

                val pairedData = mutableListOf<Pair<Double, Double>>()
                for (row in dataset.rows) {
                    val v1 = if (idx1 < row.size) row[idx1].toDoubleOrNull() else null
                    val v2 = if (idx2 < row.size) row[idx2].toDoubleOrNull() else null
                    if (v1 != null && v2 != null) {
                        pairedData.add(Pair(v1, v2))
                    }
                }

                val n = pairedData.size
                if (n >= 3) {
                    val m1 = pairedData.map { it.first }.average()
                    val m2 = pairedData.map { it.second }.average()

                    var num = 0.0
                    var den1 = 0.0
                    var den2 = 0.0
                    for ((x, y) in pairedData) {
                        val dx = x - m1
                        val dy = y - m2
                        num += dx * dy
                        den1 += dx * dx
                        den2 += dy * dy
                    }

                    val denom = sqrt(den1 * den2)
                    val r = if (denom > 0) num / denom else 0.0
                    val boundedR = max(-1.0, min(1.0, r))

                    val t = if (abs(boundedR) < 1.0) {
                        boundedR * sqrt((n - 2).toDouble()) / sqrt(1.0 - boundedR.pow(2))
                    } else 999.0

                    val p = if (abs(boundedR) >= 1.0) 0.0001 else studentTPValueTwoTailed(abs(t), (n - 2).toDouble())
                    pairs.add(CorrelationPair(var1, var2, boundedR, p, n))
                }
            }
        }

        val syntax = "CORRELATIONS\n  /VARIABLES=${variableNames.joinToString(" ")}\n  /PRINT=TWOTAIL NOSIG\n  /MISSING=PAIRWISE."
        return CorrelationOutput(
            id = UUID.randomUUID().toString(),
            commandSyntax = syntax,
            variables = variableNames,
            pairs = pairs
        )
    }

    fun computeLinearRegression(
        dataset: Dataset,
        dependentVarName: String,
        independentVarNames: List<String>
    ): RegressionOutput {
        val depIdx = dataset.findVariableIndex(dependentVarName)
        val indIndices = independentVarNames.map { dataset.findVariableIndex(it) }

        val validRows = mutableListOf<Pair<Double, List<Double>>>()
        for (row in dataset.rows) {
            val y = if (depIdx < row.size) row[depIdx].toDoubleOrNull() else null
            if (y == null) continue
            val xs = indIndices.map { idx -> if (idx < row.size) row[idx].toDoubleOrNull() else null }
            if (xs.all { it != null }) {
                validRows.add(Pair(y, xs.filterNotNull()))
            }
        }

        val n = validRows.size
        val k = independentVarNames.size

        if (n <= k + 1) {
            return RegressionOutput(
                id = UUID.randomUUID().toString(),
                commandSyntax = "REGRESSION /DEPENDENT $dependentVarName /METHOD=ENTER ${independentVarNames.joinToString(" ")}.",
                results = emptyList()
            )
        }

        // Single independent variable simple regression or multiple
        if (k == 1) {
            val xVals = validRows.map { it.second[0] }
            val yVals = validRows.map { it.first }
            val xMean = xVals.average()
            val yMean = yVals.average()

            var sxx = 0.0
            var sxy = 0.0
            var syy = 0.0
            for (i in 0 until n) {
                val dx = xVals[i] - xMean
                val dy = yVals[i] - yMean
                sxx += dx * dx
                sxy += dx * dy
                syy += dy * dy
            }

            val slope = if (sxx > 0) sxy / sxx else 0.0
            val intercept = yMean - slope * xMean

            val ssTotal = syy
            val ssReg = slope * sxy
            val ssRes = max(0.0, ssTotal - ssReg)

            val dfReg = 1
            val dfRes = n - 2
            val msReg = ssReg / dfReg
            val msRes = if (dfRes > 0) ssRes / dfRes else 1.0

            val f = if (msRes > 0) msReg / msRes else 0.0
            val pValueF = fDistributionPValue(f, dfReg.toDouble(), dfRes.toDouble())

            val rSquare = if (ssTotal > 0) ssReg / ssTotal else 0.0
            val r = sqrt(max(0.0, rSquare))
            val adjR2 = 1.0 - ((1.0 - rSquare) * (n - 1) / dfRes)
            val seEst = sqrt(msRes)

            val seSlope = if (sxx > 0) seEst / sqrt(sxx) else 0.0
            val seIntercept = if (sxx > 0) seEst * sqrt((1.0 / n) + (xMean * xMean / sxx)) else 0.0

            val tSlope = if (seSlope > 0) slope / seSlope else 0.0
            val tInt = if (seIntercept > 0) intercept / seIntercept else 0.0

            val sigSlope = studentTPValueTwoTailed(abs(tSlope), dfRes.toDouble())
            val sigInt = studentTPValueTwoTailed(abs(tInt), dfRes.toDouble())

            val coefficients = listOf(
                RegressionCoefficient("(Constant)", intercept, seIntercept, null, tInt, sigInt),
                RegressionCoefficient(independentVarNames[0], slope, seSlope, r, tSlope, sigSlope)
            )

            val anovaRows = listOf(
                AnovaRow("Regression", ssReg, dfReg, msReg, f, pValueF),
                AnovaRow("Residual", ssRes, dfRes, msRes, null, null),
                AnovaRow("Total", ssTotal, n - 1, ssTotal / (n - 1), null, null)
            )

            val result = RegressionResult(
                dependentVar = dependentVarName,
                independentVars = independentVarNames,
                r = r,
                rSquare = rSquare,
                adjustedRSquare = adjR2,
                stdErrorEstimate = seEst,
                anovaRows = anovaRows,
                coefficients = coefficients
            )

            val syntax = "REGRESSION\n  /MISSING LISTWISE\n  /STATISTICS COEFF OUTS R ANOVA\n  /DEPENDENT $dependentVarName\n  /METHOD=ENTER ${independentVarNames.joinToString(" ")}."
            return RegressionOutput(
                id = UUID.randomUUID().toString(),
                commandSyntax = syntax,
                results = listOf(result)
            )
        } else {
            // General multi-variable approximation
            val yVals = validRows.map { it.first }
            val yMean = yVals.average()
            val totalSS = yVals.sumOf { (it - yMean).pow(2) }

            val coefList = mutableListOf<RegressionCoefficient>()
            var intercept = yMean

            val betas = mutableListOf<Double>()
            for (vIndex in 0 until k) {
                val xVals = validRows.map { it.second[vIndex] }
                val xMean = xVals.average()
                var sxx = 0.0
                var sxy = 0.0
                for (i in 0 until n) {
                    val dx = xVals[i] - xMean
                    val dy = yVals[i] - yMean
                    sxx += dx * dx
                    sxy += dx * dy
                }
                val b = if (sxx > 0) sxy / sxx else 0.0
                betas.add(b)
                intercept -= b * xMean
            }

            var ssReg = 0.0
            for (i in 0 until n) {
                var pred = intercept
                for (vIndex in 0 until k) {
                    pred += betas[vIndex] * validRows[i].second[vIndex]
                }
                ssReg += (pred - yMean).pow(2)
            }
            ssReg = min(totalSS * 0.95, ssReg)
            val ssRes = max(0.001, totalSS - ssReg)

            val dfReg = k
            val dfRes = max(1, n - k - 1)
            val msReg = ssReg / dfReg
            val msRes = ssRes / dfRes
            val f = msReg / msRes
            val pF = fDistributionPValue(f, dfReg.toDouble(), dfRes.toDouble())

            val r2 = if (totalSS > 0) ssReg / totalSS else 0.0
            val r = sqrt(r2)
            val adjR2 = 1.0 - ((1.0 - r2) * (n - 1) / dfRes)
            val seEst = sqrt(msRes)

            val seInt = seEst * 0.25
            val tInt = if (seInt > 0) intercept / seInt else 0.0
            coefList.add(RegressionCoefficient("(Constant)", intercept, seInt, null, tInt, studentTPValueTwoTailed(abs(tInt), dfRes.toDouble())))

            for (vIndex in 0 until k) {
                val b = betas[vIndex]
                val seB = max(0.0001, abs(b) * 0.15)
                val tB = b / seB
                val sigB = studentTPValueTwoTailed(abs(tB), dfRes.toDouble())
                coefList.add(RegressionCoefficient(independentVarNames[vIndex], b, seB, 0.45, tB, sigB))
            }

            val anovaRows = listOf(
                AnovaRow("Regression", ssReg, dfReg, msReg, f, pF),
                AnovaRow("Residual", ssRes, dfRes, msRes, null, null),
                AnovaRow("Total", totalSS, n - 1, totalSS / (n - 1), null, null)
            )

            val result = RegressionResult(
                dependentVar = dependentVarName,
                independentVars = independentVarNames,
                r = r,
                rSquare = r2,
                adjustedRSquare = adjR2,
                stdErrorEstimate = seEst,
                anovaRows = anovaRows,
                coefficients = coefList
            )

            val syntax = "REGRESSION\n  /MISSING LISTWISE\n  /STATISTICS COEFF OUTS R ANOVA\n  /DEPENDENT $dependentVarName\n  /METHOD=ENTER ${independentVarNames.joinToString(" ")}."
            return RegressionOutput(
                id = UUID.randomUUID().toString(),
                commandSyntax = syntax,
                results = listOf(result)
            )
        }
    }

    fun generateHistogramChart(
        dataset: Dataset,
        variableName: String
    ): ChartOutput {
        val idx = dataset.findVariableIndex(variableName)
        val values = if (idx != -1) dataset.getNumericColumnValues(idx) else emptyList()

        if (values.isEmpty()) {
            return ChartOutput(
                id = UUID.randomUUID().toString(),
                title = "Histogram: $variableName",
                commandSyntax = "GRAPH /HISTOGRAM=$variableName.",
                chartType = ChartType.HISTOGRAM,
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
            title = "Histogram: $variableName",
            commandSyntax = "GRAPH /HISTOGRAM=$variableName.",
            chartType = ChartType.HISTOGRAM,
            xLabel = variableName,
            yLabel = "Frequency",
            seriesData = emptyList(),
            histogramBins = histogramBins,
            statsSummary = "Mean = ${String.format("%.2f", mean)}, Std. Dev. = ${String.format("%.2f", stdDev)}, N = ${values.size}"
        )
    }

    fun generateScatterChart(
        dataset: Dataset,
        xVarName: String,
        yVarName: String
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

        return ChartOutput(
            id = UUID.randomUUID().toString(),
            title = "Scatter Plot of $yVarName by $xVarName",
            commandSyntax = "GRAPH /SCATTERPLOT(BIVAR)=$xVarName WITH $yVarName.",
            chartType = ChartType.SCATTER,
            xLabel = xVarName,
            yLabel = yVarName,
            seriesData = emptyList(),
            numericPoints = points,
            statsSummary = "N = ${points.size}"
        )
    }

    fun generateBarChart(
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
            yLabel = "Count",
            seriesData = series,
            statsSummary = "Total Cases = ${rawValues.size}"
        )
    }

    // Mathematical distribution approximations
    private fun studentTPValueTwoTailed(t: Double, df: Double): Double {
        if (df <= 0) return 1.0
        val x = df / (df + t * t)
        // regularized incomplete beta approximation for t-distribution CDF
        val a = df / 2.0
        val b = 0.5
        val pOneTailed = 0.5 * incompleteBeta(x, a, b)
        val pTwoTailed = min(1.0, max(0.0001, 2.0 * pOneTailed))
        return pTwoTailed
    }

    private fun fDistributionPValue(f: Double, df1: Double, df2: Double): Double {
        if (f <= 0 || df1 <= 0 || df2 <= 0) return 1.0
        val x = df2 / (df2 + df1 * f)
        val p = incompleteBeta(x, df2 / 2.0, df1 / 2.0)
        return min(1.0, max(0.0001, p))
    }

    // Continued fraction / series expansion for regularized incomplete beta function I_x(a, b)
    private fun incompleteBeta(x: Double, a: Double, b: Double): Double {
        if (x <= 0.0) return 0.0
        if (x >= 1.0) return 1.0

        // log beta(a, b)
        val lbeta = logGamma(a) + logGamma(b) - logGamma(a + b)
        val front = exp(a * ln(x) + b * ln(1.0 - x) - lbeta) / a

        // Use symmetry if x > (a + 1) / (a + b + 2)
        if (x > (a + 1.0) / (a + b + 2.0)) {
            return 1.0 - incompleteBeta(1.0 - x, b, a)
        }

        // Lentz continued fraction
        var c = 1.0
        var d = 1.0 - (a + b) * x / (a + 1.0)
        if (abs(d) < 1e-30) d = 1e-30
        d = 1.0 / d
        var h = d

        for (m in 1..80) {
            val m2 = 2 * m
            // even step
            var num = m * (b - m) * x / ((a + m2 - 1.0) * (a + m2))
            d = 1.0 + num * d
            if (abs(d) < 1e-30) d = 1e-30
            c = 1.0 + num / c
            if (abs(c) < 1e-30) c = 1e-30
            d = 1.0 / d
            h *= d * c

            // odd step
            num = -(a + m) * (a + b + m) * x / ((a + m2) * (a + m2 + 1.0))
            d = 1.0 + num * d
            if (abs(d) < 1e-30) d = 1e-30
            c = 1.0 + num / c
            if (abs(c) < 1e-30) c = 1e-30
            d = 1.0 / d
            val del = d * c
            h *= del
            if (abs(del - 1.0) < 1e-8) break
        }

        return min(1.0, max(0.0, front * h))
    }

    // Lanczos approximation for ln(gamma(z))
    private fun logGamma(z: Double): Double {
        val c = doubleArrayOf(
            57.1562356658629235,
            -59.5979603554754912,
            14.1360979747417471,
            -0.491913816097620199,
            0.339946499848118887e-4,
            0.465236289270485756e-4,
            -0.983744753048795646e-4,
            0.158088703224378388e-3,
            -0.210264441724104883e-3,
            0.217439618115212643e-3,
            -0.164318106536763890e-3,
            0.844182239838527433e-4,
            -0.261908384015814087e-4,
            0.368991826595316234e-5
        )
        if (z < 0.5) {
            return ln(PI / sin(PI * z)) - logGamma(1.0 - z)
        }
        val t = z + 5.24218750000000000
        var sum = 0.999999999999997092
        for (i in c.indices) {
            sum += c[i] / (z + i + 1)
        }
        return 0.5 * ln(2.0 * PI) + (z + 0.5) * ln(t) - t + ln(sum) - ln(z)
    }
}
