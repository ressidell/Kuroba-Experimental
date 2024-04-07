package com.github.k1rakishou.chan.ui.globalstate.reply

import com.github.k1rakishou.chan.features.reply.data.ReplyLayoutVisibility
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor

data class ReplyLayoutVisibilityStates(
  val catalog: ReplyLayoutVisibility,
  val thread: ReplyLayoutVisibility
) {

  fun anyOpenedOrExpanded(): Boolean {
    return anyOpened() || anyExpanded()
  }

  fun anyOpened(): Boolean {
    return catalog.isOpened() || thread.isOpened()
  }

  fun anyExpanded(): Boolean {
    return catalog.isExpanded() || thread.isExpanded()
  }

  fun isOpenedForDescriptor(chanDescriptor: ChanDescriptor): Boolean {
    return when (chanDescriptor) {
      is ChanDescriptor.ICatalogDescriptor -> catalog.isOpened()
      is ChanDescriptor.ThreadDescriptor -> thread.isOpened()
    }
  }

}