package com.github.k1rakishou.chan.ui.globalstate.global

import android.view.MotionEvent
import android.view.VelocityTracker
import com.github.k1rakishou.chan.ui.controller.base.ControllerKey
import com.github.k1rakishou.core_logger.Logger
import kotlin.math.absoluteValue

class VelocityTracking {
  private val _velocityTracker by lazy { VelocityTracker.obtain() }
  private val _screensTrackingVelocity = mutableSetOf<ControllerKey>()

  fun startTrackingScrollSpeed(controllerKey: ControllerKey) {
    _screensTrackingVelocity += controllerKey
    _velocityTracker.clear()
  }

  fun updateScrollSpeed(controllerKey: ControllerKey, motionEvent: MotionEvent) {
    if (!_screensTrackingVelocity.contains(controllerKey)) {
      Logger.error(TAG) { "${controllerKey} is not present in _screensTrackingVelocity. Did you forget to call startTrackingScrollSpeed()?" }
    }

    _velocityTracker.addMovement(motionEvent)
  }

  fun stopTrackingScrollSpeed(controllerKey: ControllerKey) {
    _screensTrackingVelocity -= controllerKey
    _velocityTracker.clear()
  }

  fun calculateVelocity(controllerKey: ControllerKey): Velocity {
    if (!_screensTrackingVelocity.contains(controllerKey)) {
      Logger.error(TAG) { "${controllerKey} is not present in _screensTrackingVelocity. Did you forget to call startTrackingScrollSpeed()?" }
    }

    _velocityTracker.computeCurrentVelocity(1000)

    return Velocity(
      xVelocity = _velocityTracker.xVelocity.absoluteValue,
      yVelocity = _velocityTracker.yVelocity.absoluteValue
    )
  }

  data class Velocity(
    val xVelocity: Float,
    val yVelocity: Float
  )

  companion object {
    private const val TAG = "VelocityTracking"
  }

}