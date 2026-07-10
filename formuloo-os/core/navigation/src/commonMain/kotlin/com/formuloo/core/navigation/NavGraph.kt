package com.formuloo.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun rememberAppNavController(): NavHostController = rememberNavController()

/**
 * Wrapper fin autour de [NavHost]. Le graphe de destinations (ecrans des
 * modules feature) est fourni par composeApp via [builder], pour eviter
 * une dependance de core:navigation vers les modules feature.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Route,
    modifier: Modifier = Modifier,
    builder: NavGraphBuilder.() -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        builder = builder,
    )
}
