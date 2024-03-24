package com.github.k1rakishou.chan.ui.view.floating_menu

open class FloatingListMenuItem @JvmOverloads constructor(
  val key: Any,
  val name: String,
  val value: Any? = null,
  val visible: Boolean = true,
  val enabled: Boolean = true,
  val more: List<FloatingListMenuItem> = listOf()
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as FloatingListMenuItem

    if (key != other.key) return false
    if (name != other.name) return false
    if (value != other.value) return false
    if (visible != other.visible) return false
    if (enabled != other.enabled) return false
    if (more != other.more) return false

    return true
  }

  override fun hashCode(): Int {
    var result = key.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + visible.hashCode()
    result = 31 * result + enabled.hashCode()
    result = 31 * result + (value?.hashCode() ?: 0)
    result = 31 * result + more.hashCode()
    return result
  }

  override fun toString(): String {
    return "FloatingListMenuItem(key=$key, name='$name', value=$value, visible=$visible, " +
      "enabled=$enabled, moreItemsCount=${more.size})"
  }

}