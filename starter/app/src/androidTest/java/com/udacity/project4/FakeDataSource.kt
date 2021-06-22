package com.udacity.project4

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource() : ReminderDataSource {

    var mTasks: MutableList<ReminderDTO> = mutableListOf()
    var mReturnErrors: Boolean = false

    //    DONE: Create a fake data source to act as a double to the real data source

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        // DONE("Return the reminders")
        if (mReturnErrors) {
            return Result.Error("No reminders found")
        }

        return Result.Success(mTasks)

    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        mTasks.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (mReturnErrors) {
            return Result.Error("No reminders found")
        }

        return Result.Success(mTasks.first { it.id == id })
    }

    override suspend fun deleteAllReminders() {
        mTasks = mutableListOf()
    }

    fun setTasks(tasks: MutableList<ReminderDTO>) {
        mTasks = tasks
    }

    fun setReturnErrors(value: Boolean) {
        mReturnErrors = value
    }

}