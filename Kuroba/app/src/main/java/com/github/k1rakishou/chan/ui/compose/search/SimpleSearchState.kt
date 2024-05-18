package com.github.k1rakishou.chan.ui.compose.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.github.k1rakishou.chan.ui.compose.clearText

@Stable
class SimpleSearchStateV2<T>(
  val textFieldState: TextFieldState,
  results: List<T>
) : RememberObserver {
  val results = mutableStateOf(results)

  val usingSearch: Boolean
    get() = textFieldState.text.isNotEmpty()
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