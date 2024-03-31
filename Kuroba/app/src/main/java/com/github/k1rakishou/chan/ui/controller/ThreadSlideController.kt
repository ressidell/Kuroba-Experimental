package com.github.k1rakishou.chan.ui.controller

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.toArgb
import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.controller.Controller
import com.github.k1rakishou.chan.controller.DeprecatedNavigationFlags
import com.github.k1rakishou.chan.controller.transition.ControllerTransition
import com.github.k1rakishou.chan.controller.transition.TransitionMode
import com.github.k1rakishou.chan.core.di.component.activity.ActivityComponent
import com.github.k1rakishou.chan.features.drawer.MainControllerCallbacks
import com.github.k1rakishou.chan.features.toolbar_v2.BackArrowMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.HamburgMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.KurobaToolbarState
import com.github.k1rakishou.chan.ui.controller.navigation.DoubleNavigationController
import com.github.k1rakishou.chan.ui.globalstate.reply.ReplyLayoutBoundsStates
import com.github.k1rakishou.chan.ui.globalstate.reply.ReplyLayoutVisibilityStates
import com.github.k1rakishou.chan.ui.layout.ThreadSlidingPaneLayout
import com.github.k1rakishou.chan.ui.widget.SlidingPaneLayoutEx
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.core_themes.ThemeEngine
import com.github.k1rakishou.core_themes.ThemeEngine.ThemeChangesListener
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class ThreadSlideController(
  context: Context,
  mainControllerCallbacks: MainControllerCallbacks,
  private val emptyView: ViewGroup
) : Controller(context),
  DoubleNavigationController,
  SlidingPaneLayoutEx.PanelSlideListener,
  ThemeChangesListener {

  @Inject
  lateinit var themeEngine: ThemeEngine

  private var leftController: BrowseController? = null
  private var rightController: ViewThreadController? = null
  private var mainControllerCallbacks: MainControllerCallbacks?
  private var slidingPaneLayout: ThreadSlidingPaneLayout? = null
  private var slidingPaneLayoutOpenState = SlidingPaneLayoutOpenState.LeftOpened
  private var isSlidingInProgress = false

  private val emptyCatalogToolbar by lazy(LazyThreadSafetyMode.NONE) {
    val kurobaToolbarState = KurobaToolbarState(
      controllerKey = controllerKey,
      globalUiStateHolder = globalUiStateHolder
    )

    kurobaToolbarState.enterDefaultMode(
      leftItem = HamburgMenuItem(
        onClick = {
          // no-op
        }
      )
    )

    return@lazy kurobaToolbarState
  }

  private val emptyThreadToolbar by lazy(LazyThreadSafetyMode.NONE) {
    val kurobaToolbarState = KurobaToolbarState(
      controllerKey = controllerKey,
      globalUiStateHolder = globalUiStateHolder
    )

    kurobaToolbarState.enterDefaultMode(
      leftItem = BackArrowMenuItem(
        onClick = { switchToController(leftController = true, animated = true) }
      )
    )

    return@lazy kurobaToolbarState
  }

  override fun injectDependencies(component: ActivityComponent) {
    component.inject(this)
  }

  init {
    this.mainControllerCallbacks = mainControllerCallbacks
  }

  override val toolbarState: KurobaToolbarState
    get() = getToolbarState(slidingPaneLayoutOpenState)

  override fun onCreate() {
    super.onCreate()

    doubleNavigationController = this

    updateNavigationFlags(
      newNavigationFlags = DeprecatedNavigationFlags(
        swipeable = false,
        hasDrawer = true
      )
    )

    toolbarState.enterDefaultMode(
      leftItem = null,
      middleContent = null
    )

    view = AppModuleAndroidUtils.inflate(context, R.layout.controller_thread_slide)
    slidingPaneLayout = view.findViewById<ThreadSlidingPaneLayout>(R.id.sliding_pane_layout).also { slidingPane ->
      slidingPane.setThreadSlideController(this)
      slidingPane.setPanelSlideListener(this)
      slidingPane.setParallaxDistance(AppModuleAndroidUtils.dp(100f))
      slidingPane.allowedToSlide(ChanSettings.viewThreadControllerSwipeable.get())

      if (ChanSettings.isSlideLayoutMode()) {
        slidingPane.setShadowResourceLeft(R.drawable.panel_shadow)
      }

      slidingPane.openPane()
    }

    updateLeftController(leftController = null, animated = false)
    updateRightController(rightController = null, animated = false)

    val textView = emptyView.findViewById<TextView>(R.id.select_thread_text)
    textView?.setTextColor(themeEngine.chanTheme.textColorSecondary)

    themeEngine.addListener(this)
    onThemeChanged()

    controllerScope.launch {
      combine(
        flow = globalUiStateHolder.replyLayout.replyLayoutVisibilityEventsFlow,
        flow2 = globalUiStateHolder.replyLayout.replyLayoutsBoundsFlow,
        flow3 = globalUiStateHolder.mainUiState.touchPositionFlow,
        transform = { replyLayoutVisibilityEvents, replyLayoutsBounds, touchPosition ->
          return@combine SlidingPaneLockState(
            replyLayoutVisibilityStates = replyLayoutVisibilityEvents,
            replyLayoutsBounds = replyLayoutsBounds,
            touchPosition = touchPosition,
          )
        }
      )
        .onEach { slidingPaneLockState -> slidingPaneLayout?.lockUnlockSliding(slidingPaneLockState.isLocked()) }
        .collect()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    themeEngine.removeListener(this)
    mainControllerCallbacks = null
  }

  override fun onThemeChanged() {
    slidingPaneLayout?.sliderFadeColor = themeEngine.chanTheme.backColorCompose.copy(alpha = 0.7f).toArgb()
    slidingPaneLayout?.coveredFadeColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f).toArgb()
  }

  override fun onShow() {
    super.onShow()
    mainControllerCallbacks?.resetBottomNavViewCheckState()
  }

  fun onSlidingPaneLayoutStateRestored() {
    val restoredOpen = slidingPaneLayout?.preservedOpenState
      ?: return

    val newSlidingPaneLayoutOpenState = SlidingPaneLayoutOpenState.from(restoredOpen)
    if (newSlidingPaneLayoutOpenState != slidingPaneLayoutOpenState) {
      slidingPaneLayoutOpenState = newSlidingPaneLayoutOpenState
      slideStateChanged(false)
    }
  }

  override fun onPanelSlide(panel: View, slideOffset: Float) {
    if (!isSlidingInProgress) {
      isSlidingInProgress = true
      startToolbarTransition()
    } else {
      updateToolbarTransition(slideOffset)
    }
  }

  override fun onPanelOpened(panel: View) {
    if (slidingPaneLayoutOpenState != SlidingPaneLayoutOpenState.from(isLeftOpen())) {
      slidingPaneLayoutOpenState = SlidingPaneLayoutOpenState.LeftOpened
      slideStateChanged()
    }

    finishToolbarTransition(slidingPaneLayoutOpenState)
  }

  override fun onPanelClosed(panel: View) {
    if (slidingPaneLayoutOpenState != SlidingPaneLayoutOpenState.from(isLeftOpen())) {
      slidingPaneLayoutOpenState = SlidingPaneLayoutOpenState.RightOpened
      slideStateChanged()
    }

    finishToolbarTransition(slidingPaneLayoutOpenState)
  }

  override fun switchToController(leftController: Boolean) {
    switchToController(leftController, true)
  }

  override fun switchToController(leftController: Boolean, animated: Boolean) {
    if (leftController != isLeftOpen()) {
      if (leftController) {
        slidingPaneLayout?.openPane()
        slidingPaneLayoutOpenState = SlidingPaneLayoutOpenState.LeftOpening
      } else {
        slidingPaneLayout?.closePane()
        slidingPaneLayoutOpenState = SlidingPaneLayoutOpenState.RightOpening
      }

      toolbarState.showToolbar()

      // TODO: New toolbar.
//      leftOpen = leftController
//      slideStateChanged(animated)
    }
  }

  override fun updateLeftController(leftController: Controller?, animated: Boolean) {
    this.leftController?.let { left ->
      left.onHide()
      removeChildController(left)
    }

    this.leftController = leftController as BrowseController?

    if (leftController != null && slidingPaneLayout != null) {
      addChildController(leftController)
      leftController.attachToParentView(slidingPaneLayout!!.leftPane)
      leftController.onShow()

      if (isLeftOpen()) {
        updateContainerToolbarStateWithChildToolbarState(
          slidingPaneLayoutOpenState = SlidingPaneLayoutOpenState.LeftOpened,
          animate = animated
        )
      }
    }
  }

  override fun updateRightController(rightController: Controller?, animated: Boolean) {
    if (this.rightController != null) {
      this.rightController!!.onHide()
      removeChildController(this.rightController!!)
    } else {
      slidingPaneLayout?.rightPane?.removeAllViews()
    }

    this.rightController = rightController as ViewThreadController?

    if (rightController != null) {
      if (slidingPaneLayout != null) {
        addChildController(rightController)
        rightController.attachToParentView(slidingPaneLayout!!.rightPane)
        rightController.onShow()

        if (!isLeftOpen()) {
          updateContainerToolbarStateWithChildToolbarState(
            slidingPaneLayoutOpenState = SlidingPaneLayoutOpenState.RightOpened,
            animate = animated
          )
        }
      }
    } else {
      slidingPaneLayout?.rightPane?.addView(emptyView)
    }
  }

  override fun leftController(): BrowseController? {
    return leftController
  }

  override fun rightController(): ViewThreadController? {
    return rightController
  }

  override fun openControllerWrappedIntoBottomNavAwareController(controller: Controller) {
    requireStartActivity().openControllerWrappedIntoBottomNavAwareController(controller)
  }

  override fun pushController(to: Controller): Boolean {
    return navigationController?.pushController(to) ?: false
  }

  override fun pushController(to: Controller, animated: Boolean): Boolean {
    return navigationController?.pushController(to, animated) ?: false
  }

  override fun pushController(to: Controller, transition: ControllerTransition?): Boolean {
    return navigationController?.pushController(to, transition) ?: false
  }

  override fun popController(): Boolean {
    return navigationController?.popController() ?: false
  }

  override fun popController(animated: Boolean): Boolean {
    return navigationController?.popController(animated) ?: false
  }

  override fun popController(transition: ControllerTransition?): Boolean {
    return navigationController?.popController(transition) ?: false
  }

  override fun onBack(): Boolean {
    if (!isLeftOpen()) {
      if (rightController != null && rightController?.onBack() == true) {
        return true
      }

      switchToController(true)
      return true
    } else {
      if (leftController != null && leftController?.onBack() == true) {
        return true
      }
    }

    return super.onBack()
  }

  fun isLeftOpen(): Boolean {
    return slidingPaneLayout!!.isOpen
  }

  private fun slideStateChanged(animated: Boolean = true) {
    updateContainerToolbarStateWithChildToolbarState(
      slidingPaneLayoutOpenState = slidingPaneLayoutOpenState,
      animate = animated
    )

    if (slidingPaneLayoutOpenState.leftOpenedOrOpening && rightController != null) {
      (rightController as ReplyAutoCloseListener).onReplyViewShouldClose()
    } else if (slidingPaneLayoutOpenState.rightOpenedOrOpening && leftController != null) {
      (leftController as ReplyAutoCloseListener).onReplyViewShouldClose()
    }

    notifyFocusLost(
      controllerType = if (slidingPaneLayoutOpenState.leftOpenedOrOpening) {
        ThreadControllerType.Thread
      } else {
        ThreadControllerType.Catalog
      },
      controller = if (slidingPaneLayoutOpenState.leftOpenedOrOpening) {
        rightController
      } else {
        leftController
      }
    )

    notifyFocusGained(
      controllerType = if (slidingPaneLayoutOpenState.leftOpenedOrOpening) {
        ThreadControllerType.Catalog
      } else {
        ThreadControllerType.Thread
      },
      controller = if (slidingPaneLayoutOpenState.leftOpenedOrOpening) {
        leftController
      } else {
        rightController
      }
    )
  }

  private fun notifyFocusLost(controllerType: ThreadControllerType, controller: Controller?) {
    if (controller == null) {
      return
    }

    if (controller is SlideChangeListener) {
      (controller as SlideChangeListener).onLostFocus(controllerType)
    }

    for (childController in controller.childControllers) {
      notifyFocusLost(controllerType, childController)
    }
  }

  private fun notifyFocusGained(controllerType: ThreadControllerType, controller: Controller?) {
    if (controller == null) {
      return
    }

    if (controller is SlideChangeListener) {
      (controller as SlideChangeListener).onGainedFocus(controllerType)
    }

    for (childController in controller.childControllers) {
      notifyFocusGained(controllerType, childController)
    }
  }

  private fun updateContainerToolbarStateWithChildToolbarState(
    slidingPaneLayoutOpenState: SlidingPaneLayoutOpenState,
    animate: Boolean
  ) {
//    requireToolbarNavController().toolbarState.updateFromState(getToolbarState(left))

    // TODO: New toolbar. Might not work entirely correct.
    containerToolbarState = getToolbarState(slidingPaneLayoutOpenState)
  }

  private fun getToolbarState(slidingPaneLayoutOpenState: SlidingPaneLayoutOpenState): KurobaToolbarState {
    var kurobaToolbarState = if (slidingPaneLayoutOpenState.leftOpenedOrOpening) {
      leftController?.toolbarState
    } else {
      rightController?.toolbarState
    }

    if (kurobaToolbarState == null) {
      kurobaToolbarState = if (slidingPaneLayoutOpenState.leftOpenedOrOpening) {
        emptyCatalogToolbar
      } else {
        emptyThreadToolbar
      }
    }

    return kurobaToolbarState
  }

  private fun startToolbarTransition() {
    val transitionMode = if (slidingPaneLayoutOpenState.rightOpenedOrOpening) {
      TransitionMode.In
    } else {
      TransitionMode.Out
    }

    val kurobaToolbarState = getToolbarState(slidingPaneLayoutOpenState.invert())

    containerToolbarState.onTransitionProgressStart(
      other = kurobaToolbarState,
      transitionMode = transitionMode
    )
  }

  private fun updateToolbarTransition(slideOffset: Float) {
    containerToolbarState.onTransitionProgress(
      progress = slideOffset
    )
  }

  private fun finishToolbarTransition(slidingPaneLayoutOpenState: SlidingPaneLayoutOpenState) {
    if (!isSlidingInProgress) {
      return
    }

    containerToolbarState.onTransitionProgressFinished()
    // TODO: New toolbar. Might not work correctly.
    containerToolbarState = getToolbarState(slidingPaneLayoutOpenState)

    isSlidingInProgress = false
  }

  fun passMotionEventIntoSlidingPaneLayout(event: MotionEvent): Boolean {
    return slidingPaneLayout?.onTouchEvent(event) ?: false
  }

  interface ReplyAutoCloseListener {
    fun onReplyViewShouldClose()
  }

  interface SlideChangeListener {
    fun onGainedFocus(controllerType: ThreadControllerType)
    fun onLostFocus(controllerType: ThreadControllerType)
  }

  private data class SlidingPaneLockState(
    val replyLayoutVisibilityStates: ReplyLayoutVisibilityStates,
    val replyLayoutsBounds: ReplyLayoutBoundsStates,
    val touchPosition: Offset,
  ) {

    fun isLocked(): Boolean {
      if (replyLayoutVisibilityStates.anyExpanded()) {
        return true
      }

      if (touchPosition.isSpecified) {
        if (!replyLayoutsBounds.catalog.isEmpty && !replyLayoutVisibilityStates.catalog.isCollapsed()) {
          if (replyLayoutsBounds.catalog.contains(touchPosition)) {
            return true
          }
        }

        if (!replyLayoutsBounds.thread.isEmpty && !replyLayoutVisibilityStates.thread.isCollapsed()) {
          if (replyLayoutsBounds.thread.contains(touchPosition)) {
            return true
          }
        }
      }

      return false
    }

  }

  private enum class SlidingPaneLayoutOpenState {
    LeftOpening,
    LeftOpened,
    RightOpening,
    RightOpened;

    fun asTransition(): SlidingPaneLayoutOpenState {
      return when (this) {
        LeftOpened -> LeftOpening
        RightOpened -> RightOpening
        LeftOpening -> error("Shouldn't happen")
        RightOpening -> error("Shouldn't happen")
      }
    }

    fun invert(): SlidingPaneLayoutOpenState {
      return when (this) {
        LeftOpening -> RightOpening
        LeftOpened -> RightOpened
        RightOpening -> LeftOpening
        RightOpened -> LeftOpened
      }
    }

    val leftOpenedOrOpening: Boolean
      get() = this == LeftOpened || this == LeftOpening
    val rightOpenedOrOpening: Boolean
      get() = this == RightOpened || this == RightOpening

    companion object {
      fun from(leftOpened: Boolean): SlidingPaneLayoutOpenState {
        return if (leftOpened) {
          LeftOpened
        } else {
          RightOpened
        }
      }
    }
  }

  companion object {
    private const val TAG = "ThreadSlideController"
  }
}
