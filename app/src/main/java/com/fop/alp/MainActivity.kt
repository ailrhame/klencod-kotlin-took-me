package com.fop.alp

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

// ============================================================
// MainActivity — واجهة الإعدادات والصلاحيات
// ============================================================
class MainActivity : Activity() {

    private val colorBg = 0xFF0D1117.toInt()
    private val colorSurface = 0xFF161B22.toInt()
    private val colorSurfaceAlt = 0xFF21262D.toInt()
    private val colorPrimary = 0xFF1F6FEB.toInt()
    private val colorPrimaryLight = 0xFF58A6FF.toInt()
    private val colorText = 0xFFE6EDF3.toInt()
    private val colorMuted = 0xFF8B949E.toInt()
    private val colorBorder = 0xFF30363D.toInt()
    private val colorSuccess = 0xFF3FB950.toInt()
    private val colorDanger = 0xFFF85149.toInt()
    private val colorGreen = 0xFF238636.toInt()
    private val colorGreenLight = 0xFF56D364.toInt()

    private lateinit var etMessage: EditText
    private lateinit var etSeconds: EditText
    private lateinit var tvAdminStatus: TextView
    private lateinit var tvOverlayStatus: TextView
    private lateinit var tvServiceStatus: TextView

    private val prefs by lazy {
        getSharedPreferences("fakelock", Context.MODE_PRIVATE)
    }

    private fun dp(v: Int): Int = Math.round(v * resources.displayMetrics.density)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = colorBg
        window.navigationBarColor = colorBg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorBg)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, -1))

        // ---------- Header ----------
        val title = TextView(this).apply {
            text = "🔒 FakeLock"
            setTextColor(colorText)
            setTextSize(26f)
            typeface = Typeface.DEFAULT_BOLD
        }
        val subtitle = TextView(this).apply {
            text = "شاشة وهمية عند محاولة فتح القفل"
            setTextColor(colorMuted)
            setTextSize(13f)
            setPadding(0, dp(4), 0, dp(20))
        }
        content.addView(title)
        content.addView(subtitle)

        // ---------- Card: الإعدادات ----------
        val settingsCard = makeCard("⚙ الإعدادات")

        val msgLabel = makeLabel("نص الرسالة")
        settingsCard.addView(msgLabel)

        etMessage = EditText(this).apply {
            setTextColor(colorText)
            setHintTextColor(colorMuted)
            hint = "تم اكتشاف وصول غير مصرح به"
            setTextSize(14f)
            background = makeInputDrawable()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setText(prefs.getString("message", "تم اكتشاف وصول غير مصرح به"))
            setSelection(text.length)
        }
        val msgLp = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) }
        settingsCard.addView(etMessage, msgLp)

        val secLabel = makeLabel("مدة العداد (ثانية)").apply {
            setPadding(0, dp(12), 0, 0)
        }
        settingsCard.addView(secLabel)

        etSeconds = EditText(this).apply {
            setTextColor(colorText)
            setHintTextColor(colorMuted)
            hint = "15"
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextSize(14f)
            background = makeInputDrawable()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setText(prefs.getInt("seconds", 15).toString())
        }
        val secLp = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) }
        settingsCard.addView(etSeconds, secLp)

        val saveBtn = makePrimaryButton("💾 حفظ الإعدادات", colorSurfaceAlt, colorBorder, colorText)
        saveBtn.setOnClickListener { saveSettings() }
        val saveLp = LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(14) }
        settingsCard.addView(saveBtn, saveLp)

        content.addView(settingsCard, cardLp())

        // ---------- Card: الصلاحيات ----------
        val permCard = makeCard("🔑 الصلاحيات")

        val adminRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val adminLabel = makeLabel("Device Admin").apply {
            setTextSize(14f)
            setTextColor(colorText)
        }
        adminRow.addView(adminLabel, LinearLayout.LayoutParams(0, -2, 1f))

        tvAdminStatus = TextView(this).apply {
            setTextSize(12f)
            setPadding(dp(8), 0, dp(10), 0)
        }
        adminRow.addView(tvAdminStatus)

        val adminBtn = makeSmallButton("تفعيل").apply {
            setOnClickListener { requestAdmin() }
        }
        adminRow.addView(adminBtn, LinearLayout.LayoutParams(-2, dp(34)))

        permCard.addView(adminRow)

        val overlayRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val overlayLabel = makeLabel("الرسم فوق التطبيقات").apply {
            setTextSize(14f)
            setTextColor(colorText)
        }
        overlayRow.addView(overlayLabel, LinearLayout.LayoutParams(0, -2, 1f))

        tvOverlayStatus = TextView(this).apply {
            setTextSize(12f)
            setPadding(dp(8), 0, dp(10), 0)
        }
        overlayRow.addView(tvOverlayStatus)

        val overlayBtn = makeSmallButton("تفعيل").apply {
            setOnClickListener { requestOverlay() }
        }
        overlayRow.addView(overlayBtn, LinearLayout.LayoutParams(-2, dp(34)))

        val overlayRowLp = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
        permCard.addView(overlayRow, overlayRowLp)

        content.addView(permCard, cardLp())

        // ---------- Card: التحكم ----------
        val ctrlCard = makeCard("▶ التحكم")

        val startBtn = makePrimaryButton(
            "تشغيل المراقبة", colorGreen, colorGreenLight, Color.WHITE
        )
        startBtn.setOnClickListener { startMonitoring() }
        ctrlCard.addView(startBtn, LinearLayout.LayoutParams(-1, dp(52)))

        val testBtn = makePrimaryButton(
            "🧪 اختبار الشاشة السوداء", colorPrimary, colorPrimaryLight, Color.WHITE
        )
        testBtn.setOnClickListener { testOverlay() }
        val testLp = LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(10) }
        ctrlCard.addView(testBtn, testLp)

        tvServiceStatus = TextView(this).apply {
            text = "الخدمة متوقفة"
            setTextColor(colorMuted)
            setTextSize(12f)
            setPadding(0, dp(12), 0, 0)
            gravity = Gravity.CENTER
        }
        ctrlCard.addView(tvServiceStatus)

        content.addView(ctrlCard, cardLp())

        // ---------- Footer ----------
        val footer = TextView(this).apply {
            text = "© 2026 FakeLock — KlencodIDE"
            setTextColor(colorMuted)
            setTextSize(11f)
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, 0)
        }
        content.addView(footer)

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    // ============================================================
    // Helpers
    // ============================================================
    private fun makeCard(titleText: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = GradientDrawable().apply {
                setColor(colorSurface)
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), colorBorder)
            }
        }
        val t = TextView(this).apply {
            text = titleText
            setTextColor(colorText)
            setTextSize(15f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(10))
        }
        card.addView(t)
        return card
    }

    private fun cardLp(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }

    private fun makeLabel(text: String): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(colorMuted)
            setTextSize(12f)
        }

    private fun makeInputDrawable(): GradientDrawable =
        GradientDrawable().apply {
            setColor(colorSurfaceAlt)
            cornerRadius = dp(8).toFloat()
            setStroke(dp(1), colorBorder)
        }

    private fun makePrimaryButton(
        text: String, bg: Int, stroke: Int, txt: Int
    ): Button = Button(this).apply {
        this.text = text
        setTextSize(14f)
        isAllCaps = false
        setTextColor(txt)
        typeface = Typeface.DEFAULT_BOLD
        background = GradientDrawable().apply {
            setColor(bg)
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), stroke)
        }
        setPadding(dp(12), 0, dp(12), 0)
    }

    private fun makeSmallButton(text: String): Button = Button(this).apply {
        this.text = text
        setTextSize(12f)
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = GradientDrawable().apply {
            setColor(colorPrimary)
            cornerRadius = dp(8).toFloat()
        }
        setPadding(dp(14), 0, dp(14), 0)
    }

    // ============================================================
    // Permissions
    // ============================================================
    private fun refreshPermissions() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val comp = ComponentName(this, MyDeviceAdminReceiver::class.java)
        val adminOn = dpm != null && dpm.isAdminActive(comp)
        tvAdminStatus.text = if (adminOn) "✅" else "❌"
        tvAdminStatus.setTextColor(if (adminOn) colorSuccess else colorDanger)

        val overlayOn = Settings.canDrawOverlays(this)
        tvOverlayStatus.text = if (overlayOn) "✅" else "❌"
        tvOverlayStatus.setTextColor(if (overlayOn) colorSuccess else colorDanger)
    }

    private fun requestAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(
            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
            ComponentName(this, MyDeviceAdminReceiver::class.java)
        )
        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "مطلوب لمراقبة محاولات فتح القفل الفاشلة"
        )
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestOverlay() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ============================================================
    // Actions
    // ============================================================
    private fun saveSettings() {
        val msg = etMessage.text.toString().ifBlank { "تم اكتشاف وصول غير مصرح به" }
        val sec = etSeconds.text.toString().toIntOrNull()?.coerceIn(3, 3600) ?: 15
        prefs.edit()
            .putString("message", msg)
            .putInt("seconds", sec)
            .apply()
        Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show()
    }

    private fun startMonitoring() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "فعّل صلاحية Overlay أولاً", Toast.LENGTH_SHORT).show()
            return
        }
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val comp = ComponentName(this, MyDeviceAdminReceiver::class.java)
        if (dpm == null || !dpm.isAdminActive(comp)) {
            Toast.makeText(this, "تنبيه: فعّل Device Admin لتعمل المراقبة تلقائياً", Toast.LENGTH_LONG).show()
        }
        val svc = Intent(this, LockMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc)
        } else {
            startService(svc)
        }
        tvServiceStatus.text = "✅ المراقبة نشطة"
        tvServiceStatus.setTextColor(colorSuccess)
        Toast.makeText(this, "المراقبة تعمل الآن", Toast.LENGTH_SHORT).show()
    }

    private fun testOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "فعّل صلاحية Overlay أولاً", Toast.LENGTH_SHORT).show()
            return
        }
        val svc = Intent(this, LockMonitorService::class.java)
        svc.action = LockMonitorService.ACTION_SHOW_OVERLAY
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc)
        } else {
            startService(svc)
        }
    }
}


// ============================================================
// MyDeviceAdminReceiver — يستقبل محاولات فتح القفل الفاشلة
// ============================================================
class MyDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        val svc = Intent(context, LockMonitorService::class.java)
        svc.action = LockMonitorService.ACTION_SHOW_OVERLAY
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc)
        } else {
            context.startService(svc)
        }
    }
}


// ============================================================
// LockMonitorService — الخدمة + الشاشة السوداء
// ============================================================
class LockMonitorService : Service() {

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.fop.alp.SHOW_OVERLAY"
        private const val CHANNEL_ID = "fakelock"
        private const val NOTIF_ID = 1001
    }

    private lateinit var wm: WindowManager
    private var overlayView: View? = null
    private var countdownTimer: CountDownTimer? = null
    private var volumeReceiver: BroadcastReceiver? = null
    private var registered = false

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIF_ID,
                buildNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }

        if (intent?.action == ACTION_SHOW_OVERLAY) {
            showOverlay()
        }
        return START_STICKY
    }

    private fun createChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(
            CHANNEL_ID, "FakeLock",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            enableVibration(false)
        }
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("🔒 FakeLock")
            .setContentText("المراقبة نشطة")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    // ============================================================
    // Overlay
    // ============================================================
    private fun showOverlay() {
        if (overlayView != null) return

        val prefs = getSharedPreferences("fakelock", Context.MODE_PRIVATE)
        val message = prefs.getString("message", "تم اكتشاف وصول غير مصرح به") ?: ""
        val seconds = prefs.getInt("seconds", 15).toLong()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            isFocusableInTouchMode = true
            isFocusable = true
            requestFocus()
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }

        val icon = TextView(this).apply {
            text = "⚠"
            setTextColor(0xFFF85149.toInt())
            textSize = 64f
            gravity = Gravity.CENTER
        }

        val tvMsg = TextView(this).apply {
            text = message
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setPadding(48, 24, 48, 24)
        }

        val tvCountdown = TextView(this).apply {
            setTextColor(0xFFF85149.toInt())
            textSize = 52f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        val tvHint = TextView(this).apply {
            text = "اضغط زر الصوت للإخفاء"
            setTextColor(0xFF666666.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }

        container.addView(icon)
        container.addView(tvMsg)
        container.addView(tvCountdown)
        container.addView(tvHint)
        root.addView(container)

        // Volume key
        root.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN &&
                (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                        keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
                hideOverlay()
                true
            } else false
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.OPAQUE
        )

        try {
            wm.addView(root, params)
            overlayView = root
            root.requestFocus()
        } catch (e: Exception) {
            Toast.makeText(this, "فشل عرض الشاشة: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        // Broadcast receiver — fallback لأزرار الصوت
        volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                if (i?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    hideOverlay()
                }
            }
        }
        try {
            registerReceiver(volumeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
            registered = true
        } catch (_: Exception) {}

        // Countdown
        tvCountdown.text = "${seconds}s"
        countdownTimer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(ms: Long) {
                tvCountdown.text = "${ms / 1000}s"
            }
            override fun onFinish() {
                hideOverlay()
            }
        }.start()
    }

    private fun hideOverlay() {
        countdownTimer?.cancel()
        countdownTimer = null

        if (registered) {
            try { unregisterReceiver(volumeReceiver) } catch (_: Exception) {}
            registered = false
        }
        volumeReceiver = null

        overlayView?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
    }

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}


// ============================================================
// BootReceiver — يعيد تشغيل المراقبة بعد إعادة التشغيل
// ============================================================
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val svc = Intent(context, LockMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svc)
            } else {
                context.startService(svc)
            }
        }
    }
}