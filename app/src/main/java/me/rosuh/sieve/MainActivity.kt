package me.rosuh.sieve

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.rosuh.sieve.model.RuleMode
import me.rosuh.sieve.model.RuleRepo
import me.rosuh.sieve.model.database.RuleSubscriptionWithRules
import me.rosuh.sieve.ui.theme.SieveTheme
import me.rosuh.sieve.utils.Logger
import me.rosuh.sieve.utils.calculateDuration

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
                            NavHost(navController = navController, startDestination = Screen.Weave.route) {
                                composable(Screen.Weave.route) { backStackEntry ->
                                    val mainViewModel: MainViewModel = hiltViewModel()
                                    WeaveScreen(
                                        mainViewModel,
                                        onScan = {
                                            mainViewModel.processUIAction(
                                                MainViewModel.UIAction.Scan(
                                                    it
                                                )
                                            )
                                        },
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
                                                navController.popBackStack(Screen.Weave.route, false)
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
fun AddSubscriptionDialog(
    isUrlError: Boolean,
    onAddSubscriptionFinish: (name: String?, url: String?) -> Unit
) {
    Dialog(
        onDismissRequest = { onAddSubscriptionFinish(null, "") },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
    ) {
        Card {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "添加订阅",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                var name by remember { mutableStateOf("") }
                var url by remember { mutableStateOf("") }
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("订阅名称") },
                    placeholder = { Text("留空则由配置文件指定") }
                )
                TextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("订阅地址") },
                    placeholder = { Text("http 开头") },
                    isError = isUrlError,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onAddSubscriptionFinish(null, null) },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = { onAddSubscriptionFinish(name, url) },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SubscriptionScreen(
    subscriptionManagerState: MainViewModel.SubscriptionManagerState,
    onAddSubscription: () -> Unit = {},
    onSubscriptionSwitch: (RuleSubscriptionWithRules, Boolean) -> Unit = { _, _ -> },
    onPullToRefresh: (mode: RuleMode) -> Unit = { _ -> }
) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("订阅管理")
                }
            )
        },
        floatingActionButton = {
            Button(onClick = onAddSubscription) {
                Text("添加订阅")
            }
        }
    ) { innerPadding ->
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        val mode = if (selectedTabIndex == 0) {
            RuleMode.ByPass
        } else {
            RuleMode.Proxy
        }
        val pullToRefreshState = rememberPullToRefreshState()
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            val titles = listOf(RuleMode.ByPass.value, RuleMode.Proxy.value)
            val pagerState = rememberPagerState(pageCount = {
                titles.size
            })
            val coroutineScope = rememberCoroutineScope()
            LaunchedEffect(pagerState) {
                // Collect from the a snapshotFlow reading the currentPage
                snapshotFlow { pagerState.currentPage }.collect { page ->
                    selectedTabIndex = page
                }
            }
            if (pullToRefreshState.isRefreshing) {
                LaunchedEffect(true) {
                    onPullToRefresh(mode)
                }
            }
            LaunchedEffect(subscriptionManagerState.isRefreshing) {
                if (subscriptionManagerState.isRefreshing.not()) {
                    pullToRefreshState.endRefresh()
                }
            }
            val indicatorPadding = PaddingValues(36.dp)
            val contentPadding = remember(indicatorPadding) {
                object : PaddingValues {
                    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
                        indicatorPadding.calculateLeftPadding(layoutDirection)

                    override fun calculateTopPadding(): Dp = indicatorPadding.calculateTopPadding()

                    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
                        indicatorPadding.calculateRightPadding(layoutDirection)

                    override fun calculateBottomPadding(): Dp =
                        indicatorPadding.calculateBottomPadding()
                }
            }
            PullToRefreshContainer(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(contentPadding),
                state = pullToRefreshState
            )
            Column(Modifier.fillMaxSize()) {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    maxLines = 2,
                                    style = MaterialTheme.typography.titleMedium,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val list = if (page == 0) {
                        subscriptionManagerState.byPassSubscriptionList
                    } else {
                        subscriptionManagerState.proxySubscriptionList
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(pullToRefreshState.nestedScrollConnection)
                    ) {
                        if (list.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_empty_placeholder),
                                    modifier = Modifier.size(56.dp),
                                    contentDescription = "empty"
                                )
                                Text(
                                    text = "暂无订阅",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(list) { subscription ->
                                    val isLast = list.indexOf(subscription) == list.size - 1
                                    val modifier = if (isLast) {
                                        Modifier
                                            .padding(
                                                16.dp,
                                                top = 8.dp,
                                                bottom = (8 + 56).dp,
                                                end = 16.dp
                                            )
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 56.dp)
                                    } else {
                                        Modifier
                                            .padding(16.dp, 8.dp)
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 56.dp)
                                    }
                                    Row(
                                        modifier = modifier,
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val name = subscription.ruleSubscription.name
                                        val subTitle = "规则数: ${subscription.ruleList.size}"
                                        val isChecked = subscription.ruleSubscription.enable
                                        val updateTime =
                                            subscription.ruleSubscription.updateTimeMill.time.calculateDuration()
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        ) {
                                            Text(
                                                text = name,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                            Text(
                                                text = subTitle,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = updateTime,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Switch(
                                            checked = isChecked,
                                            onCheckedChange = {
                                                onSubscriptionSwitch(subscription, it)
                                            },
                                            thumbContent = if (isChecked) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                                    )
                                                }
                                            } else {
                                                null
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