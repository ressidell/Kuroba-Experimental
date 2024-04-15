package com.github.k1rakishou.chan.features.drawer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateInt
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.ChanSettings
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.cache.CacheFileType
import com.github.k1rakishou.chan.features.drawer.data.HistoryControllerState
import com.github.k1rakishou.chan.features.drawer.data.NavHistoryBookmarkAdditionalInfo
import com.github.k1rakishou.chan.features.drawer.data.NavigationHistoryEntry
import com.github.k1rakishou.chan.ui.compose.ImageLoaderRequest
import com.github.k1rakishou.chan.ui.compose.ImageLoaderRequestData
import com.github.k1rakishou.chan.ui.compose.KurobaComposeImage
import com.github.k1rakishou.chan.ui.compose.components.IconTint
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeErrorMessage
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeIcon
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeProgressIndicator
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeSelectionIndicator
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeText
import com.github.k1rakishou.chan.ui.compose.components.KurobaSearchInput
import com.github.k1rakishou.chan.ui.compose.components.kurobaClickable
import com.github.k1rakishou.chan.ui.compose.ktu
import com.github.k1rakishou.chan.ui.compose.providers.LocalChanTheme
import com.github.k1rakishou.chan.ui.compose.providers.LocalWindowInsets
import com.github.k1rakishou.chan.ui.compose.search.SimpleSearchState
import com.github.k1rakishou.chan.ui.compose.search.rememberSimpleSearchState
import com.github.k1rakishou.chan.ui.compose.simpleVerticalScrollbar
import com.github.k1rakishou.core_themes.ChanTheme
import com.github.k1rakishou.core_themes.ThemeEngine
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun KurobaDrawer(
  mainControllerViewModel: MainControllerViewModel,
  onSwitchDayNightThemeIconClick: () -> Unit,
  onShowDrawerOptionIconClick: () -> Unit,
  onHistoryEntryViewClicked: (NavigationHistoryEntry) -> Unit,
  onHistoryEntryViewLongClicked: (NavigationHistoryEntry) -> Unit,
  onHistoryDeleteClicked: (NavigationHistoryEntry) -> Unit,
  onHistoryEntrySelectionChanged: (Boolean, NavigationHistoryEntry) -> Unit,
  kurobaComposeBottomPanel: @Composable () -> Unit
) {
  val chanTheme = LocalChanTheme.current
  val bgColor = chanTheme.backColorCompose

  val kurobaDrawerState = mainControllerViewModel.kurobaDrawerState
  val historyControllerState by kurobaDrawerState.historyControllerState
  val searchState = rememberSimpleSearchState<NavigationHistoryEntry>()

  val coroutineScope = rememberCoroutineScope()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(bgColor)
  ) {
    BuildNavigationHistoryListHeader(
      searchQuery = searchState.queryState,
      onSearchQueryChanged = { newQuery -> searchState.query = newQuery },
      onSwitchDayNightThemeIconClick = onSwitchDayNightThemeIconClick,
      onShowDrawerOptionsIconClick = { onShowDrawerOptionIconClick() }
    )

    when (historyControllerState) {
      HistoryControllerState.Loading -> {
        KurobaComposeProgressIndicator()
      }

      is HistoryControllerState.Error -> {
        KurobaComposeErrorMessage(
          errorMessage = (historyControllerState as HistoryControllerState.Error).errorText
        )
      }

      is HistoryControllerState.Data -> {
        val navHistoryEntryList = remember { kurobaDrawerState.navigationHistoryEntryList }
        if (navHistoryEntryList.isEmpty()) {
          KurobaComposeText(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            text = stringResource(id = R.string.drawer_controller_navigation_history_is_empty),
            textAlign = TextAlign.Center,
          )
        } else {
          BuildNavigationHistoryList(
            mainControllerViewModel = mainControllerViewModel,
            kurobaDrawerState = kurobaDrawerState,
            navHistoryEntryList = navHistoryEntryList,
            searchState = searchState,
            onHistoryEntryViewClicked = { navHistoryEntry ->
              onHistoryEntryViewClicked(navHistoryEntry)

              coroutineScope.launch {
                delay(100L)
                searchState.reset()
              }
            },
            onHistoryEntryViewLongClicked = { navHistoryEntry ->
              onHistoryEntryViewLongClicked(navHistoryEntry)
            },
            onHistoryEntrySelectionChanged = { currentlySelected, navHistoryEntry ->
              onHistoryEntrySelectionChanged(currentlySelected, navHistoryEntry)
            },
            onNavHistoryDeleteClicked = onHistoryDeleteClicked
          )
        }
      }
    }

    kurobaComposeBottomPanel()
  }
}

@Composable
private fun ColumnScope.BuildNavigationHistoryList(
  mainControllerViewModel: MainControllerViewModel,
  kurobaDrawerState: KurobaDrawerState,
  navHistoryEntryList: List<NavigationHistoryEntry>,
  searchState: SimpleSearchState<NavigationHistoryEntry>,
  onHistoryEntryViewClicked: (NavigationHistoryEntry) -> Unit,
  onHistoryEntryViewLongClicked: (NavigationHistoryEntry) -> Unit,
  onHistoryEntrySelectionChanged: (Boolean, NavigationHistoryEntry) -> Unit,
  onNavHistoryDeleteClicked: (NavigationHistoryEntry) -> Unit
) {
  LaunchedEffect(key1 = searchState.query, block = {
    if (searchState.query.isEmpty()) {
      searchState.results = navHistoryEntryList
      return@LaunchedEffect
    }

    delay(125L)

    withContext(Dispatchers.Default) {
      searchState.searching = true
      searchState.results = processSearchQuery(searchState.query, navHistoryEntryList)
      searchState.searching = false
    }
  })

  if (searchState.searching) {
    KurobaComposeProgressIndicator()
    return
  }

  val query = searchState.query
  val searchResults = searchState.results

  if (searchResults.isEmpty()) {
    KurobaComposeText(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .padding(8.dp),
      textAlign = TextAlign.Center,
      text = stringResource(id = R.string.search_nothing_found_with_query, query)
    )

    return
  }

  val selectedHistoryEntries = remember { kurobaDrawerState.selectedHistoryEntries }
  val isLowRamDevice = ChanSettings.isLowRamDevice()
  val padding by kurobaDrawerState.bottomPadding
  val contentPadding = remember(key1 = padding) { PaddingValues(bottom = 4.dp + padding.dp) }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxWidth()
      .weight(1f)
  ) {
    val chanTheme = LocalChanTheme.current
    val drawerGridMode by kurobaDrawerState.drawerGridMode

    if (drawerGridMode) {
      val state = rememberLazyGridState()

      val spanCount = with(LocalDensity.current) {
        (maxWidth.toPx() / MainController.GRID_COLUMN_WIDTH).toInt()
          .coerceIn(MainController.MIN_SPAN_COUNT, MainController.MAX_SPAN_COUNT)
      }

      LazyVerticalGrid(
        state = state,
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
          .simpleVerticalScrollbar(state, chanTheme, contentPadding),
        contentPadding = contentPadding,
        columns = GridCells.Fixed(count = spanCount),
        content = {
          items(count = searchResults.size) { index ->
            val navHistoryEntry = searchResults[index]
            val isSelectionMode = selectedHistoryEntries.isNotEmpty()
            val isSelected = selectedHistoryEntries.contains(navHistoryEntry.descriptor)

            key(searchResults[index].descriptor) {
              BuildNavigationHistoryListEntryGridMode(
                mainControllerViewModel = mainControllerViewModel,
                kurobaDrawerState = kurobaDrawerState,
                searchQuery = query,
                navHistoryEntry = navHistoryEntry,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                isLowRamDevice = isLowRamDevice,
                onHistoryEntryViewClicked = onHistoryEntryViewClicked,
                onHistoryEntryViewLongClicked = onHistoryEntryViewLongClicked,
                onHistoryEntrySelectionChanged = onHistoryEntrySelectionChanged,
                onNavHistoryDeleteClicked = onNavHistoryDeleteClicked
              )
            }
          }
        })
    } else {
      val state = rememberLazyListState()

      LazyColumn(
        state = state,
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
          .simpleVerticalScrollbar(
            state = state,
            chanTheme = chanTheme,
            contentPadding = contentPadding
          ),
        contentPadding = contentPadding,
        content = {
          items(count = searchResults.size) { index ->
            val navHistoryEntry = searchResults[index]
            val isSelectionMode = selectedHistoryEntries.isNotEmpty()
            val isSelected = selectedHistoryEntries.contains(navHistoryEntry.descriptor)

            key(searchResults[index].descriptor) {
              BuildNavigationHistoryListEntryListMode(
                mainControllerViewModel = mainControllerViewModel,
                kurobaDrawerState = kurobaDrawerState,
                searchQuery = query,
                navHistoryEntry = navHistoryEntry,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                isLowRamDevice = isLowRamDevice,
                onHistoryEntryViewClicked = onHistoryEntryViewClicked,
                onHistoryEntryViewLongClicked = onHistoryEntryViewLongClicked,
                onHistoryEntrySelectionChanged = onHistoryEntrySelectionChanged,
                onNavHistoryDeleteClicked = onNavHistoryDeleteClicked
              )
            }
          }
        })
    }
  }
}

@Composable
private fun BuildNavigationHistoryListHeader(
  searchQuery: MutableState<String>,
  onSearchQueryChanged: (String) -> Unit,
  onSwitchDayNightThemeIconClick: () -> Unit,
  onShowDrawerOptionsIconClick: () -> Unit
) {
  val chanTheme = LocalChanTheme.current
  val windowInsets = LocalWindowInsets.current
  val backgroundColor = chanTheme.primaryColorCompose

  val topInset = windowInsets.top
  val toolbarHeight = dimensionResource(id = R.dimen.toolbar_height)

  Row(
    modifier = Modifier
      .background(backgroundColor)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .height(toolbarHeight + topInset)
    ) {
      Spacer(modifier = Modifier.height(topInset))

      Row(
        modifier = Modifier
          .fillMaxHeight()
          .padding(start = 2.dp, end = 2.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.End
      ) {
        Row(
          modifier = Modifier
            .wrapContentHeight()
            .weight(1f)
        ) {
          KurobaSearchInput(
            modifier = Modifier
              .wrapContentHeight()
              .fillMaxWidth()
              .padding(start = 4.dp, end = 4.dp, top = 8.dp),
            chanTheme = chanTheme,
            onBackgroundColor = backgroundColor,
            searchQueryState = searchQuery,
            onSearchQueryChanged = onSearchQueryChanged
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        KurobaComposeIcon(
          drawableId = R.drawable.ic_baseline_wb_sunny_24,
          modifier = Modifier
            .align(Alignment.CenterVertically)
            .kurobaClickable(onClick = onSwitchDayNightThemeIconClick),
          iconTint = IconTint.TintWithColor(ThemeEngine.resolveDrawableTintColorCompose(backgroundColor))
        )

        Spacer(modifier = Modifier.width(16.dp))

        KurobaComposeIcon(
          drawableId = R.drawable.ic_more_vert_white_24dp,
          modifier = Modifier
            .align(Alignment.CenterVertically)
            .kurobaClickable(onClick = onShowDrawerOptionsIconClick),
          iconTint = IconTint.TintWithColor(ThemeEngine.resolveDrawableTintColorCompose(backgroundColor))
        )
      }
    }
  }
}

@Composable
private fun BuildNavigationHistoryListEntryListMode(
  mainControllerViewModel: MainControllerViewModel,
  kurobaDrawerState: KurobaDrawerState,
  searchQuery: String,
  navHistoryEntry: NavigationHistoryEntry,
  isSelectionMode: Boolean,
  isSelected: Boolean,
  isLowRamDevice: Boolean,
  onHistoryEntryViewClicked: (NavigationHistoryEntry) -> Unit,
  onHistoryEntryViewLongClicked: (NavigationHistoryEntry) -> Unit,
  onHistoryEntrySelectionChanged: (Boolean, NavigationHistoryEntry) -> Unit,
  onNavHistoryDeleteClicked: (NavigationHistoryEntry) -> Unit
) {
  val chanDescriptor = navHistoryEntry.descriptor
  val chanTheme = LocalChanTheme.current

  val circleCropTransformation = remember(key1 = chanDescriptor) {
    if (chanDescriptor is ChanDescriptor.ICatalogDescriptor) {
      emptyList()
    } else {
      listOf(MainController.CIRCLE_CROP)
    }
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(MainController.LIST_MODE_ROW_HEIGHT)
      .padding(all = 2.dp)
      .kurobaClickable(
        bounded = true,
        onClick = {
          if (isSelectionMode) {
            onHistoryEntrySelectionChanged(isSelected, navHistoryEntry)
          } else {
            onHistoryEntryViewClicked(navHistoryEntry)
          }
        },
        onLongClick = {
          onHistoryEntryViewLongClicked(navHistoryEntry)
        }
      ),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box {
      val contentScale = if (navHistoryEntry.descriptor is ChanDescriptor.ICatalogDescriptor) {
        ContentScale.Fit
      } else {
        ContentScale.Crop
      }

      val thumbnailRequest = remember(key1 = chanDescriptor) {
        if (navHistoryEntry.isCompositeIconUrl) {
          ImageLoaderRequest(
            data = ImageLoaderRequestData.DrawableResource(R.drawable.composition_icon),
            transformations = circleCropTransformation
          )
        } else {
          ImageLoaderRequest(
            data = ImageLoaderRequestData.Url(
              httpUrl = navHistoryEntry.threadThumbnailUrl,
              cacheFileType = CacheFileType.NavHistoryThumbnail
            ),
            transformations = circleCropTransformation
          )
        }
      }

      KurobaComposeImage(
        request = thumbnailRequest,
        contentScale = contentScale,
        modifier = Modifier
          .size(MainController.LIST_MODE_ROW_HEIGHT)
          .padding(horizontal = 6.dp, vertical = 2.dp),
        imageLoaderV2 = mainControllerViewModel.imageLoaderV2
      )

      val showDeleteButtonShortcut by remember { kurobaDrawerState.showDeleteButtonShortcut }

      if (isSelectionMode) {
        KurobaComposeSelectionIndicator(
          size = MainController.NAV_HISTORY_DELETE_BTN_SIZE,
          currentlySelected = isSelected,
          onSelectionChanged = { checked -> onHistoryEntrySelectionChanged(checked, navHistoryEntry) }
        )
      } else if (showDeleteButtonShortcut) {
        val shape = remember { CircleShape }

        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .size(MainController.NAV_HISTORY_DELETE_BTN_SIZE)
            .kurobaClickable(onClick = { onNavHistoryDeleteClicked(navHistoryEntry) })
            .background(color = MainController.NAV_HISTORY_DELETE_BTN_BG_COLOR, shape = shape)
        ) {
          Image(
            modifier = Modifier.padding(4.dp),
            painter = painterResource(id = R.drawable.ic_clear_white_24dp),
            contentDescription = null
          )
        }
      }

      Column(
        modifier = Modifier
          .wrapContentHeight()
          .wrapContentWidth()
          .align(Alignment.TopEnd)
      ) {
        val siteIconRequest = remember(key1 = chanDescriptor) {
          if (navHistoryEntry.siteThumbnailUrl != null) {
            val data = ImageLoaderRequestData.Url(
              httpUrl = navHistoryEntry.siteThumbnailUrl,
              cacheFileType = CacheFileType.SiteIcon
            )

            ImageLoaderRequest(data = data)
          } else {
            null
          }
        }

        if (siteIconRequest != null) {
          KurobaComposeImage(
            request = siteIconRequest,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(20.dp),
            imageLoaderV2 = mainControllerViewModel.imageLoaderV2,
            error = {
              Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(id = R.drawable.error_icon),
                contentDescription = null
              )
            }
          )

          Spacer(modifier = Modifier.weight(1f))
        }

        if (navHistoryEntry.pinned) {
          Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.sticky_icon),
            contentDescription = null
          )
        }
      }
    }

    KurobaComposeText(
      modifier = Modifier
        .weight(1f)
        .wrapContentHeight(),
      text = navHistoryEntry.title,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      fontSize = 16.ktu
    )

    if (navHistoryEntry.additionalInfo != null) {
      BuildAdditionalBookmarkInfoText(
        kurobaDrawerState = kurobaDrawerState,
        isLowRamDevice = isLowRamDevice,
        isListMode = true,
        searchQuery = searchQuery,
        additionalInfo = navHistoryEntry.additionalInfo,
        chanTheme = chanTheme
      )
    }
  }
}

@Composable
private fun BuildNavigationHistoryListEntryGridMode(
  mainControllerViewModel: MainControllerViewModel,
  kurobaDrawerState: KurobaDrawerState,
  searchQuery: String,
  navHistoryEntry: NavigationHistoryEntry,
  isSelectionMode: Boolean,
  isSelected: Boolean,
  isLowRamDevice: Boolean,
  onHistoryEntryViewClicked: (NavigationHistoryEntry) -> Unit,
  onHistoryEntryViewLongClicked: (NavigationHistoryEntry) -> Unit,
  onHistoryEntrySelectionChanged: (Boolean, NavigationHistoryEntry) -> Unit,
  onNavHistoryDeleteClicked: (NavigationHistoryEntry) -> Unit
) {
  val chanDescriptor = navHistoryEntry.descriptor
  val chanTheme = LocalChanTheme.current

  val siteIconRequest = remember(key1 = chanDescriptor) {
    if (navHistoryEntry.siteThumbnailUrl != null) {
      val data = ImageLoaderRequestData.Url(
        httpUrl = navHistoryEntry.siteThumbnailUrl,
        cacheFileType = CacheFileType.SiteIcon
      )

      ImageLoaderRequest(data)
    } else {
      null
    }
  }

  val thumbnailRequest = remember(key1 = chanDescriptor) {
    if (navHistoryEntry.isCompositeIconUrl) {
      ImageLoaderRequest(ImageLoaderRequestData.DrawableResource(R.drawable.composition_icon))
    } else {
      val data = ImageLoaderRequestData.Url(
        httpUrl = navHistoryEntry.threadThumbnailUrl,
        cacheFileType = CacheFileType.NavHistoryThumbnail
      )

      ImageLoaderRequest(data)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .wrapContentHeight()
      .padding(all = 2.dp)
      .kurobaClickable(
        onClick = {
          if (isSelectionMode) {
            onHistoryEntrySelectionChanged(isSelected, navHistoryEntry)
          } else {
            onHistoryEntryViewClicked(navHistoryEntry)
          }
        },
        onLongClick = {
          onHistoryEntryViewLongClicked(navHistoryEntry)
        }
      ),
  ) {
    Box {
      val contentScale = if (navHistoryEntry.descriptor is ChanDescriptor.ICatalogDescriptor) {
        ContentScale.Fit
      } else {
        ContentScale.Crop
      }

      KurobaComposeImage(
        request = thumbnailRequest,
        contentScale = contentScale,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp),
        imageLoaderV2 = mainControllerViewModel.imageLoaderV2
      )

      val showDeleteButtonShortcut by remember { kurobaDrawerState.showDeleteButtonShortcut }

      if (isSelectionMode) {
        KurobaComposeSelectionIndicator(
          size = MainController.NAV_HISTORY_DELETE_BTN_SIZE,
          currentlySelected = isSelected,
          onSelectionChanged = { checked -> onHistoryEntrySelectionChanged(checked, navHistoryEntry) }
        )
      } else if (showDeleteButtonShortcut) {
        val shape = remember { CircleShape }

        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .size(MainController.NAV_HISTORY_DELETE_BTN_SIZE)
            .kurobaClickable(onClick = { onNavHistoryDeleteClicked(navHistoryEntry) })
            .background(color = MainController.NAV_HISTORY_DELETE_BTN_BG_COLOR, shape = shape)
        ) {
          Image(
            modifier = Modifier.padding(4.dp),
            painter = painterResource(id = R.drawable.ic_clear_white_24dp),
            contentDescription = null
          )
        }
      }

      Row(
        modifier = Modifier
          .wrapContentHeight()
          .wrapContentWidth()
          .align(Alignment.TopEnd)
      ) {

        if (navHistoryEntry.pinned) {
          Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.sticky_icon),
            contentDescription = null
          )
        }

        if (siteIconRequest != null) {
          KurobaComposeImage(
            request = siteIconRequest,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(20.dp),
            imageLoaderV2 = mainControllerViewModel.imageLoaderV2,
            error = {
              Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(id = R.drawable.error_icon),
                contentDescription = null
              )
            }
          )
        }
      }
    }

    if (navHistoryEntry.additionalInfo != null) {
      BuildAdditionalBookmarkInfoText(
        kurobaDrawerState = kurobaDrawerState,
        isLowRamDevice = isLowRamDevice,
        isListMode = false,
        searchQuery = searchQuery,
        additionalInfo = navHistoryEntry.additionalInfo,
        chanTheme = chanTheme,
      )
    }

    KurobaComposeText(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight(),
      text = navHistoryEntry.title,
      maxLines = 4,
      fontSize = 12.ktu,
      textAlign = TextAlign.Center
    )
  }
}

@Composable
private fun BuildAdditionalBookmarkInfoText(
  kurobaDrawerState: KurobaDrawerState,
  isLowRamDevice: Boolean,
  isListMode: Boolean,
  searchQuery: String,
  additionalInfo: NavHistoryBookmarkAdditionalInfo,
  chanTheme: ChanTheme
) {
  val drawerOpened by kurobaDrawerState.drawerOpenedState

  // Now this is epic. So what this thing does (and ^ this one above), they receive all changes
  // to navigation history elements and if the drawer is currently opened display them (with or
  // without animation depending on settings and other stuff) but if the drawer is currently closed
  // then the changes are accumulated and the next time the drawer is opened the difference is
  // displayed. Basically once you open the drawer you will see the changes applied to bookmarks
  // during the time the drawer was closed.
  val prevAdditionalInfoState = remember { mutableStateOf(additionalInfo.copy()) }
  val prevAdditionalInfo by prevAdditionalInfoState

  val currentAdditionalInfo = if (drawerOpened) {
    additionalInfo
  } else {
    prevAdditionalInfo
  }

  val transition = updateTransition(
    targetState = currentAdditionalInfo,
    label = "Text transition animation"
  )

  val animationDisabled = isLowRamDevice
    || searchQuery.isNotEmpty()
    || !drawerOpened
    || prevAdditionalInfo == currentAdditionalInfo

  val textAnimationSpec: FiniteAnimationSpec<Int> = if (animationDisabled) {
    // This will disable animations, basically it will switch to the final animation frame right
    // away
    snap()
  } else {
    tween(durationMillis = MainController.TEXT_ANIMATION_DURATION)
  }

  val newPostsCountAnimated by transition.animateInt(
    transitionSpec = { textAnimationSpec },
    label = "New posts animation"
  ) { info -> info.newPosts }
  val newQuotesCountAnimated by transition.animateInt(
    transitionSpec = { textAnimationSpec },
    label = "New quotes animation"
  ) { info -> info.newQuotes }

  val additionalInfoString = remember(
    additionalInfo,
    chanTheme,
    newPostsCountAnimated,
    newQuotesCountAnimated
  ) {
    return@remember currentAdditionalInfo.toAnnotatedString(
      chanTheme = chanTheme,
      newPostsCount = newPostsCountAnimated,
      newQuotesCount = newQuotesCountAnimated
    )
  }


  val targetColor = if (transition.isRunning) {
    val alpha = .35f

    when {
      currentAdditionalInfo.newQuotes > 0 -> {
        chanTheme.bookmarkCounterHasRepliesColorCompose.copy(alpha = alpha)
      }

      currentAdditionalInfo.newPosts > 0 || currentAdditionalInfo.watching -> {
        chanTheme.bookmarkCounterNormalColorCompose.copy(alpha = alpha)
      }

      else -> {
        chanTheme.bookmarkCounterNotWatchingColorCompose.copy(alpha = alpha)
      }
    }
  } else {
    Color.Unspecified
  }

  val shape = if (transition.isRunning) {
    RoundedCornerShape(4.dp)
  } else {
    RectangleShape
  }

  val bgAnimationSpec: AnimationSpec<Color> = if (animationDisabled) {
    snap()
  } else {
    tween(durationMillis = MainController.TEXT_ANIMATION_DURATION)
  }

  val backgroundColor by animateColorAsState(
    targetValue = targetColor,
    animationSpec = bgAnimationSpec
  )

  if (isListMode) {
    KurobaComposeText(
      modifier = Modifier
        .wrapContentWidth()
        .wrapContentHeight()
        .background(color = backgroundColor, shape = shape)
        .padding(horizontal = 6.dp),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      fontSize = 16.ktu,
      text = additionalInfoString
    )
  } else {
    KurobaComposeText(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .background(color = backgroundColor, shape = shape),
      maxLines = 1,
      textAlign = TextAlign.Center,
      overflow = TextOverflow.Ellipsis,
      fontSize = 14.ktu,
      text = additionalInfoString
    )
  }

  SideEffect {
    if (drawerOpened) {
      prevAdditionalInfoState.value = additionalInfo.copy()
    }
  }
}

private fun processSearchQuery(
  query: String,
  navHistoryEntryList: List<NavigationHistoryEntry>
): List<NavigationHistoryEntry> {
  if (query.isEmpty()) {
    return navHistoryEntryList
  }

  return navHistoryEntryList.filter { navigationHistoryEntry ->
    navigationHistoryEntry.title.contains(other = query, ignoreCase = true)
  }
}