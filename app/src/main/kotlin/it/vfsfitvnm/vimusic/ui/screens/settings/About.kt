package it.vfsfitvnm.vimusic.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import it.vfsfitvnm.vimusic.BuildConfig
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.preferences.DataPreferences
import it.vfsfitvnm.vimusic.service.ServiceNotifications
import it.vfsfitvnm.vimusic.ui.components.themed.CircularProgressIndicator
import it.vfsfitvnm.vimusic.ui.components.themed.DefaultDialog
import it.vfsfitvnm.core.ui.*

import it.vfsfitvnm.vimusic.ui.components.themed.SecondaryTextButton
import it.vfsfitvnm.vimusic.ui.screens.Route
import it.vfsfitvnm.vimusic.utils.bold
import it.vfsfitvnm.vimusic.utils.center
import it.vfsfitvnm.vimusic.utils.hasPermission
import it.vfsfitvnm.vimusic.utils.pendingIntent
import it.vfsfitvnm.vimusic.utils.semiBold
import it.vfsfitvnm.vimusic.utils.medium
import it.vfsfitvnm.vimusic.utils.secondary
import it.vfsfitvnm.core.data.utils.Version
import it.vfsfitvnm.core.data.utils.version
import it.vfsfitvnm.core.ui.LocalAppearance
import it.vfsfitvnm.core.ui.utils.isAtLeastAndroid13
import it.vfsfitvnm.core.ui.utils.isCompositionLaunched
import it.vfsfitvnm.providers.github.GitHub
import it.vfsfitvnm.providers.github.models.Release
import it.vfsfitvnm.providers.github.requests.releases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.toJavaDuration

private val VERSION_NAME = BuildConfig.VERSION_NAME.substringBeforeLast("-")
private const val REPO_OWNER = "Jigen-Ohtsusuki"
private const val REPO_NAME = "ViMusic"

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private val permission = Manifest.permission.POST_NOTIFICATIONS

class VersionCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {
        private const val WORK_TAG = "version_check_worker"

        fun upsert(context: Context, period: Duration?) = runCatching {
            val workManager = WorkManager.getInstance(context)

            if (period == null) {
                workManager.cancelAllWorkByTag(WORK_TAG)
                return@runCatching
            }

            val request = PeriodicWorkRequestBuilder<VersionCheckWorker>(period.toJavaDuration())
                .addTag(WORK_TAG)
                .setConstraints(
                    Constraints(
                        requiredNetworkType = NetworkType.CONNECTED,
                        requiresBatteryNotLow = true
                    )
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                /* uniqueWorkName = */ WORK_TAG,
                /* existingPeriodicWorkPolicy = */ ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                /* periodicWork = */ request
            )

            Unit
        }.also { it.exceptionOrNull()?.printStackTrace() }
    }

    override suspend fun doWork(): Result = with(applicationContext) {
        if (isAtLeastAndroid13 && !hasPermission(permission)) return Result.retry()

        val result = withContext(Dispatchers.IO) {
            VERSION_NAME.version
                .getNewerVersion()
                .also { it?.exceptionOrNull()?.printStackTrace() }
        }

        result?.getOrNull()?.let { release ->
            ServiceNotifications.version.sendNotification(applicationContext) {
                this
                    .setSmallIcon(R.drawable.download)
                    .setContentTitle(getString(R.string.new_version_available))
                    .setContentText(getString(R.string.redirect_github))
                    .setContentIntent(
                        pendingIntent(
                            Intent(
                                /* action = */ Intent.ACTION_VIEW,
                                /* uri = */ Uri.parse(release.frontendUrl.toString())
                            )
                        )
                    )
                    .setAutoCancel(true)
                    .also {
                        it.setStyle(
                            NotificationCompat
                                .BigTextStyle(it)
                                .bigText(getString(R.string.new_version_available))
                        )
                    }
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
            }
        }

        return when {
            result == null || result.isFailure -> Result.retry()
            result.isSuccess -> Result.success()
            else -> Result.failure() // Unreachable
        }
    }
}

private suspend fun Version.getNewerVersion(
    repoOwner: String = REPO_OWNER,
    repoName: String = REPO_NAME,
    contentType: String = "application/vnd.android.package-archive"
) = GitHub.releases(
    owner = repoOwner,
    repo = repoName
)?.mapCatching { releases ->
    releases
        .sortedByDescending { it.publishedAt }
        .firstOrNull { release ->
            !release.draft &&
                !release.preRelease &&
                release.tag.version > this &&
                release.assets.any {
                    it.contentType == contentType && it.state == Release.Asset.State.Uploaded
                }
        }
}

@Route
@Composable
fun About() = SettingsCategoryScreen(
    title = stringResource(R.string.about),
    description = stringResource(
        R.string.format_version_credits,
        VERSION_NAME
    ) + "\nModified by KOU"
) {
    val (colorPalette, typography) = LocalAppearance.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    var hasPermission by remember(isCompositionLaunched()) {
        mutableStateOf(
            if (isAtLeastAndroid13) context.applicationContext.hasPermission(permission)
            else true
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { hasPermission = it }
    )

    SettingsGroup(title = stringResource(R.string.social)) {
        androidx.compose.material3.ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                containerColor = colorPalette.surface,
                contentColor = colorPalette.text
            ),
            onClick = {
                uriHandler.openUri("https://github.com/$REPO_OWNER/$REPO_NAME")
            }
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(16.dp)
            ) {
                BasicText(
                    text = stringResource(R.string.github),
                    style = typography.s.semiBold
                )
                BasicText(
                    text = stringResource(R.string.view_source),
                    style = typography.xs.secondary
                )
            }
        }
    }

    SettingsGroup(title = stringResource(R.string.contact)) {
        androidx.compose.material3.ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                containerColor = colorPalette.surface,
                contentColor = colorPalette.text
            ),
            onClick = {
                uriHandler.openUri(
                    @Suppress("MaximumLineLength")
                    "https://github.com/$REPO_OWNER/$REPO_NAME/issues/new?assignees=&labels=bug&template=bug_report.yaml"
                )
            }
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(16.dp)
            ) {
                BasicText(
                    text = stringResource(R.string.report_bug),
                    style = typography.s.semiBold
                )
                BasicText(
                    text = stringResource(R.string.report_bug_description),
                    style = typography.xs.secondary
                )
            }
        }
        
        androidx.compose.material3.ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                containerColor = colorPalette.surface,
                contentColor = colorPalette.text
            ),
            onClick = {
                uriHandler.openUri(
                    @Suppress("MaximumLineLength")
                    "https://github.com/$REPO_OWNER/$REPO_NAME/issues/new?assignees=&labels=enhancement&template=feature_request.md"
                )
            }
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(16.dp)
            ) {
                BasicText(
                    text = stringResource(R.string.request_feature),
                    style = typography.s.semiBold
                )
                BasicText(
                    text = stringResource(R.string.redirect_github),
                    style = typography.xs.secondary
                )
            }
        }
    }

    var newVersionDialogOpened by rememberSaveable { mutableStateOf(false) }

    SettingsGroup(title = stringResource(R.string.version)) {
        androidx.compose.material3.ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                containerColor = colorPalette.surface,
                contentColor = colorPalette.text
            ),
            onClick = { newVersionDialogOpened = true }
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(16.dp)
            ) {
                BasicText(
                    text = stringResource(R.string.check_new_version),
                    style = typography.s.semiBold
                )
                BasicText(
                    text = stringResource(R.string.current_version, VERSION_NAME),
                    style = typography.xs.secondary
                )
            }
        }

        // Modernized enum value selector
        androidx.compose.material3.ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                containerColor = colorPalette.surface,
                contentColor = colorPalette.text
            )
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(16.dp)
            ) {
                BasicText(
                    text = stringResource(R.string.version_check),
                    style = typography.s.semiBold
                )
                
                Spacer(Modifier.height(8.dp))
                
                androidx.compose.material3.DropdownMenu(
                    expanded = false,
                    onDismissRequest = { },
                    modifier = Modifier.fillMaxWidth()
                ) { }
                
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        // Open dropdown for selection
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = colorPalette.accent
                    )
                ) {
                    BasicText(
                        text = DataPreferences.versionCheckPeriod.displayName(),
                        style = typography.xs.medium.copy(color = colorPalette.accent)
                    )
                }
            }
        }
        
        // Add the actual enum functionality
        EnumValueSelectorSettingsEntry(
            title = "", // Empty title since we've already created the card UI above
            selectedValue = DataPreferences.versionCheckPeriod,
            onValueSelect = onSelect@{
                DataPreferences.versionCheckPeriod = it
                if (isAtLeastAndroid13 && it.period != null && !hasPermission)
                    launcher.launch(permission)

                VersionCheckWorker.upsert(context.applicationContext, it.period)
            },
            valueText = { it.displayName() },
            modifier = Modifier.height(0.dp) // Hide this component as we're using our custom UI above
        )
    }

    if (newVersionDialogOpened) {
        var newerVersion: Result<Release?>? by remember { mutableStateOf(null) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                newerVersion = VERSION_NAME.version
                    .getNewerVersion()
                    ?.onFailure(Throwable::printStackTrace)
            }
        }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { newVersionDialogOpened = false },
            title = {
                BasicText(
                    text = stringResource(R.string.check_new_version),
                    style = typography.s.semiBold.center
                )
            },
            text = {

                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    newerVersion?.getOrNull()?.let {
                        BasicText(
                            text = stringResource(R.string.new_version_available),
                            style = typography.xs.semiBold.center
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        BasicText(
                            text = it.name ?: it.tag,
                            style = typography.m.bold.center
                        )
                    } ?: newerVersion?.exceptionOrNull()?.let {
                        BasicText(
                            text = stringResource(R.string.error_github),
                            style = typography.xs.semiBold.center,
                            modifier = Modifier.padding(all = 24.dp)
                        )
                    } ?: if (newerVersion?.isSuccess == true) {
                        BasicText(
                            text = stringResource(R.string.up_to_date),
                            style = typography.xs.semiBold.center
                        )
                    } else {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            },
            confirmButton = {
                newerVersion?.getOrNull()?.let {
                    androidx.compose.material3.TextButton(
                        onClick = { uriHandler.openUri(it.frontendUrl.toString()) }
                    ) {
                        BasicText(
                            text = stringResource(R.string.more_information),
                            style = typography.xs.medium.copy(color = colorPalette.accent)
                        )
                    }
                } ?: newerVersion?.exceptionOrNull()?.let {
                    androidx.compose.material3.TextButton(
                        onClick = { newVersionDialogOpened = false }
                    ) {
                        BasicText(
                            text = stringResource(android.R.string.ok),
                            style = typography.xs.medium.copy(color = colorPalette.accent)
                        )
                    }
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { newVersionDialogOpened = false }
                ) {
                    BasicText(
                        text = stringResource(android.R.string.cancel),
                        style = typography.xs.medium.copy(color = colorPalette.accent)
                    )
                }
            },
            containerColor = colorPalette.background0,
            titleContentColor = colorPalette.text,
            textContentColor = colorPalette.textSecondary
        )
    }
}
