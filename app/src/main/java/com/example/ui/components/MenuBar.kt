package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class MenuId {
    FILE, EDIT, VIEW, DATA, TRANSFORM, ANALYZE, GRAPHS, UTILITIES, EXTENSIONS, WINDOW, HELP
}

@Composable
fun TopMenuBar(
    activeMenu: MenuId?,
    onMenuToggle: (MenuId?) -> Unit,
    onAction: (String) -> Unit,
    showStatusBar: Boolean,
    onToggleStatusBar: () -> Unit,
    showValueLabels: Boolean,
    onToggleValueLabels: () -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    var subMenuOpen by remember { mutableStateOf<String?>(null) }

    val barBg = if (isDark) Color(0xFF18181B) else Color(0xFFF3F4F6)
    val barBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val textNormal = if (isDark) Color(0xFFE4E4E7) else Color(0xFF1F2937)
    val textSelected = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
    val itemSelectedBg = if (isDark) Color(0xFF27272A) else Color(0xFFE0E7FF)
    val dropdownBg = if (isDark) Color(0xFF121212) else Color.White
    val dropdownBorder = if (isDark) Color(0xFF3F3F46) else Color(0xFFCBD5E1)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(barBg)
            .border(width = 0.5.dp, color = barBorder)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            val menus = listOf(
                MenuId.FILE to "File",
                MenuId.EDIT to "Edit",
                MenuId.VIEW to "View",
                MenuId.DATA to "Data",
                MenuId.TRANSFORM to "Transform",
                MenuId.ANALYZE to "Analyze",
                MenuId.GRAPHS to "Graphs",
                MenuId.UTILITIES to "Utilities",
                MenuId.EXTENSIONS to "Extensions",
                MenuId.WINDOW to "Window",
                MenuId.HELP to "Help"
            )

            menus.forEach { (menuId, label) ->
                Box {
                    val isSelected = activeMenu == menuId
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = if (isSelected) textSelected else textNormal,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .testTag("menu_bar_${label.lowercase()}")
                            .background(if (isSelected) itemSelectedBg else Color.Transparent)
                            .clickable {
                                subMenuOpen = null
                                onMenuToggle(if (isSelected) null else menuId)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    // Top-level dropdown
                    DropdownMenu(
                        expanded = isSelected,
                        onDismissRequest = {
                            onMenuToggle(null)
                            subMenuOpen = null
                        },
                        offset = DpOffset(0.dp, 0.dp),
                        modifier = Modifier
                            .background(dropdownBg)
                            .border(1.dp, dropdownBorder)
                    ) {
                        when (menuId) {
                            MenuId.FILE -> {
                                MenuActionItem("New Dataset", onClick = { onAction("FILE_NEW"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Open Data (File Manager)...", shortcut = "Ctrl+O", onClick = { onAction("FILE_OPEN_SAF"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Import Data (CSV/Excel)...", onClick = { onAction("FILE_IMPORT_DIALOG"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Open Syntax...", onClick = { onAction("FILE_OPEN_SYNTAX"); onMenuToggle(null) }, isDark = isDark)
                                HorizontalDivider(color = dropdownBorder)
                                MenuActionItem("Save to Android Files (CSV)...", shortcut = "Ctrl+S", onClick = { onAction("FILE_SAVE_SAF"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Save As...", onClick = { onAction("FILE_SAVE_SAF"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Save Syntax As...", onClick = { onAction("FILE_SAVE_SYNTAX"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Export Output Report...", onClick = { onAction("FILE_EXPORT_OUTPUT_SAF"); onMenuToggle(null) }, isDark = isDark)
                            }
                            MenuId.EDIT -> {
                                MenuActionItem("Undo", shortcut = "Ctrl+Z", onClick = { onAction("EDIT_UNDO"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Redo", shortcut = "Ctrl+Shift+Z", onClick = { onAction("EDIT_REDO"); onMenuToggle(null) }, isDark = isDark)
                                HorizontalDivider(color = dropdownBorder)
                                MenuActionItem("Cut", shortcut = "Ctrl+X", onClick = { onAction("EDIT_CUT"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Copy", shortcut = "Ctrl+C", onClick = { onAction("EDIT_COPY"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Paste", shortcut = "Ctrl+V", onClick = { onAction("EDIT_PASTE"); onMenuToggle(null) }, isDark = isDark)
                                HorizontalDivider(color = dropdownBorder)
                                MenuActionItem("Insert Variable", onClick = { onAction("EDIT_INSERT_VAR"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Insert Cases", onClick = { onAction("EDIT_INSERT_CASES"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Delete Cases...", onClick = { onAction("EDIT_DELETE_CASES"); onMenuToggle(null) }, isDark = isDark)
                                HorizontalDivider(color = dropdownBorder)
                                MenuActionItem("Delete Variable...", onClick = { onAction("EDIT_DELETE_VAR"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Go to Variable...", onClick = { onAction("EDIT_GOTO_VAR"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Clear Variable...", onClick = { onAction("EDIT_CLEAR_VAR"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Options...", onClick = { onAction("EDIT_OPTIONS"); onMenuToggle(null) }, isDark = isDark)
                            }
                            MenuId.VIEW -> {
                                MenuActionItem(
                                    label = "Status Bar",
                                    checked = showStatusBar,
                                    onClick = { onToggleStatusBar(); onMenuToggle(null) },
                                    isDark = isDark
                                )
                                MenuActionItem(
                                    label = "Value Labels",
                                    checked = showValueLabels,
                                    onClick = { onToggleValueLabels(); onMenuToggle(null) },
                                    isDark = isDark
                                )
                                MenuActionItem(
                                    label = "Dark Theme",
                                    checked = isDark,
                                    onClick = { onToggleTheme(); onMenuToggle(null) },
                                    isDark = isDark
                                )
                            }
                            MenuId.DATA -> {
                                MenuActionItem("Sort Cases...", onClick = { onAction("DATA_SORT"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Sort Cases with Filter...", onClick = { onAction("DATA_SORT_FILTER"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Split File...", onClick = { onAction("DATA_SPLIT"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Select Cases...", onClick = { onAction("DATA_SELECT"); onMenuToggle(null) }, isDark = isDark)
                                HorizontalDivider(color = dropdownBorder)
                                MenuActionItem("Weight Cases...", onClick = { onAction("DATA_WEIGHT_CASES"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Transpose...", onClick = { onAction("DATA_TRANSPOSE"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Aggregate...", onClick = { onAction("DATA_AGGREGATE"); onMenuToggle(null) }, isDark = isDark)
                            }
                            MenuId.TRANSFORM -> {
                                MenuActionItem("Compute Variable (e.g. BMI)...", onClick = { onAction("TRANS_COMPUTE"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Count Values within Cases...", onClick = { onAction("TRANS_COUNT"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Shift Value (Lead/Lag)...", onClick = { onAction("TRANS_SHIFT"); onMenuToggle(null) }, isDark = isDark)
                                HorizontalDivider(color = dropdownBorder)
                                MenuActionItem("Recode into Same Variables...", onClick = { onAction("TRANS_RECODE_SAME"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Recode into Different Variables...", onClick = { onAction("TRANS_RECODE_DIFF"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Automatic Recode...", onClick = { onAction("TRANS_AUTO_RECODE"); onMenuToggle(null) }, isDark = isDark)
                                HorizontalDivider(color = dropdownBorder)
                                MenuActionItem("Rank Cases...", onClick = { onAction("TRANS_RANK"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Replace Missing Values...", onClick = { onAction("TRANS_REPLACE_MISSING"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Run Pending Transforms", onClick = { onAction("TRANS_RUN_PENDING"); onMenuToggle(null) }, isDark = isDark)
                            }
                            MenuId.ANALYZE -> {
                                SubMenuHeaderItem("Descriptive Statistics", isOpen = subMenuOpen == "DESCRIPTIVE", isDark = isDark) {
                                    subMenuOpen = if (subMenuOpen == "DESCRIPTIVE") null else "DESCRIPTIVE"
                                }
                                if (subMenuOpen == "DESCRIPTIVE") {
                                    Column(modifier = Modifier.padding(start = 16.dp)) {
                                        SubMenuItem("Frequencies...", isDark = isDark) { onAction("ANALYZE_FREQUENCIES"); onMenuToggle(null) }
                                        SubMenuItem("Descriptives...", isDark = isDark) { onAction("ANALYZE_DESCRIPTIVES"); onMenuToggle(null) }
                                        SubMenuItem("Explore...", isDark = isDark) { onAction("ANALYZE_EXPLORE"); onMenuToggle(null) }
                                        SubMenuItem("Crosstabs...", isDark = isDark) { onAction("ANALYZE_CROSSTABS"); onMenuToggle(null) }
                                        SubMenuItem("Tables...", isDark = isDark) { onAction("ANALYZE_TABLES"); onMenuToggle(null) }
                                    }
                                }

                                SubMenuHeaderItem("Compare Means and Proportions", isOpen = subMenuOpen == "COMPARE_MEANS", isDark = isDark) {
                                    subMenuOpen = if (subMenuOpen == "COMPARE_MEANS") null else "COMPARE_MEANS"
                                }
                                if (subMenuOpen == "COMPARE_MEANS") {
                                    Column(modifier = Modifier.padding(start = 16.dp)) {
                                        SubMenuItem("One-Sample T Test...", isDark = isDark) { onAction("ANALYZE_ONE_SAMPLE_T"); onMenuToggle(null) }
                                        SubMenuItem("Independent-Samples T Test...", isDark = isDark) { onAction("ANALYZE_IND_T"); onMenuToggle(null) }
                                        SubMenuItem("Paired-Samples T Test...", isDark = isDark) { onAction("ANALYZE_PAIRED_T"); onMenuToggle(null) }
                                        SubMenuItem("One-Way ANOVA...", isDark = isDark) { onAction("ANALYZE_ANOVA"); onMenuToggle(null) }
                                        SubMenuItem("Comparison of Proportions (Z-test)...", isDark = isDark) { onAction("ANALYZE_PROP_Z"); onMenuToggle(null) }
                                    }
                                }

                                SubMenuHeaderItem("Correlate & Association", isOpen = subMenuOpen == "CORRELATE", isDark = isDark) {
                                    subMenuOpen = if (subMenuOpen == "CORRELATE") null else "CORRELATE"
                                }
                                if (subMenuOpen == "CORRELATE") {
                                    Column(modifier = Modifier.padding(start = 16.dp)) {
                                        SubMenuItem("Bivariate Correlation...", isDark = isDark) { onAction("ANALYZE_BIVARIATE_CORR"); onMenuToggle(null) }
                                        SubMenuItem("2×2 Consistency & Odds Ratio...", isDark = isDark) { onAction("ANALYZE_2X2_OR"); onMenuToggle(null) }
                                        SubMenuItem("Chi-Square Test...", isDark = isDark) { onAction("ANALYZE_CHISQUARE"); onMenuToggle(null) }
                                        SubMenuItem("Fisher's Exact Test...", isDark = isDark) { onAction("ANALYZE_FISHER"); onMenuToggle(null) }
                                    }
                                }

                                SubMenuHeaderItem("Regression", isOpen = subMenuOpen == "REGRESSION", isDark = isDark) {
                                    subMenuOpen = if (subMenuOpen == "REGRESSION") null else "REGRESSION"
                                }
                                if (subMenuOpen == "REGRESSION") {
                                    Column(modifier = Modifier.padding(start = 16.dp)) {
                                        SubMenuItem("Linear Regression...", isDark = isDark) { onAction("ANALYZE_LINEAR_REG"); onMenuToggle(null) }
                                        SubMenuItem("Binary Logistic Regression...", isDark = isDark) { onAction("ANALYZE_LOGISTIC_REG"); onMenuToggle(null) }
                                        SubMenuItem("Ordinal Regression...", isDark = isDark) { onAction("ANALYZE_ORDINAL_REG"); onMenuToggle(null) }
                                    }
                                }

                                SubMenuHeaderItem("General Linear Model", isOpen = subMenuOpen == "GLM", isDark = isDark) {
                                    subMenuOpen = if (subMenuOpen == "GLM") null else "GLM"
                                }
                                if (subMenuOpen == "GLM") {
                                    Column(modifier = Modifier.padding(start = 16.dp)) {
                                        SubMenuItem("Univariate Analysis...", isDark = isDark) { onAction("ANALYZE_UNIVARIATE"); onMenuToggle(null) }
                                        SubMenuItem("General Linear Model (GLM)...", isDark = isDark) { onAction("ANALYZE_GLM"); onMenuToggle(null) }
                                    }
                                }

                                SubMenuHeaderItem("Classify (Cluster)", isOpen = subMenuOpen == "CLASSIFY", isDark = isDark) {
                                    subMenuOpen = if (subMenuOpen == "CLASSIFY") null else "CLASSIFY"
                                }
                                if (subMenuOpen == "CLASSIFY") {
                                    Column(modifier = Modifier.padding(start = 16.dp)) {
                                        SubMenuItem("K-Means Cluster...", isDark = isDark) { onAction("ANALYZE_KMEANS"); onMenuToggle(null) }
                                        SubMenuItem("Two-Step Cluster...", isDark = isDark) { onAction("ANALYZE_TWOSTEP"); onMenuToggle(null) }
                                        SubMenuItem("Hierarchical Cluster...", isDark = isDark) { onAction("ANALYZE_HIERARCHICAL"); onMenuToggle(null) }
                                        SubMenuItem("Discriminant Cluster...", isDark = isDark) { onAction("ANALYZE_DISCRIMINANT"); onMenuToggle(null) }
                                        SubMenuItem("Nearest Neighbors Cluster...", isDark = isDark) { onAction("ANALYZE_KNN"); onMenuToggle(null) }
                                    }
                                }

                                SubMenuHeaderItem("Nonparametric Tests", isOpen = subMenuOpen == "NONPAR", isDark = isDark) {
                                    subMenuOpen = if (subMenuOpen == "NONPAR") null else "NONPAR"
                                }
                                if (subMenuOpen == "NONPAR") {
                                    Column(modifier = Modifier.padding(start = 16.dp)) {
                                        SubMenuItem("Mann-Whitney U Test...", isDark = isDark) { onAction("ANALYZE_MANN_WHITNEY"); onMenuToggle(null) }
                                        SubMenuItem("Binomial Test...", isDark = isDark) { onAction("ANALYZE_BINOMIAL"); onMenuToggle(null) }
                                        SubMenuItem("1-Sample K-S Test...", isDark = isDark) { onAction("ANALYZE_1S_KS"); onMenuToggle(null) }
                                        SubMenuItem("2 Related Samples...", isDark = isDark) { onAction("ANALYZE_2_RELATED"); onMenuToggle(null) }
                                        SubMenuItem("K Related Samples...", isDark = isDark) { onAction("ANALYZE_K_RELATED"); onMenuToggle(null) }
                                        SubMenuItem("K Independent Samples...", isDark = isDark) { onAction("ANALYZE_K_INDEP"); onMenuToggle(null) }
                                    }
                                }

                                SubMenuHeaderItem("Scale & Reliability", isOpen = subMenuOpen == "SCALE", isDark = isDark) {
                                    subMenuOpen = if (subMenuOpen == "SCALE") null else "SCALE"
                                }
                                if (subMenuOpen == "SCALE") {
                                    Column(modifier = Modifier.padding(start = 16.dp)) {
                                        SubMenuItem("Reliability Analysis (Alpha)...", isDark = isDark) { onAction("ANALYZE_RELIABILITY"); onMenuToggle(null) }
                                    }
                                }

                                SubMenuHeaderItem("Dimension Reduction", isOpen = subMenuOpen == "DIM_REDUCTION", isDark = isDark) {
                                    subMenuOpen = if (subMenuOpen == "DIM_REDUCTION") null else "DIM_REDUCTION"
                                }
                                if (subMenuOpen == "DIM_REDUCTION") {
                                    Column(modifier = Modifier.padding(start = 16.dp)) {
                                        SubMenuItem("Factor Analysis...", isDark = isDark) { onAction("ANALYZE_FACTOR"); onMenuToggle(null) }
                                    }
                                }

                                SubMenuHeaderItem("Survival & Diagnostics", isOpen = subMenuOpen == "SURVIVAL", isDark = isDark) {
                                    subMenuOpen = if (subMenuOpen == "SURVIVAL") null else "SURVIVAL"
                                }
                                if (subMenuOpen == "SURVIVAL") {
                                    Column(modifier = Modifier.padding(start = 16.dp)) {
                                        SubMenuItem("Kaplan-Meier Test...", isDark = isDark) { onAction("ANALYZE_KM_TEST"); onMenuToggle(null) }
                                        SubMenuItem("ROC Curve with AUC...", isDark = isDark) { onAction("ANALYZE_ROC_TEST"); onMenuToggle(null) }
                                    }
                                }
                            }
                            MenuId.GRAPHS -> {
                                MenuActionItem("Histogram...", onClick = { onAction("GRAPH_HISTOGRAM"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Histogram with Plotted Formula Curve...", onClick = { onAction("GRAPH_HIST_FORMULA"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Scatter/Dot Plot...", onClick = { onAction("GRAPH_SCATTER"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Scatter Plot with Trend Line...", onClick = { onAction("GRAPH_SCATTER_TREND"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Bar Chart...", onClick = { onAction("GRAPH_BAR"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Pie Chart...", onClick = { onAction("GRAPH_PIE"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Line Diagram...", onClick = { onAction("GRAPH_LINE"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("ROC Curve with AUC...", onClick = { onAction("GRAPH_ROC"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Kaplan-Meier Survival Curve...", onClick = { onAction("GRAPH_KAPLAN_MEIER"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Box Plot with Outliers...", onClick = { onAction("GRAPH_BOXPLOT"); onMenuToggle(null) }, isDark = isDark)
                            }
                            MenuId.UTILITIES -> {
                                MenuActionItem("Variables...", onClick = { onAction("WINDOW_VARIABLE_VIEW"); onMenuToggle(null) }, isDark = isDark)
                            }
                            MenuId.EXTENSIONS -> {
                                MenuActionItem("Extension Hub...", onClick = { onAction("EXTENSIONS_HUB"); onMenuToggle(null) }, isDark = isDark)
                            }
                            MenuId.WINDOW -> {
                                MenuActionItem("Data Editor", onClick = { onAction("WINDOW_DATA_EDITOR"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Variable View", onClick = { onAction("WINDOW_VARIABLE_VIEW"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Syntax", onClick = { onAction("WINDOW_SYNTAX"); onMenuToggle(null) }, isDark = isDark)
                                MenuActionItem("Output", onClick = { onAction("WINDOW_OUTPUT"); onMenuToggle(null) }, isDark = isDark)
                            }
                            MenuId.HELP -> {
                                MenuActionItem("About Statsys", onClick = { onAction("HELP_ABOUT"); onMenuToggle(null) }, isDark = isDark)
                            }
                        }
                    }
                }
            }
        }

        // Right side badges + Dark Theme Toggle Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.size(26.dp).testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                    contentDescription = "Toggle Dark/Light Theme",
                    tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF475569),
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = "Statsys",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF374151)
            )
            Text(
                text = if (isDark) "Dark (Black)" else "Light",
                fontSize = 10.sp,
                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
                modifier = Modifier
                    .background(if (isDark) Color(0xFF0F172A) else Color(0xFFDBEAFE), RoundedCornerShape(2.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun MenuActionItem(
    label: String,
    shortcut: String? = null,
    checked: Boolean? = null,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val textCol = if (isDark) Color(0xFFF4F4F5) else Color(0xFF1E293B)
    val shortcutCol = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (checked != null) {
                        if (checked) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Checked",
                                tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = textCol
                    )
                }
                if (shortcut != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = shortcut,
                        fontSize = 11.sp,
                        color = shortcutCol
                    )
                }
            }
        },
        onClick = onClick,
        modifier = Modifier.height(30.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
    )
}

@Composable
private fun SubMenuHeaderItem(
    label: String,
    isOpen: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val textCol = if (isDark) Color(0xFFF4F4F5) else Color(0xFF1E293B)
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = textCol
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                    contentDescription = "Expand submenu",
                    modifier = Modifier.size(14.dp),
                    tint = if (isOpen) (if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)) else (if (isDark) Color(0xFF71717A) else Color(0xFF64748B))
                )
            }
        },
        onClick = onClick,
        modifier = Modifier.height(30.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
    )
}

@Composable
private fun SubMenuItem(
    label: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val textCol = if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155)
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                fontSize = 11.5.sp,
                color = textCol
            )
        },
        onClick = onClick,
        modifier = Modifier.height(28.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
    )
}
