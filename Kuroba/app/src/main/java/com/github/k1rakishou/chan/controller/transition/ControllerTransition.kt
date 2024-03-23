package com.github.k1rakishou.chan.controller.transition

import android.animation.AnimatorSet
import com.github.k1rakishou.chan.controller.Controller
import java.util.*

abstract class ControllerTransition  {
  protected val animatorSet = AnimatorSet()
  private var callback: Callback? = null

  @JvmField
  var from: Controller? = null
  @JvmField
  var to: Controller? = null

  fun onCompleted() {
    if (callback == null) {
      throw NullPointerException("Callback cannot be null here!")
    }

    callback!!.onControllerTransitionCompleted(this)
  }

  fun setCallback(callback: Callback?) {
    this.callback = callback
  }

  fun debugInfo(): String {
    return String.format(
      Locale.ENGLISH,
      "callback=" + callback?.javaClass?.simpleName + ", " +
        "from=" + from?.javaClass?.simpleName + ", " +
        "to=" + to?.javaClass?.simpleName
    )
  }

  fun interface Callback {
    fun onControllerTransitionCompleted(transition: ControllerTransition?)
  }

  abstract fun perform()
}