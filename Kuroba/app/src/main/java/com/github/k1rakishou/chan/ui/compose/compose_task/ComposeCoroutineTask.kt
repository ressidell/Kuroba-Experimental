package com.github.k1rakishou.chan.ui.compose.compose_task

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

interface ComposeCoroutineTask {
  val coroutineScope: CoroutineScope
  val isRunning: Boolean

  fun launch(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
  )
}

@Composable
fun rememberSingleInstanceCoroutineTask(): ComposeCoroutineTask {
  return rememberCoroutineTask(taskType = TaskType.SingleInstance)
}

@Composable
fun rememberCancellableCoroutineTask(): ComposeCoroutineTask {
  return rememberCoroutineTask(taskType = TaskType.Cancellable)
}

@Composable
fun rememberCoroutineTask(taskType: TaskType): ComposeCoroutineTask {
  val coroutineScope = rememberCoroutineScope()

  val composeCoroutineTask by remember {
    val task = when (taskType) {
      TaskType.SingleInstance -> SingleInstanceTask(coroutineScope)
      TaskType.Cancellable -> CancellableTask(coroutineScope)
    }

    return@remember mutableStateOf(task)
  }

  return composeCoroutineTask
}

enum class TaskType {
  SingleInstance,
  Cancellable
}
