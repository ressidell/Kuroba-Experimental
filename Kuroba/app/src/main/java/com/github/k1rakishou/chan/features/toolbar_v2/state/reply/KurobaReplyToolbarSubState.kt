package com.github.k1rakishou.chan.features.toolbar_v2.state.reply

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenu
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.KurobaToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarStateKind
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor

data class KurobaReplyToolbarParams(
  val chanDescriptor: ChanDescriptor? = null,
  val leftItem: ToolbarMenuItem? = null,
  val toolbarMenu: ToolbarMenu? = null
) : IKurobaToolbarParams {
  override val kind: ToolbarStateKind = ToolbarStateKind.Reply
}

class KurobaReplyToolbarSubState(
  params: KurobaReplyToolbarParams = KurobaReplyToolbarParams()
) : KurobaToolbarSubState() {
  private val _chanDescriptor = mutableStateOf<ChanDescriptor?>(params.chanDescriptor)
  val chanDescriptor: State<ChanDescriptor?>
    get() = _chanDescriptor

  private val _leftItem = mutableStateOf<ToolbarMenuItem?>(params.leftItem)
  val leftItem: State<ToolbarMenuItem?>
    get() = _leftItem

  private val _toolbarMenu = mutableStateOf<ToolbarMenu?>(params.toolbarMenu)
  val toolbarMenu: State<ToolbarMenu?>
    get() = _toolbarMenu

  override val kind: ToolbarStateKind = params.kind

  override val leftMenuItem: ToolbarMenuItem? = null

  override val rightToolbarMenu: ToolbarMenu?
    get() = _toolbarMenu.value

  override fun update(params: IKurobaToolbarParams) {
    params as KurobaReplyToolbarParams

    _leftItem.value = params.leftItem
    _chanDescriptor.value = params.chanDescriptor
    _toolbarMenu.value = params.toolbarMenu
  }

  override fun onPushed() {
  }

  override fun onPopped() {
  }

  override fun toString(): String {
    return "KurobaReplyToolbarSubState(chanDescriptor: ${_chanDescriptor.value})"
  }

}