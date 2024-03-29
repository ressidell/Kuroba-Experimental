package com.github.k1rakishou.chan.ui.globalstate.drawer

import com.github.k1rakishou.core_logger.Logger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

interface IDrawerGlobalState {
  interface Readable {
    val drawerOpenCloseEventFlow: SharedFlow<Boolean>
    val drawerAppearanceEventFlow: StateFlow<DrawerAppearanceEvent>
  }

  interface Writable {
    fun openDrawer()
    fun closeDrawer()
    fun onDrawerAppearanceChanged(opened: Boolean)
  }
}

class DrawerGlobalState : IDrawerGlobalState.Readable, IDrawerGlobalState.Writable {

  private val _drawerOpenCloseEventFlow = MutableSharedFlow<Boolean>(
    replay = 1,
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  override val drawerOpenCloseEventFlow: SharedFlow<Boolean>
    get() = _drawerOpenCloseEventFlow.asSharedFlow()

  private val _drawerAppearanceEventFlow = MutableStateFlow<DrawerAppearanceEvent>(DrawerAppearanceEvent.Closed)
  override val drawerAppearanceEventFlow: StateFlow<DrawerAppearanceEvent>
    get() = _drawerAppearanceEventFlow.asStateFlow()

  override fun openDrawer() {
    _drawerOpenCloseEventFlow.tryEmit(true)
  }

  override fun closeDrawer() {
    _drawerOpenCloseEventFlow.tryEmit(false)
  }

  override fun onDrawerAppearanceChanged(opened: Boolean) {
    Logger.verbose(TAG) { "onDrawerAppearanceChanged() opened: ${opened}" }

    val drawerAppearanceEvent = if (opened) {
      DrawerAppearanceEvent.Opened
    } else {
      DrawerAppearanceEvent.Closed
    }

    _drawerAppearanceEventFlow.value = drawerAppearanceEvent
  }

  companion object {
    private const val TAG = "DrawerGlobalState"
  }
}