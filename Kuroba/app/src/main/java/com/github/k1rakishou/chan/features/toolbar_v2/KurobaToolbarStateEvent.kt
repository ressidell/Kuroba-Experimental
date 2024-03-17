package com.github.k1rakishou.chan.features.toolbar_v2

import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarStateKind

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
