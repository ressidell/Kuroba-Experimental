package com.github.k1rakishou.chan.ui.compose.compose_task

import androidx.compose.runtime.RememberObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class CancellableTask(
  override val coroutineScope: CoroutineScope
) : ComposeCoroutineTask, RememberObserver {

  @Volatile private var job: Job? = null

  override val isRunning: Boolean
    get() = job != null

  override fun launch(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> Unit
  ) {
    synchronized(this) {
      job?.cancel()
      job = coroutineScope.launch(
        context = context,
        block = {
          try {
            block()
          } finally {
            synchronized(this@CancellableTask) { job = null }
          }
        }
      )
    }
  }

  fun cancel() {
    synchronized(this) {
      job?.cancel()
      job = null
    }
  }

  override fun onRemembered() {
  }

  override fun onForgotten() {
    cancel()
  }

  override fun onAbandoned() {
    cancel()
  }

}