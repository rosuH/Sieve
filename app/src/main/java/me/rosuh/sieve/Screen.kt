package me.rosuh.sieve

import androidx.navigation.NamedNavArgument

sealed class Screen(
    val route: String,
    val navArguments: List<NamedNavArgument> = emptyList()
) {
    data object Weave : Screen("weave") {
        val icon by lazy { R.drawable.ic_home_nav }
    }

    data object Subscription : Screen("subscription") {
        val icon by lazy { R.drawable.ic_subscription_nav }
    }

    data object About : Screen("about") {
        val icon by lazy { R.drawable.ic_setting_nav }
    }
}
