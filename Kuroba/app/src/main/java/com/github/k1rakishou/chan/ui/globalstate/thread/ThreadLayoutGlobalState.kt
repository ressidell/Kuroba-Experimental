package com.github.k1rakishou.chan.ui.globalstate.thread

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.github.k1rakishou.chan.ui.controller.ThreadControllerType
import com.github.k1rakishou.chan.ui.layout.ThreadLayout
import com.github.k1rakishou.core_logger.Logger

interface IThreadLayoutGlobalState {
  interface Readable {
    val focusedControllerState: State<ThreadControllerType>

    fun threadLayoutState(threadControllerType: ThreadControllerType): State<ThreadLayout.State>
  }

  interface Writeable {
    fun updateThreadLayoutState(threadControllerType: ThreadControllerType, state: ThreadLayout.State)
    fun updateFocusedController(threadControllerType: ThreadControllerType)
  }
}

class ThreadLayoutGlobalState : IThreadLayoutGlobalState.Readable, IThreadLayoutGlobalState.Writeable {
  private val catalogLayoutState = IndividualThreadLayoutGlobalState(isCatalog = true)
  private val threadLayoutState = IndividualThreadLayoutGlobalState(isCatalog = false)

  private val _focusedControllerState = mutableStateOf<ThreadControllerType>(ThreadControllerType.Catalog)
  override val focusedControllerState: State<ThreadControllerType>
    get() = _focusedControllerState

  override fun threadLayoutState(threadControllerType: ThreadControllerType): State<ThreadLayout.State> {
    return when (threadControllerType) {
      ThreadControllerType.Catalog -> catalogLayoutState.threadLayoutState
      ThreadControllerType.Thread -> threadLayoutState.threadLayoutState
    }
  }

  override fun updateThreadLayoutState(threadControllerType: ThreadControllerType, state: ThreadLayout.State) {
    Logger.verbose(threadControllerType.tag()) { "updateThreadLayoutState() state: ${state}" }

    when (threadControllerType) {
      ThreadControllerType.Catalog -> catalogLayoutState.updateThreadLayoutState(state)
      ThreadControllerType.Thread -> threadLayoutState.updateThreadLayoutState(state)
    }
  }

  override fun updateFocusedController(threadControllerType: ThreadControllerType) {
    Logger.verbose(threadControllerType.tag()) { "updateFocusedController() threadControllerType: ${threadControllerType}" }

    _focusedControllerState.value = threadControllerType
  }

  private fun ThreadControllerType.tag(): String {
    return when (this) {
      ThreadControllerType.Catalog -> "CatalogLayoutGlobalState"
      ThreadControllerType.Thread -> "ThreadLayoutGlobalState"
    }
  }

}

