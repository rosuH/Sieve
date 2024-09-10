package me.rosuh.sieve

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rosuh.sieve.model.AppInfo
import me.rosuh.sieve.model.RuleMode
import me.rosuh.sieve.model.RuleRepo
import me.rosuh.sieve.model.AppList
import me.rosuh.sieve.model.database.RuleSubscriptionWithRules
import me.rosuh.sieve.model.database.StableRuleSubscriptionWithRules
import me.rosuh.sieve.ui.screen.SubscriptionManagerState
import me.rosuh.sieve.ui.screen.HomeState
import me.rosuh.sieve.utils.Logger
import me.rosuh.sieve.utils.catchIO
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val repo: RuleRepo
) : ViewModel() {
    
    companion object {
        private const val TAG = "MainViewModel"
    }

    sealed class UIAction {
        data object Init : UIAction()
        data class Scan(val pm: PackageManager) : UIAction()
        data class ChangeMode(val ruleMode: RuleMode) : UIAction()
        data class Filter(val packageList: List<AppInfo>, val mode: RuleMode) : UIAction()
        data class Export(val packageList: List<AppInfo>, val mode: RuleMode, val exportType: RuleRepo.ExportType) : UIAction()
        data object AddSubscription : UIAction()
        data class AddSubscriptionFinish(val name: String?, val url: String?) : UIAction()
        data class SubscriptionSwitch(
            val subscription: RuleSubscriptionWithRules,
            val checked: Boolean
        ) : UIAction()
        data class SubscriptionPullToRefresh(val ruleMode: RuleMode) : UIAction()
        data object ResetAddSubscriptionError : UIAction()
    }

    sealed class Event {
        data class ScanFailed(val msg: String = "Scan failed") : Event()
    }

    private val _eventChannel: Channel<Event> = Channel(BUFFERED)

    val homeState: HomeState = HomeState()

    val subscriptionManagerState = SubscriptionManagerState()

    fun processUIAction(action: UIAction) {
        Logger.i(TAG, "processUIAction ${hashCode()}: $action")
        viewModelScope.launch {
            when (action) {
                is UIAction.Init -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        repo.getAllActiveWithRuleFlow().collect {
                            val list =
                                it.filter { sub -> sub.ruleSubscription.ruleMode == homeState.mode }
                            updatehomeState {
                                updateSubscription(StableRuleSubscriptionWithRules(list))
                            }
                        }
                    }

                    viewModelScope.launch(Dispatchers.IO) {
                        repo.getAllActiveWithRuleFlow(RuleMode.Proxy).collect {
                            updateSubscriptionManagerState {
                                updateProxySubscriptionList(it)
                            }
                        }
                    }

                    viewModelScope.launch(Dispatchers.IO) {
                        repo.getAllActiveWithRuleFlow(RuleMode.ByPass).collect {
                            updateSubscriptionManagerState {
                                updateByPassSubscriptionList(it)
                            }
                        }
                    }
                }
                is UIAction.Scan -> {
                    updatehomeState {
                        isScanningPackage = true
                    }
                    withContext(Dispatchers.IO) {
                        action.pm.getInstalledApplications(PackageManager.GET_META_DATA).map {
                            async {
                                AppInfo.fromApplicationInfo(it, action.pm)
                            }
                        }.awaitAll().takeIf {
                            it.isNotEmpty()
                        }?.groupBy {
                            it.isUserApp
                        }?.let {
                            updatehomeState {
                                isScanningPackage = false
                                latestScanTime = System.currentTimeMillis()
                            }
                            homeState.updateInstallPackageList((it[true] ?: emptyList()) + (it[false] ?: emptyList()))
                            homeState.updateUserPackageList(AppList(it[true] ?: emptyList()))
                        } ?: run {
                            _eventChannel.send(Event.ScanFailed("请到设置中授予「获取应用列表」权限"))
                            updatehomeState {
                                isScanningPackage = false
                                latestScanTime = System.currentTimeMillis()
                            }
                            homeState.updateInstallPackageList(emptyList())
                            homeState.updateUserPackageList(AppList(emptyList()))
                        }
                    }
                }

                is UIAction.ChangeMode -> {
                    withContext(Dispatchers.IO) {
                        val list = repo.getAllActiveWithRule(action.ruleMode)
                        updatehomeState {
                            mode = action.ruleMode
                            subscription = StableRuleSubscriptionWithRules(list)
                        }
                    }
                }

                is UIAction.Filter -> {
                    updatehomeState {
                        isFiltering = true
                    }
                    repo.filter(action.packageList, action.mode)
                }

                is UIAction.Export -> {
                    updatehomeState {
                        isExporting = true
                        isExportFailed = false
                        isExportSuccess = false
                        exportMsg = "导出中..."
                        exportType = action.exportType
                    }
                    kotlin.runCatching {
                        repo.exportApplicationList(action.packageList, action.mode, action.exportType)
                    }.fold(
                        onSuccess = {
                            updatehomeState {
                                isExporting = false
                                isExportSuccess = true
                                isExportFailed = false
                                exportMsg = "导出成功"
                                exportResult = it
                            }
                        },
                        onFailure = { throwable ->
                            updatehomeState {
                                isExporting = false
                                isExportSuccess = false
                                isExportFailed = true
                                exportMsg = "导出失败: ${throwable.message}"
                            }
                        }
                    )
                }

                UIAction.AddSubscription -> {
                    updateSubscriptionManagerState {
                        isAddSubscription = true
                    }
                }

                is UIAction.AddSubscriptionFinish -> {
                    withContext(Dispatchers.Default) {
                        if (action.name.isNullOrBlank() && action.url.isNullOrBlank()) {
                            updateSubscriptionManagerState {
                                isAddSubscription = false
                            }
                            return@withContext
                        }
                        val url = kotlin.runCatching { action.url?.toHttpUrl() }.getOrNull()
                        if (url == null) {
                            updateSubscriptionManagerState {
                                addSubscriptionCheckFailed = true
                            }
                            return@withContext
                        }
                        updateSubscriptionManagerState {
                            isAddSubscription = false
                            addSubscriptionCheckFailed = false
                        }
                        // add subscription
                        catchIO {
                            repo.addSubscription(
                                name = action.name,
                                url = url
                            )
                        }.fold(
                            {
                                updateSubscriptionManagerState {
                                    isAddSubscriptionFailed = true
                                    addSubscriptionFailedTips = "添加失败: ${it.message}"
                                }
                            },
                            {
                            }
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
                            when (action.ruleMode) {
                                RuleMode.Proxy -> {
                                    isProxyModeRefreshing = true
                                }
                                else -> {
                                    isBypassModeRefreshing = true
                                }
                            }
                        }
                        repo.syncAllConf(action.ruleMode)
                        updateSubscriptionManagerState {
                            when (action.ruleMode) {
                                RuleMode.Proxy -> {
                                    isProxyModeRefreshing = false
                                }
                                else -> {
                                    isBypassModeRefreshing = false
                                }
                            }
                        }
                    }
                }

                UIAction.ResetAddSubscriptionError -> {
                    updateSubscriptionManagerState {
                        addSubscriptionCheckFailed = false
                        isAddSubscriptionFailed = false
                    }
                }
            }
        }
    }

    private suspend fun updatehomeState(reduce: suspend HomeState.() -> Unit) {
        withContext(Dispatchers.Main.immediate) {
            homeState.reduce()
            homeState.isInit = false
        }
    }

    private suspend fun updateSubscriptionManagerState(reduce: SubscriptionManagerState.() -> Unit) {
        withContext(Dispatchers.Main.immediate) {
            subscriptionManagerState.reduce()
        }
    }
}