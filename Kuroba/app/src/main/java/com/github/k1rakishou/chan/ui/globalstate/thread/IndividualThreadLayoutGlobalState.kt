package com.github.k1rakishou.chan.ui.globalstate.thread

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.github.k1rakishou.chan.ui.layout.ThreadLayout

interface IIndividualThreadLayoutGlobalState {
  interface Readable {
    val threadLayoutState: State<ThreadLayout.State>
  }

  interface Writable {
    fun updateThreadLayoutState(state: ThreadLayout.State)
  }
}


class IndividualThreadLayoutGlobalState(
  private val isCatalog: Boolean
) : IIndividualThreadLayoutGlobalState.Readable, IIndividualThreadLayoutGlobalState.Writable {
  private val _threadLayoutState = mutableStateOf<ThreadLayout.State>(ThreadLayout.State.EMPTY)
  override val threadLayoutState: State<ThreadLayout.State>
    get() = _threadLayoutState

  override fun updateThreadLayoutState(state: ThreadLayout.State) {
    _threadLayoutState.value = state
  }
}