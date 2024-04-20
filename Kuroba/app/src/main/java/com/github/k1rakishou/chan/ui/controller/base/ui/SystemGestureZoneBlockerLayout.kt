package com.github.k1rakishou.chan.ui.controller.base.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.core.base.KurobaCoroutineScope
import com.github.k1rakishou.chan.ui.globalstate.GlobalUiStateHolder
import com.github.k1rakishou.chan.ui.globalstate.global.GlobalTouchPositionListener
import com.github.k1rakishou.chan.ui.helper.AppResources
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.chan.utils.awaitUntilGloballyLaidOutAndGetSize
import com.github.k1rakishou.common.AndroidUtils
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.absoluteValue

class SystemGestureZoneBlockerLayout @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), GlobalTouchPositionListener {

  @Inject
  lateinit var appResources: AppResources
  @Inject
  lateinit var globalUiStateHolder: GlobalUiStateHolder

  private val _coroutineScope = KurobaCoroutineScope()
  private val _gestureIgnoreZones = mutableListOf(Rect(), Rect())
  private val _touchPositionListenerKey = "SystemGestureZoneBlockerLayoutListener"

  private val isAndroid10 by lazy(LazyThreadSafetyMode.NONE) { AndroidUtils.isAndroid10() }

  private val debugPaint by lazy(LazyThreadSafetyMode.NONE) {
    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
      style = android.graphics.Paint.Style.FILL
      color = Color.MAGENTA
    }
  }

  private var _totalWidth: Int = 0
  private var _totalHeight: Int = 0
  private var _zonesNeedInvalidation: Boolean = false
  private var _middlePointY = 0

  init {
    if (!isInEditMode) {
      AppModuleAndroidUtils.extractActivityComponent(context)
        .inject(this)
    }

    setWillNotDraw(false)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()

    if (isAndroid10) {
      globalUiStateHolder.mainUi.addTouchPositionListener(_touchPositionListenerKey, this)

      _coroutineScope.cancelChildren()
      _coroutineScope.launch {
        val (totalWidth, totalHeight) = awaitUntilGloballyLaidOutAndGetSize(waitForWidth = true, waitForHeight = true)

        _totalWidth = totalWidth
        _totalHeight = totalHeight
      }
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()

    if (isAndroid10) {
      _coroutineScope.cancelChildren()
      globalUiStateHolder.mainUi.removeTouchPositionListener(_touchPositionListenerKey)
    }
  }

  override fun dispatchDraw(canvas: Canvas) {
    super.dispatchDraw(canvas)

    // For visualizing
//    if (isAndroid10) {
//      _gestureIgnoreZones.forEach { rect ->
//        canvas.drawRect(rect, debugPaint)
//      }
//    }

    if (AndroidUtils.isAndroid10() && _zonesNeedInvalidation) {
      val gestureZonesValid = _gestureIgnoreZones.all { rect -> rect.width() > 0 && rect.height() > 0 }
      if (gestureZonesValid) {
        systemGestureExclusionRects = _gestureIgnoreZones
      }

      _zonesNeedInvalidation = false
    }
  }

  override fun onTouchPositionUpdated(touchPosition: Offset, eventAction: Int?) {
    if (!isAndroid10 || !touchPosition.isValid() || touchPosition.isUnspecified || _totalWidth <= 0 || _totalHeight <= 0) {
      return
    }

    val totalWidth = _totalWidth
    val totalHeight = _totalHeight

    val minZoneSize = with(appResources.composeDensity) { 24.dp.roundToPx() }
    val zoneHalfSize = with(appResources.composeDensity) { 200.dp.roundToPx() / 2 }
    val recalculateMiddlePointOffset = zoneHalfSize - minZoneSize

    when (eventAction) {
      MotionEvent.ACTION_DOWN -> {
        _middlePointY = touchPosition.y.toInt()
      }
      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_CANCEL -> {
        _middlePointY = 0
        return
      }
    }

    if ((touchPosition.y.toInt() - _middlePointY).absoluteValue >= recalculateMiddlePointOffset) {
      _middlePointY = touchPosition.y.toInt()
    }

    val middlePointY = _middlePointY

    val zoneTop = (middlePointY - zoneHalfSize).coerceIn(0, totalHeight)
    val zoneBottom = (middlePointY + zoneHalfSize).coerceIn(0, totalHeight)

    val newLeftZone = Rect(0, zoneTop, minZoneSize, zoneBottom)
    val newRightZone = Rect(totalWidth - minZoneSize, zoneTop, totalWidth, zoneBottom)

    val currentLeftZone = _gestureIgnoreZones[0]
    val currentRightZone = _gestureIgnoreZones[1]

    var changed = false

    if (currentLeftZone != newLeftZone) {
      _gestureIgnoreZones[0] = newLeftZone
      changed = true
    }

    if (currentRightZone != newRightZone) {
      _gestureIgnoreZones[1] = newRightZone
      changed = true
    }

    if (changed) {
      _zonesNeedInvalidation = true
      invalidate()
    }
  }

}