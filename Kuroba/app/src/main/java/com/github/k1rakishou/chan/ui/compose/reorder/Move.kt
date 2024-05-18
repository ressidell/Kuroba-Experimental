package com.github.k1rakishou.chan.ui.compose.reorder

import kotlinx.collections.immutable.PersistentList

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

fun <T> PersistentList<T>.move(fromIdx: Int, toIdx: Int): PersistentList<T> {
  if (fromIdx == toIdx) {
    return this
  }

  if (fromIdx < 0 || fromIdx >= size) {
    return this
  }

  if (toIdx < 0 || toIdx >= size) {
    return this
  }

  val element = get(fromIdx)
  return removeAt(fromIdx).add(toIdx, element)
}