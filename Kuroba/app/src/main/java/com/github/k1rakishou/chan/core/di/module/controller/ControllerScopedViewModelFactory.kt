package com.github.k1rakishou.chan.core.di.module.controller

import androidx.lifecycle.ViewModel
import com.github.k1rakishou.chan.core.di.module.shared.AbstractViewModelFactory
import com.github.k1rakishou.chan.core.di.module.shared.ViewModelAssistedFactory
import javax.inject.Inject

class ControllerScopedViewModelFactory @Inject constructor(
  creators: Map<@JvmSuppressWildcards Class<out ViewModel>, @JvmSuppressWildcards ViewModelAssistedFactory<out ViewModel>>,
  controllerScopedSavedStateRegistryOwner: ControllerScopedSavedStateRegistryOwner
) : AbstractViewModelFactory(creators, controllerScopedSavedStateRegistryOwner.savedStateRegistryOwner)