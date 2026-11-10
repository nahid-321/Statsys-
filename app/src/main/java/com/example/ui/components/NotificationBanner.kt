package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotificationBanner(
    datasetName: String,
    onKeep: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF9C3)) // soft yellow matching screenshot
            .border(width = 0.5.dp, color = Color(0xFFFDE047))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Restored previous session — ",
                fontSize = 12.sp,
                color = Color(0xFF713F12)
            )
            Text(
                text = "$datasetName ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF713F12)
            )
            Text(
                text = "(saved 11/2/2026, 9:39:33 PM)",
                fontSize = 11.sp,
                color = Color(0xFF854D0E)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onKeep,
                modifier = Modifier
                    .height(26.dp)
                    .testTag("banner_keep_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                shape = RoundedCornerShape(3.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1E293B)
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
            ) {
                Text(text = "Keep", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier
                    .height(26.dp)
                    .testTag("banner_discard_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                shape = RoundedCornerShape(3.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFFDC2626)
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
            ) {
                Text(text = "Discard", fontSize = 11.sp)
            }
        }
    }
}
