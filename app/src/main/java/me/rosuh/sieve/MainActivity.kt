package me.rosuh.sieve

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import me.rosuh.sieve.ui.screen.Screen
import me.rosuh.sieve.ui.screen.SettingScreen
import me.rosuh.sieve.ui.screen.SubscriptionScreen
import me.rosuh.sieve.ui.screen.HomeScreen
import me.rosuh.sieve.ui.theme.AppTheme

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
                Surface(modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()) {
                    val navController = rememberNavController()
                    var subscriptionEnterTransition = remember {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    }
                    Box(Modifier.fillMaxSize()) {
                        var selectedItem by remember { mutableIntStateOf(0) }
                        val items = listOf(
                            stringResource(id = Screen.Home.title) to Screen.Home.icon,
                            stringResource(id = Screen.Subscription.title) to Screen.Subscription.icon,
                            stringResource(id = Screen.Setting.title) to Screen.Setting.icon
                        )
                        val transitionDuration = 350
                        Surface(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 80.dp)
                        ) {
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
                            NavHost(
                                navController = navController,
                                startDestination = Screen.Home.route
                            ) {
                                composable(
                                    Screen.Home.route,
                                    exitTransition = {
                                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(transitionDuration))
                                    },
                                    popEnterTransition = {
                                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(transitionDuration))
                                    }
                                ) { backStackEntry ->
                                    HomeScreen(
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
                                composable(
                                    Screen.Subscription.route,
                                    enterTransition = {
                                        slideIntoContainer(subscriptionEnterTransition, animationSpec = tween(transitionDuration))
                                    },
                                    exitTransition = {
                                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(transitionDuration))
                                    },
                                    popEnterTransition = {
                                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(transitionDuration))
                                    },
                                    popExitTransition = {
                                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(transitionDuration))
                                    }
                                ) {
                                    val context = LocalContext.current
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
                                                MainViewModel.UIAction.SubscriptionPullToRefresh(it, context.filesDir)
                                            )
                                        },
                                        onBackPress = {
                                            selectedItem = 0
                                        }
                                    )
                                }
                                composable(
                                    Screen.Setting.route,
                                    enterTransition = {
                                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(transitionDuration))
                                    },
                                    exitTransition = {
                                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(transitionDuration))
                                    },
                                    popEnterTransition = {
                                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(transitionDuration))
                                    },
                                    popExitTransition = {
                                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(transitionDuration))
                                    }
                                ) {
                                    SettingScreen {
                                        selectedItem = 0
                                    }
                                }
                            }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth(0.6f)
                                .padding(bottom = 16.dp)
                                .shadow(1.dp, shape = MaterialTheme.shapes.large)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainer,
                                    shape = MaterialTheme.shapes.large
                                )
                                .align(Alignment.BottomCenter)
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
                                    label = { Text(item.first, style = MaterialTheme.typography.bodySmall, maxLines = 1) },
                                    selected = selectedItem == index,
                                    onClick = {
                                        selectedItem = index
                                    }
                                )
                            }
                        }
                        when (selectedItem) {
                            0 -> {
                                navController.popBackStack(Screen.Home.route, false)
                            }

                            1 -> {
                                subscriptionEnterTransition = if (navController.currentDestination?.route != Screen.Setting.route) {
                                    AnimatedContentTransitionScope.SlideDirection.Left
                                } else {
                                    AnimatedContentTransitionScope.SlideDirection.Right
                                }
                                navController.navigate(Screen.Subscription.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }

                            2 -> {
                                navController.navigate(Screen.Setting.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}
