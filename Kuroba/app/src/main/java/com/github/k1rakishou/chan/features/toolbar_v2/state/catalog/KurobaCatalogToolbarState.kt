package com.github.k1rakishou.chan.features.toolbar_v2.state.catalog

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenu
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarText
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarStateKind

@Immutable
data class KurobaCatalogToolbarParams(
  val leftItem: ToolbarMenuItem? = null,
  val title: ToolbarText? = null,
  val subtitle: ToolbarText? = null,
  val toolbarMenu: ToolbarMenu? = null,
  val onMainContentClick: (() -> Unit)? = null,
  val iconClickInterceptor: ((ToolbarMenuItem) -> Boolean)? = null
) : IKurobaToolbarParams {
  override val kind: ToolbarStateKind = ToolbarStateKind.Catalog
}

class KurobaCatalogToolbarState(
  params: KurobaCatalogToolbarParams = KurobaCatalogToolbarParams()
) : IKurobaToolbarState {
  private val _leftItem = mutableStateOf<ToolbarMenuItem?>(params.leftItem)
  val leftItem: State<ToolbarMenuItem?>
    get() = _leftItem

  private val _title = mutableStateOf<ToolbarText?>(params.title)
  val title: State<ToolbarText?>
    get() = _title

  private val _subtitle = mutableStateOf<ToolbarText?>(params.subtitle)
  val subtitle: State<ToolbarText?>
    get() = _subtitle

  private val _toolbarMenu = mutableStateOf<ToolbarMenu?>(params.toolbarMenu)
  val toolbarMenu: State<ToolbarMenu?>
    get() = _toolbarMenu

  private var _onMainContentClick: (() -> Unit)? = params.onMainContentClick
  val onMainContentClick: (() -> Unit)?
    get() = _onMainContentClick

  private var _iconClickInterceptor: ((ToolbarMenuItem) -> Boolean)? = params.iconClickInterceptor
  val iconClickInterceptor: ((ToolbarMenuItem) -> Boolean)?
    get() = _iconClickInterceptor

  override val kind: ToolbarStateKind = params.kind

  override val leftMenuItem: ToolbarMenuItem?
    get() = _leftItem.value

  override val rightToolbarMenu: ToolbarMenu?
    get() = _toolbarMenu.value

  override fun update(params: IKurobaToolbarParams) {
    params as KurobaCatalogToolbarParams

    _leftItem.value = params.leftItem
    _title.value = params.title
    _subtitle.value = params.subtitle
    _toolbarMenu.value = params.toolbarMenu
    _onMainContentClick = params.onMainContentClick
    _iconClickInterceptor = params.iconClickInterceptor
  }

  override fun updateFromState(toolbarState: IKurobaToolbarState) {
    toolbarState as KurobaCatalogToolbarState

    _leftItem.value = toolbarState._leftItem.value
    _title.value = toolbarState._title.value
    _subtitle.value = toolbarState._subtitle.value
    _toolbarMenu.value = toolbarState._toolbarMenu.value
    _onMainContentClick = toolbarState._onMainContentClick
    _iconClickInterceptor = toolbarState._iconClickInterceptor
  }

  override fun onPushed() {
  }

  override fun onPopped() {
  }

  fun updateTitle(
    newTitle: ToolbarText? = _title.value,
    newSubTitle: ToolbarText? = _subtitle.value
  ) {
    _title.value = newTitle
    _subtitle.value = newSubTitle
  }

  override fun toString(): String {
    return "KurobaCatalogToolbarState(title: '${_title.value}', subtitle: '${_subtitle.value}')"
  }

}