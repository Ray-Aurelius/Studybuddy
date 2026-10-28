package com.rayaurelius.studybuddy.data

import com.rayaurelius.studybuddy.data.db.CustomCategory
import com.rayaurelius.studybuddy.data.db.Goal
import com.rayaurelius.studybuddy.data.db.LearningItem
import com.rayaurelius.studybuddy.data.db.StudySession
import com.rayaurelius.studybuddy.data.db.Subject
import com.rayaurelius.studybuddy.data.db.UnlockedMilestone
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Portable JSON export: the learner owns their record and can take it anywhere. */
@Serializable
data class BackupData(
    val app: String = "studybuddy",
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
        const val CURRENT_VERSION = 2
        val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

        fun encode(data: BackupData): String = json.encodeToString(serializer(), data)

        fun decode(text: String): BackupData {
            val data = json.decodeFromString(serializer(), text)
            require(data.app == "studybuddy") { "This file isn't a Studybuddy backup." }
            require(data.version <= CURRENT_VERSION) { "This backup is from a newer version of Studybuddy. Update the app first." }
            return data
        }
    }
}
