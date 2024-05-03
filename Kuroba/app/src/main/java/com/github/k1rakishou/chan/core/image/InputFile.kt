package com.github.k1rakishou.chan.core.image

import android.net.Uri
import com.github.k1rakishou.common.StringUtils
import java.io.File

sealed class InputFile {

  fun extension(): String? {
    when (this) {
      is FileUri -> {
        return uri.lastPathSegment
          ?.let { lastPathSegment -> StringUtils.extractFileNameExtension(lastPathSegment) }
      }
      is JavaFile -> {
        return StringUtils.extractFileNameExtension(file.name)
      }
    }
  }

  fun fileName(): String? {
    when (this) {
      is FileUri -> {
        return uri.lastPathSegment
      }
      is JavaFile -> {
        return file.name
      }
    }
  }

  fun path(): String {
    when (this) {
      is FileUri -> {
        return uri.toString()
      }
      is JavaFile -> {
        return file.absolutePath
      }
    }
  }

  data class JavaFile(val file: File) : InputFile()

  data class FileUri(val uri: Uri) : InputFile()
}