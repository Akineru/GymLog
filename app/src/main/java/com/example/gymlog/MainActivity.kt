package com.example.gymlog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.data.ActiveWorkoutNotifier
import com.example.gymlog.data.ThemeMode
import com.example.gymlog.ui.GymLogApp
import com.example.gymlog.ui.calendar.LocalCalendarColorPalette
import com.example.gymlog.ui.theme.GymLogTheme

class MainActivity : ComponentActivity() {
    /** 点「正在训练」通知进来时自增，作为一次性跳转信号传给 Compose。 */
    private var openActiveWorkoutSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 只有冷启动才消费 launcher intent；配置变更重建时不要重复触发跳转
        if (savedInstanceState == null) consumeOpenWorkoutIntent(intent)
        setContent {
            val app = application as GymLogApplication
            val appearance by app.appearanceStore.state.collectAsStateWithLifecycle()
            val calendarColors by app.calendarColorStore.state.collectAsStateWithLifecycle()
            val calendarPalette = remember(calendarColors) { calendarColors.palette() }
            val darkTheme = when (appearance.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            GymLogTheme(
                darkTheme = darkTheme,
                dynamicColor = appearance.useWallpaperColors,
            ) {
                CompositionLocalProvider(LocalCalendarColorPalette provides calendarPalette) {
                    RequestNotificationPermissionIfNeeded()
                    GymLogApp(openActiveWorkoutSignal = openActiveWorkoutSignal)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeOpenWorkoutIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // 通知权限可能是在离开 App 期间才被授予的（系统设置 / 权限弹窗）。此时 Compose
        // 状态没变、不会重新投递通知，回到前台补投一次；用户主动划掉过的通知不会被拉回。
        ActiveWorkoutNotifier.refresh(applicationContext)
    }

    /** 消费掉 action，防止 Activity 重建后重复跳转。 */
    private fun consumeOpenWorkoutIntent(intent: Intent?) {
        if (intent?.action == ActiveWorkoutNotifier.ACTION_OPEN_ACTIVE_WORKOUT) {
            openActiveWorkoutSignal += 1
            intent.action = Intent.ACTION_MAIN
        }
    }
}

/**
 * Without POST_NOTIFICATIONS the timer can only buzz while the app is in the
 * foreground, so ask once on launch. The Vibrator fallback in TimerAlerts keeps
 * working if the user says no.
 */
@Composable
private fun RequestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
