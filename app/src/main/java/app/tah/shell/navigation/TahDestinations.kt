package app.tah.shell.navigation

object TahDestinations {
    const val ONBOARD = "onboard"
    const val BOARD = "board"
    const val DISPATCH = "dispatch"
    const val PROVIDERS = "providers"
    const val SKILLS = "skills"
    const val SETTINGS = "settings"
    const val SESSION_DETAIL = "session/{sessionId}?focus={focus}"

    fun sessionDetail(sessionId: String, focus: String = ""): String =
        "session/$sessionId?focus=$focus"
}
