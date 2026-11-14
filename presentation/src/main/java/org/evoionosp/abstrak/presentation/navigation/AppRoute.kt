package org.evoionosp.abstrak.presentation.navigation

internal const val BOOK_DETAILS_ARG_LIBRARY_ID = "libraryId"
internal const val BOOK_DETAILS_ARG_AUDIOBOOK_ID = "audiobookId"

internal enum class AppRoute(
    val route: String,
) {
    Auth("auth"),
    Home("home"),
    Library("library"),
    Search("search"),
    Downloads("downloads"),
    CatalogError("catalog_error"),
    Preferences("preferences"),
    Appearance("appearance"),
    ServerConnection("server_connection"),
    StorageDownloads("storage_downloads"),
    BookDetails("book_details/{$BOOK_DETAILS_ARG_LIBRARY_ID}/{$BOOK_DETAILS_ARG_AUDIOBOOK_ID}"),
}

internal fun bookDetailsRoute(
    libraryId: String,
    audiobookId: String,
): String = "book_details/$libraryId/$audiobookId"
