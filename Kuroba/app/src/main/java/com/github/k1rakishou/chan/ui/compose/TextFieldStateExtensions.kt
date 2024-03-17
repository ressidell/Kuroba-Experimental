package com.github.k1rakishou.chan.ui.compose

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.placeCursorAtEnd

fun TextFieldBuffer.clearText() {
  if (length > 0) {
    delete(0, length)
  }

  placeCursorAtEnd()
}

fun TextFieldBuffer.replaceWithText(newText: CharSequence) {
  replace(0, length, newText)
  placeCursorAtEnd()
}