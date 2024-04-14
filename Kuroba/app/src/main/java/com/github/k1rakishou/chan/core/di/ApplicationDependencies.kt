package com.github.k1rakishou.chan.core.di

import com.github.k1rakishou.chan.Chan
import com.github.k1rakishou.chan.ui.globalstate.GlobalUiStateHolder
import com.github.k1rakishou.chan.ui.helper.AppResources
import com.github.k1rakishou.core_themes.ThemeEngine

interface ApplicationDependencies {
  val application: Chan
  val themeEngine: ThemeEngine
  val globalUiStateHolder: GlobalUiStateHolder
  val appResources: AppResources
}