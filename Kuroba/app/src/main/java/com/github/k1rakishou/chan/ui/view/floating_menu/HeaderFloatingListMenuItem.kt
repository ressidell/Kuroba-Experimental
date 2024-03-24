package com.github.k1rakishou.chan.ui.view.floating_menu

class HeaderFloatingListMenuItem(key: Any, title: String) : FloatingListMenuItem(key, title) {

  override fun toString(): String {
    return "HeaderFloatingListMenuItem(key=$key, name='$name', value=$value, visible=$visible, " +
      "enabled=$enabled, moreItemsCount=${more.size})"
  }

}