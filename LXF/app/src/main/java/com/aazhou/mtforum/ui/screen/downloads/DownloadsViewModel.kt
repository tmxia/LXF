package com.aazhou.lxforum.ui.screen.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aazhou.lxforum.data.download.ThreadArchiveStore
import com.aazhou.lxforum.data.model.DownloadedThread
import com.aazhou.lxforum.data.repo.DownloadStore
import com.aazhou.lxforum.ui.common.deleteThreadDownload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch





class DownloadsViewModel(
    private val downloadStore: DownloadStore,
    private val archiveStore: ThreadArchiveStore,
) : ViewModel() {

    val items: Flow<List<DownloadedThread>> = downloadStore.items

    fun delete(tid: String) {
        viewModelScope.launch {
            deleteThreadDownload(tid, downloadStore, archiveStore)
        }
    }
}
