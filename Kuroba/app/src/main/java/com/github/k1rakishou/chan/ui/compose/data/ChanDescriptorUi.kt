package com.github.k1rakishou.chan.ui.compose.data

import androidx.compose.runtime.Immutable
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor

@Immutable
data class ChanDescriptorUi(
  val chanDescriptor: ChanDescriptor
)