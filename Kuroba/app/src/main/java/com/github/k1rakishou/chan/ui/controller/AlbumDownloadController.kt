package com.github.k1rakishou.chan.ui.controller

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.di.component.activity.ActivityComponent
import com.github.k1rakishou.chan.core.helper.DialogFactory
import com.github.k1rakishou.chan.core.manager.GlobalWindowInsetsManager
import com.github.k1rakishou.chan.core.manager.WindowInsetsListener
import com.github.k1rakishou.chan.features.image_saver.ImageSaverV2
import com.github.k1rakishou.chan.features.image_saver.ImageSaverV2.SimpleSaveableMediaInfo
import com.github.k1rakishou.chan.features.image_saver.ImageSaverV2.SimpleSaveableMediaInfo.Companion.fromChanPostImage
import com.github.k1rakishou.chan.features.image_saver.ImageSaverV2OptionsController
import com.github.k1rakishou.chan.features.image_saver.ImageSaverV2OptionsController.Options.MultipleImages
import com.github.k1rakishou.chan.features.toolbar.BackArrowMenuItem
import com.github.k1rakishou.chan.features.toolbar.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar.ToolbarMiddleContent
import com.github.k1rakishou.chan.features.toolbar.ToolbarText
import com.github.k1rakishou.chan.ui.cell.post_thumbnail.PostImageThumbnailView
import com.github.k1rakishou.chan.ui.compose.lazylist.ScrollbarView
import com.github.k1rakishou.chan.ui.controller.base.Controller
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableFloatingActionButton
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableGridRecyclerView
import com.github.k1rakishou.chan.ui.view.ThumbnailView.ThumbnailViewOptions
import com.github.k1rakishou.chan.ui.view.insets.ColorizableInsetAwareGridRecyclerView
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.chan.utils.RecyclerUtils.clearRecyclerCache
import com.github.k1rakishou.common.updateMargins
import com.github.k1rakishou.fsaf.FileManager
import com.github.k1rakishou.model.data.post.ChanPostImage
import com.github.k1rakishou.persist_state.ImageSaverV2Options
import dagger.Lazy
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class AlbumDownloadController(context: Context) : Controller(context),
  View.OnClickListener,
  WindowInsetsListener {

  @Inject
  lateinit var imageSaverV2: Lazy<ImageSaverV2>
  @Inject
  lateinit var globalWindowInsetsManager: GlobalWindowInsetsManager
  @Inject
  lateinit var dialogFactory: DialogFactory
  @Inject
  lateinit var fileManager: FileManager

  private lateinit var recyclerView: ColorizableInsetAwareGridRecyclerView
  private lateinit var download: ColorizableFloatingActionButton
  private lateinit var scrollbarView: ScrollbarView

  private var allChecked = true
  private val items: MutableList<AlbumDownloadItem> = ArrayList()

  private val checkCount: Int
    get() {
      var checkCount = 0

      for (item in items) {
        if (item.checked) {
          checkCount++
        }
      }

      return checkCount
    }

  override fun injectActivityDependencies(component: ActivityComponent) {
    component.inject(this)
  }

  override fun onCreate() {
    super.onCreate()
    view = AppModuleAndroidUtils.inflate(context, R.layout.controller_album_download)

    toolbarState.enterDefaultMode(
      leftItem = BackArrowMenuItem(
        onClick = { requireNavController().popController() }
      ),
      middleContent = ToolbarMiddleContent.Title(
        title = ToolbarText.String("")
      ),
      menuBuilder = {
        withMenuItem(drawableId = R.drawable.ic_select_all_white_24dp, onClick = { menuItem -> onCheckAllClicked(menuItem) })
      }
    )

    updateTitle()

    download = view.findViewById<ColorizableFloatingActionButton>(R.id.download)
    download.setOnClickListener(this)

    recyclerView = view.findViewById<ColorizableInsetAwareGridRecyclerView>(R.id.recycler_view)
    recyclerView.setHasFixedSize(true)

    val gridLayoutManager = GridLayoutManager(context, 3)
    recyclerView.setLayoutManager(gridLayoutManager)
    recyclerView.setSpanWidth(AppModuleAndroidUtils.dp(90f))

    val adapter = AlbumAdapter()
    recyclerView.setAdapter(adapter)

    scrollbarView = view.findViewById(R.id.album_download_controller_scrollbar)
    scrollbarView.attachRecyclerView(recyclerView)
    scrollbarView.isScrollbarDraggable(true)

    globalWindowInsetsManager.addInsetsUpdatesListener(this)
    onInsetsChanged()

    controllerScope.launch {
      combine(
        globalUiStateHolder.toolbar.toolbarHeight,
        globalUiStateHolder.bottomPanel.bottomPanelHeight
      ) { t1, t2 -> t1 to t2 }
        .onEach { onInsetsChanged() }
        .collect()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    globalWindowInsetsManager.removeInsetsUpdatesListener(this)

    scrollbarView.cleanup()

    if (::recyclerView.isInitialized) {
      recyclerView.swapAdapter(null, true)
    }
  }

  override fun onInsetsChanged() {
    val bottomPadding = with(appResources.composeDensity) {
      maxOf(
        globalWindowInsetsManager.bottom(),
        globalUiStateHolder.bottomPanel.bottomPanelHeight.value.roundToPx()
      )
    }

    val fabAdditionalBottomPadding = with(appResources.composeDensity) { 16.dp.roundToPx() }
    val fabAdditionalRightPadding = with(appResources.composeDensity) { 16.dp.roundToPx() }

    download.updateMargins(
      bottom = bottomPadding + fabAdditionalBottomPadding,
      right = fabAdditionalRightPadding
    )
  }

  override fun onClick(v: View) {
    if (v === download) {
      onDownloadClicked()
    }
  }

  private fun onDownloadClicked() {
    val checkCount = checkCount
    if (checkCount == 0) {
      showToast(R.string.album_download_none_checked)
      return
    }

    val simpleSaveableMediaInfoList = ArrayList<SimpleSaveableMediaInfo>(items.size)

    for (item in items) {
      if (item.postImage.isInlined || item.postImage.hidden) {
        // Do not download inlined files via the Album downloads (because they often
        // fail with SSL exceptions) and we can't really trust those files.
        // Also don't download filter hidden items
        continue
      }

      if (item.checked) {
        val simpleSaveableMediaInfo = fromChanPostImage(item.postImage)
        if (simpleSaveableMediaInfo != null) {
          simpleSaveableMediaInfoList.add(simpleSaveableMediaInfo)
        }
      }
    }

    if (simpleSaveableMediaInfoList.isEmpty()) {
      showToast(R.string.album_download_no_suitable_images)
      return
    }

    val options = MultipleImages(
      onSaveClicked = { updatedImageSaverV2Options: ImageSaverV2Options? ->
        imageSaverV2.get().saveMany(updatedImageSaverV2Options!!, simpleSaveableMediaInfoList)

        // Close this controller
        navigationController?.popController()
      }
    )

    presentController(ImageSaverV2OptionsController(context, options))
  }

  private fun onCheckAllClicked(menuItem: ToolbarMenuItem) {
    clearRecyclerCache(recyclerView)

    var i = 0
    val itemsSize = items.size

    while (i < itemsSize) {
      val item = items[i]
      if (item.checked == allChecked) {
        item.checked = !allChecked
        val cell = recyclerView.findViewHolderForAdapterPosition(i) as AlbumDownloadCell?
        if (cell != null) {
          setItemChecked(cell, item.checked, true)
        }
      }

      i++
    }

    updateAllChecked()
    updateTitle()
  }

  fun setPostImages(postImages: List<ChanPostImage?>) {
    var i = 0
    val postImagesSize = postImages.size

    while (i < postImagesSize) {
      val postImage = postImages[i]
      if (postImage == null || postImage.isInlined || postImage.hidden) {
        // Do not allow downloading inlined files via the Album downloads (because they often
        // fail with SSL exceptions) and we can't really trust those files.
        // Also don't allow filter hidden items
        i++
        continue
      }

      items.add(AlbumDownloadItem(postImage, true, i))
      i++
    }
  }

  private fun updateTitle() {
    toolbarState.default.updateTitle(
      newTitle = ToolbarText.String(appResources.string(R.string.album_download_screen, checkCount, items.size))
    )
  }

  private fun updateAllChecked() {
    allChecked = checkCount == items.size
  }

  private class AlbumDownloadItem(
    val postImage: ChanPostImage,
    var checked: Boolean,
    val id: Int
  )

  private inner class AlbumAdapter : RecyclerView.Adapter<AlbumDownloadCell>() {
    private val ALBUM_CELL_TYPE = 1

    init {
      setHasStableIds(true)
    }

    override fun getItemViewType(position: Int): Int {
      return ALBUM_CELL_TYPE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumDownloadCell {
      val view = AppModuleAndroidUtils.inflate(parent.context, R.layout.cell_album_download, parent, false)
      return AlbumDownloadCell(view)
    }

    override fun onBindViewHolder(holder: AlbumDownloadCell, position: Int) {
      val item = items[position]

      holder.thumbnailView.bindPostImage(
        postImage = item.postImage,
        canUseHighResCells = ColorizableGridRecyclerView.canUseHighResCells(recyclerView.currentSpanCount),
        thumbnailViewOptions = ThumbnailViewOptions(
          postThumbnailScaling = ChanSettings.PostThumbnailScaling.CenterCrop,
          drawThumbnailBackground = false,
          drawRipple = true
        )
      )

      setItemChecked(holder, item.checked, false)
    }

    override fun getItemCount(): Int {
      return items.size
    }

    override fun getItemId(position: Int): Long {
      return items[position].id.toLong()
    }
  }

  private inner class AlbumDownloadCell(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
    val checkbox: ImageView
    val thumbnailView: PostImageThumbnailView

    init {
      itemView.layoutParams.height = recyclerView.realSpanWidth
      checkbox = itemView.findViewById<ImageView>(R.id.checkbox)
      thumbnailView = itemView.findViewById<PostImageThumbnailView>(R.id.thumbnail_image_view)
      thumbnailView.setImageClickListener(ALBUM_DOWNLOAD_VIEW_CELL_THUMBNAIL_CLICK_TOKEN, this)
    }

    override fun onClick(v: View) {
      val adapterPosition = adapterPosition

      val item = items[adapterPosition]
      item.checked = !item.checked

      updateAllChecked()
      updateTitle()

      setItemChecked(this, item.checked, true)
    }
  }

  private fun setItemChecked(cell: AlbumDownloadCell, checked: Boolean, animated: Boolean) {
    val scale = if (checked) 0.75f else 1f

    if (animated) {
      cell.thumbnailView.animate()
        .scaleX(scale)
        .scaleY(scale)
        .setInterpolator(slowdown)
        .setDuration(500)
        .start()
    } else {
      cell.thumbnailView.scaleX = scale
      cell.thumbnailView.scaleY = scale
    }

    val drawableId: Int = if (checked) {
      R.drawable.ic_blue_checkmark_24dp
    } else {
      R.drawable.ic_radio_button_unchecked_white_24dp
    }

    val drawable = ContextCompat.getDrawable(context, drawableId)
    cell.checkbox.setImageDrawable(drawable)
  }

  companion object {
    private const val ALBUM_DOWNLOAD_VIEW_CELL_THUMBNAIL_CLICK_TOKEN = "ALBUM_DOWNLOAD_VIEW_CELL_THUMBNAIL_CLICK"
    private val slowdown = DecelerateInterpolator(3f)
  }
}
