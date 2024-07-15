package me.rosuh.sieve

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.rosuh.sieve.model.RuleMode
import me.rosuh.sieve.model.database.RuleSubscriptionWithRules
import me.rosuh.sieve.utils.calculateDuration

@Stable
class SubscriptionManagerState(
    isRefreshing: Boolean = true,
    isLoadingBypass: Boolean = false,
    isLoadingProxy: Boolean = false,
    isAddSubscription: Boolean = false,
    addSubscriptionCheckFailed: Boolean = false,
    byPassSubscriptionList: List<RuleSubscriptionWithRules> = emptyList(),
    proxySubscriptionList: List<RuleSubscriptionWithRules> = emptyList(),
) {
    var isRefreshing: Boolean by mutableStateOf(isRefreshing)
    var isLoadingBypass: Boolean by mutableStateOf(isLoadingBypass)
    var isLoadingProxy: Boolean by mutableStateOf(isLoadingProxy)
    var isAddSubscription: Boolean by mutableStateOf(isAddSubscription)
    var addSubscriptionCheckFailed: Boolean by mutableStateOf(addSubscriptionCheckFailed)

    private var _byPassSubscriptionList = MutableStateFlow(byPassSubscriptionList)
    private var _proxySubscriptionList = MutableStateFlow(proxySubscriptionList)

    val byPassSubscriptionList: StateFlow<List<RuleSubscriptionWithRules>> = _byPassSubscriptionList
    val proxySubscriptionList: StateFlow<List<RuleSubscriptionWithRules>> = _proxySubscriptionList

    fun updateByPassSubscriptionList(list: List<RuleSubscriptionWithRules>) {
        _byPassSubscriptionList.value = list
    }

    fun updateProxySubscriptionList(list: List<RuleSubscriptionWithRules>) {
        _proxySubscriptionList.value = list
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SubscriptionScreen(
    viewModel: MainViewModel,
    onAddSubscription: () -> Unit = {},
    onSubscriptionSwitch: (RuleSubscriptionWithRules, Boolean) -> Unit = { _, _ -> },
    onPullToRefresh: (mode: RuleMode) -> Unit = { _ -> }
) {
    val subscriptionManagerState = remember {
        viewModel.subscriptionManagerState
    }
    val byPassSubscriptionList by subscriptionManagerState.byPassSubscriptionList.collectAsStateWithLifecycle()
    val proxySubscriptionList by subscriptionManagerState.proxySubscriptionList.collectAsStateWithLifecycle()
    if (subscriptionManagerState.isAddSubscription) {
        AddSubscriptionDialog(
            isUrlError = subscriptionManagerState.addSubscriptionCheckFailed,
            onAddSubscriptionFinish = { name, url ->
                viewModel.processUIAction(
                    MainViewModel.UIAction.AddSubscriptionFinish(
                        name,
                        url
                    )
                )
            }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
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
                        byPassSubscriptionList
                    } else {
                        proxySubscriptionList
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
//                TextField(
//                    value = name,
//                    onValueChange = { name = it },
//                    label = { Text("订阅名称") },
//                    placeholder = { Text("留空则由配置文件指定") }
//                )
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