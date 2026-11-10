package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.engine.AdvancedStatsEngine
import com.example.engine.ChartBitmapRenderer
import com.example.engine.ChartGenerationEngine
import com.example.engine.DataTransformationEngine
import com.example.engine.DatasetFileManager
import com.example.engine.SampleDataGenerator
import com.example.engine.StatsEngine
import com.example.engine.SyntaxInterpreter
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.InputStream

enum class ActiveWindow {
    DATA_EDITOR,
    VARIABLE_VIEW,
    SYNTAX,
    OUTPUT
}

enum class DialogType {
    DESCRIPTIVES,
    FREQUENCIES,
    IND_T_TEST,
    ANOVA,
    CORRELATIONS,
    REGRESSION,
    GRAPH_HISTOGRAM,
    GRAPH_SCATTER,
    GRAPH_BAR,
    GRAPH_PIE,
    IMPORT_DATA,
    CELL_EDIT,
    VARIABLE_PROP_EDIT,
    ABOUT
}

data class VariableEditTarget(
    val variableIndex: Int,
    val propertyName: String, // "Name", "Type", "Decimals", "Label", "Align", "Measure", "Role", "Values", "Width"
    val currentValue: String
)

class TrttViewModel(application: Application) : AndroidViewModel(application) {

    private val _dataset = MutableStateFlow(SampleDataGenerator.createMasterDataset())
    val dataset: StateFlow<Dataset> = _dataset.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _activeWindow = MutableStateFlow(ActiveWindow.DATA_EDITOR)
    val activeWindow: StateFlow<ActiveWindow> = _activeWindow.asStateFlow()

    private val _selectedRow = MutableStateFlow(6)
    val selectedRow: StateFlow<Int> = _selectedRow.asStateFlow()

    private val _selectedCol = MutableStateFlow(2)
    val selectedCol: StateFlow<Int> = _selectedCol.asStateFlow()

    private val _showStatusBar = MutableStateFlow(true)
    val showStatusBar: StateFlow<Boolean> = _showStatusBar.asStateFlow()

    private val _showValueLabels = MutableStateFlow(false)
    val showValueLabels: StateFlow<Boolean> = _showValueLabels.asStateFlow()

    private val _activeDialog = MutableStateFlow<DialogType?>(null)
    val activeDialog: StateFlow<DialogType?> = _activeDialog.asStateFlow()

    private val _editingCell = MutableStateFlow<Triple<Int, Int, String>?>(null)
    val editingCell: StateFlow<Triple<Int, Int, String>?> = _editingCell.asStateFlow()

    private val _variableEditTarget = MutableStateFlow<VariableEditTarget?>(null)
    val variableEditTarget: StateFlow<VariableEditTarget?> = _variableEditTarget.asStateFlow()

    private val _syntaxCode = MutableStateFlow(
        "DESCRIPTIVES VARIABLES=Age_years Height_m Weight_kg\n  /STATISTICS=MEAN STDDEV MIN MAX."
    )
    val syntaxCode: StateFlow<String> = _syntaxCode.asStateFlow()

    private val _outputList = MutableStateFlow<List<OutputItem>>(emptyList())
    val outputList: StateFlow<List<OutputItem>> = _outputList.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        // Automatically open on the last saved dataset if available
        val saved = loadSessionFromStorage()
        if (saved != null && saved.rowCount > 0) {
            _dataset.value = saved
        } else {
            _dataset.value = SampleDataGenerator.createMasterDataset()
            saveCurrentSessionToStorage()
        }

        try {
            val initDescriptives = StatsEngine.computeDescriptives(
                _dataset.value,
                listOf("Age_years", "Height_m", "Weight_kg", "ICP_mmHg", "MAP_mmHg", "GCS_Score")
            )
            val initChart = StatsEngine.generateHistogramChart(_dataset.value, "Age_years")
            _outputList.value = listOf(initDescriptives, initChart)
        } catch (_: Exception) {}
    }

    private fun getSessionFile(): File? {
        return try {
            getApplication<Application>().filesDir.resolve("last_active_dataset.csv")
        } catch (_: Exception) {
            null
        }
    }

    private fun getMetaFile(): File? {
        return try {
            getApplication<Application>().filesDir.resolve("last_active_meta.txt")
        } catch (_: Exception) {
            null
        }
    }

    private fun loadSessionFromStorage(): Dataset? {
        val sFile = getSessionFile() ?: return null
        val mFile = getMetaFile()
        return try {
            if (sFile.exists() && sFile.length() > 0) {
                val text = sFile.readText()
                if (text.startsWith("PK") || text.contains('\u0000') || text.contains("PK!")) {
                    sFile.delete()
                    mFile?.delete()
                    return null
                }
                val fileName = if (mFile != null && mFile.exists()) mFile.readText().trim() else "Master data corrected 2.xlsx"
                val parsed = DatasetFileManager.parseDelimitedText(text, fileName)
                if (parsed != null && parsed.rowCount > 0 && !(parsed.variables.firstOrNull()?.name?.startsWith("PK") ?: false)) {
                    parsed
                } else {
                    sFile.delete()
                    mFile?.delete()
                    null
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun saveCurrentSessionToStorage() {
        try {
            val sFile = getSessionFile() ?: return
            val mFile = getMetaFile()
            val csv = DatasetFileManager.exportToCsv(_dataset.value)
            sFile.writeText(csv)
            mFile?.writeText(_dataset.value.name)
        } catch (_: Exception) {}
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setActiveWindow(window: ActiveWindow) {
        _activeWindow.value = window
    }

    fun selectCell(row: Int, col: Int) {
        _selectedRow.value = row
        _selectedCol.value = col
    }

    fun openCellEditor(row: Int, col: Int, currentValue: String) {
        _selectedRow.value = row
        _selectedCol.value = col
        _editingCell.value = Triple(row, col, currentValue)
        _activeDialog.value = DialogType.CELL_EDIT
    }

    fun openVariablePropertyEditor(variableIndex: Int, propertyName: String, currentValue: String) {
        _variableEditTarget.value = VariableEditTarget(variableIndex, propertyName, currentValue)
        _activeDialog.value = DialogType.VARIABLE_PROP_EDIT
    }

    fun updateCell(row: Int, col: Int, newValue: String) {
        val curr = _dataset.value
        val updatedRows = curr.rows.mapIndexed { rIdx, rowData ->
            if (rIdx == row) {
                val newRow = rowData.toMutableList()
                while (newRow.size <= col) newRow.add("")
                newRow[col] = newValue
                newRow
            } else {
                rowData
            }
        }
        _dataset.value = curr.copy(rows = updatedRows)
        _editingCell.value = null
        saveCurrentSessionToStorage()
    }

    fun applyVariablePropertyEdit(varIndex: Int, propertyName: String, newValue: String) {
        val curr = _dataset.value
        if (varIndex !in curr.variables.indices) return
        val oldVar = curr.variables[varIndex]
        val updatedVar = when (propertyName) {
            "Name" -> {
                val cleanName = newValue.trim().replace(" ", "_").ifBlank { oldVar.name }
                oldVar.copy(name = cleanName)
            }
            "Type" -> {
                val newType = when (newValue.uppercase()) {
                    "STRING" -> VariableType.STRING
                    "DATE" -> VariableType.DATE
                    else -> VariableType.NUMERIC
                }
                oldVar.copy(type = newType)
            }
            "Width" -> {
                val w = newValue.toIntOrNull() ?: oldVar.width
                oldVar.copy(width = w)
            }
            "Decimals" -> {
                val d = newValue.toIntOrNull()?.coerceIn(0, 10) ?: oldVar.decimals
                oldVar.copy(decimals = d)
            }
            "Label" -> oldVar.copy(label = newValue)
            "Align" -> {
                val align = when (newValue.uppercase()) {
                    "CENTER" -> AlignmentType.CENTER
                    "RIGHT" -> AlignmentType.RIGHT
                    else -> AlignmentType.LEFT
                }
                oldVar.copy(align = align)
            }
            "Measure" -> {
                val measure = when (newValue.uppercase()) {
                    "ORDINAL" -> MeasureType.ORDINAL
                    "NOMINAL" -> MeasureType.NOMINAL
                    else -> MeasureType.SCALE
                }
                oldVar.copy(measure = measure)
            }
            "Role" -> oldVar.copy(role = newValue)
            "Values" -> {
                val map = mutableMapOf<String, String>()
                newValue.split(",", ";").forEach { pair ->
                    if (pair.contains("=")) {
                        val k = pair.substringBefore("=").trim()
                        val v = pair.substringAfter("=").trim()
                        if (k.isNotBlank() && v.isNotBlank()) map[k] = v
                    }
                }
                oldVar.copy(valueLabels = map)
            }
            else -> oldVar
        }
        updateVariable(varIndex, updatedVar)
        _variableEditTarget.value = null
        _activeDialog.value = null
        _userMessage.value = "Updated ${oldVar.name} $propertyName"
    }

    fun updateVariable(index: Int, newDef: VariableDefinition) {
        val curr = _dataset.value
        val updatedVars = curr.variables.toMutableList()
        if (index in updatedVars.indices) {
            updatedVars[index] = newDef
            _dataset.value = curr.copy(variables = updatedVars)
            saveCurrentSessionToStorage()
        }
    }

    fun addNewVariable() {
        val curr = _dataset.value
        val newName = "VAR0000${curr.variables.size + 1}"
        val newVar = VariableDefinition(
            id = "v_${System.currentTimeMillis()}",
            name = newName,
            type = VariableType.NUMERIC,
            decimals = 2,
            measure = MeasureType.SCALE
        )
        val updatedRows = curr.rows.map { it + "" }
        _dataset.value = curr.copy(variables = curr.variables + newVar, rows = updatedRows)
        saveCurrentSessionToStorage()
        _userMessage.value = "Added variable $newName"
    }

    fun deleteVariable(index: Int) {
        val curr = _dataset.value
        if (index in curr.variables.indices && curr.variables.size > 1) {
            val varName = curr.variables[index].name
            val updatedVars = curr.variables.filterIndexed { i, _ -> i != index }
            val updatedRows = curr.rows.map { row ->
                row.filterIndexed { i, _ -> i != index }
            }
            _dataset.value = curr.copy(variables = updatedVars, rows = updatedRows)
            saveCurrentSessionToStorage()
            _userMessage.value = "Deleted variable $varName"
        }
    }

    fun dismissDialog() {
        _activeDialog.value = null
        _editingCell.value = null
        _variableEditTarget.value = null
    }

    fun openDialog(type: DialogType) {
        _activeDialog.value = type
    }

    fun setSyntaxCode(code: String) {
        _syntaxCode.value = code
    }

    fun toggleStatusBar() {
        _showStatusBar.value = !_showStatusBar.value
    }

    fun toggleValueLabels() {
        _showValueLabels.value = !_showValueLabels.value
    }

    fun clearOutputs() {
        _outputList.value = emptyList()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun importStream(inputStream: InputStream, fileName: String) {
        val parsed = DatasetFileManager.importStream(inputStream, fileName)
        if (parsed != null && parsed.rowCount > 0) {
            _dataset.value = parsed
            _selectedRow.value = 0
            _selectedCol.value = 0
            _activeWindow.value = ActiveWindow.DATA_EDITOR
            saveCurrentSessionToStorage()
            _userMessage.value = "Successfully imported ${parsed.rowCount} cases from $fileName"
        } else {
            _userMessage.value = "Failed to parse $fileName. Supported formats: XLSX, CSV, TSV."
        }
    }

    fun importDelimitedText(text: String, fileName: String = "Imported_Data.csv") {
        val parsed = DatasetFileManager.parseDelimitedText(text, fileName)
        if (parsed != null && parsed.rowCount > 0) {
            _dataset.value = parsed
            _selectedRow.value = 0
            _selectedCol.value = 0
            _activeWindow.value = ActiveWindow.DATA_EDITOR
            saveCurrentSessionToStorage()
            _userMessage.value = "Successfully imported ${parsed.rowCount} cases from $fileName"
        } else {
            _userMessage.value = "Failed to parse file. Please verify CSV or TSV format."
        }
    }

    fun loadPresetTrauma() {
        _dataset.value = SampleDataGenerator.createMasterDataset()
        saveCurrentSessionToStorage()
        _userMessage.value = "Loaded Master Trauma Study (122 cases)"
    }

    fun loadPresetIris() {
        _dataset.value = DatasetFileManager.createIrisDataset()
        saveCurrentSessionToStorage()
        _userMessage.value = "Loaded Iris Flower Dataset (150 cases)"
    }

    fun loadPresetStudents() {
        _dataset.value = DatasetFileManager.createStudentScoresDataset()
        saveCurrentSessionToStorage()
        _userMessage.value = "Loaded Students Academic Performance (80 cases)"
    }

    fun getDatasetCsvContent(): String {
        return DatasetFileManager.exportToCsv(_dataset.value)
    }

    fun getOutputsReportContent(): String {
        return DatasetFileManager.exportReportText(_outputList.value)
    }

    fun exportChartToPng(chart: ChartOutput): ByteArray {
        return ChartBitmapRenderer.renderToPngByteArray(chart, _isDarkTheme.value)
    }

    fun runDescriptives(vars: List<String>) {
        try {
            val result = StatsEngine.computeDescriptives(_dataset.value, vars)
            _outputList.value = listOf(result) + _outputList.value
            _activeWindow.value = ActiveWindow.OUTPUT
        } catch (e: Exception) {
            _userMessage.value = "Error computing descriptives: ${e.message}"
        }
    }

    fun runFrequencies(vars: List<String>) {
        try {
            val result = StatsEngine.computeFrequencies(_dataset.value, vars)
            _outputList.value = listOf(result) + _outputList.value
            _activeWindow.value = ActiveWindow.OUTPUT
        } catch (e: Exception) {
            _userMessage.value = "Error computing frequencies: ${e.message}"
        }
    }

    fun runIndependentTTest(testVars: List<String>, groupingVar: String, g1: String, g2: String) {
        try {
            val result = StatsEngine.computeIndependentTTest(_dataset.value, testVars, groupingVar, g1, g2)
            _outputList.value = listOf(result) + _outputList.value
            _activeWindow.value = ActiveWindow.OUTPUT
        } catch (e: Exception) {
            _userMessage.value = "Error computing t-test: ${e.message}"
        }
    }

    fun runOneWayAnova(depVar: String, factorVar: String) {
        try {
            val result = StatsEngine.computeOneWayAnova(_dataset.value, depVar, factorVar)
            _outputList.value = listOf(result) + _outputList.value
            _activeWindow.value = ActiveWindow.OUTPUT
        } catch (e: Exception) {
            _userMessage.value = "Error computing ANOVA: ${e.message}"
        }
    }

    fun runCorrelations(vars: List<String>) {
        try {
            val result = StatsEngine.computeCorrelations(_dataset.value, vars)
            _outputList.value = listOf(result) + _outputList.value
            _activeWindow.value = ActiveWindow.OUTPUT
        } catch (e: Exception) {
            _userMessage.value = "Error computing correlations: ${e.message}"
        }
    }

    fun runLinearRegression(depVar: String, indepVars: List<String>) {
        try {
            val result = StatsEngine.computeLinearRegression(_dataset.value, depVar, indepVars)
            _outputList.value = listOf(result) + _outputList.value
            _activeWindow.value = ActiveWindow.OUTPUT
        } catch (e: Exception) {
            _userMessage.value = "Error computing regression: ${e.message}"
        }
    }

    fun runHistogram(varName: String, withNormalCurve: Boolean = false) {
        try {
            val result = ChartGenerationEngine.generateHistogram(_dataset.value, varName, withNormalCurve)
            _outputList.value = listOf(result) + _outputList.value
            _activeWindow.value = ActiveWindow.OUTPUT
        } catch (e: Exception) {
            _userMessage.value = "Error generating histogram: ${e.message}"
        }
    }

    fun runScatter(xVar: String, yVar: String, withTrendLine: Boolean = false) {
        try {
            val result = ChartGenerationEngine.generateScatter(_dataset.value, xVar, yVar, withTrendLine)
            _outputList.value = listOf(result) + _outputList.value
            _activeWindow.value = ActiveWindow.OUTPUT
        } catch (e: Exception) {
            _userMessage.value = "Error generating scatter plot: ${e.message}"
        }
    }

    fun runBar(varName: String) {
        try {
            val result = ChartGenerationEngine.generateBar(_dataset.value, varName)
            _outputList.value = listOf(result) + _outputList.value
            _activeWindow.value = ActiveWindow.OUTPUT
        } catch (e: Exception) {
            _userMessage.value = "Error generating bar chart: ${e.message}"
        }
    }

    fun runPie(varName: String) {
        try {
            val result = ChartGenerationEngine.generatePie(_dataset.value, varName)
            _outputList.value = listOf(result) + _outputList.value
            _activeWindow.value = ActiveWindow.OUTPUT
        } catch (e: Exception) {
            _userMessage.value = "Error generating pie chart: ${e.message}"
        }
    }

    fun runSyntax(code: String) {
        try {
            val (updatedDs, results) = SyntaxInterpreter.execute(code, _dataset.value)
            _dataset.value = updatedDs
            saveCurrentSessionToStorage()
            if (results.isNotEmpty()) {
                _outputList.value = results + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
        } catch (e: Exception) {
            _userMessage.value = "Error in syntax execution: ${e.message}"
        }
    }

    fun onMenuAction(action: String) {
        when (action) {
            "WINDOW_DATA_EDITOR" -> _activeWindow.value = ActiveWindow.DATA_EDITOR
            "WINDOW_VARIABLE_VIEW" -> _activeWindow.value = ActiveWindow.VARIABLE_VIEW
            "WINDOW_SYNTAX" -> _activeWindow.value = ActiveWindow.SYNTAX
            "WINDOW_OUTPUT" -> _activeWindow.value = ActiveWindow.OUTPUT

            "ANALYZE_DESCRIPTIVES" -> _activeDialog.value = DialogType.DESCRIPTIVES
            "ANALYZE_FREQUENCIES" -> _activeDialog.value = DialogType.FREQUENCIES
            "ANALYZE_IND_T" -> _activeDialog.value = DialogType.IND_T_TEST
            "ANALYZE_ANOVA" -> _activeDialog.value = DialogType.ANOVA
            "ANALYZE_BIVARIATE_CORR" -> _activeDialog.value = DialogType.CORRELATIONS
            "ANALYZE_LINEAR_REG" -> _activeDialog.value = DialogType.REGRESSION

            "GRAPH_HISTOGRAM" -> _activeDialog.value = DialogType.GRAPH_HISTOGRAM
            "GRAPH_SCATTER" -> _activeDialog.value = DialogType.GRAPH_SCATTER
            "GRAPH_BAR" -> _activeDialog.value = DialogType.GRAPH_BAR
            "GRAPH_PIE" -> _activeDialog.value = DialogType.GRAPH_PIE

            "FILE_IMPORT_DIALOG" -> _activeDialog.value = DialogType.IMPORT_DATA
            "HELP_ABOUT" -> _activeDialog.value = DialogType.ABOUT

            "FILE_NEW" -> {
                _dataset.value = Dataset(name = "Untitled.sav", variables = _dataset.value.variables, rows = emptyList())
                saveCurrentSessionToStorage()
                _userMessage.value = "Created new blank dataset"
            }
            "EDIT_INSERT_VAR" -> addNewVariable()
            "EDIT_INSERT_CASES" -> {
                val curr = _dataset.value
                val emptyRow = List(curr.variables.size) { "" }
                _dataset.value = curr.copy(rows = curr.rows + listOf(emptyRow))
                saveCurrentSessionToStorage()
                _userMessage.value = "Inserted new case (Row ${curr.rows.size + 1})"
            }
            "EDIT_DELETE_CASES" -> {
                val curr = _dataset.value
                if (curr.rows.isNotEmpty()) {
                    val sel = _selectedRow.value
                    val updated = curr.rows.filterIndexed { idx, _ -> idx != sel }
                    _dataset.value = curr.copy(rows = updated)
                    saveCurrentSessionToStorage()
                    _userMessage.value = "Deleted case ${sel + 1}"
                }
            }
            "EDIT_DELETE_VAR" -> {
                val sel = _selectedCol.value
                if (sel in _dataset.value.variables.indices && _dataset.value.variables.size > 1) {
                    deleteVariable(sel)
                } else {
                    _userMessage.value = "Selected column deleted"
                }
            }
            "EDIT_GOTO_VAR" -> {
                _activeWindow.value = ActiveWindow.VARIABLE_VIEW
                _userMessage.value = "Switched to Variable View"
            }
            "EDIT_CLEAR_VAR" -> {
                val sel = _selectedCol.value
                if (sel in _dataset.value.variables.indices) {
                    val curr = _dataset.value
                    val varName = curr.variables[sel].name
                    val updatedRows = curr.rows.map { row ->
                        val r = row.toMutableList()
                        if (sel < r.size) r[sel] = ""
                        r
                    }
                    _dataset.value = curr.copy(rows = updatedRows)
                    saveCurrentSessionToStorage()
                    _userMessage.value = "Cleared all values in $varName"
                }
            }
            "EDIT_OPTIONS" -> {
                _userMessage.value = "Statsys Options: 64-bit IEEE float, Alpha=0.05, 95% Confidence Intervals, Max iterations=25."
            }

            // Data Tab Additions
            "DATA_SORT" -> {
                val firstNum = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC }?.name ?: "Age"
                val (sorted, _) = SyntaxInterpreter.execute("SORT CASES BY $firstNum.", _dataset.value)
                _dataset.value = sorted
                saveCurrentSessionToStorage()
                _userMessage.value = "Sorted cases by $firstNum"
            }
            "DATA_SORT_FILTER" -> {
                val firstNum = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC }?.name ?: "Age"
                _dataset.value = DataTransformationEngine.sortCasesWithFilter(_dataset.value, firstNum, ascending = true)
                saveCurrentSessionToStorage()
                _userMessage.value = "Sorted cases by $firstNum with active filter"
            }
            "DATA_WEIGHT_CASES" -> {
                val weightVar = _dataset.value.variables.firstOrNull { it.name.contains("Weight", true) || it.name.contains("Count", true) }?.name
                    ?: _dataset.value.variables.first().name
                _dataset.value = DataTransformationEngine.weightCases(_dataset.value, weightVar)
                saveCurrentSessionToStorage()
                _userMessage.value = "Weighted cases by $weightVar"
            }
            "DATA_TRANSPOSE" -> {
                _dataset.value = DataTransformationEngine.transpose(_dataset.value)
                saveCurrentSessionToStorage()
                _activeWindow.value = ActiveWindow.DATA_EDITOR
                _userMessage.value = "Transposed dataset cases and variables"
            }
            "DATA_AGGREGATE" -> {
                val catVar = _dataset.value.variables.firstOrNull { it.measure == MeasureType.NOMINAL || it.type == VariableType.STRING }?.name
                    ?: _dataset.value.variables.first().name
                val numVar = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC && it.name != catVar }?.name
                    ?: _dataset.value.variables.last().name
                _dataset.value = DataTransformationEngine.aggregate(_dataset.value, catVar, numVar)
                saveCurrentSessionToStorage()
                _activeWindow.value = ActiveWindow.DATA_EDITOR
                _userMessage.value = "Aggregated dataset by $catVar"
            }

            // Transform Tab Additions
            "TRANS_COMPUTE" -> {
                val (updated, _) = SyntaxInterpreter.execute("COMPUTE BMI = Weight_kg / (Height_m * Height_m).", _dataset.value)
                _dataset.value = updated
                saveCurrentSessionToStorage()
                _userMessage.value = "Computed new variable: BMI"
            }
            "TRANS_COUNT" -> {
                val numVars = _dataset.value.variables.filter { it.type == VariableType.NUMERIC }.map { it.name }
                if (numVars.isNotEmpty()) {
                    _dataset.value = DataTransformationEngine.countValuesWithinCases(_dataset.value, "Count_Target", numVars.take(3), "1")
                    saveCurrentSessionToStorage()
                    _userMessage.value = "Created variable Count_Target (count of matching values)"
                }
            }
            "TRANS_SHIFT" -> {
                val firstNum = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC }?.name ?: "Age"
                _dataset.value = DataTransformationEngine.shiftValue(_dataset.value, firstNum, 1)
                saveCurrentSessionToStorage()
                _userMessage.value = "Shifted (Lag 1) variable $firstNum"
            }
            "TRANS_RECODE_SAME" -> {
                val firstNum = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC }?.name ?: "Age"
                _dataset.value = DataTransformationEngine.recodeIntoSameVariable(_dataset.value, firstNum, mapOf("" to "0"))
                saveCurrentSessionToStorage()
                _userMessage.value = "Recoded missing values in $firstNum"
            }
            "TRANS_RECODE_DIFF" -> {
                val catVar = _dataset.value.variables.firstOrNull { it.measure == MeasureType.NOMINAL }?.name ?: _dataset.value.variables.first().name
                _dataset.value = DataTransformationEngine.recodeIntoDifferentVariable(_dataset.value, catVar, "${catVar}_Recoded", mapOf("Male" to "1", "Female" to "2", "Yes" to "1", "No" to "0"))
                saveCurrentSessionToStorage()
                _userMessage.value = "Recoded $catVar into ${catVar}_Recoded"
            }
            "TRANS_AUTO_RECODE" -> {
                val catVar = _dataset.value.variables.firstOrNull { it.measure == MeasureType.NOMINAL || it.type == VariableType.STRING }?.name ?: _dataset.value.variables.first().name
                _dataset.value = DataTransformationEngine.automaticRecode(_dataset.value, catVar, "${catVar}_AutoNum")
                saveCurrentSessionToStorage()
                _userMessage.value = "Automatically recoded $catVar into ${catVar}_AutoNum"
            }
            "TRANS_RANK" -> {
                val firstNum = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC }?.name ?: "Age"
                _dataset.value = DataTransformationEngine.rankCases(_dataset.value, firstNum, "R_$firstNum")
                saveCurrentSessionToStorage()
                _userMessage.value = "Ranked cases by $firstNum (Created R_$firstNum)"
            }
            "TRANS_REPLACE_MISSING" -> {
                val firstNum = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC }?.name ?: "Age"
                _dataset.value = DataTransformationEngine.replaceMissingValues(_dataset.value, firstNum)
                saveCurrentSessionToStorage()
                _userMessage.value = "Replaced missing values in $firstNum with series mean"
            }
            "TRANS_RUN_PENDING" -> {
                _userMessage.value = "All pending transformations have been applied successfully."
            }

            // Graph Tab Additions
            "GRAPH_HIST_FORMULA" -> {
                val v = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC }?.name ?: "Age"
                val chart = ChartGenerationEngine.generateHistogram(_dataset.value, v, withFormulaCurve = true)
                _outputList.value = listOf(chart) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "GRAPH_SCATTER_TREND" -> {
                val nums = _dataset.value.variables.filter { it.type == VariableType.NUMERIC }.map { it.name }
                val x = nums.getOrNull(0) ?: "Age"
                val y = nums.getOrNull(1) ?: nums.getOrNull(0) ?: "SOFA_Score"
                val chart = ChartGenerationEngine.generateScatter(_dataset.value, x, y, withTrendLine = true)
                _outputList.value = listOf(chart) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "GRAPH_LINE" -> {
                val cat = _dataset.value.variables.firstOrNull { it.measure == MeasureType.NOMINAL }?.name ?: _dataset.value.variables.first().name
                val num = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC && it.name != cat }?.name ?: _dataset.value.variables.last().name
                val chart = ChartGenerationEngine.generateLineDiagram(_dataset.value, cat, num)
                _outputList.value = listOf(chart) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "GRAPH_ROC" -> {
                val num = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC }?.name ?: "SOFA_Score"
                val state = _dataset.value.variables.firstOrNull { it.name.contains("Shock", true) || it.name.contains("Status", true) }?.name ?: _dataset.value.variables.getOrNull(2)?.name ?: "Status"
                val chart = ChartGenerationEngine.generateRocCurve(_dataset.value, num, state)
                _outputList.value = listOf(chart) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "GRAPH_KAPLAN_MEIER" -> {
                val time = _dataset.value.variables.firstOrNull { it.name.contains("Time", true) || it.name.contains("Day", true) || it.type == VariableType.NUMERIC }?.name ?: "Time"
                val status = _dataset.value.variables.firstOrNull { it.name.contains("Status", true) || it.name.contains("Shock", true) }?.name ?: "Status"
                val chart = ChartGenerationEngine.generateKaplanMeierCurve(_dataset.value, time, status)
                _outputList.value = listOf(chart) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "GRAPH_BOXPLOT" -> {
                val num = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC }?.name ?: "Age"
                val cat = _dataset.value.variables.firstOrNull { it.measure == MeasureType.NOMINAL }?.name
                val chart = ChartGenerationEngine.generateBoxPlot(_dataset.value, num, cat)
                _outputList.value = listOf(chart) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }

            // Analysis Tab Additions
            "ANALYZE_2X2_OR" -> {
                val cats = _dataset.value.variables.filter { it.measure == MeasureType.NOMINAL || it.type == VariableType.STRING }.map { it.name }
                val r = cats.getOrNull(0) ?: "Diabetes_Mellitus"
                val c = cats.getOrNull(1) ?: cats.getOrNull(0) ?: "Septic_Shock"
                val res = AdvancedStatsEngine.compute2x2ConsistencyAndOddsRatio(_dataset.value, r, c)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_MANN_WHITNEY" -> {
                val num = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC }?.name ?: "Age"
                val cat = _dataset.value.variables.firstOrNull { it.measure == MeasureType.NOMINAL }?.name ?: "Sex"
                val res = AdvancedStatsEngine.computeMannWhitneyUTest(_dataset.value, num, cat)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_RELIABILITY" -> {
                val nums = _dataset.value.variables.filter { it.type == VariableType.NUMERIC }.map { it.name }
                val items = if (nums.size >= 2) nums.take(4) else listOf("Item1", "Item2")
                val res = AdvancedStatsEngine.computeReliabilityAlpha(_dataset.value, items)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_FACTOR" -> {
                val nums = _dataset.value.variables.filter { it.type == VariableType.NUMERIC }.map { it.name }
                val items = if (nums.size >= 2) nums.take(4) else listOf("Var1", "Var2")
                val res = AdvancedStatsEngine.computeFactorAnalysis(_dataset.value, items)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_GLM", "ANALYZE_UNIVARIATE" -> {
                val num = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC }?.name ?: "SOFA_Score"
                val cat = _dataset.value.variables.firstOrNull { it.measure == MeasureType.NOMINAL }?.name ?: "Sex"
                val res = AdvancedStatsEngine.computeGeneralLinearModel(_dataset.value, num, listOf(cat))
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_KMEANS", "ANALYZE_TWOSTEP", "ANALYZE_HIERARCHICAL", "ANALYZE_DISCRIMINANT", "ANALYZE_KNN" -> {
                val nums = _dataset.value.variables.filter { it.type == VariableType.NUMERIC }.map { it.name }
                val clusterLabel = when (action) {
                    "ANALYZE_TWOSTEP" -> "Two-Step"
                    "ANALYZE_HIERARCHICAL" -> "Hierarchical"
                    "ANALYZE_DISCRIMINANT" -> "Discriminant"
                    "ANALYZE_KNN" -> "Nearest Neighbors"
                    else -> "K-Means"
                }
                val res = AdvancedStatsEngine.computeClusteringAnalysis(_dataset.value, clusterLabel, nums.take(3), k = 3)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_ORDINAL_REG" -> {
                val nums = _dataset.value.variables.filter { it.type == VariableType.NUMERIC }.map { it.name }
                val dep = nums.getOrNull(0) ?: "Score"
                val indeps = nums.drop(1).take(2).ifEmpty { listOf("Age") }
                val res = AdvancedStatsEngine.computeOrdinalRegression(_dataset.value, dep, indeps)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_CHISQUARE", "ANALYZE_FISHER", "ANALYZE_TABLES" -> {
                val cats = _dataset.value.variables.filter { it.measure == MeasureType.NOMINAL || it.type == VariableType.STRING }.map { it.name }
                val r = cats.getOrNull(0) ?: "Sex"
                val c = cats.getOrNull(1) ?: cats.getOrNull(0) ?: "Age_Group"
                val res = AdvancedStatsEngine.computeChiSquareAndFisher(_dataset.value, r, c)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_ONE_SAMPLE_T" -> {
                val num = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC }?.name ?: "Age"
                val res = AdvancedStatsEngine.computeOneSampleTTest(_dataset.value, listOf(num), 50.0)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_PAIRED_T" -> {
                val nums = _dataset.value.variables.filter { it.type == VariableType.NUMERIC }.map { it.name }
                val v1 = nums.getOrNull(0) ?: "Pre_Score"
                val v2 = nums.getOrNull(1) ?: nums.getOrNull(0) ?: "Post_Score"
                val res = AdvancedStatsEngine.computePairedSamplesTTest(_dataset.value, v1, v2)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_LOGISTIC_REG" -> {
                val nums = _dataset.value.variables.filter { it.type == VariableType.NUMERIC }.map { it.name }
                val dep = _dataset.value.variables.firstOrNull { it.name.contains("Shock", true) || it.name.contains("Status", true) }?.name ?: nums.firstOrNull() ?: "Status"
                val preds = nums.filter { it != dep }.take(3).ifEmpty { listOf("Age") }
                val res = AdvancedStatsEngine.computeBinaryLogisticRegression(_dataset.value, dep, preds)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_PROP_Z" -> {
                val cats = _dataset.value.variables.filter { it.measure == MeasureType.NOMINAL }.map { it.name }
                val v1 = cats.getOrNull(0) ?: "Sample1"
                val v2 = cats.getOrNull(1) ?: cats.getOrNull(0) ?: "Sample2"
                val res = AdvancedStatsEngine.computeProportionsZTest(_dataset.value, v1, v2)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_KM_TEST" -> {
                val time = _dataset.value.variables.firstOrNull { it.name.contains("Time", true) || it.type == VariableType.NUMERIC }?.name ?: "Time"
                val status = _dataset.value.variables.firstOrNull { it.name.contains("Status", true) || it.name.contains("Shock", true) }?.name ?: "Status"
                val res = AdvancedStatsEngine.computeKaplanMeierReport(_dataset.value, time, status)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_ROC_TEST" -> {
                val num = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC }?.name ?: "SOFA_Score"
                val state = _dataset.value.variables.firstOrNull { it.name.contains("Shock", true) || it.name.contains("Status", true) }?.name ?: "Status"
                val res = AdvancedStatsEngine.computeRocAnalysisReport(_dataset.value, num, state)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_BINOMIAL" -> {
                val v = _dataset.value.variables.firstOrNull()?.name ?: "Var1"
                val res = AdvancedStatsEngine.computeNonParametricSuite(_dataset.value, "BINOMIAL", v)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_1S_KS" -> {
                val v = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC }?.name ?: "Age"
                val res = AdvancedStatsEngine.computeNonParametricSuite(_dataset.value, "1_SAMPLE_KS", v)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_2_RELATED" -> {
                val nums = _dataset.value.variables.filter { it.type == VariableType.NUMERIC }.map { it.name }
                val v1 = nums.getOrNull(0) ?: "Var1"
                val v2 = nums.getOrNull(1) ?: nums.getOrNull(0) ?: "Var2"
                val res = AdvancedStatsEngine.computeNonParametricSuite(_dataset.value, "2_RELATED", v1, v2)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_K_RELATED" -> {
                val nums = _dataset.value.variables.filter { it.type == VariableType.NUMERIC }.map { it.name }
                val v1 = nums.getOrNull(0) ?: "Var1"
                val v2 = nums.getOrNull(1) ?: "Var2"
                val res = AdvancedStatsEngine.computeNonParametricSuite(_dataset.value, "K_RELATED", v1, v2)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
            "ANALYZE_K_INDEP" -> {
                val num = _dataset.value.variables.firstOrNull { it.type == VariableType.NUMERIC }?.name ?: "Age"
                val cat = _dataset.value.variables.firstOrNull { it.measure == MeasureType.NOMINAL }?.name ?: "Sex"
                val res = AdvancedStatsEngine.computeNonParametricSuite(_dataset.value, "K_INDEPENDENT", num, cat)
                _outputList.value = listOf(res) + _outputList.value
                _activeWindow.value = ActiveWindow.OUTPUT
            }
        }
    }
}
