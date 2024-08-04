package me.rosuh.sieve.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import me.rosuh.sieve.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen() {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(id = R.string.tab_setting))
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            val ctx = LocalContext.current
            Text(
                stringResource(id = R.string.tab_setting_title_about),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 24.dp)
            )
            Spacer(
                modifier = Modifier
                    .height(8.dp)
            )
            Card(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 0.dp)
                    .shadow(0.dp)
            ) {
                AboutItem(
                    title = stringResource(id = R.string.tab_setting_title_version),
                    icon = R.drawable.ic_release,
                    value = versionName
                )
                AboutItem(
                    title = stringResource(id = R.string.tab_setting_title_repo),
                    icon = R.drawable.ic_github,
                    onClick = {
                        openUrlInBrowser(
                            ctx,
                            "https://github.com/rosuH/Sieve"
                        )
                    })
                AboutItem(
                    title = "Privacy Policy",
                    icon = R.drawable.ic_privacy,
                    onClick = {
                        openUrlInBrowser(
                            ctx,
                            "https://raw.githubusercontent.com/rosuH/Sieve/master/PRIVACY.md"
                        )
                    })
                AboutItem(
                    title = "隐私条款",
                    icon = R.drawable.ic_privacy,
                    onClick = {
                        openUrlInBrowser(
                            ctx,
                            "https://raw.githubusercontent.com/rosuH/Sieve/master/PRIVACY_CN.md"
                        )
                    })
            }

            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )
            Text(
                stringResource(id = R.string.tab_setting_title_author),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 24.dp)
            )
            Spacer(
                modifier = Modifier
                    .height(8.dp)
            )

            Card(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .shadow(0.dp)
            ) {
                AboutItem(
                    title = "rosuH",
                    icon = R.drawable.ic_author,
                    rawImage = true,
                    onClick = {
                        openUrlInBrowser(
                            ctx,
                            "https://github.com/rosuH"
                        )
                    })
            }
        }
    }
}

private fun openUrlInBrowser(ctx: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    runCatching { ctx.startActivity(intent) }
}

@Composable
fun AboutItem(
    title: String,
    icon: Int,
    rawImage: Boolean = false,
    value: String? = null,
    onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentPadding = PaddingValues(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        if (rawImage) {
            AsyncImage(model = icon, contentDescription = title, modifier = Modifier.size(28.dp))
        } else {
            Icon(
                painter = painterResource(id = icon),
                modifier = Modifier.size(24.dp),
                contentDescription = title
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.weight(1f))
        if (value == null) {
            Icon(painterResource(id = R.drawable.ic_right), null)
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}