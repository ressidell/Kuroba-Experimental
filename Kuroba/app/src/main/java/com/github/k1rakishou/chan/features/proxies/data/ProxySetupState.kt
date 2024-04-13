package com.github.k1rakishou.chan.features.proxies.data

sealed class ProxySetupState {
  data object Uninitialized : ProxySetupState()
  data object Empty : ProxySetupState()
  data class Data(val proxyEntryViewList: List<ProxyEntryView>) : ProxySetupState()
}