package ch.orellbuehler.acolyte.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import ch.orellbuehler.acolyte.image.ArtPlaceholder
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.ui.LocalSdk
import coil3.compose.AsyncImage

@Composable
fun ServerImage(
    source: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: ImageSize = ImageSize.POSTER,
    placeholderName: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
) {
    val images = LocalSdk.current.images
    val url = remember(source, size) { images.url(source, size) }
    Box(modifier) {
        if (placeholderName != null) ArtPlaceholder(placeholderName, Modifier.fillMaxSize())
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = contentScale,
                alignment = alignment,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun ArtPlaceholder(
    name: String,
    modifier: Modifier = Modifier,
) {
    val hue = remember(name) { ArtPlaceholder.hue(name).toFloat() }
    val text = remember(name) { ArtPlaceholder.text(name) }
    val dark = Color.hsl(hue, 0.32f, 0.26f)
    Box(modifier.background(dark), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = Color.hsl(hue, 0.45f, 0.82f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
