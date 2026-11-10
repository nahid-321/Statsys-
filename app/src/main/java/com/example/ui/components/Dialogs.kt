package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Dataset
import com.example.model.VariableType

@Composable
fun DialogActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    testTag: String? = null
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .fillMaxWidth()
                .height(34.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            shape = RoundedCornerShape(3.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .fillMaxWidth()
                .height(34.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            shape = RoundedCornerShape(3.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun DescriptivesDialog(
    dataset: Dataset,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onRun: (selectedVars: List<String>) -> Unit,
    onPaste: (selectedVars: List<String>) -> Unit
) {
    val numericVars = remember(dataset) {
        dataset.variables.filter { it.type == VariableType.NUMERIC }.map { it.name }
    }
    var sourceVars by remember { mutableStateOf(numericVars) }
    var targetVars by remember { mutableStateOf(listOf<String>()) }
    var selectedSource by remember { mutableStateOf<String?>(null) }
    var selectedTarget by remember { mutableStateOf<String?>(null) }

    val bg = if (isDark) Color(0xFF121212) else Color(0xFFF8FAFC)
    val borderCol = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
    val textCol = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = bg,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
            modifier = Modifier.width(520.dp).padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Descriptives",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textCol
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(text = "Variables:", fontSize = 11.sp, color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        VariableListBox(
                            variables = sourceVars,
                            selected = selectedSource,
                            isDark = isDark,
                            onSelect = { selectedSource = it; selectedTarget = null }
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                selectedSource?.let { v ->
                                    sourceVars = sourceVars - v
                                    targetVars = targetVars + v
                                    selectedSource = null
                                }
                            },
                            enabled = selectedSource != null,
                            modifier = Modifier.size(32.dp).testTag("move_right_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Add", tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB))
                        }

                        IconButton(
                            onClick = {
                                selectedTarget?.let { v ->
                                    targetVars = targetVars - v
                                    sourceVars = sourceVars + v
                                    selectedTarget = null
                                }
                            },
                            enabled = selectedTarget != null,
                            modifier = Modifier.size(32.dp).testTag("move_left_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Remove", tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB))
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(text = "Variable(s):", fontSize = 11.sp, color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        VariableListBox(
                            variables = targetVars,
                            selected = selectedTarget,
                            isDark = isDark,
                            onSelect = { selectedTarget = it; selectedSource = null }
                        )
                    }

                    Column(
                        modifier = Modifier.width(92.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DialogActionButton(
                            text = "OK",
                            onClick = { onRun(targetVars); onDismiss() },
                            enabled = targetVars.isNotEmpty(),
                            isPrimary = true,
                            testTag = "dialog_ok_button"
                        )
                        DialogActionButton(
                            text = "Paste",
                            onClick = { onPaste(targetVars); onDismiss() },
                            enabled = targetVars.isNotEmpty()
                        )
                        DialogActionButton(
                            text = "Reset",
                            onClick = {
                                sourceVars = numericVars
                                targetVars = emptyList()
                                selectedSource = null
                                selectedTarget = null
                            }
                        )
                        DialogActionButton(
                            text = "Cancel",
                            onClick = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FrequenciesDialog(
    dataset: Dataset,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onRun: (selectedVars: List<String>) -> Unit
) {
    val allVars = remember(dataset) { dataset.variables.map { it.name } }
    var sourceVars by remember { mutableStateOf(allVars) }
    var targetVars by remember { mutableStateOf(listOf<String>()) }
    var selectedSource by remember { mutableStateOf<String?>(null) }
    var selectedTarget by remember { mutableStateOf<String?>(null) }

    val bg = if (isDark) Color(0xFF121212) else Color(0xFFF8FAFC)
    val borderCol = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
    val textCol = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = bg,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
            modifier = Modifier.width(520.dp).padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Frequencies", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textCol)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(text = "Variables:", fontSize = 11.sp, color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        VariableListBox(variables = sourceVars, selected = selectedSource, isDark = isDark, onSelect = { selectedSource = it })
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                selectedSource?.let {
                                    sourceVars = sourceVars - it
                                    targetVars = targetVars + it
                                    selectedSource = null
                                }
                            },
                            enabled = selectedSource != null
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Add")
                        }
                        IconButton(
                            onClick = {
                                selectedTarget?.let {
                                    targetVars = targetVars - it
                                    sourceVars = sourceVars + it
                                    selectedTarget = null
                                }
                            },
                            enabled = selectedTarget != null
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Remove")
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(text = "Variable(s):", fontSize = 11.sp, color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        VariableListBox(variables = targetVars, selected = selectedTarget, isDark = isDark, onSelect = { selectedTarget = it })
                    }

                    Column(modifier = Modifier.width(92.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DialogActionButton(
                            text = "OK",
                            onClick = { onRun(targetVars); onDismiss() },
                            enabled = targetVars.isNotEmpty(),
                            isPrimary = true,
                            testTag = "freq_dialog_ok"
                        )
                        DialogActionButton(
                            text = "Cancel",
                            onClick = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IndependentTTestDialog(
    dataset: Dataset,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onRun: (testVars: List<String>, groupingVar: String, g1: String, g2: String) -> Unit
) {
    val numericVars = remember(dataset) { dataset.variables.filter { it.type == VariableType.NUMERIC }.map { it.name } }
    var selectedTestVars by remember { mutableStateOf(listOf("Age_years", "Height_m", "Weight_kg")) }
    val selectedGroupingVar = "Group"

    val bg = if (isDark) Color(0xFF121212) else Color(0xFFF8FAFC)
    val borderCol = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
    val textCol = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = bg,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
            modifier = Modifier.width(500.dp).padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Independent-Samples T Test", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textCol)
                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Test Variable(s):", fontSize = 11.sp, color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569))
                Spacer(modifier = Modifier.height(4.dp))
                VariableListBox(
                    variables = numericVars,
                    selected = null,
                    isDark = isDark,
                    onSelect = {},
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Grouping Variable: Group (Group A vs Group B)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = textCol)

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onRun(selectedTestVars, selectedGroupingVar, "A", "B")
                            onDismiss()
                        },
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(3.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("OK", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OneWayAnovaDialog(
    dataset: Dataset,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onRun: (depVar: String, factorVar: String) -> Unit
) {
    val depVar = "Age_years"
    val factorVar = "Cause_of_Injury"
    val bg = if (isDark) Color(0xFF121212) else Color(0xFFF8FAFC)
    val textCol = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = bg,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)),
            modifier = Modifier.width(460.dp).padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "One-Way ANOVA", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textCol)
                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Dependent Variable (Numeric): $depVar", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textCol)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Factor Variable (Category): $factorVar", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textCol)

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onRun(depVar, factorVar)
                            onDismiss()
                        },
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(3.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("OK", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BivariateCorrDialog(
    dataset: Dataset,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onRun: (vars: List<String>) -> Unit
) {
    val numericVars = remember(dataset) { dataset.variables.filter { it.type == VariableType.NUMERIC }.map { it.name } }
    val targetVars = remember { numericVars.take(4) }
    val bg = if (isDark) Color(0xFF121212) else Color(0xFFF8FAFC)
    val textCol = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = bg,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)),
            modifier = Modifier.width(460.dp).padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Bivariate Correlations (Pearson)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textCol)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Variables: ${targetVars.joinToString(", ")}", fontSize = 12.sp, color = textCol)

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onRun(targetVars)
                            onDismiss()
                        },
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(3.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("OK", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LinearRegressionDialog(
    dataset: Dataset,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onRun: (depVar: String, indepVars: List<String>) -> Unit
) {
    val depVar = "Weight_kg"
    val indepVars = listOf("Height_m", "Age_years")
    val bg = if (isDark) Color(0xFF121212) else Color(0xFFF8FAFC)
    val textCol = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = bg,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)),
            modifier = Modifier.width(460.dp).padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Linear Regression", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textCol)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Dependent Variable: $depVar", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textCol)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Independent Variable(s): ${indepVars.joinToString(", ")}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textCol)

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onRun(depVar, indepVars)
                            onDismiss()
                        },
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(3.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("OK", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistogramDialog(
    dataset: Dataset,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onRun: (varName: String, showNormalCurve: Boolean) -> Unit,
    onPaste: (varName: String) -> Unit
) {
    val numericVars = remember(dataset) {
        dataset.variables.filter { it.type == VariableType.NUMERIC }.map { it.name }
    }
    var sourceVars by remember { mutableStateOf(numericVars) }
    var targetVar by remember { mutableStateOf<String?>(numericVars.firstOrNull()) }
    var selectedSource by remember { mutableStateOf<String?>(null) }
    var selectedTarget by remember { mutableStateOf<String?>(null) }
    var showNormalCurve by remember { mutableStateOf(true) }

    val bg = if (isDark) Color(0xFF121212) else Color(0xFFF8FAFC)
    val borderCol = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
    val textCol = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = bg,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
            modifier = Modifier.width(520.dp).padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Histogram",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textCol
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(
                            text = "Variables:",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        VariableListBox(
                            variables = sourceVars,
                            selected = selectedSource,
                            isDark = isDark,
                            onSelect = { selectedSource = it; selectedTarget = null }
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                selectedSource?.let { v ->
                                    targetVar = v
                                    selectedSource = null
                                }
                            },
                            enabled = selectedSource != null,
                            modifier = Modifier.size(32.dp).testTag("hist_move_right")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Add",
                                tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                            )
                        }

                        IconButton(
                            onClick = {
                                targetVar = null
                                selectedTarget = null
                            },
                            enabled = targetVar != null && selectedTarget != null,
                            modifier = Modifier.size(32.dp).testTag("hist_move_left")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Remove",
                                tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(
                            text = "Variable:",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        VariableListBox(
                            variables = targetVar?.let { listOf(it) } ?: emptyList(),
                            selected = selectedTarget,
                            isDark = isDark,
                            onSelect = { selectedTarget = it; selectedSource = null }
                        )
                    }

                    Column(
                        modifier = Modifier.width(92.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DialogActionButton(
                            text = "OK",
                            onClick = {
                                targetVar?.let { onRun(it, showNormalCurve) }
                                onDismiss()
                            },
                            enabled = targetVar != null,
                            isPrimary = true,
                            testTag = "hist_dialog_ok"
                        )

                        DialogActionButton(
                            text = "Paste",
                            onClick = {
                                targetVar?.let { onPaste(it) }
                                onDismiss()
                            },
                            enabled = targetVar != null
                        )

                        DialogActionButton(
                            text = "Reset",
                            onClick = {
                                sourceVars = numericVars
                                targetVar = numericVars.firstOrNull()
                                selectedSource = null
                                selectedTarget = null
                            }
                        )

                        DialogActionButton(
                            text = "Cancel",
                            onClick = onDismiss
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showNormalCurve,
                        onCheckedChange = { showNormalCurve = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Display normal curve",
                        fontSize = 11.5.sp,
                        color = textCol
                    )
                }
            }
        }
    }
}

@Composable
fun ScatterPlotDialog(
    dataset: Dataset,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onRun: (xVar: String, yVar: String) -> Unit,
    onPaste: (xVar: String, yVar: String) -> Unit
) {
    val numericVars = remember(dataset) {
        dataset.variables.filter { it.type == VariableType.NUMERIC }.map { it.name }
    }
    var selectedSource by remember { mutableStateOf<String?>(null) }
    var yAxisVar by remember { mutableStateOf(numericVars.getOrNull(2) ?: numericVars.firstOrNull() ?: "") }
    var xAxisVar by remember { mutableStateOf(numericVars.getOrNull(1) ?: numericVars.firstOrNull() ?: "") }

    val bg = if (isDark) Color(0xFF121212) else Color(0xFFF8FAFC)
    val borderCol = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
    val textCol = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = bg,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
            modifier = Modifier.width(530.dp).padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Scatter / Dot Plot",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textCol
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().height(230.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(text = "Variables:", fontSize = 11.sp, color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        VariableListBox(
                            variables = numericVars,
                            selected = selectedSource,
                            isDark = isDark,
                            onSelect = { selectedSource = it }
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1.1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { selectedSource?.let { yAxisVar = it } },
                                    enabled = selectedSource != null,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Set Y", tint = Color(0xFF2563EB))
                                }
                                Text(text = "Y Axis: $yAxisVar", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = textCol)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            VariableListBox(
                                variables = if (yAxisVar.isNotBlank()) listOf(yAxisVar) else emptyList(),
                                selected = null,
                                isDark = isDark,
                                onSelect = {},
                                modifier = Modifier.fillMaxWidth().height(70.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { selectedSource?.let { xAxisVar = it } },
                                    enabled = selectedSource != null,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Set X", tint = Color(0xFF2563EB))
                                }
                                Text(text = "X Axis: $xAxisVar", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = textCol)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            VariableListBox(
                                variables = if (xAxisVar.isNotBlank()) listOf(xAxisVar) else emptyList(),
                                selected = null,
                                isDark = isDark,
                                onSelect = {},
                                modifier = Modifier.fillMaxWidth().height(70.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.width(92.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DialogActionButton(
                            text = "OK",
                            onClick = {
                                if (xAxisVar.isNotBlank() && yAxisVar.isNotBlank()) {
                                    onRun(xAxisVar, yAxisVar)
                                }
                                onDismiss()
                            },
                            enabled = xAxisVar.isNotBlank() && yAxisVar.isNotBlank(),
                            isPrimary = true,
                            testTag = "scatter_dialog_ok"
                        )

                        DialogActionButton(
                            text = "Paste",
                            onClick = {
                                onPaste(xAxisVar, yAxisVar)
                                onDismiss()
                            },
                            enabled = xAxisVar.isNotBlank() && yAxisVar.isNotBlank()
                        )

                        DialogActionButton(
                            text = "Cancel",
                            onClick = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarChartDialog(
    dataset: Dataset,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onRun: (varName: String) -> Unit
) {
    val allVars = remember(dataset) { dataset.variables.map { it.name } }
    var selectedSource by remember { mutableStateOf<String?>(null) }
    var targetVar by remember { mutableStateOf(allVars.firstOrNull { it.contains("Age") || it.contains("Group") } ?: allVars.firstOrNull() ?: "") }

    val bg = if (isDark) Color(0xFF121212) else Color(0xFFF8FAFC)
    val borderCol = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
    val textCol = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = bg,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
            modifier = Modifier.width(520.dp).padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Define Simple Bar: Counts of Values", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textCol)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(text = "Variables:", fontSize = 11.sp, color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        VariableListBox(variables = allVars, selected = selectedSource, isDark = isDark, onSelect = { selectedSource = it })
                    }

                    IconButton(
                        onClick = { selectedSource?.let { targetVar = it } },
                        enabled = selectedSource != null,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Add", tint = Color(0xFF2563EB))
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(text = "Category Axis:", fontSize = 11.sp, color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        VariableListBox(variables = if (targetVar.isNotBlank()) listOf(targetVar) else emptyList(), selected = null, isDark = isDark, onSelect = {})
                    }

                    Column(
                        modifier = Modifier.width(86.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                if (targetVar.isNotBlank()) onRun(targetVar)
                                onDismiss()
                            },
                            enabled = targetVar.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(28.dp),
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("OK", fontSize = 11.sp)
                        }

                        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(28.dp), shape = RoundedCornerShape(2.dp)) {
                            Text("Cancel", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieChartDialog(
    dataset: Dataset,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onRun: (varName: String) -> Unit
) {
    val allVars = remember(dataset) { dataset.variables.map { it.name } }
    var selectedSource by remember { mutableStateOf<String?>(null) }
    var targetVar by remember { mutableStateOf(allVars.firstOrNull { it.contains("Sex") || it.contains("HTS") } ?: allVars.firstOrNull() ?: "") }

    val bg = if (isDark) Color(0xFF121212) else Color(0xFFF8FAFC)
    val borderCol = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
    val textCol = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = bg,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
            modifier = Modifier.width(520.dp).padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Define Pie: Values of Individual Cases", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textCol)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(text = "Variables:", fontSize = 11.sp, color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        VariableListBox(variables = allVars, selected = selectedSource, isDark = isDark, onSelect = { selectedSource = it })
                    }

                    IconButton(
                        onClick = { selectedSource?.let { targetVar = it } },
                        enabled = selectedSource != null,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Add", tint = Color(0xFF2563EB))
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(text = "Define Slices by:", fontSize = 11.sp, color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        VariableListBox(variables = if (targetVar.isNotBlank()) listOf(targetVar) else emptyList(), selected = null, isDark = isDark, onSelect = {})
                    }

                    Column(
                        modifier = Modifier.width(92.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DialogActionButton(
                            text = "OK",
                            onClick = {
                                if (targetVar.isNotBlank()) onRun(targetVar)
                                onDismiss()
                            },
                            enabled = targetVar.isNotBlank(),
                            isPrimary = true,
                            testTag = "piechart_dialog_ok"
                        )

                        DialogActionButton(
                            text = "Cancel",
                            onClick = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VariablePropertyEditDialog(
    target: com.example.ui.VariableEditTarget,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (varIndex: Int, propertyName: String, newValue: String) -> Unit,
    onDelete: (varIndex: Int) -> Unit
) {
    var editValue by remember { mutableStateOf(target.currentValue) }
    val bg = if (isDark) Color(0xFF121212) else Color.White
    val textCol = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val borderCol = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = bg,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
            modifier = Modifier.width(420.dp).padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Edit Variable ${target.propertyName}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textCol
                )
                Text(
                    text = "Variable #${target.variableIndex + 1} (${target.propertyName})",
                    fontSize = 11.5.sp,
                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                )

                Spacer(modifier = Modifier.height(14.dp))

                when (target.propertyName) {
                    "Type" -> {
                        val types = listOf("NUMERIC", "STRING", "DATE")
                        types.forEach { t ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editValue = t }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = editValue.equals(t, ignoreCase = true),
                                    onClick = { editValue = t }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (t) {
                                        "NUMERIC" -> "Numeric (numbers, floating point)"
                                        "STRING" -> "String (text characters)"
                                        "DATE" -> "Date (calendar dates)"
                                        else -> t
                                    },
                                    fontSize = 12.sp,
                                    color = textCol
                                )
                            }
                        }
                    }
                    "Measure" -> {
                        val measures = listOf("SCALE", "ORDINAL", "NOMINAL")
                        measures.forEach { m ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editValue = m }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = editValue.equals(m, ignoreCase = true),
                                    onClick = { editValue = m }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (m) {
                                        "SCALE" -> "Scale (Continuous ratio/interval metric)"
                                        "ORDINAL" -> "Ordinal (Ranked ordered categories)"
                                        "NOMINAL" -> "Nominal (Discrete nominal groups)"
                                        else -> m
                                    },
                                    fontSize = 12.sp,
                                    color = textCol
                                )
                            }
                        }
                    }
                    "Align" -> {
                        val aligns = listOf("LEFT", "CENTER", "RIGHT")
                        aligns.forEach { a ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editValue = a }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = editValue.equals(a, ignoreCase = true),
                                    onClick = { editValue = a }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = a.lowercase().replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp,
                                    color = textCol
                                )
                            }
                        }
                    }
                    "Decimals" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val currDec = editValue.toIntOrNull() ?: 2
                            Button(
                                onClick = { editValue = "${(currDec - 1).coerceAtLeast(0)}" },
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text("-", fontSize = 14.sp)
                            }
                            Text(
                                text = "$currDec decimals",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textCol
                            )
                            Button(
                                onClick = { editValue = "${(currDec + 1).coerceAtMost(8)}" },
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text("+", fontSize = 14.sp)
                            }
                        }
                    }
                    "Values" -> {
                        Column {
                            Text(
                                text = "Enter label mappings in format: 1=Male, 2=Female",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = editValue,
                                onValueChange = { editValue = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. 1=Low, 2=Medium, 3=High", fontSize = 11.sp) }
                            )
                        }
                    }
                    else -> {
                        OutlinedTextField(
                            value = editValue,
                            onValueChange = { editValue = it },
                            modifier = Modifier.fillMaxWidth().testTag("var_prop_input"),
                            singleLine = true,
                            label = { Text(target.propertyName, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onDelete(target.variableIndex)
                            onDismiss()
                        }
                    ) {
                        Text("Delete Variable", color = Color(0xFFEF4444), fontSize = 11.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Cancel", fontSize = 11.sp)
                            }
                        }
                        Button(
                            onClick = {
                                onSave(target.variableIndex, target.propertyName, editValue)
                                onDismiss()
                            },
                            modifier = Modifier.height(34.dp).testTag("save_var_prop_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Apply", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportDataDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onLaunchFileManager: () -> Unit,
    onLoadMasterTrauma: () -> Unit,
    onLoadIris: () -> Unit,
    onLoadStudents: () -> Unit,
    onPasteCsv: (String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var tabIndex by remember { mutableStateOf(0) }

    val bg = if (isDark) Color(0xFF121212) else Color.White
    val textCol = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val subCol = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = bg,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)),
            modifier = Modifier.width(500.dp).padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(text = "Import Data — Statsys", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textCol)
                Spacer(modifier = Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = if (isDark) Color(0xFF18181B) else Color(0xFFF1F5F9),
                    contentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                ) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("File Manager & Presets", fontSize = 11.sp) })
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Paste CSV / TSV", fontSize = 11.sp) })
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (tabIndex == 0) {
                    // Option 1: Android System File Manager
                    Button(
                        onClick = {
                            onDismiss()
                            onLaunchFileManager()
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp).testTag("import_file_manager_button"),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2563EB) else Color(0xFF2563EB))
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Folder", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open from Android File Manager (CSV/TSV/Excel)", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Or Load Standard Benchmark Datasets:", fontSize = 11.5.sp, color = subCol)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = { onLoadMasterTrauma(); onDismiss() },
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text("Master Trauma Study Dataset (122 Cases - Screenshot match)", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { onLoadIris(); onDismiss() },
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text("Fisher's Iris Flower Dataset (150 Cases, 5 Variables)", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { onLoadStudents(); onDismiss() },
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text("Students Academic Performance (80 Cases, Exam Scores)", fontSize = 11.sp)
                    }
                } else {
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        placeholder = { Text("Paste CSV or Tab-delimited text with header row here...", fontSize = 11.sp) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (rawText.isNotBlank()) onPasteCsv(rawText)
                            onDismiss()
                        },
                        enabled = rawText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(32.dp)
                    ) {
                        Text("Import Pasted Table", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.height(28.dp)) {
                        Text("Close", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CellEditDialog(
    currentValue: String,
    row: Int,
    varName: String,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentValue) }
    val bg = if (isDark) Color(0xFF121212) else Color.White
    val textCol = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = bg,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)),
            modifier = Modifier.width(360.dp).padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Edit Value (Case ${row + 1} : $varName)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textCol
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().testTag("cell_edit_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Cancel", fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(text); onDismiss() },
                        modifier = Modifier.height(34.dp).testTag("cell_edit_save"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Update Cell", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutTrttDialog(isDark: Boolean, onDismiss: () -> Unit) {
    val bg = if (isDark) Color(0xFF121212) else Color.White
    val textCol = if (isDark) Color(0xFFF4F4F5) else Color(0xFF1E3A8A)
    val bodyCol = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = bg,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)),
            modifier = Modifier.width(420.dp).padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Statsys", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textCol)
                Text(text = "Desktop Statistical Analysis Engine", fontSize = 12.sp, color = if (isDark) Color(0xFF38BDF8) else Color(0xFF64748B))

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "An offline desktop-grade statistical computing environment engineered for Android. Features full SPSS-compatible data management, Variable View specification, descriptive & inferential tests, syntax interpretation, Android File Manager Open & Save, dark theme, and publication-ready tables & charts.",
                    fontSize = 12.sp,
                    color = bodyCol,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Version: 2.7.0 (Offline Native with Dark Theme)", fontSize = 11.sp, color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B))

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss, modifier = Modifier.height(30.dp)) {
                        Text("Close", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun VariableListBox(
    variables: List<String>,
    selected: String?,
    isDark: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val boxBg = if (isDark) Color(0xFF09090B) else Color.White
    val borderCol = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
    val selBg = if (isDark) Color(0xFF1E3A8A) else Color(0xFFBFDBFE)
    val textNormal = if (isDark) Color(0xFFF4F4F5) else Color(0xFF1E293B)
    val textSel = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E3A8A)

    Box(
        modifier = modifier
            .background(boxBg, RoundedCornerShape(2.dp))
            .border(1.dp, borderCol, RoundedCornerShape(2.dp))
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            items(variables) { v ->
                val isSel = v == selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(if (isSel) selBg else Color.Transparent)
                        .clickable { onSelect(v) }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = v,
                        fontSize = 11.sp,
                        color = if (isSel) textSel else textNormal,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
