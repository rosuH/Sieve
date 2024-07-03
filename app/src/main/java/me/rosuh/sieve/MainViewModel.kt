package me.rosuh.sieve

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rosuh.sieve.model.ExportConf
import me.rosuh.sieve.model.RuleMode
import me.rosuh.sieve.model.RuleRepo
import me.rosuh.sieve.model.database.RuleSubscriptionWithRules
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class MainViewModel @Inject constructor(
    val repo: RuleRepo
) : ViewModel() {
    
    companion object {
        private const val TAG = "MainViewModel"
    }

    sealed class UIAction {
        data class Scan(val pm: PackageManager) : UIAction()
        data class ChangeMode(val ruleMode: RuleMode) : UIAction()
        data class Filter(val packageList: List<ApplicationInfo>, val mode: RuleMode) : UIAction()
        data class Export(val packageList: List<ApplicationInfo>, val mode: RuleMode, val exportType: RuleRepo.ExportType) : UIAction()
        data object AddSubscription : UIAction()
        data class AddSubscriptionFinish(val name: String?, val url: String?) : UIAction()
        data class SubscriptionSwitch(
            val subscription: RuleSubscriptionWithRules,
            val checked: Boolean
        ) : UIAction()
        data class SubscriptionPullToRefresh(val ruleMode: RuleMode) : UIAction()
    }

    data class WeaveState(
        val isInit: Boolean = false,
        val isScanningPackage: Boolean = false,
        val isFiltering: Boolean = false,
        val isFilterFailed: Boolean = false,
        val isExporting: Boolean = false,
        val isExportFailed: Boolean = false,
        val isExportSuccess: Boolean = false,
        val installPackageList: List<ApplicationInfo> = emptyList(),
        val latestScanTime: Long = 0L,
        val filterMsg: String = "规则应用中...",
        val exportMsg: String = "规则导出中...",
        val mode: RuleMode = RuleMode.ByPass,
        val subscription: List<RuleSubscriptionWithRules> = emptyList(),
        val exportResult: String = "",
    )

    data class SubscriptionManagerState(
        val isRefreshing: Boolean = false,
        val isLoadingBypass: Boolean = false,
        val isLoadingProxy: Boolean = false,
        val isAddSubscription: Boolean = false,
        val addSubscriptionCheckFailed: Boolean = false,
        val byPassSubscriptionList: List<RuleSubscriptionWithRules> = emptyList(),
        val proxySubscriptionList: List<RuleSubscriptionWithRules> = emptyList(),
    )

    sealed class Event {
        data class ScanFailed(val msg: String = "Scan failed") : Event()
    }

    private val _weaveState = MutableStateFlow<WeaveState>(WeaveState(isInit = true))
    private val _eventChannel: Channel<Event> = Channel(BUFFERED)
    val weaveState: StateFlow<WeaveState> = _weaveState

    private val _subscriptionManagerState = MutableStateFlow<SubscriptionManagerState>(SubscriptionManagerState())
    val subscriptionManagerState: StateFlow<SubscriptionManagerState> = _subscriptionManagerState

    val eventFlow: Flow<Event> = _eventChannel.receiveAsFlow().flowOn(Dispatchers.Main)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repo.getAllActiveWithRuleFlow().collect {
                val list =
                    it.filter { sub -> sub.ruleSubscription.ruleMode == weaveState.value.mode }
                updateWeaveState {
                    copy(subscription = list)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            repo.getAllActiveWithRuleFlow(RuleMode.Proxy).collect {
                updateSubscriptionManagerState {
                    copy(proxySubscriptionList = it)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            repo.getAllActiveWithRuleFlow(RuleMode.ByPass).collect {
                updateSubscriptionManagerState {
                    copy(byPassSubscriptionList = it)
                }
            }
        }
    }

    fun processUIAction(action: UIAction) {
        viewModelScope.launch {
            when (action) {
                is UIAction.Scan -> {
                    updateWeaveState {
                        copy(isScanningPackage = true)
                    }
                    withContext(Dispatchers.IO) {
                        val packages = action.pm.getInstalledApplications(PackageManager.GET_META_DATA)
                        if (packages.size <= 1) {
                            _eventChannel.send(Event.ScanFailed("请到设置中授予「获取应用列表」权限"))
                        } else {
                            updateWeaveState {
                                copy(
                                    isScanningPackage = false,
                                    installPackageList = packages,
                                    latestScanTime = System.currentTimeMillis()
                                )
                            }
                        }
                    }
                }

                is UIAction.ChangeMode -> {
                    withContext(Dispatchers.IO) {
                        val list = repo.getAllActiveWithRule(action.ruleMode)
                        updateWeaveState {
                            copy(mode = action.ruleMode, subscription = list)
                        }
                    }
                    updateWeaveState {
                        copy(mode = action.ruleMode)
                    }
                }

                is UIAction.Filter -> {
                    updateWeaveState {
                        copy(isFiltering = true)
                    }
                    repo.filter(action.packageList, action.mode)
                }

                is UIAction.Export -> {
                    updateWeaveState {
                        copy(isExporting = true, isExportFailed = false, isExportSuccess = false, exportMsg = "导出中...")
                    }
                    kotlin.runCatching {
                        repo.exportApplicationList(action.packageList, action.mode, action.exportType)
                    }.fold(
                        onSuccess = {
                            updateWeaveState {
                                copy(
                                    isExporting = false,
                                    isExportSuccess = true,
                                    isExportFailed = false,
                                    exportMsg = "导出成功",
                                    exportResult = it
                                )
                            }
                        },
                        onFailure = { throwable ->
                            updateWeaveState {
                                copy(
                                    isExporting = false,
                                    isExportSuccess = false,
                                    isExportFailed = true,
                                    exportMsg = "导出失败: ${throwable.message}"
                                )
                            }
                        }
                    )
                }

                UIAction.AddSubscription -> {
                    updateSubscriptionManagerState {
                        copy(isAddSubscription = true)
                    }
                }

                is UIAction.AddSubscriptionFinish -> {
                    withContext(Dispatchers.Default) {
                        if (action.name.isNullOrBlank() && action.url.isNullOrBlank()) {
                            updateSubscriptionManagerState {
                                copy(isAddSubscription = false)
                            }
                            return@withContext
                        }
                        val url = kotlin.runCatching { action.url?.toHttpUrl() }.getOrNull()
                        if (url == null) {
                            updateSubscriptionManagerState {
                                copy(addSubscriptionCheckFailed = true)
                            }
                            return@withContext
                        }
                        updateSubscriptionManagerState {
                            copy(isAddSubscription = false, addSubscriptionCheckFailed = false)
                        }
                        // add subscription
                        repo.addSubscription(
                            name = action.name,
                            url = url
                        )
                    }
                }

                is UIAction.SubscriptionSwitch -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        repo.switchSubscription(action.subscription, action.checked)
                    }
                }

                is UIAction.SubscriptionPullToRefresh -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        updateSubscriptionManagerState {
                            copy(isRefreshing = true)
                        }
                        repo.syncAllConf(action.ruleMode)
                        updateSubscriptionManagerState {
                            copy(isRefreshing = false)
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateWeaveState(reduce: WeaveState.() -> WeaveState) {
        withContext(Dispatchers.Main.immediate) {
            _weaveState.value = _weaveState.value.reduce().copy(isInit = false)
        }
    }

    private suspend fun updateSubscriptionManagerState(reduce: SubscriptionManagerState.() -> SubscriptionManagerState) {
        withContext(Dispatchers.Main.immediate) {
            _subscriptionManagerState.value = _subscriptionManagerState.value.reduce()
        }
    }
}