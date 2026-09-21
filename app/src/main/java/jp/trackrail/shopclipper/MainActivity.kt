package jp.trackrail.shopclipper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jp.trackrail.shopclipper.data.settingsRepository
import jp.trackrail.shopclipper.net.UrlConnectionTransport
import jp.trackrail.shopclipper.ui.settings.SettingsActions
import jp.trackrail.shopclipper.ui.settings.SettingsScreen
import jp.trackrail.shopclipper.ui.settings.SettingsViewModel
import jp.trackrail.shopclipper.ui.theme.ShopClipperTheme

// The launcher icon opens the settings (計画書 §4). The share sheet opens
// ui/share/ShareActivity instead.
class MainActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels {
        viewModelFactory {
            initializer { SettingsViewModel(settingsRepository(applicationContext), UrlConnectionTransport()) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShopClipperTheme {
                val state by viewModel.state.collectAsState()
                LaunchedEffect(Unit) { viewModel.load() }
                SettingsScreen(
                    state = state,
                    actions = SettingsActions(
                        onToken = viewModel::onTokenChange,
                        onTest = viewModel::testConnection,
                        onProject = viewModel::onProjectSelected,
                        onSection = viewModel::onSectionSelected,
                        onSiteLabels = viewModel::onSiteLabelsChange,
                        onLabels = viewModel::onLabelsChange,
                        onPriority = viewModel::onPriorityChange,
                        onTitleLimit = viewModel::onTitleLimitChange,
                        onSave = viewModel::save,
                    ),
                )
            }
        }
    }
}
