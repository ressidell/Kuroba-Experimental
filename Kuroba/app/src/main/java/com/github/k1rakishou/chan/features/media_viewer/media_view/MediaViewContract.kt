package com.github.k1rakishou.chan.features.media_viewer.media_view

import android.view.View
import com.github.k1rakishou.chan.features.media_viewer.ViewableMedia
import com.github.k1rakishou.chan.ui.view.floating_menu.FloatingListMenuItem

interface MediaViewContract {
  fun changeMediaViewerBackgroundAlpha(newAlpha: Float)

  fun toggleSoundMuteState()
  fun isSoundCurrentlyMuted(): Boolean

  fun onTapped()
  fun closeMediaViewer()
  suspend fun onDownloadButtonClick(viewableMedia: ViewableMedia, longClick: Boolean): Boolean
  fun onOptionsButtonClick(viewableMedia: ViewableMedia)
  fun onMediaLongClick(view: View, viewableMedia: ViewableMedia, mediaLongClickOptions: List<FloatingListMenuItem>)
}