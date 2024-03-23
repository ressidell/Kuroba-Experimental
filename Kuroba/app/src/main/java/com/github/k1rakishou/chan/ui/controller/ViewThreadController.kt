package com.github.k1rakishou.chan.ui.controller

import android.content.Context
import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.R.string.action_reload
import com.github.k1rakishou.chan.controller.DeprecatedNavigationFlags
import com.github.k1rakishou.chan.core.di.component.activity.ActivityComponent
import com.github.k1rakishou.chan.core.helper.DialogFactory
import com.github.k1rakishou.chan.core.manager.BookmarksManager
import com.github.k1rakishou.chan.core.manager.BookmarksManager.BookmarkChange
import com.github.k1rakishou.chan.core.manager.BookmarksManager.BookmarkChange.BookmarksCreated
import com.github.k1rakishou.chan.core.manager.BookmarksManager.BookmarkChange.BookmarksDeleted
import com.github.k1rakishou.chan.core.manager.BookmarksManager.BookmarkChange.BookmarksInitialized
import com.github.k1rakishou.chan.core.manager.HistoryNavigationManager
import com.github.k1rakishou.chan.core.manager.ThreadDownloadManager
import com.github.k1rakishou.chan.core.presenter.ThreadPresenter
import com.github.k1rakishou.chan.features.drawer.MainControllerCallbacks
import com.github.k1rakishou.chan.features.thirdeye.ThirdEyeSettingsController
import com.github.k1rakishou.chan.features.thread_downloading.ThreadDownloaderSettingsController
import com.github.k1rakishou.chan.features.toolbar_v2.BackArrowMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenuCheckableOverflowItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenuItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarMenuOverflowItem
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarOverflowMenuBuilder
import com.github.k1rakishou.chan.features.toolbar_v2.ToolbarText
import com.github.k1rakishou.chan.ui.controller.ThreadSlideController.ReplyAutoCloseListener
import com.github.k1rakishou.chan.ui.controller.navigation.NavigationController
import com.github.k1rakishou.chan.ui.controller.navigation.StyledToolbarNavigationController
import com.github.k1rakishou.chan.ui.layout.ThreadLayout.ThreadLayoutCallback
import com.github.k1rakishou.chan.ui.view.KurobaBottomNavigationView
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.getString
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.isDevBuild
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.shareLink
import com.github.k1rakishou.chan.utils.SharingUtils.getUrlForSharing
import com.github.k1rakishou.core_logger.Logger
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor.ThreadDescriptor
import com.github.k1rakishou.model.data.descriptor.PostDescriptor
import com.github.k1rakishou.model.data.options.ChanLoadOptions
import com.github.k1rakishou.model.data.thread.ThreadDownload
import com.github.k1rakishou.model.util.ChanPostUtils
import com.github.k1rakishou.persist_state.PersistableChanState
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

open class ViewThreadController(
  context: Context,
  mainControllerCallbacks: MainControllerCallbacks,
  startingThreadDescriptor: ThreadDescriptor
) : ThreadController(context, mainControllerCallbacks),
  ThreadLayoutCallback,
  ReplyAutoCloseListener {

  @Inject
  lateinit var historyNavigationManagerLazy: Lazy<HistoryNavigationManager>
  @Inject
  lateinit var bookmarksManagerLazy: Lazy<BookmarksManager>
  @Inject
  lateinit var threadDownloadManagerLazy: Lazy<ThreadDownloadManager>

  private val historyNavigationManager: HistoryNavigationManager
    get() = historyNavigationManagerLazy.get()
  private val bookmarksManager: BookmarksManager
    get() = bookmarksManagerLazy.get()
  private val threadDownloadManager: ThreadDownloadManager
    get() = threadDownloadManagerLazy.get()

  private var pinItemPinned = false
  private var threadDescriptor: ThreadDescriptor = startingThreadDescriptor

  override val threadControllerType: ThreadControllerType
    get() = ThreadControllerType.Thread

  override fun injectDependencies(component: ActivityComponent) {
    component.inject(this)
  }

  override fun onCreate() {
    super.onCreate()

    threadLayout.setBoardPostViewMode(ChanSettings.BoardPostViewMode.LIST)
    view.setBackgroundColor(themeEngine.chanTheme.backColor)

    updateNavigationFlags(
      newNavigationFlags = DeprecatedNavigationFlags(
        hasDrawer = true,
        scrollableTitle = ChanSettings.scrollingTextForThreadTitles.get()
      )
    )

    buildToolbar()

    controllerScope.launch {
      bookmarksManager.listenForBookmarksChanges()
        .filter { bookmarkChange: BookmarkChange? -> bookmarkChange !is BookmarksInitialized }
        .debounce(350.milliseconds)
        .collect { bookmarkChange -> updatePinIconStateIfNeeded(bookmarkChange) }
    }

    controllerScope.launch(Dispatchers.Main) { loadThread(threadDescriptor) }
  }

  override fun onShow() {
    super.onShow()
    setPinIconState(false)

    mainControllerCallbacks.resetBottomNavViewCheckState()

    if (KurobaBottomNavigationView.isBottomNavViewEnabled()) {
      mainControllerCallbacks.showBottomNavBar(unlockTranslation = false, unlockCollapse = false)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    updateLeftPaneHighlighting(null)
  }

  override fun onReplyViewShouldClose() {
    if (toolbarState.isInReplyMode()) {
      toolbarState.pop()
    }

    threadLayout.openReply(false)
  }

  override suspend fun showThreadWithoutFocusing(descriptor: ThreadDescriptor, animated: Boolean) {
    Logger.d(TAG, "showThreadWithoutFocusing($descriptor, $animated)")
    showThread(descriptor, animated)
  }

  override suspend fun showThread(descriptor: ThreadDescriptor, animated: Boolean) {
    controllerScope.launch(Dispatchers.Main.immediate) {
      Logger.d(TAG, "showThread($descriptor, $animated)")
      loadThread(descriptor)
    }
  }

  override suspend fun showCatalogWithoutFocusing(catalogDescriptor: ChanDescriptor.ICatalogDescriptor, animated: Boolean) {
    controllerScope.launch(Dispatchers.Main.immediate) {
      Logger.d(TAG, "showCatalog($catalogDescriptor, $animated)")

      showCatalogInternal(
        catalogDescriptor = catalogDescriptor,
        showCatalogOptions = ShowCatalogOptions(
          switchToCatalogController = false,
          withAnimation = animated
        )
      )
    }
  }

  override suspend fun showCatalog(catalogDescriptor: ChanDescriptor.ICatalogDescriptor, animated: Boolean) {
    controllerScope.launch(Dispatchers.Main.immediate) {
      Logger.d(TAG, "showCatalog($catalogDescriptor, $animated)")

      showCatalogInternal(
        catalogDescriptor = catalogDescriptor,
        showCatalogOptions = ShowCatalogOptions(
          switchToCatalogController = true,
          withAnimation = animated
        )
      )
    }
  }

  override suspend fun setCatalog(catalogDescriptor: ChanDescriptor.ICatalogDescriptor, animated: Boolean) {
    controllerScope.launch(Dispatchers.Main.immediate) {
      Logger.d(TAG, "setCatalog($catalogDescriptor, $animated)")

      showCatalogInternal(
        catalogDescriptor = catalogDescriptor,
        showCatalogOptions = ShowCatalogOptions(
          switchToCatalogController = true,
          withAnimation = animated
        )
      )
    }
  }

  suspend fun loadThread(
    threadDescriptor: ThreadDescriptor,
    openingExternalThread: Boolean = false,
    openingPreviousThread: Boolean = false
  ) {
    Logger.d(TAG, "loadThread($threadDescriptor)")

    val presenter = threadLayout.presenter
    if (threadDescriptor != presenter.currentChanDescriptor) {
      loadThreadInternal(threadDescriptor, openingExternalThread, openingPreviousThread)
    }
  }

  override suspend fun openExternalThread(postDescriptor: PostDescriptor, scrollToPost: Boolean) {
    val descriptor = chanDescriptor
      ?: return

    openExternalThreadHelper.openExternalThread(
      currentChanDescriptor = descriptor,
      postDescriptor = postDescriptor,
      scrollToPost = scrollToPost
    ) { threadDescriptor ->
      controllerScope.launch { loadThread(threadDescriptor = threadDescriptor, openingExternalThread = true) }
    }
  }

  override suspend fun showPostsInExternalThread(
    postDescriptor: PostDescriptor,
    isPreviewingCatalogThread: Boolean
  ) {
    showPostsInExternalThreadHelper.showPostsInExternalThread(postDescriptor, isPreviewingCatalogThread)
  }

  override fun onShowPosts() {
    super.onShowPosts()

    setNavigationTitleFromDescriptor(threadDescriptor)
    setPinIconState(false)
  }

  override fun onShowError() {
    super.onShowError()

    toolbarState.thread.updateTitle(
      newTitle = ToolbarText.Id(R.string.thread_loading_error_title)
    )
  }

  override fun threadBackPressed(): Boolean {
    val threadDescriptor = threadFollowHistoryManager.removeTop()
      ?: return false

    controllerScope.launch(Dispatchers.Main.immediate) {
      loadThread(threadDescriptor, openingPreviousThread = true)
    }

    return true
  }

  override fun threadBackLongPressed() {
    threadFollowHistoryManager.clearAllExcept(threadDescriptor)
    showToast(R.string.thread_follow_history_has_been_cleared)
  }

  override fun onLostFocus(wasFocused: ThreadControllerType) {
    super.onLostFocus(wasFocused)
    check(wasFocused == threadControllerType) { "Unexpected controllerType: $wasFocused" }
  }

  override fun onGainedFocus(nowFocused: ThreadControllerType) {
    super.onGainedFocus(nowFocused)
    check(nowFocused == threadControllerType) { "Unexpected controllerType: $nowFocused" }

    if (historyNavigationManager.isInitialized) {
      if (threadLayout.presenter.chanThreadLoadingState == ThreadPresenter.ChanThreadLoadingState.Loaded) {
        controllerScope.launch { historyNavigationManager.moveNavElementToTop(threadDescriptor) }
      }
    }

    currentOpenedDescriptorStateManager.updateCurrentFocusedController(ThreadPresenter.CurrentFocusedController.Thread)
    updateLeftPaneHighlighting(threadDescriptor)
  }

  private fun updatePinIconStateIfNeeded(bookmarkChange: BookmarkChange) {
    val currentThreadDescriptor = threadLayout.presenter.threadDescriptorOrNull()
      ?: return
    val changedBookmarkDescriptors = bookmarkChange.threadDescriptors()

    if (changedBookmarkDescriptors.isEmpty()) {
      return
    }

    var animate = false

    if (bookmarkChange is BookmarksCreated || bookmarkChange is BookmarksDeleted) {
      animate = true
    }

    for (changedBookmarkDescriptor in changedBookmarkDescriptors) {
      if (changedBookmarkDescriptor == currentThreadDescriptor) {
        setPinIconState(animate)
        return
      }
    }
  }

  private suspend fun showCatalogInternal(
    catalogDescriptor: ChanDescriptor.ICatalogDescriptor,
    showCatalogOptions: ShowCatalogOptions
  ) {
    Logger.d(TAG, "showCatalogInternal($catalogDescriptor, $showCatalogOptions)")

    if (doubleNavigationController != null && doubleNavigationController?.leftController() is BrowseController) {
      val browseController = doubleNavigationController?.leftController() as? BrowseController
      if (browseController != null) {
        browseController.setCatalog(catalogDescriptor)

        if (showCatalogOptions.switchToCatalogController) {
          // slide layout
          doubleNavigationController?.switchToController(true, showCatalogOptions.withAnimation)
        }
      }

      return
    }

    if (doubleNavigationController?.leftController() is StyledToolbarNavigationController) {
      // split layout
      val browseController = doubleNavigationController
        ?.leftController()
        ?.childControllers
        ?.getOrNull(0) as? BrowseController

      if (browseController != null) {
        browseController.setCatalog(catalogDescriptor)
      }

      return
    }

    // phone layout
    var browseController: BrowseController? = null

    for (controller in requireNavController().childControllers) {
      if (controller is BrowseController) {
        browseController = controller
        break
      }
    }

    if (browseController != null) {
      browseController.setCatalog(catalogDescriptor)
      requireNavController().popController(showCatalogOptions.withAnimation)
    }
  }

  private suspend fun loadThreadInternal(
    newThreadDescriptor: ThreadDescriptor,
    openingExternalThread: Boolean,
    openingPreviousThread: Boolean
  ) {
    if (!openingExternalThread && !openingPreviousThread) {
      threadFollowHistoryManager.clear()
    }

    val presenter = threadLayout.presenter
    val oldThreadDescriptor = threadLayout.presenter.currentChanDescriptor as? ThreadDescriptor

    presenter.bindChanDescriptor(newThreadDescriptor)
    this.threadDescriptor = newThreadDescriptor

    updateMenuItems()
    updateNavigationTitle(oldThreadDescriptor, newThreadDescriptor)

    setPinIconState(false)
    updateLeftPaneHighlighting(newThreadDescriptor)
  }

  private fun updateNavigationTitle(
    oldThreadDescriptor: ThreadDescriptor?,
    newThreadDescriptor: ThreadDescriptor?
  ) {
    if (oldThreadDescriptor == null && newThreadDescriptor == null) {
      return
    }

    if (oldThreadDescriptor == newThreadDescriptor) {
      setNavigationTitleFromDescriptor(newThreadDescriptor)
    } else {
      toolbarState.thread.updateTitle(
        newTitle = ToolbarText.String(getString(R.string.loading))
      )
    }
  }

  private fun setNavigationTitleFromDescriptor(threadDescriptor: ThreadDescriptor?) {
    val originalPost = chanThreadManager.getChanThread(threadDescriptor)
      ?.getOriginalPost()

    toolbarState.thread.updateTitle(
      newTitle = ToolbarText.String(ChanPostUtils.getTitle(originalPost, threadDescriptor))
    )
  }

  private suspend fun updateMenuItems() {
    toolbarState.findOverflowItem(ACTION_PREVIEW_THREAD_IN_ARCHIVE)
      ?.updateVisibility(archivesManager.supports(threadDescriptor))
    toolbarState.findOverflowItem(ACTION_OPEN_THREAD_IN_ARCHIVE)
      ?.updateVisibility(archivesManager.supports(threadDescriptor))

    updateThreadDownloadItem()
  }

  private suspend fun updateThreadDownloadItem() {
    toolbarState.findOverflowItem(ACTION_DOWNLOAD_THREAD)?.let { downloadThreadItem ->
      val status = threadDownloadManager.getStatus(threadDescriptor)
      downloadThreadItem.updateVisibility(status != ThreadDownload.Status.Completed)

      when (status) {
        null,
        ThreadDownload.Status.Stopped -> {
          downloadThreadItem.updateMenuText(getString(R.string.action_start_thread_download))
        }
        ThreadDownload.Status.Running -> {
          downloadThreadItem.updateMenuText(getString(R.string.action_stop_thread_download))
        }
        ThreadDownload.Status.Completed -> {
          downloadThreadItem.updateVisibility(false)
        }
      }
    }
  }

  private fun updateLeftPaneHighlighting(chanDescriptor: ChanDescriptor?) {
    if (doubleNavigationController == null) {
      return
    }

    var threadController: ThreadController? = null
    val leftController = doubleNavigationController?.leftController()

    if (leftController is ThreadController) {
      threadController = leftController
    } else if (leftController is NavigationController) {
      for (controller in leftController.childControllers) {
        if (controller is ThreadController) {
          threadController = controller
          break
        }
      }
    }

    if (threadController == null) {
      return
    }

    if (chanDescriptor is ThreadDescriptor) {
      threadController.highlightPost(postDescriptor = chanDescriptor.toOriginalPostDescriptor(), blink = false)
    } else if (chanDescriptor == null) {
      threadController.highlightPost(postDescriptor = null, blink = false)
    }
  }

  private fun setPinIconState(animated: Boolean) {
    val presenter = threadLayout.presenterOrNull
    if (presenter != null) {
      setPinIconStateDrawable(presenter.isPinned, animated)
    }
  }

  private fun setPinIconStateDrawable(pinned: Boolean, animated: Boolean) {
    if (pinned == pinItemPinned) {
      return
    }

    val menuItem = toolbarState.findItem(ACTION_PIN)
      ?: return

    pinItemPinned = pinned

    val drawableId = if (pinned) {
      R.drawable.ic_bookmark_white_24dp
    } else {
      R.drawable.ic_bookmark_border_white_24dp
    }

    menuItem.updateDrawableId(drawableId)
  }

  private fun buildToolbar() {
    toolbarState.enterThreadMode(
      leftItem = BackArrowMenuItem(
        onClick = {
          // TODO: New toolbar
        }
      ),
      scrollableTitle = ChanSettings.scrollingTextForThreadTitles.get(),
      menuBuilder = {
        withMenuItem(
          id = ACTION_ALBUM,
          drawableId = R.drawable.ic_image_white_24dp,
          onClick = { item -> albumClicked(item) }
        )
        withMenuItem(
          id = ACTION_PIN,
          drawableId = R.drawable.ic_bookmark_border_white_24dp,
          onClick = { item -> pinClicked(item) }
        )

        withOverflowMenu {
          if (!ChanSettings.enableReplyFab.get()) {
            withOverflowMenuItem(
              id = ACTION_REPLY,
              stringId = R.string.action_reply,
              onClick = { item -> replyClicked(item) })
          }

          withOverflowMenuItem(
            id = ACTION_SEARCH,
            stringId = R.string.action_search,
            onClick = { item -> searchClicked(item) }
          )
          withOverflowMenuItem(
            id = ACTION_RELOAD,
            stringId = action_reload,
            onClick = { item -> reloadClicked(item) }
          )
          withOverflowMenuItem(
            id = ACTION_DOWNLOAD_THREAD,
            stringId = R.string.action_start_thread_download,
            visible = true,
            onClick = { item -> downloadOrStopDownloadThread(item) }
          )
          withOverflowMenuItem(
            id = ACTION_VIEW_REMOVED_POSTS,
            stringId = R.string.action_view_removed_posts,
            onClick = { item -> showRemovedPostsDialog(item) }
          )
          withOverflowMenuItem(
            id = ACTION_PREVIEW_THREAD_IN_ARCHIVE,
            stringId = R.string.action_preview_thread_in_archive,
            visible = false,
            onClick = { showAvailableArchivesList(postDescriptor = threadDescriptor.toOriginalPostDescriptor(), preview = true) }
          )
          withOverflowMenuItem(
            id = ACTION_OPEN_THREAD_IN_ARCHIVE,
            stringId = R.string.action_open_in_archive,
            visible = false,
            onClick = { showAvailableArchivesList(postDescriptor = threadDescriptor.toOriginalPostDescriptor(), preview = false) }
          )
          withOverflowMenuItem(
            id = ACTION_OPEN_BROWSER,
            stringId = R.string.action_open_browser,
            onClick = { item -> openBrowserClicked(item) }
          )
          withOverflowMenuItem(
            id = ACTION_SHARE,
            stringId = R.string.action_share,
            onClick = { item -> shareClicked(item) }
          )
          withOverflowMenuItem(
            id = ACTION_GO_TO_POST,
            stringId = R.string.action_go_to_post,
            visible = isDevBuild(),
            onClick = { item -> onGoToPostClicked(item) },
            builder = { withMoreThreadOptions() }
          )
          withOverflowMenuItem(
            id = ACTION_SCROLL_TO_TOP,
            stringId = R.string.action_scroll_to_top,
            onClick = { item -> upClicked(item) }
          )
          withOverflowMenuItem(
            id = ACTION_SCROLL_TO_BOTTOM,
            stringId = R.string.action_scroll_to_bottom,
            onClick = { item -> downClicked(item) }
          )
        }
      }
    )
  }

  private fun ToolbarOverflowMenuBuilder.withMoreThreadOptions() {
    withCheckableOverflowMenuItem(
      id = ACTION_USE_SCROLLING_TEXT_FOR_THREAD_TITLE,
      stringId = R.string.action_use_scrolling_text_for_thread_title,
      visible = true,
      checked = ChanSettings.scrollingTextForThreadTitles.get(),
      onClick = { item -> onThreadViewOptionClicked(item) }
    )
    withCheckableOverflowMenuItem(
      id = ACTION_MARK_YOUR_POSTS_ON_SCROLLBAR,
      stringId = R.string.action_mark_your_posts_on_scrollbar,
      visible = true,
      checked = ChanSettings.markYourPostsOnScrollbar.get(),
      onClick = { item -> onScrollbarLabelingOptionClicked(item) }
    )
    withCheckableOverflowMenuItem(
      id = ACTION_MARK_REPLIES_TO_YOU_ON_SCROLLBAR,
      stringId = R.string.action_mark_replies_to_your_posts_on_scrollbar,
      visible = true,
      checked = ChanSettings.markRepliesToYourPostOnScrollbar.get(),
      onClick = { item -> onScrollbarLabelingOptionClicked(item) }
    )
    withCheckableOverflowMenuItem(
      id = ACTION_MARK_CROSS_THREAD_REPLIES_ON_SCROLLBAR,
      stringId = R.string.action_mark_cross_thread_quotes_on_scrollbar,
      visible = true,
      checked = ChanSettings.markCrossThreadQuotesOnScrollbar.get(),
      onClick = { item -> onScrollbarLabelingOptionClicked(item) }
    )
    withCheckableOverflowMenuItem(
      id = ACTION_MARK_DELETED_POSTS_ON_SCROLLBAR,
      stringId = R.string.action_mark_deleted_posts_on_scrollbar,
      visible = true,
      checked = ChanSettings.markDeletedPostsOnScrollbar.get(),
      onClick = { item -> onScrollbarLabelingOptionClicked(item) }
    )
    withCheckableOverflowMenuItem(
      id = ACTION_MARK_HOT_POSTS_ON_SCROLLBAR,
      stringId = R.string.action_mark_hot_posts_on_scrollbar,
      visible = true,
      checked = ChanSettings.markHotPostsOnScrollbar.get(),
      onClick = { item -> onScrollbarLabelingOptionClicked(item) }
    )
    withCheckableOverflowMenuItem(
      id = ACTION_GLOBAL_NSFW_MODE,
      stringId = R.string.action_catalog_thread_nsfw_mode,
      visible = true,
      checked = ChanSettings.globalNsfwMode.get(),
      onClick = { item -> onScrollbarLabelingOptionClicked(item) }
    )
    withOverflowMenuItem(
      id = ACTION_THIRD_EYE_SETTINGS,
      stringId = R.string.action_third_eye_settings,
      visible = true,
      onClick = { presentController(ThirdEyeSettingsController(context)) }
    )
  }

  private fun replyClicked(item: ToolbarMenuOverflowItem) {
    threadLayout.openReply(true)
  }

  private fun albumClicked(item: ToolbarMenuItem) {
    threadLayout.presenter.showAlbum()
  }

  private fun pinClicked(item: ToolbarMenuItem) {
    controllerScope.launch {
      if (threadLayout.presenter.pin()) {
        setPinIconState(true)
      }
    }
  }

  private fun searchClicked(item: ToolbarMenuOverflowItem) {
    if (chanDescriptor == null) {
      return
    }

    threadLayout.popupHelper.showSearchPopup(chanDescriptor!!)
  }

  private fun reloadClicked(item: ToolbarMenuOverflowItem) {
    threadLayout.presenter.resetTicker()
    threadLayout.presenter.normalLoad(
      showLoading = true,
      chanLoadOptions = ChanLoadOptions.clearMemoryCache()
    )
  }

  private fun downloadOrStopDownloadThread(item: ToolbarMenuOverflowItem) {
    val warningShown = PersistableChanState.threadDownloaderArchiveWarningShown.get()

    if (!warningShown && archivesManager.isSiteArchive(threadDescriptor.siteDescriptor())) {
      PersistableChanState.threadDownloaderArchiveWarningShown.set(true)

      dialogFactory.createSimpleInformationDialog(
        context = context,
        titleText = getString(R.string.thread_download_archive_thread_warning_title),
        descriptionText = getString(R.string.thread_download_archive_thread_warning_description),
        onPositiveButtonClickListener = { downloadOrStopDownloadThread(item) }
      )

      return
    }

    controllerScope.launch {
      when (threadDownloadManager.getStatus(threadDescriptor)) {
        ThreadDownload.Status.Running -> {
          threadDownloadManager.stopDownloading(threadDescriptor)
        }
        null,
        ThreadDownload.Status.Stopped -> {
          val threadDownloaderSettingsController = ThreadDownloaderSettingsController(
            context = context,
            downloadClicked = { downloadMedia ->
              controllerScope.launch {
                val threadThumbnailUrl = chanThreadManager.getChanThread(threadDescriptor)
                  ?.getOriginalPost()
                  ?.firstImage()
                  ?.actualThumbnailUrl
                  ?.toString()

                threadDownloadManager.startDownloading(
                  threadDescriptor = threadDescriptor,
                  threadThumbnailUrl = threadThumbnailUrl,
                  downloadMedia = downloadMedia
                )

                updateThreadDownloadItem()
              }
            }
          )

          presentController(threadDownloaderSettingsController, animated = true)
        }
        ThreadDownload.Status.Completed -> {
          return@launch
        }
      }

      updateThreadDownloadItem()
    }
  }

  private fun showRemovedPostsDialog(item: ToolbarMenuOverflowItem?) {
    threadLayout.presenter.showRemovedPostsDialog()
  }

  private fun openBrowserClicked(item: ToolbarMenuOverflowItem) {
    if (threadLayout.presenter.currentChanDescriptor == null) {
      showToast(R.string.cannot_open_in_browser_already_deleted)
      return
    }

    val url = getUrlForSharing(siteManager, threadDescriptor)
    if (url == null) {
      showToast(R.string.cannot_open_in_browser_already_deleted)
      return
    }

    AppModuleAndroidUtils.openLink(url)
  }

  private fun shareClicked(item: ToolbarMenuOverflowItem) {
    if (threadLayout.presenter.currentChanDescriptor == null) {
      showToast(R.string.cannot_shared_thread_already_deleted)
      return
    }

    val url = getUrlForSharing(siteManager, threadDescriptor)
    if (url == null) {
      showToast(R.string.cannot_shared_thread_already_deleted)
      return
    }

    shareLink(url)
  }

  private fun onGoToPostClicked(item: ToolbarMenuOverflowItem) {
    dialogFactory.createSimpleDialogWithInput(
      context = context,
      titleTextId = R.string.view_thread_controller_enter_post_id,
      onValueEntered = { input: String ->
        try {
          val postNo = input.toLong()
          threadLayout.presenter.scrollToPost(PostDescriptor.create(threadDescriptor, postNo))
        } catch (e: NumberFormatException) {
          //ignored
        }
      },
      inputType = DialogFactory.DialogInputType.Integer
    )
  }

  private fun onThreadViewOptionClicked(item: ToolbarMenuCheckableOverflowItem) {
    val clickedItemId = item.id
    if (clickedItemId == ACTION_USE_SCROLLING_TEXT_FOR_THREAD_TITLE) {
      toolbarState.findCheckableOverflowItem(ACTION_USE_SCROLLING_TEXT_FOR_THREAD_TITLE)
        ?.updateChecked(ChanSettings.scrollingTextForThreadTitles.toggle())

      showToast(R.string.restart_the_app)
    }
  }

  private fun onScrollbarLabelingOptionClicked(item: ToolbarMenuCheckableOverflowItem) {
    when (item.id) {
      ACTION_MARK_REPLIES_TO_YOU_ON_SCROLLBAR -> {
        toolbarState.findCheckableOverflowItem(ACTION_MARK_REPLIES_TO_YOU_ON_SCROLLBAR)
          ?.updateChecked(ChanSettings.markRepliesToYourPostOnScrollbar.toggle())
      }
      ACTION_MARK_CROSS_THREAD_REPLIES_ON_SCROLLBAR -> {
        toolbarState.findCheckableOverflowItem(ACTION_MARK_CROSS_THREAD_REPLIES_ON_SCROLLBAR)
          ?.updateChecked(ChanSettings.markCrossThreadQuotesOnScrollbar.toggle())
      }
      ACTION_MARK_YOUR_POSTS_ON_SCROLLBAR -> {
        toolbarState.findCheckableOverflowItem(ACTION_MARK_YOUR_POSTS_ON_SCROLLBAR)
          ?.updateChecked(ChanSettings.markYourPostsOnScrollbar.toggle())
      }
      ACTION_MARK_DELETED_POSTS_ON_SCROLLBAR -> {
        toolbarState.findCheckableOverflowItem(ACTION_MARK_DELETED_POSTS_ON_SCROLLBAR)
          ?.updateChecked(ChanSettings.markDeletedPostsOnScrollbar.toggle())
      }
      ACTION_MARK_HOT_POSTS_ON_SCROLLBAR -> {
        toolbarState.findCheckableOverflowItem(ACTION_MARK_HOT_POSTS_ON_SCROLLBAR)
          ?.updateChecked(ChanSettings.markHotPostsOnScrollbar.toggle())
      }
      ACTION_GLOBAL_NSFW_MODE -> {
        toolbarState.findCheckableOverflowItem(ACTION_GLOBAL_NSFW_MODE)
          ?.updateChecked(ChanSettings.globalNsfwMode.toggle())
      }
    }

    threadLayout.presenter.quickReloadFromMemoryCache()
  }

  private fun upClicked(item: ToolbarMenuOverflowItem) {
    threadLayout.scrollTo(0, false)
  }

  private fun downClicked(item: ToolbarMenuOverflowItem) {
    threadLayout.scrollTo(-1, false)
  }

  companion object {
    private const val TAG = "ViewThreadController"
    private const val ACTION_PIN = 8001
    private const val ACTION_ALBUM = 8002
    private const val ACTION_REPLY = 9000
    private const val ACTION_SEARCH = 9001
    private const val ACTION_RELOAD = 9002
    private const val ACTION_VIEW_REMOVED_POSTS = 9004
    private const val ACTION_PREVIEW_THREAD_IN_ARCHIVE = 9005
    private const val ACTION_OPEN_THREAD_IN_ARCHIVE = 9006
    private const val ACTION_OPEN_BROWSER = 9007
    private const val ACTION_SHARE = 9008
    private const val ACTION_GO_TO_POST = 9009
    private const val ACTION_THREAD_MORE_OPTIONS = 9010
    private const val ACTION_SCROLL_TO_TOP = 9011
    private const val ACTION_SCROLL_TO_BOTTOM = 9012
    private const val ACTION_DOWNLOAD_THREAD = 9013

    private const val ACTION_USE_SCROLLING_TEXT_FOR_THREAD_TITLE = 9100
    private const val ACTION_MARK_YOUR_POSTS_ON_SCROLLBAR = 9101
    private const val ACTION_MARK_REPLIES_TO_YOU_ON_SCROLLBAR = 9102
    private const val ACTION_MARK_CROSS_THREAD_REPLIES_ON_SCROLLBAR = 9103
    private const val ACTION_MARK_DELETED_POSTS_ON_SCROLLBAR = 9104
    private const val ACTION_MARK_HOT_POSTS_ON_SCROLLBAR = 9105
    private const val ACTION_GLOBAL_NSFW_MODE = 9106
    private const val ACTION_THIRD_EYE_SETTINGS = 9107
  }
}
