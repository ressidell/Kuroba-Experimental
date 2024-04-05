package com.github.k1rakishou.chan.features.toolbar_v2.state.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.textAsFlow
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenu
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.KurobaToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarStateKind
import com.github.k1rakishou.chan.ui.compose.clearText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class KurobaSearchToolbarParams(
  val initialSearchQuery: String? = null,
  val toolbarMenu: ToolbarMenu? = null
) : IKurobaToolbarParams {
  override val kind: ToolbarStateKind = ToolbarStateKind.Search
}

@Stable
class KurobaSearchToolbarSubState(
  params: KurobaSearchToolbarParams = KurobaSearchToolbarParams()
) : KurobaToolbarSubState {
  private val _toolbarMenu = mutableStateOf<ToolbarMenu?>(params.toolbarMenu)
  val toolbarMenu: State<ToolbarMenu?>
    get() = _toolbarMenu

  private val _searchVisibleState = mutableStateOf(false)
  val searchVisibleState: State<Boolean>
    get() = _searchVisibleState

  private val _searchQueryState = TextFieldState(initialText = params.initialSearchQuery ?: "")
  val searchQueryState: TextFieldState
    get() = _searchQueryState

  override val kind: ToolbarStateKind = params.kind

  override val leftMenuItem: ToolbarMenuItem? = null

  override val rightToolbarMenu: ToolbarMenu?
    get() = _toolbarMenu.value

  override fun update(params: IKurobaToolbarParams) {
    params as KurobaSearchToolbarParams

    _toolbarMenu.value = params.toolbarMenu
  }

  override fun onPushed() {
    _searchVisibleState.value = true
  }

  override fun onPopped() {
    _searchVisibleState.value = false
    _searchQueryState.edit { clearText() }
  }

  fun listenForSearchQueryUpdates(): Flow<String> {
    return _searchQueryState.textAsFlow()
      .map { textFieldCharSequence -> textFieldCharSequence.toString() }
  }

  fun listenForSearchVisibilityUpdates(): Flow<Boolean> {
    return snapshotFlow { _searchVisibleState.value }
  }

  fun isInSearchMode(): Boolean {
    return _searchVisibleState.value
  }

  fun clearSearchQuery() {
    _searchQueryState.edit { clearText() }
  }

  override fun toString(): String {
    return "KurobaSearchToolbarSubState(searchVisible: ${_searchVisibleState.value}, searchQuery: '${_searchQueryState.text}')"
  }

}