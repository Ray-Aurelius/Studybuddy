package com.rayaurelius.sophinity.ui.today

import androidx.annotation.StringRes
import com.rayaurelius.sophinity.R
import java.time.LocalDate

/**
 * A line of ancient Greek wisdom for each day. Only well-attested sayings, with the source,
 * in plain modern English. (Several famous "Greek" quotes are misattributed; e.g. "We are what
 * we repeatedly do" is Will Durant summarizing Aristotle, so it is not included.)
 */
data class Wisdom(@StringRes val text: Int, @StringRes val who: Int, @StringRes val source: Int)

object Wisdoms {
    val all = listOf(
        Wisdom(R.string.wisdom_1, R.string.wisdom_1_who, R.string.wisdom_1_source),
        Wisdom(R.string.wisdom_2, R.string.wisdom_2_who, R.string.wisdom_2_source),
        Wisdom(R.string.wisdom_3, R.string.wisdom_3_who, R.string.wisdom_3_source),
        Wisdom(R.string.wisdom_4, R.string.wisdom_4_who, R.string.wisdom_4_source),
        Wisdom(R.string.wisdom_5, R.string.wisdom_5_who, R.string.wisdom_5_source),
        Wisdom(R.string.wisdom_6, R.string.wisdom_6_who, R.string.wisdom_6_source),
        Wisdom(R.string.wisdom_7, R.string.wisdom_7_who, R.string.wisdom_7_source),
        Wisdom(R.string.wisdom_8, R.string.wisdom_8_who, R.string.wisdom_8_source),
        Wisdom(R.string.wisdom_9, R.string.wisdom_9_who, R.string.wisdom_9_source),
        Wisdom(R.string.wisdom_10, R.string.wisdom_10_who, R.string.wisdom_10_source),
        Wisdom(R.string.wisdom_11, R.string.wisdom_11_who, R.string.wisdom_11_source),
        Wisdom(R.string.wisdom_12, R.string.wisdom_12_who, R.string.wisdom_12_source),
        Wisdom(R.string.wisdom_13, R.string.wisdom_13_who, R.string.wisdom_13_source),
        Wisdom(R.string.wisdom_14, R.string.wisdom_14_who, R.string.wisdom_14_source),
        Wisdom(R.string.wisdom_15, R.string.wisdom_15_who, R.string.wisdom_15_source),
        Wisdom(R.string.wisdom_16, R.string.wisdom_16_who, R.string.wisdom_16_source),
        Wisdom(R.string.wisdom_17, R.string.wisdom_17_who, R.string.wisdom_17_source),
        Wisdom(R.string.wisdom_18, R.string.wisdom_18_who, R.string.wisdom_18_source),
    )

    fun forDay(day: LocalDate): Wisdom = all[(day.toEpochDay() % all.size).toInt()]
}
