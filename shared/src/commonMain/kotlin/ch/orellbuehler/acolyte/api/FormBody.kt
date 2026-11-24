package ch.orellbuehler.acolyte.api

internal class FormFile(
    val field: String,
    val name: String,
    val type: String,
    val bytes: ByteArray,
)

internal interface FormBody {
    val formFields: List<Pair<String, String>>

    val formFiles: List<FormFile>
}

internal interface RawBody {
    val rawBytes: ByteArray

    val rawType: String
}
