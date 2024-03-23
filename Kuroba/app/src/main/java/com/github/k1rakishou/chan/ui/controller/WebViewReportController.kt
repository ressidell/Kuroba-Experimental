package com.github.k1rakishou.chan.ui.controller

import android.annotation.SuppressLint
import android.content.Context
import android.util.AndroidRuntimeException
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.controller.Controller
import com.github.k1rakishou.chan.core.di.component.activity.ActivityComponent
import com.github.k1rakishou.chan.core.navigation.RequiresNoBottomNavBar
import com.github.k1rakishou.chan.core.site.Site
import com.github.k1rakishou.chan.features.toolbar_v2.BackArrowMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMiddleContent
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarText
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.common.updatePaddings
import com.github.k1rakishou.model.data.post.ChanPost
import com.github.k1rakishou.model.util.ChanPostUtils.getTitle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class WebViewReportController(
  context: Context,
  private val post: ChanPost,
  private val site: Site
) : Controller(context), RequiresNoBottomNavBar {
  private lateinit var frameLayout: FrameLayout

  override fun injectDependencies(component: ActivityComponent) {
    component.inject(this)
  }

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate() {
    super.onCreate()

    toolbarState.enterDefaultMode(
      leftItem = BackArrowMenuItem(
        onClick = {
          // TODO: New toolbar
        }
      ),
      middleContent = ToolbarMiddleContent.Title(
        title = ToolbarText.String(appResources.string(R.string.report_screen, getTitle(post, null)))
      )
    )

    controllerScope.launch {
      toolbarState.toolbarHeightState
        .onEach { updatePaddings() }
        .collect()
    }

    val url = site.endpoints().report(post)

    frameLayout = FrameLayout(context)
    frameLayout.setLayoutParams(
      LinearLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
      )
    )

    try {
      val webView = WebView(context)
      val siteRequestModifier = site.requestModifier()

      if (siteRequestModifier != null) {
        siteRequestModifier.modifyWebView(webView)
      }

      val settings = webView.getSettings()
      settings.javaScriptEnabled = true
      settings.domStorageEnabled = true
      webView.loadUrl(url.toString())

      frameLayout.addView(
        webView,
        FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.MATCH_PARENT,
          FrameLayout.LayoutParams.MATCH_PARENT
        )
      )

      view = frameLayout
      updatePaddings()
    } catch (error: Throwable) {
      var errmsg = ""

      if (
        error is AndroidRuntimeException &&
        error.message != null &&
        error.message?.contains("MissingWebViewPackageException") == true
        ) {
        errmsg = appResources.string(R.string.fail_reason_webview_is_not_installed)
      } else {
        errmsg = appResources.string(R.string.fail_reason_some_part_of_webview_not_initialized, error.message ?: "Unknown error")
      }

      view = AppModuleAndroidUtils.inflate(context, R.layout.layout_webview_error)
      view.findViewById<TextView>(R.id.text).text = errmsg
    }
  }

  private fun updatePaddings() {
    val toolbarHeightDp = toolbarState.toolbarHeight

    var toolbarHeight = with(appResources.composeDensity) { toolbarHeightDp?.toPx()?.toInt() }
    if (toolbarHeight == null) {
      toolbarHeight = appResources.dimension(com.github.k1rakishou.chan.R.dimen.toolbar_height).toInt()
    }

    frameLayout.updatePaddings(
      left = null,
      right = null,
      top = toolbarHeight,
      bottom = null
    )
  }

}
