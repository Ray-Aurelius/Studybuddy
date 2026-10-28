package com.rayaurelius.sophinity.domain

import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.data.db.CustomCategory
import com.rayaurelius.sophinity.data.db.Goal
import com.rayaurelius.sophinity.data.db.ItemType
import com.rayaurelius.sophinity.data.db.LearningItem

/**
 * One library category as shown to the learner: a built-in type (translated) or one they
 * created (shown exactly as they named it).
 */
data class CategoryInfo(
    val type: ItemType,
    val customId: Long?,
    val emoji: String,
    val label: Txt,
    val plural: Txt,
    val unit: Txt,
) {
    /** Stable key for UI state and grouping. */
    val key: String get() = if (type == ItemType.CUSTOM) "c${customId ?: 0}" else type.name
    val isCustom: Boolean get() = type == ItemType.CUSTOM

    fun matches(item: LearningItem): Boolean =
        item.type == type && (type != ItemType.CUSTOM || item.categoryId == customId)

    fun matchesGoal(goal: Goal): Boolean =
        goal.itemType == type && (type != ItemType.CUSTOM || goal.categoryId == customId)

    /** "3 books"; for learner-named categories, which can't be pluralized reliably in every language: "Workshop × 3". */
    fun count(n: Int): Txt =
        if (type == ItemType.CUSTOM) Txt.Res(R.string.count_custom, listOf(n, label)) else Txt.Plural(type.countRes, n)
}

object Categories {
    /** Shown for items whose custom category was deleted. */
    val OTHER = CategoryInfo(ItemType.CUSTOM, null, "📌", Txt.Res(R.string.type_other), Txt.Res(R.string.type_other), Txt.Res(R.string.unit_parts))

    fun builtIn(t: ItemType) = CategoryInfo(t, null, t.emoji, Txt.Res(t.labelRes), Txt.Res(t.pluralRes), Txt.Res(t.unitRes))

    fun custom(c: CustomCategory) = CategoryInfo(ItemType.CUSTOM, c.id, c.emoji, Txt.Raw(c.name), Txt.Raw(c.name), Txt.Raw(c.unitLabel))

    fun all(custom: List<CustomCategory>): List<CategoryInfo> =
        ItemType.entries.filter { it != ItemType.CUSTOM }.map(::builtIn) + custom.map(::custom)

    fun of(item: LearningItem, customById: Map<Long, CustomCategory>): CategoryInfo =
        if (item.type != ItemType.CUSTOM) builtIn(item.type)
        else item.categoryId?.let { customById[it] }?.let(::custom) ?: OTHER

    fun forGoal(goal: Goal, customById: Map<Long, CustomCategory>): CategoryInfo? = when (goal.itemType) {
        null -> null
        ItemType.CUSTOM -> goal.categoryId?.let { customById[it] }?.let(::custom) ?: OTHER
        else -> builtIn(goal.itemType)
    }

}
