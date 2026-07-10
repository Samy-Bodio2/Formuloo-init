package com.formuloo.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formuloo.core.designsystem.FormulooButton
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary

@Composable
fun InvitationSuccessScreen(
    organisationName: String,
    role: String?,
    onAccessSpace: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(FormulooPrimary)) {
        Spacer(Modifier.height(80.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(FormulooMint, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = FormulooPrimary)
                }
                Spacer(Modifier.height(16.dp))
                Text("Vous y êtes !", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Votre compte a été créé et rattaché à $organisationName. Vous accédez à votre espace avec le rôle attribué.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FormulooOnSurfaceVariant,
                )
                if (role != null) {
                    Spacer(Modifier.height(16.dp))
                    RoleBadge(role)
                }
                Spacer(Modifier.height(24.dp))
                FormulooButton(text = "Accéder à l'espace $organisationName", onClick = onAccessSpace)
            }
        }
    }
}

@Composable
private fun RoleBadge(role: String) {
    Row(
        modifier = Modifier
            .background(FormulooOutline, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("RÔLE  ", fontSize = 11.sp, color = FormulooOnSurfaceVariant)
        Text(role, fontWeight = FontWeight.Bold, color = FormulooTextPrimary)
    }
}
