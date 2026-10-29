package com.rayaurelius.sophinity.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AutoBackupTest {
    @Test fun keepsTheNewestFourteenDays() {
        val start = LocalDate.of(2026, 10, 1)
        val names = (0 until 20).map { AutoBackup.fileName(start.plusDays(it.toLong())) }.shuffled()
        val pruned = AutoBackup.toPrune(names)
        assertEquals(6, pruned.size)
        // The six oldest go, across a month boundary too.
        assertEquals((0 until 6).map { AutoBackup.fileName(start.plusDays(it.toLong())) }.sortedDescending(), pruned)
    }

    @Test fun leavesOtherFilesAlone() {
        val names = listOf("notes.txt", "sophinity-2026-10-01.json") + (1..15).map { AutoBackup.fileName(LocalDate.of(2026, 9, it)) }
        val pruned = AutoBackup.toPrune(names)
        assertEquals(listOf(AutoBackup.fileName(LocalDate.of(2026, 9, 1))), pruned)
    }

    @Test fun nothingToPruneWithFewBackups() {
        assertEquals(emptyList<String>(), AutoBackup.toPrune(listOf(AutoBackup.fileName(LocalDate.of(2026, 10, 28)))))
    }
}
