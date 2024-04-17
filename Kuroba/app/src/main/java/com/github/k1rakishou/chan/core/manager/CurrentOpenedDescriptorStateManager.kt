package com.github.k1rakishou.chan.core.manager

import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CurrentOpenedDescriptorStateManager {
  private val _currentCatalogDescriptorFlow = MutableStateFlow<ChanDescriptor.ICatalogDescriptor?>(null)
  val currentCatalogDescriptorFlow: StateFlow<ChanDescriptor.ICatalogDescriptor?>
    get() = _currentCatalogDescriptorFlow.asStateFlow()
  val currentCatalogDescriptor: ChanDescriptor.ICatalogDescriptor?
    get() = currentCatalogDescriptorFlow.value

  private val _currentThreadDescriptorFlow = MutableStateFlow<ChanDescriptor.ThreadDescriptor?>(null)
  val currentThreadDescriptorFlow: StateFlow<ChanDescriptor.ThreadDescriptor?>
    get() = _currentThreadDescriptorFlow.asStateFlow()
  val currentThreadDescriptor: ChanDescriptor.ThreadDescriptor?
    get() = currentThreadDescriptorFlow.value

  private val _currentFocusedController = MutableStateFlow<CurrentFocusedController>(
    CurrentFocusedController.None
  )
  val currentFocusedController: StateFlow<CurrentFocusedController>
    get() = _currentFocusedController.asStateFlow()

  val currentFocusedDescriptor: ChanDescriptor?
    get() {
      return when (_currentFocusedController.value) {
        CurrentFocusedController.Catalog -> currentCatalogDescriptor as ChanDescriptor?
        CurrentFocusedController.Thread -> currentThreadDescriptor
        CurrentFocusedController.None -> null
      }
    }

  fun updateCatalogDescriptor(catalogDescriptor: ChanDescriptor.ICatalogDescriptor?) {
    _currentCatalogDescriptorFlow.value = catalogDescriptor
  }

  fun updateThreadDescriptor(threadDescriptor: ChanDescriptor.ThreadDescriptor?) {
    _currentThreadDescriptorFlow.value = threadDescriptor
  }

  fun updateCurrentFocusedController(focusedController: CurrentFocusedController) {
    _currentFocusedController.value = focusedController
  }

}

enum class CurrentFocusedController {
  Catalog,
  Thread,
  None
}