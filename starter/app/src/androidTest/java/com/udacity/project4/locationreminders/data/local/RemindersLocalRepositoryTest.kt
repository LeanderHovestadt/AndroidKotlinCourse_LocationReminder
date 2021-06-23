package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.util.MainCoroutineRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.junit.*
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

//    DONE: Add testing implementation to the RemindersLocalRepository.kt

    private lateinit var reminderDao: RemindersDao
    private lateinit var remindersLocalRepository: RemindersLocalRepository
    private lateinit var reminderOktoberfest: ReminderDTO
    private lateinit var reminderMtbTrailsSintra: ReminderDTO
    private lateinit var reminderMeetChristian: ReminderDTO

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        reminderDao = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build().reminderDao()
        remindersLocalRepository = RemindersLocalRepository(
            reminderDao, Dispatchers.Unconfined
        )
    }

    @Before
    fun initVariables() {
        reminderOktoberfest = ReminderDTO(
            "Oktoberfest reminder",
            "Go to the Oktoberfest!",
            "munich",
            48.137154,
            11.576124
        )
        reminderMtbTrailsSintra = ReminderDTO(
            "Sintra trail reminder",
            "Oh my god, you need to ride these trails!",
            "sintra",
            38.787777,
            -9.390556
        )
        reminderMeetChristian = ReminderDTO(
            "Siegen meet Christian Textor",
            "Do not forget to drink a beer with the national champ Christian Textor!",
            "siegen",
            50.883331,
            8.016667
        )
    }

    @Test
    fun saveReminder_doesAddReminderToLocalCache() = runBlockingTest {

        // GIVEN - an empty local cache

        // WHEN - a reminder is saved to the tasks repository
        remindersLocalRepository.saveReminder(reminderOktoberfest)

        // THEN - the local sources are called and the cache is updated

        val loadedReminders = (remindersLocalRepository.getReminders() as? Result.Success)?.data
        Assert.assertThat<List<ReminderDTO>>(
            loadedReminders as List<ReminderDTO>,
            CoreMatchers.notNullValue()
        )
        Truth.assertThat(loadedReminders.contains(reminderOktoberfest))
        Truth.assertThat(!loadedReminders.contains(reminderMtbTrailsSintra))
        Truth.assertThat(!loadedReminders.contains(reminderMeetChristian))

        // WHEN - an other reminder is saved to the tasks repository
        remindersLocalRepository.saveReminder(reminderMtbTrailsSintra)

        // THEN - the local sources are called and the cache is updated
        val loadedRemindersSecond = (remindersLocalRepository.getReminders() as? Result.Success)?.data
        Assert.assertThat<List<ReminderDTO>>(
            loadedRemindersSecond as List<ReminderDTO>,
            CoreMatchers.notNullValue()
        )
        Truth.assertThat(loadedRemindersSecond.contains(reminderOktoberfest))
        Truth.assertThat(loadedRemindersSecond.contains(reminderMtbTrailsSintra))
        Truth.assertThat(!loadedRemindersSecond.contains(reminderMeetChristian))

        // WHEN - an other reminder is saved to the tasks repository
        remindersLocalRepository.saveReminder(reminderMeetChristian)

        // THEN - the local sources are called and the cache is updated
        val loadedRemindersThird = (remindersLocalRepository.getReminders() as? Result.Success)?.data
        Assert.assertThat<List<ReminderDTO>>(
            loadedRemindersThird as List<ReminderDTO>,
            CoreMatchers.notNullValue()
        )
        Truth.assertThat(loadedRemindersThird.contains(reminderOktoberfest))
        Truth.assertThat(loadedRemindersThird.contains(reminderMtbTrailsSintra))
        Truth.assertThat(loadedRemindersThird.contains(reminderMeetChristian))
    }

    @Test
    fun getReminder_returnsCorrectReminder() = runBlockingTest {

        // GIVEN - there are some reminders in the Dao
        remindersLocalRepository.saveReminder(reminderOktoberfest)
        remindersLocalRepository.saveReminder(reminderMtbTrailsSintra)
        remindersLocalRepository.saveReminder(reminderMeetChristian)

        // WHEN - reminder is requested by id
        val loadedReminder =
            (remindersLocalRepository.getReminder(reminderOktoberfest.id) as? Result.Success)?.data

        // THEN - the correct reminder has been returned
        Assert.assertThat<ReminderDTO>(loadedReminder as ReminderDTO, CoreMatchers.notNullValue())
        Assert.assertThat(loadedReminder.id, CoreMatchers.`is`(reminderOktoberfest.id))
        Assert.assertThat(loadedReminder.title, CoreMatchers.`is`(reminderOktoberfest.title))
        Assert.assertThat(
            loadedReminder.description,
            CoreMatchers.`is`(reminderOktoberfest.description)
        )
        Assert.assertThat(loadedReminder.location, CoreMatchers.`is`(reminderOktoberfest.location))
        Assert.assertThat(loadedReminder.latitude, CoreMatchers.`is`(reminderOktoberfest.latitude))
        Assert.assertThat(
            loadedReminder.longitude,
            CoreMatchers.`is`(reminderOktoberfest.longitude)
        )
    }

    @Test
    fun getReminder_withInvalidIdReturnsErrorMessage() = runBlockingTest {

        // GIVEN - there are some reminders in the Dao
        remindersLocalRepository.saveReminder(reminderOktoberfest)
        remindersLocalRepository.saveReminder(reminderMtbTrailsSintra)
        remindersLocalRepository.saveReminder(reminderMeetChristian)

        // WHEN - reminder is requested by wrong id
        val message = (remindersLocalRepository.getReminder("anyWrongId") as? Result.Error)?.message

        // THEN - the correct error message has been returned
        Assert.assertThat<String>(message, CoreMatchers.notNullValue())
        Assert.assertThat<String>(message, CoreMatchers.`is`("Reminder not found!"))
    }

    @Test
    fun deleteAllReminders_willDeleteAllReminders() = runBlockingTest {

        // GIVEN - there are some reminders in the Dao
        remindersLocalRepository.saveReminder(reminderOktoberfest)
        remindersLocalRepository.saveReminder(reminderMtbTrailsSintra)
        remindersLocalRepository.saveReminder(reminderMeetChristian)

        // WHEN - reminders are deleted
        remindersLocalRepository.deleteAllReminders()

        // THEN - getReminders should return empty list
        Truth.assertThat((remindersLocalRepository.getReminders() as? Result.Success)?.data)
            .isEmpty()
    }
}