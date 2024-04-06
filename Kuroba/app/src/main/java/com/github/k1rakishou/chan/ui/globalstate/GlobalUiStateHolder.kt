package com.github.k1rakishou.chan.ui.globalstate

import com.github.k1rakishou.chan.ui.globalstate.drawer.DrawerGlobalState
import com.github.k1rakishou.chan.ui.globalstate.drawer.IDrawerGlobalState
import com.github.k1rakishou.chan.ui.globalstate.fastsroller.FastScrollerGlobalState
import com.github.k1rakishou.chan.ui.globalstate.fastsroller.IFastScrollerGlobalState
import com.github.k1rakishou.chan.ui.globalstate.global.IMainUiState
import com.github.k1rakishou.chan.ui.globalstate.global.MainUiState
import com.github.k1rakishou.chan.ui.globalstate.reply.IReplyLayoutGlobalState
import com.github.k1rakishou.chan.ui.globalstate.reply.ReplyLayoutGlobalState
import com.github.k1rakishou.chan.ui.globalstate.scroll.IScrollGlobalState
import com.github.k1rakishou.chan.ui.globalstate.scroll.ScrollGlobalState
import com.github.k1rakishou.chan.ui.globalstate.toolbar.IToolbarGlobalState
import com.github.k1rakishou.chan.ui.globalstate.toolbar.ToolbarGlobalState
import com.github.k1rakishou.chan.ui.helper.AppResources

class GlobalUiStateHolder(
  private val appResources: AppResources
) {
  private val _mainUiState = MainUiState()
  val mainUiState: IMainUiState.Readable
    get() = _mainUiState

  private val _replyLayout = ReplyLayoutGlobalState()
  val replyLayout: IReplyLayoutGlobalState.Readable
    get() = _replyLayout

  private val _fastScroller = FastScrollerGlobalState()
  val fastScroller: IFastScrollerGlobalState.Readable
    get() = _fastScroller

  private val _drawer = DrawerGlobalState()
  val drawer: IDrawerGlobalState.Readable
    get() = _drawer

  private val _scrollState = ScrollGlobalState(appResources)
  val scrollState: IScrollGlobalState.Readable
    get() = _scrollState

  private val _toolbarState = ToolbarGlobalState()
  val toolbarState: IToolbarGlobalState.Readable
    get() = _toolbarState

  fun updateMainUiState(updater: IMainUiState.Writeable.() -> Unit) {
    updater(_mainUiState)
  }

  fun updateReplyLayoutState(updater: IReplyLayoutGlobalState.Writable.() -> Unit) {
    updater(_replyLayout)
  }

  fun updateFastScrollerState(updater: IFastScrollerGlobalState.Writeable.() -> Unit) {
    updater(_fastScroller)
  }

  fun updateDrawerState(updater: IDrawerGlobalState.Writable.() -> Unit) {
    updater(_drawer)
  }

  fun updateScrollState(updater: IScrollGlobalState.Writable.() -> Unit) {
    updater(_scrollState)
  }

  fun updateToolbarState(updater: IToolbarGlobalState.Writable.() -> Unit) {
    updater(_toolbarState)
  }

}