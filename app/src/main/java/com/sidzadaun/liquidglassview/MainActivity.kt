from pathlib import Path
import zipfile, textwrap, os

root = Path("/mnt/data/GlassLauncherV2")
files = {
"settings.gradle.kts": r'''
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "GlassLauncher"
include(":app")
''',

"build.gradle.kts": r'''
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
''',

"gradle.properties": r'''
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
''',

"app/build.gradle.kts": r'''
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.sidzadaun.liquidglassview"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sidzadaun.liquidglassview"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
''',

"app/src/main/AndroidManifest.xml": r'''
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent>
    </queries>

    <application
        android:allowBackup="true"
        android:label="Glass Launcher"
        android:supportsRtl="true"
        android:theme="@style/Theme.GlassLauncher">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="unspecified">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>

        <service
            android:name=".GlassOverlayService"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="Persistent launcher quick panel overlay" />
        </service>
    </application>
</manifest>
''',

"app/src/main/res/values/styles.xml": r'''
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.GlassLauncher" parent="android:style/Theme.Material.Light.NoActionBar">
        <item name="android:fontFamily">sans</item>
        <item name="android:windowLightStatusBar">false</item>
        <item name="android:statusBarColor">#080B12</item>
        <item name="android:navigationBarColor">#080B12</item>
        <item name="android:windowActionModeOverlay">true</item>
    </style>
</resources>
''',

"app/src/main/java/com/sidzadaun/liquidglassview/AppInfo.kt": r'''
package com.sidzadaun.liquidglassview

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable
)
''',

"app/src/main/java/com/sidzadaun/liquidglassview/MainActivity.kt": r'''
package com.sidzadaun.liquidglassview

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Bg = Color(0xFF080B12)
private val Glass = Color.White.copy(alpha = 0.105f)
private val GlassStrong = Color.White.copy(alpha = 0.17f)
private val Stroke = Color.White.copy(alpha = 0.20f)
private val TextPrimary = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.64f)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GlassLauncherTheme {
                GlassLauncherScreen()
            }
        }

        requestHomeRole()
    }

    private fun requestHomeRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                startActivityForResult(
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME),
                    900
                )
            }
        }
    }
}

@Composable
private fun GlassLauncherScreen() {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var drawer by remember { mutableStateOf(false) }
    var sidebar by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            loadApps(context)
        }
    }

    val filtered = remember(apps, search) {
        if (search.isBlank()) apps
        else apps.filter { it.label.contains(search, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF111827), Bg, Color(0xFF05070B))
                )
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, drag ->
                        if (drag < -45) sidebar = true
                        if (drag > 45) sidebar = false
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 26.dp, start = 18.dp, end = 18.dp, bottom = 12.dp)
        ) {
            Header(
                onSearch = { drawer = true },
                onSettings = {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            )

            Spacer(Modifier.height(22.dp))

            if (!drawer) {
                HomeContent(
                    apps = apps.take(12),
                    onApp = { launchApp(context, it.packageName) }
                )
            } else {
                AppDrawer(
                    apps = filtered,
                    search = search,
                    onSearchChange = { search = it },
                    onApp = { launchApp(context, it.packageName) },
                    onClose = {
                        drawer = false
                        search = ""
                    }
                )
            }

            Spacer(Modifier.weight(1f))

            GlassDock(
                apps = apps.take(5),
                onApp = { launchApp(context, it.packageName) },
                onDrawer = { drawer = true },
                onSidebar = { sidebar = !sidebar }
            )
        }

        AnimatedVisibility(
            visible = sidebar,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Sidebar(
                onClose = { sidebar = false },
                onSettings = {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                },
                onWifi = {
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                },
                onBluetooth = {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                },
                onBattery = {
                    context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
                },
                onOverlay = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        !Settings.canDrawOverlays(context)
                    ) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    } else {
                        ContextCompat.startForegroundService(
                            context,
                            Intent(context, GlassOverlayService::class.java)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun Header(onSearch: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "GLASS",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 5.sp
            )
            Text(
                "launcher",
                color = TextSecondary,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )
        }

        GlassCircleButton(Icons.Default.Search, onSearch)
        Spacer(Modifier.width(8.dp))
        GlassCircleButton(Icons.Default.Settings, onSettings)
    }
}

@Composable
private fun HomeContent(apps: List<AppInfo>, onApp: (AppInfo) -> Unit) {
    Column {
        Text(
            "YOUR APPS",
            color = TextSecondary,
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = false,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(apps) { app ->
                GlassAppTile(app, onApp)
            }
        }
    }
}

@Composable
private fun AppDrawer(
    apps: List<AppInfo>,
    search: String,
    onSearchChange: (String) -> Unit,
    onApp: (AppInfo) -> Unit,
    onClose: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = search,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Search apps", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Glass,
                    unfocusedContainerColor = Glass,
                    focusedBorderColor = Stroke,
                    unfocusedBorderColor = Stroke,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            Spacer(Modifier.width(8.dp))
            GlassCircleButton(Icons.Default.Close, onClose)
        }

        Spacer(Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(apps) { app ->
                GlassAppTile(app, onApp)
            }
        }
    }
}

@Composable
private fun GlassAppTile(app: AppInfo, onApp: (AppInfo) -> Unit) {
    val bitmap = remember(app.packageName) {
        app.icon.toBitmap(96, 96).asImageBitmap()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onApp(app) }
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = .19f),
                            Color.White.copy(alpha = .055f)
                        )
                    )
                )
                .border(1.dp, Stroke, RoundedCornerShape(19.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = app.label,
                modifier = Modifier.size(43.dp)
            )
        }

        Spacer(Modifier.height(5.dp))

        Text(
            app.label,
            color = TextPrimary.copy(alpha = .88f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GlassDock(
    apps: List<AppInfo>,
    onApp: (AppInfo) -> Unit,
    onDrawer: () -> Unit,
    onSidebar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(27.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = .18f),
                            Color.White.copy(alpha = .075f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = .25f), RoundedCornerShape(27.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            apps.forEach { app ->
                val bitmap = remember(app.packageName) {
                    app.icon.toBitmap(72, 72).asImageBitmap()
                }
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .clickable { onApp(app) }
                )
            }

            Icon(
                Icons.Default.Apps,
                contentDescription = "Apps",
                tint = TextPrimary,
                modifier = Modifier
                    .size(45.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(GlassStrong)
                    .clickable { onDrawer() }
                    .padding(10.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        GlassCircleButton(Icons.Default.DarkMode, onSidebar)
    }
}

@Composable
private fun Sidebar(
    onClose: () -> Unit,
    onSettings: () -> Unit,
    onWifi: () -> Unit,
    onBluetooth: () -> Unit,
    onBattery: () -> Unit,
    onOverlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(end = 12.dp)
            .width(255.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = .22f),
                        Color.White.copy(alpha = .09f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = .28f), RoundedCornerShape(30.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "QUICK GLASS",
                color = TextPrimary,
                fontSize = 14.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = TextPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onClose() }
                    .padding(6.dp)
            )
        }

        Spacer(Modifier.height(18.dp))

        QuickAction(Icons.Default.Wifi, "Wi-Fi", onWifi)
        QuickAction(Icons.Default.Bluetooth, "Bluetooth", onBluetooth)
        QuickAction(Icons.Default.BatteryFull, "Battery", onBattery)
        QuickAction(Icons.Default.FlashlightOn, "Overlay", onOverlay)
        QuickAction(Icons.Default.Settings, "Settings", onSettings)
    }
}

@Composable
private fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = .07f))
            .clickable { onClick() }
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextPrimary)
        Spacer(Modifier.width(13.dp))
        Text(label, color = TextPrimary, fontSize = 14.sp)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun GlassCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Glass)
            .border(1.dp, Stroke, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
    }
}

private fun loadApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        .asSequence()
        .filter { it.activityInfo.packageName != context.packageName }
        .map {
            AppInfo(
                packageName = it.activityInfo.packageName,
                label = it.loadLabel(pm).toString(),
                icon = it.loadIcon(pm)
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
        .toList()
}

private fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent != null) context.startActivity(intent)
}

@Composable
private fun GlassLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Bg,
            surface = Bg,
            primary = Color.White,
            onPrimary = Color.Black,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        ),
        content = content
    )
}
''',

"app/src/main/java/com/sidzadaun/liquidglassview/GlassOverlayService.kt": r'''
package com.sidzadaun.liquidglassview

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class GlassOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var pill: View? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                2001,
                buildNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(2001, buildNotification())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            Settings.canDrawOverlays(this)
        ) {
            showPill()
        }
    }

    private fun showPill() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val text = TextView(this).apply {
            text = "≡"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setBackgroundColor(0x442FFFFFF)
            setOnClickListener {
                stopSelf()
            }
        }

        val type =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            52,
            52,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = 8
        }

        pill = text
        windowManager?.addView(text, params)
    }

    override fun onDestroy() {
        pill?.let { runCatching { windowManager?.removeView(it) } }
        pill = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "glass_launcher_overlay",
                "Glass Launcher",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "glass_launcher_overlay")
                .setContentTitle("Glass Launcher")
                .setContentText("Glass sidebar is active")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Glass Launcher")
                .setContentText("Glass sidebar is active")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build()
        }
    }
}
''',

"README.md": r'''
# Glass Launcher V2

A lightweight glassmorphism Android launcher designed for older Samsung tablets such as the Galaxy Tab A SM-T295.

## Features

- Android HOME launcher
- Real installed application icons
- App drawer
- App search
- Glass app tiles
- Glass dock
- Quick sidebar
- Wi-Fi/Bluetooth/Battery/Settings shortcuts
- Optional overlay pill
- No root required
- Lightweight visual effects designed for 2 GB RAM devices

## Build

Open this repository in Android Studio and run:

```bash
./gradlew assembleDebug
