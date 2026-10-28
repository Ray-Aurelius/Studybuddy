package com.rayaurelius.studybuddy.domain

import com.rayaurelius.studybuddy.data.db.CustomCategory
import com.rayaurelius.studybuddy.data.db.Goal
import com.rayaurelius.studybuddy.data.db.ItemType
import com.rayaurelius.studybuddy.data.db.LearningItem

/** One library category as shown to the learner: a built-in type or one they created. */
data class CategoryInfo(
    val type: ItemType,
    val customId: Long?,
    val emoji: String,
    val label: String,
    val plural: String,
    val unit: String,
) {
    /** Stable key for UI state and grouping. */
    val key: String get() = if (type == ItemType.CUSTOM) "c${customId ?: 0}" else type.name
    val isCustom: Boolean get() = type == ItemType.CUSTOM

    fun matches(item: LearningItem): Boolean =
        item.type == type && (type != ItemType.CUSTOM || item.categoryId == customId)

    fun matchesGoal(goal: Goal): Boolean =
        goal.itemType == type && (type != ItemType.CUSTOM || goal.categoryId == customId)

    fun count(n: Int): String = "$n ${if (n == 1) label.lowercase() else plural.lowercase()}"
}

object Categories {
    /** Shown for items whose custom category was deleted. */
    val OTHER = CategoryInfo(ItemType.CUSTOM, null, "📌", "Other", "Other", "parts")

    fun builtIn(t: ItemType) = CategoryInfo(t, null, t.emoji, t.label, t.plural, t.unitLabel)

    fun custom(c: CustomCategory) = CategoryInfo(ItemType.CUSTOM, c.id, c.emoji, c.name, pluralize(c.name), c.unitLabel)

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

    /** Simple English plural for user-typed names: "Workshop" → "Workshops", "Class" → "Classes". */
    fun pluralize(name: String): String {
        val n = name.trim()
        if (n.isEmpty()) return n
        val lower = n.lowercase()
        return when {
            lower.endsWith("s") && !lower.endsWith("ss") -> n
            lower.endsWith("ss") || lower.endsWith("sh") || lower.endsWith("ch") || lower.endsWith("x") -> n + "es"
            lower.endsWith("y") && lower.length > 1 && lower[lower.length - 2] !in "aeiou" -> n.dropLast(1) + "ies"
            else -> n + "s"
        }
    }
}
