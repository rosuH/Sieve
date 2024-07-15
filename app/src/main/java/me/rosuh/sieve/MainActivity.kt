package me.rosuh.sieve

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import me.rosuh.sieve.ui.theme.AppTheme
import me.rosuh.sieve.utils.Logger

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    val navController = rememberNavController()
                    Column(Modifier.fillMaxSize()) {
                        var selectedItem by remember { mutableIntStateOf(0) }
                        val items = listOf(
                            Screen.Weave.route to Screen.Weave.icon,
                            Screen.Subscription.route to Screen.Subscription.icon,
                            Screen.About.route to Screen.About.icon
                        )
                        Surface(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            val mainViewModel: MainViewModel = hiltViewModel()
                            navController.addOnDestinationChangedListener { controller, destination, arguments ->
                                Logger.d(TAG, "onDestinationChanged: ${destination.label}")
                                selectedItem = when (destination.route) {
                                    Screen.Subscription.route -> 1
                                    Screen.About.route -> 2
                                    else -> 0
                                }
                            }
                            NavHost(
                                navController = navController,
                                startDestination = Screen.Weave.route
                            ) {
                                composable(Screen.Weave.route) { backStackEntry ->
                                    val onScan = remember {
                                        {
                                            mainViewModel.processUIAction(
                                                MainViewModel.UIAction.Scan(
                                                    packageManager
                                                )
                                            )
                                        }
                                    }
                                    WeaveScreen(
                                        mainViewModel,
                                        onScan = onScan,
                                        onChangeMode = { mode ->
                                            mainViewModel.processUIAction(
                                                MainViewModel.UIAction.ChangeMode(mode)
                                            )
                                        },
                                        onFilter = { list, mode ->
                                            mainViewModel.processUIAction(
                                                MainViewModel.UIAction.Filter(list, mode)
                                            )
                                        },
                                        onExport = { list, mode, type ->
                                            mainViewModel.processUIAction(
                                                MainViewModel.UIAction.Export(list, mode, type)
                                            )
                                        }
                                    )
                                }
                                composable(Screen.Subscription.route) {
                                    SubscriptionScreen(
                                        viewModel = mainViewModel,
                                        onAddSubscription = {
                                            mainViewModel.processUIAction(
                                                MainViewModel.UIAction.AddSubscription
                                            )
                                        },
                                        onSubscriptionSwitch = { subscription, isChecked ->
                                            mainViewModel.processUIAction(
                                                MainViewModel.UIAction.SubscriptionSwitch(
                                                    subscription,
                                                    isChecked
                                                )
                                            )
                                        },
                                        onPullToRefresh = {
                                            mainViewModel.processUIAction(
                                                MainViewModel.UIAction.SubscriptionPullToRefresh(it)
                                            )
                                        }
                                    )
                                }
                                composable(Screen.About.route) {
                                    AboutScreen()
                                }
                            }
                        }
                        NavigationBar(
                            Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        ) {
                            items.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            painterResource(id = item.second),
                                            contentDescription = item.first,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text(item.first) },
                                    selected = selectedItem == index,
                                    onClick = {
                                        when (index) {
                                            0 -> {
                                                navController.popBackStack(
                                                    Screen.Weave.route,
                                                    false
                                                )
                                            }

                                            1 -> {
                                                navController.navigate(Screen.Subscription.route) {
                                                    popUpTo(Screen.About.route) {
                                                        inclusive = true
                                                    }
                                                }
                                            }

                                            2 -> {
                                                navController.navigate(Screen.About.route) {
                                                    popUpTo(Screen.Subscription.route) {
                                                        inclusive = true
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
