package com.formuloo.feature.admin.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.common.UiState
import com.formuloo.core.designsystem.BadgeTone
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooBlue
import com.formuloo.core.designsystem.FormulooButton
import com.formuloo.core.designsystem.FormulooDeepOrange
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooGreen
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooOutlinedButton
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooPurple
import com.formuloo.core.designsystem.FormulooPurpleBg
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTeal
import com.formuloo.core.designsystem.FormulooTextField
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.InitialsAvatar
import com.formuloo.core.designsystem.StatusBadge
import com.formuloo.feature.admin.domain.model.AdminUser
import com.formuloo.feature.admin.domain.model.UserStats
import com.formuloo.feature.admin.domain.model.UserStatus
import com.formuloo.feature.admin.presentation.viewmodel.StatusFilter
import com.formuloo.feature.admin.presentation.viewmodel.UsersViewModel
import kotlin.math.abs
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    onBack: () -> Unit,
    onInviteUser: () -> Unit,
    viewModel: UsersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val totalResults by viewModel.totalResults.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }
    var menuForUserId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = { Text("Utilisateurs & Accès", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FormulooPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onInviteUser, containerColor = FormulooPrimary) {
                Icon(Icons.Filled.Add, contentDescription = "Inviter un utilisateur", tint = Color.White)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FormulooButton(
                    text = "Inviter un utilisateur",
                    onClick = onInviteUser,
                    showArrow = false,
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1.6f),
                )
                ExportFormatGroup(modifier = Modifier.weight(1f))
            }

            StatsRow(stats)

            FormulooTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = "Rechercher un nom ou e-mail",
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FormulooOutlinedButton(
                    text = "Filtres",
                    onClick = { showFilterSheet = true },
                    leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f),
                )
                FormulooOutlinedButton(
                    text = "Exporter",
                    onClick = { /* Export non câblé : aucune fonctionnalité serveur demandée pour ce bouton décoratif. */ },
                    leadingIcon = { Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = FormulooOutline)

            when (state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
                is UiState.Empty -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Aucun utilisateur ne correspond.", color = FormulooOnSurfaceVariant)
                }
                is UiState.Success -> {
                    val users = (state as UiState.Success<List<AdminUser>>).data
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    ) {
                        items(users, key = { it.id }) { user ->
                            UserRow(
                                user = user,
                                menuExpanded = menuForUserId == user.id,
                                onToggleMenu = { menuForUserId = if (menuForUserId == user.id) null else user.id },
                                onDismissMenu = { menuForUserId = null },
                                onToggleActive = { active -> viewModel.setUserActive(user.id, active) },
                            )
                        }
                        item {
                            PaginationFooter(
                                currentPage = currentPage,
                                totalPages = totalPages,
                                totalResults = totalResults,
                                onPrev = { viewModel.setPage(currentPage - 1) },
                                onNext = { viewModel.setPage(currentPage + 1) },
                            )
                        }
                    }
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text((state as UiState.Error).message, color = FormulooError)
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            selected = statusFilter,
            onSelect = {
                viewModel.setStatusFilter(it)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false },
        )
    }
}

private val statusFilterOptions = listOf(
    StatusFilter.ALL to "Tous",
    StatusFilter.ACTIVE to "Actifs",
    StatusFilter.INVITED to "Invités",
    StatusFilter.SUSPENDED to "Suspendus",
)

@Composable
private fun ExportFormatGroup(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(52.dp)
            .border(BorderStroke(1.dp, FormulooOutline), RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable { /* Export visuel décoratif — pas de fonctionnalité serveur demandée. */ },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.FileDownload, contentDescription = null, tint = FormulooOnSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("PNG", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FormulooTextPrimary)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(FormulooOutline))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable { /* Export visuel décoratif — pas de fonctionnalité serveur demandée. */ },
            contentAlignment = Alignment.Center,
        ) {
            Text("JPG", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FormulooTextPrimary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(selected: StatusFilter, onSelect: (StatusFilter) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = FormulooSurface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Filtrer par statut", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FormulooTextPrimary)
            Spacer(Modifier.height(8.dp))
            statusFilterOptions.forEach { (filter, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(filter) }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        fontSize = 15.sp,
                        color = FormulooTextPrimary,
                        fontWeight = if (filter == selected) FontWeight.Bold else FontWeight.Normal,
                    )
                    if (filter == selected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = FormulooPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(stats: UserStats) {
    LazyRow(
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { StatCard("Utilisateurs", stats.total.toString(), Icons.Filled.People) }
        item { StatCard("Actifs", stats.active.toString(), Icons.Filled.CheckCircle) }
        item { StatCard("Invités en attente", stats.invited.toString(), Icons.Filled.MailOutline) }
        item { StatCard("Suspendus", stats.suspended.toString(), Icons.Filled.Block) }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp).width(170.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(label, fontSize = 13.sp, color = FormulooOnSurfaceVariant, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(FormulooMint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = FormulooPrimary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = FormulooTextPrimary)
        }
    }
}

private val avatarPalette = listOf(FormulooPrimary, FormulooBlue, FormulooPurple, FormulooDeepOrange, FormulooGreen, FormulooTeal, FormulooSecondary)
private fun avatarColor(id: String): Color = avatarPalette[abs(id.hashCode()) % avatarPalette.size]

private fun moduleCountLabel(count: Int): String = if (count <= 1) "$count module" else "$count modules"

private fun formatDate(iso: String): String = try {
    val parts = iso.substringBefore("T").split("-")
    val months = listOf(
        "", "janv.", "févr.", "mars", "avr.", "mai", "juin",
        "juil.", "août", "sept.", "oct.", "nov.", "déc.",
    )
    "${parts[2].toInt()} ${months[parts[1].toInt()]} ${parts[0]}"
} catch (e: Exception) {
    iso
}

@Composable
private fun RoleBadge(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(FormulooPurpleBg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(FormulooPurple))
        Spacer(Modifier.width(6.dp))
        Text(label, color = FormulooPurple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun UserRow(
    user: AdminUser,
    menuExpanded: Boolean,
    onToggleMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                InitialsAvatar(initials = user.initials, size = 44.dp, backgroundColor = avatarColor(user.id))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FormulooTextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(user.email, fontSize = 12.sp, color = FormulooOnSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                StatusBadge(label = statusLabel(user.status), tone = statusTone(user.status), dot = true)
            }

            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("RÔLE")
                    Spacer(Modifier.height(6.dp))
                    RoleBadge(label = user.roleLabel)
                }
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("MODULES")
                    Spacer(Modifier.height(6.dp))
                    Text(moduleCountLabel(user.moduleCount), fontSize = 14.sp, color = FormulooTextPrimary, fontWeight = FontWeight.Medium)
                }
            }

            if (user.createdAt != null) {
                Spacer(Modifier.height(14.dp))
                Column {
                    SectionLabel("AJOUTÉ LE")
                    Spacer(Modifier.height(6.dp))
                    Text(formatDate(user.createdAt), fontSize = 14.sp, color = FormulooTextPrimary, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = FormulooOutline)
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormulooOutlinedButton(
                    text = "",
                    onClick = { /* Pas d'écran de détail utilisateur pour le moment. */ },
                    leadingIcon = { Icon(Icons.Filled.Visibility, contentDescription = "Voir le détail", modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f),
                )
                Box(modifier = Modifier.weight(1f)) {
                    FormulooOutlinedButton(
                        text = "",
                        onClick = onToggleMenu,
                        leadingIcon = { Icon(Icons.Filled.MoreVert, contentDescription = "Autres actions", modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                        if (user.status == UserStatus.INVITED) {
                            DropdownMenuItem(
                                text = { Text("En attente d'acceptation", color = FormulooOnSurfaceVariant) },
                                onClick = onDismissMenu,
                                enabled = false,
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(if (user.status == UserStatus.ACTIVE) "Désactiver" else "Activer") },
                                onClick = {
                                    onToggleActive(user.status != UserStatus.ACTIVE)
                                    onDismissMenu()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FormulooOnSurfaceVariant, letterSpacing = 0.5.sp)
}

@Composable
private fun PaginationFooter(currentPage: Int, totalPages: Int, totalResults: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$totalResults résultats", fontSize = 13.sp, color = FormulooOnSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev, enabled = currentPage > 1) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = "Page précédente",
                    tint = if (currentPage > 1) FormulooTextPrimary else FormulooOutline,
                )
            }
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(50)).background(FormulooPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text("$currentPage", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onNext, enabled = currentPage < totalPages) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Page suivante",
                    tint = if (currentPage < totalPages) FormulooTextPrimary else FormulooOutline,
                )
            }
        }
    }
}

private fun statusLabel(status: UserStatus): String = when (status) {
    UserStatus.ACTIVE -> "Actif"
    UserStatus.INVITED -> "Invité"
    UserStatus.SUSPENDED -> "Suspendu"
}

private fun statusTone(status: UserStatus): BadgeTone = when (status) {
    UserStatus.ACTIVE -> BadgeTone.SUCCESS
    UserStatus.INVITED -> BadgeTone.WARNING
    UserStatus.SUSPENDED -> BadgeTone.DANGER
}
