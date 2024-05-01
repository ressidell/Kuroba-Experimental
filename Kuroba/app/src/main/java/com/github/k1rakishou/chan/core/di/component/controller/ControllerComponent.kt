package com.github.k1rakishou.chan.core.di.component.controller

import com.github.k1rakishou.chan.core.di.module.controller.ControllerModule
import com.github.k1rakishou.chan.core.di.module.controller.ControllerScopedViewModelFactoryModule
import com.github.k1rakishou.chan.core.di.module.controller.ControllerScopedViewModelModule
import com.github.k1rakishou.chan.core.di.scope.PerController
import com.github.k1rakishou.chan.features.album.AlbumViewControllerV2
import com.github.k1rakishou.chan.features.bookmarks.BookmarkGroupPatternSettingsController
import com.github.k1rakishou.chan.features.bookmarks.BookmarkGroupSettingsController
import com.github.k1rakishou.chan.features.bookmarks.BookmarksController
import com.github.k1rakishou.chan.features.bookmarks.BookmarksSortingController
import com.github.k1rakishou.chan.features.bypass.SiteFirewallBypassController
import com.github.k1rakishou.chan.features.create_sound_media.CreateSoundMediaController
import com.github.k1rakishou.chan.features.drawer.MainController
import com.github.k1rakishou.chan.features.filters.CreateOrUpdateFilterController
import com.github.k1rakishou.chan.features.filters.FilterBoardSelectorController
import com.github.k1rakishou.chan.features.filters.FilterTypeSelectionController
import com.github.k1rakishou.chan.features.filters.FiltersController
import com.github.k1rakishou.chan.features.image_saver.ImageSaverV2OptionsController
import com.github.k1rakishou.chan.features.image_saver.ResolveDuplicateImagesController
import com.github.k1rakishou.chan.features.login.LoginController
import com.github.k1rakishou.chan.features.media_viewer.MediaViewerController
import com.github.k1rakishou.chan.features.media_viewer.MediaViewerGesturesSettingsController
import com.github.k1rakishou.chan.features.mpv.EditMpvConfController
import com.github.k1rakishou.chan.features.my_posts.SavedPostsController
import com.github.k1rakishou.chan.features.proxies.ProxyEditorController
import com.github.k1rakishou.chan.features.proxies.ProxySetupController
import com.github.k1rakishou.chan.features.reencoding.ImageOptionsController
import com.github.k1rakishou.chan.features.reencoding.ImageReencodeOptionsController
import com.github.k1rakishou.chan.features.remote_image_search.ImageSearchController
import com.github.k1rakishou.chan.features.reordering.SimpleListItemsReorderingController
import com.github.k1rakishou.chan.features.report_bugs.ReportIssueController
import com.github.k1rakishou.chan.features.report_posts.Chan4ReportPostController
import com.github.k1rakishou.chan.features.search.GlobalSearchController
import com.github.k1rakishou.chan.features.search.SearchResultsController
import com.github.k1rakishou.chan.features.search.SelectBoardForSearchController
import com.github.k1rakishou.chan.features.search.SelectSiteForSearchController
import com.github.k1rakishou.chan.features.settings.MainSettingsControllerV2
import com.github.k1rakishou.chan.features.settings.screens.delegate.ExportBackupOptionsController
import com.github.k1rakishou.chan.features.setup.AddBoardsController
import com.github.k1rakishou.chan.features.setup.BoardSelectionController
import com.github.k1rakishou.chan.features.setup.BoardsSetupController
import com.github.k1rakishou.chan.features.setup.ComposeBoardsController
import com.github.k1rakishou.chan.features.setup.ComposeBoardsSelectorController
import com.github.k1rakishou.chan.features.setup.CompositeCatalogsSetupController
import com.github.k1rakishou.chan.features.setup.SiteSettingsController
import com.github.k1rakishou.chan.features.setup.SitesSetupController
import com.github.k1rakishou.chan.features.site_archive.BoardArchiveController
import com.github.k1rakishou.chan.features.themes.ThemeGalleryController
import com.github.k1rakishou.chan.features.themes.ThemeSettingsController
import com.github.k1rakishou.chan.features.thirdeye.AddOrEditBooruController
import com.github.k1rakishou.chan.features.thirdeye.ThirdEyeSettingsController
import com.github.k1rakishou.chan.features.thread_downloading.LocalArchiveController
import com.github.k1rakishou.chan.features.thread_downloading.ThreadDownloaderSettingsController
import com.github.k1rakishou.chan.ui.controller.AlbumDownloadController
import com.github.k1rakishou.chan.ui.controller.BrowseController
import com.github.k1rakishou.chan.ui.controller.CaptchaContainerController
import com.github.k1rakishou.chan.ui.controller.FloatingListMenuController
import com.github.k1rakishou.chan.ui.controller.LicensesController
import com.github.k1rakishou.chan.ui.controller.LoadingViewController
import com.github.k1rakishou.chan.ui.controller.LogsController
import com.github.k1rakishou.chan.ui.controller.OpenUrlInWebViewController
import com.github.k1rakishou.chan.ui.controller.PostLinksController
import com.github.k1rakishou.chan.ui.controller.PostOmittedImagesController
import com.github.k1rakishou.chan.ui.controller.RemovedPostsController
import com.github.k1rakishou.chan.ui.controller.ThreadSlideController
import com.github.k1rakishou.chan.ui.controller.ViewThreadController
import com.github.k1rakishou.chan.ui.controller.WebViewReportController
import com.github.k1rakishou.chan.ui.controller.base.Controller
import com.github.k1rakishou.chan.ui.controller.dialog.KurobaAlertDialogHostController
import com.github.k1rakishou.chan.ui.controller.dialog.KurobaComposeDialogController
import com.github.k1rakishou.chan.ui.controller.navigation.SplitNavigationController
import com.github.k1rakishou.chan.ui.controller.navigation.StyledToolbarNavigationController
import com.github.k1rakishou.chan.ui.controller.popup.PostRepliesPopupController
import com.github.k1rakishou.chan.ui.controller.popup.PostSearchPopupController
import com.github.k1rakishou.chan.ui.controller.settings.RangeSettingUpdaterController
import com.github.k1rakishou.chan.ui.controller.settings.captcha.JsCaptchaCookiesEditorController
import dagger.BindsInstance
import dagger.Subcomponent

@PerController
@Subcomponent(
  modules = [
    ControllerModule::class,
    ControllerScopedViewModelFactoryModule::class,
    ControllerScopedViewModelModule::class
  ]
)
interface ControllerComponent {
  fun inject(albumViewControllerV2: AlbumViewControllerV2)
  fun inject(addBoardsController: AddBoardsController)
  fun inject(addOrEditBooruController: AddOrEditBooruController)
  fun inject(albumDownloadController: AlbumDownloadController)
  fun inject(boardArchiveController: BoardArchiveController)
  fun inject(boardSelectionController: BoardSelectionController)
  fun inject(browseController: BrowseController)
  fun inject(mainController: MainController)
  fun inject(filtersController: FiltersController)
  fun inject(imageOptionsController: ImageOptionsController)
  fun inject(imageReencodeOptionsController: ImageReencodeOptionsController)
  fun inject(licensesController: LicensesController)
  fun inject(loginController: LoginController)
  fun inject(logsController: LogsController)
  fun inject(postRepliesPopupController: PostRepliesPopupController)
  fun inject(postSearchPopupController: PostSearchPopupController)
  fun inject(removedPostsController: RemovedPostsController)
  fun inject(webViewReportController: WebViewReportController)
  fun inject(sitesSetupController: SitesSetupController)
  fun inject(splitNavigationController: SplitNavigationController)
  fun inject(styledToolbarNavigationController: StyledToolbarNavigationController)
  fun inject(themeSettingsController: ThemeSettingsController)
  fun inject(themeGalleryController: ThemeGalleryController)
  fun inject(threadSlideController: ThreadSlideController)
  fun inject(viewThreadController: ViewThreadController)
  fun inject(bookmarksController: BookmarksController)
  fun inject(rangeSettingUpdaterController: RangeSettingUpdaterController)
  fun inject(bookmarksSortingController: BookmarksSortingController)
  fun inject(proxyEditorController: ProxyEditorController)
  fun inject(proxySetupController: ProxySetupController)
  fun inject(globalSearchController: GlobalSearchController)
  fun inject(searchResultsController: SearchResultsController)
  fun inject(boardsSetupController: BoardsSetupController)
  fun inject(mainSettingsControllerV2: MainSettingsControllerV2)
  fun inject(siteSettingsController: SiteSettingsController)
  fun inject(reportIssueController: ReportIssueController)
  fun inject(floatingListMenuController: FloatingListMenuController)
  fun inject(jsCaptchaCookiesEditorController: JsCaptchaCookiesEditorController)
  fun inject(loadingViewController: LoadingViewController)
  fun inject(postLinksController: PostLinksController)
  fun inject(selectSiteForSearchController: SelectSiteForSearchController)
  fun inject(selectBoardForSearchController: SelectBoardForSearchController)
  fun inject(siteFirewallBypassController: SiteFirewallBypassController)
  fun inject(imageSaverV2OptionsController: ImageSaverV2OptionsController)
  fun inject(resolveDuplicateImagesController: ResolveDuplicateImagesController)
  fun inject(kurobaAlertDialogHostController: KurobaAlertDialogHostController)
  fun inject(simpleListItemsReorderingController: SimpleListItemsReorderingController)
  fun inject(captchaContainerController: CaptchaContainerController)
  fun inject(mediaViewerController: MediaViewerController)
  fun inject(mediaViewerGesturesSettingsController: MediaViewerGesturesSettingsController)
  fun inject(savedPostsController: SavedPostsController)
  fun inject(threadDownloaderSettingsController: ThreadDownloaderSettingsController)
  fun inject(localArchiveController: LocalArchiveController)
  fun inject(postOmittedImagesController: PostOmittedImagesController)
  fun inject(exportBackupOptionsController: ExportBackupOptionsController)
  fun inject(composeBoardsController: ComposeBoardsController)
  fun inject(composeBoardsSelectorController: ComposeBoardsSelectorController)
  fun inject(compositeCatalogsSetupController: CompositeCatalogsSetupController)
  fun inject(createOrUpdateFilterController: CreateOrUpdateFilterController)
  fun inject(filterTypeSelectionController: FilterTypeSelectionController)
  fun inject(filterBoardSelectorController: FilterBoardSelectorController)
  fun inject(bookmarkGroupSettingsController: BookmarkGroupSettingsController)
  fun inject(bookmarkGroupPatternSettingsController: BookmarkGroupPatternSettingsController)
  fun inject(chan4ReportPostController: Chan4ReportPostController)
  fun inject(thirdEyeSettingsController: ThirdEyeSettingsController)
  fun inject(imageSearchController: ImageSearchController)
  fun inject(editMpvConfController: EditMpvConfController)
  fun inject(openUrlInWebViewController: OpenUrlInWebViewController)
  fun inject(createSoundMediaController: CreateSoundMediaController)
  fun inject(kurobaComposeDialogController: KurobaComposeDialogController)

  @Subcomponent.Builder
  interface Builder {
    @BindsInstance
    fun controller(controller: Controller): Builder
    @BindsInstance
    fun controllerModule(module: ControllerModule): Builder

    fun build(): ControllerComponent
  }
}