package com.formuloo.feature.hr.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.common.UiState
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSecondaryBg
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.feature.hr.domain.model.OrgNode
import com.formuloo.feature.hr.presentation.viewmodel.OrgChartViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgChartScreen(
    onBack: () -> Unit,
    viewModel: OrgChartViewModel = koinViewModel(),
) {
    val state by viewModel.treeState.collectAsStateWithLifecycle()
    val expanded by viewModel.expandedNodeIds.collectAsStateWithLifecycle()

    var scale by remember { mutableFloatStateOf(1f) }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = { Text("Organigramme", fontWeight = FontWeight.Bold) },
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
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 2.5f)
                    }
                },
        ) {
            when (state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
                is UiState.Empty -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Aucun département.", color = FormulooOnSurfaceVariant)
                }
                is UiState.Success -> {
                    val roots = (state as UiState.Success<List<OrgNode>>).data
                    val flat = remember(roots, expanded) { flattenVisible(roots, expanded) }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .graphicsLayer(scaleX = scale, scaleY = scale),
                    ) {
                        items(flat, key = { it.id }) { node ->
                            OrgNodeRow(
                                node = node,
                                isExpanded = node.id in expanded,
                                isRoot = node.depth == 0,
                                onToggle = { viewModel.toggleNode(node.id) },
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
}

/** Parcours en profondeur, ne descend dans les enfants que si le nœud est dans [expanded]. */
private fun flattenVisible(roots: List<OrgNode>, expanded: Set<String>): List<OrgNode> {
    val result = mutableListOf<OrgNode>()
    fun visit(node: OrgNode) {
        result += node
        if (node.id in expanded) {
            node.children.forEach { visit(it) }
        }
    }
    roots.forEach { visit(it) }
    return result
}

@Composable
private fun OrgNodeRow(node: OrgNode, isExpanded: Boolean, isRoot: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = (node.depth * 20).dp, top = 4.dp, bottom = 4.dp)
            .background(
                if (isRoot) FormulooMint else FormulooSurface,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (node.children.isNotEmpty()) {
            IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = FormulooPrimary,
                )
            }
        } else {
            Box(modifier = Modifier.size(28.dp))
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(node.nom, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = FormulooTextPrimary)
            Box(
                modifier = Modifier
                    .background(FormulooSecondaryBg, shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text("${node.nbEmployes} employés", fontSize = 11.sp, color = FormulooOnSurfaceVariant)
            }
        }
    }
}
