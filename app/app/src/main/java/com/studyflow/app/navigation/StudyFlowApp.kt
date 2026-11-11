package com.studyflow.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.studyflow.app.core.i18n.LocalStrings
import com.studyflow.app.core.session.AuthUser
import com.studyflow.app.core.ui.components.InitialAvatar
import com.studyflow.app.core.ui.components.SettingsActions
import com.studyflow.app.core.ui.theme.StudyFlowTheme
import com.studyflow.app.feature.auth.AuthScreen
import com.studyflow.app.feature.auth.BrandMark
import com.studyflow.app.feature.auth.SessionViewModel
import com.studyflow.app.feature.dashboard.DashboardScreen
import com.studyflow.app.feature.kanban.KanbanScreen
import com.studyflow.app.feature.notes.NotesScreen
import com.studyflow.app.feature.pomodoro.PomodoroScreen
import com.studyflow.app.feature.pomodoro.PomodoroWidget
import com.studyflow.app.feature.profile.ProfileScreen
import com.studyflow.app.feature.projects.ProjectDetailScreen
import com.studyflow.app.feature.projects.ProjectsScreen
import com.studyflow.app.feature.review.ReviewScreen
import com.studyflow.app.feature.tasks.TasksScreen
import com.studyflow.app.ui.appViewModel
import kotlinx.coroutines.launch

private data class Destination(
    val route: String,
    val icon: ImageVector,
    val labelKey: String,
)

private val drawerDestinations = listOf(
    Destination(Routes.DASHBOARD, Icons.Filled.Home, "dashboard"),
    Destination(Routes.POMODORO, Icons.Filled.Timer, "pomodoro"),
    Destination(Routes.NOTES, Icons.Filled.Description, "notes"),
    Destination(Routes.TASKS, Icons.Filled.Checklist, "tasks"),
    Destination(Routes.KANBAN, Icons.Filled.ViewKanban, "kanban"),
    Destination(Routes.PROJECTS, Icons.Filled.School, "projects"),
    Destination(Routes.REVIEW, Icons.Filled.FindInPage, "review"),
    Destination(Routes.PROFILE, Icons.Filled.Person, "profile"),
)

private val bottomDestinations = drawerDestinations.filter {
    it.route in setOf(Routes.DASHBOARD, Routes.POMODORO, Routes.NOTES, Routes.TASKS, Routes.PROJECTS)
}

@Composable
fun StudyFlowApp() {
    val sessionVm: SessionViewModel = appViewModel { SessionViewModel(it.authRepository) }
    val session by sessionVm.session.collectAsStateWithLifecycle()
    val user = session?.user

    if (user == null) {
        AuthScreen()
    } else {
        MainScaffold(user = user, onLogout = sessionVm::logout)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(user: AuthUser, onLogout: () -> Unit) {
    val s = LocalStrings.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isProjectDetail = route == Routes.PROJECT_DETAIL

    val go: (String) -> Unit = { destination ->
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val title = when (route) {
        Routes.POMODORO -> s["timerTitle"]
        Routes.NOTES -> s["notesTitle"]
        Routes.TASKS -> s["tasks"]
        Routes.KANBAN -> s["kanban"]
        Routes.PROJECTS, Routes.PROJECT_DETAIL -> s["projectsTitle"]
        Routes.PROFILE -> s["profileTitle"]
        Routes.REVIEW -> s["reviewTitle"]
        else -> s["dashboard"]
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            StudyFlowDrawer(
                user = user,
                currentRoute = route,
                onNavigate = { destination ->
                    scope.launch { drawerState.close() }
                    go(destination)
                },
                onLogout = onLogout,
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        if (isProjectDetail) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s["goBack"])
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = s["menu"])
                            }
                        }
                    },
                    actions = {
                        SettingsActions(
                            profileName = user.name,
                            onProfile = { go(Routes.PROFILE) },
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
            bottomBar = {
                if (!isProjectDetail && bottomDestinations.any { it.route == route }) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
                        bottomDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = route == destination.route,
                                onClick = { go(destination.route) },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(s[destination.labelKey], style = MaterialTheme.typography.labelSmall) },
                                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = StudyFlowTheme.colors.primarySoft,
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.DASHBOARD,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable(Routes.DASHBOARD) { DashboardScreen(onNavigate = go) }
                    composable(Routes.POMODORO) { PomodoroScreen() }
                    composable(Routes.NOTES) { NotesScreen() }
                    composable(Routes.TASKS) { TasksScreen() }
                    composable(Routes.KANBAN) { KanbanScreen() }
                    composable(Routes.PROJECTS) {
                        ProjectsScreen(onOpenProject = { id -> navController.navigate(Routes.projectDetail(id)) })
                    }
                    composable(
                        route = Routes.PROJECT_DETAIL,
                        arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
                    ) { entry ->
                        ProjectDetailScreen(
                            projectId = entry.arguments?.getString("projectId").orEmpty(),
                            onBack = { navController.popBackStack() },
                            onManageTasks = { go(Routes.TASKS) },
                        )
                    }
                    composable(Routes.PROFILE) { ProfileScreen() }
                    composable(Routes.REVIEW) { ReviewScreen() }
                }
                if (route != Routes.POMODORO) {
                    PomodoroWidget(
                        onOpen = { go(Routes.POMODORO) },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, end = 76.dp, bottom = 16.dp)
                            .widthIn(max = 320.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyFlowDrawer(
    user: AuthUser,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
) {
    val s = LocalStrings.current
    val colors = StudyFlowTheme.colors

    ModalDrawerSheet(
        drawerContainerColor = colors.sidebar,
        drawerContentColor = Color.White,
        modifier = Modifier.width(300.dp),
    ) {
        Column(Modifier.fillMaxHeight().padding(20.dp)) {
            BrandMark(dark = true)
            Spacer(Modifier.height(28.dp))
            drawerDestinations.forEach { destination ->
                val disabled = destination.route == Routes.REVIEW
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(
                            if (currentRoute == destination.route) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(8.dp),
                        )
                        .clickable(enabled = !disabled) { onNavigate(destination.route) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        destination.icon,
                        contentDescription = null,
                        tint = if (currentRoute == destination.route) Color.White else colors.sidebarText,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        s[destination.labelKey],
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (currentRoute == destination.route) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (disabled) colors.sidebarText.copy(alpha = 0.55f)
                        else if (currentRoute == destination.route) Color.White else colors.sidebarText,
                        modifier = Modifier.weight(1f),
                    )
                    if (disabled) {
                        Text(
                            s["comingSoon"],
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.sidebarText.copy(alpha = 0.7f),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider(color = Color.White.copy(alpha = 0.13f))
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InitialAvatar(name = user.name, size = 38.dp)
                Column {
                    Text(
                        user.name.ifBlank { s["studentFallback"] },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text(
                        user.course ?: "—",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.sidebarText,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = colors.sidebarText),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(s["logoutAction"], style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
