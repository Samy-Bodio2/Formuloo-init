package com.formuloo.feature.admin.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.common.UiState
import com.formuloo.core.designsystem.CalculateIcon
import com.formuloo.core.designsystem.FormulooButton
import com.formuloo.core.designsystem.FormulooDropdownField
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutlinedButton
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextField
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.InventoryIcon
import com.formuloo.core.designsystem.ModuleCheckCard
import com.formuloo.core.designsystem.PeopleIcon
import com.formuloo.core.designsystem.ProjectsIcon
import com.formuloo.core.designsystem.TrendingUpIcon
import com.formuloo.feature.admin.domain.model.ModuleOption
import com.formuloo.feature.admin.presentation.viewmodel.InviteUserViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * NOTE produit : la maquette ne comporte pas de champs Prénom/Nom, mais
 * `POST /invite/` (InviteSerializer) les exige côté backend — ils sont donc
 * conservés ici, restylés en pleine largeur dans le même style que le champ
 * e-mail plutôt que retirés, pour que l'invitation reste fonctionnelle.
 */
@Composable
fun InviteUserDialog(
    onDismiss: () -> Unit,
    onInvited: () -> Unit,
    viewModel: InviteUserViewModel = koinViewModel(),
) {
    var email by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }

    val roles by viewModel.roles.collectAsStateWithLifecycle()
    val selectedRoleCode by viewModel.selectedRoleCode.collectAsStateWithLifecycle()
    val selectedModuleKeys by viewModel.selectedModuleKeys.collectAsStateWithLifecycle()
    val inviteState by viewModel.inviteState.collectAsStateWithLifecycle()

    val selectedRole = roles.firstOrNull { it.code == selectedRoleCode }
    val canSubmit = email.isNotBlank() && firstName.isNotBlank() && lastName.isNotBlank() && selectedRoleCode != null

    LaunchedEffect(roles) {
        if (selectedRoleCode == null && roles.isNotEmpty()) {
            viewModel.selectRole(roles.first().code)
        }
    }

    LaunchedEffect(inviteState) {
        if (inviteState is UiState.Success) onInvited()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
            Surface(
                color = FormulooSurface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Poignée de drag (pattern bottom sheet)
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(FormulooOutline),
                        )
                    }

                    // En-tête : badge enveloppe + titre/sous-titre + bouton fermer
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(FormulooMint),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = FormulooPrimary)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f).padding(top = 2.dp)) {
                            Text(
                                "Inviter un utilisateur",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = FormulooTextPrimary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Un e-mail d'invitation sera envoyé avec un lien d'inscription.",
                                fontSize = 13.sp,
                                color = FormulooOnSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = FormulooOnSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Contenu scrollable : champs, rôle, modules
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                    ) {
                        FieldLabel("Adresse e-mail", required = true)
                        FormulooTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = "",
                            placeholder = "collaborateur@sahel-distrib.cm",
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(16.dp))
                        FieldLabel("Prénom", required = true)
                        FormulooTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = "",
                            placeholder = "Awa",
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(16.dp))
                        FieldLabel("Nom", required = true)
                        FormulooTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = "",
                            placeholder = "Traoré",
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(16.dp))
                        FieldLabel("Rôle", required = true)
                        FormulooDropdownField(
                            value = selectedRole?.name ?: "",
                            onValueChange = { name -> roles.firstOrNull { it.name == name }?.let { viewModel.selectRole(it.code) } },
                            label = "",
                            options = roles.map { it.name },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (selectedRole != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(selectedRole.description, fontSize = 13.sp, color = FormulooOnSurfaceVariant)
                        }

                        Spacer(Modifier.height(24.dp))
                        Text(
                            "Modules accessibles",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = FormulooTextPrimary,
                        )
                        Spacer(Modifier.height(12.dp))
                        ModuleList(
                            options = viewModel.moduleOptions,
                            selectedKeys = selectedModuleKeys,
                            onToggle = viewModel::toggleModule,
                        )

                        if (inviteState is UiState.Error) {
                            Spacer(Modifier.height(12.dp))
                            Text((inviteState as UiState.Error).message, color = FormulooError)
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Pied de page : Annuler + Envoyer l'invitation
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FormulooOutlinedButton(
                            text = "Annuler",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        )
                        FormulooButton(
                            text = "Envoyer l'invitation",
                            onClick = { viewModel.submit(firstName, lastName, email) },
                            enabled = canSubmit,
                            loading = inviteState is UiState.Loading,
                            showArrow = false,
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1.4f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String, required: Boolean) {
    Row {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = FormulooTextPrimary)
        if (required) {
            Text(" *", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = FormulooError)
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ModuleList(
    options: List<ModuleOption>,
    selectedKeys: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
            ModuleCheckCard(
                icon = { tint -> moduleIcon(option.key, tint) },
                label = option.label,
                // Un module "Bientôt disponible" ne doit jamais paraître coché : aucune
                // permission backend ne le couvre encore, même si le rôle sélectionné
                // (ex: Administrateur PME) inclut techniquement sa clé dans moduleKeys.
                checked = option.supported && option.key in selectedKeys,
                onCheckedChange = { onToggle(option.key) },
                enabled = option.supported,
                helperText = if (!option.supported) "Bientôt disponible" else null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun moduleIcon(key: String, tint: Color) {
    when (key) {
        "hr" -> PeopleIcon(tint = tint)
        "compta" -> CalculateIcon(tint = tint)
        "crm" -> Icon(Icons.Filled.Favorite, contentDescription = null, tint = tint)
        "stock" -> InventoryIcon(tint = tint)
        "projects" -> ProjectsIcon(tint = tint)
        "analytics" -> TrendingUpIcon(tint = tint)
    }
}
