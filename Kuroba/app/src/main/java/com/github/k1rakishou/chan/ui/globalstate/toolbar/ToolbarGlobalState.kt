package com.github.k1rakishou.chan.ui.globalstate.toolbar

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface IToolbarGlobalState {
  interface Readable {
    fun toolbarAppearanceFlow(): Flow<Boolean>
  }

  interface Writable {
    fun onToolbarAppearanceChanged(visible: Boolean)
  }
}

class ToolbarGlobalState : IToolbarGlobalState.Readable, IToolbarGlobalState.Writable {
  private val _toolbarShown = MutableStateFlow<Boolean>(true)

  override fun toolbarAppearanceFlow(): Flow<Boolean> {
    return _toolbarShown.asStateFlow()
  }

  override fun onToolbarAppearanceChanged(visible: Boolean) {
    _toolbarShown.value = visible
  }

}