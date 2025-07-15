/*
 * Copyright (C) 2021 Dr.NooB
 *
 * This file is a part of Data Tracker <https://github.com/Sergey842248/DataTracker>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.drnoob.datamonitor.utils

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.drnoob.datamonitor.core.Values.DATA_QUOTA
import com.drnoob.datamonitor.core.Values.DATA_QUOTA_PERFORMED_RESET
import com.drnoob.datamonitor.core.Values.DATA_RESET
import com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY
import com.drnoob.datamonitor.core.Values.SESSION_YESTERDAY
import kotlin.math.round

/**
 * Worker class to calculate and update data quota everyday.
 */
class DataRolloverHelper(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    companion object {
        private val TAG = DataRolloverHelper::class.simpleName
    }

    override fun doWork(): Result {
        Log.d(TAG, "Recalculating data quota.")

        val workManager = WorkManager.getInstance(applicationContext)
        val smartDataAllocationWorkRequest = OneTimeWorkRequest
            .Builder(SmartDataAllocationService::class.java)
            .build()
        workManager.enqueueUniqueWork(
            "smart_data_allocation",
            ExistingWorkPolicy.REPLACE,
            smartDataAllocationWorkRequest
        )

        return Result.success()
    }

    /**
     * Worker class that resets the data quota once the current data plan iss over.
     */
    class QuotaRefreshHelper(context: Context, workerParams: WorkerParameters) :
        Worker(context, workerParams) {
        override fun doWork(): Result {
            Log.d(TAG, "doWork: Resetting data quota")

            val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)

            if (preference.getBoolean("smart_data_allocation", false)) {
                val workManager = WorkManager.getInstance(applicationContext)

                workManager.cancelUniqueWork("smart_data_allocation")
                workManager.cancelUniqueWork("data_rollover")
                workManager.cancelUniqueWork("quota_reset")

                val smartDataAllocationWorkRequest = OneTimeWorkRequest
                    .Builder(SmartDataAllocationService::class.java)
                    .build()

                workManager.enqueueUniqueWork(
                    "smart_data_allocation",
                    ExistingWorkPolicy.KEEP,
                    smartDataAllocationWorkRequest
                )

                preference.edit().putLong(DATA_QUOTA_PERFORMED_RESET, System.currentTimeMillis()).apply()

                return Result.success()
            }
            return Result.failure()
        }
    }
}
