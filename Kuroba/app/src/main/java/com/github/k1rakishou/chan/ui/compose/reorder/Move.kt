package com.github.k1rakishou.chan.ui.compose.reorder

fun <T> MutableList<T>.move(fromIdx: Int, toIdx: Int): Boolean {
  if (fromIdx == toIdx) {
    return false
  }

  if (fromIdx < 0 || fromIdx >= size) {
    return false
  }

  if (toIdx < 0 || toIdx >= size) {
    return false
  }

  add(toIdx, removeAt(fromIdx))
  return true
}