package com.github.k1rakishou

import com.google.gson.annotations.SerializedName

data class ReorderableBottomNavViewButtons(
  @SerializedName("bottom_nav_view_button_ids")
  val bottomNavViewButtonIds: List<Long> = DEFAULT_IDS
) {
  fun bottomNavViewButtons(): List<BottomNavViewButton> {
    val buttonIds = bottomNavViewButtonIds
    if (buttonIds.isNullOrEmpty()) {
      return DEFAULT
    }

    if (buttonIds.toSet().size != DEFAULT.size) {
      return DEFAULT
    }

    val defaultButtonIds = DEFAULT.map { defaultButton -> defaultButton.id }.toSet()

    for (buttonId in buttonIds) {
      if (!defaultButtonIds.contains(buttonId)) {
        return DEFAULT
      }
    }

    val buttons = buttonIds.mapNotNull { buttonId -> BottomNavViewButton.findByIdOrNull(buttonId) }
    if (buttons.size != DEFAULT.size) {
      return DEFAULT
    }

    return buttons
  }

  companion object {
    val DEFAULT = listOf(
      BottomNavViewButton.Search,
      BottomNavViewButton.Archive,
      BottomNavViewButton.Bookmarks,
      BottomNavViewButton.Settings
    )

    val DEFAULT_IDS = DEFAULT.map { button -> button.id }
  }
}

enum class BottomNavViewButton(val id: Long, val title: String) {
  Search(0, "Search"),
  Bookmarks(1, "Bookmarks"),
  // TODO: This needs to be removed but it will probably break the sorting orders
  Browse(2, "Browse"),
  Settings(3, "Settings"),
  Archive(4, "Archive");

  companion object {
    fun contains(id: Long): Boolean {
      return entries.any { button -> button.id == id }
    }

    fun findByIdOrNull(id: Long): BottomNavViewButton? {
      return entries.firstOrNull { button -> button.id == id }
    }
  }
}