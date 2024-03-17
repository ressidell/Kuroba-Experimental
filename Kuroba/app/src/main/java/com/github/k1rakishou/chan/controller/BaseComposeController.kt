package com.github.k1rakishou.chan.controller

import android.content.Context
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.github.k1rakishou.chan.core.manager.GlobalWindowInsetsManager
import com.github.k1rakishou.chan.core.manager.WindowInsetsListener
import com.github.k1rakishou.chan.features.toolbar_v2.BackArrowMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMiddleContent
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarText
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.chan.ui.compose.providers.ProvideEverythingForCompose
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.core_themes.ThemeEngine
import javax.inject.Inject

abstract class BaseComposeController<VM : ViewModel>(
    context: Context,
    @StringRes private val titleStringId: Int
) : Controller(context), WindowInsetsListener {

    @Inject
    lateinit var themeEngine: ThemeEngine
    @Inject
    lateinit var globalWindowInsetsManager: GlobalWindowInsetsManager

    protected val controllerViewModel by lazy { controllerVM() }

    protected val controllerPaddingsState = mutableStateOf(PaddingValues())

    final override fun onCreate() {
        super.onCreate()

        globalWindowInsetsManager.addInsetsUpdatesListener(this)

        setupNavigation()
        onInsetsChanged()
        onPrepare()

        view = ComposeView(context).apply {
            setContent {
                ProvideEverythingForCompose {
                    val chanTheme = LocalChanTheme.current

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(chanTheme.backColorCompose)
                    ) {
                        BuildContent()
                    }
                }
            }
        }
    }

    open fun setupNavigation() {
        updateNavigationFlags(DeprecatedNavigationFlags(swipeable = false))

        toolbarState.enterDefaultMode(
            leftItem = BackArrowMenuItem(
                onClick = {
                    // TODO: New toolbar
                }
            ),
            middleContent = ToolbarMiddleContent.Title(
                title = ToolbarText.Id(titleStringId),
                subtitle = null
            )
        )
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()

        globalWindowInsetsManager.removeInsetsUpdatesListener(this)
    }

    override fun onInsetsChanged() {
        var toolbarHeight = with(appResources.composeDensity) { toolbarState.toolbarHeight?.toPx()?.toInt() }
        if (toolbarHeight == null) {
            toolbarHeight = appResources.dimension(com.github.k1rakishou.chan.R.dimen.toolbar_height).toInt()
        }

        val bottomPaddingDp = calculateBottomPaddingForRecyclerInDp(
            globalWindowInsetsManager = globalWindowInsetsManager,
            mainControllerCallbacks = null
        )

        Snapshot.withMutableSnapshot {
            controllerPaddingsState.value = PaddingValues(
                top = AppModuleAndroidUtils.pxToDp(toolbarHeight).dp,
                bottom = bottomPaddingDp.dp
            )
        }
    }

    open fun onPrepare() {

    }

    abstract fun controllerVM(): VM

    @Composable
    abstract fun BuildContent()

}