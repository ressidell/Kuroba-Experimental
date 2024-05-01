package com.github.k1rakishou.chan.core.di.module.activity

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.github.k1rakishou.chan.core.di.module.shared.KurobaViewModelStore
import com.github.k1rakishou.chan.core.di.scope.PerActivity
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module(includes = [ActivityScopedViewModelFactoryModule.Bindings::class])
class ActivityScopedViewModelFactoryModule {

  @PerActivity
  @Provides
  fun provideSavedStateRegistryOwner(activity: AppCompatActivity): ActivityScopedSavedStateRegistryOwner {
    return ActivityScopedSavedStateRegistryOwner(savedStateRegistryOwner = activity)
  }

  @Module
  interface Bindings {
    @PerActivity
    @Binds
    fun bindViewModelFactory(impl: ActivityScopedViewModelFactory): ViewModelProvider.Factory

    @PerActivity
    @Binds
    fun bindViewModelStore(impl: KurobaViewModelStore): ViewModelStore
  }

}