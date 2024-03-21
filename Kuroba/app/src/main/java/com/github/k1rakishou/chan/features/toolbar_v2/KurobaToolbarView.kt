package com.github.k1rakishou.chan.features.toolbar_v2

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.github.k1rakishou.chan.controller.Controller
import com.github.k1rakishou.chan.ui.compose.providers.ComposeEntrypoint


class KurobaToolbarView @JvmOverloads constructor(
  context: Context,
  attrSet: AttributeSet? = null,
  defAttrStyle: Int = 0
) : FrameLayout(context, attrSet, defAttrStyle) {
  private val _kurobaToolbarState = mutableStateOf<KurobaToolbarState?>(null)

  init {
    addView(
      ComposeView(context).also { composeView ->
        composeView.setContent {
          ComposeEntrypoint {
            val kurobaToolbarState by _kurobaToolbarState
            KurobaToolbar(kurobaToolbarState = kurobaToolbarState)
          }
        }
      }
    )
  }
  
  fun init(controller: Controller) {
    _kurobaToolbarState.value = controller.toolbarState
  }

  fun init(kurobaToolbarState: KurobaToolbarState) {
    _kurobaToolbarState.value = kurobaToolbarState
  }

  companion object {
    val ToolbarAnimationInterpolator = FastOutSlowInInterpolator()
    const val ToolbarAnimationDurationMs = 175L
  }
  
}