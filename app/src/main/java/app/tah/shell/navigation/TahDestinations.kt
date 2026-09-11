package app.tah.shell.navigation

object TahDestinations {
    const val BOARD = "board"
    const val DISPATCH = "dispatch"
    const val PROVIDERS = "providers"
    const val SKILLS = "skills"
    const val SETTINGS = "settings"
    const val SESSION_DETAIL = "session/{sessionId}"

    fun sessionDetail(sessionId: String): String = "session/$sessionId"
}
