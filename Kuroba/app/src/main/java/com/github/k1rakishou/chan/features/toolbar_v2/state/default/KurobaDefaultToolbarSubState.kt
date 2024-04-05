package com.github.k1rakishou.chan.features.toolbar_v2.state.default

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenu
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMiddleContent
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarText
import com.github.k1rakishou.chan.features.toolbar_v2.state.IKurobaToolbarParams
import com.github.k1rakishou.chan.features.toolbar_v2.state.KurobaToolbarSubState
import com.github.k1rakishou.chan.features.toolbar_v2.state.ToolbarStateKind

@Immutable
data class KurobaDefaultToolbarParams(
  val leftItem: ToolbarMenuItem? = null,
  val scrollableTitle: Boolean = false,
  val middleContent: ToolbarMiddleContent? = null,
  val toolbarMenu: ToolbarMenu? = null,
  val iconClickInterceptor: ((ToolbarMenuItem) -> Boolean)? = null
) : IKurobaToolbarParams {
  override val kind: ToolbarStateKind = ToolbarStateKind.Default
}

@Stable
class KurobaDefaultToolbarSubState(
  params: KurobaDefaultToolbarParams = KurobaDefaultToolbarParams()
) : KurobaToolbarSubState {
  private val _leftItem = mutableStateOf<ToolbarMenuItem?>(params.leftItem)
  val leftItem: State<ToolbarMenuItem?>
    get() = _leftItem

  private val _scrollableTitle = mutableStateOf<Boolean>(params.scrollableTitle)
  val scrollableTitle: State<Boolean>
    get() = _scrollableTitle

  private val _middleContent = mutableStateOf<ToolbarMiddleContent?>(params.middleContent)
  val middleContent: State<ToolbarMiddleContent?>
    get() = _middleContent

  private val _toolbarMenu = mutableStateOf<ToolbarMenu?>(params.toolbarMenu)
  val toolbarMenu: State<ToolbarMenu?>
    get() = _toolbarMenu

  private var _iconClickInterceptor: ((ToolbarMenuItem) -> Boolean)? = params.iconClickInterceptor
  val iconClickInterceptor: ((ToolbarMenuItem) -> Boolean)?
    get() = _iconClickInterceptor

  override val kind: ToolbarStateKind = params.kind

  override val leftMenuItem: ToolbarMenuItem?
    get() = _leftItem.value

  override val rightToolbarMenu: ToolbarMenu?
    get() = _toolbarMenu.value

  override fun update(params: IKurobaToolbarParams) {
    params as KurobaDefaultToolbarParams

    _leftItem.value = params.leftItem
    _scrollableTitle.value = params.scrollableTitle
    _middleContent.value = params.middleContent
    _toolbarMenu.value = params.toolbarMenu
    _iconClickInterceptor = params.iconClickInterceptor
  }

  override fun onPushed() {
  }

  override fun onPopped() {
  }

  fun updateTitle(
    newTitle: ToolbarText? = (_middleContent.value as? ToolbarMiddleContent.Title)?.title,
    newSubTitle: ToolbarText? = (_middleContent.value as? ToolbarMiddleContent.Title)?.subtitle
  ) {
    if (_middleContent.value !is ToolbarMiddleContent.Title) {
      return
    }

    _middleContent.value = ToolbarMiddleContent.Title(
      title = newTitle,
      subtitle = newSubTitle
    )
  }

  override fun toString(): String {
    val middleContentToolbarTitle = _middleContent.value as? ToolbarMiddleContent.Title
    return "KurobaDefaultToolbarSubState(middleContent: ${middleContentToolbarTitle})"
  }

}