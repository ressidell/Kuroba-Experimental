package com.github.k1rakishou.chan.ui.globalstate.toolbar

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface IToolbarGlobalState {
  interface Readable {
    fun toolbarVisibilityStateFlow(): StateFlow<Boolean>
    fun toolbarHeightStateFlow(): StateFlow<Dp>
  }

  interface Writable {
    fun updateToolbarVisibilityState(visible: Boolean)
    fun updateToolbarHeightState(height: Dp)
  }
}

class ToolbarGlobalState : IToolbarGlobalState.Readable, IToolbarGlobalState.Writable {
  private val _toolbarShown = MutableStateFlow<Boolean>(true)
  private val _toolbarHeight = MutableStateFlow<Dp>(0.dp)

  override fun toolbarVisibilityStateFlow(): StateFlow<Boolean> {
    return _toolbarShown.asStateFlow()
  }

  override fun toolbarHeightStateFlow(): StateFlow<Dp> {
    return _toolbarHeight
  }

  override fun updateToolbarVisibilityState(visible: Boolean) {
    _toolbarShown.value = visible
  }

  override fun updateToolbarHeightState(height: Dp) {
    _toolbarHeight.value = height
  }
}