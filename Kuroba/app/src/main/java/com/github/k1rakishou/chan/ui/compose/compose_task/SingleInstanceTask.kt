package com.github.k1rakishou.chan.ui.compose.compose_task

import androidx.compose.runtime.RememberObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


/**
 * A task where only one block can be executed at a time. Launching a new block while another one is being executed
 * will just do nothing. Good for launching coroutines from button click listeners where you want to be sure that there
 * can be no more than one callback executed at the same time.
 * */
class SingleInstanceTask(
  override val coroutineScope: CoroutineScope
) : ComposeCoroutineTask, RememberObserver {
  @Volatile private var job: Job? = null

  override val isRunning: Boolean
    get() = job != null

  override fun launch(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> Unit
  ) {
    val alreadyExecuting = synchronized(this) { job != null }
    if (alreadyExecuting) {
      return
    }

    val newJob = coroutineScope.launch(
      context = context,
      start = CoroutineStart.LAZY,
      block = {
        try {
          block()
        } finally {
          synchronized(this@SingleInstanceTask) { job = null }
        }
      }
    )

    synchronized(this) {
      if (job != null) {
        newJob.cancel()
        return@synchronized
      }

      job = newJob
      newJob.start()
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