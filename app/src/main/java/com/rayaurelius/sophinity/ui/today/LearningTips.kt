package com.rayaurelius.sophinity.ui.today

import androidx.annotation.StringRes
import com.rayaurelius.sophinity.R
import java.time.LocalDate

/** Evidence-based learning strategies, one per day. Short, actionable, and true. */
data class Tip(val glyph: String, @StringRes val title: Int, @StringRes val body: Int)

object LearningTips {
    val all = listOf(
        Tip("sundial", R.string.tip_1_title, R.string.tip_1_body),
        Tip("tablet", R.string.tip_2_title, R.string.tip_2_body),
        Tip("mosaic", R.string.tip_3_title, R.string.tip_3_body),
        Tip("stoa", R.string.tip_4_title, R.string.tip_4_body),
        Tip("hourglass", R.string.tip_5_title, R.string.tip_5_body),
        Tip("moon", R.string.tip_6_title, R.string.tip_6_body),
        Tip("steps", R.string.tip_7_title, R.string.tip_7_body),
        Tip("quill", R.string.tip_8_title, R.string.tip_8_body),
        Tip("shield", R.string.tip_9_title, R.string.tip_9_body),
        Tip("dyad", R.string.tip_10_title, R.string.tip_10_body),
        Tip("sprout", R.string.tip_11_title, R.string.tip_11_body),
        Tip("amphora", R.string.tip_12_title, R.string.tip_12_body),
        Tip("lamp", R.string.tip_13_title, R.string.tip_13_body),
        Tip("scroll", R.string.tip_14_title, R.string.tip_14_body),
    )

    fun forDay(day: LocalDate): Tip = all[(day.toEpochDay() % all.size).toInt()]
}
