package com.github.k1rakishou.chan.ui.globalstate.toolbar

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.core_logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface IToolbarGlobalState {
  interface Readable {
    fun toolbarVisibilityStateFlow(): StateFlow<Boolean>
    fun toolbarHeightStateFlow(): StateFlow<Dp>
  }

  interface Writeable {
    fun updateToolbarVisibilityState(visible: Boolean)
    fun updateToolbarHeightState(height: Dp)
  }
}

class ToolbarGlobalState : IToolbarGlobalState.Readable, IToolbarGlobalState.Writeable {
  private val _toolbarShown = MutableStateFlow<Boolean>(true)
  private val _toolbarHeight = MutableStateFlow<Dp>(0.dp)

  override fun toolbarVisibilityStateFlow(): StateFlow<Boolean> {
    return _toolbarShown.asStateFlow()
  }

  override fun toolbarHeightStateFlow(): StateFlow<Dp> {
    return _toolbarHeight
  }

  override fun updateToolbarVisibilityState(visible: Boolean) {
    Logger.verbose(TAG) { "updateToolbarVisibilityState() visible: ${visible}" }
    _toolbarShown.value = visible
  }

  override fun updateToolbarHeightState(height: Dp) {
    Logger.verbose(TAG) { "updateToolbarHeightState() height: ${height}" }
    _toolbarHeight.value = height
  }

  companion object {
    private const val TAG = "ToolbarGlobalState"
  }

}