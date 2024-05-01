package com.github.k1rakishou.chan.core.di.module.controller

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.github.k1rakishou.chan.core.di.module.shared.KurobaViewModelStore
import com.github.k1rakishou.chan.core.di.scope.PerController
import com.github.k1rakishou.chan.ui.controller.base.Controller
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module(includes = [ControllerScopedViewModelFactoryModule.Bindings::class])
class ControllerScopedViewModelFactoryModule {

  @PerController
  @Provides
  fun provideSavedStateRegistryOwner(controller: Controller): ControllerScopedSavedStateRegistryOwner {
    return ControllerScopedSavedStateRegistryOwner(savedStateRegistryOwner = controller)
  }

  @Module
  interface Bindings {
    @PerController
    @Binds
    fun bindViewModelFactory(impl: ControllerScopedViewModelFactory): ViewModelProvider.Factory

    @PerController
    @Binds
    fun bindViewModelStore(impl: KurobaViewModelStore): ViewModelStore
  }

}