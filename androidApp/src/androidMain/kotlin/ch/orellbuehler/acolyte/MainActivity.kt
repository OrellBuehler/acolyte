package ch.orellbuehler.acolyte

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ch.orellbuehler.acolyte.ui.AcolyteApp
import ch.orellbuehler.acolyte.ui.SessionViewModel

class MainActivity : ComponentActivity() {
    private val sdk get() = (application as AcolyteApplication).sdk

    private val sessionViewModel: SessionViewModel by viewModels { factory { SessionViewModel(sdk) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AcolyteApp(sdk, sessionViewModel) }
    }

    override fun onResume() {
        super.onResume()
        sessionViewModel.refresh()
    }

    private inline fun <reified VM : ViewModel> factory(crossinline create: () -> VM) =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
        }
}
