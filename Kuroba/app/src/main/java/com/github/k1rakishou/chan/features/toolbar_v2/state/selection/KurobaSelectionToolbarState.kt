package com.github.k1rakishou.chan.features.toolbar_v2.state.selection

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenu
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarText
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarStateKind

data class KurobaSelectionToolbarParams(
  val title: ToolbarText? = null,
  val toolbarMenu: ToolbarMenu? = null
) : IKurobaToolbarParams {
  override val kind: ToolbarStateKind = ToolbarStateKind.Selection
}

class KurobaSelectionToolbarState(
  params: KurobaSelectionToolbarParams = KurobaSelectionToolbarParams()
) : IKurobaToolbarState {
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

    _title.value = params.title
    _toolbarMenu.value = params.toolbarMenu
  }

  override fun updateFromState(toolbarState: IKurobaToolbarState) {
    toolbarState as KurobaSelectionToolbarState

    _title.value = toolbarState._title.value
    _toolbarMenu.value = toolbarState._toolbarMenu.value
  }

  fun updateTitle(text: ToolbarText) {
    _title.value = text
  }

}