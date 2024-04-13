package com.github.k1rakishou.chan.features.toolbar.state.selection

import androidx.compose.runtime.IntState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.github.k1rakishou.chan.features.toolbar.ToolbarMenu
import com.github.k1rakishou.chan.features.toolbar.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar.state.IKurobaToolbarParams
import com.github.k1rakishou.chan.features.toolbar.state.KurobaToolbarSubState
import com.github.k1rakishou.chan.features.toolbar.state.ToolbarStateKind

data class KurobaSelectionToolbarParams(
  val leftItem: ToolbarMenuItem? = null,
  val selectedItemsCount: Int = 0,
  val totalItemsCount: Int = 0,
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

  private val _selectedItemsCount = mutableIntStateOf(params.selectedItemsCount)
  val selectedItemsCount: IntState
    get() = _selectedItemsCount
  private val _totalItemsCount = mutableIntStateOf(params.totalItemsCount)
  val totalItemsCount: IntState
    get() = _totalItemsCount

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
    _selectedItemsCount.intValue = params.selectedItemsCount
    _totalItemsCount.intValue = params.totalItemsCount
    _toolbarMenu.value = params.toolbarMenu
  }

  fun updateCounters(selectedItemsCount: Int, totalItemsCount: Int) {
    _selectedItemsCount.intValue = selectedItemsCount
    _totalItemsCount.intValue = totalItemsCount
  }

  override fun toString(): String {
    return "KurobaSelectionToolbarSubState(selectedItemsCount: ${selectedItemsCount.intValue}, totalItemsCount: ${totalItemsCount.intValue})"
  }

}