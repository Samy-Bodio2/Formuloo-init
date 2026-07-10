package com.formuloo.feature.gesdoc.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSurface

/**
 * Barre d'onglets partagée entre GesDocDashboardScreen (index 0) et
 * GesDocUploadScreen (index 1). L'onglet Audit (index 2) sera câblé plus tard.
 */
@Composable
internal fun GesDocTabBar(
    activeTabIndex: Int,
    onNavigateToDashboard: () -> Unit,
    onNavigateToUpload: () -> Unit,
    onNavigateToAudit: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FormulooSurface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GesDocTabPill(
                icon = Icons.Filled.Description,
                label = "Documents",
                selected = activeTabIndex == 0,
                onClick = onNavigateToDashboard,
            )
            GesDocTabPill(
                icon = Icons.Filled.Add,
                label = "Nouvelle pièce",
                selected = activeTabIndex == 1,
                onClick = onNavigateToUpload,
            )
            GesDocTabPill(
                icon = Icons.Filled.History,
                label = "Audit",
                selected = activeTabIndex == 2,
                onClick = onNavigateToAudit,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(FormulooOutline),
        )
    }
}

@Composable
internal fun GesDocTabPill(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (selected) FormulooPrimary else Color.Transparent
    val contentColor = if (selected) Color.White else FormulooOnSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
        )
    }
}
