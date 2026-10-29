package org.cabas.app.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.*
import org.cabas.app.MainActivity
import org.cabas.app.R
import org.cabas.app.data.Backup
import org.cabas.app.data.CabasDatabase
import org.cabas.app.domain.autoBackupName
import org.cabas.app.domain.autoBackupsToDelete
import org.cabas.app.domain.delayUntilHour
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Local preferences shared by the app and its background work. Never part of a backup. */
class AppPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("preferences", 0)
    var lastBackupDay: LocalDate?
        get() = prefs.getLong("lastBackupDay", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }?.let(LocalDate::ofEpochDay)
        set(value) { prefs.edit().apply { if (value == null) remove("lastBackupDay") else putLong("lastBackupDay", value.toEpochDay()) }.apply() }
    var autoBackupFolder: String?
        get() = prefs.getString("autoBackupFolder", null)
        set(value) { prefs.edit().putString("autoBackupFolder", value).apply() }
    var lastAutoBackupFailed: Boolean
        get() = prefs.getBoolean("lastAutoBackupFailed", false)
        set(value) { prefs.edit().putBoolean("lastAutoBackupFailed", value).apply() }
    /** Hour of the daily reminder, or null when it is off. */
    var reminderHour: Int?
        get() = prefs.getInt("reminderHour", -1).takeIf { it in 0..23 }
        set(value) { prefs.edit().putInt("reminderHour", value ?: -1).apply() }
    var lockEnabled: Boolean
        get() = prefs.getBoolean("lockEnabled", false)
        set(value) { prefs.edit().putBoolean("lockEnabled", value).apply() }
}

const val ACTION_ADD = "org.cabas.app.action.ADD"
private const val AUTO_BACKUP_WORK = "auto-backup"
private const val REMINDER_WORK = "daily-reminder"
private const val REMINDER_CHANNEL = "daily-reminder"

object Background {
    /** Weekly backup into the folder chosen by the user, plus one right now. */
    fun scheduleAutoBackup(context: Context) {
        val work = WorkManager.getInstance(context)
        work.enqueueUniquePeriodicWork(AUTO_BACKUP_WORK, ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS).build())
        work.enqueue(OneTimeWorkRequestBuilder<AutoBackupWorker>().build())
    }

    fun cancelAutoBackup(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(AUTO_BACKUP_WORK)

    fun scheduleReminder(context: Context, hour: Int) {
        val delay = delayUntilHour(LocalDateTime.now(), hour)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(REMINDER_WORK, ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS).build())
    }

    fun cancelReminder(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK)

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Writes a backup into [folder] and keeps only the newest automatic copies there. */
    suspend fun writeAutoBackup(context: Context, folder: Uri) {
        val tree = DocumentFile.fromTreeUri(context, folder) ?: error("No folder")
        if (!tree.canWrite()) error("Folder not writable")
        val today = LocalDate.now()
        val json = Backup.toJson(Backup.read(CabasDatabase.get(context)), today)
        val name = autoBackupName(today)
        val file = tree.findFile(name) ?: tree.createFile("application/json", name) ?: error("Cannot create file")
        context.contentResolver.openOutputStream(file.uri, "wt")?.use { it.write(json.toByteArray(Charsets.UTF_8)) } ?: error("Cannot write")
        val files = tree.listFiles().associateBy { it.name.orEmpty() }
        autoBackupsToDelete(files.keys.toList()).forEach { files[it]?.delete() }
    }
}

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = AppPrefs(applicationContext)
        val folder = prefs.autoBackupFolder?.let(Uri::parse) ?: return Result.success()
        return try {
            Background.writeAutoBackup(applicationContext, folder)
            prefs.lastBackupDay = LocalDate.now()
            prefs.lastAutoBackupFailed = false
            Result.success()
        } catch (_: Exception) {
            prefs.lastAutoBackupFailed = true
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

@SuppressLint("MissingPermission") // Checked in canNotify() and guarded against revocation below.
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        if (AppPrefs(context).reminderHour == null || !Background.canNotify(context)) return Result.success()
        // Nothing to remind if something was already noted today.
        if (CabasDatabase.get(context).movements().countOn(LocalDate.now().toEpochDay()) > 0) return Result.success()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(REMINDER_CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 1,
            Intent(context, MainActivity::class.java).setAction(ACTION_ADD).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text))
            .setContentIntent(open).setAutoCancel(true).build()
        try { NotificationManagerCompat.from(context).notify(1, notification) } catch (_: SecurityException) { }
        return Result.success()
    }
}
