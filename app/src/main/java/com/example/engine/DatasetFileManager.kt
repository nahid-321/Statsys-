package com.example.engine

import com.example.model.*

object DatasetFileManager {

    fun importStream(inputStream: java.io.InputStream, fileName: String): Dataset? {
        val bytes = inputStream.readBytes()
        val isZip = bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
        if (isZip || fileName.endsWith(".xlsx", ignoreCase = true)) {
            try {
                val parsed = parseXlsx(java.io.ByteArrayInputStream(bytes), fileName)
                if (parsed != null && parsed.rowCount > 0) return parsed
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // If it's a binary ZIP archive, never parse as plain text CSV
            if (isZip) return null
        }
        val text = String(bytes, Charsets.UTF_8)
        // If text contains binary control characters or null bytes, it's not text
        if (text.contains('\u0000') || text.startsWith("PK")) return null
        return parseDelimitedText(text, fileName)
    }

    fun parseXlsx(inputStream: java.io.InputStream, fileName: String = "Imported_Data.xlsx"): Dataset? {
        val zip = java.util.zip.ZipInputStream(inputStream)
        var entry = zip.nextEntry
        val entries = mutableMapOf<String, ByteArray>()

        while (entry != null) {
            val name = entry.name.replace('\\', '/').trimStart('/').lowercase()
            if (name.contains("sharedstrings.xml") || (name.contains("sheet") && name.endsWith(".xml"))) {
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(4096)
                var len = zip.read(buf)
                while (len > 0) {
                    out.write(buf, 0, len)
                    len = zip.read(buf)
                }
                entries[name] = out.toByteArray()
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }

        val docBuilder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()

        // 1. Parse sharedStrings.xml
        val sharedStrings = mutableListOf<String>()
        val ssKey = entries.keys.firstOrNull { it.contains("sharedstrings.xml") }
        val ssBytes = if (ssKey != null) entries[ssKey] else null

        if (ssBytes != null) {
            try {
                val doc = docBuilder.parse(java.io.ByteArrayInputStream(ssBytes))
                val siList = doc.getElementsByTagName("si")
                for (i in 0 until siList.length) {
                    val item = siList.item(i)
                    sharedStrings.add(item.textContent ?: "")
                }
            } catch (_: Exception) {}
        }

        // 2. Find first worksheet (e.g. xl/worksheets/sheet1.xml)
        val sheetKey = entries.keys.firstOrNull { it.contains("worksheets/sheet") }
            ?: entries.keys.firstOrNull { it.contains("sheet") && it.endsWith(".xml") }
            ?: return null
        val sheetBytes = entries[sheetKey] ?: return null

        val rowsMap = sortedMapOf<Int, MutableMap<Int, String>>()

        try {
            val sheetDoc = docBuilder.parse(java.io.ByteArrayInputStream(sheetBytes))
            val rowList = sheetDoc.getElementsByTagName("row")

            for (r in 0 until rowList.length) {
                val rowElem = rowList.item(r) as? org.w3c.dom.Element ?: continue
                val rAttr = rowElem.getAttribute("r")
                val rowIdx = (rAttr.toIntOrNull()?.minus(1)) ?: (if (rowsMap.isEmpty()) 0 else rowsMap.lastKey() + 1)
                val rowMap = rowsMap.getOrPut(rowIdx) { mutableMapOf() }

                val cList = rowElem.getElementsByTagName("c")
                for (c in 0 until cList.length) {
                    val cElem = cList.item(c) as? org.w3c.dom.Element ?: continue
                    val rRef = cElem.getAttribute("r")
                    val cellType = cElem.getAttribute("t")

                    val colIdx = if (rRef.isNotBlank()) {
                        val colLetters = rRef.takeWhile { it.isLetter() }
                        colLettersToIndex(colLetters)
                    } else {
                        (rowMap.keys.maxOrNull() ?: -1) + 1
                    }

                    val vList = cElem.getElementsByTagName("v")
                    val rawVal = if (vList.length > 0) {
                        vList.item(0).textContent ?: ""
                    } else {
                        val tList = cElem.getElementsByTagName("t")
                        if (tList.length > 0) tList.item(0).textContent ?: "" else ""
                    }

                    val resolved = when (cellType) {
                        "s" -> {
                            val idx = rawVal.toIntOrNull()
                            if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else rawVal
                        }
                        "b" -> if (rawVal == "1") "True" else "False"
                        else -> rawVal
                    }

                    if (colIdx >= 0) {
                        rowMap[colIdx] = resolved
                    }
                }
            }
        } catch (_: Exception) {
            return null
        }

        if (rowsMap.isEmpty()) return null
        val minRowIdx = rowsMap.firstKey()
        val maxRowIdx = rowsMap.lastKey()
        val maxColCount = rowsMap.values.maxOfOrNull { (it.keys.maxOrNull() ?: -1) + 1 } ?: 0
        if (maxColCount == 0) return null

        val headerRowMap = rowsMap[minRowIdx] ?: emptyMap()
        val dataRows = mutableListOf<List<String>>()

        for (r in (minRowIdx + 1)..maxRowIdx) {
            val rMap = rowsMap[r] ?: emptyMap()
            if (rMap.values.any { it.isNotBlank() }) {
                val rowCells = (0 until maxColCount).map { c -> rMap[c] ?: "" }
                dataRows.add(rowCells)
            }
        }

        val variables = (0 until maxColCount).map { c ->
            val rawName = headerRowMap[c]?.trim() ?: ""
            val cleanName = rawName.replace(" ", "_").ifBlank { "VAR${c + 1}" }
            val colVals = dataRows.mapNotNull { if (c < it.size) it[c].trim() else null }.filter { it.isNotBlank() }
            val isNumeric = colVals.isNotEmpty() && colVals.all { it.toDoubleOrNull() != null }

            VariableDefinition(
                id = "v_${c}_${System.currentTimeMillis()}",
                name = cleanName,
                type = if (isNumeric) VariableType.NUMERIC else VariableType.STRING,
                decimals = if (isNumeric) 2 else 0,
                align = if (isNumeric) AlignmentType.RIGHT else AlignmentType.LEFT,
                measure = if (isNumeric) MeasureType.SCALE else MeasureType.NOMINAL
            )
        }

        return Dataset(
            name = fileName,
            variables = variables,
            rows = dataRows
        )
    }

    private fun colLettersToIndex(letters: String): Int {
        var result = 0
        for (ch in letters.uppercase()) {
            if (ch in 'A'..'Z') {
                result = result * 26 + (ch - 'A' + 1)
            }
        }
        return result - 1
    }

    fun parseDelimitedText(text: String, fileName: String = "Imported_Data.csv"): Dataset? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return null

        // Detect delimiter: comma, tab, or semicolon
        val firstLine = lines[0]
        val delimiter = when {
            firstLine.count { it == '\t' } >= firstLine.count { it == ',' } && firstLine.count { it == '\t' } > 0 -> '\t'
            firstLine.count { it == ';' } > firstLine.count { it == ',' } -> ';'
            else -> ','
        }

        val parsedLines = lines.map { parseLine(it, delimiter) }
        if (parsedLines.isEmpty()) return null

        val headerRow = parsedLines[0]
        val dataRows = parsedLines.drop(1)

        val variables = headerRow.mapIndexed { colIdx, rawColName ->
            val colName = rawColName.trim().removeSurrounding("\"", "'").ifBlank { "VAR${colIdx + 1}" }
            val colValues = dataRows.mapNotNull { if (colIdx < it.size) it[colIdx].trim() else null }.filter { it.isNotBlank() }
            val isNumeric = colValues.isNotEmpty() && colValues.all { it.toDoubleOrNull() != null }

            VariableDefinition(
                id = "v_${colIdx}_${System.currentTimeMillis()}",
                name = colName,
                type = if (isNumeric) VariableType.NUMERIC else VariableType.STRING,
                decimals = if (isNumeric) 2 else 0,
                align = if (isNumeric) AlignmentType.RIGHT else AlignmentType.LEFT,
                measure = if (isNumeric) MeasureType.SCALE else MeasureType.NOMINAL
            )
        }

        return Dataset(
            name = fileName,
            variables = variables,
            rows = dataRows
        )
    }

    private fun parseLine(line: String, delimiter: Char): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '\"' || char == '\'' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> {
                    tokens.add(sb.toString().trim().removeSurrounding("\"", "'"))
                    sb.clear()
                }
                else -> sb.append(char)
            }
        }
        tokens.add(sb.toString().trim().removeSurrounding("\"", "'"))
        return tokens
    }

    fun exportToCsv(dataset: Dataset): String {
        val sb = StringBuilder()
        // Header
        sb.append(dataset.variables.joinToString(",") { escapeCsv(it.name) })
        sb.append("\n")

        // Rows
        for (row in dataset.rows) {
            val formattedRow = dataset.variables.indices.map { colIdx ->
                val cell = if (colIdx < row.size) row[colIdx] else ""
                escapeCsv(cell)
            }
            sb.append(formattedRow.joinToString(","))
            sb.append("\n")
        }
        return sb.toString()
    }

    fun exportReportText(outputs: List<OutputItem>): String {
        val sb = StringBuilder()
        sb.append("======================================================================\n")
        sb.append("STATSYS - STATISTICAL ANALYSIS REPORT\n")
        sb.append("Compatible with IBM SPSS Statistics Standard Output\n")
        sb.append("Generated on: ").append(java.util.Date().toString()).append("\n")
        sb.append("======================================================================\n\n")

        for (item in outputs) {
            sb.append("Command: ").append(item.commandSyntax).append("\n")
            sb.append("Title:   ").append(item.title).append("\n")
            sb.append("----------------------------------------------------------------------\n")
            when (item) {
                is DescriptivesOutput -> {
                    sb.append(String.format("%-20s %-8s %-12s %-12s %-12s %-12s\n", "Variable", "N", "Min", "Max", "Mean", "Std.Dev"))
                    for (s in item.statsList) {
                        sb.append(String.format("%-20s %-8d %-12.2f %-12.2f %-12.2f %-12.2f\n", s.variableName, s.n, s.min, s.max, s.mean, s.stdDev))
                    }
                }
                is FrequenciesOutput -> {
                    for (table in item.tables) {
                        sb.append("Variable: ").append(table.variableName).append("\n")
                        sb.append(String.format("%-20s %-10s %-10s %-12s\n", "Value", "Frequency", "Percent", "Cumulative %"))
                        for (e in table.entries) {
                            sb.append(String.format("%-20s %-10d %-9.1f%% %-11.1f%%\n", e.value, e.frequency, e.percent, e.cumulativePercent))
                        }
                    }
                }
                is IndependentTTestOutput -> {
                    for (res in item.results) {
                        sb.append("Test Variable: ").append(res.testVariable).append(" by ").append(res.groupingVariable).append("\n")
                        sb.append(String.format("t = %.3f, df = %.2f, Sig. (2-tailed) = %.4f\n", res.tStatistic, res.df, res.pValueTwoTailed))
                        sb.append(String.format("Mean Diff = %.2f, Std Error Diff = %.3f, 95%% CI: [%.2f, %.2f]\n", res.meanDifference, res.stdErrorDifference, res.confidenceInterval95.first, res.confidenceInterval95.second))
                    }
                }
                is AnovaOutput -> {
                    for (res in item.results) {
                        sb.append("ANOVA: ").append(res.dependentVariable).append(" by ").append(res.factorVariable).append("\n")
                        for (r in res.anovaRows) {
                            sb.append(String.format("%-18s SS: %-10.2f df: %-4d MS: %-10.2f F: %s Sig: %s\n", r.source, r.sumOfSquares, r.df, r.meanSquare, r.fStatistic?.let { String.format("%.3f", it) } ?: "", r.pValue?.let { String.format("%.4f", it) } ?: ""))
                        }
                    }
                }
                is CorrelationOutput -> {
                    sb.append("Correlations Matrix (Pearson r / Sig):\n")
                    for (v1 in item.variables) {
                        for (v2 in item.variables) {
                            val pair = item.pairs.firstOrNull { (it.var1 == v1 && it.var2 == v2) || (it.var1 == v2 && it.var2 == v1) }
                            if (pair != null) {
                                sb.append(String.format("%s <-> %s: r = %.3f, p = %.4f (N=%d)\n", v1, v2, pair.pearsonR, pair.pValue, pair.n))
                            }
                        }
                    }
                }
                is RegressionOutput -> {
                    for (res in item.results) {
                        sb.append("Model: Dependent = ").append(res.dependentVar).append("\n")
                        sb.append(String.format("R = %.3f, R2 = %.3f, Adj R2 = %.3f, SE = %.3f\n", res.r, res.rSquare, res.adjustedRSquare, res.stdErrorEstimate))
                        for (c in res.coefficients) {
                            sb.append(String.format("  %-16s B = %-8.3f SE = %-8.3f t = %-8.3f Sig = %.4f\n", c.term, c.b, c.stdError, c.t, c.sig))
                        }
                    }
                }
                is ChartOutput -> {
                    sb.append("Chart Type: ").append(item.chartType.name).append(" - ").append(item.statsSummary).append("\n")
                }
                is CustomAnalysisOutput -> {
                    for (t in item.tables) {
                        sb.append(t.title).append("\n")
                        sb.append(t.headers.joinToString(" | ")).append("\n")
                        for (r in t.rows) {
                            sb.append(r.joinToString(" | ")).append("\n")
                        }
                        sb.append("\n")
                    }
                    if (item.notes.isNotBlank()) sb.append("Notes: ").append(item.notes).append("\n")
                }
                is LogOutput -> {
                    sb.append(item.message).append("\n")
                }
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(text: String): String {
        return if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else {
            text
        }
    }

    fun createIrisDataset(): Dataset {
        val vars = listOf(
            VariableDefinition("v1", "Sepal_Length", VariableType.NUMERIC, 8, 2, "Sepal Length in cm", emptyMap(), "None", 8, AlignmentType.RIGHT, MeasureType.SCALE),
            VariableDefinition("v2", "Sepal_Width", VariableType.NUMERIC, 8, 2, "Sepal Width in cm", emptyMap(), "None", 8, AlignmentType.RIGHT, MeasureType.SCALE),
            VariableDefinition("v3", "Petal_Length", VariableType.NUMERIC, 8, 2, "Petal Length in cm", emptyMap(), "None", 8, AlignmentType.RIGHT, MeasureType.SCALE),
            VariableDefinition("v4", "Petal_Width", VariableType.NUMERIC, 8, 2, "Petal Width in cm", emptyMap(), "None", 8, AlignmentType.RIGHT, MeasureType.SCALE),
            VariableDefinition("v5", "Species", VariableType.STRING, 16, 0, "Iris Species", mapOf("Setosa" to "Iris Setosa", "Versicolor" to "Iris Versicolor", "Virginica" to "Iris Virginica"), "None", 8, AlignmentType.LEFT, MeasureType.NOMINAL)
        )
        val rows = mutableListOf<List<String>>()
        // 50 setosa
        for (i in 1..50) rows.add(listOf("5.1", "3.5", "1.4", "0.2", "Setosa"))
        // 50 versicolor
        for (i in 1..50) rows.add(listOf("6.2", "2.9", "4.3", "1.3", "Versicolor"))
        // 50 virginica
        for (i in 1..50) rows.add(listOf("6.7", "3.1", "5.6", "2.1", "Virginica"))

        return Dataset(name = "Iris_Flower_Dataset.csv", variables = vars, rows = rows)
    }

    fun createStudentScoresDataset(): Dataset {
        val vars = listOf(
            VariableDefinition("v1", "StudentID", VariableType.STRING, 8, 0, "Student Identifier", emptyMap(), "None", 8, AlignmentType.LEFT, MeasureType.NOMINAL),
            VariableDefinition("v2", "Gender", VariableType.STRING, 8, 0, "Gender", mapOf("M" to "Male", "F" to "Female"), "None", 8, AlignmentType.CENTER, MeasureType.NOMINAL),
            VariableDefinition("v3", "Test_Prep", VariableType.STRING, 12, 0, "Test Preparation Course", mapOf("Completed" to "Completed", "None" to "None"), "None", 8, AlignmentType.LEFT, MeasureType.NOMINAL),
            VariableDefinition("v4", "Math_Score", VariableType.NUMERIC, 8, 0, "Mathematics Score (0-100)", emptyMap(), "None", 8, AlignmentType.RIGHT, MeasureType.SCALE),
            VariableDefinition("v5", "Reading_Score", VariableType.NUMERIC, 8, 0, "Reading Score (0-100)", emptyMap(), "None", 8, AlignmentType.RIGHT, MeasureType.SCALE),
            VariableDefinition("v6", "Writing_Score", VariableType.NUMERIC, 8, 0, "Writing Score (0-100)", emptyMap(), "None", 8, AlignmentType.RIGHT, MeasureType.SCALE)
        )
        val rows = mutableListOf<List<String>>()
        for (i in 1..80) {
            val prep = if (i % 2 == 0) "Completed" else "None"
            val gender = if (i % 3 == 0) "F" else "M"
            val base = if (prep == "Completed") 78 else 64
            val math = (base + (i % 17) - 8).coerceIn(40, 99)
            val reading = (base + 3 + (i % 13) - 6).coerceIn(45, 100)
            val writing = (base + 2 + (i % 15) - 7).coerceIn(42, 98)
            rows.add(listOf("STU" + i.toString().padStart(3, '0'), gender, prep, "$math", "$reading", "$writing"))
        }
        return Dataset(name = "Students_Academic_Performance.csv", variables = vars, rows = rows)
    }
}
