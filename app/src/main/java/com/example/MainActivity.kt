package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.CardStudioScreen
import com.example.ui.screens.PresetsAndGuideScreen
import com.example.ui.screens.RadarMapScreen
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.GeoCamoTheme
import com.example.viewmodel.GeoCamoViewModel

private const val ROUTE_RADAR = "radar_map"
private const val ROUTE_CARD = "whatsapp_card_studio"
private const val ROUTE_PRESETS = "presets_and_guide"

private data class NavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoCamoTheme {
                GeoCamoAppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoCamoAppRoot(viewModel: GeoCamoViewModel = viewModel()) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_RADAR

    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshSystemMockStatus()
    }

    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBannerMessage()
        }
    }

    val navItems = remember {
        listOf(
            NavItem(
                route = ROUTE_RADAR,
                labelRes = R.string.nav_radar,
                selectedIcon = Icons.Filled.MyLocation,
                unselectedIcon = Icons.Outlined.MyLocation,
                testTag = "nav_tab_radar"
            ),
            NavItem(
                route = ROUTE_CARD,
                labelRes = R.string.nav_card,
                selectedIcon = Icons.Filled.ChatBubble,
                unselectedIcon = Icons.Outlined.ChatBubbleOutline,
                testTag = "nav_tab_card"
            ),
            NavItem(
                route = ROUTE_PRESETS,
                labelRes = R.string.nav_presets,
                selectedIcon = Icons.Filled.Bookmarks,
                unselectedIcon = Icons.Outlined.Bookmarks,
                testTag = "nav_tab_presets"
            )
        )
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 700.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (sessionState.isCamouflaging) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = EmeraldGlow.copy(alpha = 0.18f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldGlow)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "CAMUFLADO",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldGlow,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_RADAR,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_RADAR) {
                        RadarMapScreen(
                            sessionState = sessionState,
                            uiState = uiState,
                            onSearchQueryChanged = viewModel::onSearchQueryChanged,
                            onClearSearch = viewModel::clearSearch,
                            onSelectPlace = viewModel::selectPlace,
                            onMapCoordinateSelected = viewModel::selectMapCoordinate,
                            onMovementModeSelected = viewModel::setMovementMode,
                            onToggleFavorite = viewModel::toggleFavoriteCurrentPlace,
                            onToggleCamouflage = { hasLocPerm ->
                                viewModel.toggleCamouflage(context, hasLocPerm)
                            },
                            onOpenCardStudio = { navigateToTab(ROUTE_CARD) }
                        )
                    }

                    composable(ROUTE_CARD) {
                        CardStudioScreen(
                            sessionState = sessionState,
                            uiState = uiState,
                            onUpdateConfig = viewModel::updateCardConfig,
                            onSetStyle = viewModel::setCardStyle,
                            onSetTheme = viewModel::setCardTheme,
                            onSetDuration = viewModel::setCardDuration,
                            onShareToWhatsApp = { viewModel.shareCard(context, preferWhatsApp = true) },
                            onShareToSystemSheet = { viewModel.shareCard(context, preferWhatsApp = false) },
                            onShareLinkOnly = { viewModel.shareLinkOnly(context) },
                            onCopyLink = { viewModel.copyMapsLink(context) }
                        )
                    }

                    composable(ROUTE_PRESETS) {
                        PresetsAndGuideScreen(
                            uiState = uiState,
                            onSelectPlaceAndOpenRadar = { place ->
                                viewModel.selectPlace(place)
                                navigateToTab(ROUTE_RADAR)
                            },
                            onRemoveFavorite = viewModel::removeFavoritePlace,
                            onRefreshMockStatus = viewModel::refreshSystemMockStatus
                        )
                    }
                }
            }
        }
    }
}
