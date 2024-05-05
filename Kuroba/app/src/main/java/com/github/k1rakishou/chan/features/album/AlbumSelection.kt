package com.github.k1rakishou.chan.features.album

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf

data class AlbumSelection(
  val isInSelectionMode: Boolean = false,
  val selectedItems: PersistentSet<Long> = persistentSetOf()
) {
  val size: Int
    get() = selectedItems.size

  fun add(albumItemId: Long): AlbumSelection {
    return copy(selectedItems = selectedItems.add(albumItemId))
  }

  fun addAll(albumItemIds: PersistentSet<Long>): AlbumSelection {
    return copy(selectedItems = selectedItems.addAll(albumItemIds))
  }

  fun contains(albumItemId: Long): Boolean {
    return selectedItems.contains(albumItemId)
  }

  fun remove(albumItemId: Long): AlbumSelection {
    return copy(selectedItems = selectedItems.remove(albumItemId))
  }

  fun isNotEmpty(): Boolean {
    return selectedItems.isNotEmpty()
  }

}