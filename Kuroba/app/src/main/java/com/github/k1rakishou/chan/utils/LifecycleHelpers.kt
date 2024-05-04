package com.github.k1rakishou.chan.utils

import androidx.lifecycle.Lifecycle

fun Lifecycle.isResumed(): Boolean {
  return currentState.isAtLeast(Lifecycle.State.RESUMED)
}

fun Lifecycle.isStarted(): Boolean {
  return currentState.isAtLeast(Lifecycle.State.STARTED)
}