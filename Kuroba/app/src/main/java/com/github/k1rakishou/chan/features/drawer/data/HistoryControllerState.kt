package com.github.k1rakishou.chan.features.drawer.data

sealed class HistoryControllerState {
  data object Loading : HistoryControllerState()
  data class Error(val errorText: String) : HistoryControllerState()
  data object Data : HistoryControllerState()
}