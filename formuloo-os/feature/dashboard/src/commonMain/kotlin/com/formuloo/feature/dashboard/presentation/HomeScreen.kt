package com.formuloo.feature.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.formuloo.feature.hr.presentation.screen.hasHrManagerAccess
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.auth.domain.model.UserProfile
import com.formuloo.core.common.model.ActivityItem
import com.formuloo.core.common.model.CompanyInfo
import com.formuloo.core.common.model.DashboardState
import com.formuloo.core.common.model.KpiCard
import com.formuloo.core.common.model.ModuleItem
import com.formuloo.core.common.model.PendingRequest
import com.formuloo.core.designsystem.ActivityListItem
import com.formuloo.core.designsystem.DashboardIcon
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooLabelGray
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooSecondaryBg
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.InitialsAvatar
import com.formuloo.core.designsystem.KpiCardItem
import com.formuloo.core.designsystem.ModuleCard
import com.formuloo.core.designsystem.PendingRequestItem
import com.formuloo.core.designsystem.SectionHeader
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile?,
    onNavigateToModule: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(userProfile) {
        userProfile?.let { viewModel.setUserProfile(it) }
    }

    val visibleModules = remember(state.modules, userProfile?.roles) {
        val roles = userProfile?.roles ?: emptyList()
        state.modules.filter { module ->
            when (module.id) {
                "hr" -> hasHrManagerAccess(roles)
                else -> true
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentRoute = "dashboard",
                onNavigate = { key ->
                    scope.launch { drawerState.close() }
                    onNavigateToModule(key)
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    onLogout()
                },
                user = userProfile,
            )
        },
    ) {
        Scaffold(
            containerColor = FormulooBackground,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { onNavigateToModule("notifications") }) {
                                Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                            }
                            if (state.notificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-6).dp, y = 6.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(FormulooSecondary),
                                )
                            }
                        }
                        InitialsAvatar(
                            initials = state.userInitials,
                            size = 36.dp,
                            backgroundColor = FormulooPrimary,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = FormulooBackground),
                )
            },
            bottomBar = {
                DashboardBottomBar(
                    notificationCount = state.notificationCount,
                    onNavigate = onNavigateToModule,
                    onQuickAdd = {},
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            ) {
                item { GreetingSection(state) }
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(16.dp))
                        CompanyCard(state.company)
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(20.dp))
                        KpiGrid(state.kpis)
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(20.dp))
                        ModulesSection(modules = visibleModules, onModuleClick = onNavigateToModule)
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(20.dp))
                        PendingSection(
                            requests = state.pendingRequests,
                            onApprove = viewModel::onApproveRequest,
                            onReject = viewModel::onRejectRequest,
                        )
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(20.dp))
                        ActivitySection(state.recentActivity)
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun GreetingSection(state: DashboardState) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "${state.greeting}, ${state.userName} 👋",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = FormulooTextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = state.dateLabel,
            fontSize = 13.sp,
            color = FormulooLabelGray,
        )
    }
}

@Composable
private fun CompanyCard(company: CompanyInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InitialsAvatar(initials = company.initials, size = 40.dp, backgroundColor = FormulooPrimary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(company.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FormulooTextPrimary)
                Text(
                    text = "${company.type} · ${company.userCount} utilisateurs · ${company.country}",
                    fontSize = 12.sp,
                    color = FormulooLabelGray,
                )
            }
            // Pas de gestionnaire de clic ici : la carte n'ouvre ni dropdown ni navigation
            // aujourd'hui — seul le chevron a été corrigé pour refléter un sélecteur,
            // le comportement réel reste à câbler séparément.
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = FormulooLabelGray,
            )
        }
    }
}

@Composable
private fun KpiGrid(kpis: List<KpiCard>) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        kpis.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { kpi ->
                    KpiCardItem(kpi = kpi, modifier = Modifier.weight(1f))
                }
                if (row.size < 2) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PendingSection(
    requests: List<PendingRequest>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("À valider", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FormulooTextPrimary)
            PendingCountBadge(count = requests.size)
        }
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = FormulooSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                requests.forEachIndexed { index, request ->
                    PendingRequestItem(
                        request = request,
                        onApprove = { onApprove(request.id) },
                        onReject = { onReject(request.id) },
                    )
                    if (index < requests.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingCountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(FormulooSecondaryBg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "• $count demandes",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = FormulooSecondary,
        )
    }
}

@Composable
private fun ActivitySection(items: List<ActivityItem>) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(title = "Activité récente")
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = FormulooSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                items.forEachIndexed { index, item ->
                    ActivityListItem(item = item)
                    if (index < items.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ModulesSection(modules: List<ModuleItem>, onModuleClick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(title = "Modules", actionLabel = "Tout voir", onAction = {})
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            modules.chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { module ->
                        ModuleCard(
                            module = module,
                            onClick = { onModuleClick(module.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size < 2) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardBottomBar(
    notificationCount: Int,
    onNavigate: (String) -> Unit,
    onQuickAdd: () -> Unit,
) {
    BottomAppBar(containerColor = FormulooSurface) {
        BottomBarItem(
            label = "Accueil",
            selected = true,
            onClick = { onNavigate("dashboard") },
            modifier = Modifier.weight(1f),
            icon = { tint -> DashboardIcon(tint = tint) },
        )
        BottomBarItem(
            label = "Modules",
            selected = false,
            onClick = { onNavigate("modules") },
            modifier = Modifier.weight(1f),
            icon = { tint -> Icon(Icons.Filled.Layers, contentDescription = null, tint = tint) },
        )
        BottomBarQuickAddItem(onClick = onQuickAdd, modifier = Modifier.weight(1f))
        BottomBarItem(
            label = "Notifs",
            selected = false,
            onClick = { onNavigate("notifications") },
            modifier = Modifier.weight(1f),
            showBadge = notificationCount > 0,
            icon = { tint -> Icon(Icons.Filled.Notifications, contentDescription = null, tint = tint) },
        )
        BottomBarItem(
            label = "Profil",
            selected = false,
            onClick = { onNavigate("profile") },
            modifier = Modifier.weight(1f),
            icon = { tint -> Icon(Icons.Filled.Person, contentDescription = null, tint = tint) },
        )
    }
}

/** Bouton "+" intégré dans la barre du bas (squircle), au lieu d'un FAB flottant
 * qui chevauchait le contenu scrollable. */
@Composable
private fun BottomBarQuickAddItem(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(FormulooSecondary)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = Color.White)
        }
    }
}

@Composable
private fun BottomBarItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
    icon: @Composable (Color) -> Unit,
) {
    val tint = if (selected) FormulooPrimary else FormulooLabelGray
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            icon(tint)
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(FormulooSecondary),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(text = label, fontSize = 11.sp, color = tint)
    }
}
