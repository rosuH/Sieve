package me.rosuh.sieve.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.annotation.Keep
import kotlinx.collections.immutable.ImmutableList

@Keep
@Serializable
data class FlClashModel(
    @SerialName("acceptList")
    val acceptList: List<String>,
    @SerialName("isFilterSystemApp")
    val isFilterSystemApp: Boolean,
    @SerialName("mode")
    val mode: String,
    @SerialName("rejectList")
    val rejectList: List<String>,
    @SerialName("sort")
    val sort: String
)