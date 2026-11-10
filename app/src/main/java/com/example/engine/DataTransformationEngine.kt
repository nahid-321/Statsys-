package com.example.engine

import com.example.model.Dataset
import com.example.model.MeasureType
import com.example.model.VariableDefinition
import com.example.model.VariableType
import kotlin.math.*

object DataTransformationEngine {

    // 1. Weight Cases
    fun weightCases(dataset: Dataset, weightVarName: String): Dataset {
        val wIdx = dataset.findVariableIndex(weightVarName)
        if (wIdx == -1) return dataset
        val newRows = mutableListOf<List<String>>()
        for (row in dataset.rows) {
            val weight = if (wIdx < row.size) row[wIdx].toIntOrNull() ?: 1 else 1
            val repeatCount = max(1, min(20, weight))
            for (i in 0 until repeatCount) {
                newRows.add(row)
            }
        }
        return dataset.copy(rows = newRows)
    }

    // 2. Transpose Dataset
    fun transpose(dataset: Dataset): Dataset {
        if (dataset.rows.isEmpty() || dataset.variables.isEmpty()) return dataset
        val numOldRows = dataset.rows.size
        val numOldCols = dataset.variables.size

        val newVariables = mutableListOf<VariableDefinition>()
        newVariables.add(
            VariableDefinition(
                id = "v_trans_orig",
                name = "Original_Variable",
                label = "Original Variable Name",
                type = VariableType.STRING,
                measure = MeasureType.NOMINAL
            )
        )
        for (r in 0 until min(50, numOldRows)) {
            newVariables.add(
                VariableDefinition(
                    id = "v_trans_$r",
                    name = "Case_${r + 1}",
                    label = "Case ${r + 1}",
                    type = VariableType.NUMERIC,
                    measure = MeasureType.SCALE
                )
            )
        }

        val newRows = mutableListOf<List<String>>()
        for (c in 0 until numOldCols) {
            val rowData = mutableListOf<String>()
            rowData.add(dataset.variables[c].name)
            for (r in 0 until min(50, numOldRows)) {
                rowData.add(if (c < dataset.rows[r].size) dataset.rows[r][c] else "")
            }
            newRows.add(rowData)
        }

        return Dataset(
            name = "Transposed_${dataset.name}",
            variables = newVariables,
            rows = newRows
        )
    }

    // 3. Aggregate Dataset
    fun aggregate(dataset: Dataset, breakdownVar: String, metricVar: String): Dataset {
        val bIdx = dataset.findVariableIndex(breakdownVar)
        val mIdx = dataset.findVariableIndex(metricVar)
        if (bIdx == -1 || mIdx == -1) return dataset

        val groups = mutableMapOf<String, MutableList<Double>>()
        for (row in dataset.rows) {
            val b = if (bIdx < row.size) row[bIdx] else ""
            val m = if (mIdx < row.size) row[mIdx].toDoubleOrNull() else null
            if (b.isNotBlank() && m != null) {
                groups.getOrPut(b) { mutableListOf() }.add(m)
            }
        }

        val newVars = listOf(
            VariableDefinition(id = "v_agg_b", name = breakdownVar, label = breakdownVar, type = VariableType.STRING, measure = MeasureType.NOMINAL),
            VariableDefinition(id = "v_agg_n", name = "N_Breakdown", label = "Case Count", type = VariableType.NUMERIC, measure = MeasureType.SCALE),
            VariableDefinition(id = "v_agg_mean", name = "${metricVar}_Mean", label = "Mean of $metricVar", type = VariableType.NUMERIC, measure = MeasureType.SCALE),
            VariableDefinition(id = "v_agg_sum", name = "${metricVar}_Sum", label = "Sum of $metricVar", type = VariableType.NUMERIC, measure = MeasureType.SCALE)
        )

        val newRows = groups.map { (cat: String, list: MutableList<Double>) ->
            listOf(
                cat,
                "${list.size}",
                String.format("%.2f", list.average()),
                String.format("%.2f", list.sum())
            )
        }

        return Dataset(
            name = "Aggregated_${dataset.name}",
            variables = newVars,
            rows = newRows
        )
    }

    // 4. Sort Cases with Filter
    fun sortCasesWithFilter(
        dataset: Dataset,
        sortVar: String,
        ascending: Boolean = true,
        filterVar: String? = null,
        filterValue: String? = null
    ): Dataset {
        val sIdx = dataset.findVariableIndex(sortVar)
        val fIdx = if (filterVar != null) dataset.findVariableIndex(filterVar) else -1

        var filteredRows = dataset.rows
        if (fIdx != -1 && filterValue != null && filterValue.isNotBlank()) {
            filteredRows = filteredRows.filter { row ->
                fIdx < row.size && row[fIdx].trim().equals(filterValue.trim(), ignoreCase = true)
            }
        }

        if (sIdx != -1) {
            filteredRows = filteredRows.sortedWith(Comparator { r1, r2 ->
                val v1 = if (sIdx < r1.size) r1[sIdx] else ""
                val v2 = if (sIdx < r2.size) r2[sIdx] else ""
                val d1 = v1.toDoubleOrNull()
                val d2 = v2.toDoubleOrNull()
                val cmp = if (d1 != null && d2 != null) d1.compareTo(d2) else v1.compareTo(v2)
                if (ascending) cmp else -cmp
            })
        }

        return dataset.copy(rows = filteredRows)
    }

    // 5. Count Variable / Values within Cases
    fun countValuesWithinCases(
        dataset: Dataset,
        targetVarName: String,
        sourceVars: List<String>,
        valueToMatch: String
    ): Dataset {
        val indices = sourceVars.map { dataset.findVariableIndex(it) }.filter { it != -1 }
        val newVars = dataset.variables.toMutableList()
        newVars.add(
            VariableDefinition(
                id = "v_cnt_${System.currentTimeMillis()}",
                name = targetVarName,
                label = "Count of $valueToMatch",
                type = VariableType.NUMERIC,
                measure = MeasureType.SCALE
            )
        )

        val newRows = dataset.rows.map { row ->
            var count = 0
            for (idx in indices) {
                if (idx < row.size && row[idx].trim().equals(valueToMatch.trim(), ignoreCase = true)) {
                    count++
                }
            }
            row + "$count"
        }

        return dataset.copy(variables = newVars, rows = newRows)
    }

    // 6. Shift Value (Lead/Lag)
    fun shiftValue(dataset: Dataset, varName: String, shiftAmount: Int): Dataset {
        val idx = dataset.findVariableIndex(varName)
        if (idx == -1) return dataset

        val targetName = if (shiftAmount >= 0) "${varName}_Lag$shiftAmount" else "${varName}_Lead${abs(shiftAmount)}"
        val newVars = dataset.variables.toMutableList()
        newVars.add(
            VariableDefinition(
                id = "v_shift_${System.currentTimeMillis()}",
                name = targetName,
                label = "Shifted $varName by $shiftAmount",
                type = VariableType.NUMERIC,
                measure = MeasureType.SCALE
            )
        )

        val n = dataset.rows.size
        val newRows = dataset.rows.mapIndexed { r, row ->
            val srcRow = r - shiftAmount
            val newVal = if (srcRow in 0 until n && idx < dataset.rows[srcRow].size) dataset.rows[srcRow][idx] else ""
            row + newVal
        }

        return dataset.copy(variables = newVars, rows = newRows)
    }

    // 7. Recode into Same Variable
    fun recodeIntoSameVariable(
        dataset: Dataset,
        varName: String,
        mappings: Map<String, String>
    ): Dataset {
        val idx = dataset.findVariableIndex(varName)
        if (idx == -1) return dataset

        val newRows = dataset.rows.map { row ->
            val rowList = row.toMutableList()
            if (idx < rowList.size) {
                val oldVal = rowList[idx].trim()
                if (mappings.containsKey(oldVal)) {
                    rowList[idx] = mappings[oldVal] ?: oldVal
                }
            }
            rowList
        }

        return dataset.copy(rows = newRows)
    }

    // 8. Recode into Different Variable
    fun recodeIntoDifferentVariable(
        dataset: Dataset,
        srcVarName: String,
        targetVarName: String,
        mappings: Map<String, String>
    ): Dataset {
        val sIdx = dataset.findVariableIndex(srcVarName)
        if (sIdx == -1) return dataset

        val newVars = dataset.variables.toMutableList()
        newVars.add(
            VariableDefinition(
                id = "v_recode_${System.currentTimeMillis()}",
                name = targetVarName,
                label = "Recoded $srcVarName",
                type = VariableType.NUMERIC,
                measure = MeasureType.SCALE
            )
        )

        val newRows = dataset.rows.map { row ->
            val oldVal = if (sIdx < row.size) row[sIdx].trim() else ""
            val newVal = mappings[oldVal] ?: oldVal
            row + newVal
        }

        return dataset.copy(variables = newVars, rows = newRows)
    }

    // 9. Automatic Recode (converts text/categories to 1, 2, 3...)
    fun automaticRecode(dataset: Dataset, srcVarName: String, targetVarName: String): Dataset {
        val sIdx = dataset.findVariableIndex(srcVarName)
        if (sIdx == -1) return dataset

        val uniqueVals = dataset.rows.mapNotNull { if (sIdx < it.size) it[sIdx].trim() else null }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val map = uniqueVals.mapIndexed { i, v -> v to "${i + 1}" }.toMap()
        return recodeIntoDifferentVariable(dataset, srcVarName, targetVarName, map)
    }

    // 10. Rank Cases
    fun rankCases(dataset: Dataset, varName: String, targetVarName: String): Dataset {
        val sIdx = dataset.findVariableIndex(varName)
        if (sIdx == -1) return dataset

        val rowWithVal = dataset.rows.mapIndexed { rIdx, row ->
            val num = if (sIdx < row.size) row[sIdx].toDoubleOrNull() ?: Double.NEGATIVE_INFINITY else Double.NEGATIVE_INFINITY
            Triple(rIdx, num, row)
        }

        val sorted = rowWithVal.sortedBy { it.second }
        val rankMap = IntArray(dataset.rows.size)
        sorted.forEachIndexed { rank, (origIdx, _, _) ->
            rankMap[origIdx] = rank + 1
        }

        val newVars = dataset.variables.toMutableList()
        newVars.add(
            VariableDefinition(
                id = "v_rank_${System.currentTimeMillis()}",
                name = targetVarName,
                label = "Rank of $varName",
                type = VariableType.NUMERIC,
                measure = MeasureType.SCALE
            )
        )

        val newRows = dataset.rows.mapIndexed { rIdx, row ->
            row + "${rankMap[rIdx]}"
        }

        return dataset.copy(variables = newVars, rows = newRows)
    }

    // 11. Replace Missing Value (Series Mean imputation)
    fun replaceMissingValues(dataset: Dataset, varName: String): Dataset {
        val idx = dataset.findVariableIndex(varName)
        if (idx == -1) return dataset

        val numerics = dataset.getNumericColumnValues(idx)
        val meanVal = if (numerics.isNotEmpty()) String.format("%.2f", numerics.average()) else "0.0"

        val newRows = dataset.rows.map { row ->
            val rowList = row.toMutableList()
            if (idx < rowList.size && (rowList[idx].isBlank() || rowList[idx] == ".")) {
                rowList[idx] = meanVal
            }
            rowList
        }

        return dataset.copy(rows = newRows)
    }
}
