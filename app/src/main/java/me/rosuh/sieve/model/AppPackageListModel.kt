package me.rosuh.sieve.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AppPackageListModel : ArrayList<AppPackageListModel.AppPackageListModelItem>(){
    @Serializable
    data class AppPackageListModelItem(
        @SerialName("name")
        val name: String,
        @SerialName("package")
        val packageX: String
    )
}