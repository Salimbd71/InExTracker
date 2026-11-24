package com.SLM

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SLM.ads.AdManager
import com.SLM.ui.i18n.LocalAppLanguage
import com.SLM.ui.i18n.LocalAppStrings
import com.SLM.ui.i18n.stringsForLanguage
import com.SLM.ui.navigation.AppNavigation
import com.SLM.ui.theme.MyApplicationTheme
import com.SLM.ui.viewmodel.MainViewModel
import com.SLM.widget.InExTrackerWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val widgetActionFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdManager.initialize(applicationContext)
        AdManager.loadInterstitialAd(applicationContext)
        extractWidgetAction(intent)

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val language by viewModel.language.collectAsStateWithLifecycle()
            val widgetAction by widgetActionFlow.collectAsStateWithLifecycle()
            val strings = remember(language) { stringsForLanguage(language) }

            CompositionLocalProvider(
                LocalAppStrings provides strings,
                LocalAppLanguage provides language
            ) {
                MyApplicationTheme(darkTheme = isDarkMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(
                            viewModel = viewModel,
                            widgetAction = widgetAction,
                            onWidgetActionConsumed = { widgetActionFlow.value = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractWidgetAction(intent)
    }

    private fun extractWidgetAction(intent: Intent?) {
        val action = intent?.getStringExtra(InExTrackerWidgetProvider.EXTRA_WIDGET_ACTION)
        if (!action.isNullOrBlank()) {
            widgetActionFlow.value = action
            intent.removeExtra(InExTrackerWidgetProvider.EXTRA_WIDGET_ACTION)
        }
    }
}
