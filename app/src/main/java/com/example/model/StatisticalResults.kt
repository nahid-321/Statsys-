package com.example.model

sealed interface OutputItem {
    val id: String
    val title: String
    val commandSyntax: String
}

data class DescriptiveStatistics(
    val variableName: String,
    val n: Int,
    val mean: Double,
    val stdDev: Double,
    val variance: Double,
    val stdErrorMean: Double,
    val min: Double,
    val max: Double,
    val range: Double,
    val sum: Double
)

data class DescriptivesOutput(
    override val id: String,
    override val title: String = "Descriptives",
    override val commandSyntax: String,
    val statsList: List<DescriptiveStatistics>
) : OutputItem

data class FrequencyEntry(
    val value: String,
    val label: String?,
    val frequency: Int,
    val percent: Double,
    val validPercent: Double,
    val cumulativePercent: Double
)

data class FrequencyTable(
    val variableName: String,
    val entries: List<FrequencyEntry>,
    val totalCount: Int,
    val missingCount: Int
)

data class FrequenciesOutput(
    override val id: String,
    override val title: String = "Frequencies",
    override val commandSyntax: String,
    val tables: List<FrequencyTable>
) : OutputItem

data class GroupTTestStats(
    val groupName: String,
    val n: Int,
    val mean: Double,
    val stdDev: Double,
    val stdErrorMean: Double
)

data class IndependentTTestResult(
    val testVariable: String,
    val groupingVariable: String,
    val groupStats: Pair<GroupTTestStats, GroupTTestStats>,
    val tStatistic: Double,
    val df: Double,
    val pValueTwoTailed: Double,
    val meanDifference: Double,
    val stdErrorDifference: Double,
    val confidenceInterval95: Pair<Double, Double>,
    val leveneF: Double,
    val levenePValue: Double
)

data class IndependentTTestOutput(
    override val id: String,
    override val title: String = "T-Test (Independent Samples)",
    override val commandSyntax: String,
    val results: List<IndependentTTestResult>
) : OutputItem

data class AnovaRow(
    val source: String,
    val sumOfSquares: Double,
    val df: Int,
    val meanSquare: Double,
    val fStatistic: Double?,
    val pValue: Double?
)

data class AnovaResult(
    val dependentVariable: String,
    val factorVariable: String,
    val groupMeans: List<Pair<String, GroupTTestStats>>,
    val anovaRows: List<AnovaRow>
)

data class AnovaOutput(
    override val id: String,
    override val title: String = "One-Way ANOVA",
    override val commandSyntax: String,
    val results: List<AnovaResult>
) : OutputItem

data class CorrelationPair(
    val var1: String,
    val var2: String,
    val pearsonR: Double,
    val pValue: Double,
    val n: Int
)

data class CorrelationOutput(
    override val id: String,
    override val title: String = "Correlations",
    override val commandSyntax: String,
    val variables: List<String>,
    val pairs: List<CorrelationPair>
) : OutputItem

data class RegressionCoefficient(
    val term: String,
    val b: Double,
    val stdError: Double,
    val beta: Double?,
    val t: Double,
    val sig: Double
)

data class RegressionResult(
    val dependentVar: String,
    val independentVars: List<String>,
    val r: Double,
    val rSquare: Double,
    val adjustedRSquare: Double,
    val stdErrorEstimate: Double,
    val anovaRows: List<AnovaRow>,
    val coefficients: List<RegressionCoefficient>
)

data class RegressionOutput(
    override val id: String,
    override val title: String = "Linear Regression",
    override val commandSyntax: String,
    val results: List<RegressionResult>
) : OutputItem

enum class ChartType {
    HISTOGRAM,
    HISTOGRAM_FORMULA,
    SCATTER,
    SCATTER_TREND,
    BAR,
    PIE,
    LINE,
    ROC,
    KAPLAN_MEIER,
    BOXPLOT
}

data class BoxPlotStat(
    val category: String,
    val min: Double,
    val q1: Double,
    val median: Double,
    val q3: Double,
    val max: Double,
    val outliers: List<Double> = emptyList()
)

data class ChartOutput(
    override val id: String,
    override val title: String,
    override val commandSyntax: String,
    val chartType: ChartType,
    val xLabel: String,
    val yLabel: String,
    val seriesData: List<Pair<String, Double>> = emptyList(), // for bar/pie/line
    val numericPoints: List<Pair<Double, Double>> = emptyList(), // for scatter
    val histogramBins: List<Pair<String, Int>> = emptyList(), // for histogram
    val boxPlotStats: List<BoxPlotStat> = emptyList(), // for box plot
    val rocPoints: List<Pair<Double, Double>> = emptyList(), // for ROC (1-spec, sens)
    val aucValue: Double? = null,
    val statsSummary: String = "",
    val normalMean: Double? = null,
    val normalStdDev: Double? = null,
    val regressionEquation: String? = null,
    val rSquare: Double? = null
) : OutputItem

data class StatisticalTable(
    val title: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val footnotes: List<String> = emptyList()
)

data class CustomAnalysisOutput(
    override val id: String,
    override val title: String,
    override val commandSyntax: String,
    val tables: List<StatisticalTable>,
    val notes: String = ""
) : OutputItem

data class LogOutput(
    override val id: String,
    override val title: String = "Log",
    override val commandSyntax: String,
    val message: String,
    val isError: Boolean = false
) : OutputItem
