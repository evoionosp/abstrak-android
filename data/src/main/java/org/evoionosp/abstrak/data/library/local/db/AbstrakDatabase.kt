package org.evoionosp.abstrak.data.library.local.db
import androidx.room.Database
import androidx.room.RoomDatabase
import org.evoionosp.abstrak.data.audiobook.local.dao.AudiobookDao
import org.evoionosp.abstrak.data.audiobook.local.dao.AudiobookDetailDao
import org.evoionosp.abstrak.data.audiobook.local.dao.ContinueListeningDao
import org.evoionosp.abstrak.data.audiobook.local.dao.PendingProgressDao
import org.evoionosp.abstrak.data.audiobook.local.entity.AudiobookChapterEntity
import org.evoionosp.abstrak.data.audiobook.local.entity.AudiobookDetailEntity
import org.evoionosp.abstrak.data.audiobook.local.entity.AudiobookEntity
import org.evoionosp.abstrak.data.audiobook.local.entity.AudiobookTrackEntity
import org.evoionosp.abstrak.data.audiobook.local.entity.ContinueListeningEntity
import org.evoionosp.abstrak.data.audiobook.local.entity.PendingProgressEntity
import org.evoionosp.abstrak.data.download.local.dao.TrackDownloadDao
import org.evoionosp.abstrak.data.download.local.entity.TrackDownloadEntity
import org.evoionosp.abstrak.data.library.local.dao.LibraryDao
import org.evoionosp.abstrak.data.library.local.dao.LibrarySyncStateDao
import org.evoionosp.abstrak.data.library.local.entity.LibraryEntity
import org.evoionosp.abstrak.data.library.local.entity.LibrarySyncStateEntity

@Database(
    entities = [
        LibraryEntity::class,
        AudiobookEntity::class,
        AudiobookDetailEntity::class,
        AudiobookChapterEntity::class,
        AudiobookTrackEntity::class,
        ContinueListeningEntity::class,
        PendingProgressEntity::class,
        LibrarySyncStateEntity::class,
        TrackDownloadEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AbstrakDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    abstract fun audiobookDao(): AudiobookDao

    abstract fun audiobookDetailDao(): AudiobookDetailDao

    abstract fun continueListeningDao(): ContinueListeningDao

    abstract fun pendingProgressDao(): PendingProgressDao

    abstract fun librarySyncStateDao(): LibrarySyncStateDao

    abstract fun trackDownloadDao(): TrackDownloadDao
}
