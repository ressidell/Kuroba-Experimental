package com.github.k1rakishou.chan.ui.globalstate.fastsroller

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

interface IFastScrollerGlobalState {
  interface Readable {
    val isDraggingFastScrollerState: State<Boolean>

    fun isDraggingFastScroller(): Boolean
  }

  interface Writeable {
    fun updateIsDraggingFastScroller(dragging: Boolean)
  }
}

internal class FastScrollerGlobalState : IFastScrollerGlobalState.Readable, IFastScrollerGlobalState.Writeable {
  private val _isDraggingFastScrollerState = mutableStateOf(false)
  override val isDraggingFastScrollerState: State<Boolean>
    get() = _isDraggingFastScrollerState

  override fun isDraggingFastScroller(): Boolean {
    return _isDraggingFastScrollerState.value
  }

  override fun updateIsDraggingFastScroller(dragging: Boolean) {
    _isDraggingFastScrollerState.value = dragging
  }

}