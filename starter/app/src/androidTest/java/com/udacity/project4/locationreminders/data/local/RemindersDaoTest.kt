package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

//    DONE: Add testing implementation to the RemindersDao.kt
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    private lateinit var reminderOktoberfest : ReminderDTO
    private lateinit var reminderMtbTrailsSintra : ReminderDTO
    private lateinit var reminderMeetChristian : ReminderDTO

    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @Before
    fun initVariables(){
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

    @After
    fun closeDb() = database.close()

    @Test
    fun getReminders_hasAddedReminders() = runBlockingTest {
        // GIVEN - insert some reminders

        database.reminderDao().saveReminder(reminderOktoberfest)
        database.reminderDao().saveReminder(reminderMtbTrailsSintra)
        database.reminderDao().saveReminder(reminderMeetChristian)

        // WHEN - Get reminders from the database
        val reminders = database.reminderDao().getReminders()

        // THEN - There are all inserted reminders in the database
        Assert.assertThat(reminders.size, `is`(3))

        Assert.assertThat(reminders[0].id, `is`(reminderOktoberfest.id))
        Assert.assertThat(reminders[0].title, `is`(reminderOktoberfest.title))
        Assert.assertThat(reminders[0].description, `is`(reminderOktoberfest.description))
        Assert.assertThat(reminders[0].location, `is`(reminderOktoberfest.location))
        Assert.assertThat(reminders[0].latitude, `is`(reminderOktoberfest.latitude))
        Assert.assertThat(reminders[0].longitude, `is`(reminderOktoberfest.longitude))

        Assert.assertThat(reminders[1].id, `is`(reminderMtbTrailsSintra.id))
        Assert.assertThat(reminders[1].title, `is`(reminderMtbTrailsSintra.title))
        Assert.assertThat(reminders[1].description, `is`(reminderMtbTrailsSintra.description))
        Assert.assertThat(reminders[1].location, `is`(reminderMtbTrailsSintra.location))
        Assert.assertThat(reminders[1].latitude, `is`(reminderMtbTrailsSintra.latitude))
        Assert.assertThat(reminders[1].longitude, `is`(reminderMtbTrailsSintra.longitude))

        Assert.assertThat(reminders[2].id, `is`(reminderMeetChristian.id))
        Assert.assertThat(reminders[2].title, `is`(reminderMeetChristian.title))
        Assert.assertThat(reminders[2].description, `is`(reminderMeetChristian.description))
        Assert.assertThat(reminders[2].location, `is`(reminderMeetChristian.location))
        Assert.assertThat(reminders[2].latitude, `is`(reminderMeetChristian.latitude))
        Assert.assertThat(reminders[2].longitude, `is`(reminderMeetChristian.longitude))
    }

    @Test
    fun getReminderById_returnsCorrectReminder() = runBlockingTest {

        // GIVEN - insert some reminders
        database.reminderDao().saveReminder(reminderOktoberfest)
        database.reminderDao().saveReminder(reminderMtbTrailsSintra)
        database.reminderDao().saveReminder(reminderMeetChristian)

        // WHEN - Get a specific reminder from the database
        val reminder = database.reminderDao().getReminderById(reminderMtbTrailsSintra.id)

        // THEN - We got the correct reminder
        Assert.assertThat<ReminderDTO>(reminder as ReminderDTO, notNullValue())

        Assert.assertThat(reminder.id, `is`(reminderMtbTrailsSintra.id))
        Assert.assertThat(reminder.title, `is`(reminderMtbTrailsSintra.title))
        Assert.assertThat(reminder.description, `is`(reminderMtbTrailsSintra.description))
        Assert.assertThat(reminder.location, `is`(reminderMtbTrailsSintra.location))
        Assert.assertThat(reminder.latitude, `is`(reminderMtbTrailsSintra.latitude))
        Assert.assertThat(reminder.longitude, `is`(reminderMtbTrailsSintra.longitude))
    }

    @Test
    fun getReminderById_returnsNullOnWrongId() = runBlockingTest {

        // GIVEN - insert some reminders
        database.reminderDao().saveReminder(reminderOktoberfest)
        database.reminderDao().saveReminder(reminderMtbTrailsSintra)
        database.reminderDao().saveReminder(reminderMeetChristian)

        // WHEN - Get a specific reminder from the database
        val reminder = database.reminderDao().getReminderById("anyWrongId")

        // THEN - The reminder shall be null
        Assert.assertNull(reminder)
    }

    @Test
    fun deleteReminders_shallDeleteAllReminders() = runBlockingTest {

        // GIVEN - insert some reminders
        database.reminderDao().saveReminder(reminderOktoberfest)
        database.reminderDao().saveReminder(reminderMtbTrailsSintra)
        database.reminderDao().saveReminder(reminderMeetChristian)

        // WHEN - deleting all reminders
        database.reminderDao().deleteAllReminders()

        // THEN - The list is empty
        val reminders = database.reminderDao().getReminders()
        Assert.assertThat(reminders.isEmpty(), `is`(true))
    }

}