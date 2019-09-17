package com.example.beepizcontrol

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.Px
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.example.beepizcontrol.extensions.android.view.awaitOneClick
import com.google.android.material.button.MaterialButton

@SuppressLint("SetTextI18n") // This is just a sample
class BeepizControlUiImpl(private val activity: Activity) : BeepizControlUi {

    override suspend fun awaitStartMonitoringRequest() {
        startStopButton.text = "Start"
        startStopButton.setIconResource(R.drawable.ic_play_arrow_black_24dp)
        try {
            startStopButton.awaitOneClick()
        } finally {
            startStopButton.text = ""
            startStopButton.icon = null
        }
    }

    override suspend fun awaitStopMonitoringRequest() {
        startStopButton.text = "Stop"
        startStopButton.setIconResource(R.drawable.ic_stop_black_24dp)
        try {
            startStopButton.awaitOneClick()
        } finally {
            startStopButton.text = ""
            startStopButton.icon = null
        }
    }

    val root: View

    private val startStopButton = MaterialButton(activity).apply {
        val fgTint = ColorStateList.valueOf(Color.BLACK)
        iconTint = fgTint
        setTextColor(fgTint)
        backgroundTintList = colorSL(R.color.beepiz_yellow_selector)
        isEnabled = false
    }

    init {
        activity.window.apply {
            val secondaryColor = activity.color(R.color.pink_900)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = secondaryColor
            navigationBarColor = secondaryColor
        }
        root = LinearLayout(activity).apply {
            setBackgroundResource(R.color.pink_800)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            padding = dip(16)
            addView(AppCompatTextView(context).apply {
                text = "Sample third-party app"
                gravity = Gravity.CENTER
                textAppearance = R.style.TextAppearance_MaterialComponents_Headline4
            }, lParams(width = matchParent))
            addView(AppCompatTextView(context).apply {
                text = "Beepiz monitoring"
                gravity = Gravity.CENTER
                textAppearance = R.style.TextAppearance_MaterialComponents_Headline6
            }, lParams(width = matchParent) {
                topMargin = dip(16)
            })
            addView(startStopButton, lParams { topMargin = dip(16) })
        }
    }
}

private fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()
private fun View.dip(value: Int) = context.dip(value)

@ColorInt
private fun Context.color(@ColorRes colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

@ColorInt
private fun View.color(@ColorRes colorRes: Int) = context.color(colorRes)

private fun Context.colorSL(@ColorRes colorRes: Int): ColorStateList =
    ContextCompat.getColorStateList(this, colorRes)!!

private fun View.colorSL(@ColorRes colorRes: Int) = context.colorSL(colorRes)


private inline var View.padding: Int
    @Deprecated(NO_GETTER, level = DeprecationLevel.HIDDEN) get() = noGetter
    set(@Px value) = setPadding(value, value, value, value)

private var TextView.textAppearance: Int
    @Deprecated(NO_GETTER, level = DeprecationLevel.HIDDEN) get() = noGetter
    @Suppress("DEPRECATION")
    set(@StyleRes value) = if (SDK_INT < 23) setTextAppearance(context, value)
    else setTextAppearance(value)

@Suppress("unused") // Restrict usage scope.
private inline fun LinearLayout.lParams(
    width: Int = wrapContent,
    height: Int = wrapContent,
    initParams: LinearLayout.LayoutParams.() -> Unit = {}
): LinearLayout.LayoutParams {
    return LinearLayout.LayoutParams(width, height).apply(initParams)
}

/**
 * **A LESS CAPITALIZED ALIAS** to [ViewGroup.LayoutParams.MATCH_PARENT] that is only
 * visible inside [ViewGroup]s.
 */
@Suppress("unused")
private inline val View.matchParent
    get() = ViewGroup.LayoutParams.MATCH_PARENT

/**
 * **A LESS CAPITALIZED ALIAS** to [ViewGroup.LayoutParams.WRAP_CONTENT] that is only
 * visible inside [ViewGroup]s.
 */
@Suppress("unused")
private inline val View.wrapContent
    get() = ViewGroup.LayoutParams.WRAP_CONTENT


private const val NO_GETTER = "Property does not have a getter"

/**
 * Usage example:
 * `@Deprecated(NO_GETTER, level = DeprecationLevel.HIDDEN) get() = noGetter`
 */
private inline val noGetter: Nothing
    get() = throw UnsupportedOperationException(NO_GETTER)
