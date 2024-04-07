package com.github.k1rakishou.chan.features.toolbar

import com.github.k1rakishou.chan.features.toolbar.state.ToolbarStateKind

sealed interface KurobaToolbarStateEvent {
  val kind: ToolbarStateKind

  data class Pushed(
    override val kind: ToolbarStateKind
  ) : KurobaToolbarStateEvent

  data class Updated(
    override val kind: ToolbarStateKind
  ) : KurobaToolbarStateEvent

  data class Popped(
    override val kind: ToolbarStateKind
  ) : KurobaToolbarStateEvent
}
