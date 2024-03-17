package com.github.k1rakishou.chan.features.toolbar_v2

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarStateKind
import com.github.k1rakishou.chan.features.toolbar_v2.state.default.KurobaDefaultToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.default.KurobaDefaultToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.reply.KurobaReplyToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.reply.KurobaReplyToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.search.KurobaSearchToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.search.KurobaSearchToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.selection.KurobaSelectionToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.selection.KurobaSelectionToolbarState
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.isDevBuild
import com.github.k1rakishou.core_logger.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Stable
class KurobaToolbarState {
  private val _toolbarLayerEventFlow = MutableSharedFlow<KurobaToolbarStateEvent>(extraBufferCapacity = Channel.UNLIMITED)
  val toolbarLayerEventFlow: SharedFlow<KurobaToolbarStateEvent>
    get() = _toolbarLayerEventFlow.asSharedFlow()

  private val _toolbarStates = mutableStateListOf<IKurobaToolbarState>()
  val topToolbar: IKurobaToolbarState
    get() = requireNotNull(_toolbarStates.lastOrNull()) { "Toolbar has not states!" }

  val default by lazy(LazyThreadSafetyMode.NONE) { KurobaDefaultToolbarState() }
  val search by lazy(LazyThreadSafetyMode.NONE) { KurobaSearchToolbarState() }
  val selection by lazy(LazyThreadSafetyMode.NONE) { KurobaSelectionToolbarState() }
  val reply by lazy(LazyThreadSafetyMode.NONE) { KurobaReplyToolbarState() }

  private val _enabledState = mutableStateOf(false)
  val enabledState: State<Boolean>
    get() = _enabledState
  val enabled: Boolean
    get() = _enabledState.value

  private val _toolbarHeightState = mutableStateOf<Dp?>(null)
  val toolbarHeightState: State<Dp?>
    get() = _toolbarHeightState
  val toolbarHeight: Dp?
    get() = _toolbarHeightState.value

  fun onToolbarHeightChanged(totalToolbarHeight: Dp) {
    if (totalToolbarHeight.isSpecified && totalToolbarHeight > 0.dp) {
      _toolbarHeightState.value = totalToolbarHeight
    } else {
      _toolbarHeightState.value = null
    }
  }

  fun enable() {
    _enabledState.value = true
  }

  fun disable() {
    _enabledState.value = false
  }

  fun enterDefaultMode(
    leftItem: ToolbarMenuItem?,
    middleContent: ToolbarMiddleContent? = null,
    menuBuilder: (ToolbarMenuBuilder.() -> Unit)? = null,
    iconClickInterceptor: (ToolbarMenuItem) -> Boolean = { false }
  ) {
    val toolbarMenuBuilder = ToolbarMenuBuilder()
    menuBuilder?.invoke(toolbarMenuBuilder)

    enterToolbarMode(
      params = KurobaDefaultToolbarParams(
        leftItem = leftItem,
        middleContent = middleContent,
        toolbarMenu = toolbarMenuBuilder.build(),
        iconClickInterceptor = iconClickInterceptor
      ),
      state = default
    )
  }

  fun isInDefaultMode(): Boolean {
    return _toolbarStates.lastOrNull()?.kind == ToolbarStateKind.Default
  }

  fun exitDefaultMode() {
    exitToolbarMode(ToolbarStateKind.Default)
  }

  fun enterSearchMode(
    menuBuilder: (ToolbarMenuBuilder.() -> Unit)? = null,
  ) {
    val toolbarMenuBuilder = ToolbarMenuBuilder()
    menuBuilder?.invoke(toolbarMenuBuilder)

    enterToolbarMode(
      params = KurobaSearchToolbarParams(
        toolbarMenu = toolbarMenuBuilder.build(),
      ),
      state = search
    )
  }

  fun isInSearchMode(): Boolean {
    return _toolbarStates.lastOrNull()?.kind == ToolbarStateKind.Search
  }

  fun exitSearchMode() {
    if (topToolbar.kind == ToolbarStateKind.Search) {
      pop()
    }
  }

  fun enterSelectionMode(
    title: ToolbarText? = null,
    menuBuilder: (ToolbarMenuBuilder.() -> Unit)? = null,
  ) {
    val toolbarMenuBuilder = ToolbarMenuBuilder()
    menuBuilder?.invoke(toolbarMenuBuilder)

    enterToolbarMode(
      params = KurobaSelectionToolbarParams(
        title = title,
        toolbarMenu = toolbarMenuBuilder.build(),
      ),
      state = selection
    )
  }

  fun isInSelectionMode(): Boolean {
    return _toolbarStates.lastOrNull()?.kind == ToolbarStateKind.Selection
  }

  fun exitSelectionMode() {
    if (topToolbar.kind == ToolbarStateKind.Selection) {
      pop()
    }
  }

  fun enterReplyMode(
    menuBuilder: (ToolbarMenuBuilder.() -> Unit)? = null,
  ) {
    val toolbarMenuBuilder = ToolbarMenuBuilder()
    menuBuilder?.invoke(toolbarMenuBuilder)

    enterToolbarMode(
      params = KurobaReplyToolbarParams(
        toolbarMenu = toolbarMenuBuilder.build(),
      ),
      state = reply
    )
  }

  fun isInReplyMode(): Boolean {
    return _toolbarStates.lastOrNull()?.kind == ToolbarStateKind.Reply
  }

  fun exitReplyMode() {
    if (topToolbar.kind == ToolbarStateKind.Reply) {
      pop()
    }
  }

  fun pop(): Boolean {
    if (_toolbarStates.size <= 1) {
      return false
    }

    val topToolbar = _toolbarStates.removeLastOrNull()
    if (topToolbar != null) {
      topToolbar.onPopped()
      _toolbarLayerEventFlow.tryEmit(KurobaToolbarStateEvent.Popped(topToolbar.kind))
    }

    return true
  }

  fun popAll() {
    while (_toolbarStates.size > 1) {
      pop()
    }
  }

  fun onBack(): Boolean {
    return pop()
  }

  fun showToolbar() {
    TODO("TODO: New toolbar")
  }

  fun onTransitionStart(other: KurobaToolbarState) {
    TODO("TODO: New toolbar")
  }

  fun onTransitionProgress(progress: Float) {
    TODO("TODO: New toolbar")
  }

  fun onTransitionFinished(finish: Boolean) {
    TODO("TODO: New toolbar")
  }

  fun updateFromState(childControllerToolbarState: KurobaToolbarState) {
    TODO("TODO: New toolbar")
  }

  fun findItem(id: Int): ToolbarMenuItem? {
    for (toolbarState in _toolbarStates) {
      val toolbarMenuItem = toolbarState.findItem(id)
      if (toolbarMenuItem?.id == id) {
        return toolbarMenuItem
      }
    }

    return null
  }

  fun findOverflowItem(id: Int): ToolbarMenuOverflowItem? {
    for (toolbarState in _toolbarStates) {
      val toolbarOverflowMenuItem = toolbarState.findOverflowItem(id)
      if (toolbarOverflowMenuItem?.id == id) {
        return toolbarOverflowMenuItem
      }
    }

    return null
  }

  fun findCheckableOverflowItem(id: Int): ToolbarMenuCheckableOverflowItem? {
    for (toolbarState in _toolbarStates) {
      val toolbarCheckableOverflowMenuItem = toolbarState.findCheckableOverflowItem(id)
      if (toolbarCheckableOverflowMenuItem?.id == id) {
        return toolbarCheckableOverflowMenuItem
      }
    }

    return null
  }

  fun checkOrUncheckItem(subItem: ToolbarMenuCheckableOverflowItem, check: Boolean) {
    for (toolbarState in _toolbarStates) {
      toolbarState.checkOrUncheckItem(subItem, check)
    }
  }

  fun updateBadge(count: Int, highImportance: Boolean) {
    // TODO: New toolbar.
//    if (ChanSettings.isSplitLayoutMode() || ChanSettings.bottomNavigationViewEnabled.get()) {
//      badgeText = null
//      return
//    }
//    val text = if (count == 0) null else getShortUnreadCount(count)
//    if (badgeHighImportance != highImportance || !TextUtils.equals(text, badgeText)) {
//      badgeText = text
//      badgeHighImportance = highImportance
//      invalidateSelf()
//    }
  }

  private fun enterToolbarMode(
    params: IKurobaToolbarParams,
    state: IKurobaToolbarState
  ) {
    val indexOfState = _toolbarStates.indexOfFirst { prevLayer -> prevLayer.kind == params.kind }
    if (indexOfState != _toolbarStates.lastIndex) {
      if (isDevBuild()) {
        error("Can only update top toolbar. indexOfState: ${indexOfState}, lastIndex: ${_toolbarStates.lastIndex}")
      } else {
        Logger.error(TAG) {
          "Can only update top toolbar. indexOfState: ${indexOfState}, lastIndex: ${_toolbarStates.lastIndex}"
        }
      }

      return
    }

    if (indexOfState >= 0) {
      val prevToolbarLayer = _toolbarStates[indexOfState]
      prevToolbarLayer.update(params)

      _toolbarLayerEventFlow.tryEmit(KurobaToolbarStateEvent.Updated(prevToolbarLayer.kind))
    } else {
      state.update(params)

      _toolbarStates += state
      state.onPushed()

      _toolbarLayerEventFlow.tryEmit(KurobaToolbarStateEvent.Pushed(state.kind))
    }
  }

  fun exitToolbarMode(kind: ToolbarStateKind) {
    if (topToolbar.kind == kind) {
      pop()
    }
  }

  companion object {
    private const val TAG = "KurobaToolbarState"
  }

}
