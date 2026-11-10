package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SyntaxEditor(
    syntaxText: String,
    isDark: Boolean,
    onSyntaxChange: (String) -> Unit,
    onRunSyntax: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isDark) Color(0xFF000000) else Color(0xFFF8FAFC)
    val toolbarBg = if (isDark) Color(0xFF0A0A0A) else Color(0xFFF1F5F9)
    val toolbarBorder = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
    val editorBg = if (isDark) Color(0xFF09090B) else Color.White
    val editorBorder = if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
    val textColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    val templates = listOf(
        "Descriptives" to "DESCRIPTIVES VARIABLES=Age_years Height_m Weight_kg\n  /STATISTICS=MEAN STDDEV MIN MAX VARIANCE.",
        "Frequencies" to "FREQUENCIES VARIABLES=Sex TBI_Severity Cause_of_Injury\n  /ORDER=ANALYSIS.",
        "Independent T-Test" to "T-TEST GROUPS=Group('A','B')\n  /VARIABLES=Age_years Height_m Weight_kg.",
        "One-Way ANOVA" to "ONEWAY Age_years BY Cause_of_Injury\n  /STATISTICS DESCRIPTIVES.",
        "Bivariate Correlations" to "CORRELATIONS\n  /VARIABLES=Age_years Height_m Weight_kg ICP_mmHg MAP_mmHg.",
        "Linear Regression" to "REGRESSION\n  /DEPENDENT Weight_kg\n  /METHOD=ENTER Height_m Age_years.",
        "Histogram Chart" to "GRAPH /HISTOGRAM=Age_years.",
        "Scatter Plot" to "GRAPH /SCATTERPLOT=Height_m WITH Weight_kg."
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // Syntax Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(toolbarBg)
                .border(width = 0.5.dp, color = toolbarBorder)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onRunSyntax(syntaxText) },
                    modifier = Modifier.height(28.dp).testTag("syntax_run_all_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(3.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF15803D) else Color(0xFF16A34A))
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run All", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onSyntaxChange("") },
                    modifier = Modifier.height(28.dp).testTag("syntax_clear_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(3.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isDark) Color(0xFF18181B) else Color.White,
                        contentColor = Color(0xFFEF4444)
                    )
                ) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Clear", modifier = Modifier.size(12.dp), tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", fontSize = 11.sp, color = Color(0xFFEF4444))
                }
            }

            Text(
                text = "IBM SPSS Syntax Compatible Engine",
                fontSize = 11.sp,
                color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
            )
        }

        // Quick template insertion buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF))
                .border(width = 0.5.dp, color = toolbarBorder)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Insert:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFD4D4D8) else Color(0xFF334155)
            )
            templates.take(5).forEach { (name, snippet) ->
                SuggestionChip(
                    onClick = {
                        val newText = if (syntaxText.isBlank()) snippet else "$syntaxText\n\n$snippet"
                        onSyntaxChange(newText)
                    },
                    label = { Text(name, fontSize = 10.5.sp, color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E3A8A)) },
                    modifier = Modifier.height(24.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)
                    )
                )
            }
        }

        // Editor Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp)
                .background(editorBg, RoundedCornerShape(4.dp))
                .border(1.dp, editorBorder, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            OutlinedTextField(
                value = syntaxText,
                onValueChange = onSyntaxChange,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("syntax_text_field"),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    color = textColor,
                    lineHeight = 18.sp
                ),
                placeholder = {
                    Text(
                        text = "* Type or paste SPSS syntax here and click Run All...\n\nDESCRIPTIVES VARIABLES=Age_years Height_m Weight_kg\n  /STATISTICS=MEAN STDDEV MIN MAX.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }
    }
}
