package com.formuloo.feature.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formuloo.core.auth.domain.model.UserProfile
import com.formuloo.core.designsystem.CalculateIcon
import com.formuloo.feature.hr.presentation.screen.hasHrManagerAccess
import com.formuloo.core.designsystem.DashboardIcon
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnPrimary
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.InitialsAvatar
import com.formuloo.core.designsystem.InventoryIcon
import com.formuloo.core.designsystem.NotificationBadge
import com.formuloo.core.designsystem.PeopleIcon
import com.formuloo.core.designsystem.ProjectsIcon
import com.formuloo.core.designsystem.TargetIcon
import com.formuloo.core.designsystem.TrendingUpIcon

private data class DrawerItem(
    val key: String,
    val label: String,
    val icon: @Composable (Color) -> Unit,
    val badgeCount: Int = 0,
)

private val hrDrawerItem = DrawerItem("hr", "RH", { tint -> PeopleIcon(tint = tint) })
private val adminUsersDrawerItem =
    DrawerItem("admin_users", "Utilisateurs & Rôles", { tint -> Icon(Icons.Filled.People, contentDescription = null, tint = tint) })

private fun drawerItemsFor(roles: List<String>): List<DrawerItem> = buildList {
    add(DrawerItem("dashboard", "Tableau de bord", { tint -> DashboardIcon(tint = tint) }))
    if (hasHrManagerAccess(roles)) add(hrDrawerItem)
    add(DrawerItem("accounting", "Comptabilité", { tint -> CalculateIcon(tint = tint) }))
    add(DrawerItem("crm", "CRM", { tint -> TargetIcon(tint = tint) }))
    add(DrawerItem("stock", "Stock", { tint -> InventoryIcon(tint = tint) }))
    add(DrawerItem("projects", "Projets", { tint -> ProjectsIcon(tint = tint) }))
    add(DrawerItem("analytics", "Analytics", { tint -> TrendingUpIcon(tint = tint) }))
    add(DrawerItem("settings", "Paramètres", { tint -> Icon(Icons.Filled.Settings, contentDescription = null, tint = tint) }))
    if (roles.any { it.equals("admin_pme", ignoreCase = true) || it.equals("super_admin", ignoreCase = true) }) {
        add(adminUsersDrawerItem)
    }
}

@Composable
fun AppDrawer(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    user: UserProfile?,
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FormulooPrimary)
                .padding(24.dp),
        ) {
            InitialsAvatar(
                initials = user?.initials.takeUnless { it.isNullOrBlank() } ?: "?",
                size = 56.dp,
                backgroundColor = FormulooOnPrimary.copy(alpha = 0.2f),
                contentColor = FormulooOnPrimary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = user?.fullName.takeUnless { it.isNullOrBlank() } ?: "Utilisateur",
                color = FormulooOnPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = user?.email.orEmpty(),
                color = FormulooOnPrimary.copy(alpha = 0.7f),
                fontSize = 13.sp,
            )
        }

        Spacer(Modifier.height(8.dp))

        drawerItemsFor(user?.roles ?: emptyList()).forEach { item ->
            val selected = currentRoute == item.key
            val contentColor = if (selected) FormulooPrimary else FormulooTextPrimary
            NavigationDrawerItem(
                label = { Text(item.label) },
                selected = selected,
                icon = { item.icon(contentColor) },
                badge = if (!selected && item.badgeCount > 0) {
                    { NotificationBadge(item.badgeCount) }
                } else {
                    null
                },
                onClick = { onNavigate(item.key) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = FormulooMint,
                    unselectedContainerColor = Color.Transparent,
                    selectedIconColor = FormulooPrimary,
                    selectedTextColor = FormulooPrimary,
                    unselectedIconColor = FormulooTextPrimary,
                    unselectedTextColor = FormulooTextPrimary,
                ),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        HorizontalDivider()

        NavigationDrawerItem(
            label = { Text("Se déconnecter", color = FormulooError) },
            selected = false,
            icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = FormulooError) },
            onClick = onLogout,
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent,
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
