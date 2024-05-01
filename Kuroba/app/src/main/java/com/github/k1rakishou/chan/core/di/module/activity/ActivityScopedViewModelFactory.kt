package com.github.k1rakishou.chan.core.di.module.activity

import androidx.lifecycle.ViewModel
import com.github.k1rakishou.chan.core.di.module.shared.AbstractViewModelFactory
import com.github.k1rakishou.chan.core.di.module.shared.ViewModelAssistedFactory
import javax.inject.Inject

class ActivityScopedViewModelFactory @Inject constructor(
  creators: Map<@JvmSuppressWildcards Class<out ViewModel>, @JvmSuppressWildcards ViewModelAssistedFactory<out ViewModel>>,
  activityScopedSavedStateRegistryOwner: ActivityScopedSavedStateRegistryOwner
) : AbstractViewModelFactory(creators, activityScopedSavedStateRegistryOwner.savedStateRegistryOwner)