package com.github.k1rakishou.chan.ui.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.k1rakishou.chan.ui.compose.providers.LocalWindowInsets
import com.github.k1rakishou.common.errorMessageOrClassName

@Composable
fun KurobaComposeErrorMessage(
  modifier: Modifier = Modifier.fillMaxSize(),
  error: Throwable
) {
  val errorMessage = remember(key1 = error) { error.errorMessageOrClassName() }

  KurobaComposeErrorMessage(
    errorMessage = errorMessage,
    modifier = modifier
  )
}

@Composable
fun KurobaComposeErrorMessage(
  modifier: Modifier = Modifier.fillMaxSize(),
  errorMessage: String
) {
  val windowInsets = LocalWindowInsets.current

  Box(
    modifier = Modifier
        .padding(bottom = windowInsets.bottom)
        .then(modifier)
  ) {
    KurobaComposeText(
      modifier = Modifier.align(Alignment.Center),
      text = errorMessage
    )
  }
}