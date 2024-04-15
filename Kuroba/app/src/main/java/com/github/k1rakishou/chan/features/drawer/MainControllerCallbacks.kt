package com.github.k1rakishou.chan.features.drawer

import android.view.MotionEvent

interface MainControllerCallbacks {
  fun passMotionEventIntoDrawer(event: MotionEvent): Boolean
}