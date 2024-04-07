package com.github.k1rakishou.chan.ui.globalstate.fastsroller

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.github.k1rakishou.core_logger.Logger

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
    Logger.verbose(TAG) { "updateIsDraggingFastScroller() dragging: ${dragging}" }

    _isDraggingFastScrollerState.value = dragging
  }

  companion object {
    private const val TAG = "FastScrollerGlobalState"
  }

}