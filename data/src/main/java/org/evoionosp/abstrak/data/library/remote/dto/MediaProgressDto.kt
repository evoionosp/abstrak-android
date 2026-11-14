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
)
