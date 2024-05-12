package com.github.k1rakishou.chan.core.di.module.controller

import androidx.lifecycle.ViewModel
import com.github.k1rakishou.chan.core.di.key.ViewModelKey
import com.github.k1rakishou.chan.core.di.module.shared.ViewModelAssistedFactory
import com.github.k1rakishou.chan.core.di.scope.PerController
import com.github.k1rakishou.chan.features.album.AlbumViewControllerViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ControllerScopedViewModelModule {

  @IntoMap
  @ViewModelKey(AlbumViewControllerViewModel::class)
  @Binds
  @PerController
  abstract fun bindAlbumViewControllerViewModel(
    impl: AlbumViewControllerViewModel.ViewModelFactory
  ): ViewModelAssistedFactory<out ViewModel>

}