package com.github.k1rakishou.chan.features.toolbar.state.catalog

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.github.k1rakishou.chan.features.toolbar.KurobaBaseSearchToolbarSubState
import com.github.k1rakishou.chan.features.toolbar.ToolbarMenu
import com.github.k1rakishou.chan.features.toolbar.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar.state.IKurobaToolbarParams
import com.github.k1rakishou.chan.features.toolbar.state.ToolbarStateKind

data class KurobaCatalogSearchToolbarParams(
  val initialSearchQuery: String? = null,
  val toolbarMenu: ToolbarMenu? = null
) : IKurobaToolbarParams {
  override val kind: ToolbarStateKind = ToolbarStateKind.CatalogSearch
}

@Stable
class KurobaCatalogSearchToolbarSubState (
  params: KurobaCatalogSearchToolbarParams = KurobaCatalogSearchToolbarParams()
) : KurobaBaseSearchToolbarSubState(initialSearchQuery = params.initialSearchQuery) {
  private val _toolbarMenu = mutableStateOf<ToolbarMenu?>(params.toolbarMenu)
  val toolbarMenu: State<ToolbarMenu?>
    get() = _toolbarMenu

  override val kind: ToolbarStateKind = params.kind

  override val leftMenuItem: ToolbarMenuItem? = null

  override val rightToolbarMenu: ToolbarMenu?
    get() = _toolbarMenu.value

  override fun update(params: IKurobaToolbarParams) {
    params as KurobaCatalogSearchToolbarParams

    _toolbarMenu.value = params.toolbarMenu
  }

  fun updateMatchedPostsCounter(size: Int) {
    // TODO: New catalog/thread search. Update toolbarState.catalogSearch with the amount of matched posts
  }

  override fun toString(): String {
    return "KurobaCatalogSearchToolbarSubState(searchVisible: ${_searchVisibleState.value}, searchQuery: '${_searchQueryState.text}')"
  }

}