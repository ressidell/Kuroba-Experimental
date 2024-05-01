package com.github.k1rakishou.chan.core.di.module.shared

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

interface ViewModelAssistedFactory<T : ViewModel> {
  fun create(handle: SavedStateHandle): T
}