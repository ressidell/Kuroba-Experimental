package com.github.k1rakishou.chan.features.toolbar

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.textAsFlow
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import com.github.k1rakishou.chan.features.toolbar.state.KurobaToolbarSubState
import com.github.k1rakishou.chan.ui.compose.clearText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

abstract class KurobaBaseSearchToolbarSubState(
  initialSearchQuery: String?
) : KurobaToolbarSubState() {

  protected val _searchVisibleState = mutableStateOf(false)
  val searchVisibleState: State<Boolean>
    get() = _searchVisibleState

  protected val _searchQueryState = TextFieldState(initialText = initialSearchQuery ?: "")
  val searchQueryState: TextFieldState
    get() = _searchQueryState


  override fun onShown() {
    super.onShown()
    _searchVisibleState.value = true
  }

  override fun onHidden() {
    super.onHidden()
    _searchVisibleState.value = false
  }

  override fun onDestroyed() {
    super.onDestroyed()
    _searchQueryState.edit { clearText() }
  }

  fun listenForSearchQueryUpdates(): Flow<String> {
    return _searchQueryState.textAsFlow()
      .map { textFieldCharSequence -> textFieldCharSequence.toString() }
      .filter { isInSearchMode() }
  }

  fun listenForSearchVisibilityUpdates(): Flow<Boolean> {
    return snapshotFlow { _searchVisibleState.value }
  }

  fun isInSearchMode(): Boolean {
    return _searchVisibleState.value
  }

}