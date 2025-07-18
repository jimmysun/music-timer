package org.ganquan.musictimer.tools

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TAG: String = "timerWorker"
private const val KEY_PLAY_TIME = "playTime"
private const val KEY_DELAY_TIME = "delayTime"

class OneTimeWorkerService : Service() {
    private val startRequestId: UUID = UUID.randomUUID()
    private val endRequestId: UUID = UUID.randomUUID()

    override fun onCreate() {
        super.onCreate()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val playTime = intent?.getLongExtra(KEY_PLAY_TIME, 0) as Long
        val delayTime = intent.getLongExtra(KEY_DELAY_TIME, 1)
        request(playTime, delayTime)
        return START_STICKY
    }

    override fun onDestroy() {
        WorkManager.getInstance(this).cancelAllWorkByTag(TAG)
        super.onDestroy()
    }

    private fun request(
        playTime: Long,
        delayTime: Long = 1
    ) {
        val startRequest = OneTimeWorkRequestBuilder<StartWork>()
            .addTag(TAG)
            .setId(startRequestId)
            .setInitialDelay(delayTime, TimeUnit.SECONDS)
            .build()

        val endRequest = OneTimeWorkRequestBuilder<EndWork>()
            .addTag(TAG)
            .setId(endRequestId)
            .setInitialDelay(delayTime + playTime, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniqueWork("startTask", ExistingWorkPolicy.REPLACE, startRequest)
        WorkManager.getInstance(this)
            .enqueueUniqueWork("endTask", ExistingWorkPolicy.REPLACE, endRequest)

        WorkManager.getInstance(this)
            .beginWith(startRequest)
            .then(endRequest)
            .enqueue()
    }
}

class OneTimeWorkerIntent(context: Context)
    : Intent(context, OneTimeWorkerService::class.java) {

    override fun putExtra(name: String?, value: Array<out CharSequence?>?): OneTimeWorkerIntent {
        if(name in listOf<String>(KEY_DELAY_TIME, KEY_PLAY_TIME)) {
            super.putExtra(name, value)
        }
        return this
    }

    fun setExtra(delayTime: Long=0, playTime: Long=0): OneTimeWorkerIntent {
        super.putExtra(KEY_DELAY_TIME, delayTime)
        super.putExtra(KEY_PLAY_TIME, playTime)
        return this
    }
}

class StartWork(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    private val context = appContext
    override fun doWork(): Result {
        Broadcast.sendLocal(context, "start worker")
        return Result.success()
    }
}

class EndWork(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    private val context = appContext
    override fun doWork(): Result {
        Broadcast.sendLocal(context, "end worker")
        return Result.success()
    }
}