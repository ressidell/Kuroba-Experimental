package com.github.k1rakishou.chan.features.toolbar

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.ComposeView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.core.base.KurobaCoroutineScope
import com.github.k1rakishou.chan.core.manager.GlobalWindowInsetsManager
import com.github.k1rakishou.chan.ui.compose.providers.ComposeEntrypoint
import com.github.k1rakishou.chan.ui.controller.FloatingListMenuController
import com.github.k1rakishou.chan.ui.controller.navigation.ContainerToolbarStateUpdatedListener
import com.github.k1rakishou.chan.ui.controller.navigation.ToolbarNavigationController
import com.github.k1rakishou.chan.ui.globalstate.GlobalUiStateHolder
import com.github.k1rakishou.chan.ui.globalstate.reply.ReplyLayoutVisibilityStates
import com.github.k1rakishou.chan.ui.view.floating_menu.CheckableFloatingListMenuItem
import com.github.k1rakishou.chan.ui.view.floating_menu.FloatingListMenuItem
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject

// TODO: New toolbar. Fix insets on all screens.
// TODO: New toolbar. Remove PopupController when using SPLIT layout mode.
//  Open child controllers in either left or right parent controller, not on top of all of them.

class KurobaToolbarView @JvmOverloads constructor(
  context: Context,
  attrSet: AttributeSet? = null,
  defAttrStyle: Int = 0
) : FrameLayout(context, attrSet, defAttrStyle), ContainerToolbarStateUpdatedListener {

  @Inject
  lateinit var globalWindowInsetsManager: GlobalWindowInsetsManager
  @Inject
  lateinit var globalUiStateHolder: GlobalUiStateHolder

  private val coroutineScope = KurobaCoroutineScope()

  private val _kurobaToolbarState = mutableStateOf<KurobaToolbarState?>(null)
  private var _attachedController: ToolbarNavigationController? = null

  private val currentToolbarState: KurobaToolbarState?
    get() = _kurobaToolbarState.value

  init {
    if (!isInEditMode) {
      AppModuleAndroidUtils.extractActivityComponent(context)
        .inject(this)
    }

    addView(
      ComposeView(context).also { composeView ->
        composeView.setContent {
          ComposeEntrypoint {
            val kurobaToolbarState by _kurobaToolbarState
            KurobaToolbar(
              kurobaToolbarState = kurobaToolbarState,
              showFloatingMenu = { menuItems -> showFloatingMenu(menuItems, context) }
            )
          }
        }
      }
    )
  }

  override fun onStateUpdated(kurobaToolbarState: KurobaToolbarState) {
    val prevToolbar = currentToolbarState
    _kurobaToolbarState.value = kurobaToolbarState
    prevToolbar?.updateToolbarAlpha(1f)
  }

  fun init(controller: ToolbarNavigationController) {
    coroutineScope.cancelChildren()

    _kurobaToolbarState.value = controller.containerToolbarState
    _attachedController = controller

    controller.addOrReplaceContainerToolbarStateUpdated(this)

    coroutineScope.launch {
      combine(
        ChanSettings.layoutMode.listenForChanges().asFlow(),
        ChanSettings.neverHideToolbar.listenForChanges().asFlow(),
        globalUiStateHolder.replyLayout.replyLayoutVisibilityEventsFlow,
        snapshotFlow { globalUiStateHolder.fastScroller.isDraggingFastScrollerState.value },
        snapshotFlow { globalUiStateHolder.scroll.scrollTransitionProgress.floatValue }
      ) { _, _, replyLayoutVisibilityStates, isDraggingFastScroller, scrollProgress ->
        return@combine ToolbarVisibilityInfo(
          replyLayoutVisibilityStates = replyLayoutVisibilityStates,
          isDraggingFastScroller = isDraggingFastScroller,
          scrollProgress = scrollProgress
        )
      }
        .onEach { toolbarVisibilityInfo ->
          if (toolbarVisibilityInfo.cannotHideToolbar()) {
            currentToolbarState?.updateToolbarAlpha(1f)
            globalUiStateHolder.updateScrollState { resetScrollState() }
            return@onEach
          }

          currentToolbarState?.updateToolbarAlpha(toolbarVisibilityInfo.toolbarAlpha)
        }
        .collect()
    }
  }

  fun init(kurobaToolbarState: KurobaToolbarState) {
    _kurobaToolbarState.value = kurobaToolbarState
  }

  fun destroy() {
    _attachedController?.let { controller ->
      controller.removeContainerToolbarStateUpdated(this@KurobaToolbarView)
    }

    coroutineScope.cancel()
    _attachedController = null
  }

  private fun showFloatingMenu(
    menuItems: List<AbstractToolbarMenuOverflowItem>,
    context: Context
  ) {
    val controller = _attachedController ?: return

    if (menuItems.isEmpty()) {
      return
    }

    controller.presentController(
      controller = FloatingListMenuController(
        context = context,
        constraintLayoutBias = globalWindowInsetsManager.lastTouchCoordinatesAsConstraintLayoutBias(),
        items = mapMenuItems(menuItems),
        itemClickListener = { clickedMenuItem ->
          val clickedAbstractMenuItem = findClickedItemRecursively(menuItems, clickedMenuItem.key)
          if (clickedAbstractMenuItem == null) {
            return@FloatingListMenuController
          }

          when (clickedAbstractMenuItem) {
            is ToolbarMenuCheckableOverflowItem -> {
              clickedAbstractMenuItem.onClick(clickedAbstractMenuItem)
            }
            is ToolbarMenuOverflowItem -> {
              clickedAbstractMenuItem.onClick?.invoke(clickedAbstractMenuItem)
            }
            else -> {
              error("Unknown clickedAbstractMenuItem: ${clickedAbstractMenuItem::class.java.simpleName}")
            }
          }
        }
      )
    )
  }

  private fun mapMenuItems(menuItems: List<AbstractToolbarMenuOverflowItem>): List<FloatingListMenuItem> {
    return menuItems.map { abstractToolbarMenuOverflowItem ->
      when (abstractToolbarMenuOverflowItem) {
        is ToolbarMenuCheckableOverflowItem -> {
          CheckableFloatingListMenuItem(
            key = abstractToolbarMenuOverflowItem.id,
            name = abstractToolbarMenuOverflowItem.menuTextState.value,
            value = abstractToolbarMenuOverflowItem.value,
            groupId = abstractToolbarMenuOverflowItem.groupId,
            visible = abstractToolbarMenuOverflowItem.visibleState.value,
            enabled = abstractToolbarMenuOverflowItem.enabledState.value,
            more = mapMenuItems(abstractToolbarMenuOverflowItem.subItems),
            checked = abstractToolbarMenuOverflowItem.checkedState.value
          )
        }
        is ToolbarMenuOverflowItem -> {
          FloatingListMenuItem(
            key = abstractToolbarMenuOverflowItem.id,
            name = abstractToolbarMenuOverflowItem.menuTextState.value,
            value = abstractToolbarMenuOverflowItem.value,
            visible = abstractToolbarMenuOverflowItem.visibleState.value,
            enabled = abstractToolbarMenuOverflowItem.enabledState.value,
            more = mapMenuItems(abstractToolbarMenuOverflowItem.subItems)
          )
        }
        else -> {
          error("Unknown abstractToolbarMenuOverflowItem: ${abstractToolbarMenuOverflowItem::class.java.simpleName}")
        }
      }
    }
  }

  private fun findClickedItemRecursively(
    menuItems: List<AbstractToolbarMenuOverflowItem>,
    needle: Any
  ): AbstractToolbarMenuOverflowItem? {
    for (item in menuItems) {
      if (item.id == needle) {
        return item
      }

      if (item.subItems.isNotEmpty()) {
        val foundItem = findClickedItemRecursively(item.subItems, needle)
        if (foundItem != null) {
          return foundItem
        }
      }
    }

    return null
  }

  private data class ToolbarVisibilityInfo(
    val replyLayoutVisibilityStates: ReplyLayoutVisibilityStates,
    val isDraggingFastScroller: Boolean,
    val scrollProgress: Float
  ) {

    val toolbarAlpha: Float
      get() = scrollProgress

    fun cannotHideToolbar(): Boolean {
      if (isDraggingFastScroller) {
        return true
      }

      if (replyLayoutVisibilityStates.anyOpenedOrExpanded()) {
        return true
      }

      if (!ChanSettings.canCollapseToolbar()) {
        return true
      }

      return false
    }
  }

  companion object {
    val ToolbarAnimationInterpolator = FastOutSlowInInterpolator()
    const val ToolbarAnimationDurationMs = 250L
  }
  
}