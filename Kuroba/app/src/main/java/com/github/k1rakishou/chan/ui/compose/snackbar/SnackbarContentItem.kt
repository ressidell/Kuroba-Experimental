package com.github.k1rakishou.chan.ui.compose.snackbar

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.github.k1rakishou.common.awaitCatching
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong


interface SnackbarClickable {
  val snackbarButtonId: SnackbarButtonId
  val data: Any?
}

fun snackbarText(
  text: String,
  textColor: Color? = null,
  takeWholeWidth: Boolean = true
): SnackbarContentItem.Text {
  return SnackbarContentItem.Text(text, textColor, takeWholeWidth)
}

fun <T> snackbarButton(
  coroutineScope: CoroutineScope,
  text: String,
  show: Boolean = true,
  textColor: Color? = null,
  data: T? = null,
  onClick: (T?) -> Unit
): SnackbarContentItem.Button {
  val snackbarButtonId = SnackbarButtonId(id = SnackbarContentItem.KEY_COUNTER.getAndIncrement().toString())
  val clickAwaitable = CompletableDeferred<Any?>()

  coroutineScope.launch {
    clickAwaitable.awaitCatching()
      .onSuccess { value -> onClick(value as T) }
      .ignore()
  }

  return SnackbarContentItem.Button(
    snackbarButtonId = snackbarButtonId,
    data = data,
    show = show,
    textColor = textColor,
    text = text,
    clickAwaitable = clickAwaitable
  )
}

@Stable
sealed class SnackbarContentItem {
  data object LoadingIndicator : SnackbarContentItem()

  data class Text(
    private val text: String,
    val textColor: Color? = null,
    val takeWholeWidth: Boolean = true
  ) : SnackbarContentItem() {
    val formattedText by lazy { text.replace('\n', ' ') }
  }

  data class Button(
    override val snackbarButtonId: SnackbarButtonId,
    override val data: Any? = null,
    val show: Boolean = true,
    val textColor: Color? = null,
    val text: String,
    val clickAwaitable: CompletableDeferred<Any?>,
  ) : SnackbarContentItem(), SnackbarClickable

  data class Icon(
    @DrawableRes val drawableId: Int,
  ) : SnackbarContentItem()

  data class Spacer(val space: Dp) : SnackbarContentItem()

  companion object {
    internal val KEY_COUNTER = AtomicLong(0)
  }
}