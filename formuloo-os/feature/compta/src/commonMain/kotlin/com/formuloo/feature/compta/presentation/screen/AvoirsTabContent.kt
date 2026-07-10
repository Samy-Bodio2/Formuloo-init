package com.formuloo.feature.compta.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.feature.compta.domain.model.Invoice
import com.formuloo.feature.compta.domain.model.InvoiceStatus
import com.formuloo.feature.compta.domain.model.PurchaseInvoice
import com.formuloo.feature.compta.domain.model.PurchaseInvoiceStatus
import com.formuloo.feature.compta.presentation.viewmodel.AvoirsViewModel
import org.koin.compose.viewmodel.koinViewModel

private sealed class AvoirItem {
    data class Client(val avoir: Invoice) : AvoirItem()
    data class Fournisseur(val avoir: PurchaseInvoice) : AvoirItem()
}

private val MOIS_LABELS_FR = listOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre",
)

private fun formatCompact(value: Double): String = when {
    value >= 1_000_000_000 -> "${"%.1f".format(value / 1_000_000_000)} G"
    value >= 1_000_000 -> "${"%.1f".format(value / 1_000_000)} M"
    value >= 1_000 -> "${"%.1f".format(value / 1_000)} k"
    else -> "%.0f".format(value)
}

private fun monthLabelFromDateStr(dateStr: String?): String {
    if (dateStr.isNullOrEmpty() || dateStr.length < 7) return ""
    val parts = dateStr.split("-")
    if (parts.size < 2) return ""
    val month = parts[1].toIntOrNull() ?: return ""
    val year = parts[0]
    return "${MOIS_LABELS_FR.getOrNull(month - 1) ?: ""} $year"
}

private fun formatDetailDate(dateStr: String): String {
    val parts = dateStr.take(10).split("-")
    if (parts.size < 3) return dateStr
    val year = parts[0]
    val month = parts[1].toIntOrNull() ?: return dateStr
    val day = parts[2].trimStart('0').ifEmpty { "0" }
    return "$day ${MOIS_LABELS_FR.getOrNull(month - 1) ?: ""} $year"
}

private fun formatTaux(taux: Double): String =
    "%.2f".format(taux).trimEnd('0').trimEnd('.').replace(".", ",")

@Composable
fun AvoirsTab(
    viewModel: AvoirsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedItem by remember { mutableStateOf<AvoirItem?>(null) }

    val allItems = state.avoirsClients.map { AvoirItem.Client(it) } +
        state.avoirsFournisseurs.map { AvoirItem.Fournisseur(it) }

    val filteredItems: List<AvoirItem> = when (state.selectedTab) {
        1 -> state.avoirsClients.map { AvoirItem.Client(it) }
        2 -> state.avoirsFournisseurs.map { AvoirItem.Fournisseur(it) }
        else -> allItems
    }

    val isLoading = when (state.selectedTab) {
        1 -> state.isLoadingClients
        2 -> state.isLoadingFournisseurs
        else -> state.isLoadingClients || state.isLoadingFournisseurs
    }

    val error = when (state.selectedTab) {
        1 -> state.errorClients
        2 -> state.errorFournisseurs
        else -> state.errorClients ?: state.errorFournisseurs
    }

    val emissionMonthLabel: String = run {
        val dates = state.avoirsClients.mapNotNull { it.dateEmission ?: it.createdAt } +
            state.avoirsFournisseurs.map { it.dateFacture }
        monthLabelFromDateStr(dates.maxOrNull())
    }

    val detail = selectedItem
    if (detail != null) {
        AvoirDetailView(item = detail, onBack = { selectedItem = null })
    } else {
        Column(modifier = Modifier.fillMaxSize().background(FormulooBackground)) {
            AvoirsStatsRow(
                totalClients = state.totalMontantClients,
                totalFournisseurs = state.totalMontantFournisseurs,
                totalCount = allItems.size,
                emissionMonthLabel = emissionMonthLabel,
            )
            AvoirsFilterChips(
                selectedTab = state.selectedTab,
                onSelect = { viewModel.selectTab(it) },
            )
            when {
                isLoading -> AvoirsLoading()
                error != null -> AvoirsError(error) { viewModel.load() }
                filteredItems.isEmpty() -> AvoirsEmptyNew(state.selectedTab)
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredItems, key = {
                        when (it) {
                            is AvoirItem.Client -> "c_${it.avoir.id}"
                            is AvoirItem.Fournisseur -> "f_${it.avoir.id}"
                        }
                    }) { listItem ->
                        AvoirCardNew(item = listItem, onClick = { selectedItem = listItem })
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ── Detail view ──────────────────────────────────────────────────────────────

@Composable
private fun AvoirDetailView(
    item: AvoirItem,
    onBack: () -> Unit,
) {
    val title: String
    val numero: String
    val partieName: String
    val partieLabel: String
    val partieIcon: ImageVector
    val montantTtc: Double
    val montantHt: Double
    val tvaTaux: Double
    val tva: Double
    val devise: String
    val isValidated: Boolean
    val factureOrigineId: Int?
    val date: String
    val motif: String?
    val isFournisseur: Boolean

    when (item) {
        is AvoirItem.Client -> {
            val a = item.avoir
            title = "Avoir client — diminue la créance"
            numero = a.numero
            partieName = a.clientNom
            partieLabel = "CLIENT"
            partieIcon = Icons.Default.Person
            montantTtc = a.montantTtc
            montantHt = a.montantHt
            tvaTaux = a.tvaTaux
            tva = a.tva
            devise = a.devise
            isValidated = a.statut != InvoiceStatus.BROUILLON
            factureOrigineId = a.factureOrigineId
            date = a.dateEmission ?: a.createdAt
            motif = a.lignes.firstOrNull()?.description
            isFournisseur = false
        }
        is AvoirItem.Fournisseur -> {
            val a = item.avoir
            title = "Avoir fournisseur — diminue la dette"
            numero = a.numeroInterne
            partieName = a.fournisseurNom
            partieLabel = "FOURNISSEUR"
            partieIcon = Icons.Default.Person
            montantTtc = a.montantTtc
            montantHt = a.montantHt
            tvaTaux = a.tvaTaux
            tva = a.tva
            devise = a.devise
            isValidated = a.statut != PurchaseInvoiceStatus.BROUILLON
            factureOrigineId = a.factureOrigineId
            date = a.dateFacture
            motif = a.lignes.firstOrNull()?.description
            isFournisseur = true
        }
    }

    val origineRef = factureOrigineId?.let { if (isFournisseur) "AC-$it" else "INV-$it" }

    Column(modifier = Modifier.fillMaxSize().background(FormulooBackground)) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FormulooSurface)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = FormulooPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Comptabilité",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = FormulooTextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "SYSCOHADA · Avoirs",
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = FormulooOnSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(modifier = Modifier.size(48.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Hero banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FormulooError.copy(alpha = 0.08f))
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(FormulooError, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.AssignmentReturn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(title, fontSize = 13.sp, color = FormulooOnSurfaceVariant)
                        Text(
                            "– ${formatAmount(montantTtc)} $devise",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FormulooTextPrimary,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            // Details card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = FormulooSurface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Numéro + statut badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.AssignmentReturn,
                                contentDescription = null,
                                tint = FormulooPrimary,
                                modifier = Modifier.size(20.dp).padding(top = 2.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("NUMÉRO D'AVOIR", fontSize = 10.sp, color = FormulooOnSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                Text(numero, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = FormulooTextPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(
                                            if (isValidated) Color(0xFF2E7D32) else FormulooOnSurfaceVariant,
                                            CircleShape,
                                        ),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (isValidated) "Validé" else "Brouillon",
                                    fontSize = 12.sp,
                                    color = if (isValidated) Color(0xFF2E7D32) else FormulooOnSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = FormulooOutline.copy(alpha = 0.4f))
                        // Client / Fournisseur
                        AvoirDetailRow(icon = partieIcon, label = partieLabel, value = partieName)
                        // Pièce d'origine
                        if (origineRef != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = FormulooOutline.copy(alpha = 0.4f))
                            AvoirDetailRow(icon = Icons.Default.Description, label = "PIÈCE D'ORIGINE", value = origineRef)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = FormulooOutline.copy(alpha = 0.4f))
                        // Date
                        AvoirDetailRow(icon = Icons.Default.CalendarToday, label = "DATE", value = formatDetailDate(date))
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            // Montants card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = FormulooSurface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        AvoirAmountRow("Montant HT", formatAmount(montantHt), isTotal = false)
                        Spacer(Modifier.height(10.dp))
                        AvoirAmountRow("TVA ${formatTaux(tvaTaux)} %", formatAmount(tva), isTotal = false)
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = FormulooOutline.copy(alpha = 0.4f))
                        Spacer(Modifier.height(10.dp))
                        AvoirAmountRow("Total avoir TTC", "${formatAmount(montantTtc)} $devise", isTotal = true)
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            // Motif / info légale
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(FormulooMint, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = FormulooPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    val motifText = buildString {
                        if (!motif.isNullOrEmpty()) {
                            append("Motif : ")
                            append(motif)
                            append(". ")
                        }
                        append("L'avoir génère une écriture inverse de la facture d'origine et met à jour le solde du compte tiers.")
                    }
                    Text(motifText, fontSize = 12.sp, color = FormulooPrimary, lineHeight = 18.sp)
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun AvoirDetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = FormulooPrimary, modifier = Modifier.size(20.dp).padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 10.sp, color = FormulooOnSurfaceVariant, fontWeight = FontWeight.SemiBold)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = FormulooTextPrimary)
        }
    }
}

@Composable
private fun AvoirAmountRow(label: String, value: String, isTotal: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = if (isTotal) 14.sp else 13.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) FormulooTextPrimary else FormulooOnSurfaceVariant,
        )
        Text(
            value,
            fontSize = if (isTotal) 15.sp else 13.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) FormulooPrimary else FormulooTextPrimary,
        )
    }
}

// ── List view components ─────────────────────────────────────────────────────

@Composable
private fun AvoirsStatsRow(
    totalClients: Double,
    totalFournisseurs: Double,
    totalCount: Int,
    emissionMonthLabel: String,
) {
    val blue = Color(0xFF1565C0)
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            AvoirStatCard(
                title = "Avoirs clients",
                badgeColor = FormulooError,
                badgeLabel = "AV",
                value = "${formatCompact(totalClients)} F",
                subtitle = "à déduire des ventes",
            )
        }
        item {
            AvoirStatCard(
                title = "Avoirs fournisseurs",
                badgeColor = FormulooPrimary,
                badgeLabel = "AVF",
                value = "${formatCompact(totalFournisseurs)} F",
                subtitle = "à déduire des achats",
            )
        }
        item {
            AvoirStatCard(
                title = "Avoirs émis",
                badgeColor = blue,
                badgeLabel = "TOTAL",
                value = "$totalCount",
                subtitle = emissionMonthLabel,
            )
        }
    }
}

@Composable
private fun AvoirStatCard(
    title: String,
    badgeColor: Color,
    badgeLabel: String,
    value: String,
    subtitle: String,
) {
    Card(
        modifier = Modifier.width(158.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(badgeLabel, color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(6.dp))
                Text(title, fontSize = 11.sp, color = FormulooOnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = FormulooTextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = FormulooOnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AvoirsFilterChips(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("Tous", "Clients", "Fournisseurs").forEachIndexed { index, label ->
            val selected = selectedTab == index
            Box(
                modifier = Modifier
                    .background(if (selected) FormulooPrimary else Color.Transparent, RoundedCornerShape(20.dp))
                    .border(1.dp, if (selected) FormulooPrimary else FormulooOutline, RoundedCornerShape(20.dp))
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
            ) {
                Text(
                    label,
                    color = if (selected) Color.White else FormulooOnSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun AvoirCardNew(item: AvoirItem, onClick: () -> Unit) {
    val badgeColor: Color
    val badgeText: String
    val reference: String
    val libelle: String
    val montant: Double
    val devise: String
    val isValidated: Boolean
    val factureOrigineId: Int?
    val isFournisseur: Boolean

    when (item) {
        is AvoirItem.Client -> {
            val a = item.avoir
            badgeColor = FormulooError
            badgeText = "AV"
            reference = a.numero
            libelle = a.clientNom
            montant = a.montantTtc
            devise = a.devise
            isValidated = a.statut != InvoiceStatus.BROUILLON
            factureOrigineId = a.factureOrigineId
            isFournisseur = false
        }
        is AvoirItem.Fournisseur -> {
            val a = item.avoir
            badgeColor = FormulooPrimary
            badgeText = "AVF"
            reference = a.numeroInterne
            libelle = a.fournisseurNom
            montant = a.montantTtc
            devise = a.devise
            isValidated = a.statut != PurchaseInvoiceStatus.BROUILLON
            factureOrigineId = a.factureOrigineId
            isFournisseur = true
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(badgeText, color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(reference, color = FormulooPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        libelle,
                        color = FormulooOnSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("–${formatCompact(montant)} $devise", color = FormulooError, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = FormulooOnSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvoirStatutBadge(isValidated)
                    if (factureOrigineId != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${if (isFournisseur) "sur AC-" else "sur INV-"}$factureOrigineId",
                            fontSize = 11.sp,
                            color = FormulooOnSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvoirStatutBadge(isValidated: Boolean) {
    val text = if (isValidated) "Validé" else "Brouillon"
    val bg = if (isValidated) Color(0xFFE8F5E9) else FormulooOutline.copy(alpha = 0.15f)
    val fg = if (isValidated) Color(0xFF2E7D32) else FormulooOnSurfaceVariant
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text, fontSize = 10.sp, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AvoirsLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = FormulooPrimary)
    }
}

@Composable
private fun AvoirsError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.AssignmentReturn, null, tint = FormulooError, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, color = FormulooError, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRetry) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Réessayer")
        }
    }
}

@Composable
private fun AvoirsEmptyNew(selectedTab: Int) {
    val label = when (selectedTab) {
        1 -> "Aucun avoir client"
        2 -> "Aucun avoir fournisseur"
        else -> "Aucun avoir"
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(FormulooMint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.AssignmentReturn, contentDescription = null, tint = FormulooPrimary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FormulooTextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Les avoirs émis apparaîtront ici.", fontSize = 13.sp, color = FormulooOnSurfaceVariant, textAlign = TextAlign.Center)
    }
}
