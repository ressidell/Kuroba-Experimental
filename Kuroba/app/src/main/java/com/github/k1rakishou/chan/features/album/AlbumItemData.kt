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
  val albumItemPostData: AlbumViewControllerViewModel.AlbumItemPostData?,
  val mediaType: KurobaMediaType,
  val downloadUniqueId: String?
) {
  val composeKey: String
    get() {
      val fullImageUrl = fullImageUrl
      if (fullImageUrl != null) {
        return "${postDescriptor.userReadableString()}, ${thumbnailImageUrl}, ${fullImageUrl}"
      }

      return "${postDescriptor.userReadableString()}, ${thumbnailImageUrl}"
    }

  val fullImageUrlString: String? = fullImageUrl?.toString()

  val albumItemDataKey: AlbumViewControllerViewModel.AlbumItemDataKey = AlbumViewControllerViewModel.AlbumItemDataKey(
    postDescriptor = postDescriptor,
    thumbnailImageUrl = thumbnailImageUrl,
    fullImageUrl = fullImageUrl
  )

  fun isEqualToChanPostImage(chanPostImage: ChanPostImage): Boolean {
    return fullImageUrl == chanPostImage.imageUrl &&
      postDescriptor == chanPostImage.ownerPostDescriptor
  }
}