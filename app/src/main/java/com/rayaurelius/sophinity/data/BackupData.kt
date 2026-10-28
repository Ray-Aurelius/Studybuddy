package com.rayaurelius.sophinity.data

import com.rayaurelius.sophinity.data.db.CustomCategory
import com.rayaurelius.sophinity.data.db.Goal
import com.rayaurelius.sophinity.data.db.LearningItem
import com.rayaurelius.sophinity.data.db.StudySession
import com.rayaurelius.sophinity.data.db.Subject
import com.rayaurelius.sophinity.data.db.UnlockedMilestone
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Portable JSON export: the learner owns their record and can take it anywhere. */
@Serializable
data class BackupData(
    val app: String = APP_ID,
    val version: Int,
    val exportedAt: Long,
    val subjects: List<Subject>,
    /** Added in backup version 2. */
    val categories: List<CustomCategory> = emptyList(),
    val sessions: List<StudySession>,
    val items: List<LearningItem>,
    val goals: List<Goal>,
    val milestones: List<UnlockedMilestone>,
    val streakMinutes: Int = 10,
    val restDaysPerWeek: Int = 1,
) {
    companion object {
        const val CURRENT_VERSION = 3
        const val APP_ID = "sophinity"
        /** Backups made before the rename (as "studybuddy") still restore. */
        val ACCEPTED_APP_IDS = setOf(APP_ID, "studybuddy")
        val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

        fun encode(data: BackupData): String = json.encodeToString(serializer(), data)

        fun decode(text: String): BackupData {
            val data = json.decodeFromString(serializer(), text)
            require(data.app in ACCEPTED_APP_IDS) { "This file isn't a Sophinity backup." }
            require(data.version <= CURRENT_VERSION) { "This backup is from a newer version of Sophinity. Update the app first." }
            return data
        }
    }
}
