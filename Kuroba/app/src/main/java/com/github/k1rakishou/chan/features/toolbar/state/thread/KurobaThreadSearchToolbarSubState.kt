package com.github.k1rakishou.chan.features.toolbar.state.thread

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.github.k1rakishou.chan.features.toolbar.KurobaBaseSearchToolbarSubState
import com.github.k1rakishou.chan.features.toolbar.ToolbarMenu
import com.github.k1rakishou.chan.features.toolbar.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar.state.IKurobaToolbarParams
import com.github.k1rakishou.chan.features.toolbar.state.ToolbarStateKind

data class KurobaThreadSearchToolbarParams(
  val initialSearchQuery: String? = null,
  val toolbarMenu: ToolbarMenu? = null
) : IKurobaToolbarParams {
  override val kind: ToolbarStateKind = ToolbarStateKind.ThreadSearch
}

@Stable
class KurobaThreadSearchToolbarSubState (
  params: KurobaThreadSearchToolbarParams = KurobaThreadSearchToolbarParams()
) : KurobaBaseSearchToolbarSubState(initialSearchQuery = params.initialSearchQuery) {
  private val _toolbarMenu = mutableStateOf<ToolbarMenu?>(params.toolbarMenu)
  val toolbarMenu: State<ToolbarMenu?>
    get() = _toolbarMenu

  override val kind: ToolbarStateKind = params.kind

  override val leftMenuItem: ToolbarMenuItem? = null

  override val rightToolbarMenu: ToolbarMenu?
    get() = _toolbarMenu.value

  override fun update(params: IKurobaToolbarParams) {
    params as KurobaThreadSearchToolbarParams

    _toolbarMenu.value = params.toolbarMenu
  }

  fun updateMatchedPostsCounter(size: Int) {
    // TODO: New catalog/thread search. Update toolbarState.threadSearch with the amount of matched posts
  }

  override fun toString(): String {
    return "KurobaThreadSearchToolbarSubState(searchVisible: ${_searchVisibleState.value}, searchQuery: '${_searchQueryState.text}')"
  }

}