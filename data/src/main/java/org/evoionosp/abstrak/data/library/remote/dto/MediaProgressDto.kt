package org.evoionosp.abstrak.data.library.remote.dto
import com.google.gson.annotations.SerializedName

data class MediaProgressDto(
    @SerializedName("currentTime")
    val currentTime: Double? = null,
    @SerializedName("duration")
    val duration: Double? = null,
    @SerializedName("progress")
    val progress: Double? = null,
    @SerializedName("isFinished")
    val isFinished: Boolean? = null,
    @SerializedName("lastUpdate")
    val lastUpdate: Long? = null,
)

data class UpdateProgressRequestDto(
    @SerializedName("currentTime")
    val currentTime: Double,
    @SerializedName("duration")
    val duration: Double?,
    @SerializedName("progress")
    val progress: Double,
    @SerializedName("isFinished")
    val isFinished: Boolean,
    /**
     * When the position was recorded on this device, Unix epoch milliseconds. Audiobookshelf
     * keeps it as the progress lastUpdate instead of the time the request arrives; null (Gson
     * omits it) lets the server stamp its own time.
     */
    @SerializedName("lastUpdate")
    val lastUpdate: Long? = null,
)
