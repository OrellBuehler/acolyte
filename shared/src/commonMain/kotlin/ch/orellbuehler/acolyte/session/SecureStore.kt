package ch.orellbuehler.acolyte.session

interface SecureStore {
    fun read(key: String): String?

    fun write(
        key: String,
        value: String,
    )

    fun remove(key: String)
}

class MemoryStore : SecureStore {
    private val values = mutableMapOf<String, String>()

    override fun read(key: String): String? = values[key]

    override fun write(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
