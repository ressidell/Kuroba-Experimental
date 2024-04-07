package com.github.k1rakishou.chan.ui.controller

import android.content.Context
import android.graphics.Color
import android.util.AndroidRuntimeException
import android.webkit.WebView
import android.widget.TextView
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.di.component.activity.ActivityComponent
import com.github.k1rakishou.chan.features.toolbar_v2.BackArrowMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMiddleContent
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarText
import com.github.k1rakishou.chan.ui.controller.base.Controller
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils

class LicensesController(
  context: Context,
  private val title: String,
  private val url: String
) : Controller(context) {

  override fun injectDependencies(component: ActivityComponent) {
    component.inject(this)
  }

  override fun onCreate() {
    super.onCreate()

    toolbarState.enterDefaultMode(
      leftItem = BackArrowMenuItem(
        onClick = { requireNavController().popController() }
      ),
      middleContent = ToolbarMiddleContent.Title(
        title = ToolbarText.String(title)
      )
    )

    try {
      val webView = WebView(context)
      webView.loadUrl(url)
      webView.setBackgroundColor(Color.WHITE)
      view = webView
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
}
