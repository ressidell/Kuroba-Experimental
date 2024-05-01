package com.github.k1rakishou.chan.core.di.component.activity

import androidx.appcompat.app.AppCompatActivity
import com.github.k1rakishou.chan.core.di.ActivityDependencies
import com.github.k1rakishou.chan.core.di.component.controller.ControllerComponent
import com.github.k1rakishou.chan.core.di.module.activity.ActivityModule
import com.github.k1rakishou.chan.core.di.module.activity.ActivityScopedViewModelFactoryModule
import com.github.k1rakishou.chan.core.di.module.activity.ActivityScopedViewModelModule
import com.github.k1rakishou.chan.core.di.scope.PerActivity
import com.github.k1rakishou.chan.features.bookmarks.BookmarksPresenter
import com.github.k1rakishou.chan.features.bookmarks.epoxy.BaseThreadBookmarkViewHolder
import com.github.k1rakishou.chan.features.bookmarks.epoxy.EpoxyGridThreadBookmarkViewHolder
import com.github.k1rakishou.chan.features.bookmarks.epoxy.EpoxyListThreadBookmarkViewHolder
import com.github.k1rakishou.chan.features.image_saver.epoxy.EpoxyDuplicateImageView
import com.github.k1rakishou.chan.features.media_viewer.MediaViewerActivity
import com.github.k1rakishou.chan.features.media_viewer.MediaViewerToolbar
import com.github.k1rakishou.chan.features.media_viewer.media_view.AudioMediaView
import com.github.k1rakishou.chan.features.media_viewer.media_view.ExoPlayerVideoMediaView
import com.github.k1rakishou.chan.features.media_viewer.media_view.FullImageMediaView
import com.github.k1rakishou.chan.features.media_viewer.media_view.GifMediaView
import com.github.k1rakishou.chan.features.media_viewer.media_view.MpvVideoMediaView
import com.github.k1rakishou.chan.features.media_viewer.media_view.ThumbnailMediaView
import com.github.k1rakishou.chan.features.media_viewer.media_view.UnsupportedMediaView
import com.github.k1rakishou.chan.features.media_viewer.strip.MediaViewerBottomActionStrip
import com.github.k1rakishou.chan.features.media_viewer.strip.MediaViewerLeftActionStrip
import com.github.k1rakishou.chan.features.proxies.epoxy.EpoxyProxyView
import com.github.k1rakishou.chan.features.reencoding.ImageOptionsHelper
import com.github.k1rakishou.chan.features.reencoding.ImageReencodingPresenter
import com.github.k1rakishou.chan.features.reordering.EpoxyReorderableItemView
import com.github.k1rakishou.chan.features.reply.ReplyLayoutView
import com.github.k1rakishou.chan.features.search.epoxy.EpoxyBoardSelectionButtonView
import com.github.k1rakishou.chan.features.search.epoxy.EpoxySearchEndOfResultsView
import com.github.k1rakishou.chan.features.search.epoxy.EpoxySearchErrorView
import com.github.k1rakishou.chan.features.search.epoxy.EpoxySearchPostDividerView
import com.github.k1rakishou.chan.features.search.epoxy.EpoxySearchPostGapView
import com.github.k1rakishou.chan.features.search.epoxy.EpoxySearchPostView
import com.github.k1rakishou.chan.features.search.epoxy.EpoxySearchSiteView
import com.github.k1rakishou.chan.features.settings.SettingsCoordinator
import com.github.k1rakishou.chan.features.settings.epoxy.EpoxyBooleanSetting
import com.github.k1rakishou.chan.features.settings.epoxy.EpoxyLinkSetting
import com.github.k1rakishou.chan.features.settings.epoxy.EpoxyNoSettingsFoundView
import com.github.k1rakishou.chan.features.settings.epoxy.EpoxySettingsGroupTitle
import com.github.k1rakishou.chan.features.setup.epoxy.EpoxyBoardView
import com.github.k1rakishou.chan.features.setup.epoxy.EpoxySelectableBoardView
import com.github.k1rakishou.chan.features.setup.epoxy.selection.BaseBoardSelectionViewHolder
import com.github.k1rakishou.chan.features.setup.epoxy.selection.EpoxySiteSelectionView
import com.github.k1rakishou.chan.features.setup.epoxy.site.EpoxySiteView
import com.github.k1rakishou.chan.features.toolbar.KurobaToolbarView
import com.github.k1rakishou.chan.ui.activity.CrashReportActivity
import com.github.k1rakishou.chan.ui.activity.SharingActivity
import com.github.k1rakishou.chan.ui.activity.StartActivity
import com.github.k1rakishou.chan.ui.adapter.PostAdapter
import com.github.k1rakishou.chan.ui.captcha.CaptchaLayout
import com.github.k1rakishou.chan.ui.captcha.GenericWebViewAuthenticationLayout
import com.github.k1rakishou.chan.ui.captcha.chan4.Chan4CaptchaLayout
import com.github.k1rakishou.chan.ui.captcha.dvach.DvachCaptchaLayout
import com.github.k1rakishou.chan.ui.captcha.lynxchan.LynxchanCaptchaLayout
import com.github.k1rakishou.chan.ui.captcha.v1.CaptchaNojsLayoutV1
import com.github.k1rakishou.chan.ui.captcha.v2.CaptchaNoJsLayoutV2
import com.github.k1rakishou.chan.ui.cell.AlbumViewCell
import com.github.k1rakishou.chan.ui.cell.CardPostCell
import com.github.k1rakishou.chan.ui.cell.PostCell
import com.github.k1rakishou.chan.ui.cell.PostStubCell
import com.github.k1rakishou.chan.ui.cell.ThreadStatusCell
import com.github.k1rakishou.chan.ui.cell.post_thumbnail.PostImageThumbnailView
import com.github.k1rakishou.chan.ui.cell.post_thumbnail.PostImageThumbnailViewWrapper
import com.github.k1rakishou.chan.ui.compose.ThreadSearchNavigationButtonsView
import com.github.k1rakishou.chan.ui.compose.bottom_panel.KurobaComposeIconPanel
import com.github.k1rakishou.chan.ui.compose.lazylist.ScrollbarView
import com.github.k1rakishou.chan.ui.controller.base.ui.NavigationControllerContainerLayout
import com.github.k1rakishou.chan.ui.controller.base.ui.SystemGestureZoneBlockerLayout
import com.github.k1rakishou.chan.ui.controller.settings.captcha.JsCaptchaCookiesEditorLayout
import com.github.k1rakishou.chan.ui.epoxy.EpoxyDividerView
import com.github.k1rakishou.chan.ui.epoxy.EpoxyErrorView
import com.github.k1rakishou.chan.ui.epoxy.EpoxyExpandableGroupView
import com.github.k1rakishou.chan.ui.epoxy.EpoxyPostLink
import com.github.k1rakishou.chan.ui.epoxy.EpoxySimpleGroupView
import com.github.k1rakishou.chan.ui.epoxy.EpoxyTextView
import com.github.k1rakishou.chan.ui.epoxy.EpoxyTextViewWrapHeight
import com.github.k1rakishou.chan.ui.helper.RemovedPostsHelper
import com.github.k1rakishou.chan.ui.layout.FloatingControllerFrameContainer
import com.github.k1rakishou.chan.ui.layout.FloatingControllerLinearContainer
import com.github.k1rakishou.chan.ui.layout.MrSkeletonLayout
import com.github.k1rakishou.chan.ui.layout.SearchLayout
import com.github.k1rakishou.chan.ui.layout.SplitNavigationControllerLayout
import com.github.k1rakishou.chan.ui.layout.ThreadLayout
import com.github.k1rakishou.chan.ui.layout.ThreadListLayout
import com.github.k1rakishou.chan.ui.layout.ThreadSlidingPaneLayout
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableAlternativeCardView
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableBarButton
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableButton
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableCardView
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableCheckBox
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableChip
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableDivider
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableEditText
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableEpoxyRecyclerView
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableFloatingActionButton
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableFrameLayout
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableGridRecyclerView
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableLinearLayout
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableListView
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableProgressBar
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableRadioButton
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableRecyclerView
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableScrollView
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableSlider
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableSwitchMaterial
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableTabLayout
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableTextInputLayout
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableTextView
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableToolbarSearchLayoutEditText
import com.github.k1rakishou.chan.ui.theme.widget.TouchBlockingConstraintLayout
import com.github.k1rakishou.chan.ui.theme.widget.TouchBlockingCoordinatorLayout
import com.github.k1rakishou.chan.ui.theme.widget.TouchBlockingFrameLayout
import com.github.k1rakishou.chan.ui.theme.widget.TouchBlockingLinearLayout
import com.github.k1rakishou.chan.ui.view.CircularChunkedLoadingBar
import com.github.k1rakishou.chan.ui.view.FloatingMenu
import com.github.k1rakishou.chan.ui.view.HidingFloatingActionButton
import com.github.k1rakishou.chan.ui.view.OptionalSwipeViewPager
import com.github.k1rakishou.chan.ui.view.ThumbnailView
import com.github.k1rakishou.chan.ui.view.attach.AttachNewFileButton
import com.github.k1rakishou.chan.ui.view.bottom_menu_panel.BottomMenuPanel
import com.github.k1rakishou.chan.ui.view.floating_menu.epoxy.EpoxyCheckableFloatingListMenuRow
import com.github.k1rakishou.chan.ui.view.floating_menu.epoxy.EpoxyFloatingListMenuRow
import com.github.k1rakishou.chan.ui.view.floating_menu.epoxy.EpoxyGroupableFloatingListMenuRow
import com.github.k1rakishou.chan.ui.view.floating_menu.epoxy.EpoxyHeaderListMenuRow
import com.github.k1rakishou.chan.ui.view.insets.ColorizableInsetAwareEpoxyRecyclerView
import com.github.k1rakishou.chan.ui.view.insets.ColorizableInsetAwareGridRecyclerView
import com.github.k1rakishou.chan.ui.view.insets.InsetAwareEpoxyRecyclerView
import com.github.k1rakishou.chan.ui.view.insets.InsetAwareLinearLayout
import com.github.k1rakishou.chan.ui.view.insets.InsetAwareRecyclerView
import com.github.k1rakishou.chan.ui.view.sorting.BookmarkSortingItemView
import com.github.k1rakishou.chan.ui.view.widget.dialog.KurobaAlertController
import dagger.BindsInstance
import dagger.Subcomponent

@PerActivity
@Subcomponent(
  modules = [
    ActivityModule::class,
    ActivityScopedViewModelFactoryModule::class,
    ActivityScopedViewModelModule::class
  ]
)
interface ActivityComponent : ActivityDependencies {
  fun controllerComponentBuilder(): ControllerComponent.Builder

  fun inject(startActivity: StartActivity)
  fun inject(sharingActivity: SharingActivity)
  fun inject(mediaViewerActivity: MediaViewerActivity)
  fun inject(crashReportActivity: CrashReportActivity)

  fun inject(kurobaAlertController: KurobaAlertController)

  fun inject(colorizableBarButton: ColorizableBarButton)
  fun inject(colorizableButton: ColorizableButton)
  fun inject(colorizableCardView: ColorizableCardView)
  fun inject(colorizableAlternativeCardView: ColorizableAlternativeCardView)
  fun inject(colorizableCheckBox: ColorizableCheckBox)
  fun inject(colorizableChip: ColorizableChip)
  fun inject(colorizableEditText: ColorizableEditText)
  fun inject(colorizableDivider: ColorizableDivider)
  fun inject(colorizableEpoxyRecyclerView: ColorizableEpoxyRecyclerView)
  fun inject(colorizableFloatingActionButton: ColorizableFloatingActionButton)
  fun inject(colorizableListView: ColorizableListView)
  fun inject(colorizableProgressBar: ColorizableProgressBar)
  fun inject(colorizableRadioButton: ColorizableRadioButton)
  fun inject(colorizableRecyclerView: ColorizableRecyclerView)
  fun inject(colorizableGridRecyclerView: ColorizableGridRecyclerView)
  fun inject(colorizableScrollView: ColorizableScrollView)
  fun inject(colorizableSlider: ColorizableSlider)
  fun inject(colorizableSwitchMaterial: ColorizableSwitchMaterial)
  fun inject(colorizableTextInputLayout: ColorizableTextInputLayout)
  fun inject(colorizableTextView: ColorizableTextView)
  fun inject(colorizableTabLayout: ColorizableTabLayout)
  fun inject(colorizableToolbarSearchLayoutEditText: ColorizableToolbarSearchLayoutEditText)
  fun inject(colorizableFrameLayout: ColorizableFrameLayout)
  fun inject(colorizableLinearLayout: ColorizableLinearLayout)

  fun inject(epoxyGridThreadBookmarkViewHolder: EpoxyGridThreadBookmarkViewHolder)
  fun inject(epoxyListThreadBookmarkViewHolder: EpoxyListThreadBookmarkViewHolder)
  fun inject(epoxyProxyView: EpoxyProxyView)
  fun inject(epoxySearchEndOfResultsView: EpoxySearchEndOfResultsView)
  fun inject(epoxySearchErrorView: EpoxySearchErrorView)
  fun inject(epoxySearchPostDividerView: EpoxySearchPostDividerView)
  fun inject(epoxySearchPostGapView: EpoxySearchPostGapView)
  fun inject(epoxySearchPostView: EpoxySearchPostView)
  fun inject(epoxySearchSiteView: EpoxySearchSiteView)
  fun inject(epoxyBooleanSetting: EpoxyBooleanSetting)
  fun inject(epoxyLinkSetting: EpoxyLinkSetting)
  fun inject(epoxyNoSettingsFoundView: EpoxyNoSettingsFoundView)
  fun inject(epoxySettingsGroupTitle: EpoxySettingsGroupTitle)
  fun inject(epoxyBoardView: EpoxyBoardView)
  fun inject(epoxySelectableBoardView: EpoxySelectableBoardView)
  fun inject(baseBoardSelectionViewHolder: BaseBoardSelectionViewHolder)
  fun inject(epoxySiteSelectionView: EpoxySiteSelectionView)
  fun inject(epoxySiteView: EpoxySiteView)
  fun inject(epoxyDividerView: EpoxyDividerView)
  fun inject(epoxyErrorView: EpoxyErrorView)
  fun inject(epoxyExpandableGroupView: EpoxyExpandableGroupView)
  fun inject(epoxyTextView: EpoxyTextView)
  fun inject(epoxyCheckableFloatingListMenuRow: EpoxyCheckableFloatingListMenuRow)
  fun inject(epoxyGroupableFloatingListMenuRow: EpoxyGroupableFloatingListMenuRow)
  fun inject(epoxyFloatingListMenuRow: EpoxyFloatingListMenuRow)
  fun inject(epoxyHeaderListMenuRow: EpoxyHeaderListMenuRow)
  fun inject(epoxyTextViewWrapHeight: EpoxyTextViewWrapHeight)
  fun inject(epoxyPostLink: EpoxyPostLink)
  fun inject(epoxyBoardSelectionButtonView: EpoxyBoardSelectionButtonView)
  fun inject(epoxySimpleGroupView: EpoxySimpleGroupView)
  fun inject(epoxyDuplicateImageView: EpoxyDuplicateImageView)
  fun inject(epoxyReorderableItemView: EpoxyReorderableItemView)

  fun inject(insetAwareRecyclerView: InsetAwareRecyclerView)
  fun inject(insetAwareEpoxyRecyclerView: InsetAwareEpoxyRecyclerView)
  fun inject(colorizableInsetAwareEpoxyRecyclerView: ColorizableInsetAwareEpoxyRecyclerView)
  fun inject(colorizableInsetAwareGridRecyclerView: ColorizableInsetAwareGridRecyclerView)
  fun inject(insetAwareLinearLayout: InsetAwareLinearLayout)

  fun inject(captchaNoJsLayoutV2: CaptchaNoJsLayoutV2)
  fun inject(captchaNojsLayoutV1: CaptchaNojsLayoutV1)
  fun inject(thumbnailView: ThumbnailView)
  fun inject(thumbnailView: PostImageThumbnailView)
  fun inject(threadLayout: ThreadLayout)
  fun inject(threadListLayout: ThreadListLayout)
  fun inject(cardPostCell: CardPostCell)
  fun inject(captchaLayout: CaptchaLayout)
  fun inject(floatingMenu: FloatingMenu)
  fun inject(threadStatusCell: ThreadStatusCell)
  fun inject(postCell: PostCell)
  fun inject(albumViewCell: AlbumViewCell)
  fun inject(navigationControllerContainerLayout: NavigationControllerContainerLayout)
  fun inject(systemGestureZoneBlockerLayout: SystemGestureZoneBlockerLayout)
  fun inject(bookmarksPresenter: BookmarksPresenter)
  fun inject(baseThreadBookmarkViewHolder: BaseThreadBookmarkViewHolder)
  fun inject(settingsCoordinator: SettingsCoordinator)
  fun inject(jsCaptchaCookiesEditorLayout: JsCaptchaCookiesEditorLayout)
  fun inject(hidingFloatingActionButton: HidingFloatingActionButton)
  fun inject(touchBlockingConstraintLayout: TouchBlockingConstraintLayout)
  fun inject(touchBlockingCoordinatorLayout: TouchBlockingCoordinatorLayout)
  fun inject(touchBlockingFrameLayout: TouchBlockingFrameLayout)
  fun inject(touchBlockingLinearLayout: TouchBlockingLinearLayout)
  fun inject(bottomMenuPanel: BottomMenuPanel)
  fun inject(bookmarkSortingItemView: BookmarkSortingItemView)
  fun inject(genericWebViewAuthenticationLayout: GenericWebViewAuthenticationLayout)
  fun inject(postAdapter: PostAdapter)
  fun inject(removedPostsHelper: RemovedPostsHelper)
  fun inject(imageOptionsHelper: ImageOptionsHelper)
  fun inject(imageReencodingPresenter: ImageReencodingPresenter)
  fun inject(circularChunkedLoadingBar: CircularChunkedLoadingBar)
  fun inject(threadSlidingPaneLayout: ThreadSlidingPaneLayout)
  fun inject(postStubCell: PostStubCell)
  fun inject(searchLayout: SearchLayout)
  fun inject(splitNavigationControllerLayout: SplitNavigationControllerLayout)
  fun inject(attachNewFileButton: AttachNewFileButton)
  fun inject(optionalSwipeViewPager: OptionalSwipeViewPager)
  fun inject(scrollbarView: ScrollbarView)
  fun inject(postImageThumbnailViewWrapper: PostImageThumbnailViewWrapper)
  fun inject(thumbnailMediaView: ThumbnailMediaView)
  fun inject(fullImageMediaView: FullImageMediaView)
  fun inject(audioMediaView: AudioMediaView)
  fun inject(unsupportedMediaView: UnsupportedMediaView)
  fun inject(gifMediaView: GifMediaView)
  fun inject(exoPlayerVideoMediaView: ExoPlayerVideoMediaView)
  fun inject(mpvVideoMediaView: MpvVideoMediaView)
  fun inject(mediaViewerToolbar: MediaViewerToolbar)
  fun inject(mediaViewerBottomActionStrip: MediaViewerBottomActionStrip)
  fun inject(mediaViewerLeftActionStrip: MediaViewerLeftActionStrip)
  fun inject(dvachCaptchaLayout: DvachCaptchaLayout)
  fun inject(chan4CaptchaLayout: Chan4CaptchaLayout)
  fun inject(lynxchanCaptchaLayout: LynxchanCaptchaLayout)
  fun inject(mrSkeletonLayout: MrSkeletonLayout)
  fun inject(kurobaComposeIconPanel: KurobaComposeIconPanel)
  fun inject(floatingControllerLinearContainer: FloatingControllerLinearContainer)
  fun inject(floatingControllerFrameContainer: FloatingControllerFrameContainer)
  fun inject(replyLayoutView: ReplyLayoutView)
  fun inject(kurobaToolbarView: KurobaToolbarView)
  fun inject(threadSearchNavigationButtonsView: ThreadSearchNavigationButtonsView)

  @Subcomponent.Builder
  interface Builder {
    @BindsInstance
    fun activity(activity: AppCompatActivity): Builder
    @BindsInstance
    fun activityModule(module: ActivityModule): Builder

    fun build(): ActivityComponent
  }

}
