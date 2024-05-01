package com.github.k1rakishou.chan.features.album

import android.content.Context
import android.os.Parcelable
import androidx.compose.runtime.Composable
import com.github.k1rakishou.chan.core.di.component.controller.ControllerComponent
import com.github.k1rakishou.chan.features.toolbar.BackArrowMenuItem
import com.github.k1rakishou.chan.features.toolbar.ToolbarMiddleContent
import com.github.k1rakishou.chan.features.toolbar.ToolbarText
import com.github.k1rakishou.chan.ui.controller.base.BaseComposeController
import com.github.k1rakishou.chan.ui.controller.base.DeprecatedNavigationFlags
import com.github.k1rakishou.chan.utils.viewModelByKey
import kotlinx.parcelize.Parcelize

class AlbumViewControllerV2(
  context: Context,
  private val listenMode: ListenMode,
  private val initialImageFullUrl: String?
) : BaseComposeController<AlbumViewControllerV2ViewModel>(context) {

  override fun injectControllerDependencies(component: ControllerComponent) {
    component.inject(this)
  }

  override fun controllerVM(): Lazy<AlbumViewControllerV2ViewModel> {
    return viewModelByKey<AlbumViewControllerV2ViewModel>(
      params = { Params(listenMode, initialImageFullUrl) }
    )
  }

  override fun setupNavigation() {
    updateNavigationFlags(
      newNavigationFlags = DeprecatedNavigationFlags()
    )

    toolbarState.enterDefaultMode(
      leftItem = BackArrowMenuItem(
        onClick = { requireNavController().popController() }
      ),
      middleContent = ToolbarMiddleContent.Title(
        title = ToolbarText.String(""),
        subtitle = null
      )
    )
  }

  @Composable
  override fun BuildContent() {
    println("TTTAAA controllerViewModel: ${controllerViewModel.hashCode()}")
    val albumItems = controllerViewModel.albumItems

    // TODO: scoped viewmodels.
    println("TTTAAA albumItems: ${albumItems.size}")
  }

  @Parcelize
  data class Params(
    val listenMode: ListenMode,
    val initialImageFullUrl: String?
  ) : Parcelable

  @Parcelize
  enum class ListenMode : Parcelable {
    Catalog,
    Thread
  }

}