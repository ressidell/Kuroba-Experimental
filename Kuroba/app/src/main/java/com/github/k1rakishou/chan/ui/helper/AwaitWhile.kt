package com.github.k1rakishou.chan.ui.helper

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive

suspend fun CoroutineScope.awaitWhile(
  maxWaitTimeMs: Long = 1000L,
  waitWhile: () -> Boolean
): Boolean {
  val waitUntil = SystemClock.elapsedRealtime() + maxWaitTimeMs

  while (isActive && waitWhile()) {
    awaitFrame()

    val currentTime = SystemClock.elapsedRealtime()
    if (currentTime >= waitUntil) {
      // Wait the last time and try again
      awaitFrame()
      return waitWhile()
    }
  }

  return true
}