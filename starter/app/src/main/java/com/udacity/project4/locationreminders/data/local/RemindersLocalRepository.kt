package com.udacity.project4.locationreminders.data.local

import android.util.Log
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.utils.wrapEspressoIdlingResource
import kotlinx.coroutines.*

/**
 * Concrete implementation of a data source as a db.
 *
 * The repository is implemented so that you can focus on only testing it.
 *
 * @param remindersDao the dao that does the Room db operations
 * @param ioDispatcher a coroutine dispatcher to offload the blocking IO tasks
 */
const val TAG = "RemindersLocalRepository"

class RemindersLocalRepository(
    private val remindersDao: RemindersDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ReminderDataSource {

    /**
     * Get the reminders list from the local db
     * @return Result the holds a Success with all the reminders or an Error object with the error message
     */
    override suspend fun getReminders(): Result<List<ReminderDTO>> = wrapEspressoIdlingResource {
        withContext(ioDispatcher) {
            Log.d(TAG, "getReminders is called")
            return@withContext try {
                Result.Success(remindersDao.getReminders())
            } catch (ex: Exception) {
                Log.e(TAG, "getReminders has failed")
                Result.Error(ex.localizedMessage)
            }
        }
    }

    /**
     * Insert a reminder in the db.
     * @param reminder the reminder to be inserted
     */
    override suspend fun saveReminder(reminder: ReminderDTO) = wrapEspressoIdlingResource {
        withContext(ioDispatcher) {
            Log.d(TAG, "saveReminder is called")
            remindersDao.saveReminder(reminder)
        }
    }

    /**
     * Get a reminder by its id
     * @param id to be used to get the reminder
     * @return Result the holds a Success object with the Reminder or an Error object with the error message
     */
    override suspend fun getReminder(id: String): Result<ReminderDTO> = wrapEspressoIdlingResource {
        withContext(ioDispatcher) {
            try {
                Log.d(TAG, "getReminder is called for id $id")
                val reminder = remindersDao.getReminderById(id)
                if (reminder != null) {
                    Log.i(TAG, "getReminder success for id $id")
                    return@withContext Result.Success(reminder)
                } else {
                    Log.e(TAG, "getReminder failure for id $id")
                    return@withContext Result.Error("Reminder not found!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getReminder exception for id $id")
                return@withContext Result.Error(e.localizedMessage)
            }
        }
    }

    /**
     * Deletes all the reminders in the db
     */
    override suspend fun deleteAllReminders() = wrapEspressoIdlingResource {
        withContext(ioDispatcher) {
            Log.d(TAG, "deleteAllReminders is called")
            remindersDao.deleteAllReminders()
        }
    }
}
