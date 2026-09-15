package com.kronosync.domain.lockin

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Plain-View full-screen block overlay drawn over whatever app the user switched to. Deliberately
 * not Compose: a WindowManager-attached view needs its own ViewTreeLifecycleOwner/
 * SavedStateRegistryOwner wiring to host Compose, which is real complexity this simple screen
 * doesn't need.
 */
class LockInOverlayController(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    fun show(taskTitle: String?, onReturnTap: () -> Unit) {
        if (overlayView != null) return

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#101312"))
            setPadding(64, 64, 64, 64)
        }
        root.addView(TextView(context).apply {
            text = "Locked in"
            setTextColor(Color.parseColor("#5CDBBA"))
            textSize = 28f
            gravity = Gravity.CENTER
        })
        root.addView(TextView(context).apply {
            text = taskTitle?.let { "Focusing on: $it" } ?: "Ad-hoc focus session"
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 0)
        })
        root.addView(TextView(context).apply {
            text = "This app is off-limits for now. Camera and phone calls still work."
            setTextColor(Color.parseColor("#BFC9C2"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 48)
        })
        root.addView(Button(context).apply {
            text = "Back to KronoSync"
            setOnClickListener { onReturnTap() }
        })

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        )
        windowManager.addView(root, params)
        overlayView = root
    }

    fun hide() {
        overlayView?.let {
            runCatching { windowManager.removeView(it) }
            overlayView = null
        }
    }

    fun isShowing(): Boolean = overlayView != null
}
