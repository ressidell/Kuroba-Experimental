package com.github.k1rakishou.chan.ui.widget

import android.graphics.Color
import android.view.View
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.manager.GlobalWindowInsetsManager
import com.github.k1rakishou.chan.ui.controller.ThreadControllerType
import com.github.k1rakishou.chan.ui.globalstate.GlobalUiStateHolder
import com.github.k1rakishou.chan.ui.view.KurobaBottomNavigationView
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.dp
import com.github.k1rakishou.core_themes.ChanTheme
import com.github.k1rakishou.core_themes.ThemeEngine
import com.github.k1rakishou.core_themes.ThemeEngine.Companion.isDarkColor
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import javax.inject.Inject

class SnackbarWrapper private constructor(
  snackbar: Snackbar
) {

  @Inject
  lateinit var themeEngine: ThemeEngine
  @Inject
  lateinit var globalUiStateHolder: GlobalUiStateHolder
  @Inject
  lateinit var globalWindowInsetsManager: GlobalWindowInsetsManager

  private var _snackbar: Snackbar? = snackbar

  init {
    AppModuleAndroidUtils.extractActivityComponent(snackbar.context)
      .inject(this)

    _snackbar?.view?.setBackgroundColor(themeEngine.chanTheme.primaryColor)
  }

  fun dismiss() {
    _snackbar?.dismiss()
    _snackbar = null
  }

  fun setAction(actionTextId: Int, onClickListener: View.OnClickListener) {
    _snackbar?.setAction(actionTextId, onClickListener)
  }

  fun setAction(actionTextId: Int, onClickListener: () -> Unit) {
    _snackbar?.setAction(actionTextId) { onClickListener.invoke() }
  }

  fun show(threadControllerType: ThreadControllerType) {
    val snackbarClass = when (threadControllerType) {
      ThreadControllerType.Catalog -> SnackbarClass.Catalog
      ThreadControllerType.Thread -> SnackbarClass.Thread
      // TODO: SnackbarClass.Generic
    }

    val isReplyLayoutOpened = globalUiStateHolder.replyLayout.state(threadControllerType).isOpenedOrExpanded()
    if (isReplyLayoutOpened) {
      // TODO: New toolbar. We probably want to show snackbars when reply layout is opened. Need to remove this check.
      // Do not show the snackbar when the reply layout is opened
      return
    }

    _snackbar?.view?.let { snackbarView ->
      val bottomInset = globalWindowInsetsManager.bottom()

      if (!KurobaBottomNavigationView.isBottomNavViewEnabled()) {
        snackbarView.translationY = -(MARGIN.toFloat() + bottomInset)
      } else {
        val bottomNavViewSize = AppModuleAndroidUtils.getDimen(R.dimen.navigation_view_size)
        snackbarView.translationY = -(bottomNavViewSize + MARGIN + bottomInset).toFloat()
      }
    }

    _snackbar?.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
      override fun onShown(transientBottomBar: Snackbar) {
        super.onShown(transientBottomBar)

        globalUiStateHolder.updateSnackbarState {
          updateSnackbarVisibility(snackbarClass, true)
        }
      }

      override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
        super.onDismissed(transientBottomBar, event)

        val view = transientBottomBar.view
        _snackbar?.removeCallback(this)

        view.post {
          globalUiStateHolder.updateSnackbarState {
            updateSnackbarVisibility(snackbarClass, false)
          }
        }
      }
    })

    _snackbar?.show()
  }

  companion object {
    private val MARGIN = dp(8f)

    private val allowedDurations = setOf(
      Snackbar.LENGTH_INDEFINITE,
      Snackbar.LENGTH_SHORT,
      Snackbar.LENGTH_LONG
    )

    @JvmStatic
    fun create(
      theme: ChanTheme,
      view: View,
      textId: Int,
      duration: Int
    ): SnackbarWrapper {
      require(duration in allowedDurations) { "Bad duration '${duration}'" }

      val snackbar = Snackbar.make(view, textId, duration)
      snackbar.isGestureInsetBottomIgnored = true
      snackbar.animationMode = Snackbar.ANIMATION_MODE_FADE

      fixSnackbarColors(theme, snackbar)
      return SnackbarWrapper(snackbar)
    }

    @JvmStatic
    fun create(
      theme: ChanTheme,
      view: View,
      text: String,
      duration: Int
    ): SnackbarWrapper {
      require(duration in allowedDurations) { "Bad duration '${duration}'" }

      val snackbar = Snackbar.make(view, text, duration)
      snackbar.isGestureInsetBottomIgnored = true
      snackbar.animationMode = Snackbar.ANIMATION_MODE_FADE

      fixSnackbarColors(theme, snackbar)
      return SnackbarWrapper(snackbar)
    }

    private fun fixSnackbarColors(theme: ChanTheme, snackbar: Snackbar) {
      val isDarkColor = isDarkColor(theme.primaryColor)
      if (isDarkColor) {
        snackbar.setTextColor(Color.WHITE)
        snackbar.setActionTextColor(Color.WHITE)
      } else {
        snackbar.setTextColor(Color.BLACK)
        snackbar.setActionTextColor(Color.BLACK)
      }
    }
  }
}

enum class SnackbarClass {
  Catalog,
  Thread,
  Generic;

  companion object {
    fun from(threadControllerType: ThreadControllerType): SnackbarClass {
      return when (threadControllerType) {
        ThreadControllerType.Catalog -> SnackbarClass.Catalog
        ThreadControllerType.Thread -> SnackbarClass.Thread
      }
    }
  }
}