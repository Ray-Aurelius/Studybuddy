package com.rayaurelius.sophinity.ui.today

import java.time.LocalDate

/** Evidence-based learning strategies, one per day. Short, actionable, and true. */
data class Tip(val emoji: String, val title: String, val body: String)

object LearningTips {
    val all = listOf(
        Tip("🗓️", "Space it out", "Several short sessions across the week beat one long cram. Spacing is one of the most replicated findings in learning science."),
        Tip("🧠", "Test yourself", "After reading, close the book and write down what you remember. Retrieval strengthens memory far more than re-reading."),
        Tip("🔀", "Mix it up", "Interleave topics or problem types instead of drilling one at a time. It feels harder, and that's why it works."),
        Tip("🧑‍🏫", "Teach it back", "Explain the idea in plain words as if to a curious 12-year-old. Gaps in your explanation are gaps in your understanding."),
        Tip("📍", "Decide when and where", "\"I'll learn at 7 pm at the kitchen table\" makes you far more likely to follow through than \"I'll get to it tonight.\""),
        Tip("😴", "Sleep on it", "Sleep consolidates what you learned during the day. A late-night cram often costs more than it gains."),
        Tip("🎯", "Aim just past comfortable", "Pick practice that's slightly too hard. If it feels easy, you're rehearsing, not learning."),
        Tip("✍️", "One-line reflection", "Ending a session with \"one thing I learned\" is a tiny retrieval exercise that also shows you your progress."),
        Tip("📵", "Guard your focus", "Put your phone in another room for a focus block. Even a phone face-down on the desk can drain attention."),
        Tip("🔗", "Connect it", "Ask \"how does this relate to something I already know?\" Linked ideas are easier to recall later."),
        Tip("🌱", "Go easy on misses", "Missing a day doesn't break a habit. What matters is getting back on track the next day."),
        Tip("🖼️", "Words + pictures", "Sketch a quick diagram of what you're learning. Combining visuals with words gives memory two paths in."),
        Tip("❓", "Ask why", "Keep asking \"why is this true?\" as you read. Self-explanation turns facts into understanding."),
        Tip("🏁", "Finish with a plan", "End each session by noting where to start next time. Starting is the hardest part — make it easy for future you."),
    )

    fun forDay(day: LocalDate): Tip = all[(day.toEpochDay() % all.size).toInt()]
}
