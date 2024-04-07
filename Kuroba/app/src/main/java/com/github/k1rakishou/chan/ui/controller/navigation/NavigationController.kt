package com.github.k1rakishou.chan.ui.controller.navigation

import android.content.Context
import android.view.KeyEvent
import android.view.ViewGroup
import com.github.k1rakishou.chan.core.navigation.ControllerWithNavigation
import com.github.k1rakishou.chan.core.navigation.HasNavigation
import com.github.k1rakishou.chan.ui.controller.base.Controller
import com.github.k1rakishou.chan.ui.controller.base.transition.ControllerTransition
import com.github.k1rakishou.chan.ui.controller.base.transition.PopControllerTransition
import com.github.k1rakishou.chan.ui.controller.base.transition.PushControllerTransition
import com.github.k1rakishou.core_logger.Logger

abstract class NavigationController(context: Context) : Controller(context), ControllerWithNavigation, HasNavigation {
  protected var container: ViewGroup? = null
  private var controllerTransition: ControllerTransition? = null

  var isBlockingInput = false
    protected set

  override fun pushController(to: Controller): Boolean {
    return pushController(to, true)
  }

  override fun pushController(to: Controller, animated: Boolean): Boolean {
    return pushController(to, (if (animated) PushControllerTransition() else null))
  }

  override fun pushController(to: Controller, transition: ControllerTransition?): Boolean {
    val from = topController

    if (isBlockingInput) {
      Logger.error(TAG) {
        "pushController() to: ${to.controllerKey}, from: ${from?.controllerKey} isBlockingInput is true"
      }

      return false
    }

    var currentTransition = transition
    if (from == null) {
      // can't animate push if from is null, just disable the animation
      currentTransition = null
    }

    Logger.verbose(TAG) {
      "pushController() to: ${to.controllerKey}, from: ${from?.controllerKey} with transition: ${transition}"
    }

    transition(from, to, true, currentTransition)
    return true
  }

  override fun popController(): Boolean {
    return popController(true)
  }

  override fun popController(animated: Boolean): Boolean {
    return popController(if (animated) PopControllerTransition() else null)
  }

  override fun popController(transition: ControllerTransition?): Boolean {
    val from = topController
    if (from == null) {
      Logger.error(TAG) { "popController() topController is null" }
      return false
    }

    if (isBlockingInput) {
      Logger.error(TAG) { "popController() isBlockingInput is true" }
      return false
    }

    val to = childControllers.getOrNull(childControllers.size - 2)

    Logger.verbose(TAG) {
      "popController() to: ${to?.controllerKey}, from: ${from.controllerKey}  with transition: ${transition}"
    }

    transition(from, to, false, transition)
    return true
  }

  open fun beginSwipeTransition(from: Controller?, to: Controller?): Boolean {
    if (from == null && to == null) {
      return false
    }

    if (isBlockingInput) {
      return false
    }

    require(controllerTransition == null) {
      "popController() Cannot transition while another transition is in progress."
    }

    Logger.verbose(TAG) {
      "beginSwipeTransition() to: ${to?.controllerKey}, from: ${from?.controllerKey} "
    }

    isBlockingInput = true
    to?.onShow()
    return true
  }

  open fun swipeTransitionProgress(progress: Float) {

  }

  open fun endSwipeTransition(from: Controller?, to: Controller?, finish: Boolean) {
    if (from == null && to == null) {
      return
    }

    Logger.verbose(TAG) {
      "beginSwipeTransition() to: ${to?.controllerKey}, from: ${from?.controllerKey}, finish: ${finish}"
    }

    if (finish && from != null) {
      from.onHide()
      removeChildController(from)

      controllerNavigationManager.onControllerSwipedFrom(from)

      if (to != null) {
        controllerNavigationManager.onControllerSwipedTo(to)
      }
    } else if (to != null) {
      to.onHide()
    }

    controllerTransition = null
    isBlockingInput = false
  }

  open fun transition(
    from: Controller?,
    to: Controller?,
    pushing: Boolean,
    controllerTransition: ControllerTransition?
  ) {
    if (this.controllerTransition != null || isBlockingInput) {
      error("Cannot transition while another transition is in progress.")
    }

    if (!pushing && childControllers.isEmpty()) {
      error("Cannot pop with no controllers left")
    }

    Logger.verbose(TAG) {
      "transition() from: ${from?.controllerKey}, to: ${to?.controllerKey}, " +
        "pushing: ${pushing}, controllerTransition: ${controllerTransition}"
    }

    if (to != null) {
      to.navigationController = this
      to.previousSiblingController = from
    }

    if (pushing && to != null) {
      addChildController(to)
      to.attachToParentView(container)
    }

    to?.onShow()

    if (controllerTransition != null) {
      controllerTransition.navigationController = this
      controllerTransition.from = from
      controllerTransition.to = to

      isBlockingInput = true
      this.controllerTransition = controllerTransition

      controllerTransition.onTransitionFinished {
        finishTransition(
          from = from,
          to = to,
          pushing = pushing
        )
      }

      controllerTransition.perform()
      return
    }

    finishTransition(from, to, pushing)
  }

  private fun finishTransition(from: Controller?, to: Controller?, pushing: Boolean) {
    Logger.verbose(TAG) {
      "finishTransition() from: ${from?.controllerKey}, to: ${to?.controllerKey}, pushing: ${pushing}"
    }

    if (from == null && to == null) {
      return
    }

    from?.onHide()

    if (!pushing && from != null) {
      removeChildController(from)
    }

    controllerTransition = null
    isBlockingInput = false

    if (pushing && to != null) {
      controllerNavigationManager.onControllerPushed(to)
    } else if (!pushing && from != null) {
      controllerNavigationManager.onControllerPopped(from)
    }

    if (to != null) {
      containerToolbarState = to.toolbarState
    }
  }

  override fun onBack(): Boolean {
    if (isBlockingInput) {
      return true
    }

    if (childControllers.size > 0) {
      val top = topController
        ?: return false

      if (top.onBack()) {
        return true
      }

      if (childControllers.size > 1) {
        popController()
        return true
      }

      return false
    }

    return false

  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    val top = topController
    return top != null && top.dispatchKeyEvent(event)
  }

  companion object {
    private const val TAG = "NavigationController"
  }
}
