package com.github.k1rakishou.chan.features.album

import android.content.Context
import android.os.Parcelable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.core.di.component.controller.ControllerComponent
import com.github.k1rakishou.chan.features.settings.screens.AppearanceSettingsScreen
import com.github.k1rakishou.chan.features.toolbar.BackArrowMenuItem
import com.github.k1rakishou.chan.features.toolbar.ToolbarMiddleContent
import com.github.k1rakishou.chan.features.toolbar.ToolbarText
import com.github.k1rakishou.chan.ui.controller.base.BaseComposeController
import com.github.k1rakishou.chan.ui.controller.base.DeprecatedNavigationFlags
import com.github.k1rakishou.chan.utils.ViewModelScope
import kotlinx.parcelize.Parcelize

class AlbumViewControllerV2(
  context: Context,
  private val listenMode: ListenMode,
  private val initialImageFullUrl: String?
) : BaseComposeController<AlbumViewControllerV2ViewModel, AlbumViewControllerV2.Params>(
  context = context,
  viewModelClass = AlbumViewControllerV2ViewModel::class.java
) {

  override val viewModelScope: ViewModelScope
    get() = ViewModelScope.ControllerScope(this)

  override fun injectControllerDependencies(component: ControllerComponent) {
    component.inject(this)
  }

  override fun viewModelParams(): Params = Params(listenMode, initialImageFullUrl)

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
  override fun ScreenContent() {
    val density = LocalDensity.current
    val albumItems = controllerViewModel.albumItems

    val albumSpanCountMut by controllerViewModel.albumSpanCount.collectAsState()
    val albumSpanCount = albumSpanCountMut
    if (albumSpanCount == null) {
      return
    }

    val albumLayoutGridModeMut by controllerViewModel.albumLayoutGridMode.collectAsState()
    val albumLayoutGridMode = albumLayoutGridModeMut
    if (albumLayoutGridMode == null) {
      return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val actualSpanCount = with(density) {
        remember(albumSpanCount) {
          calculateActualSpanCount(albumSpanCount, maxWidth.roundToPx())
        }
      }

      if (albumLayoutGridMode) {
        AlbumItemsGrid(
          controllerKey = controllerKey,
          albumItems = albumItems,
          albumSpanCount = actualSpanCount
        )
      } else {
        AlbumItemsStaggeredGrid(
          controllerKey = controllerKey,
          albumItems = albumItems,
          albumSpanCount = actualSpanCount
        )
      }
    }
  }

  private fun Density.calculateActualSpanCount(albumSpanCount: Int, maxWidth: Int): Int {
    val defaultSpanWidth = 120.dp.roundToPx()

    val actualAlbumSpanCount = if (albumSpanCount <= 0) {
      maxWidth / defaultSpanWidth
    } else {
      maxWidth / albumSpanCount
    }

    return AppearanceSettingsScreen.clampColumnsCount(actualAlbumSpanCount)
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