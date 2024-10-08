package me.rosuh.sieve.ui.screen

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.rosuh.sieve.MainViewModel
import me.rosuh.sieve.R
import me.rosuh.sieve.model.AppInfo
import me.rosuh.sieve.model.AppList
import me.rosuh.sieve.model.RuleMode
import me.rosuh.sieve.model.RuleRepo
import me.rosuh.sieve.model.database.StableRuleSubscriptionWithRules
import me.rosuh.sieve.utils.Logger
import me.rosuh.sieve.utils.calculateDurationComposable


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onScan: () -> Unit = {},
    onChangeMode: (mode: RuleMode) -> Unit = {},
    onFilter: (List<AppInfo>, mode: RuleMode) -> Unit = { _, _ -> },
    onExport: (List<AppInfo>, mode: RuleMode, exportType: RuleRepo.ExportType) -> Unit = { _, _, _ -> },
) {
    val homeState = remember {
        viewModel.homeState
    }
    val installPackageList by homeState.installPackageList.collectAsStateWithLifecycle()
    val userPackageList by homeState.userPackageList.collectAsStateWithLifecycle()
    val isInit = homeState.isInit
    val onScanRemember by rememberUpdatedState(onScan)
    LaunchedEffect(isInit) {
        if (isInit) {
            viewModel.processUIAction(MainViewModel.UIAction.Init)
            onScan()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(id = R.string.tab_home))
                }
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val rowModifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)
            val titleModifier = Modifier
            val buttonModifier = Modifier.padding(start = 16.dp, top = 12.dp)
            Column(modifier = Modifier) {
                AppListCard(
                    installPackageSize = installPackageList.size,
                    userPackageList,
                    latestScanTime = homeState.latestScanTime,
                    onScan = onScanRemember,
                )
                Column(modifier = rowModifier) {
                    val paddingStart = 16.dp
                    Text(
                        stringResource(id = R.string.tab_home_title_rule),
                        modifier = titleModifier,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(id = R.string.tab_home_title_rule_desc),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = paddingStart, top = 8.dp)
                    )
                    Row(Modifier.padding(start = paddingStart, top = 12.dp)) {
                        FilterChip(
                            modifier = Modifier.animateContentSize(),
                            onClick = { onChangeMode(RuleMode.ByPass) },
                            label = {
                                Text(stringResource(id = R.string.tab_home_title_rule_bypass))
                            },
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = homeState.mode == RuleMode.ByPass
                            ),
                            selected = homeState.mode == RuleMode.ByPass,
                            leadingIcon = {
                                if (homeState.mode == RuleMode.ByPass) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
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
                                Text(stringResource(id = R.string.tab_home_title_rule_proxy))
                            },
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                homeState.mode == RuleMode.Proxy
                            ),
                            selected = homeState.mode == RuleMode.Proxy,
                            leadingIcon = if (homeState.mode == RuleMode.Proxy) {
                                {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
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
                            text = stringResource(id = R.string.tab_home_title_rule_enable_subscription)
                        )
                        Spacer(modifier = Modifier.size(2.dp))
                        AnimatedContent(
                            targetState = homeState.subscription.size,
                            label = "subscriptionSize"
                        ) { targetState ->
                            Text(
                                text = targetState.toString()
                            )
                        }
                    }
                    Row(modifier = Modifier.padding(start = paddingStart, top = 8.dp)) {
                        Text(
                            text = stringResource(id = R.string.tab_home_title_rule_total)
                        )
                        Spacer(modifier = Modifier.size(2.dp))
                        AnimatedContent(
                            targetState = homeState.subscription.sumOf { it.ruleList.size },
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
                        stringResource(id = R.string.tab_home_title_export),
                        modifier = titleModifier,
                        style = MaterialTheme.typography.titleLarge
                    )
                    val exportEnable = homeState.isExporting.not()
                    val iconModifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                    Row {
                        IconButton(
                            modifier = buttonModifier.size(48.dp),
                            enabled = exportEnable,
                            onClick = {
                                onExport(
                                    installPackageList,
                                    homeState.mode,
                                    RuleRepo.ExportType.FlClash
                                )
                            }) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_flclash),
                                contentDescription = "FlClash",
                                modifier = iconModifier
                            )
                        }
                        IconButton(
                            modifier = buttonModifier.size(48.dp),
                            enabled = exportEnable,
                            onClick = {
                                onExport(
                                    installPackageList,
                                    homeState.mode,
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
                                    homeState.mode,
                                    RuleRepo.ExportType.NekoBoxAndroid
                                )
                            }) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_nbfa),
                                contentDescription = "NekoBoxForAndroid",
                                modifier = iconModifier
                            )
                        }
                        IconButton(
                            modifier = buttonModifier.size(48.dp),
                            enabled = exportEnable,
                            onClick = {
                                onExport(
                                    installPackageList,
                                    homeState.mode,
                                    RuleRepo.ExportType.ClashMetaForAndroid
                                )
                            }) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_cmfa),
                                contentDescription = "ClashMetaForAndroid",
                                modifier = iconModifier
                            )
                        }
                    }
                    if (homeState.isExporting || homeState.isExportFailed || homeState.isExportSuccess) {
                        val onDismiss = { viewModel.processUIAction(MainViewModel.UIAction.DismissExportDialog) }
                        val onJump = { viewModel.processUIAction(MainViewModel.UIAction.PasteExportResult) }
                        ExportDialog(
                            isExporting = homeState.isExporting,
                            isExportFailed = homeState.isExportFailed,
                            isExportSuccess = homeState.isExportSuccess,
                            exportType = homeState.exportType,
                            exportMsg = homeState.exportMsg,
                            exportResult = homeState.exportResult,
                            onDismiss = onDismiss,
                            onJump = onJump
                        )
                    }
                }
            }
        }
    }

}

@Composable
fun ExportDialog(
    isExporting: Boolean,
    isExportFailed: Boolean,
    isExportSuccess: Boolean,
    exportType: RuleRepo.ExportType,
    exportMsg: String,
    exportResult: String,
    onDismiss: () -> Unit,
    onJump: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出") },
        text = {
            Column {
                Text(
                    text = when {
                        isExporting -> stringResource(id = R.string.tab_home_title_exporting)
                        isExportFailed -> "${stringResource(id = R.string.tab_home_title_export_failed)}, $exportMsg"
                        isExportSuccess -> "${stringResource(id = R.string.tab_home_title_export_success)}, $exportMsg"
                        else -> ""
                    }
                )
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .alpha(if (isExporting) 1f else 0f)
                )
            }
        },
        confirmButton = {
            AnimatedVisibility(
                visible = isExportSuccess,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(exportResult))
                    exportType.jump(context)
                    onJump()
                }) {
                    Text("去粘贴")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}


@Stable
class HomeState(
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
    exportType: RuleRepo.ExportType = RuleRepo.ExportType.ClashMetaForAndroid
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
    var exportType by mutableStateOf(exportType)

    private var _installPackageList = MutableStateFlow(installList)
    private var _userPackageList = MutableStateFlow(userPackageList)
    val installPackageList: StateFlow<AppList> = _installPackageList
    val userPackageList: StateFlow<AppList> = _userPackageList

    suspend fun updateInstallPackageList(appInfos: ImmutableList<AppInfo>): HomeState {
        _installPackageList.emit(AppList(appInfos))
        return this
    }

    suspend fun updateUserPackageList(appInfos: ImmutableList<AppInfo>): HomeState {
        _userPackageList.emit(AppList(appInfos))
        return this
    }

    suspend fun updateSubscription(subscription: StableRuleSubscriptionWithRules): HomeState {
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
                        text = stringResource(
                            id = R.string.tab_home_title_install_size,
                            installPackageSize
                        ),
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
                            text = "${stringResource(id = R.string.tab_home_title_scan_time)}：${latestScanTime.calculateDurationComposable()}",
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
                    contentDescription = "扫描",
                    modifier = Modifier.size(28.dp)
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
    val resource = LocalContext.current.resources
    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        (LocalContext.current.display)
    } else {
        (LocalContext.current.getSystemService(Context.WINDOW_SERVICE) as WindowManager?)?.defaultDisplay
    }
    val lazyGridState = rememberSaveable(saver = LazyGridState.Saver) {
        LazyGridState(0, 0)
    }
    if (stableUserPackageList.size > 12) {
        LaunchedEffect(stableUserPackageList.size) {
            // get screen refresh rate
            val screenRefreshRate = display?.refreshRate ?: 60f
            val interval = (1000 / screenRefreshRate).toLong()
            var scrollValue = 0.5f
            do {
                when {
                    lazyGridState.canScrollBackward.not() -> {
                        scrollValue = -scrollValue
                        lazyGridState.animateScrollBy(scrollValue)
                    }

                    lazyGridState.canScrollForward.not() -> {
                        // do nothing
                        scrollValue = -scrollValue
                        lazyGridState.animateScrollBy(scrollValue)
                    }

                    else -> {
                        lazyGridState.scrollBy(scrollValue)
                    }
                }
                delay(interval)
            } while (stableUserPackageList.isNotEmpty())
        }
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
        items(stableUserPackageList, key = { it.packageName }) { appInfo ->
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