package com.example.engine

import com.example.model.*
import java.util.UUID

object SyntaxInterpreter {

    fun execute(syntax: String, dataset: Dataset): Pair<Dataset, List<OutputItem>> {
        val outputs = mutableListOf<OutputItem>()
        var currentDataset = dataset
        val commands = syntax.split(".").map { it.trim() }.filter { it.isNotBlank() }

        for (cmd in commands) {
            val upper = cmd.uppercase()
            try {
                when {
                    upper.startsWith("DESCRIPTIVES") -> {
                        val vars = extractVariables(cmd, "VARIABLES=")
                        if (vars.isNotEmpty()) {
                            outputs.add(StatsEngine.computeDescriptives(currentDataset, vars))
                        } else {
                            // all numeric variables by default
                            val numVars = currentDataset.variables.filter { it.type == VariableType.NUMERIC }.map { it.name }
                            outputs.add(StatsEngine.computeDescriptives(currentDataset, numVars))
                        }
                    }
                    upper.startsWith("FREQUENCIES") -> {
                        val vars = extractVariables(cmd, "VARIABLES=")
                        if (vars.isNotEmpty()) {
                            outputs.add(StatsEngine.computeFrequencies(currentDataset, vars))
                        }
                    }
                    upper.startsWith("T-TEST") -> {
                        val groupVar = extractToken(cmd, "GROUPS=")
                        val vars = extractVariables(cmd, "VARIABLES=")
                        // extract group values e.g. Group('A','B')
                        val groupName = groupVar.substringBefore("(").trim()
                        val group1 = "A"
                        val group2 = "B"
                        if (vars.isNotEmpty() && groupName.isNotBlank()) {
                            outputs.add(StatsEngine.computeIndependentTTest(currentDataset, vars, groupName, group1, group2))
                        }
                    }
                    upper.startsWith("ONEWAY") -> {
                        // format: ONEWAY depVar BY factorVar
                        val afterOneway = cmd.substringAfter("ONEWAY", "").trim()
                        val parts = afterOneway.split(" BY ", " by ", " By ", ignoreCase = true)
                        if (parts.size >= 2) {
                            val dep = parts[0].trim().split(" ", "\n")[0]
                            val fac = parts[1].trim().split(" ", "\n", "/")[0]
                            outputs.add(StatsEngine.computeOneWayAnova(currentDataset, dep, fac))
                        }
                    }
                    upper.startsWith("CORRELATIONS") -> {
                        val vars = extractVariables(cmd, "VARIABLES=")
                        val targetVars = if (vars.isNotEmpty()) vars else currentDataset.variables.filter { it.type == VariableType.NUMERIC }.map { it.name }.take(5)
                        outputs.add(StatsEngine.computeCorrelations(currentDataset, targetVars))
                    }
                    upper.startsWith("REGRESSION") -> {
                        val dep = extractToken(cmd, "DEPENDENT")
                        val ind = extractVariables(cmd, "ENTER")
                        if (dep.isNotBlank() && ind.isNotEmpty()) {
                            outputs.add(StatsEngine.computeLinearRegression(currentDataset, dep, ind))
                        }
                    }
                    upper.startsWith("GRAPH") -> {
                        if (upper.contains("HISTOGRAM=")) {
                            val v = extractToken(cmd, "HISTOGRAM=")
                            outputs.add(StatsEngine.generateHistogramChart(currentDataset, v))
                        } else if (upper.contains("SCATTERPLOT")) {
                            val afterScatter = cmd.substringAfter("=", "").trim()
                            val parts = afterScatter.split(" WITH ", " with ", ignoreCase = true)
                            if (parts.size >= 2) {
                                val x = parts[0].trim().split(" ", "\n")[0]
                                val y = parts[1].trim().split(" ", "\n", ".")[0]
                                outputs.add(StatsEngine.generateScatterChart(currentDataset, x, y))
                            }
                        } else if (upper.contains("BAR")) {
                            val v = extractToken(cmd, "BY")
                            outputs.add(StatsEngine.generateBarChart(currentDataset, v))
                        }
                    }
                    upper.startsWith("COMPUTE") -> {
                        // format: COMPUTE newVar = expr
                        val assign = cmd.substringAfter("COMPUTE", "").trim()
                        val targetVar = assign.substringBefore("=").trim()
                        val expr = assign.substringAfter("=").trim()
                        currentDataset = computeNewVariable(currentDataset, targetVar, expr)
                        outputs.add(
                            LogOutput(
                                id = UUID.randomUUID().toString(),
                                title = "Compute Variable",
                                commandSyntax = cmd,
                                message = "Variable '$targetVar' computed successfully."
                            )
                        )
                    }
                    upper.startsWith("SORT") -> {
                        val v = extractToken(cmd, "BY")
                        currentDataset = sortDatasetBy(currentDataset, v, isAscending = !upper.contains("(D)"))
                        outputs.add(
                            LogOutput(
                                id = UUID.randomUUID().toString(),
                                title = "Sort Cases",
                                commandSyntax = cmd,
                                message = "Cases sorted by '$v'."
                            )
                        )
                    }
                    else -> {
                        outputs.add(
                            LogOutput(
                                id = UUID.randomUUID().toString(),
                                title = "Log",
                                commandSyntax = cmd,
                                message = "Command executed successfully."
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                outputs.add(
                    LogOutput(
                        id = UUID.randomUUID().toString(),
                        title = "Execution Error",
                        commandSyntax = cmd,
                        message = "Error: ${e.localizedMessage ?: "Unknown syntax execution error"}",
                        isError = true
                    )
                )
            }
        }

        return Pair(currentDataset, outputs)
    }

    private fun extractVariables(cmd: String, keyword: String): List<String> {
        val upper = cmd.uppercase()
        val pos = upper.indexOf(keyword.uppercase())
        if (pos == -1) return emptyList()
        val after = cmd.substring(pos + keyword.length).trim()
        val chunk = after.split("/", "\n", ".").firstOrNull() ?: return emptyList()
        return chunk.split(" ", ",", "\t")
            .map { it.trim().removeSurrounding("'", "\"") }
            .filter { it.isNotBlank() }
    }

    private fun extractToken(cmd: String, keyword: String): String {
        val upper = cmd.uppercase()
        val pos = upper.indexOf(keyword.uppercase())
        if (pos == -1) return ""
        val after = cmd.substring(pos + keyword.length).trim().removePrefix("=").trim()
        val chunk = after.split(" ", "\n", "/", ".").firstOrNull() ?: ""
        return chunk.trim().removeSurrounding("'", "\"")
    }

    private fun computeNewVariable(dataset: Dataset, varName: String, expr: String): Dataset {
        val newVars = dataset.variables.toMutableList()
        val existingIdx = dataset.findVariableIndex(varName)
        if (existingIdx == -1) {
            newVars.add(
                VariableDefinition(
                    id = "v_${System.currentTimeMillis()}",
                    name = varName,
                    type = VariableType.NUMERIC,
                    decimals = 2,
                    label = "Computed: $expr",
                    measure = MeasureType.SCALE
                )
            )
        }

        val updatedRows = dataset.rows.map { row ->
            val rowList = row.toMutableList()
            var computedVal = 0.0
            try {
                // simple expression evaluation e.g. Weight_kg / (Height_m * Height_m) or Col * 2
                val parts = expr.split("+", "-", "*", "/")
                val colTokens = parts.map { it.trim() }.filter { it.isNotBlank() }
                var res = 0.0
                if (expr.contains("/")) {
                    val p1 = expr.substringBefore("/").trim()
                    val p2 = expr.substringAfter("/").trim()
                    val v1 = getVal(dataset, row, p1)
                    val v2 = getVal(dataset, row, p2)
                    res = if (v2 != 0.0) v1 / v2 else 0.0
                } else if (expr.contains("*")) {
                    val p1 = expr.substringBefore("*").trim()
                    val p2 = expr.substringAfter("*").trim()
                    res = getVal(dataset, row, p1) * getVal(dataset, row, p2)
                } else if (expr.contains("+")) {
                    val p1 = expr.substringBefore("+").trim()
                    val p2 = expr.substringAfter("+").trim()
                    res = getVal(dataset, row, p1) + getVal(dataset, row, p2)
                } else {
                    res = getVal(dataset, row, expr)
                }
                computedVal = res
            } catch (_: Exception) {
                computedVal = 0.0
            }

            val strVal = String.format("%.2f", computedVal)
            if (existingIdx != -1 && existingIdx < rowList.size) {
                rowList[existingIdx] = strVal
            } else {
                rowList.add(strVal)
            }
            rowList
        }

        return dataset.copy(variables = newVars, rows = updatedRows)
    }

    private fun getVal(dataset: Dataset, row: List<String>, token: String): Double {
        val num = token.toDoubleOrNull()
        if (num != null) return num
        val idx = dataset.findVariableIndex(token)
        if (idx != -1 && idx < row.size) {
            return row[idx].toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }

    private fun sortDatasetBy(dataset: Dataset, varName: String, isAscending: Boolean): Dataset {
        val idx = dataset.findVariableIndex(varName)
        if (idx == -1) return dataset
        val sortedRows = dataset.rows.sortedWith(Comparator { r1, r2 ->
            val v1 = if (idx < r1.size) r1[idx] else ""
            val v2 = if (idx < r2.size) r2[idx] else ""
            val n1 = v1.toDoubleOrNull()
            val n2 = v2.toDoubleOrNull()
            val cmp = if (n1 != null && n2 != null) {
                n1.compareTo(n2)
            } else {
                v1.compareTo(v2, ignoreCase = true)
            }
            if (isAscending) cmp else -cmp
        })
        return dataset.copy(rows = sortedRows)
    }
}
