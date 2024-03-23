package com.github.k1rakishou.chan.features.toolbar_v2

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.unit.Dp
import com.github.k1rakishou.chan.controller.ControllerKey
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarStateKind
import com.github.k1rakishou.chan.features.toolbar_v2.state.catalog.KurobaCatalogToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.catalog.KurobaCatalogToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.container.KurobaContainerToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.container.KurobaContainerToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.default.KurobaDefaultToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.default.KurobaDefaultToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.reply.KurobaReplyToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.reply.KurobaReplyToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.search.KurobaSearchToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.search.KurobaSearchToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.selection.KurobaSelectionToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.selection.KurobaSelectionToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.thread.KurobaThreadToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.thread.KurobaThreadToolbarState
import com.github.k1rakishou.chan.ui.globalstate.GlobalUiStateHolder
import com.github.k1rakishou.core_logger.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

@Stable
class KurobaToolbarState(
  private val controllerKey: ControllerKey,
  private val globalUiStateHolder: GlobalUiStateHolder
) {
  private val _toolbarLayerEventFlow = MutableSharedFlow<KurobaToolbarStateEvent>(extraBufferCapacity = Channel.UNLIMITED)
  val toolbarLayerEventFlow: SharedFlow<KurobaToolbarStateEvent>
    get() = _toolbarLayerEventFlow.asSharedFlow()

  private val _toolbarStates = mutableStateListOf<IKurobaToolbarState>()
  val topToolbar: IKurobaToolbarState?
    get() = _toolbarStates.lastOrNull()

  val container by lazy(LazyThreadSafetyMode.NONE) { KurobaContainerToolbarState() }
  val catalog by lazy(LazyThreadSafetyMode.NONE) { KurobaCatalogToolbarState() }
  val thread by lazy(LazyThreadSafetyMode.NONE) { KurobaThreadToolbarState() }
  val default by lazy(LazyThreadSafetyMode.NONE) { KurobaDefaultToolbarState() }
  val search by lazy(LazyThreadSafetyMode.NONE) { KurobaSearchToolbarState() }
  val selection by lazy(LazyThreadSafetyMode.NONE) { KurobaSelectionToolbarState() }
  val reply by lazy(LazyThreadSafetyMode.NONE) { KurobaReplyToolbarState() }

  val toolbarVisibleState: Flow<Boolean>
    get() = globalUiStateHolder.toolbarState.toolbarVisibilityStateFlow()

  val toolbarHeightState: StateFlow<Dp?>
    get() = globalUiStateHolder.toolbarState.toolbarHeightStateFlow()
  val toolbarHeight: Dp?
    get() = toolbarHeightState.value

  val toolbarKey: String
    get() = "Toolbar_${controllerKey.key}"

  fun onToolbarHeightChanged(totalToolbarHeight: Dp) {
    globalUiStateHolder.updateToolbarState {
      updateToolbarHeightState(totalToolbarHeight)
    }
  }

  fun enterContainerMode() {
    enterToolbarMode(
      params = KurobaContainerToolbarParams(),
      state = container
    )
  }

  fun enterCatalogMode(
    leftItem: ToolbarMenuItem?,
    menuBuilder: (ToolbarMenuBuilder.() -> Unit)? = null,
    iconClickInterceptor: ((ToolbarMenuItem) -> Boolean)? = null
  ) {
    val toolbarMenuBuilder = ToolbarMenuBuilder()
    menuBuilder?.invoke(toolbarMenuBuilder)

    enterToolbarMode(
      params = KurobaCatalogToolbarParams(
        leftItem = leftItem,
        toolbarMenu = toolbarMenuBuilder.build(),
        iconClickInterceptor = iconClickInterceptor
      ),
      state = catalog
    )
  }

  fun enterThreadMode(
    leftItem: ToolbarMenuItem?,
    scrollableTitle: Boolean = false,
    menuBuilder: (ToolbarMenuBuilder.() -> Unit)? = null,
    iconClickInterceptor: ((ToolbarMenuItem) -> Boolean)? = null
  ) {
    val toolbarMenuBuilder = ToolbarMenuBuilder()
    menuBuilder?.invoke(toolbarMenuBuilder)

    enterToolbarMode(
      params = KurobaThreadToolbarParams(
        leftItem = leftItem,
        scrollableTitle = scrollableTitle,
        toolbarMenu = toolbarMenuBuilder.build(),
        iconClickInterceptor = iconClickInterceptor
      ),
      state = thread
    )
  }

  fun enterDefaultMode(
    leftItem: ToolbarMenuItem?,
    middleContent: ToolbarMiddleContent? = null,
    menuBuilder: (ToolbarMenuBuilder.() -> Unit)? = null,
    iconClickInterceptor: ((ToolbarMenuItem) -> Boolean)? = null
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
    if (topToolbar?.kind == ToolbarStateKind.Search) {
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
    if (topToolbar?.kind == ToolbarStateKind.Selection) {
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
    if (topToolbar?.kind == ToolbarStateKind.Reply) {
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
    globalUiStateHolder.updateToolbarState {
      updateToolbarVisibilityState(visible = true)
    }
  }

  fun onTransitionStart(other: KurobaToolbarState) {
    // TODO: New toolbar.
    updateFromState(other)
  }

  fun onTransitionProgress(progress: Float) {
    // TODO: New toolbar.
  }

  fun onTransitionFinished(other: KurobaToolbarState) {
    // TODO: New toolbar.
    updateFromState(other)
  }

  fun updateFromState(toolbarState: KurobaToolbarState) {
    Snapshot.withMutableSnapshot {
      _toolbarStates.clear()
      _toolbarStates.addAll(toolbarState._toolbarStates)

      container.updateFromState(toolbarState.container)
      default.updateFromState(toolbarState.default)
      search.updateFromState(toolbarState.search)
      selection.updateFromState(toolbarState.selection)
      reply.updateFromState(toolbarState.reply)
    }
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
    Logger.debug(TAG) { "Toolbar '${toolbarKey}' entering state ${state.kind}" }

    val indexOfState = _toolbarStates.indexOfFirst { prevLayer -> prevLayer.kind == params.kind }
    if (indexOfState >= 0) {
      val prevToolbarLayer = _toolbarStates[indexOfState]
      Snapshot.withMutableSnapshot { prevToolbarLayer.update(params) }

      _toolbarLayerEventFlow.tryEmit(KurobaToolbarStateEvent.Updated(prevToolbarLayer.kind))
    } else {
      Snapshot.withMutableSnapshot { state.update(params) }

      _toolbarStates += state
      state.onPushed()

      _toolbarLayerEventFlow.tryEmit(KurobaToolbarStateEvent.Pushed(state.kind))
    }
  }

  fun exitToolbarMode(kind: ToolbarStateKind) {
    Logger.debug(TAG) { "Toolbar '${toolbarKey}' exiting state ${kind}" }

    if (topToolbar?.kind == kind) {
      pop()
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as KurobaToolbarState

    return controllerKey == other.controllerKey
  }

  override fun hashCode(): Int {
    return controllerKey.hashCode()
  }

  override fun toString(): String {
    return "KurobaToolbarState(controllerKey=$controllerKey)"
  }

  companion object {
    private const val TAG = "KurobaToolbarState"
  }

}
