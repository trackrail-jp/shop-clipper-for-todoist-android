package jp.trackrail.shopclipper.ui.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jp.trackrail.shopclipper.MainActivity
import jp.trackrail.shopclipper.data.settingsRepository
import jp.trackrail.shopclipper.net.UrlConnectionTransport
import jp.trackrail.shopclipper.ui.theme.ShopClipperTheme
import kotlinx.coroutines.delay

/**
 * What the Android share sheet opens: a bottom sheet over the app the user
 * shared from (計画書 §4). It fills itself from the shared text, and 追加 posts
 * the task. After a successful add it closes itself (D9); after a failure it
 * stays open so 追加 can be pressed again.
 */
class ShareActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels {
        viewModelFactory {
            initializer { ShareViewModel(settingsRepository(applicationContext), UrlConnectionTransport()) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShopClipperTheme {
                val state by viewModel.state.collectAsState()
                LaunchedEffect(Unit) {
                    viewModel.load(
                        text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
                        subject = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString(),
                    )
                }
                LaunchedEffect(state.stage) {
                    if (state.stage == ShareStage.Added) {
                        delay(CLOSE_DELAY_MILLIS)
                        finish()
                    }
                }
                ShareSheet(
                    state = state,
                    actions = ShareActions(
                        onName = viewModel::onNameChange,
                        onPrice = viewModel::onPriceChange,
                        onMemo = viewModel::onMemoChange,
                        onLabels = viewModel::onLabelsChange,
                        onPriority = viewModel::onPriorityChange,
                        onProject = viewModel::onProjectSelected,
                        onSection = viewModel::onSectionSelected,
                        onAdd = viewModel::add,
                        onOpenTask = ::openUrl,
                        onOpenSettings = ::openSettings,
                        onClose = ::finish,
                    ),
                )
            }
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        finish()
    }

    private fun openSettings() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private companion object {
        /** 「追加しました」を読める程度に見せてから閉じる（D9）。 */
        const val CLOSE_DELAY_MILLIS = 3_000L
    }
}
