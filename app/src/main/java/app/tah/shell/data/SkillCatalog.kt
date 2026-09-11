package app.tah.shell.data

object SkillCatalog {
    val packs: List<SkillPack> = listOf(
        SkillPack(
            id = "general",
            title = "General steer",
            body = """
                # General
                Be concise. Surface tool intent before acting. Prefer cards over chatter.
                Never claim a tool ran unless a tool card exists.
            """.trimIndent(),
        ),
        SkillPack(
            id = "research",
            title = "Research sweep",
            body = """
                # Research
                Cite sources in plain language. Stop cleanly when the budget is hit.
                Prefer read/network tools over exec.
            """.trimIndent(),
        ),
        SkillPack(
            id = "coding",
            title = "Coding patch",
            body = """
                # Coding
                Propose the smallest write. Exec stays gated. Show paths on tool cards.
            """.trimIndent(),
        ),
    )

    fun byId(id: String?): SkillPack = packs.firstOrNull { it.id == id } ?: packs.first()
}
