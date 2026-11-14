package org.evoionosp.abstrak.data.download.work
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadCancelReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_CANCEL_DOWNLOAD) return
        val audiobookId = intent.getStringExtra(EXTRA_BOOK) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EntryPointAccessors
                    .fromApplication(context.applicationContext, DownloadWorkerEntryPoint::class.java)
                    .downloadRepository()
                    .cancelBook(audiobookId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_CANCEL_DOWNLOAD = "org.evoionosp.abstrak.download.action.CANCEL_DOWNLOAD"
        const val EXTRA_BOOK = "audiobookId"
    }
}
