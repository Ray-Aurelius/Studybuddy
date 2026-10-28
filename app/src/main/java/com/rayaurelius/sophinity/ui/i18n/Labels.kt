package com.rayaurelius.sophinity.ui.i18n

import androidx.compose.runtime.Composable
import com.rayaurelius.sophinity.data.Snapshot
import com.rayaurelius.sophinity.data.db.Goal
import com.rayaurelius.sophinity.domain.GoalMath
import com.rayaurelius.sophinity.domain.str

/**
 * A goal's display name: what the learner typed, or else a description generated in the
 * current language ("5h a week"), so untitled goals follow language changes.
 */
@Composable
fun goalTitle(snap: Snapshot, goal: Goal): String =
    goal.title.ifBlank { GoalMath.describe(goal, snap.categoryOf(goal)).str().replaceFirstChar { it.titlecase() } }
