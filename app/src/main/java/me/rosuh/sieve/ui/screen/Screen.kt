package me.rosuh.sieve.ui.screen

import androidx.navigation.NamedNavArgument
import me.rosuh.sieve.R

sealed class Screen(
    val route: String,
    val navArguments: List<NamedNavArgument> = emptyList()
) {
    data object Weave : Screen("weave") {
        val icon by lazy { R.drawable.ic_home_nav }
        val title by lazy { R.string.tab_home }
    }

    data object Subscription : Screen("subscription") {
        val icon by lazy { R.drawable.ic_subscription_nav }
        val title by lazy { R.string.tab_subscription }
    }

    data object Setting : Screen("setting") {
        val icon by lazy { R.drawable.ic_setting_nav }
        val title by lazy { R.string.tab_setting }
    }
}
