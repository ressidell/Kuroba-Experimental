package com.github.k1rakishou.chan.ui.controller.navigation

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.controller.Controller
import com.github.k1rakishou.chan.controller.transition.ControllerTransition
import com.github.k1rakishou.chan.controller.transition.PopControllerTransition
import com.github.k1rakishou.chan.controller.transition.PushControllerTransition
import com.github.k1rakishou.chan.core.di.component.activity.ActivityComponent
import com.github.k1rakishou.chan.features.drawer.MainControllerCallbacks
import com.github.k1rakishou.chan.features.toolbar_v2.KurobaToolbarState
import com.github.k1rakishou.chan.ui.controller.PopupController
import com.github.k1rakishou.chan.ui.layout.SplitNavigationControllerLayout
import com.github.k1rakishou.chan.ui.view.NavigationViewContract
import com.github.k1rakishou.common.updatePaddings
import com.github.k1rakishou.core_themes.ThemeEngine
import com.github.k1rakishou.core_themes.ThemeEngine.ThemeChangesListener
import javax.inject.Inject

class SplitNavigationController(
  context: Context,
  private val emptyView: ViewGroup,
  mainControllerCallbacks: MainControllerCallbacks
) : Controller(context), DoubleNavigationController, ThemeChangesListener {

  @Inject
  lateinit var themeEngine: ThemeEngine

  private var _leftController: Controller? = null
  private var _rightController: Controller? = null

  private var mainControllerCallbacks: MainControllerCallbacks?
  private var leftControllerView: FrameLayout? = null
  private var rightControllerView: FrameLayout? = null
  private var selectThreadText: TextView? = null
  private var popup: PopupController? = null
  private var popupChild: StyledToolbarNavigationController? = null

  override fun injectDependencies(component: ActivityComponent) {
    component.inject(this)
  }

  init {
    this.mainControllerCallbacks = mainControllerCallbacks
  }

  override val toolbarState: KurobaToolbarState
    get() = error("Cannot be used directly!")

  val leftControllerToolbarState: KurobaToolbarState?
    get() = leftController()?.toolbarState
  val rightControllerToolbarState: KurobaToolbarState?
    get() = rightController()?.toolbarState

  override fun onCreate() {
    super.onCreate()

    doubleNavigationController = this

    val container = SplitNavigationControllerLayout(context)
    view = container

    leftControllerView = FrameLayout(context)
    rightControllerView = FrameLayout(context)

    if (mainControllerCallbacks?.navigationViewContractType == NavigationViewContract.Type.BottomNavView) {
      val bottomNavViewHeight = context.resources.getDimension(R.dimen.navigation_view_size).toInt()
      container.updatePaddings(null, null, null, bottomNavViewHeight)
    }

    container.setLeftView(leftControllerView)
    container.setRightView(rightControllerView)
    container.setDivider(View(context))
    container.build()

    selectThreadText = emptyView.findViewById<TextView>(R.id.select_thread_text)
    updateRightController(null, false)

    themeEngine.addListener(this)
    onThemeChanged()
  }

  override fun onDestroy() {
    super.onDestroy()

    themeEngine.removeListener(this)
    mainControllerCallbacks = null
  }

  override fun onThemeChanged() {
    selectThreadText?.setTextColor(themeEngine.chanTheme.textColorSecondary)
  }

  override fun updateLeftController(leftController: Controller?, animated: Boolean) {
    if (leftController() != null) {
      leftController()?.onHide()
      removeChildController(leftController())
    }

    this._leftController = leftController

    if (leftController != null) {
      addChildController(leftController)
      leftController.attachToParentView(leftControllerView)
      leftController.onShow()
    }
  }

  override fun updateRightController(rightController: Controller?, animated: Boolean) {
    if (rightController() != null) {
      rightController()?.onHide()
      removeChildController(rightController())
    } else {
      rightControllerView?.removeAllViews()
    }

    this._rightController = rightController

    if (rightController != null) {
      addChildController(rightController)
      rightController.attachToParentView(rightControllerView)
      rightController.onShow()
    } else {
      rightControllerView?.addView(emptyView)
    }
  }

  override fun leftController(): Controller? {
    return _leftController
  }

  override fun rightController(): Controller? {
    return _rightController
  }

  override fun switchToController(leftController: Boolean, animated: Boolean) {
    // both are always visible
  }

  override fun switchToController(leftController: Boolean) {
    // both are always visible
  }

  override fun openControllerWrappedIntoBottomNavAwareController(controller: Controller) {
    requireStartActivity().openControllerWrappedIntoBottomNavAwareController(controller)
  }

  override fun pushController(to: Controller): Boolean {
    return pushController(to, true)
  }

  override fun pushController(to: Controller, animated: Boolean): Boolean {
    return pushController(to, if (animated) PushControllerTransition() else null)
  }

  override fun pushController(to: Controller, transition: ControllerTransition?): Boolean {
    if (popup == null) {
      popup = PopupController(context)
      presentController(popup!!)
      popupChild = StyledToolbarNavigationController(context)
      popup!!.setChildController(popupChild)
      popupChild!!.pushController(to, false)
    } else {
      popupChild!!.pushController(to, transition)
    }

    return true
  }

  override fun popController(): Boolean {
    return popController(true)
  }

  override fun popController(animated: Boolean): Boolean {
    return popController(if (animated) PopControllerTransition() else null)
  }

  override fun popController(transition: ControllerTransition?): Boolean {
    if (popup == null) {
      return false
    }

    if (popupChild?.childControllers?.size == 1) {
      if (mainControllerCallbacks != null) {
        mainControllerCallbacks!!.resetBottomNavViewCheckState()
      }
      if (presentingThisController != null) {
        presentingThisController!!.stopPresenting()
      }
      popup = null
      popupChild = null
    } else {
      popupChild!!.popController(transition)
    }

    return true
  }

  fun popAll() {
    if (popup != null) {
      if (mainControllerCallbacks != null) {
        mainControllerCallbacks!!.resetBottomNavViewCheckState()
      }

      if (presentingThisController != null) {
        presentingThisController!!.stopPresenting()
      }

      popup = null
      popupChild = null
    }
  }

  override fun onBack(): Boolean {
    if (leftController()?.onBack() == true) {
      return true
    } else if (rightController()?.onBack() == true) {
      return true
    }

    return super.onBack()
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (rightController()?.dispatchKeyEvent(event) == true) {
      return true
    }

    if (leftController()?.dispatchKeyEvent(event) == true) {
      return true
    }

    return super.dispatchKeyEvent(event)
  }
}
