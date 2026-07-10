package com.formuloo.feature.compta.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.common.UiState
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.feature.compta.domain.model.Invoice
import com.formuloo.feature.compta.presentation.viewmodel.InvoiceCreateViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceCreateScreen(
    onBack: () -> Unit,
    onCreated: (Invoice) -> Unit,
    viewModel: InvoiceCreateViewModel = koinViewModel(),
) {
    var clientNom by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quantite by remember { mutableStateOf("1") }
    var prixUnitaire by remember { mutableStateOf("") }
    var tvaTaux by remember { mutableStateOf("0") }
    var dateEcheance by remember { mutableStateOf("") }

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        val current = state
        if (current is UiState.Success) onCreated(current.data)
    }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle facture", fontWeight = FontWeight.Bold) },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = clientNom,
                onValueChange = { clientNom = it },
                label = { Text("Nom du client") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = clientEmail,
                onValueChange = { clientEmail = it },
                label = { Text("Email du client (optionnel)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description de la prestation") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = quantite,
                onValueChange = { quantite = it },
                label = { Text("Quantité") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = prixUnitaire,
                onValueChange = { prixUnitaire = it },
                label = { Text("Prix unitaire") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = tvaTaux,
                onValueChange = { tvaTaux = it },
                label = { Text("Taux de TVA (%)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = dateEcheance,
                onValueChange = { dateEcheance = it },
                label = { Text("Date d'échéance (AAAA-MM-JJ)") },
                modifier = Modifier.fillMaxWidth(),
            )

            if (state is UiState.Error) {
                Spacer(Modifier.height(12.dp))
                Text((state as UiState.Error).message, color = FormulooError)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.create(
                        clientNom = clientNom,
                        clientEmail = clientEmail.ifBlank { null },
                        description = description,
                        quantite = quantite.toDoubleOrNull() ?: 1.0,
                        prixUnitaire = prixUnitaire.toDoubleOrNull() ?: 0.0,
                        tvaTaux = tvaTaux.toDoubleOrNull() ?: 0.0,
                        dateEcheance = dateEcheance,
                    )
                },
                enabled = state !is UiState.Loading && clientNom.isNotBlank() && description.isNotBlank() && dateEcheance.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), color = Color.White)
                } else {
                    Text("Créer la facture (brouillon)")
                }
            }
        }
    }
}
