 (cd "$(git rev-parse --show-toplevel)" && printf '%s' 'diff --git a/app/src/main/AndroidManifest.xml b/app/src/main/AndroidManifest.xml
index 34695b1f9f234311ef79f56ba5433151c67dc2e6..8fe5470c313ec7d810ac89d0a87b241c78e9a623 100644
--- a/app/src/main/AndroidManifest.xml
+++ b/app/src/main/AndroidManifest.xml
@@ -1,33 +1,33 @@
 <manifest xmlns:android="http://schemas.android.com/apk/res/android">
     <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
     <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
     <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
 
     <application
         android:allowBackup="true"
         android:label="Glass Launcher"
-        android:theme="@style/Theme.GlassLauncher">
+        android:theme="@android:style/Theme.Material.NoActionBar">
         <activity
             android:name=".MainActivity"
             android:exported="true">
             <intent-filter>
                 <action android:name="android.intent.action.MAIN" />
                 <category android:name="android.intent.category.LAUNCHER" />
             </intent-filter>
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
                 android:value="User-requested launcher (cd "$(git rev-parse --show-toplevel)" && printf '%s' 'diff --git a/app/src/main/java/com/sidzadaun/liquidglassview/MainActivity.kt b/app/src/main/java/com/sidzadaun/liquidglassview/MainActivity.kt
index eda134ea6ac24c671a81a8182b338477836ce402..277314d9d721e41d2d214dadb418498d9567e54e 100644
--- a/app/src/main/java/com/sidzadaun/liquidglassview/MainActivity.kt
+++ b/app/src/main/java/com/sidzadaun/liquidglassview/MainActivity.kt
@@ -1,34 +1,38 @@
 package com.sidzadaun.liquidglassview
 
 import android.app.role.RoleManager
+import android.app.NotificationChannel
+import android.app.NotificationManager
+import android.app.Service
 import android.content.Context
 import android.content.Intent
 import android.content.pm.PackageManager
 import android.net.Uri
 import android.os.Build
 import android.os.Bundle
+import android.os.IBinder
 import android.provider.Settings
 import androidx.activity.ComponentActivity
 import androidx.activity.compose.setContent
 import androidx.compose.animation.AnimatedVisibility
 import androidx.compose.foundation.Image
 import androidx.compose.foundation.background
 import androidx.compose.foundation.border
 import androidx.compose.foundation.clickable
 import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
 import androidx.compose.foundation.layout.weight
 import androidx.compose.foundation.lazy.grid.GridCells
 import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
 import androidx.compose.foundation.lazy.grid.items
@@ -48,50 +52,51 @@ import androidx.compose.material3.Icon
 import androidx.compose.material3.MaterialTheme
 import androidx.compose.material3.OutlinedTextField
 import androidx.compose.material3.OutlinedTextFieldDefaults
 import androidx.compose.material3.Text
 import androidx.compose.material3.darkColorScheme
 import androidx.compose.runtime.Composable
 import androidx.compose.runtime.LaunchedEffect
 import androidx.compose.runtime.getValue
 import androidx.compose.runtime.mutableStateOf
 import androidx.compose.runtime.remember
 import androidx.compose.runtime.setValue
 import androidx.compose.ui.Alignment
 import androidx.compose.ui.Modifier
 import androidx.compose.ui.draw.clip
 import androidx.compose.ui.graphics.Brush
 import androidx.compose.ui.graphics.Color
 import androidx.compose.ui.graphics.asImageBitmap
 import androidx.compose.ui.graphics.vector.ImageVector
 import androidx.compose.ui.input.pointer.pointerInput
 import androidx.compose.ui.platform.LocalContext
 import androidx.compose.ui.text.font.FontWeight
 import androidx.compose.ui.text.style.TextOverflow
 import androidx.compose.ui.unit.dp
 import androidx.compose.ui.unit.sp
 import androidx.core.content.ContextCompat
+import androidx.core.app.NotificationCompat
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
         setContent { GlassLauncherTheme { GlassLauncherScreen() } }
         requestHomeRole()
     }
 
     private fun requestHomeRole() {
         if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
 
         val roleManager = getSystemService(RoleManager::class.java) ?: return
         if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
             !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
         ) {
@@ -306,25 +311,63 @@ private fun launchApp(context: Context, packageName: String) {
     context.packageManager.getLaunchIntentForPackage(packageName)?.let {
         context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
     }
 }
 
 private fun openSettings(context: Context) = context.startActivity(Intent(Settings.ACTION_SETTINGS))
 
 private fun startOverlayService(context: Context) {
     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
         context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
     } else {
         ContextCompat.startForegroundService(context, Intent(context, GlassOverlayService::class.java))
     }
 }
 
 @Composable
 private fun GlassLauncherTheme(content: @Composable () -> Unit) {
     MaterialTheme(
         colorScheme = darkColorScheme(
             background = Bg, surface = Bg, primary = Color.White, onPrimary = Color.Black,
             onBackground = TextPrimary, onSurface = TextPrimary
         ),
         content = content
     )
 }
+
+private data class AppInfo(
+    val packageName: String,
+    val label: String,
+    val icon: android.graphics.drawable.Drawable
+)
+
+/** Foreground service started after the user grants overlay permission. */
+class GlassOverlayService : Service() {
+    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
+        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
+            val channel = NotificationChannel(
+                CHANNEL_ID,
+                "Glass overlay",
+                NotificationManager.IMPORTANCE_LOW
+            )
+            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
+        }
+
+        startForeground(
+            NOTIFICATION_ID,
+            NotificationCompat.Builder(this, CHANNEL_ID)
+                .setSmallIcon(android.R.drawable.ic_menu_view)
+                .setContentTitle("Glass overlay is active")
+                .setContentText("The launcher overlay service is running.")
+                .setOngoing(true)
+                .build()
+        )
+        return START_NOT_STICKY
+    }
+
+    override fun onBind(intent: Intent?): IBinder? = null
+
+    private companion object {
+        const val CHANNEL_ID = "glass_overlay"
+        const val NOTIFICATION_ID = 1001
+    }
+}
' | git apply --3way) overlay" />
         </service>
     </application>
 </manifest>
 
' | git apply --3way)
