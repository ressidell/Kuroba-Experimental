package com.github.k1rakishou.chan.features.toolbar_v2.state.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.textAsFlow
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenu
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarState
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarStateKind
import com.github.k1rakishou.chan.ui.compose.clearText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

data class KurobaSearchToolbarParams(
  val toolbarMenu: ToolbarMenu? = null
) : IKurobaToolbarParams {
  override val kind: ToolbarStateKind = ToolbarStateKind.Search
}

@Stable
class KurobaSearchToolbarState(
  params: KurobaSearchToolbarParams = KurobaSearchToolbarParams()
) : IKurobaToolbarState {
  private val _toolbarMenu = mutableStateOf<ToolbarMenu?>(params.toolbarMenu)
  val toolbarMenu: State<ToolbarMenu?>
    get() = _toolbarMenu

  private val _searchVisible = MutableStateFlow(false)
  val searchQueryState = TextFieldState()

  override val kind: ToolbarStateKind = ToolbarStateKind.Search

  override val leftMenuItem: ToolbarMenuItem? = null

  override val rightToolbarMenu: ToolbarMenu?
    get() = _toolbarMenu.value

  override fun update(params: IKurobaToolbarParams) {
    TODO("Not yet implemented")
  }

  override fun updateFromState(toolbarState: IKurobaToolbarState) {
    toolbarState as KurobaSearchToolbarState

    _toolbarMenu.value = toolbarState._toolbarMenu.value
    _searchVisible.value = toolbarState._searchVisible.value
  }

  override fun onPushed() {
    _searchVisible.value = true
  }

  override fun onPopped() {
    _searchVisible.value = false
    searchQueryState.edit { clearText() }
  }

  fun listenForSearchQueryUpdates(): Flow<String> {
    return searchQueryState.textAsFlow()
      .map { textFieldCharSequence -> textFieldCharSequence.toString() }
  }

  fun listenForSearchVisibilityUpdates(): Flow<Boolean> {
    return _searchVisible.asStateFlow()
  }

  fun isInSearchMode(): Boolean {
    return _searchVisible.value
  }

  fun clearSearchQuery() {
    searchQueryState.edit { clearText() }
  }

}