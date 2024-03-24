package com.github.k1rakishou.chan.features.toolbar_v2

import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarState

sealed interface KurobaToolbarTransition {
  val transitionMode: TransitionMode
  val transitionToolbarState: IKurobaToolbarState
  
  data class Instance(
    override val transitionMode: TransitionMode,
    override val transitionToolbarState: IKurobaToolbarState
  ) : KurobaToolbarTransition
  
  data class Progress(
    override val transitionMode: TransitionMode,
    override val transitionToolbarState: IKurobaToolbarState,
    val progress: Float
  ) : KurobaToolbarTransition

  enum class TransitionMode {
    In,
    Out
  }

}