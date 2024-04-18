package com.github.k1rakishou.chan.ui.compose.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.k1rakishou.chan.ui.compose.clearText

@Stable
class SimpleSearchStateV2<T>(
  val textFieldState: TextFieldState,
  results: List<T>
) : RememberObserver {
  val results = mutableStateOf(results)

  val usingSearch: Boolean
    get() = textFieldState.undoState.canUndo
  val searchQuery: CharSequence
    get() = textFieldState.text

  fun reset() {
    textFieldState.edit { clearText() }
    textFieldState.undoState.clearHistory()
  }

  override fun onRemembered() {
  }

  override fun onAbandoned() {
    reset()
  }

  override fun onForgotten() {
    reset()
  }

}

@Composable
fun <T> rememberSimpleSearchStateV2(
  textFieldState: TextFieldState,
  results: List<T> = emptyList()
): SimpleSearchStateV2<T> {
  return remember(textFieldState) {
    SimpleSearchStateV2(
      textFieldState = textFieldState,
      results = results
    )
  }
}

class SimpleSearchStateNullable<T>(
  val queryState: MutableState<String?>,
  results: List<T>,
  searching: Boolean
) {
  var query by queryState

  var results by mutableStateOf(results)
  var searching by mutableStateOf(searching)

  fun isUsingSearch(): Boolean = query != null

  fun reset() {
    query = null
  }
}

@Composable
fun <T> rememberSimpleSearchStateNullable(
  searchQuery: String? = null,
  searchQueryState: MutableState<String?>? = null,
  results: List<T> = emptyList(),
  searching: Boolean = false
): SimpleSearchStateNullable<T> {
  return remember {
    val actualQueryState = when {
      searchQueryState != null -> {
        searchQueryState
      }
      else -> {
        mutableStateOf(searchQuery)
      }
    }

    SimpleSearchStateNullable(actualQueryState, results, searching)
  }
}