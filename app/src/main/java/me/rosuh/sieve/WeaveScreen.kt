package me.rosuh.sieve

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.rosuh.sieve.model.AppInfo
import me.rosuh.sieve.model.RuleMode
import me.rosuh.sieve.model.RuleRepo
import me.rosuh.sieve.model.AppList
import me.rosuh.sieve.model.database.RuleSubscriptionWithRules
import me.rosuh.sieve.model.database.StableRuleSubscriptionWithRules
import me.rosuh.sieve.utils.Logger
import me.rosuh.sieve.utils.calculateDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeaveScreen(
    viewModel: MainViewModel,
    onScan: () -> Unit = {},
    onChangeMode: (mode: RuleMode) -> Unit = {},
    onFilter: (List<AppInfo>, mode: RuleMode) -> Unit = { _, _ -> },
    onExport: (List<AppInfo>, mode: RuleMode, exportType: RuleRepo.ExportType) -> Unit = { _, _, _ -> },
) {
    val weaveState = remember {
        viewModel.weaveState
    }
    val installPackageList by weaveState.installPackageList.collectAsStateWithLifecycle()
    val userPackageList by weaveState.userPackageList.collectAsStateWithLifecycle()
    val isInit = weaveState.isInit
    val onScanRemember by rememberUpdatedState(onScan)
    LaunchedEffect(isInit) {
        if (isInit) {
            viewModel.processUIAction(MainViewModel.UIAction.Init)
            onScan()
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val rowModifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)
        val cardModifier = Modifier
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp)
            .fillMaxWidth()
            .aspectRatio(2f)
        val titleModifier = Modifier
        val buttonModifier = Modifier.padding(start = 16.dp, top = 12.dp)
        Text(
            text = "制作列表",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 24.dp)
        )
        Column(modifier = Modifier) {
            AppListCard(
                installPackageSize = installPackageList.size,
                userPackageList,
                latestScanTime = weaveState.latestScanTime,
                onScan = onScanRemember,
            )
            Column(modifier = rowModifier) {
                val paddingStart = 16.dp
                Text(
                    "规则",
                    modifier = titleModifier,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "对 APP 列表应用指定规则，过滤后即可导出",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = paddingStart, top = 8.dp)
                )
                Row(Modifier.padding(start = paddingStart, top = 12.dp)) {
                    FilterChip(
                        modifier = Modifier.animateContentSize(),
                        onClick = { onChangeMode(RuleMode.ByPass) },
                        label = {
                            Text(RuleMode.ByPass.value)
                        },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = weaveState.mode == RuleMode.ByPass
                        ),
                        selected = weaveState.mode == RuleMode.ByPass,
                        leadingIcon = {
                            if (weaveState.mode == RuleMode.ByPass) {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = "Selected Bypass Icon",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            } else {
                                Spacer(modifier = Modifier.size(FilterChipDefaults.IconSize))

                            }
                        },
                    )
                    FilterChip(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .animateContentSize(),
                        onClick = { onChangeMode(RuleMode.Proxy) },
                        label = {
                            Text(RuleMode.Proxy.value)
                        },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            weaveState.mode == RuleMode.Proxy
                        ),
                        selected = weaveState.mode == RuleMode.Proxy,
                        leadingIcon = if (weaveState.mode == RuleMode.Proxy) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = "Selected Proxy Icon",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else {
                            {
                                Spacer(modifier = Modifier.size(FilterChipDefaults.IconSize))
                            }
                        },
                    )
                }
                Row(modifier = Modifier.padding(start = paddingStart, top = 8.dp)) {
                    Text(
                        text = "已启用订阅："
                    )
                    Spacer(modifier = Modifier.size(2.dp))
                    AnimatedContent(
                        targetState = weaveState.subscription.size,
                        label = "subscriptionSize"
                    ) { targetState ->
                        Text(
                            text = targetState.toString()
                        )
                    }
                }
                Row(modifier = Modifier.padding(start = paddingStart, top = 8.dp)) {
                    Text(
                        text = "规则总计："
                    )
                    Spacer(modifier = Modifier.size(2.dp))
                    AnimatedContent(
                        targetState = weaveState.subscription.sumOf { it.ruleList.size },
                        label = "subscriptionSize"
                    ) { targetState ->
                        Text(
                            text = targetState.toString()
                        )
                    }
                }
            }
            Column(modifier = rowModifier) {
                val context = LocalContext.current
                Text(
                    "导出列表",
                    modifier = titleModifier,
                    style = MaterialTheme.typography.titleLarge
                )
                val exportEnable = weaveState.isExporting.not()
                val iconModifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                Row {
                    IconButton(
                        modifier = buttonModifier.size(48.dp),
                        enabled = exportEnable,
                        onClick = {
                            onExport(
                                installPackageList,
                                weaveState.mode,
                                RuleRepo.ExportType.Surfboard
                            )
                        }) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_surfboard),
                            contentDescription = "Surfboard",
                            modifier = iconModifier
                        )
                    }
                    IconButton(
                        modifier = buttonModifier.size(48.dp),
                        enabled = exportEnable,
                        onClick = {
                            onExport(
                                installPackageList,
                                weaveState.mode,
                                RuleRepo.ExportType.NekoBoxAndroid
                            )
                        }) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_nbfa),
                            contentDescription = "NekoBoxForAndroid",
                            modifier = iconModifier
                        )
                    }
                }
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    val modifier = Modifier.padding(start = 8.dp)
                    when {
                        weaveState.isExporting -> {
                            Text(
                                text = "导出中",
                                modifier = modifier,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        weaveState.isExportFailed -> {
                            Text(
                                text = "失败, ${weaveState.exportMsg}",
                                modifier = modifier,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        weaveState.isExportSuccess -> {
                            Text(
                                text = "成功, ${weaveState.exportMsg}",
                                modifier = modifier,
                                style = MaterialTheme.typography.labelMedium
                            )
                            val clipboardManager = LocalClipboardManager.current
                            val textToCopy by remember { mutableStateOf(weaveState.exportResult) }
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            Logger.d("WeaveScreen", "Copy to clipboard: $textToCopy")
                        }
                    }
                }
            }
        }
    }
}


@Stable
class WeaveState(
    isInit: Boolean = true,
    isScanningPackage: Boolean = false,
    isFiltering: Boolean = false,
    isFilterFailed: Boolean = false,
    isExporting: Boolean = false,
    isExportFailed: Boolean = false,
    isExportSuccess: Boolean = false,
    latestScanTime: Long = 0L,
    filterMsg: String = "规则应用中...",
    exportMsg: String = "规则导出中...",
    mode: RuleMode = RuleMode.ByPass,
    installList: AppList = AppList.empty,
    userPackageList: AppList = AppList.empty,
    subscription: StableRuleSubscriptionWithRules = StableRuleSubscriptionWithRules.empty,
    exportResult: String = "",
) {

    var isInit by mutableStateOf(isInit)
    var isScanningPackage by mutableStateOf(isScanningPackage)
    var isFiltering by mutableStateOf(isFiltering)
    var isFilterFailed by mutableStateOf(isFilterFailed)
    var isExporting by mutableStateOf(isExporting)
    var isExportFailed by mutableStateOf(isExportFailed)
    var isExportSuccess by mutableStateOf(isExportSuccess)
    var latestScanTime by mutableLongStateOf(latestScanTime)
    var filterMsg by mutableStateOf(filterMsg)
    var exportMsg by mutableStateOf(exportMsg)
    var mode by mutableStateOf(mode)
    var subscription by mutableStateOf(subscription)
    var exportResult by mutableStateOf(exportResult)

    private var _installPackageList = MutableStateFlow(installList)
    private var _userPackageList = MutableStateFlow(userPackageList)
    val installPackageList: StateFlow<AppList> = _installPackageList
    val userPackageList: StateFlow<AppList> = _userPackageList

    suspend fun updateInstallPackageList(appInfos: List<AppInfo>): WeaveState {
        _installPackageList.emit(AppList(appInfos))
        return this
    }

    suspend fun updateUserPackageList(appInfos: List<AppInfo>): WeaveState {
        _userPackageList.emit(AppList(appInfos))
        return this
    }

    suspend fun updateSubscription(subscription: StableRuleSubscriptionWithRules): WeaveState {
        this.subscription = subscription
        return this
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListCard(
    installPackageSize: Int = 0,
    userPackageList: AppList,
    latestScanTime: Long = 0L,
    onScan: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp)
            .fillMaxWidth()
            .aspectRatio(2f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(12.dp)
            ) {
                AppGrid(userPackageList)
            }
            Column(modifier = Modifier.align(Alignment.Center)) {
                Surface(
                    shape = CircleShape,
                ) {
                    Text(
                        text = "共安装${installPackageSize}个应用",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 2.dp,
                            bottom = 2.dp
                        )
                    )
                }
                if (latestScanTime != 0L) {
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "扫描时间：${latestScanTime.calculateDuration()}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(
                                start = 12.dp,
                                end = 12.dp,
                                top = 2.dp,
                                bottom = 2.dp
                            )
                        )
                    }
                }
            }
            IconButton(onClick = {
                onScan()
            }, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_refresh),
                    contentDescription = "扫描"
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AppGrid(
    stableUserPackageList: AppList,
) {
    val pm = LocalContext.current.packageManager
    val lazyGridState = rememberSaveable(saver = LazyGridState.Saver) {
        LazyGridState(0, 0)
    }
    LaunchedEffect(stableUserPackageList.size) {
        var scrollValue = 0.5f
        do {
            when {
                lazyGridState.canScrollBackward.not() -> {
                    scrollValue = -scrollValue
                    lazyGridState.animateScrollBy(scrollValue)
                    delay(16)
                }

                lazyGridState.canScrollForward.not() -> {
                    // do nothing
                    scrollValue = -scrollValue
                    lazyGridState.animateScrollBy(scrollValue)
                    delay(16)
                }

                else -> {
                    lazyGridState.scrollBy(scrollValue)
                    delay(16)
                }
            }
        } while (stableUserPackageList.isNotEmpty())
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        state = lazyGridState,
        userScrollEnabled = false,
    ) {
        items(stableUserPackageList, key = { it.appName }) { appInfo ->
            AsyncImage(
                model = appInfo.applicationInfo.loadIcon(pm),
                contentDescription = appInfo.appName,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp)
                    .animateItemPlacement()
            )
        }
    }
}

@Composable
private fun WelcomeCard(
    modifier: Modifier = Modifier,
    onScan: (pm: PackageManager) -> Unit = {},
) {
    val pm = LocalContext.current.packageManager
    Card(
        modifier = Modifier
            .padding(start = 16.dp)
            .fillMaxSize(),
    ) {
        Column(modifier = modifier) {
            Text(
                text = "扫描应用列表，而后开始过滤",
                style = MaterialTheme.typography.headlineSmall,
            )
            Button(onClick = {
                onScan(pm)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "扫描"
                )
                Text("扫描")
            }
        }
    }
}