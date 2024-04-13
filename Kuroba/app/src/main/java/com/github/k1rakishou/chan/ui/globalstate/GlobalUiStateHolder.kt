package com.github.k1rakishou.chan.ui.globalstate

import com.github.k1rakishou.chan.ui.globalstate.bottompanel.BottomPanelGlobalState
import com.github.k1rakishou.chan.ui.globalstate.bottompanel.IBottomPanelGlobalState
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
import com.github.k1rakishou.chan.ui.globalstate.snackbar.ISnackbarGlobalState
import com.github.k1rakishou.chan.ui.globalstate.snackbar.SnackbarGlobalState
import com.github.k1rakishou.chan.ui.globalstate.thread.IThreadLayoutGlobalState
import com.github.k1rakishou.chan.ui.globalstate.thread.ThreadLayoutGlobalState
import com.github.k1rakishou.chan.ui.globalstate.toolbar.IToolbarGlobalState
import com.github.k1rakishou.chan.ui.globalstate.toolbar.ToolbarGlobalState
import com.github.k1rakishou.chan.ui.helper.AppResources

class GlobalUiStateHolder(
  private val appResources: AppResources
) {
  private val _mainUi = MainUiState()
  val mainUi: IMainUiState.Readable
    get() = _mainUi

  private val _replyLayout = ReplyLayoutGlobalState()
  val replyLayout: IReplyLayoutGlobalState.Readable
    get() = _replyLayout

  private val _fastScroller = FastScrollerGlobalState()
  val fastScroller: IFastScrollerGlobalState.Readable
    get() = _fastScroller

  private val _drawer = DrawerGlobalState()
  val drawer: IDrawerGlobalState.Readable
    get() = _drawer

  private val _scroll = ScrollGlobalState(appResources)
  val scroll: IScrollGlobalState.Readable
    get() = _scroll

  private val _toolbar = ToolbarGlobalState()
  val toolbar: IToolbarGlobalState.Readable
    get() = _toolbar

  private val _bottomPanel = BottomPanelGlobalState()
  val bottomPanel: IBottomPanelGlobalState.Readable
    get() = _bottomPanel

  private val _threadLayout = ThreadLayoutGlobalState()
  val threadLayout: IThreadLayoutGlobalState.Readable
    get() = _threadLayout

  private val _snackbar = SnackbarGlobalState()
  val snackbar: ISnackbarGlobalState.Readable
    get() = _snackbar

  fun updateMainUiState(updater: IMainUiState.Writeable.() -> Unit) {
    updater(_mainUi)
  }

  fun updateReplyLayoutState(updater: IReplyLayoutGlobalState.Writeable.() -> Unit) {
    updater(_replyLayout)
  }

  fun updateFastScrollerState(updater: IFastScrollerGlobalState.Writeable.() -> Unit) {
    updater(_fastScroller)
  }

  fun updateDrawerState(updater: IDrawerGlobalState.Writeable.() -> Unit) {
    updater(_drawer)
  }

  fun updateScrollState(updater: IScrollGlobalState.Writeable.() -> Unit) {
    updater(_scroll)
  }

  fun updateToolbarState(updater: IToolbarGlobalState.Writeable.() -> Unit) {
    updater(_toolbar)
  }

  fun updateThreadLayoutState(updater: IThreadLayoutGlobalState.Writeable.() -> Unit) {
    updater(_threadLayout)
  }

  fun updateSnackbarState(updater: ISnackbarGlobalState.Writeable.() -> Unit) {
    updater(_snackbar)
  }

  fun updateBottomPanelState(updater: IBottomPanelGlobalState.Writeable.() -> Unit) {
    updater(_bottomPanel)
  }

}