package com.github.k1rakishou.chan.features.toolbar_v2

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.KurobaDefaultToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.reply.KurobaReplyToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.search.KurobaSearchToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.search.KurobaToolbarSearchContent
import com.github.k1rakishou.chan.features.toolbar_v2.state.selection.KurobaSelectionToolbarState
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.isDevBuild
import com.github.k1rakishou.core_logger.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Stable
class KurobaToolbarState {
  private val _toolbarLayerEventFlow = MutableSharedFlow<KurobaToolbarLayerEvent>(extraBufferCapacity = Channel.UNLIMITED)
  val toolbarLayerEventFlow: SharedFlow<KurobaToolbarLayerEvent>
    get() = _toolbarLayerEventFlow.asSharedFlow()

  private val _layers = mutableStateListOf<KurobaToolbarLayer>()
  val topLayer: KurobaToolbarLayer
    get() = requireNotNull(_layers.lastOrNull()) { "Toolbar has not layers!" }

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

  private val _deprecatedNavigationFlags = mutableStateOf<DeprecatedNavigationFlags?>(null)
  val hasDrawer: Boolean
    get() = _deprecatedNavigationFlags.value?.hasDrawer == true
  val hasBack: Boolean
    get() = _deprecatedNavigationFlags.value?.hasBack == true
  val swipeable: Boolean
    get() = _deprecatedNavigationFlags.value?.swipeable == true

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

  fun pushOrUpdateDefaultLayer(
    navigationFlags: DeprecatedNavigationFlags = DeprecatedNavigationFlags(),
    leftItem: ToolbarMenuItem?,
    middleContent: ToolbarMiddleContent? = null,
    menuBuilder: ToolbarMenuBuilder.() -> Unit = { },
    iconClickInterceptor: (ToolbarMenuItem) -> Boolean = { false }
  ) {
    pushOrUpdateLayer(
      toolbarLayerId = ToolbarLayerId.Default,
      kurobaToolbarState = KurobaDefaultToolbarState(),
      leftItem = leftItem,
      middleContent = middleContent,
      menuBuilder = menuBuilder,
      iconClickInterceptor = iconClickInterceptor
    )

    _deprecatedNavigationFlags.value = navigationFlags
  }

  fun pushOrUpdateLayer(
    toolbarLayerId: ToolbarLayerId,
    kurobaToolbarState: IKurobaToolbarState,
    leftItem: ToolbarMenuItem?,
    middleContent: ToolbarMiddleContent? = null,
    menuBuilder: ToolbarMenuBuilder.() -> Unit = { },
    iconClickInterceptor: (ToolbarMenuItem) -> Boolean = { false }
  ) {
    val toolbarMenuBuilder = ToolbarMenuBuilder()
    with(toolbarMenuBuilder) { menuBuilder() }

    val toolbarLayer = KurobaToolbarLayer(
      toolbarLayerId = toolbarLayerId,
      kurobaToolbarState = kurobaToolbarState,
      leftIcon = leftItem,
      middleContent = middleContent,
      toolbarMenu = toolbarMenuBuilder.build(),
      iconClickInterceptor = iconClickInterceptor
    )

    val indexOfLayer = _layers.indexOfFirst { prevLayer -> prevLayer.toolbarLayerId == toolbarLayer.toolbarLayerId }
    if (indexOfLayer != _layers.lastIndex) {
      if (isDevBuild()) {
        error("Can only update top layer. indexOfLayer: ${indexOfLayer}, lastIndex: ${_layers.lastIndex}")
      } else {
        Logger.error(TAG) { "Can only update top layer. indexOfLayer: ${indexOfLayer}, lastIndex: ${_layers.lastIndex}" }
      }

      return
    }

    if (indexOfLayer >= 0) {
      _layers[indexOfLayer] = toolbarLayer
      _toolbarLayerEventFlow.tryEmit(KurobaToolbarLayerEvent.Replaced(toolbarLayer))
    } else {
      _layers += toolbarLayer
      _toolbarLayerEventFlow.tryEmit(KurobaToolbarLayerEvent.Pushed(toolbarLayer))
      toolbarLayer.kurobaToolbarState.onLayerPushed()
    }

  }

  fun popLayer(): Boolean {
    if (_layers.size <= 1) {
      return false
    }

    val lastLayer = _layers.removeLastOrNull()
    if (lastLayer != null) {
      lastLayer.kurobaToolbarState.onLayerPopped()
      _toolbarLayerEventFlow.tryEmit(KurobaToolbarLayerEvent.Popped(lastLayer))
    }

    return true
  }

  fun popAllLayers() {
    while (_layers.size > 1) {
      popLayer()
    }
  }

  fun onBack(): Boolean {
    return popLayer()
  }

  fun showToolbar() {

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

  fun enterSearchMode(searchState: KurobaSearchToolbarState) {
    pushOrUpdateLayer(
      toolbarLayerId = ToolbarLayerId.Search,
      kurobaToolbarState = searchState,
      leftItem = BackArrowMenuItem(
        onClick = {
          // TODO: New toolbar
        }
      ),
      middleContent = ToolbarMiddleContent.Custom(
        content = { KurobaToolbarSearchContent(searchState) }
      )
    )
  }

  fun isInSearchMode(): Boolean {
    return _layers.lastOrNull()?.toolbarLayerId == ToolbarLayerId.Search
  }

  fun exitSearchMode() {
    if (topLayer.toolbarLayerId == ToolbarLayerId.Search) {
      popLayer()
    }
  }

  fun enterSelectionMode(selectionState: KurobaSelectionToolbarState) {
    pushOrUpdateLayer(
      toolbarLayerId = ToolbarLayerId.Selection,
      kurobaToolbarState = selectionState,
      leftItem = BackArrowMenuItem(
        onClick = {
          // TODO: New toolbar
        }
      ),
      middleContent = ToolbarMiddleContent.Custom(
        content = {
          // TODO: New toolbar
        }
      )
    )
  }

  fun isInSelectionMode(): Boolean {
    return _layers.lastOrNull()?.toolbarLayerId == ToolbarLayerId.Selection
  }

  fun exitSelectionMode() {
    if (topLayer.toolbarLayerId == ToolbarLayerId.Selection) {
      popLayer()
    }
  }

  fun enterReplyMode(replyState: KurobaReplyToolbarState) {
    TODO("Not yet implemented")
  }

  fun isInReplyMode(): Boolean {
    TODO("Not yet implemented")
  }

  fun exitReplyMode() {
    TODO("Not yet implemented")
  }

  fun updateToolbarLayer(
    toolbarLayerId: ToolbarLayerId,
    updater: (KurobaToolbarLayer) -> KurobaToolbarLayer?
  ) {
    val indexOfLayer = _layers.indexOfFirst { kurobaToolbarLayer -> kurobaToolbarLayer.toolbarLayerId == toolbarLayerId }
    if (indexOfLayer < 0) {
      return
    }

    val oldLayer = _layers[indexOfLayer]
    val newLayer = updater(oldLayer)

    if (newLayer == null || oldLayer == newLayer) {
      return
    }

    _layers[indexOfLayer] = newLayer
  }

  fun updateTitle(
    toolbarLayerId: ToolbarLayerId = topLayer.toolbarLayerId,
    newTitle: ToolbarText
  ) {
    updateToolbarLayer(toolbarLayerId) { oldLayer ->
      when (oldLayer.middleContent) {
        is ToolbarMiddleContent.Custom -> null
        is ToolbarMiddleContent.Title -> {
          oldLayer.copy(middleContent = oldLayer.middleContent.copy(title = newTitle))
        }
        null -> null
      }
    }
  }

  fun updateSubtitle(
    toolbarLayerId: ToolbarLayerId = topLayer.toolbarLayerId,
    newSubTitle: ToolbarText
  ) {
    updateToolbarLayer(toolbarLayerId) { oldLayer ->
      when (oldLayer.middleContent) {
        is ToolbarMiddleContent.Custom -> null
        is ToolbarMiddleContent.Title -> {
          oldLayer.copy(middleContent = oldLayer.middleContent.copy(subtitle = newSubTitle))
        }
        null -> null
      }
    }
  }

  fun findItem(id: Int): ToolbarMenuItem? {
    for (toolbarLayer in _layers) {
      if (toolbarLayer.leftIcon?.id == id) {
        return toolbarLayer.leftIcon
      }

      for (menuItem in toolbarLayer.toolbarMenu.menuItems) {
        if (menuItem.id == id) {
          return menuItem
        }
      }
    }

    return null
  }

  fun findOverflowItem(id: Int): ToolbarMenuOverflowItem? {
    for (toolbarLayer in _layers) {
      val overflowMenuItems = mutableListOf<ToolbarMenuOverflowItem>()
      overflowMenuItems += toolbarLayer.toolbarMenu.overflowMenuItems
        .filterIsInstance<ToolbarMenuOverflowItem>()

      for (overflowMenuItem in overflowMenuItems) {
        if (overflowMenuItem.id == id) {
          return overflowMenuItem
        }

        overflowMenuItems += overflowMenuItem.subItems
          .filterIsInstance<ToolbarMenuOverflowItem>()
      }
    }

    return null
  }

  fun findCheckableOverflowItem(id: Int): ToolbarMenuCheckableOverflowItem? {
    for (toolbarLayer in _layers) {
      val overflowMenuItems = mutableListOf<ToolbarMenuCheckableOverflowItem>()
      overflowMenuItems += toolbarLayer.toolbarMenu.overflowMenuItems
        .filterIsInstance<ToolbarMenuCheckableOverflowItem>()

      for (overflowMenuItem in overflowMenuItems) {
        if (overflowMenuItem.id == id) {
          return overflowMenuItem
        }

        overflowMenuItems += overflowMenuItem.subItems
          .filterIsInstance<ToolbarMenuCheckableOverflowItem>()
      }
    }

    return null
  }

  fun checkOrUncheckItem(subItem: ToolbarMenuCheckableOverflowItem, check: Boolean) {
    for (toolbarLayer in _layers) {
      val overflowMenuItems = mutableListOf<ToolbarMenuCheckableOverflowItem>()
      overflowMenuItems += toolbarLayer.toolbarMenu.overflowMenuItems
        .filterIsInstance<ToolbarMenuCheckableOverflowItem>()

      val groupId = subItem.groupId

      for (overflowMenuItem in overflowMenuItems) {
        if (overflowMenuItem.id == subItem.id) {
          overflowMenuItem.updateChecked(check)
        } else if (groupId != null && overflowMenuItem.groupId == groupId) {
          overflowMenuItem.updateChecked(false)
        }

        overflowMenuItems += overflowMenuItem.subItems
          .filterIsInstance<ToolbarMenuCheckableOverflowItem>()
      }
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

  enum class ToolbarLayerId {
    Default,
    Search,
    Selection;

    fun isSearch(): Boolean = this == Search
  }

  companion object {
    private const val TAG = "KurobaToolbarState"
  }

}
