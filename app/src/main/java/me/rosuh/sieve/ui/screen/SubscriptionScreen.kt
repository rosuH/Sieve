package me.rosuh.sieve.ui.screen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.rosuh.sieve.MainViewModel
import me.rosuh.sieve.R
import me.rosuh.sieve.model.RuleMode
import me.rosuh.sieve.model.database.RuleSubscriptionWithRules
import me.rosuh.sieve.utils.calculateDuration

@Stable
class SubscriptionManagerState(
    isProxyModeRefreshing: Boolean = false,
    isBypassModeRefreshing: Boolean = false,
    isLoadingBypass: Boolean = false,
    isLoadingProxy: Boolean = false,
    showAddSubscriptionDialog: Boolean = false,
    isAddingSubscription: Boolean = false,
    isAddSubscriptionSuccess: Boolean = false,
    isAddSubscriptionFailed: Boolean = false,
    addSubscriptionFailedTips: String = "",
    addSubscriptionCheckFailed: Boolean = false,
    byPassSubscriptionList: List<RuleSubscriptionWithRules> = emptyList(),
    proxySubscriptionList: List<RuleSubscriptionWithRules> = emptyList(),
) {
    var isProxyModeRefreshing: Boolean by mutableStateOf(isProxyModeRefreshing)
    var isBypassModeRefreshing: Boolean by mutableStateOf(isProxyModeRefreshing)
    var showAddSubscriptionDialog: Boolean by mutableStateOf(showAddSubscriptionDialog)
    var isAddingSubscription: Boolean by mutableStateOf(isAddingSubscription)
    var isAddSubscriptionFailed: Boolean by mutableStateOf(isAddSubscriptionFailed)
    var isAddSubscriptionSuccess: Boolean by mutableStateOf(isAddSubscriptionSuccess)
    var addSubscriptionFailedTips: String by mutableStateOf(addSubscriptionFailedTips)
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
    onPullToRefresh: (mode: RuleMode) -> Unit = { _ -> },
    onBackPress: () -> Unit = {}
) {
    BackHandler { onBackPress() }
    val subscriptionManagerState = remember {
        viewModel.subscriptionManagerState
    }
    val byPassSubscriptionList by subscriptionManagerState.byPassSubscriptionList.collectAsStateWithLifecycle()
    val proxySubscriptionList by subscriptionManagerState.proxySubscriptionList.collectAsStateWithLifecycle()
    val context = LocalContext.current
    if (subscriptionManagerState.showAddSubscriptionDialog) {
        AddSubscriptionDialog(
            isUrlError = subscriptionManagerState.addSubscriptionCheckFailed,
            isAddingSubscription = subscriptionManagerState.isAddingSubscription,
            isAddSubscriptionFailed = subscriptionManagerState.isAddSubscriptionFailed,
            isAddSubscriptionSuccess = subscriptionManagerState.isAddSubscriptionSuccess,
            onAddSubscriptionFinish = { name, url ->
                viewModel.processUIAction(
                    MainViewModel.UIAction.AddSubscriptionIng(
                        name,
                        url,
                        context.filesDir
                    )
                )
            }
        )
    }
    LaunchedEffect(subscriptionManagerState.isAddSubscriptionFailed) {
        if (subscriptionManagerState.isAddSubscriptionFailed) {
            Toast.makeText(
                context,
                subscriptionManagerState.addSubscriptionFailedTips,
                Toast.LENGTH_SHORT
            ).show()
            // Reset the error state after showing the toast
            viewModel.processUIAction(MainViewModel.UIAction.ResetAddSubscriptionError)
        }
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
            Button(modifier = Modifier.padding(bottom = 20.dp), onClick = onAddSubscription) {
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
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
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
                    val pullToRefreshState = rememberPullToRefreshState()
                    val isRefreshing = if (page == 0) {
                        subscriptionManagerState.isBypassModeRefreshing
                    } else {
                        subscriptionManagerState.isProxyModeRefreshing
                    }
                    PullToRefreshBox(
                        modifier = Modifier,
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            onPullToRefresh(mode)
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            if (list.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val painter = if (isSystemInDarkTheme()) {
                                        R.drawable.ic_empty_placeholder_dark
                                    } else {
                                        R.drawable.ic_empty_placeholder_light
                                    }
                                    Image(
                                        painter = painterResource(painter),
                                        modifier = Modifier.size(78.dp),
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
                                        val isLast = list.size > 1 && list.indexOf(subscription) == list.size - 1
                                        SubscriptionItem(
                                            isLast,
                                            subscription = subscription,
                                            onSubscriptionSwitch = onSubscriptionSwitch,
                                            onEdit = {
                                                viewModel.processUIAction(
                                                    MainViewModel.UIAction.EditSubscription(it)
                                                )
                                            },
                                            onDelete = {
                                                viewModel.processUIAction(
                                                    MainViewModel.UIAction.DeleteSubscription(it)
                                                )
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
private fun SubscriptionItem(
    isLast: Boolean,
    modifier: Modifier = Modifier,
    subscription: RuleSubscriptionWithRules,
    onSubscriptionSwitch: (RuleSubscriptionWithRules, Boolean) -> Unit,
    onEdit: (subscription: RuleSubscriptionWithRules) -> Unit,
    onDelete: (subscription: RuleSubscriptionWithRules) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var touchX by remember { mutableStateOf(0f) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val innerModifier = if (isLast) {
        modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touchX = offset.x
                    },
                    onLongPress = {
                        expanded = !expanded
                    }
                )
            }
            .padding(
                16.dp,
                top = 8.dp,
                bottom = (8 + 56).dp,
                end = 16.dp
            )
            .fillMaxWidth()
    } else {
        modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touchX = offset.x
                    },
                    onLongPress = {
                        expanded = !expanded
                    }
                )
            }
            .fillMaxWidth()
            .padding(16.dp, 8.dp)
    }

    Row(
        modifier = innerModifier,
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
    Box(modifier = Modifier) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.align(Alignment.TopStart),
            offset = DpOffset((touchX / LocalDensity.current.density).dp, 0.dp),
        ) {
            DropdownMenuItem(onClick = {
                expanded = false
                onEdit(subscription)
            }, text = { Text("编辑") })
            DropdownMenuItem(onClick = {
                expanded = false
                showDeleteConfirmation = true
            }, text = { Text("删除") })
        }
    }
    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("确认删除") },
            text = { Text("您确定要删除此订阅吗？") },
            confirmButton = {
                Button(onClick = {
                    onDelete(subscription)
                    showDeleteConfirmation = false
                }) {
                    Text("删除")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmation = false }) {
                    Text("取消")
                }
            }
        )
    }
}


@Composable
fun AddSubscriptionDialog(
    isUrlError: Boolean,
    isAddingSubscription: Boolean = false,
    isAddSubscriptionFailed: Boolean = false,
    isAddSubscriptionSuccess: Boolean = false,
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
                val name by remember { mutableStateOf("") }
                var url by remember { mutableStateOf("") }
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onAddSubscriptionFinish(null, null) },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("取消")
                    }
                    Box(
                        modifier = Modifier.width(88.dp), // 固定宽度，确保空间一致
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isAddingSubscription,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isAddingSubscription,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            TextButton(
                                onClick = { onAddSubscriptionFinish(name, url) },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text("确定")
                            }
                        }
                    }
                }
            }
        }
    }
}