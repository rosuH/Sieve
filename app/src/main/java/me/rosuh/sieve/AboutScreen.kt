package me.rosuh.sieve

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("关于")
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            val ctx = LocalContext.current
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            AboutItem(title = "Version", value = versionName)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            AboutItem(title = "Privacy Policy", onClick = { openUrlInBrowser(ctx, "https://raw.githubusercontent.com/rosuH/Sieve/master/PRIVACY.md") })
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            AboutItem(title = "隐私条款", onClick = { openUrlInBrowser(ctx, "https://raw.githubusercontent.com/rosuH/Sieve/master/PRIVACY_CN.md") })
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Build with ❤️ by rosuH",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

private fun openUrlInBrowser(ctx: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    runCatching { ctx.startActivity(intent) }
}

@Composable
fun AboutItem(title: String, value: String? = null, onClick: () -> Unit = {}) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentPadding = PaddingValues(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            value?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (value == null) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
        }
    }
}