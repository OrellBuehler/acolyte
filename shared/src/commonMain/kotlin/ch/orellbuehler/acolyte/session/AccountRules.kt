package ch.orellbuehler.acolyte.session

object AccountRules {
    private val NAME = Regex("^[A-Za-z0-9_.]{1,30}$")

    const val MAX_PASSWORD_BYTES = 1024
    const val MAX_DISPLAY_NAME = 64

    fun validName(name: String): Boolean = NAME.matches(name)

    fun cleanName(raw: String): String =
        raw.filter { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '_' || it == '.' }.take(30)

    fun validPassword(
        password: String,
        minimum: Int,
    ): Boolean {
        val bytes = password.encodeToByteArray().size
        return bytes >= maxOf(minimum, 4) && bytes <= MAX_PASSWORD_BYTES
    }

    fun validDisplayName(name: String): Boolean = name.trim().let { it.length <= MAX_DISPLAY_NAME && it.none { c -> c.isISOControl() } }
}
