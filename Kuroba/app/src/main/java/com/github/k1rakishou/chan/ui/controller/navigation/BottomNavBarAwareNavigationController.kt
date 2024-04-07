package com.github.k1rakishou.chan.ui.controller.navigation

import android.content.Context
import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.di.component.activity.ActivityComponent
import com.github.k1rakishou.chan.core.manager.GlobalWindowInsetsManager
import com.github.k1rakishou.chan.core.manager.WindowInsetsListener
import com.github.k1rakishou.chan.features.toolbar_v2.KurobaToolbarView
import com.github.k1rakishou.chan.ui.controller.base.transition.ControllerTransition
import com.github.k1rakishou.chan.ui.controller.base.ui.NavigationControllerContainerLayout
import com.github.k1rakishou.chan.ui.view.NavigationViewContract
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.getDimen
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.inflate
import com.github.k1rakishou.common.updatePaddings
import com.github.k1rakishou.core_themes.ThemeEngine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class BottomNavBarAwareNavigationController(
  context: Context,
  private val navigationViewType: NavigationViewContract.Type,
  private val listener: CloseBottomNavBarAwareNavigationControllerListener
) :
  ToolbarNavigationController(context),
  WindowInsetsListener {

  @Inject
  lateinit var themeEngine: ThemeEngine
  @Inject
  lateinit var globalWindowInsetsManager: GlobalWindowInsetsManager

  private lateinit var toolbar: KurobaToolbarView

  override fun injectDependencies(component: ActivityComponent) {
    component.inject(this)
  }

  override fun onCreate() {
    super.onCreate()

    view = inflate(context, R.layout.controller_navigation_bottom_nav_bar_aware)
    container = view.findViewById<NavigationControllerContainerLayout>(R.id.bottom_bar_aware_controller_container)

    toolbar = view.findViewById<KurobaToolbarView>(R.id.toolbar)
    toolbar.init(this)
    containerToolbarState.enterContainerMode()

    // Wait a little bit so that GlobalWindowInsetsManager have time to get initialized so we can
    // use the insets
    view.post {
      onInsetsChanged()
      globalWindowInsetsManager.addInsetsUpdatesListener(this)
    }

    controllerScope.launch {
      containerToolbarState.toolbarHeightState
        .onEach { onInsetsChanged() }
        .collect()
    }
  }

  override fun onDestroy() {
    super.onDestroy()

    if (::toolbar.isInitialized) {
      toolbar.destroy()
    }

    globalWindowInsetsManager.removeInsetsUpdatesListener(this)
  }

  override fun onInsetsChanged() {
    val bottomPadding = if (ChanSettings.isBottomNavigationPresent()) {
      globalWindowInsetsManager.bottom() + getDimen(R.dimen.navigation_view_size)
    } else {
      0
    }

    val toolbarHeight = with(appResources.composeDensity) {
      containerToolbarState.toolbarHeightState.value?.toPx()
    }?.toInt() ?: 0

    container?.updatePaddings(
      top = toolbarHeight,
      bottom = bottomPadding
    )
  }

  override fun popController(transition: ControllerTransition?): Boolean {
    if (childControllers.size > 1) {
      return super.popController(transition)
    }

    // TODO: New toolbar. This has a bug where the toolbar is not updated after closing the BottomNavBarAwareNavigationController
    listener.onCloseController()
    return true
  }

  interface CloseBottomNavBarAwareNavigationControllerListener {
    fun onCloseController()
    fun onShowMenu()
  }
}