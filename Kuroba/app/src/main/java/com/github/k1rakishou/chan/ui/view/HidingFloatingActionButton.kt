package com.github.k1rakishou.chan.ui.view

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.runtime.snapshotFlow
import androidx.core.graphics.withSave
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import androidx.core.view.updatePadding
import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.base.KurobaCoroutineScope
import com.github.k1rakishou.chan.core.manager.GlobalWindowInsetsManager
import com.github.k1rakishou.chan.core.manager.WindowInsetsListener
import com.github.k1rakishou.chan.ui.controller.ThreadControllerType
import com.github.k1rakishou.chan.ui.globalstate.GlobalUiStateHolder
import com.github.k1rakishou.chan.ui.globalstate.reply.ReplyLayoutVisibilityStates
import com.github.k1rakishou.chan.ui.layout.ThreadLayout
import com.github.k1rakishou.chan.ui.view.widget.SnackbarClass
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.dp
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.getDimen
import com.github.k1rakishou.chan.utils.TimeUtils
import com.github.k1rakishou.chan.utils.combineMany
import com.github.k1rakishou.chan.utils.setAlphaFast
import com.github.k1rakishou.common.updateMargins
import com.github.k1rakishou.core_themes.ThemeEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject

class HidingFloatingActionButton
  : AppCompatImageView,
  WindowInsetsListener,
  ThemeEngine.ThemeChangesListener {

  private var _listeningForInsetsChanges = false
  private var _threadControllerType: ThreadControllerType? = null
  private var _snackbarClass: SnackbarClass? = null

  private val padding = dp(12f)
  private val additionalPadding = dp(17f)
  private val hatOffsetX = dp(7f).toFloat()
  private val isChristmasToday = TimeUtils.isChristmasToday()
  private val is4chanBirthdayToday = TimeUtils.is4chanBirthdayToday()

  @Inject
  lateinit var globalUiStateHolder: GlobalUiStateHolder
  @Inject
  lateinit var globalWindowInsetsManager: GlobalWindowInsetsManager
  @Inject
  lateinit var themeEngine: ThemeEngine

  private val coroutineScope = KurobaCoroutineScope()
  private val christmasHat by lazy(LazyThreadSafetyMode.NONE) { BitmapFactory.decodeResource(resources, R.drawable.christmashat)!! }
  private val partyHat by lazy(LazyThreadSafetyMode.NONE) { BitmapFactory.decodeResource(resources, R.drawable.partyhat)!! }
  private val paint by lazy(LazyThreadSafetyMode.NONE) { Paint(Paint.ANTI_ALIAS_FLAG) }
  private val outlinePath = Path()
  private val fabOutlineProvider = FabOutlineProvider(outlinePath)

  private val paddingL = if (isChristmasToday || is4chanBirthdayToday) {
    additionalPadding + padding
  } else {
    padding
  }
  private val paddingT = if (isChristmasToday || is4chanBirthdayToday) {
    additionalPadding + padding
  } else {
    padding
  }

  constructor(context: Context) : super(context) {
    init()
  }

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    init()
  }

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr
  ) {
    init()
  }

  private fun init() {
    setWillNotDraw(false)

    updatePadding(left = paddingL, top = paddingT, right = padding, bottom = padding)

    if (!isInEditMode) {
      AppModuleAndroidUtils.extractActivityComponent(context)
        .inject(this)

      startListeningForInsetsChangesIfNeeded()
      onThemeChanged()
      updatePaddings()
    }

    outlineProvider = fabOutlineProvider
    setAlphaFast(0f)
  }

  fun setThreadControllerType(threadControllerType: ThreadControllerType) {
    check(_threadControllerType == null) {
      "Attempt to set threadControllerType twice! current: ${_threadControllerType}, new: ${threadControllerType}"
    }

    _threadControllerType = threadControllerType
  }

  fun setSnackbarClass(snackbarClass: SnackbarClass) {
    check(_snackbarClass == null) {
      "Attempt to set snackbarClass twice! current: ${_snackbarClass}, snackbarClass: ${snackbarClass}"
    }

    _snackbarClass = snackbarClass
  }

  override fun onTouchEvent(ev: MotionEvent): Boolean {
    if (alpha < 1f) {
      return false
    }

    return super.onTouchEvent(ev)
  }

  override fun onThemeChanged() {
    setBackgroundColor(themeEngine.chanTheme.accentColor)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()

    if (!isInEditMode) {
      startListeningForInsetsChangesIfNeeded()
      themeEngine.addListener(this)
      updatePaddings()

      listenForFabVisibilityFlags()
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()

    stopListeningForInsetsChanges()
    themeEngine.removeListener(this)
    coroutineScope.cancelChildren()
  }

  override fun onInsetsChanged() {
    updatePaddings()
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)

    updatePath(measuredWidth, measuredHeight)
  }

  override fun draw(canvas: Canvas) {
    canvas.withSave {
      canvas.clipPath(outlinePath)
      super.draw(canvas)
    }

    if (isChristmasToday || is4chanBirthdayToday) {
      canvas.withScale(x = 0.5f, y = 0.5f, pivotX = 0.5f, pivotY = 0.5f) {
        canvas.withTranslation(x = hatOffsetX) {
          if (isChristmasToday) {
            canvas.drawBitmap(christmasHat, 0f, 0f, paint)
          } else if (is4chanBirthdayToday) {
            canvas.drawBitmap(partyHat, 0f, 0f, paint)
          }
        }
      }
    }
  }

  private fun listenForFabVisibilityFlags() {
    coroutineScope.cancelChildren()
    coroutineScope.launch {
      while (_snackbarClass == null && _threadControllerType == null && isActive) {
        delay(100)
      }

      val snackbarClass = _snackbarClass
        ?: return@launch
      val threadControllerType = _threadControllerType
        ?: return@launch

      combineMany(
        ChanSettings.layoutMode.listenForChanges().asFlow(),
        ChanSettings.neverHideToolbar.listenForChanges().asFlow(),
        ChanSettings.enableReplyFab.listenForChanges().asFlow(),
        globalUiStateHolder.replyLayout.replyLayoutVisibilityEventsFlow,
        snapshotFlow { globalUiStateHolder.threadLayout.threadLayoutState(threadControllerType).value },
        snapshotFlow { globalUiStateHolder.threadLayout.focusedControllerState.value },
        snapshotFlow { globalUiStateHolder.fastScroller.isDraggingFastScrollerState.value },
        snapshotFlow { globalUiStateHolder.scroll.scrollTransitionProgress.floatValue },
        snapshotFlow { globalUiStateHolder.snackbar.snackbarVisibilityState(snackbarClass).value }
      ) { _, _, enableFab, replyLayoutState, threadLayout, focusedController, draggingFastScroller, scroll, snackbarVisible ->
        return@combineMany FabVisibilityInfo(
          fabEnabled = enableFab,
          replyLayoutVisibilityStates = replyLayoutState,
          threadLayoutState = threadLayout,
          focusedController = focusedController,
          scrollProgress = scroll,
          isDraggingFastScroller = draggingFastScroller,
          snackbarVisible = snackbarVisible
        )
      }
        .onEach { fabVisibilityInfo ->
          if (fabVisibilityInfo.isFabForceVisible(threadControllerType)) {
            setAlphaFast(1f)
            return@onEach
          }

          if (fabVisibilityInfo.needToHideFab(threadControllerType)) {
            setAlphaFast(0f)
            return@onEach
          }

          setAlphaFast(fabVisibilityInfo.fabAlpha)
        }
        .collect()
    }
  }

  private fun updatePath(inputWidthPx: Int, inputHeightPx: Int) {
    val widthPx = if (isChristmasToday || is4chanBirthdayToday) {
      inputWidthPx - additionalPadding
    } else {
      inputWidthPx
    }

    val heightPx = if (isChristmasToday || is4chanBirthdayToday) {
      inputHeightPx - additionalPadding
    } else {
      inputHeightPx
    }

    val offsetX = if (isChristmasToday || is4chanBirthdayToday) additionalPadding else 0
    val offsetY = if (isChristmasToday || is4chanBirthdayToday) additionalPadding else 0

    val centerX = offsetX + (widthPx / 2f)
    val centerY = offsetY + (heightPx / 2f)

    outlinePath.reset()
    outlinePath.addCircle(centerX, centerY, widthPx / 2f, Path.Direction.CW)
    outlinePath.close()
  }

  private fun stopListeningForInsetsChanges() {
    globalWindowInsetsManager.removeInsetsUpdatesListener(this)
    _listeningForInsetsChanges = false
  }

  private fun startListeningForInsetsChangesIfNeeded() {
    if (_listeningForInsetsChanges) {
      return
    }

    globalWindowInsetsManager.addInsetsUpdatesListener(this)
    _listeningForInsetsChanges = true
  }

  private fun updatePaddings() {
    val fabBottomMargin = getDimen(R.dimen.hiding_fab_margin)
    updateMargins(bottom = fabBottomMargin + globalWindowInsetsManager.bottom())
  }

  private data class FabVisibilityInfo(
    val fabEnabled: Boolean,
    val replyLayoutVisibilityStates: ReplyLayoutVisibilityStates,
    val threadLayoutState: ThreadLayout.State,
    val focusedController: ThreadControllerType,
    val scrollProgress: Float,
    val isDraggingFastScroller: Boolean,
    val snackbarVisible: Boolean
  ) {
    val fabAlpha: Float
      get() = scrollProgress

    fun isFabForceVisible(threadControllerType: ThreadControllerType): Boolean {
      if (!fabEnabled) {
        return false
      }

      if (ChanSettings.isSplitLayoutMode()) {
        if (isCurrentReplyLayoutOpened(threadControllerType)) {
          return false
        }

        if (threadLayoutState.isNotInContentState()) {
          return false
        }

        // fallthrough
      }

      return !ChanSettings.canCollapseToolbar()
    }

    fun needToHideFab(threadControllerType: ThreadControllerType): Boolean {
      if (!fabEnabled) {
        return true
      }

      if (ChanSettings.isSplitLayoutMode()) {
        if (isCurrentReplyLayoutOpened(threadControllerType)) {
          return true
        }

        if (threadLayoutState.isNotInContentState()) {
          return true
        }

        return false
      }

      if (threadLayoutState.isNotInContentState()) {
        return true
      }

      if (focusedController != threadControllerType) {
        return true
      }

      if (isDraggingFastScroller) {
        return true
      }

      if (isCurrentReplyLayoutOpened(threadControllerType)) {
        return true
      }

      if (snackbarVisible) {
        return true
      }

      return false
    }

    private fun isCurrentReplyLayoutOpened(threadControllerType: ThreadControllerType): Boolean {
      return when (threadControllerType) {
        ThreadControllerType.Catalog -> replyLayoutVisibilityStates.catalog.isOpenedOrExpanded()
        ThreadControllerType.Thread -> replyLayoutVisibilityStates.thread.isOpenedOrExpanded()
      }
    }

  }

  private class FabOutlineProvider(
    private val path: Path
  ) : ViewOutlineProvider() {

    override fun getOutline(view: View, outline: Outline) {
      outline.setConvexPath(path)
    }

  }

}