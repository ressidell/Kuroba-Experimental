package com.github.k1rakishou.chan.features.toolbar_v2

import androidx.compose.runtime.Immutable
import com.github.k1rakishou.chan.controller.transition.TransitionMode
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarState

@Immutable
sealed interface KurobaToolbarTransition {
  val transitionMode: TransitionMode
  val transitionToolbarState: IKurobaToolbarState?

  data class Progress(
    override val transitionMode: TransitionMode,
    override val transitionToolbarState: IKurobaToolbarState?,
    val progress: Float
  ) : KurobaToolbarTransition

  data class Instant(
    override val transitionMode: TransitionMode,
    override val transitionToolbarState: IKurobaToolbarState,
  ) : KurobaToolbarTransition

}