package com.rayaurelius.sophinity.ui.today

import androidx.annotation.StringRes
import com.rayaurelius.sophinity.R
import java.time.LocalDate

/** Evidence-based learning strategies, one per day. Short, actionable, and true. */
data class Tip(val emoji: String, @StringRes val title: Int, @StringRes val body: Int)

object LearningTips {
    val all = listOf(
        Tip("🗓️", R.string.tip_1_title, R.string.tip_1_body),
        Tip("🧠", R.string.tip_2_title, R.string.tip_2_body),
        Tip("🔀", R.string.tip_3_title, R.string.tip_3_body),
        Tip("🧑‍🏫", R.string.tip_4_title, R.string.tip_4_body),
        Tip("📍", R.string.tip_5_title, R.string.tip_5_body),
        Tip("😴", R.string.tip_6_title, R.string.tip_6_body),
        Tip("🎯", R.string.tip_7_title, R.string.tip_7_body),
        Tip("✍️", R.string.tip_8_title, R.string.tip_8_body),
        Tip("📵", R.string.tip_9_title, R.string.tip_9_body),
        Tip("🔗", R.string.tip_10_title, R.string.tip_10_body),
        Tip("🌱", R.string.tip_11_title, R.string.tip_11_body),
        Tip("🖼️", R.string.tip_12_title, R.string.tip_12_body),
        Tip("❓", R.string.tip_13_title, R.string.tip_13_body),
        Tip("🏁", R.string.tip_14_title, R.string.tip_14_body),
    )

    fun forDay(day: LocalDate): Tip = all[(day.toEpochDay() % all.size).toInt()]
}
