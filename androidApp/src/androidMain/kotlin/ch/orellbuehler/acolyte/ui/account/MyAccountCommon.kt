package ch.orellbuehler.acolyte.ui.account

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.SessionState
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun currentAccount(): Account? {
    val state by LocalSdk.current.session.state
        .collectAsStateWithLifecycle()
    return (state as? SessionState.SignedIn)?.account
}

@Composable
fun ToastOnce(
    text: String?,
    onShown: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(text) {
        if (text != null) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            onShown()
        }
    }
}

@Composable
fun FormColumn(
    padding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.Screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.Large),
    ) {
        content()
    }
}

@Composable
fun FormCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun LabeledRow(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        content()
    }
}

val PaddingValuesZero = PaddingValues(0.dp)

@Composable
fun ErrorBannerText(text: String) {
    ch.orellbuehler.acolyte.ui.components
        .NoticeBanner(text)
}
