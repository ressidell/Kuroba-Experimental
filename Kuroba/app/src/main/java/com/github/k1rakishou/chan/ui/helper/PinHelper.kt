package com.github.k1rakishou.chan.ui.helper

object PinHelper {

  @JvmStatic
  fun getShortUnreadCount(value: Int): String {
    if (value < 1000) {
      return value.toString()
    }

    val thousands = value.toFloat() / 1000f
    if (thousands >= 1000f) {
      return thousands.toString() + "kk"
    }

    return "%.${1}f".format(thousands) + "k"
  }

}