package com.github.k1rakishou.chan.ui.compose

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan.core.manager.ThreadPostSearchManager
import com.github.k1rakishou.chan.ui.compose.components.IconTint
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeCard
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeIcon
import com.github.k1rakishou.chan.ui.compose.components.kurobaClickable
import com.github.k1rakishou.chan.ui.compose.providers.ComposeEntrypoint
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.chan.ui.compose.providers.LocalContentPaddings
import com.github.k1rakishou.chan.ui.controller.base.ControllerKey
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.core_themes.ThemeEngine
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import javax.inject.Inject

class ThreadSearchNavigationButtonsView @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  defStyle: Int = 0
) : FrameLayout(context, attributeSet, defStyle) {
  private val _shown = mutableStateOf(false)
  private val _parameters = mutableStateOf<Parameters?>(null)

  @Inject
  lateinit var threadPostSearchManager: ThreadPostSearchManager

  init {
    AppModuleAndroidUtils.extractActivityComponent(context)
      .inject(this)

    addView(
      ComposeView(context).apply {
        setContent {
          ComposeEntrypoint {
            ThreadSearchNavigationButtons()
          }
        }
      }
    )
  }

  fun updateParameters(parameters: Parameters) {
    _parameters.value = parameters
  }

  fun show() {
    _shown.value = true
  }

  fun hide() {
    _shown.value = false
  }

  @Composable
  private fun ThreadSearchNavigationButtons() {
    val parametersMut by _parameters
    val parameters = parametersMut
    val shown by _shown

    val chanTheme = LocalChanTheme.current
    val contentPaddings = LocalContentPaddings.current

    val iconTint = remember(key1 = chanTheme.primaryColorCompose) {
      val color = if (ThemeEngine.isDarkColor(chanTheme.primaryColorCompose)) {
        Color.White
      } else {
        Color.Black
      }

      return@remember IconTint.TintWithColor(color)
    }

    AnimatedVisibility(
      visible = parameters != null && shown,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      Column {
        Column(
          modifier = Modifier
            .width(48.dp)
            .height(112.dp)
        ) {
          KurobaComposeCard(
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .kurobaClickable(
                bounded = true,
                onClick = {
                  if (parameters?.chanDescriptor != null) {
                    threadPostSearchManager.goToPrevious(parameters.chanDescriptor)
                  }
                }
              ),
            backgroundColor = chanTheme.primaryColorCompose
          ) {
            KurobaComposeIcon(
              modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = 90f },
              drawableId = com.github.k1rakishou.chan.R.drawable.ic_chevron_left_black_24dp,
              iconTint = iconTint
            )
          }

          Spacer(modifier = Modifier.weight(1f))

          KurobaComposeCard(
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .kurobaClickable(
                bounded = true,
                onClick = {
                  if (parameters?.chanDescriptor != null) {
                    threadPostSearchManager.goToNext(parameters.chanDescriptor)
                  }
                }
              ),
            backgroundColor = chanTheme.primaryColorCompose
          ) {
            KurobaComposeIcon(
              modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = 270f },
              drawableId = com.github.k1rakishou.chan.R.drawable.ic_chevron_left_black_24dp,
              iconTint = iconTint
            )
          }
        }

        if (parameters != null) {
          val bottomPadding = remember(key1 = parameters.controllerKey, key2 = contentPaddings) {
            contentPaddings.calculateBottomPadding(parameters.controllerKey)
          }

          Spacer(modifier = Modifier.height(bottomPadding))
        }
      }
    }
  }

  data class Parameters(
    val chanDescriptor: ChanDescriptor?,
    val controllerKey: ControllerKey
  )

}