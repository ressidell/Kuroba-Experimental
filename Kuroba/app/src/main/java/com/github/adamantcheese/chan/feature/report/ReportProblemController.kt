package com.github.adamantcheese.chan.feature.report

import android.content.Context
import com.github.adamantcheese.chan.controller.Controller
import com.github.adamantcheese.chan.ui.controller.LoadingViewController

class ReportProblemController(context: Context)
    : Controller(context), ReportProblemLayout.ReportProblemControllerCallbacks {
    private var loadingViewController: LoadingViewController? = null

    override fun onCreate() {
        view = ReportProblemLayout(context).apply {
            onReady(this@ReportProblemController)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        (view as ReportProblemLayout).destroy()
    }

    override fun showProgressDialog() {
        hideProgressDialog()

        loadingViewController = LoadingViewController(context, true)
        presentController(loadingViewController)
    }

    override fun hideProgressDialog() {
        loadingViewController?.stopPresenting()
        loadingViewController = null
    }

    override fun onFinished() {
        this.stopPresenting()
    }
}