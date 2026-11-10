package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun VariableViewGrid(
    dataset: Dataset,
    isDark: Boolean,
    onEditProperty: (index: Int, propertyName: String, currentValue: String) -> Unit,
    onAddVariable: () -> Unit,
    onDeleteVariable: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hScrollState = rememberScrollState()

    val gridBg = if (isDark) Color(0xFF000000) else Color.White
    val toolbarBg = if (isDark) Color(0xFF0D0D0E) else Color(0xFFF1F5F9)
    val headerBg = if (isDark) Color(0xFF18181B) else Color(0xFFE2E8F0)
    val headerBorder = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
    val headerText = if (isDark) Color(0xFFFAFAFA) else Color(0xFF1E293B)
    val rowNumBg = if (isDark) Color(0xFF0A0A0A) else Color(0xFFF8FAFC)
    val rowNumText = if (isDark) Color(0xFF71717A) else Color(0xFF64748B)

    val columns = listOf(
        "Name" to 110.dp,
        "Type" to 85.dp,
        "Width" to 60.dp,
        "Decimals" to 70.dp,
        "Label" to 220.dp,
        "Values" to 120.dp,
        "Missing" to 75.dp,
        "Columns" to 65.dp,
        "Align" to 80.dp,
        "Measure" to 95.dp,
        "Role" to 70.dp,
        "Actions" to 65.dp
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gridBg)
            .testTag("variable_view_grid")
    ) {
        // 1. Variable View Action Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(toolbarBg)
                .border(width = 0.5.dp, color = headerBorder)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddVariable,
                    modifier = Modifier
                        .height(26.dp)
                        .testTag("add_variable_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(3.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF2563EB) else Color(0xFF2563EB)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Variable",
                        modifier = Modifier.size(13.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Variable", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Text(
                    text = "Total Variables: ${dataset.variables.size}",
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
                )
            }

            Text(
                text = "💡 Tap any cell to edit attributes",
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
                fontWeight = FontWeight.Medium
            )
        }

        // 2. Variable View Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .border(width = 0.5.dp, color = headerBorder)
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(28.dp)
                    .background(if (isDark) Color(0xFF18181B) else Color(0xFFE2E8F0))
                    .border(width = 0.5.dp, color = headerBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "#", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = headerText)
            }

            Row(modifier = Modifier.horizontalScroll(hScrollState)) {
                columns.forEach { (colName, width) ->
                    Box(
                        modifier = Modifier
                            .width(width)
                            .height(28.dp)
                            .background(headerBg)
                            .border(width = 0.5.dp, color = headerBorder)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = colName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = headerText
                        )
                    }
                }
            }
        }

        // 3. Variable Rows
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(dataset.variables.size) { varIndex ->
                val v = dataset.variables[varIndex]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                ) {
                    // Row index number
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .fillMaxHeight()
                            .background(rowNumBg)
                            .border(width = 0.5.dp, color = headerBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${varIndex + 1}",
                            fontSize = 11.sp,
                            color = rowNumText
                        )
                    }

                    // Editable attributes
                    Row(modifier = Modifier.horizontalScroll(hScrollState)) {
                        // 1. Name
                        EditableVariableCell(
                            text = v.name,
                            width = 110.dp,
                            isBold = true,
                            isDark = isDark,
                            onClick = { onEditProperty(varIndex, "Name", v.name) }
                        )

                        // 2. Type
                        EditableVariableCell(
                            text = when (v.type) {
                                VariableType.NUMERIC -> "Numeric"
                                VariableType.STRING -> "String"
                                VariableType.DATE -> "Date"
                            },
                            width = 85.dp,
                            isDark = isDark,
                            onClick = { onEditProperty(varIndex, "Type", v.type.name) }
                        )

                        // 3. Width
                        EditableVariableCell(
                            text = "${v.width}",
                            width = 60.dp,
                            isDark = isDark,
                            onClick = { onEditProperty(varIndex, "Width", "${v.width}") }
                        )

                        // 4. Decimals
                        EditableVariableCell(
                            text = "${v.decimals}",
                            width = 70.dp,
                            isDark = isDark,
                            onClick = { onEditProperty(varIndex, "Decimals", "${v.decimals}") }
                        )

                        // 5. Label
                        EditableVariableCell(
                            text = v.label.ifBlank { "None" },
                            width = 220.dp,
                            isDark = isDark,
                            onClick = { onEditProperty(varIndex, "Label", v.label) }
                        )

                        // 6. Values
                        val valuesSummary = if (v.valueLabels.isNotEmpty()) {
                            "{${v.valueLabels.size} labels}"
                        } else "None"
                        val rawValuesStr = v.valueLabels.entries.joinToString(", ") { "${it.key}=${it.value}" }
                        EditableVariableCell(
                            text = valuesSummary,
                            width = 120.dp,
                            isDark = isDark,
                            onClick = { onEditProperty(varIndex, "Values", rawValuesStr) }
                        )

                        // 7. Missing
                        EditableVariableCell(
                            text = v.missing.ifBlank { "None" },
                            width = 75.dp,
                            isDark = isDark,
                            onClick = { onEditProperty(varIndex, "Missing", v.missing) }
                        )

                        // 8. Columns
                        EditableVariableCell(
                            text = "${v.columns}",
                            width = 65.dp,
                            isDark = isDark,
                            onClick = { onEditProperty(varIndex, "Columns", "${v.columns}") }
                        )

                        // 9. Align
                        EditableVariableCell(
                            text = v.align.name.lowercase().replaceFirstChar { it.uppercase() },
                            width = 80.dp,
                            isDark = isDark,
                            onClick = { onEditProperty(varIndex, "Align", v.align.name) }
                        )

                        // 10. Measure
                        val measureLabel = when (v.measure) {
                            MeasureType.SCALE -> "📏 Scale"
                            MeasureType.ORDINAL -> "📶 Ordinal"
                            MeasureType.NOMINAL -> "🏷️ Nominal"
                        }
                        EditableVariableCell(
                            text = measureLabel,
                            width = 95.dp,
                            isDark = isDark,
                            isHighlighted = true,
                            onClick = { onEditProperty(varIndex, "Measure", v.measure.name) }
                        )

                        // 11. Role
                        EditableVariableCell(
                            text = v.role,
                            width = 70.dp,
                            isDark = isDark,
                            onClick = { onEditProperty(varIndex, "Role", v.role) }
                        )

                        // 12. Actions (Delete button)
                        Box(
                            modifier = Modifier
                                .width(65.dp)
                                .fillMaxHeight()
                                .background(if (isDark) Color(0xFF121212) else Color.White)
                                .border(width = 0.5.dp, color = headerBorder)
                                .clickable { onDeleteVariable(varIndex) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Variable",
                                tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableVariableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isDark: Boolean,
    isBold: Boolean = false,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    val cellBg = when {
        isHighlighted -> if (isDark) Color(0xFF0F261E) else Color(0xFFECFDF5)
        else -> if (isDark) Color(0xFF121212) else Color.White
    }
    val cellBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textColor = when {
        isHighlighted -> if (isDark) Color(0xFF6EE7B7) else Color(0xFF059669)
        else -> if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    }

    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(cellBg)
            .border(width = 0.5.dp, color = cellBorder)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
