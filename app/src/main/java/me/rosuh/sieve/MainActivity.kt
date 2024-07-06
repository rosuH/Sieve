package me.rosuh.sieve

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import me.rosuh.sieve.ui.theme.SieveTheme
import me.rosuh.sieve.utils.Logger

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SieveTheme {
                // A surface container using the 'background' color from the theme

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
                                    val mainViewModel: MainViewModel = hiltViewModel()
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
                                    val mainViewModel: MainViewModel = hiltViewModel()
                                    val subscriptionManagerState by mainViewModel.subscriptionManagerState.collectAsStateWithLifecycle()
                                    if (subscriptionManagerState.isAddSubscription) {
                                        AddSubscriptionDialog(
                                            isUrlError = subscriptionManagerState.addSubscriptionCheckFailed,
                                            onAddSubscriptionFinish = { name, url ->
                                                mainViewModel.processUIAction(
                                                    MainViewModel.UIAction.AddSubscriptionFinish(
                                                        name,
                                                        url
                                                    )
                                                )
                                            }
                                        )
                                    }
                                    SubscriptionScreen(
                                        subscriptionManagerState = subscriptionManagerState,
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
                                    Column(
                                        Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "关于"
                                        )
                                    }
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


@Composable
private fun OrderDot(text: String, isSelected: Boolean) {
    val color by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        label = "HighLightDotColor"
    )
    Text(
        text,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .padding(top = 20.dp)
            .size(36.dp)
            .background(color, CircleShape)
            .wrapContentHeight()
    )
}