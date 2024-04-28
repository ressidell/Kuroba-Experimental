package com.github.k1rakishou.chan.ui.controller.base

import android.content.Context
import androidx.annotation.CallSuper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModel
import com.github.k1rakishou.chan.core.manager.GlobalWindowInsetsManager
import com.github.k1rakishou.chan.ui.compose.providers.ComposeEntrypoint
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.core_themes.ThemeEngine
import javax.inject.Inject

abstract class BaseComposeController<VM : ViewModel>(
  context: Context
) : Controller(context) {

  @Inject
  lateinit var themeEngine: ThemeEngine
  @Inject
  lateinit var globalWindowInsetsManager: GlobalWindowInsetsManager

  private val controllerViewModelLazy = controllerVM()
  protected val controllerViewModel: VM
    get() = controllerViewModelLazy.value

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

  abstract fun setupNavigation()

  @CallSuper
  override fun onDestroy() {
    super.onDestroy()
  }

  open fun onPrepare() {

  }

  abstract fun controllerVM(): Lazy<VM>

  @Composable
  abstract fun BuildContent()

}