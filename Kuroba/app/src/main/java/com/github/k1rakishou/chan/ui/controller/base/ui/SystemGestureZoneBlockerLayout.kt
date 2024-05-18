package com.github.k1rakishou.chan.ui.controller.base.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.base.KurobaCoroutineScope
import com.github.k1rakishou.chan.core.helper.DialogFactory
import com.github.k1rakishou.chan.core.manager.GlobalWindowInsetsManager
import com.github.k1rakishou.chan.core.manager.HapticFeedbackManager
import com.github.k1rakishou.chan.ui.globalstate.GlobalUiStateHolder
import com.github.k1rakishou.chan.ui.globalstate.global.GlobalTouchPositionListener
import com.github.k1rakishou.chan.ui.helper.AppResources
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.chan.utils.awaitUntilGloballyLaidOutAndGetSize
import com.github.k1rakishou.common.AndroidUtils
import com.github.k1rakishou.common.resumeValueSafe
import com.github.k1rakishou.core_logger.Logger
import com.github.k1rakishou.core_themes.ThemeEngine
import com.github.k1rakishou.persist_state.PersistableChanState
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.suspendCancellableCoroutine
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
  @Inject
  lateinit var globalWindowInsetsManager: GlobalWindowInsetsManager
  @Inject
  lateinit var dialogFactory: DialogFactory
  @Inject
  lateinit var themeEngine: ThemeEngine
  @Inject
  lateinit var hapticFeedbackManager: HapticFeedbackManager

  private val _coroutineScope = KurobaCoroutineScope()
  private val _gestureIgnoreZones = mutableListOf(Rect(), Rect())
  private val _touchPositionListenerKey = "SystemGestureZoneBlockerLayoutListener"

  private val _animationRequests = MutableSharedFlow<Unit>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  private val _showExplanationDialog = MutableSharedFlow<Unit>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_LATEST
  )

  private val paint by lazy(LazyThreadSafetyMode.NONE) {
    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
      style = android.graphics.Paint.Style.FILL
      color = Color.MAGENTA
      alpha = 0
    }
  }

  private var _isGestureNavigationEnabled = false
  private var _anyReplyLayoutOpened = false
  private var _newSystemGestureZoneBlockerDialogShown = false
  private var _wasInsideOfTheZone: Boolean = false

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

  fun init() {
    if (AndroidUtils.isAndroid10()) {
      globalUiStateHolder.mainUi.addTouchPositionListener(_touchPositionListenerKey, this)

      _coroutineScope.launch {
        val (totalWidth, totalHeight) = awaitUntilGloballyLaidOutAndGetSize(waitForWidth = true, waitForHeight = true)

        _totalWidth = totalWidth
        _totalHeight = totalHeight
      }
    }

    _coroutineScope.launch {
      snapshotFlow { globalWindowInsetsManager.isGestureNavigationEnabled.value }
        .onEach { isGestureNavigationEnabledNow ->
          Logger.debug(TAG) { "isGestureNavigationEnabled: ${isGestureNavigationEnabledNow}" }

          if (_isGestureNavigationEnabled != isGestureNavigationEnabledNow) {
            _isGestureNavigationEnabled = isGestureNavigationEnabledNow
          }
        }
        .collect()
    }

    _coroutineScope.launch {
      globalUiStateHolder.replyLayout.replyLayoutVisibilityEventsFlow
        .onEach { replyLayoutVisibilityStates ->
          _anyReplyLayoutOpened = replyLayoutVisibilityStates.anyOpenedOrExpanded()
        }
        .collect()
    }

    _coroutineScope.launch {
      PersistableChanState.newSystemGestureZoneBlockerDialogShown.listenForChanges().asFlow()
        .onEach { newSystemGestureZoneBlockerDialogShown ->
          _newSystemGestureZoneBlockerDialogShown = newSystemGestureZoneBlockerDialogShown
        }
        .collect()
    }

    _coroutineScope.launch {
      _showExplanationDialog
        .takeWhile { !PersistableChanState.newSystemGestureZoneBlockerDialogShown.get() }
        .collectLatest {
          suspendCancellableCoroutine { continuation ->
            dialogFactory.createSimpleInformationDialog(
              context = context,
              titleText = appResources.string(R.string.gesture_exclusion_zone_dialog_title),
              descriptionText = appResources.string(R.string.gesture_exclusion_zone_dialog_description),
              onPositiveButtonClickListener = { PersistableChanState.newSystemGestureZoneBlockerDialogShown.set(true) },
              onDismissListener = { continuation.resumeValueSafe(Unit) }
            )
          }

          _animationRequests.tryEmit(Unit)
        }
    }

    _coroutineScope.launch {
      paint.color = themeEngine.chanTheme.accentColor

      themeEngine.themeChangeEventsFlow
        .collectLatest { chanTheme -> paint.color = chanTheme.accentColor }
    }

    _coroutineScope.launch {
      _animationRequests
        .collectLatest {
          // Poor man's alpha animation

          var currentAlpha = 150
          paint.alpha = currentAlpha
          delay(1000)

          while (isActive) {
            awaitFrame()

            currentAlpha -= 2
            if (currentAlpha < 0) {
              currentAlpha = 0
            }

            paint.alpha = currentAlpha
            invalidate()

            if (paint.alpha <= 0) {
              break
            }
          }
        }
    }
  }

  fun destroy() {
    _coroutineScope.cancelChildren()
    globalUiStateHolder.mainUi.removeTouchPositionListener(_touchPositionListenerKey)
  }

  override fun dispatchDraw(canvas: Canvas) {
    super.dispatchDraw(canvas)

    if (_isGestureNavigationEnabled && _anyReplyLayoutOpened && paint.alpha > 0 && AndroidUtils.isAndroid10() ) {
      _gestureIgnoreZones.forEach { rect ->
        canvas.drawRect(rect, paint)
      }
    }

    if (AndroidUtils.isAndroid10() && _zonesNeedInvalidation) {
      val gestureZonesValid = _gestureIgnoreZones.all { rect -> rect.width() > 0 && rect.height() > 0 }
      if (gestureZonesValid) {
        systemGestureExclusionRects = _gestureIgnoreZones
      } else {
        systemGestureExclusionRects = emptyList()
      }

      _zonesNeedInvalidation = false
    }
  }

  override fun onTouchPositionUpdated(touchPosition: Offset, eventAction: Int?) {
    if (!AndroidUtils.isAndroid10()) {
      return
    }

    if (!touchPosition.isValid() || touchPosition.isUnspecified || _totalWidth <= 0 || _totalHeight <= 0) {
      return
    }

    if (!_isGestureNavigationEnabled || !_anyReplyLayoutOpened) {
      if (systemGestureExclusionRects.isNotEmpty()) {
        _gestureIgnoreZones[0] = Rect()
        _gestureIgnoreZones[1] = Rect()

        _zonesNeedInvalidation = true
      }

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

    val isInsideOfTheZoneNow = touchPosition.x.toInt() in newLeftZone.left..newLeftZone.right
      || touchPosition.x.toInt() in newRightZone.left..newRightZone.right

    if (isInsideOfTheZoneNow) {
      if (!_newSystemGestureZoneBlockerDialogShown) {
        _showExplanationDialog.tryEmit(Unit)
        _newSystemGestureZoneBlockerDialogShown = true
      }

      _animationRequests.tryEmit(Unit)
    }

    if (isInsideOfTheZoneNow != _wasInsideOfTheZone) {
      if (isInsideOfTheZoneNow) {
        hapticFeedbackManager.gestureStart()
      } else {
        hapticFeedbackManager.gestureEnd()
      }
    }

    _wasInsideOfTheZone = isInsideOfTheZoneNow

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

  companion object {
    private const val TAG = "SystemGestureZoneBlockerLayout"
  }

}