package com.github.k1rakishou.chan.features.toolbar_v2.state.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.textAsFlow
import androidx.compose.runtime.Stable
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarState
import com.github.k1rakishou.chan.ui.compose.clearText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

@Stable
class KurobaSearchToolbarState : IKurobaToolbarState {
  private val _searchVisible = MutableStateFlow(false)

  val searchQueryState = TextFieldState()

  fun listenForSearchQueryUpdates(): Flow<String> {
    return searchQueryState.textAsFlow()
      .map { textFieldCharSequence -> textFieldCharSequence.toString() }
  }

  fun listenForSearchVisibilityUpdates(): Flow<Boolean> {
    return _searchVisible.asStateFlow()
  }

  override fun onLayerPushed() {
    _searchVisible.value = true
  }

  override fun onLayerPopped() {
    _searchVisible.value = false
    searchQueryState.edit { clearText() }
  }

  fun isInSearchMode(): Boolean {
    return _searchVisible.value
  }

  fun clearSearchQuery() {
    searchQueryState.edit { clearText() }
  }

}