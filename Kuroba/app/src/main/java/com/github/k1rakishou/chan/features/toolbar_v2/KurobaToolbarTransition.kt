package com.github.k1rakishou.chan.features.toolbar_v2

import com.github.k1rakishou.chan.controller.transition.TransitionMode
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarState

data class KurobaToolbarTransition(
  val transitionMode: TransitionMode,
  val transitionToolbarState: IKurobaToolbarState,
  val progress: Float
)