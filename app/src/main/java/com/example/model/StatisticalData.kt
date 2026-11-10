package com.example.model

enum class VariableType {
    NUMERIC,
    STRING,
    DATE
}

enum class MeasureType {
    SCALE,
    ORDINAL,
    NOMINAL
}

enum class AlignmentType {
    LEFT,
    RIGHT,
    CENTER
}

data class VariableDefinition(
    val id: String,
    val name: String,
    val type: VariableType = VariableType.NUMERIC,
    val width: Int = 8,
    val decimals: Int = if (type == VariableType.NUMERIC) 2 else 0,
    val label: String = "",
    val valueLabels: Map<String, String> = emptyMap(),
    val missing: String = "None",
    val columns: Int = 8,
    val align: AlignmentType = if (type == VariableType.NUMERIC) AlignmentType.RIGHT else AlignmentType.LEFT,
    val measure: MeasureType = if (type == VariableType.NUMERIC) MeasureType.SCALE else MeasureType.NOMINAL,
    val role: String = "Input"
)

data class Dataset(
    val name: String = "Master data corrected 2.xlsx",
    val variables: List<VariableDefinition>,
    val rows: List<List<String>> // rows of string values corresponding to variables
) {
    val rowCount: Int get() = rows.size
    val columnCount: Int get() = variables.size

    fun getColumnValues(variableIndex: Int): List<String> {
        if (variableIndex !in variables.indices) return emptyList()
        return rows.map { row ->
            if (variableIndex < row.size) row[variableIndex] else ""
        }
    }

    fun getNumericColumnValues(variableIndex: Int): List<Double> {
        return getColumnValues(variableIndex).mapNotNull { it.trim().toDoubleOrNull() }
    }

    fun findVariableIndex(varName: String): Int {
        return variables.indexOfFirst { it.name.equals(varName, ignoreCase = true) }
    }
}
