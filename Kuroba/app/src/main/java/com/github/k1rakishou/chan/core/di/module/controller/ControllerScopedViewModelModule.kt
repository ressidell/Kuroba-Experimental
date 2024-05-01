package com.github.k1rakishou.chan.core.di.module.controller

import androidx.lifecycle.ViewModel
import com.github.k1rakishou.chan.core.di.key.ViewModelKey
import com.github.k1rakishou.chan.core.di.module.shared.ViewModelAssistedFactory
import com.github.k1rakishou.chan.core.di.scope.PerController
import com.github.k1rakishou.chan.features.album.AlbumViewControllerV2ViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ControllerScopedViewModelModule {

  @IntoMap
  @ViewModelKey(AlbumViewControllerV2ViewModel::class)
  @Binds
  @PerController
  abstract fun bindAlbumViewControllerV2ViewModel(
    impl: AlbumViewControllerV2ViewModel.ViewModelFactory
  ): ViewModelAssistedFactory<out ViewModel>

}