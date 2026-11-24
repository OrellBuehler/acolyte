package ch.orellbuehler.acolyte.ui.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import ch.orellbuehler.acolyte.api.TransferredFile
import java.io.File

fun shareTransferredFile(
    context: Context,
    file: TransferredFile,
    mime: String = "application/octet-stream",
) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.transfers", File(file.path))
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(Intent.createChooser(send, null))
}

fun shareText(
    context: Context,
    text: String,
) {
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    context.startActivity(Intent.createChooser(send, null))
}
