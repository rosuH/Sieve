package me.rosuh.sieve.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.annotation.Keep

@Keep
@Serializable
data class SurfboardModel(
    @SerialName("mode")
    val mode: String,
    @SerialName("package_name")
    val packageName: List<String>
)