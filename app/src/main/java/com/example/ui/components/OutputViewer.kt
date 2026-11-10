package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun OutputViewer(
    outputs: List<OutputItem>,
    isDark: Boolean,
    onClearOutputs: () -> Unit,
    onExportOutput: () -> Unit,
    onExportChartImage: (ChartOutput) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bg = if (isDark) Color(0xFF000000) else Color(0xFFF8FAFC)
    val toolbarBg = if (isDark) Color(0xFF0A0A0A) else Color(0xFFF1F5F9)
    val toolbarBorder = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
    val cardBg = if (isDark) Color(0xFF121212) else Color.White
    val cardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // Output toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(toolbarBg)
                .border(width = 0.5.dp, color = toolbarBorder)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Output Viewer — IBM SPSS / Statsys Format",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${outputs.size} Item(s)",
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onExportOutput,
                    modifier = Modifier.height(26.dp).testTag("output_export_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(3.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isDark) Color(0xFF18181B) else Color.White,
                        contentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Export", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export Report", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onClearOutputs,
                    modifier = Modifier.height(26.dp).testTag("output_clear_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(3.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isDark) Color(0xFF18181B) else Color.White,
                        contentColor = Color(0xFFEF4444)
                    )
                ) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Clear", modifier = Modifier.size(12.dp), tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", fontSize = 11.sp, color = Color(0xFFEF4444))
                }
            }
        }

        if (outputs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No statistical analysis output yet.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select Analyze or Graphs from the top menu, or run SPSS syntax to generate tables and charts.",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(outputs, key = { it.id }) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardBg, RoundedCornerShape(3.dp))
                            .border(1.dp, cardBorder, RoundedCornerShape(3.dp))
                            .padding(12.dp)
                    ) {
                        // Monospace syntax log box
                        val syntaxBg = if (isDark) Color(0xFF0C1D2F) else Color(0xFFF0F9FF)
                        val syntaxBorder = if (isDark) Color(0xFF1E3A5F) else Color(0xFFBAE6FD)
                        val syntaxColor = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)

                        Text(
                            text = item.commandSyntax,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = syntaxColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(syntaxBg)
                                .border(0.5.dp, syntaxBorder)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        when (item) {
                            is DescriptivesOutput -> DescriptivesTable(item, isDark)
                            is FrequenciesOutput -> FrequenciesTables(item, isDark)
                            is IndependentTTestOutput -> IndependentTTestTables(item, isDark)
                            is AnovaOutput -> AnovaTables(item, isDark)
                            is CorrelationOutput -> CorrelationTable(item, isDark)
                            is RegressionOutput -> RegressionTables(item, isDark)
                            is ChartOutput -> StatisticalChartCard(item, isDark, onExportChartImage)
                            is CustomAnalysisOutput -> CustomAnalysisReport(item, isDark)
                            is LogOutput -> {
                                Text(
                                    text = item.message,
                                    fontSize = 12.sp,
                                    color = if (item.isError) Color(0xFFEF4444) else (if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DescriptivesTable(output: DescriptivesOutput, isDark: Boolean) {
    Column {
        Text(
            text = "Descriptive Statistics",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        val headers = listOf("Variable", "N", "Minimum", "Maximum", "Mean", "Std. Error", "Std. Deviation", "Variance")
        val widths = listOf(130.dp, 60.dp, 80.dp, 80.dp, 80.dp, 90.dp, 90.dp, 90.dp)
        SpssTableLayout(
            headers = headers,
            widths = widths,
            rows = output.statsList.map { s ->
                listOf(
                    s.variableName,
                    "${s.n}",
                    String.format("%.2f", s.min),
                    String.format("%.2f", s.max),
                    String.format("%.2f", s.mean),
                    String.format("%.3f", s.stdErrorMean),
                    String.format("%.2f", s.stdDev),
                    String.format("%.2f", s.variance)
                )
            },
            isDark = isDark
        )
    }
}

@Composable
private fun FrequenciesTables(output: FrequenciesOutput, isDark: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        output.tables.forEach { table ->
            Column {
                Text(
                    text = "Frequencies: ${table.variableName}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val headers = listOf("Value", "Frequency", "Percent", "Valid Percent", "Cumulative %")
                val widths = listOf(140.dp, 80.dp, 80.dp, 90.dp, 100.dp)
                val rows = table.entries.map { e ->
                    listOf(
                        e.value,
                        "${e.frequency}",
                        String.format("%.1f%%", e.percent),
                        String.format("%.1f%%", e.validPercent),
                        String.format("%.1f%%", e.cumulativePercent)
                    )
                }.toMutableList()

                rows.add(listOf("Total Valid", "${table.totalCount - table.missingCount}", "100.0%", "100.0%", "100.0%"))
                if (table.missingCount > 0) {
                    rows.add(listOf("Missing", "${table.missingCount}", "${table.missingCount}", "-", "-"))
                }

                SpssTableLayout(headers = headers, widths = widths, rows = rows, isDark = isDark)
            }
        }
    }
}

@Composable
private fun IndependentTTestTables(output: IndependentTTestOutput, isDark: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        output.results.forEach { res ->
            Column {
                Text(
                    text = "Group Statistics",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                val groupHeaders = listOf("Variable", res.groupingVariable, "N", "Mean", "Std. Deviation", "Std. Error Mean")
                val groupWidths = listOf(120.dp, 100.dp, 60.dp, 80.dp, 90.dp, 100.dp)
                val groupRows = listOf(
                    listOf(
                        res.testVariable,
                        res.groupStats.first.groupName,
                        "${res.groupStats.first.n}",
                        String.format("%.2f", res.groupStats.first.mean),
                        String.format("%.2f", res.groupStats.first.stdDev),
                        String.format("%.3f", res.groupStats.first.stdErrorMean)
                    ),
                    listOf(
                        "",
                        res.groupStats.second.groupName,
                        "${res.groupStats.second.n}",
                        String.format("%.2f", res.groupStats.second.mean),
                        String.format("%.2f", res.groupStats.second.stdDev),
                        String.format("%.3f", res.groupStats.second.stdErrorMean)
                    )
                )
                SpssTableLayout(headers = groupHeaders, widths = groupWidths, rows = groupRows, isDark = isDark)

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Independent Samples Test: ${res.testVariable}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                val testHeaders = listOf("t", "df", "Sig. (2-tailed)", "Mean Diff", "Std. Error Diff", "95% CI Lower", "95% CI Upper")
                val testWidths = listOf(80.dp, 70.dp, 90.dp, 90.dp, 100.dp, 95.dp, 95.dp)
                val testRows = listOf(
                    listOf(
                        String.format("%.3f", res.tStatistic),
                        String.format("%.2f", res.df),
                        String.format("%.4f", res.pValueTwoTailed),
                        String.format("%.2f", res.meanDifference),
                        String.format("%.3f", res.stdErrorDifference),
                        String.format("%.2f", res.confidenceInterval95.first),
                        String.format("%.2f", res.confidenceInterval95.second)
                    )
                )
                SpssTableLayout(headers = testHeaders, widths = testWidths, rows = testRows, isDark = isDark)
            }
        }
    }
}

@Composable
private fun AnovaTables(output: AnovaOutput, isDark: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        output.results.forEach { res ->
            Column {
                Text(
                    text = "ANOVA: ${res.dependentVariable} by ${res.factorVariable}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                val headers = listOf("Source", "Sum of Squares", "df", "Mean Square", "F", "Sig.")
                val widths = listOf(140.dp, 110.dp, 60.dp, 100.dp, 80.dp, 80.dp)
                val rows = res.anovaRows.map { r ->
                    listOf(
                        r.source,
                        String.format("%.3f", r.sumOfSquares),
                        "${r.df}",
                        String.format("%.3f", r.meanSquare),
                        r.fStatistic?.let { String.format("%.3f", it) } ?: "",
                        r.pValue?.let { String.format("%.4f", it) } ?: ""
                    )
                }
                SpssTableLayout(headers = headers, widths = widths, rows = rows, isDark = isDark)
            }
        }
    }
}

@Composable
private fun CorrelationTable(output: CorrelationOutput, isDark: Boolean) {
    Column {
        Text(
            text = "Correlations Matrix",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        val headers = mutableListOf("Metric")
        headers.addAll(output.variables)
        val widths = mutableListOf(140.dp)
        output.variables.forEach { _ -> widths.add(90.dp) }

        val rows = mutableListOf<List<String>>()
        for (v1 in output.variables) {
            val rRow = mutableListOf("$v1 (Pearson r)")
            val sigRow = mutableListOf("Sig. (2-tailed)")
            val nRow = mutableListOf("N")

            for (v2 in output.variables) {
                val pair = output.pairs.firstOrNull { (it.var1 == v1 && it.var2 == v2) || (it.var1 == v2 && it.var2 == v1) }
                if (v1 == v2) {
                    rRow.add("1.000")
                    sigRow.add("-")
                    nRow.add("${output.pairs.firstOrNull { it.var1 == v1 }?.n ?: 122}")
                } else if (pair != null) {
                    rRow.add(String.format("%.3f", pair.pearsonR))
                    sigRow.add(String.format("%.4f", pair.pValue))
                    nRow.add("${pair.n}")
                } else {
                    rRow.add("-")
                    sigRow.add("-")
                    nRow.add("-")
                }
            }
            rows.add(rRow)
            rows.add(sigRow)
            rows.add(nRow)
        }

        SpssTableLayout(headers = headers, widths = widths, rows = rows, isDark = isDark)
    }
}

@Composable
private fun RegressionTables(output: RegressionOutput, isDark: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        output.results.forEach { res ->
            Column {
                Text(
                    text = "Model Summary: Dependent Variable = ${res.dependentVar}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                val summaryHeaders = listOf("R", "R Square", "Adjusted R Square", "Std. Error of Estimate")
                val summaryWidths = listOf(90.dp, 90.dp, 120.dp, 140.dp)
                val summaryRows = listOf(
                    listOf(
                        String.format("%.3f", res.r),
                        String.format("%.3f", res.rSquare),
                        String.format("%.3f", res.adjustedRSquare),
                        String.format("%.3f", res.stdErrorEstimate)
                    )
                )
                SpssTableLayout(headers = summaryHeaders, widths = summaryWidths, rows = summaryRows, isDark = isDark)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Coefficients",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                val coefHeaders = listOf("Model", "Unstandardized B", "Std. Error", "Beta", "t", "Sig.")
                val coefWidths = listOf(140.dp, 120.dp, 90.dp, 80.dp, 80.dp, 80.dp)
                val coefRows = res.coefficients.map { c ->
                    listOf(
                        c.term,
                        String.format("%.3f", c.b),
                        String.format("%.3f", c.stdError),
                        c.beta?.let { String.format("%.3f", it) } ?: "",
                        String.format("%.3f", c.t),
                        String.format("%.4f", c.sig)
                    )
                }
                SpssTableLayout(headers = coefHeaders, widths = coefWidths, rows = coefRows, isDark = isDark)
            }
        }
    }
}

@Composable
private fun SpssTableLayout(
    headers: List<String>,
    widths: List<androidx.compose.ui.unit.Dp>,
    rows: List<List<String>>,
    isDark: Boolean
) {
    val tableBorder = if (isDark) Color(0xFF3F3F46) else Color(0xFF0F172A)
    val headerBg = if (isDark) Color(0xFF18181B) else Color(0xFFF1F5F9)
    val headerText = if (isDark) Color(0xFFFAFAFA) else Color(0xFF0F172A)
    val cellText = if (isDark) Color(0xFFF4F4F5) else Color(0xFF1E293B)
    val rowEvenBg = if (isDark) Color(0xFF121212) else Color.White
    val rowOddBg = if (isDark) Color(0xFF1E1E22) else Color(0xFFFAFAFA)
    val innerBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val hScroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, tableBorder)
            .background(rowEvenBg)
            .horizontalScroll(hScroll)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .background(headerBg)
                    .border(width = 0.5.dp, color = tableBorder)
            ) {
                headers.forEachIndexed { i, h ->
                    val colW = widths.getOrElse(i) { 100.dp }
                    Box(
                        modifier = Modifier
                            .width(colW)
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        contentAlignment = if (i == 0) Alignment.CenterStart else Alignment.CenterEnd
                    ) {
                        Text(
                            text = h,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = headerText,
                            textAlign = if (i == 0) TextAlign.Start else TextAlign.End
                        )
                    }
                }
            }

            // Data Rows
            rows.forEachIndexed { rIdx, row ->
                Row(
                    modifier = Modifier
                        .background(if (rIdx % 2 == 1) rowOddBg else rowEvenBg)
                        .border(width = 0.5.dp, color = innerBorder)
                ) {
                    headers.indices.forEach { cIdx ->
                        val cell = row.getOrElse(cIdx) { "" }
                        val colW = widths.getOrElse(cIdx) { 100.dp }
                        Box(
                            modifier = Modifier
                                .width(colW)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = if (cIdx == 0) Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            Text(
                                text = cell,
                                fontSize = 11.sp,
                                color = cellText,
                                fontWeight = if (cIdx == 0) FontWeight.Medium else FontWeight.Normal,
                                textAlign = if (cIdx == 0) TextAlign.Start else TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomAnalysisReport(output: CustomAnalysisOutput, isDark: Boolean) {
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        output.tables.forEach { table ->
            Column {
                Text(
                    text = table.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                val colWidths = table.headers.mapIndexed { i, _ ->
                    if (i == 0) 150.dp else 105.dp
                }

                SpssTableLayout(
                    headers = table.headers,
                    widths = colWidths,
                    rows = table.rows,
                    isDark = isDark
                )

                if (table.footnotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    table.footnotes.forEach { fn ->
                        Text(
                            text = fn,
                            fontSize = 10.sp,
                            color = textSecondary,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }

        if (output.notes.isNotBlank()) {
            Text(
                text = output.notes,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
