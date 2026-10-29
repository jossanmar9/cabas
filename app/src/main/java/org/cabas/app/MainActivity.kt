package org.cabas.app

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cabas.app.domain.AppLocale
import org.cabas.app.domain.ThemeMode
import org.cabas.app.work.AppPrefs
import java.util.Locale

/** Time in the background after which the app locks again, when the lock is on. */
private const val RELOCK_AFTER_MS = 60_000L
private const val LOCK_AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

class MainActivity : FragmentActivity() {
    private val model: CabasViewModel by viewModels()
    private var locked by mutableStateOf(false)
    private var backgroundSince = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLocale.current = Locale.forLanguageTag(getString(R.string.locale_tag))
        AppLocale.monthPattern = getString(R.string.month_pattern)
        AppLocale.dayPattern = getString(R.string.day_pattern)
        enableEdgeToEdge()
        locked = AppPrefs(this).lockEnabled && savedInstanceState?.getBoolean("unlocked") != true
        if (savedInstanceState == null) model.handleIntent(intent)
        setContent {
            val theme by model.themeMode.collectAsStateWithLifecycle()
            val lockEnabled by model.lockEnabled.collectAsStateWithLifecycle()
            val dark = when (theme) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
                // With the lock on, hide the content in the recent apps screen.
                if (lockEnabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            CabasTheme(dark) {
                if (locked) LockScreen { authenticate { locked = false } }
                else CabasApp(model, LockSupport(canLock(), ::authenticate))
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        model.handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) backgroundSince = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        if (backgroundSince > 0 && AppPrefs(this).lockEnabled && SystemClock.elapsedRealtime() - backgroundSince > RELOCK_AFTER_MS) locked = true
        backgroundSince = 0
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("unlocked", !locked)
    }

    private fun canLock() = BiometricManager.from(this).canAuthenticate(LOCK_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    /** Asks for the fingerprint, face or screen lock of the phone. Cabàs never stores a PIN itself. */
    private fun authenticate(onSuccess: () -> Unit) {
        if (!canLock()) { onSuccess(); return }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.lock_prompt_title))
            .setSubtitle(getString(R.string.lock_prompt_subtitle))
            .setAllowedAuthenticators(LOCK_AUTHENTICATORS)
            .build())
    }
}

/** What the settings page needs to turn the lock on: whether the phone has a screen lock, and a way to confirm it. */
internal class LockSupport(val available: Boolean, val authenticate: (onSuccess: () -> Unit) -> Unit)

@Composable
private fun LockScreen(unlock: () -> Unit) {
    var prompted by rememberSaveable { mutableStateOf(false) }
    // Ask once automatically; if the user cancels, the button asks again.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (!prompted) { prompted = true; unlock() } }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.cabas_logo), null, Modifier.size(120.dp))
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = unlock) { Text(stringResource(R.string.unlock)) }
        }
    }
}

/** Pages reachable from the menu, in order. "reflection" is opened from the summary and "search" from the top bar. */
internal val menuPages = listOf("home", "stats", "recurring", "categories", "data", "settings", "help", "privacy", "about")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CabasApp(model: CabasViewModel, lock: LockSupport) {
    var page by rememberSaveable { mutableStateOf("home") }
    var menu by remember { mutableStateOf(false) }
    val error by model.error.collectAsStateWithLifecycle()
    val notice by model.notice.collectAsStateWithLifecycle()
    val addRequested by model.addRequested.collectAsStateWithLifecycle()
    var homeDialogOpen by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    val errorText = error?.text()
    val noticeText = notice?.text()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.generateRecurring(); model.refreshPreferences() }
    fun open(target: String) { model.clearError(); page = target }
    BackHandler(enabled = page != "home") { open("home") }
    LaunchedEffect(addRequested) { if (addRequested) page = "home" }

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
                    if (page != "search") IconButton(onClick = { open("search") }) { Icon(Icons.Default.Search, stringResource(R.string.search)) }
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
            "search" -> SearchContent(model, errorText, modifier)
            "stats" -> StatsContent(model, modifier)
            "recurring" -> RecurringContent(model, errorText, modifier)
            "reflection" -> ReflectionContent(model, errorText, modifier)
            "categories" -> CategoriesContent(model, errorText, modifier)
            "data" -> DataContent(model, errorText, modifier)
            "settings" -> SettingsContent(model, lock, modifier)
            "help", "privacy", "about" -> InfoContent(page, modifier)
            else -> HomeContent(model, errorText, modifier, onDialogChange = { homeDialogOpen = it }, open = ::open)
        }
    }
}
