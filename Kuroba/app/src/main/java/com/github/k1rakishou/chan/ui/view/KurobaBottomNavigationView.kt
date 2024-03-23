package com.github.k1rakishou.chan.ui.view

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.ComposeView
import com.github.k1rakishou.BottomNavViewButton
import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.base.KurobaCoroutineScope
import com.github.k1rakishou.chan.features.toolbar_v2.KurobaToolbarView
import com.github.k1rakishou.chan.ui.compose.bottom_panel.KurobaComposeIconPanel
import com.github.k1rakishou.chan.ui.globalstate.GlobalUiStateHolder
import com.github.k1rakishou.chan.ui.widget.SimpleAnimatorListener
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.chan.utils.setAlphaFast
import com.github.k1rakishou.common.updatePaddings
import com.github.k1rakishou.core_themes.ChanTheme
import com.github.k1rakishou.persist_state.PersistableChanState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class KurobaBottomNavigationView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), NavigationViewContract {

  @Inject
  lateinit var globalUiStateHolder: GlobalUiStateHolder

  private var attachedToWindow = false
  private var attachedToToolbar = false
  private var animating = false
  private var lastScrollTransitionProgress = 1f
  private var isTranslationLocked = false
  private var isCollapseLocked = false
  private var isBottomNavViewEnabled = ChanSettings.isNavigationViewEnabled()
  private var menuItemClickListener: ((Int) -> Boolean)? = null

  private val coroutineScope = KurobaCoroutineScope()

  private val kurobaComposeIconPanel by lazy {
    KurobaComposeIconPanel(
      context = context,
      orientation = KurobaComposeIconPanel.Orientation.Horizontal,
      defaultSelectedMenuItemId = R.id.action_browse,
      menuItems = bottomNavViewButtons()
    )
  }

  override val actualView: ViewGroup
    get() = this
  override val type: NavigationViewContract.Type
    get() = NavigationViewContract.Type.BottomNavView

  override var viewElevation: Float
    get() = elevation
    set(value) { elevation = value }
  override var selectedMenuItemId: Int
    get() = kurobaComposeIconPanel.selectedMenuItemId
    set(value) { kurobaComposeIconPanel.setMenuItemSelected(value) }

  init {
    if (!isInEditMode) {
      AppModuleAndroidUtils.extractActivityComponent(context)
        .inject(this)
    }

    setOnApplyWindowInsetsListener(null)

    if (!isBottomNavViewEnabled) {
      completelyDisableBottomNavigationView()
    } else {
      removeAllViews()
      addView(ComposeView(context).also { composeView -> composeView.setContent { BuildContent() } })
    }
  }

  @Composable
  private fun BuildContent() {
    kurobaComposeIconPanel.BuildPanel(
      onMenuItemClicked = { menuItemId -> menuItemClickListener?.invoke(menuItemId) }
    )
  }

  override fun setMenuItemSelected(menuItemId: Int) {
    kurobaComposeIconPanel.setMenuItemSelected(menuItemId)
  }

  override fun updateBadge(menuItemId: Int, menuItemBadgeInfo: KurobaComposeIconPanel.MenuItemBadgeInfo?) {
    kurobaComposeIconPanel.updateBadge(menuItemId, menuItemBadgeInfo)
  }

  override fun updatePaddings(leftPadding: Int?, bottomPadding: Int?) {
    bottomPadding?.let { padding -> updatePaddings(bottom = padding) }
  }

  override fun onThemeChanged(chanTheme: ChanTheme) {
    this.setBackgroundColor(chanTheme.primaryColor)
  }

  override fun hide(lockTranslation: Boolean, lockCollapse: Boolean) {
    if (ChanSettings.getCurrentLayoutMode() == ChanSettings.LayoutMode.SPLIT) {
      throw IllegalStateException("The nav bar should always be visible when using SPLIT layout")
    }

    if (!isBottomNavViewEnabled) {
      completelyDisableBottomNavigationView()
      return
    }

    onCollapseAnimationInternal(collapse = true, isFromToolbarCallbacks = false)

    if (lockTranslation) {
      isTranslationLocked = true
    }

    if (lockCollapse) {
      isCollapseLocked = true
    }
  }

  override fun show(unlockTranslation: Boolean, unlockCollapse: Boolean) {
    if (ChanSettings.getCurrentLayoutMode() == ChanSettings.LayoutMode.SPLIT) {
      throw IllegalStateException("The nav bar should always be visible when using SPLIT layout")
    }

    if (!isBottomNavViewEnabled) {
      completelyDisableBottomNavigationView()
      return
    }

    if (unlockTranslation) {
      isTranslationLocked = false
    }

    if (unlockCollapse) {
      isCollapseLocked = false
    }

    onCollapseAnimationInternal(collapse = false, isFromToolbarCallbacks = false)
  }

  override fun resetState(unlockTranslation: Boolean, unlockCollapse: Boolean) {
    if (!isBottomNavViewEnabled) {
      completelyDisableBottomNavigationView()
      return
    }

    isTranslationLocked = !unlockTranslation
    isCollapseLocked = !unlockCollapse

    restoreHeightWithAnimation()
  }

  fun isFullyVisible(): Boolean {
    return alpha >= 0.99f && isBottomNavViewEnabled
  }

  override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
    if (!isFullyVisible()) {
      // Steal event from children
      return true
    }

    return super.onInterceptTouchEvent(ev)
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    if (!isFullyVisible()) {
      // Pass event to other views
      return false
    }

    return super.onTouchEvent(event)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    attachedToWindow = true

    if (!attachedToToolbar) {
      attachedToToolbar = true
    }

    coroutineScope.launch {
      globalUiStateHolder.toolbarState.toolbarVisibilityStateFlow()
        .onEach { toolbarVisible ->
          if (!isBottomNavViewEnabled) {
            completelyDisableBottomNavigationView()
            return@onEach
          }

          onCollapseAnimationInternal(
            collapse = !toolbarVisible,
            isFromToolbarCallbacks = true
          )
        }
        .collect()
    }

    coroutineScope.launch {
      snapshotFlow { globalUiStateHolder.scrollState.scrollTransitionProgress.floatValue }
        .onEach { scrollTransitionProgress ->
          if (!isBottomNavViewEnabled) {
            completelyDisableBottomNavigationView()
            return@onEach
          }

          onCollapseTranslation(scrollTransitionProgress)
        }
        .collect()
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    attachedToWindow = false

    if (attachedToToolbar) {
      attachedToToolbar = false
    }

    coroutineScope.cancelChildren()
  }

  private fun onCollapseTranslation(scrollTransitionProgress: Float) {
    lastScrollTransitionProgress = scrollTransitionProgress

    if (isCollapseLocked) {
      return
    }

    val newAlpha = scrollTransitionProgress
    if (newAlpha == alpha) {
      return
    }

    if (animating) {
      return
    }

    setAlphaFast(newAlpha)
  }

  override fun setOnNavigationItemSelectedListener(listener: (Int) -> Boolean) {
    this.menuItemClickListener = listener
  }

  private fun onCollapseAnimationInternal(collapse: Boolean, isFromToolbarCallbacks: Boolean) {
    if (isFromToolbarCallbacks) {
      lastScrollTransitionProgress = if (collapse) {
        1f
      } else {
        0f
      }
    }

    if (isTranslationLocked) {
      return
    }

    val newAlpha = if (collapse) {
      0f
    } else {
      1f
    }

    if (newAlpha == alpha) {
      return
    }

    animate().cancel()

    animate()
      .alpha(newAlpha)
      .setDuration(KurobaToolbarView.ToolbarAnimationDurationMs)
      .setInterpolator(KurobaToolbarView.ToolbarAnimationInterpolator)
      .setListener(object : SimpleAnimatorListener() {
        override fun onAnimationEnd(animation: Animator) {
          animating = false
        }

        override fun onAnimationCancel(animation: Animator) {
          animating = false
        }

        override fun onAnimationStart(animation: Animator) {
          animating = true
        }
      })
      .start()
  }

  private fun restoreHeightWithAnimation() {
    if (isTranslationLocked || isCollapseLocked) {
      return
    }

    val newAlpha = lastScrollTransitionProgress
    if (newAlpha == alpha) {
      return
    }

    animate().cancel()

    animate()
      .alpha(newAlpha)
      .setDuration(KurobaToolbarView.ToolbarAnimationDurationMs)
      .setInterpolator(KurobaToolbarView.ToolbarAnimationInterpolator)
      .start()
  }

  private fun completelyDisableBottomNavigationView() {
    if (visibility == View.GONE) {
      return
    }

    visibility = View.GONE
    isTranslationLocked = true
    isCollapseLocked = true
    setAlphaFast(0f)
  }

  companion object {

    fun isBottomNavViewEnabled(): Boolean {
      if (ChanSettings.getCurrentLayoutMode() == ChanSettings.LayoutMode.SPLIT) {
        return false
      }

      return ChanSettings.isNavigationViewEnabled()
    }

    fun bottomNavViewButtons(): List<KurobaComposeIconPanel.MenuItem> {
      val bottomNavViewButtons = PersistableChanState.reorderableBottomNavViewButtons.get()

      return bottomNavViewButtons.bottomNavViewButtons().map { bottomNavViewButton ->
        return@map when (bottomNavViewButton) {
          BottomNavViewButton.Search -> {
            KurobaComposeIconPanel.MenuItem(
              id = R.id.action_search,
              iconId = R.drawable.ic_search_white_24dp
            )
          }
          BottomNavViewButton.Archive -> {
            KurobaComposeIconPanel.MenuItem(
              id = R.id.action_archive,
              iconId = R.drawable.ic_baseline_archive_24
            )
          }
          BottomNavViewButton.Bookmarks -> {
            KurobaComposeIconPanel.MenuItem(
              id = R.id.action_bookmarks,
              iconId = R.drawable.ic_bookmark_white_24dp
            )
          }
          BottomNavViewButton.Browse -> {
            KurobaComposeIconPanel.MenuItem(
              id = R.id.action_browse,
              iconId = R.drawable.ic_baseline_laptop
            )
          }
          BottomNavViewButton.Settings -> {
            KurobaComposeIconPanel.MenuItem(
              id = R.id.action_settings,
              iconId = R.drawable.ic_baseline_settings
            )
          }
        }
      }
    }

  }

}