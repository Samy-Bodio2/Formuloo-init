package com.formuloo.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formuloo.core.designsystem.BusinessIcon
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooButton
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooSecondaryBg
import com.formuloo.core.designsystem.FormulooSecondarySelectedBg
import com.formuloo.core.designsystem.FormulooSurface

enum class AccountType(val value: String, val title: String, val description: String) {
    PARTICULIER(
        value = "particulier",
        title = "Particulier",
        description = "Pour gérer vos finances et projets personnels",
    ),
    ENTREPRISE(
        value = "entreprise",
        title = "Entreprise",
        description = "Pour gérer votre PME avec vos collaborateurs",
    ),
}

@Composable
fun AccountTypeScreen(
    onBack: () -> Unit,
    onContinue: (AccountType) -> Unit,
) {
    var selected by remember { mutableStateOf<AccountType?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormulooBackground)
            .padding(24.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("< Retour", color = FormulooPrimary)
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Créer un compte",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Quel type de compte souhaitez-vous créer ?",
            style = MaterialTheme.typography.bodyLarge,
            color = FormulooOnSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        AccountTypeCard(
            title = AccountType.PARTICULIER.title,
            description = AccountType.PARTICULIER.description,
            badgeText = "Gratuit",
            badgeBackgroundColor = FormulooMint,
            badgeContentColor = FormulooPrimary,
            icon = { Icon(Icons.Filled.Person, contentDescription = null, tint = FormulooPrimary) },
            iconBackgroundColor = FormulooMint,
            selected = selected == AccountType.PARTICULIER,
            selectedBorderColor = FormulooPrimary,
            selectedBackgroundColor = FormulooMint,
            fadedOut = selected == AccountType.ENTREPRISE,
            onClick = { selected = AccountType.PARTICULIER },
        )

        Spacer(Modifier.height(16.dp))

        AccountTypeCard(
            title = AccountType.ENTREPRISE.title,
            description = AccountType.ENTREPRISE.description,
            badgeText = "Pro",
            badgeBackgroundColor = FormulooSecondaryBg,
            badgeContentColor = FormulooSecondary,
            icon = { BusinessIcon(tint = FormulooSecondary) },
            iconBackgroundColor = FormulooSecondaryBg,
            selected = selected == AccountType.ENTREPRISE,
            selectedBorderColor = FormulooSecondary,
            selectedBackgroundColor = FormulooSecondarySelectedBg,
            fadedOut = selected == AccountType.PARTICULIER,
            onClick = { selected = AccountType.ENTREPRISE },
        )

        Spacer(Modifier.height(16.dp))

        AccountTypeCard(
            title = "Administrateur",
            description = "Compte technique avec privilèges de sécurité, créé uniquement sur invitation.",
            badgeText = "Sur invitation",
            badgeBackgroundColor = FormulooOutline,
            badgeContentColor = FormulooOnSurfaceVariant,
            icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = FormulooOnSurfaceVariant) },
            iconBackgroundColor = FormulooBackground,
            selected = false,
            selectedBorderColor = FormulooOutline,
            selectedBackgroundColor = FormulooSurface,
            enabled = false,
            fadedOut = true,
            onClick = {},
        )

        Spacer(Modifier.weight(1f))

        FormulooButton(
            text = "Continuer",
            enabled = selected != null,
            onClick = { selected?.let(onContinue) },
        )
    }
}

@Composable
private fun AccountTypeCard(
    title: String,
    description: String,
    badgeText: String,
    badgeBackgroundColor: Color,
    badgeContentColor: Color,
    icon: @Composable () -> Unit,
    iconBackgroundColor: Color,
    selected: Boolean,
    selectedBorderColor: Color,
    selectedBackgroundColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    fadedOut: Boolean = false,
) {
    val borderColor = if (selected) selectedBorderColor else FormulooOutline
    val backgroundColor = if (selected) selectedBackgroundColor else FormulooSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (fadedOut) 0.6f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(8.dp))
                    AccountBadge(badgeText, badgeBackgroundColor, badgeContentColor)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FormulooOnSurfaceVariant,
                )
            }
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(selectedBorderColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun AccountBadge(text: String, backgroundColor: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold,
        )
    }
}
