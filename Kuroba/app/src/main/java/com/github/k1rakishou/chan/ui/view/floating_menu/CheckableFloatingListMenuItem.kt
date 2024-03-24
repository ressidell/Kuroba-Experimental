package com.github.k1rakishou.chan.ui.view.floating_menu

class CheckableFloatingListMenuItem(
  key: Any,
  name: String,
  value: Any? = null,
  val groupId: Any? = null,
  visible: Boolean = true,
  enabled: Boolean = true,
  more: List<FloatingListMenuItem> = listOf(),
  val checked: Boolean = false
) : FloatingListMenuItem(key, name, value, visible, enabled, more) {

  override fun toString(): String {
    return "CheckableFloatingListMenuItem(key=$key, name='$name', value=$value, visible=$visible, enabled=$enabled, moreItemsCount=${more.size}, groupId=$groupId, checked=$checked)"
  }

}