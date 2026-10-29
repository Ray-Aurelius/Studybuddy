package com.rayaurelius.sophinity.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rayaurelius.sophinity.SophinityApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Automatic backups: once a day, the whole learning history is written as a JSON backup
 * (the same format as a manual export) into a folder the learner picked. Files are named
 * by date, so each day replaces only its own file, and the [KEEP] most recent are kept.
 *
 * The folder lives outside the app's private storage, so the backups survive the app being
 * uninstalled or its data cleared. Android's own cloud backup covers phone loss separately.
 */
object AutoBackup {
    private const val WORK = "auto_backup"
    const val KEEP = 14
    private const val PREFIX = "sophinity-auto-"

    fun schedule(context: Context, s: AppSettings) {
        val wm = runCatching { WorkManager.getInstance(context) }.getOrNull() ?: return
        if (!s.autoBackup || s.backupFolder == null) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val req = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        // KEEP: changing other settings must not push the next backup further away.
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    /** Keeps read/write access to the picked folder across reboots. */
    fun takePermission(context: Context, tree: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                tree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    /** Writes today's backup now. Returns true on success; the outcome is also recorded in settings. */
    suspend fun runNow(context: Context): Boolean {
        val c = (context.applicationContext as SophinityApp).container
        val s = c.settingsStore.current()
        val folder = s.backupFolder ?: return false
        val ok = runCatching {
            val json = BackupData.encode(c.repo.exportData())
            withContext(Dispatchers.IO) { write(context, Uri.parse(folder), json, LocalDate.now()) }
        }.isSuccess
        c.settingsStore.update {
            if (ok) it.copy(lastBackupAt = System.currentTimeMillis(), lastBackupFailed = false) else it.copy(lastBackupFailed = true)
        }
        return ok
    }

    /** Human-readable name of the picked folder, e.g. "Documents", or null if it can't be read. */
    fun folderName(context: Context, tree: String?): String? = tree?.let {
        runCatching {
            val uri = Uri.parse(it)
            val doc = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
            context.contentResolver.query(doc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cur ->
                if (cur.moveToFirst()) cur.getString(0) else null
            }
        }.getOrNull()
    }

    /** Which automatic backups to delete: everything but the newest [KEEP]. Dated names sort chronologically. */
    internal fun toPrune(names: List<String>, keep: Int = KEEP): List<String> =
        names.filter { it.startsWith(PREFIX) }.distinct().sortedDescending().drop(keep)

    fun fileName(date: LocalDate) = "$PREFIX$date.json"

    private fun write(context: Context, tree: Uri, json: String, date: LocalDate) {
        val resolver = context.contentResolver
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)
        val existing = mutableListOf<Pair<String, String>>() // name to documentId
        resolver.query(
            children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { cur ->
            while (cur.moveToNext()) {
                val name = cur.getString(1) ?: continue
                if (name.startsWith(PREFIX)) existing += name to cur.getString(0)
            }
        }
        val name = fileName(date)
        val target = existing.firstOrNull { it.first == name }?.let { DocumentsContract.buildDocumentUriUsingTree(tree, it.second) }
            ?: DocumentsContract.createDocument(resolver, parent, "application/json", name)
            ?: error("Could not create $name")
        resolver.openOutputStream(target, "wt")!!.use { it.write(json.toByteArray()) }

        toPrune(existing.map { it.first } + name).forEach { old ->
            existing.firstOrNull { it.first == old }?.let { (_, id) ->
                runCatching { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, id)) }
            }
        }
    }
}

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // A failure (e.g. the folder was deleted) is shown in Settings; retrying wouldn't fix it.
        AutoBackup.runNow(applicationContext)
        return Result.success()
    }
}
