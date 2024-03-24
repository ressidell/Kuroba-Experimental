package com.github.k1rakishou.chan.features.toolbar_v2

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.github.k1rakishou.chan.controller.Controller
import com.github.k1rakishou.chan.core.manager.GlobalWindowInsetsManager
import com.github.k1rakishou.chan.ui.compose.providers.ComposeEntrypoint
import com.github.k1rakishou.chan.ui.controller.FloatingListMenuController
import com.github.k1rakishou.chan.ui.view.floating_menu.CheckableFloatingListMenuItem
import com.github.k1rakishou.chan.ui.view.floating_menu.FloatingListMenuItem
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import javax.inject.Inject


class KurobaToolbarView @JvmOverloads constructor(
  context: Context,
  attrSet: AttributeSet? = null,
  defAttrStyle: Int = 0
) : FrameLayout(context, attrSet, defAttrStyle) {

  @Inject
  lateinit var globalWindowInsetsManager: GlobalWindowInsetsManager

  private val _kurobaToolbarState = mutableStateOf<KurobaToolbarState?>(null)
  private var attachedController: Controller? = null

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

  fun init(controller: Controller) {
    _kurobaToolbarState.value = controller.toolbarState
    attachedController = controller
  }

  fun init(kurobaToolbarState: KurobaToolbarState) {
    _kurobaToolbarState.value = kurobaToolbarState
  }

  private fun showFloatingMenu(
    menuItems: List<AbstractToolbarMenuOverflowItem>,
    context: Context
  ) {
    val controller = attachedController ?: return

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

  companion object {
    val ToolbarAnimationInterpolator = FastOutSlowInInterpolator()
    const val ToolbarAnimationDurationMs = 175L
  }
  
}