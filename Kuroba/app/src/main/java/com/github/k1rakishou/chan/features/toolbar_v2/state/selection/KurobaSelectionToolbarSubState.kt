package com.github.k1rakishou.chan.features.toolbar_v2.state.selection

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenu
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarText
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.KurobaToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarStateKind

data class KurobaSelectionToolbarParams(
  val leftItem: ToolbarMenuItem? = null,
  val title: ToolbarText? = null,
  val toolbarMenu: ToolbarMenu? = null
) : IKurobaToolbarParams {
  override val kind: ToolbarStateKind = ToolbarStateKind.Selection
}

class KurobaSelectionToolbarSubState(
  params: KurobaSelectionToolbarParams = KurobaSelectionToolbarParams()
) : KurobaToolbarSubState() {
  private val _leftItem = mutableStateOf<ToolbarMenuItem?>(params.leftItem)
  val leftItem: State<ToolbarMenuItem?>
    get() = _leftItem

  private val _toolbarMenu = mutableStateOf<ToolbarMenu?>(params.toolbarMenu)
  val toolbarMenu: State<ToolbarMenu?>
    get() = _toolbarMenu

  private val _title = mutableStateOf<ToolbarText?>(params.title)
  val title: State<ToolbarText?>
    get() = _title

  override val kind: ToolbarStateKind = params.kind

  override val leftMenuItem: ToolbarMenuItem? = null

  override val rightToolbarMenu: ToolbarMenu?
    get() = _toolbarMenu.value

  override fun onPushed() {
  }

  override fun onPopped() {
  }

  override fun update(params: IKurobaToolbarParams) {
    params as KurobaSelectionToolbarParams

    _leftItem.value = params.leftItem
    _title.value = params.title
    _toolbarMenu.value = params.toolbarMenu
  }

  fun updateTitle(title: ToolbarText) {
    _title.value = title
  }

  override fun toString(): String {
    return "KurobaSelectionToolbarSubState(title: '${_title.value}')"
  }

}