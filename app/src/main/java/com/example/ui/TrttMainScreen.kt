package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ChartOutput
import com.example.ui.components.*

@Composable
fun TrttMainScreen(
    viewModel: TrttViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataset by viewModel.dataset.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val activeWindow by viewModel.activeWindow.collectAsStateWithLifecycle()
    val selectedRow by viewModel.selectedRow.collectAsStateWithLifecycle()
    val selectedCol by viewModel.selectedCol.collectAsStateWithLifecycle()
    val showStatusBar by viewModel.showStatusBar.collectAsStateWithLifecycle()
    val showValueLabels by viewModel.showValueLabels.collectAsStateWithLifecycle()
    val activeDialog by viewModel.activeDialog.collectAsStateWithLifecycle()
    val editingCell by viewModel.editingCell.collectAsStateWithLifecycle()
    val variableEditTarget by viewModel.variableEditTarget.collectAsStateWithLifecycle()
    val syntaxCode by viewModel.syntaxCode.collectAsStateWithLifecycle()
    val outputList by viewModel.outputList.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    var activeMenu by remember { mutableStateOf<MenuId?>(null) }
    var pendingChartForSave by remember { mutableStateOf<ChartOutput?>(null) }

    // Android File Manager Activity Result Launchers
    // 1. Open Document (Full XLSX, CSV, TSV, text support)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_data.xlsx"
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    viewModel.importStream(stream, fileName)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 2. Save / Export Dataset CSV to Android File Manager
    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    val csvText = viewModel.getDatasetCsvContent()
                    stream.bufferedWriter().use { it.write(csvText) }
                    Toast.makeText(context, "Saved dataset to Android Storage!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 3. Save / Export Output Report Text to Android File Manager
    val saveReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    val reportText = viewModel.getOutputsReportContent()
                    stream.bufferedWriter().use { it.write(reportText) }
                    Toast.makeText(context, "Exported analysis report successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error exporting report: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 4. Save Graph Picture as PNG to Android Storage
    val saveChartLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null && pendingChartForSave != null) {
            try {
                val pngBytes = viewModel.exportChartToPng(pendingChartForSave!!)
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(pngBytes)
                    Toast.makeText(context, "Saved graph picture successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving graph picture: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Show toast on userMessage changes
    LaunchedEffect(userMessage) {
        userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    // Colors matching pure black with subtle shades requested by user
    val appBg = if (isDark) Color(0xFF000000) else Color(0xFFF1F5F9)
    val tabStripBg = if (isDark) Color(0xFF0A0A0A) else Color(0xFFE2E8F0)
    val statusBarBg = if (isDark) Color(0xFF0A0A0A) else Color(0xFFF1F5F9)
    val statusBorder = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
    val statusText = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("trtt_main_scaffold"),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(appBg)
        ) {
            // 1. Top Menu Bar
            TopMenuBar(
                activeMenu = activeMenu,
                onMenuToggle = { activeMenu = it },
                onAction = { action ->
                    when (action) {
                        "FILE_OPEN_SAF" -> {
                            openDocumentLauncher.launch(arrayOf(
                                "*/*",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "text/csv",
                                "text/tab-separated-values",
                                "text/plain"
                            ))
                        }
                        "FILE_SAVE_SAF" -> {
                            val defaultName = dataset.name.substringBeforeLast('.') + ".csv"
                            saveCsvLauncher.launch(defaultName)
                        }
                        "FILE_EXPORT_OUTPUT_SAF" -> {
                            saveReportLauncher.launch("TRTT_Analysis_Report.txt")
                        }
                        else -> viewModel.onMenuAction(action)
                    }
                },
                showStatusBar = showStatusBar,
                onToggleStatusBar = { viewModel.toggleStatusBar() },
                showValueLabels = showValueLabels,
                onToggleValueLabels = { viewModel.toggleValueLabels() },
                isDark = isDark,
                onToggleTheme = { viewModel.toggleTheme() }
            )

            // Previous session banner has been removed as requested! App now opens seamlessly on the last file.

            // 2. Sub-window tabs (Data Editor, Variable View, Syntax, Output)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tabStripBg)
                    .padding(start = 8.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                WindowTab(
                    label = "Data Editor",
                    isActive = activeWindow == ActiveWindow.DATA_EDITOR,
                    isDark = isDark,
                    onClick = { viewModel.setActiveWindow(ActiveWindow.DATA_EDITOR) },
                    testTag = "window_tab_data_editor"
                )
                WindowTab(
                    label = "Variable View",
                    isActive = activeWindow == ActiveWindow.VARIABLE_VIEW,
                    isDark = isDark,
                    onClick = { viewModel.setActiveWindow(ActiveWindow.VARIABLE_VIEW) },
                    testTag = "window_tab_variable_view"
                )
                WindowTab(
                    label = "Syntax",
                    isActive = activeWindow == ActiveWindow.SYNTAX,
                    isDark = isDark,
                    onClick = { viewModel.setActiveWindow(ActiveWindow.SYNTAX) },
                    testTag = "window_tab_syntax"
                )
                WindowTab(
                    label = "Output (${outputList.size})",
                    isActive = activeWindow == ActiveWindow.OUTPUT,
                    isDark = isDark,
                    onClick = { viewModel.setActiveWindow(ActiveWindow.OUTPUT) },
                    testTag = "window_tab_output"
                )
            }

            // 3. Main Body Workspace
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeWindow) {
                    ActiveWindow.DATA_EDITOR -> {
                        DataGrid(
                            dataset = dataset,
                            selectedRow = selectedRow,
                            selectedCol = selectedCol,
                            isDark = isDark,
                            onCellSelect = { r, c -> viewModel.selectCell(r, c) },
                            onCellDoubleTap = { r, c, v -> viewModel.openCellEditor(r, c, v) },
                            showValueLabels = showValueLabels
                        )
                    }
                    ActiveWindow.VARIABLE_VIEW -> {
                        VariableViewGrid(
                            dataset = dataset,
                            isDark = isDark,
                            onEditProperty = { idx, prop, curr ->
                                viewModel.openVariablePropertyEditor(idx, prop, curr)
                            },
                            onAddVariable = { viewModel.addNewVariable() },
                            onDeleteVariable = { viewModel.deleteVariable(it) }
                        )
                    }
                    ActiveWindow.SYNTAX -> {
                        SyntaxEditor(
                            syntaxText = syntaxCode,
                            isDark = isDark,
                            onSyntaxChange = { viewModel.setSyntaxCode(it) },
                            onRunSyntax = { viewModel.runSyntax(it) }
                        )
                    }
                    ActiveWindow.OUTPUT -> {
                        OutputViewer(
                            outputs = outputList,
                            isDark = isDark,
                            onClearOutputs = { viewModel.clearOutputs() },
                            onExportOutput = {
                                saveReportLauncher.launch("Statsys_Analysis_Report.txt")
                            },
                            onExportChartImage = { chart ->
                                pendingChartForSave = chart
                                val safeTitle = chart.title.replace(Regex("[^a-zA-Z0-9_]"), "_").trim('_')
                                saveChartLauncher.launch("${safeTitle}.png")
                            }
                        )
                    }
                }
            }

            // 4. Status Bar
            if (showStatusBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(statusBarBg)
                        .border(width = 0.5.dp, color = statusBorder)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Statsys Processor is ready",
                        fontSize = 11.sp,
                        color = statusText
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Engine",
                            fontSize = 11.sp,
                            color = statusText
                        )
                        Text(
                            text = "Cases: ${dataset.rowCount}",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Weight Off",
                            fontSize = 11.sp,
                            color = statusText
                        )
                    }
                }
            }
        }
    }

    // Statistical, Graph & File Manager Dialogs
    when (activeDialog) {
        DialogType.DESCRIPTIVES -> {
            DescriptivesDialog(
                dataset = dataset,
                isDark = isDark,
                onDismiss = { viewModel.dismissDialog() },
                onRun = { vars -> viewModel.runDescriptives(vars) },
                onPaste = { vars ->
                    viewModel.setSyntaxCode("DESCRIPTIVES VARIABLES=${vars.joinToString(" ")}\n  /STATISTICS=MEAN STDDEV MIN MAX.")
                    viewModel.setActiveWindow(ActiveWindow.SYNTAX)
                }
            )
        }
        DialogType.FREQUENCIES -> {
            FrequenciesDialog(
                dataset = dataset,
                isDark = isDark,
                onDismiss = { viewModel.dismissDialog() },
                onRun = { vars -> viewModel.runFrequencies(vars) }
            )
        }
        DialogType.IND_T_TEST -> {
            IndependentTTestDialog(
                dataset = dataset,
                isDark = isDark,
                onDismiss = { viewModel.dismissDialog() },
                onRun = { vars, groupVar, g1, g2 -> viewModel.runIndependentTTest(vars, groupVar, g1, g2) }
            )
        }
        DialogType.ANOVA -> {
            OneWayAnovaDialog(
                dataset = dataset,
                isDark = isDark,
                onDismiss = { viewModel.dismissDialog() },
                onRun = { dep, fac -> viewModel.runOneWayAnova(dep, fac) }
            )
        }
        DialogType.CORRELATIONS -> {
            BivariateCorrDialog(
                dataset = dataset,
                isDark = isDark,
                onDismiss = { viewModel.dismissDialog() },
                onRun = { vars -> viewModel.runCorrelations(vars) }
            )
        }
        DialogType.REGRESSION -> {
            LinearRegressionDialog(
                dataset = dataset,
                isDark = isDark,
                onDismiss = { viewModel.dismissDialog() },
                onRun = { dep, indep -> viewModel.runLinearRegression(dep, indep) }
            )
        }

        // Dedicated Graph Popup Dialogs
        DialogType.GRAPH_HISTOGRAM -> {
            HistogramDialog(
                dataset = dataset,
                isDark = isDark,
                onDismiss = { viewModel.dismissDialog() },
                onRun = { varName, withNormal -> viewModel.runHistogram(varName, withNormal) },
                onPaste = { varName ->
                    viewModel.setSyntaxCode("GRAPH /HISTOGRAM=$varName.")
                    viewModel.setActiveWindow(ActiveWindow.SYNTAX)
                }
            )
        }
        DialogType.GRAPH_SCATTER -> {
            ScatterPlotDialog(
                dataset = dataset,
                isDark = isDark,
                onDismiss = { viewModel.dismissDialog() },
                onRun = { x, y -> viewModel.runScatter(x, y) },
                onPaste = { x, y ->
                    viewModel.setSyntaxCode("GRAPH /SCATTERPLOT(BIVAR)=$y WITH $x.")
                    viewModel.setActiveWindow(ActiveWindow.SYNTAX)
                }
            )
        }
        DialogType.GRAPH_BAR -> {
            BarChartDialog(
                dataset = dataset,
                isDark = isDark,
                onDismiss = { viewModel.dismissDialog() },
                onRun = { varName -> viewModel.runBar(varName) }
            )
        }
        DialogType.GRAPH_PIE -> {
            PieChartDialog(
                dataset = dataset,
                isDark = isDark,
                onDismiss = { viewModel.dismissDialog() },
                onRun = { varName -> viewModel.runPie(varName) }
            )
        }

        // Variable Property Editor Dialog
        DialogType.VARIABLE_PROP_EDIT -> {
            variableEditTarget?.let { target ->
                VariablePropertyEditDialog(
                    target = target,
                    isDark = isDark,
                    onDismiss = { viewModel.dismissDialog() },
                    onSave = { idx, prop, newVal ->
                        viewModel.applyVariablePropertyEdit(idx, prop, newVal)
                    },
                    onDelete = { idx ->
                        viewModel.deleteVariable(idx)
                    }
                )
            }
        }

        DialogType.IMPORT_DATA -> {
            ImportDataDialog(
                isDark = isDark,
                onDismiss = { viewModel.dismissDialog() },
                onLaunchFileManager = {
                    openDocumentLauncher.launch(arrayOf(
                        "*/*",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel",
                        "text/csv",
                        "text/tab-separated-values",
                        "text/plain"
                    ))
                },
                onLoadMasterTrauma = { viewModel.loadPresetTrauma() },
                onLoadIris = { viewModel.loadPresetIris() },
                onLoadStudents = { viewModel.loadPresetStudents() },
                onPasteCsv = { rawText -> viewModel.importDelimitedText(rawText, "Pasted_Data.csv") }
            )
        }
        DialogType.CELL_EDIT -> {
            editingCell?.let { (r, c, v) ->
                val varName = dataset.variables.getOrNull(c)?.name ?: "VAR"
                CellEditDialog(
                    currentValue = v,
                    row = r,
                    varName = varName,
                    isDark = isDark,
                    onDismiss = { viewModel.dismissDialog() },
                    onSave = { newVal -> viewModel.updateCell(r, c, newVal) }
                )
            }
        }
        DialogType.ABOUT -> {
            AboutTrttDialog(isDark = isDark, onDismiss = { viewModel.dismissDialog() })
        }
        else -> {}
    }
}

@Composable
private fun WindowTab(
    label: String,
    isActive: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val activeBg = if (isDark) Color(0xFF18181B) else Color.White
    val inactiveBg = if (isDark) Color(0xFF09090B) else Color(0xFFE2E8F0)
    val borderCol = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
    val activeText = if (isDark) Color(0xFFFAFAFA) else Color(0xFF1E293B)
    val inactiveText = if (isDark) Color(0xFF71717A) else Color(0xFF64748B)

    Box(
        modifier = Modifier
            .testTag(testTag)
            .background(
                color = if (isActive) activeBg else inactiveBg,
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
            )
            .border(
                width = 0.5.dp,
                color = borderCol,
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) activeText else inactiveText
        )
    }
}
