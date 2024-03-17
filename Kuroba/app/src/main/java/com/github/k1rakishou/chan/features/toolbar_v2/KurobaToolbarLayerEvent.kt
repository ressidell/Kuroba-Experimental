package com.github.k1rakishou.chan.features.toolbar_v2

sealed interface KurobaToolbarLayerEvent {
  val layer: KurobaToolbarLayer

  data class Pushed(
    override val layer: KurobaToolbarLayer
  ) : KurobaToolbarLayerEvent

  data class Replaced(
    override val layer: KurobaToolbarLayer
  ) : KurobaToolbarLayerEvent

  data class Popped(
    override val layer: KurobaToolbarLayer
  ) : KurobaToolbarLayerEvent
}
