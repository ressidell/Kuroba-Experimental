package com.github.k1rakishou.chan.features.album

import androidx.compose.runtime.Immutable
import com.github.k1rakishou.chan.utils.KurobaMediaType
import com.github.k1rakishou.model.data.descriptor.PostDescriptor
import com.github.k1rakishou.model.data.post.ChanPostImage
import okhttp3.HttpUrl

@Immutable
data class AlbumItemData(
  val id: Long,
  val isCatalogMode: Boolean,
  val postDescriptor: PostDescriptor,
  val thumbnailImageUrl: HttpUrl,
  val spoilerThumbnailImageUrl: HttpUrl?,
  val fullImageUrl: HttpUrl?,
  val albumItemPostData: AlbumViewControllerV2ViewModel.AlbumItemPostData?,
  val mediaType: KurobaMediaType,
  val downloadUniqueId: String?
) {
  val composeKey: String
    get() {
      val fullImageUrl = fullImageUrl
      if (fullImageUrl != null) {
        return "${postDescriptor.serializeToString()}_${thumbnailImageUrl}_${fullImageUrl}"
      }

      return "${postDescriptor.serializeToString()}_${thumbnailImageUrl}"
    }

  val albumItemDataKey: AlbumViewControllerV2ViewModel.AlbumItemDataKey =
    AlbumViewControllerV2ViewModel.AlbumItemDataKey(postDescriptor, thumbnailImageUrl)

  fun isEqualToChanPostImage(chanPostImage: ChanPostImage): Boolean {
    return fullImageUrl == chanPostImage.imageUrl &&
      postDescriptor == chanPostImage.ownerPostDescriptor
  }
}