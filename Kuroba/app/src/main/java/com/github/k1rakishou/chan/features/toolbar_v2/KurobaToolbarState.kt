package com.github.k1rakishou.chan.features.toolbar_v2

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.unit.Dp
import com.github.k1rakishou.chan.controller.ControllerKey
import com.github.k1rakishou.chan.controller.transition.TransitionMode
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.KurobaToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarStateKind
import com.github.k1rakishou.chan.features.toolbar_v2.state.catalog.KurobaCatalogToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.catalog.KurobaCatalogToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.container.KurobaContainerToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.container.KurobaContainerToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.default.KurobaDefaultToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.default.KurobaDefaultToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.reply.KurobaReplyToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.reply.KurobaReplyToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.search.KurobaSearchToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.search.KurobaSearchToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.selection.KurobaSelectionToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.selection.KurobaSelectionToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.thread.KurobaThreadToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.thread.KurobaThreadToolbarSubState
import com.github.k1rakishou.chan.ui.globalstate.GlobalUiStateHolder
import com.github.k1rakishou.common.quantize
import com.github.k1rakishou.core_logger.Logger
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Stable
class KurobaToolbarState(
  private val controllerKey: ControllerKey,
  private val globalUiStateHolder: GlobalUiStateHolder
) {
  private var _destroying = false

  private val _toolbarStateList = mutableStateOf<PersistentList<KurobaToolbarSubState>>(persistentListOf())
  val toolbarStateList: State<ImmutableList<KurobaToolbarSubState>>
    get() = _toolbarStateList

  private val _toolbarList: PersistentList<KurobaToolbarSubState>
    get() = _toolbarStateList.value

  val topToolbar: KurobaToolbarSubState?
    get() = _toolbarStateList.value.lastOrNull()

  private val _transitionToolbarState = mutableStateOf<KurobaToolbarTransition?>(null)
  val transitionToolbarState: State<KurobaToolbarTransition?>
    get() = _transitionToolbarState

  private val _container by lazy(LazyThreadSafetyMode.NONE) { mutableStateOf(KurobaContainerToolbarSubState()) }
  val container: KurobaContainerToolbarSubState
    get() = _container.value
  private val _catalog by lazy(LazyThreadSafetyMode.NONE) { mutableStateOf(KurobaCatalogToolbarSubState()) }
  val catalog: KurobaCatalogToolbarSubState
    get() = _catalog.value
  private val _thread by lazy(LazyThreadSafetyMode.NONE) { mutableStateOf(KurobaThreadToolbarSubState()) }
  val thread: KurobaThreadToolbarSubState
    get() = _thread.value
  private val _default by lazy(LazyThreadSafetyMode.NONE) { mutableStateOf(KurobaDefaultToolbarSubState()) }
  val default: KurobaDefaultToolbarSubState
    get() = _default.value
  private val _search by lazy(LazyThreadSafetyMode.NONE) { mutableStateOf(KurobaSearchToolbarSubState()) }
  val search: KurobaSearchToolbarSubState
    get() = _search.value
  private val _selection by lazy(LazyThreadSafetyMode.NONE) { mutableStateOf(KurobaSelectionToolbarSubState()) }
  val selection: KurobaSelectionToolbarSubState
    get() = _selection.value
  private val _reply by lazy(LazyThreadSafetyMode.NONE) { mutableStateOf(KurobaReplyToolbarSubState()) }
  val reply: KurobaReplyToolbarSubState
    get() = _reply.value

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

  fun showToolbar() {
    globalUiStateHolder.updateToolbarState {
      updateToolbarVisibilityState(visible = true)
    }
  }

  fun onTransitionProgressStart(
    other: KurobaToolbarState,
    transitionMode: TransitionMode
  ) {
    when (val transition = _transitionToolbarState.value) {
      is KurobaToolbarTransition.Instant -> {
        // End current transition animation
        onKurobaToolbarTransitionInstantFinished(transition)
      }
      is KurobaToolbarTransition.Progress -> {
        error("Attempt to perform more than one transition at the same time!\n" +
          "current: ${transitionToolbarState.value}\n" +
          "new: ${other} with transitionMode: ${transitionMode}")
      }
      null -> {
        // no-op
      }
    }

    val topToolbar = checkNotNull(other.topToolbar) {
      "Attempt to perform a transition with a non-initialized toolbar! toolbar: ${other}"
    }

    _transitionToolbarState.value = KurobaToolbarTransition.Progress(
      transitionToolbarState = topToolbar,
      transitionMode = transitionMode,
      progress = -1f
    )
  }

  fun onTransitionProgress(progress: Float) {
    val transitionState = _transitionToolbarState.value
      ?: return

    check(transitionState is KurobaToolbarTransition.Progress) {
      "Expected transitionState to be Progress but got ${transitionState}"
    }

    val quantizedProgress = progress.quantize(precision = 0.033f)
    if (quantizedProgress == transitionState.progress) {
      return
    }

    _transitionToolbarState.value = transitionState.copy(progress = quantizedProgress)
  }

  fun onTransitionProgressFinished() {
    val transitionState = _transitionToolbarState.value

    if (transitionState != null) {
      check(transitionState is KurobaToolbarTransition.Progress) {
        "Expected transitionState to be Progress but got ${transitionState}"
      }
    }

    _transitionToolbarState.value = null
  }

  fun enterContainerMode() {
    enterToolbarMode(
      params = KurobaContainerToolbarParams(),
      state = _container.value,
      withAnimation = false
    )
  }

  fun enterCatalogMode(
    leftItem: ToolbarMenuItem?,
    onMainContentClick: () -> Unit,
    menuBuilder: (ToolbarMenuBuilder.() -> Unit)? = null,
    iconClickInterceptor: ((ToolbarMenuItem) -> Boolean)? = null
  ) {
    val toolbarMenuBuilder = ToolbarMenuBuilder()
    menuBuilder?.invoke(toolbarMenuBuilder)

    enterToolbarMode(
      params = KurobaCatalogToolbarParams(
        leftItem = leftItem,
        toolbarMenu = toolbarMenuBuilder.build(),
        onMainContentClick = onMainContentClick,
        iconClickInterceptor = iconClickInterceptor
      ),
      state = _catalog.value,
      withAnimation = false
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
      state = _thread.value,
      withAnimation = false
    )
  }

  fun enterDefaultMode(
    leftItem: ToolbarMenuItem?,
    scrollableTitle: Boolean = false,
    withAnimation: Boolean = true,
    middleContent: ToolbarMiddleContent? = null,
    menuBuilder: (ToolbarMenuBuilder.() -> Unit)? = null,
    iconClickInterceptor: ((ToolbarMenuItem) -> Boolean)? = null
  ) {
    val toolbarMenuBuilder = ToolbarMenuBuilder()
    menuBuilder?.invoke(toolbarMenuBuilder)

    enterToolbarMode(
      params = KurobaDefaultToolbarParams(
        leftItem = leftItem,
        scrollableTitle = scrollableTitle,
        middleContent = middleContent,
        toolbarMenu = toolbarMenuBuilder.build(),
        iconClickInterceptor = iconClickInterceptor
      ),
      state = _default.value,
      withAnimation = withAnimation
    )
  }

  fun enterSearchMode(
    withAnimation: Boolean = true,
    initialSearchQuery: String? = null,
    menuBuilder: (ToolbarMenuBuilder.() -> Unit)? = null,
  ) {
    val toolbarMenuBuilder = ToolbarMenuBuilder()
    menuBuilder?.invoke(toolbarMenuBuilder)

    enterToolbarMode(
      params = KurobaSearchToolbarParams(
        initialSearchQuery = initialSearchQuery,
        toolbarMenu = toolbarMenuBuilder.build(),
      ),
      state = _search.value,
      withAnimation = withAnimation
    )
  }

  fun isInSearchMode(): Boolean {
    return topToolbar?.kind == ToolbarStateKind.Search
  }

  fun enterSelectionMode(
    leftItem: ToolbarMenuItem?,
    withAnimation: Boolean = true,
    title: ToolbarText? = null,
    menuBuilder: (ToolbarMenuBuilder.() -> Unit)? = null,
  ) {
    val toolbarMenuBuilder = ToolbarMenuBuilder()
    menuBuilder?.invoke(toolbarMenuBuilder)

    enterToolbarMode(
      params = KurobaSelectionToolbarParams(
        leftItem = leftItem,
        title = title,
        toolbarMenu = toolbarMenuBuilder.build(),
      ),
      state = _selection.value,
      withAnimation = withAnimation
    )
  }

  fun isInSelectionMode(): Boolean {
    return topToolbar?.kind == ToolbarStateKind.Selection
  }

  fun enterReplyMode(
    chanDescriptor: ChanDescriptor,
    withAnimation: Boolean = true,
    leftItem: ToolbarMenuItem? = null,
    menuBuilder: (ToolbarMenuBuilder.() -> Unit)? = null,
  ) {
    val toolbarMenuBuilder = ToolbarMenuBuilder()
    menuBuilder?.invoke(toolbarMenuBuilder)

    enterToolbarMode(
      params = KurobaReplyToolbarParams(
        leftItem = leftItem,
        chanDescriptor = chanDescriptor,
        toolbarMenu = toolbarMenuBuilder.build(),
      ),
      state = _reply.value,
      withAnimation = withAnimation
    )
  }

  fun isInReplyMode(): Boolean {
    return topToolbar?.kind == ToolbarStateKind.Reply
  }

  fun pop(withAnimation: Boolean = true): Boolean {
    if (_toolbarList.size <= 1) {
      return false
    }

    if (!_destroying && _transitionToolbarState.value != null) {
      return false
    }

    val topToolbar = _toolbarList.lastOrNull()
    if (topToolbar == null) {
      return false
    }

    Logger.debug(TAG) { "Toolbar '${toolbarKey}' exiting state ${topToolbar.kind}, withAnimation: ${withAnimation}" }

    if (!withAnimation) {
      _toolbarStateList.value = _toolbarList.removeAt(_toolbarList.lastIndex)
      topToolbar.onPopped()
    } else {
      val belowTop = _toolbarList.getOrNull(_toolbarList.lastIndex - 1)
      if (belowTop == null) {
        return false
      }

      _transitionToolbarState.value = KurobaToolbarTransition.Instant(
        transitionMode = TransitionMode.Out,
        transitionToolbarState = belowTop
      )
    }

    return true
  }

  fun popAll() {
    _destroying = true
    Logger.debug(TAG) { "Toolbar '${toolbarKey}' is being destroyed" }

    while (_toolbarList.size > 1) {
      if (!pop(withAnimation = false)) {
        break
      }
    }
  }

  fun findItem(id: Int): ToolbarMenuItem? {
    for (toolbarState in _toolbarList) {
      val toolbarMenuItem = toolbarState.findItem(id)
      if (toolbarMenuItem?.id == id) {
        return toolbarMenuItem
      }
    }

    return null
  }

  fun findOverflowItem(id: Int): ToolbarMenuOverflowItem? {
    for (toolbarState in _toolbarList) {
      val toolbarOverflowMenuItem = toolbarState.findOverflowItem(id)
      if (toolbarOverflowMenuItem?.id == id) {
        return toolbarOverflowMenuItem
      }
    }

    return null
  }

  fun findCheckableOverflowItem(id: Int): ToolbarMenuCheckableOverflowItem? {
    for (toolbarState in _toolbarList) {
      val toolbarCheckableOverflowMenuItem = toolbarState.findCheckableOverflowItem(id)
      if (toolbarCheckableOverflowMenuItem?.id == id) {
        return toolbarCheckableOverflowMenuItem
      }
    }

    return null
  }

  fun checkOrUncheckItem(subItem: ToolbarMenuCheckableOverflowItem, check: Boolean) {
    for (toolbarState in _toolbarList) {
      toolbarState.checkOrUncheckItem(subItem, check)
    }
  }

  fun hideBadge() {
    catalog.hideBadge()
    thread.hideBadge()
    default.hideBadge()
  }

  fun updateBadge(count: Int, highImportance: Boolean) {
    // For now we only display badge on catalog/thread toolbar. We might display it on default toolbars
    // in the near future. For the other toolbars there is no need for now.
    catalog.updateBadge(count, highImportance)
    thread.updateBadge(count, highImportance)
    default.updateBadge(count, highImportance)
  }

  fun onKurobaToolbarTransitionInstantFinished(instant: KurobaToolbarTransition.Instant) {
    if (_transitionToolbarState.value == null) {
      // Already canceled by someone else
      return
    }

    if (_transitionToolbarState.value is KurobaToolbarTransition.Progress) {
      error("Attempt to cancel Progress transition with Instant animation. " +
        "Progress transition takes higher priority so it can't be canceled like this.")
    }

    when (instant.transitionMode) {
      TransitionMode.In -> {
        _toolbarStateList.value = _toolbarList.add(instant.transitionToolbarState)
        instant.transitionToolbarState.onPushed()
      }
      TransitionMode.Out -> {
        val topToolbar = _toolbarList.lastOrNull()
        if (topToolbar != null) {
          _toolbarStateList.value = _toolbarList.removeAt(_toolbarList.lastIndex)
          topToolbar.onPopped()
        }
      }
    }

    _transitionToolbarState.value = null
  }

  private fun enterToolbarMode(
    params: IKurobaToolbarParams,
    state: KurobaToolbarSubState,
    withAnimation: Boolean
  ) {
    Logger.debug(TAG) { "Toolbar '${toolbarKey}' entering state ${state.kind}, withAnimation: ${withAnimation}" }

    val indexOfState = _toolbarList.indexOfFirst { prevLayer -> prevLayer.kind == params.kind }
    if (indexOfState >= 0) {
      val prevToolbarLayer = _toolbarList[indexOfState]
      Snapshot.withMutableSnapshot { prevToolbarLayer.update(params) }
    } else {
      Snapshot.withMutableSnapshot { state.update(params) }

      if (topToolbar == null || !withAnimation) {
        _toolbarStateList.value = _toolbarList.add(state)
        state.onPushed()
      } else {
        _transitionToolbarState.value = KurobaToolbarTransition.Instant(
          transitionMode = TransitionMode.In,
          transitionToolbarState = state
        )
      }
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
    return "KurobaToolbarState(controllerKey: $controllerKey)"
  }

  companion object {
    private const val TAG = "KurobaToolbarState"
  }

}