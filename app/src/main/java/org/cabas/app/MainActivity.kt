package org.cabas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.cabas.app.domain.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: CabasViewModel = viewModel()
            val theme by model.themeMode.collectAsStateWithLifecycle()
            val dark = when (theme) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            CabasTheme(dark) { CabasApp(model) }
        }
    }
}

/** Pages reachable from the menu, in order. "reflection" is opened from the monthly summary. */
internal val menuPages = listOf("home", "recurring", "categories", "data", "appearance", "help", "privacy", "about")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CabasApp(model: CabasViewModel) {
    var page by rememberSaveable { mutableStateOf("home") }
    var menu by remember { mutableStateOf(false) }
    val error by model.error.collectAsStateWithLifecycle()
    val notice by model.notice.collectAsStateWithLifecycle()
    val theme by model.themeMode.collectAsStateWithLifecycle()
    var homeDialogOpen by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    val errorText = error?.text()
    val noticeText = notice?.text()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.generateRecurring() }
    fun open(target: String) { model.clearError(); page = target }
    BackHandler(enabled = page != "home") { open("home") }

    // Pages other than the summary show their errors next to the form that caused them.
    LaunchedEffect(errorText, page, homeDialogOpen) {
        if (errorText != null && page == "home" && !homeDialogOpen) { snack.showSnackbar(errorText); model.clearError() }
    }
    LaunchedEffect(noticeText) {
        if (noticeText != null) { snack.showSnackbar(noticeText); model.clearNotice() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (page != "home") IconButton(onClick = { open("home") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                title = {
                    if (page == "home") Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Image(painterResource(R.drawable.cabas_logo), null, Modifier.size(36.dp))
                        Text(stringResource(R.string.app_name))
                    } else Text(stringResource(pageTitle(page)))
                },
                actions = {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.Menu, stringResource(R.string.menu)) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            menuPages.forEach { target ->
                                DropdownMenuItem(text = { Text(stringResource(pageTitle(target))) }, onClick = { menu = false; open(target) })
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (page) {
            "recurring" -> RecurringContent(model, errorText, modifier)
            "reflection" -> ReflectionContent(model, errorText, modifier)
            "categories" -> CategoriesContent(model, errorText, modifier)
            "data" -> DataContent(model, errorText, modifier)
            "appearance" -> AppearanceContent(theme, model::setTheme, modifier)
            "help", "privacy", "about" -> InfoContent(page, modifier)
            else -> HomeContent(model, errorText, modifier, onDialogChange = { homeDialogOpen = it }, open = ::open)
        }
    }
}
