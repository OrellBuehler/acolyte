package ch.orellbuehler.acolyte.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ch.orellbuehler.acolyte.tv.nav.TvApp
import ch.orellbuehler.acolyte.ui.SessionViewModel

class TvActivity : ComponentActivity() {
    private val sdk get() = (application as TvApplication).sdk

    private val sessionViewModel: SessionViewModel by viewModels { viewModelFactory { initializer { SessionViewModel(sdk) } } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TvApp(sdk, sessionViewModel) }
    }

    override fun onResume() {
        super.onResume()
        sessionViewModel.refresh()
    }
}
