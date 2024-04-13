package com.github.k1rakishou.chan.ui.view.insets

import android.content.Context
import android.util.AttributeSet
import com.airbnb.epoxy.EpoxyRecyclerView
import com.github.k1rakishou.chan.core.base.KurobaCoroutineScope
import com.github.k1rakishou.chan.core.manager.GlobalWindowInsetsManager
import com.github.k1rakishou.chan.core.manager.WindowInsetsListener
import com.github.k1rakishou.chan.ui.globalstate.GlobalUiStateHolder
import com.github.k1rakishou.chan.ui.helper.AppResources
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class InsetAwareEpoxyRecyclerView @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null
) : EpoxyRecyclerView(context, attributeSet), WindowInsetsListener {

  @Inject
  lateinit var globalWindowInsetsManager: GlobalWindowInsetsManager
  @Inject
  lateinit var globalUiStateHolder: GlobalUiStateHolder
  @Inject
  lateinit var appResources: AppResources

  private val coroutineScope = KurobaCoroutineScope()

  init {
    AppModuleAndroidUtils.extractActivityComponent(context)
      .inject(this)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    globalWindowInsetsManager.addInsetsUpdatesListener(this)

    coroutineScope.cancelChildren()
    coroutineScope.launch {
      combine(
        globalUiStateHolder.toolbar.toolbarHeight,
        globalUiStateHolder.bottomPanel.bottomPanelHeight
      ) { t1, t2 -> t1 to t2 }
        .onEach { onInsetsChanged() }
        .collect()
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    globalWindowInsetsManager.removeInsetsUpdatesListener(this)
    coroutineScope.cancelChildren()
  }

  override fun onInsetsChanged() {
    val bottomPadding = with(appResources.composeDensity) {
      maxOf(
        globalWindowInsetsManager.bottom(),
        globalUiStateHolder.bottomPanel.bottomPanelHeight.value.roundToPx()
      )
    }

    val topPadding = with(appResources.composeDensity) {
      maxOf(
        globalWindowInsetsManager.top(),
        globalUiStateHolder.toolbar.toolbarHeight.value.roundToPx()
      )
    }

    setPadding(
      paddingLeft,
      topPadding,
      paddingRight,
      bottomPadding
    )
  }

}