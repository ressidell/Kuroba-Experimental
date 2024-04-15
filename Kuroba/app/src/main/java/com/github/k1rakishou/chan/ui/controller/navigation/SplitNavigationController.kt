package com.github.k1rakishou.chan.ui.controller.navigation

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.di.component.activity.ActivityComponent
import com.github.k1rakishou.chan.features.toolbar.KurobaToolbarState
import com.github.k1rakishou.chan.ui.controller.base.Controller
import com.github.k1rakishou.chan.ui.controller.base.transition.ControllerTransition
import com.github.k1rakishou.chan.ui.controller.base.transition.PopControllerTransition
import com.github.k1rakishou.chan.ui.layout.SplitNavigationControllerLayout
import com.github.k1rakishou.core_themes.ThemeEngine
import com.github.k1rakishou.core_themes.ThemeEngine.ThemeChangesListener
import javax.inject.Inject

class SplitNavigationController(
  context: Context,
  private val emptyView: ViewGroup,
) : Controller(context), DoubleNavigationController, ThemeChangesListener {

  @Inject
  lateinit var themeEngine: ThemeEngine

  private var _leftController: Controller? = null
  private var _rightController: Controller? = null

  private var leftControllerView: FrameLayout? = null
  private var rightControllerView: FrameLayout? = null
  private var selectThreadText: TextView? = null

  override fun injectDependencies(component: ActivityComponent) {
    component.inject(this)
  }

  override val toolbarState: KurobaToolbarState
    get() = error("Cannot be used directly!")

  override val leftControllerToolbarState: KurobaToolbarState?
    get() = leftController()?.toolbarState
  override val rightControllerToolbarState: KurobaToolbarState?
    get() = rightController()?.toolbarState

  override fun onCreate() {
    super.onCreate()

    doubleNavigationController = this

    val container = SplitNavigationControllerLayout(context)
    view = container

    leftControllerView = FrameLayout(context)
    rightControllerView = FrameLayout(context)

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

  override fun switchToLeftController(animated: Boolean) {
    // both are always visible
  }

  override fun switchToRightController(animated: Boolean) {
    // both are always visible
  }

  override fun pushToLeftController(controller: Controller, animated: Boolean) {
    check(controller.doubleControllerType == null) {
      "doubleControllerType (${doubleControllerType}) was already set for controller: ${controller}."
    }

    controller.doubleControllerType = DoubleControllerType.Left

    leftController()?.requireNavController()?.pushController(controller, animated)
  }

  override fun pushToRightController(controller: Controller, animated: Boolean) {
    check(controller.doubleControllerType == null) {
      "doubleControllerType (${doubleControllerType}) was already set for controller: ${controller}."
    }

    controller.doubleControllerType = DoubleControllerType.Right

    rightController()?.requireNavController()?.pushController(controller, animated)
  }

  override fun pushController(to: Controller): Boolean {
    error("Use pushToLeftController/pushToRightController when in SPLIT mode!")
  }

  override fun pushController(to: Controller, animated: Boolean): Boolean {
    error("Use pushToLeftController/pushToRightController when in SPLIT mode!")
  }

  override fun pushController(to: Controller, onFinished: () -> Unit): Boolean {
    error("Use pushToLeftController/pushToRightController when in SPLIT mode!")
  }

  override fun pushController(to: Controller, transition: ControllerTransition?): Boolean {
    error("Use pushToLeftController/pushToRightController when in SPLIT mode!")
  }

  override fun popController(): Boolean {
    return popController(true)
  }

  override fun popController(animated: Boolean): Boolean {
    return popController(if (animated) PopControllerTransition() else null)
  }

  override fun popController(onFinished: () -> Unit): Boolean {
    val transition = PopControllerTransition()
    transition.onTransitionFinished { onFinished() }
    return popController(transition)
  }

  override fun popController(transition: ControllerTransition?): Boolean {
    error("Seems like this is never used?")
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
