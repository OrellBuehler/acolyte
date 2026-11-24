package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceCheckLine
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceCheckResult
import ch.orellbuehler.acolyte.feature.sources.SettingsSourceCheckWord
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.requests.FailText
import ch.orellbuehler.acolyte.ui.requests.Hint
import ch.orellbuehler.acolyte.ui.requests.PickerField
import ch.orellbuehler.acolyte.ui.requests.SettingsCard
import ch.orellbuehler.acolyte.ui.theme.Spacing

@Composable
fun SourceCheckCard(model: SourceCheckViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    var kind by remember { mutableStateOf("movie") }
    var title by remember { mutableStateOf(model.sample("movie")) }
    SettingsCard(
        title = stringResource(R.string.acq_src_check_title),
        subtitle = stringResource(R.string.acq_src_check_description),
    ) {
        PickerField(
            stringResource(R.string.acq_src_check_kind),
            listOf(
                "movie" to stringResource(R.string.acq_src_check_movie),
                "show" to stringResource(R.string.acq_src_check_show),
                "music" to stringResource(R.string.acq_src_check_music),
            ),
            kind,
            {
                title = model.titleAfterKind(kind, title, it)
                kind = it
            },
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(200) },
            label = { Text(stringResource(R.string.acq_src_check_title_field)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            Button(onClick = { model.run(kind, title) }, enabled = !state.running) {
                if (state.running) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.acq_src_check_asking), modifier = Modifier.padding(start = Spacing.Small))
                } else {
                    Text(stringResource(R.string.acq_src_check_run))
                }
            }
        }
        state.error?.let { FailText(sourceErrorText(it)) }
        state.result?.let { CheckResult(it) }
    }
}

@Composable
private fun CheckResult(result: SettingsSourceCheckResult) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        if (result.total == 0) {
            Hint(stringResource(R.string.acq_src_check_none))
        } else {
            val parts = mutableListOf(stringResource(R.string.acq_src_check_summary, result.connected, result.total))
            if (result.failed > 0) parts += stringResource(R.string.acq_src_check_failed, result.failed)
            if (result.paused > 0) parts += stringResource(R.string.acq_src_check_paused_count, result.paused)
            Text(parts.joinToString(", ") + ".", style = MaterialTheme.typography.bodyMedium)
        }
        result.lines.forEach { CheckResultLine(it) }
    }
}

@Composable
private fun CheckResultLine(line: SettingsSourceCheckLine) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
            Text(line.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            StatusChip(sourceCheckWordText(line.word), line.tone)
        }
        val meta =
            buildList {
                if (line.word == SettingsSourceCheckWord.CONNECTED) {
                    add(pluralStringResource(R.plurals.acq_src_check_results, line.count, line.count))
                }
                if (line.word != SettingsSourceCheckWord.PAUSED && line.ms > 0) add(sourceDurationText(line.ms))
            }
        if (meta.isNotEmpty()) Hint(meta.joinToString(" · "))
        if (line.detail.isNotBlank()) {
            if (line.word == SettingsSourceCheckWord.NOT_CONNECTED) FailText(line.detail) else Hint(line.detail)
        }
    }
}
