package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlignmentType
import com.example.model.Dataset

@Composable
fun DataGrid(
    dataset: Dataset,
    selectedRow: Int,
    selectedCol: Int,
    isDark: Boolean,
    onCellSelect: (row: Int, col: Int) -> Unit,
    onCellDoubleTap: (row: Int, col: Int, currentValue: String) -> Unit,
    showValueLabels: Boolean,
    modifier: Modifier = Modifier
) {
    val hScrollState = rememberScrollState()
    val vScrollState = rememberLazyListState()

    val gridBg = if (isDark) Color(0xFF000000) else Color.White
    val headerBg = if (isDark) Color(0xFF18181B) else Color(0xFFF1F5F9)
    val headerBorder = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
    val headerText = if (isDark) Color(0xFFFAFAFA) else Color(0xFF1E293B)
    val headerActiveBg = if (isDark) Color(0xFF1E293B) else Color(0xFFE0E7FF)
    val headerActiveText = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)

    val rowNumBg = if (isDark) Color(0xFF0A0A0A) else Color(0xFFF8FAFC)
    val rowNumActiveBg = if (isDark) Color(0xFF1E293B) else Color(0xFFE0E7FF)
    val rowNumText = if (isDark) Color(0xFF71717A) else Color(0xFF64748B)

    val cellBg = if (isDark) Color(0xFF121212) else Color.White
    val cellSelectedBg = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)
    val cellBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val cellSelectedBorder = if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)
    val cellText = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    val colWidths = remember(dataset.variables) {
        dataset.variables.map { v ->
            when (v.name) {
                "Patient_ID" -> 90.dp
                "Group" -> 70.dp
                "HTS_Mode" -> 160.dp
                "Intubated" -> 80.dp
                "Age_years" -> 80.dp
                "Age_Group" -> 90.dp
                "Sex" -> 80.dp
                "Cause_of_Injury" -> 180.dp
                "TBI_Severity" -> 120.dp
                "Height_m" -> 80.dp
                "Weight_kg" -> 80.dp
                "ICP_mmHg" -> 90.dp
                "MAP_mmHg" -> 90.dp
                "GCS_Score" -> 80.dp
                else -> 110.dp
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gridBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .border(width = 0.5.dp, color = headerBorder)
            ) {
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(28.dp)
                        .background(if (isDark) Color(0xFF18181B) else Color(0xFFE2E8F0))
                        .border(width = 0.5.dp, color = headerBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "", fontSize = 11.sp, color = rowNumText)
                }

                Row(modifier = Modifier.horizontalScroll(hScrollState)) {
                    dataset.variables.forEachIndexed { colIdx, variable ->
                        val isColActive = selectedCol == colIdx
                        Box(
                            modifier = Modifier
                                .width(colWidths.getOrElse(colIdx) { 100.dp })
                                .height(28.dp)
                                .background(if (isColActive) headerActiveBg else headerBg)
                                .border(width = 0.5.dp, color = headerBorder)
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = variable.name,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isColActive) headerActiveText else headerText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Data Rows (LazyColumn)
            LazyColumn(
                state = vScrollState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(
                    count = dataset.rows.size,
                    key = { it }
                ) { rowIndex ->
                    val row = dataset.rows[rowIndex]
                    val isRowActive = selectedRow == rowIndex

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    ) {
                        // Left Fixed Row Number Column
                        Box(
                            modifier = Modifier
                                .width(46.dp)
                                .fillMaxHeight()
                                .background(if (isRowActive) rowNumActiveBg else rowNumBg)
                                .border(width = 0.5.dp, color = headerBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${rowIndex + 1}",
                                fontSize = 11.sp,
                                color = if (isRowActive) headerActiveText else rowNumText,
                                fontWeight = if (isRowActive) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        // Data cells
                        Row(modifier = Modifier.horizontalScroll(hScrollState)) {
                            dataset.variables.forEachIndexed { colIdx, variable ->
                                val isCellSelected = selectedRow == rowIndex && selectedCol == colIdx
                                val rawValue = if (colIdx < row.size) row[colIdx] else ""
                                val displayValue = if (showValueLabels && variable.valueLabels.containsKey(rawValue)) {
                                    variable.valueLabels[rawValue] ?: rawValue
                                } else {
                                    rawValue
                                }

                                val textAlign = when (variable.align) {
                                    AlignmentType.RIGHT -> TextAlign.Right
                                    AlignmentType.CENTER -> TextAlign.Center
                                    AlignmentType.LEFT -> TextAlign.Left
                                }

                                Box(
                                    modifier = Modifier
                                        .width(colWidths.getOrElse(colIdx) { 100.dp })
                                        .fillMaxHeight()
                                        .background(if (isCellSelected) cellSelectedBg else cellBg)
                                        .border(
                                            width = if (isCellSelected) 2.dp else 0.5.dp,
                                            color = if (isCellSelected) cellSelectedBorder else cellBorder
                                        )
                                        .clickable {
                                            if (isCellSelected) {
                                                onCellDoubleTap(rowIndex, colIdx, rawValue)
                                            } else {
                                                onCellSelect(rowIndex, colIdx)
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = when (variable.align) {
                                        AlignmentType.RIGHT -> Alignment.CenterEnd
                                        AlignmentType.CENTER -> Alignment.Center
                                        AlignmentType.LEFT -> Alignment.CenterStart
                                    }
                                ) {
                                    Text(
                                        text = displayValue,
                                        fontSize = 11.sp,
                                        color = cellText,
                                        textAlign = textAlign,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
