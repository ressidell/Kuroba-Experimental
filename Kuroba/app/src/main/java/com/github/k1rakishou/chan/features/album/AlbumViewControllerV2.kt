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
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.di.component.controller.ControllerComponent
import com.github.k1rakishou.chan.features.settings.screens.AppearanceSettingsScreen
import com.github.k1rakishou.chan.features.toolbar.BackArrowMenuItem
import com.github.k1rakishou.chan.features.toolbar.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar.ToolbarMiddleContent
import com.github.k1rakishou.chan.features.toolbar.ToolbarText
import com.github.k1rakishou.chan.ui.compose.snackbar.SnackbarContainer
import com.github.k1rakishou.chan.ui.compose.snackbar.SnackbarScope
import com.github.k1rakishou.chan.ui.controller.base.BaseComposeController
import com.github.k1rakishou.chan.ui.controller.base.DeprecatedNavigationFlags
import com.github.k1rakishou.chan.utils.ViewModelScope
import com.github.k1rakishou.persist_state.PersistableChanState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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

  override val snackbarScope: SnackbarScope
    get() {
      return when (listenMode) {
        ListenMode.Catalog -> SnackbarScope.Album(mainLayoutAnchor = SnackbarScope.MainLayoutAnchor.Catalog)
        ListenMode.Thread -> SnackbarScope.Album(mainLayoutAnchor = SnackbarScope.MainLayoutAnchor.Thread)
      }
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
      ),
      menuBuilder = {
        withMenuItem(
          id = ACTION_TOGGLE_LAYOUT_MODE,
          drawableId = if (PersistableChanState.albumLayoutGridMode.get()) {
            R.drawable.ic_baseline_view_quilt_24
          } else {
            R.drawable.ic_baseline_view_comfy_24
          },
          onClick = { item -> toggleLayoutModeClicked(item) }
        )

        withOverflowMenu {
          withCheckableOverflowMenuItem(
            id = ACTION_TOGGLE_IMAGE_DETAILS,
            stringId = R.string.action_album_show_image_details,
            visible = true,
            checked = PersistableChanState.showAlbumViewsImageDetails.get(),
            onClick = { onToggleAlbumViewsImageInfoToggled() }
          )
        }
      }
    )

    controllerScope.launch {
      controllerViewModel.toolbarData
        .onEach { toobarData ->
          toolbarState.default.updateTitle(
            newTitle = ToolbarText.Companion.from(toobarData.title),
            newSubTitle = ToolbarText.Companion.from(toobarData.subtitle)
          )
        }
        .collect()
    }

    controllerScope.launch {
      controllerViewModel.albumLayoutGridMode
        .onEach { onAlbumLayoutGridModeToggled() }
        .collect()
    }
  }

  @Composable
  override fun ScreenContent() {
    val density = LocalDensity.current

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
      val actualSpanCount = remember(albumSpanCount, maxWidth) {
        with(density) { calculateActualSpanCount(albumSpanCount, maxWidth.roundToPx()) }
      }

      if (albumLayoutGridMode) {
        AlbumItemsGrid(
          controllerKey = controllerKey,
          controllerViewModel = controllerViewModel,
          albumSpanCount = actualSpanCount
        )
      } else {
        AlbumItemsStaggeredGrid(
          controllerKey = controllerKey,
          controllerViewModel = controllerViewModel,
          albumSpanCount = actualSpanCount
        )
      }

      SnackbarContainer(modifier = Modifier.fillMaxSize())
    }
  }

  private fun toggleLayoutModeClicked(item: ToolbarMenuItem) {
    PersistableChanState.albumLayoutGridMode.toggle()

    toolbarState.findItem(ACTION_TOGGLE_LAYOUT_MODE)?.let { toolbarMenuItem ->
      val drawableId = if (PersistableChanState.albumLayoutGridMode.get()) {
        R.drawable.ic_baseline_view_quilt_24
      } else {
        R.drawable.ic_baseline_view_comfy_24
      }

      toolbarMenuItem.updateDrawableId(drawableId)
    }
  }

  private fun onAlbumLayoutGridModeToggled() {
    toolbarState.findItem(ACTION_TOGGLE_LAYOUT_MODE)?.let { toolbarMenuItem ->
      val downloadDrawableId = if (PersistableChanState.albumLayoutGridMode.get()) {
        R.drawable.ic_baseline_view_quilt_24
      } else {
        R.drawable.ic_baseline_view_comfy_24
      }

      toolbarMenuItem.updateDrawableId(downloadDrawableId)
    }
  }

  private fun onToggleAlbumViewsImageInfoToggled() {
    toolbarState.findCheckableOverflowItem(ACTION_TOGGLE_IMAGE_DETAILS)
      ?.updateChecked(PersistableChanState.showAlbumViewsImageDetails.toggle())
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

  companion object {
    private const val ACTION_TOGGLE_LAYOUT_MODE = 1
    private const val ACTION_TOGGLE_IMAGE_DETAILS = 2
  }

}