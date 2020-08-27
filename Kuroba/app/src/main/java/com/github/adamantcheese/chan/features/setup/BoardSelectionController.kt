package com.github.adamantcheese.chan.features.setup

import android.content.Context
import android.widget.FrameLayout
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyRecyclerView
import com.github.adamantcheese.chan.R
import com.github.adamantcheese.chan.features.setup.data.BoardSelectionControllerState
import com.github.adamantcheese.chan.features.setup.epoxy.selection.epoxyBoardSelectionView
import com.github.adamantcheese.chan.features.setup.epoxy.selection.epoxySiteSelectionView
import com.github.adamantcheese.chan.ui.controller.BaseFloatingController
import com.github.adamantcheese.chan.ui.epoxy.epoxyErrorView
import com.github.adamantcheese.chan.ui.epoxy.epoxyLoadingView
import com.github.adamantcheese.chan.ui.epoxy.epoxyTextView
import com.github.adamantcheese.chan.ui.layout.SearchLayout
import com.github.adamantcheese.chan.ui.view.ViewContainerWithMaxSize
import com.github.adamantcheese.chan.utils.AndroidUtils
import com.github.adamantcheese.chan.utils.plusAssign
import com.github.adamantcheese.model.data.descriptor.BoardDescriptor
import com.github.adamantcheese.model.data.descriptor.SiteDescriptor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

class BoardSelectionController(
  context: Context,
  private val callback: UserSelectionListener
) : BaseFloatingController(context), BoardSelectionView {
  private val presenter = BoardSelectionPresenter()

  private lateinit var epoxyRecyclerView: EpoxyRecyclerView
  private lateinit var searchView: SearchLayout
  private lateinit var outsideArea: FrameLayout

  private var presenting = true

  override fun getLayoutId(): Int = R.layout.controller_board_selection

  @OptIn(ExperimentalTime::class)
  override fun onCreate() {
    super.onCreate()

    epoxyRecyclerView = view.findViewById(R.id.epoxy_recycler_view)
    epoxyRecyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

    outsideArea = view.findViewById(R.id.outside_area)
    searchView = view.findViewById(R.id.search_view)

    val container = view.findViewById<ViewContainerWithMaxSize>(R.id.container_with_max_size)
    val (displayWidth, displayHeight) = AndroidUtils.getDisplaySize()
    container.maxWidth = displayWidth
    container.maxHeight = displayHeight

    outsideArea.setOnClickListener { pop() }

    mainScope.launch {
      startListeningForSearchQueries()
        .debounce(350.milliseconds)
        .collect { query -> presenter.onSearchQueryChanged(query) }
    }

    compositeDisposable += presenter.listenForStateChanges()
      .subscribe { state -> onStateChanged(state) }

    presenter.onCreate(this)
  }

  override fun onDestroy() {
    super.onDestroy()

    presenter.onDestroy()
  }

  private fun onStateChanged(state: BoardSelectionControllerState) {
    epoxyRecyclerView.withModels {
      when (state) {
        BoardSelectionControllerState.Loading -> {
          epoxyLoadingView {
            id("boards_selection_loading_view")
          }
        }
        BoardSelectionControllerState.Empty -> {
          epoxyTextView {
            id("boards_selection_empty_text_view")
            message(context.getString(R.string.controller_boards_selection_no_boards))
          }
        }
        is BoardSelectionControllerState.Error -> {
          epoxyErrorView {
            id("boards_selection_error_view")
            errorMessage(state.errorText)
          }
        }
        is BoardSelectionControllerState.Data -> {
          state.sortedSiteWithBoardsData.entries.forEach { (siteCellData, boardCellDataList) ->
            epoxySiteSelectionView {
              id("boards_selection_site_selection_view_${siteCellData.siteDescriptor}")
              bindIcon(siteCellData.siteIcon)
              bindSiteName(siteCellData.siteName)
              bindRowClickCallback {
                callback.onSiteSelected(siteCellData.siteDescriptor)
                pop()
              }
            }

            boardCellDataList.forEach { boardCellData ->
              epoxyBoardSelectionView {
                id("boards_selection_board_selection_view_${boardCellData.boardDescriptor}")
                bindBoardName(boardCellData.name)
                bindRowClickCallback {
                  callback.onBoardSelected(boardCellData.boardDescriptor)
                  pop()
                }
              }
            }
          }
        }
      }
    }
  }

  override fun onBack(): Boolean {
    if (presenting) {
      pop()
      return true
    }

    return super.onBack()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun startListeningForSearchQueries(): Flow<String> {
    return callbackFlow<String> {
      searchView.setCallback { query ->
        offer(query)
      }

      awaitClose()
    }
  }


  private fun pop() {
    presenting = false
    stopPresenting()
  }

  interface UserSelectionListener {
    fun onSiteSelected(siteDescriptor: SiteDescriptor)
    fun onBoardSelected(boardDescriptor: BoardDescriptor)
  }

}