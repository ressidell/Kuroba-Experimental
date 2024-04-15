package com.github.k1rakishou.chan.ui.controller.base

import android.content.Context
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModel
import com.github.k1rakishou.chan.core.manager.GlobalWindowInsetsManager
import com.github.k1rakishou.chan.features.toolbar.BackArrowMenuItem
import com.github.k1rakishou.chan.features.toolbar.ToolbarMiddleContent
import com.github.k1rakishou.chan.features.toolbar.ToolbarText
import com.github.k1rakishou.chan.ui.compose.providers.ComposeEntrypoint
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.core_themes.ThemeEngine
import javax.inject.Inject

abstract class BaseComposeController<VM : ViewModel>(
  context: Context,
  @StringRes private val titleStringId: Int
) : Controller(context) {

  @Inject
  lateinit var themeEngine: ThemeEngine
  @Inject
  lateinit var globalWindowInsetsManager: GlobalWindowInsetsManager

  protected val controllerViewModel by lazy { controllerVM() }

  final override fun onCreate() {
    super.onCreate()

    setupNavigation()
    onPrepare()

    view = ComposeView(context).apply {
      setContent {
        ComposeEntrypoint {
          val chanTheme = LocalChanTheme.current

          Box(
            modifier = Modifier
                .fillMaxSize()
                .background(chanTheme.backColorCompose)
          ) {
            BuildContent()
          }
        }
      }
    }
  }

  open fun setupNavigation() {
    updateNavigationFlags(DeprecatedNavigationFlags(swipeable = false))

    toolbarState.enterDefaultMode(
      leftItem = BackArrowMenuItem(
        onClick = { requireNavController().popController() }
      ),
      middleContent = ToolbarMiddleContent.Title(
        title = ToolbarText.Id(titleStringId),
        subtitle = null
      )
    )
  }

  @CallSuper
  override fun onDestroy() {
    super.onDestroy()
  }

  open fun onPrepare() {

  }

  abstract fun controllerVM(): VM

  @Composable
  abstract fun BuildContent()

}