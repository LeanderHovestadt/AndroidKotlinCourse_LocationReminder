package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers
import org.hamcrest.core.IsNot
import org.junit.*
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.R])
class RemindersListViewModelTest {

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var reminderListViewModel: RemindersListViewModel

    //TODO: provide testing to the RemindersListViewModel and its live data objects
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setUp(){
        fakeDataSource = FakeDataSource()
        reminderListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun loadReminders_returnsCorrectValues() {
        val reminderOktoberfest = ReminderDTO(
            "Oktoberfest reminder",
            "Go to the Oktoberfest!",
            "munich",
            48.137154,
            11.576124
        )
        val reminderMtbTrailsSintra = ReminderDTO(
            "Sintra trail reminder",
            "Oh my god, you need to ride these trails!",
            "sintra",
            38.787777,
            -9.390556
        )
        val reminderMeetChristian = ReminderDTO(
            "Siegen meet Christian Textor",
            "Do not forget to drink a beer with the national champ Christian Textor!",
            "siegen",
            50.883331,
            8.016667
        )

        val remindersList = mutableListOf(reminderOktoberfest, reminderMtbTrailsSintra, reminderMeetChristian)
        fakeDataSource.setTasks(remindersList)

        reminderListViewModel.loadReminders()
        Assert.assertThat(
            reminderListViewModel.remindersList.getOrAwaitValue(),
            (IsNot.not(emptyList()))
        )
        Assert.assertThat(
            reminderListViewModel.remindersList.getOrAwaitValue().size,
            CoreMatchers.`is`(remindersList.size)
        )
    }

    @Test
    fun loadReminders_showLoadingInInitialState() {
        mainCoroutineRule.pauseDispatcher()
        reminderListViewModel.loadReminders()
        Assert.assertThat(
            reminderListViewModel.showLoading.getOrAwaitValue(),
            CoreMatchers.`is`(true)
        )
    }

    @Test
    fun loadReminders_snackBarShowsCorrectErrorMessage() {
        fakeDataSource.setReturnErrors(true)
        reminderListViewModel.loadReminders()
        Assert.assertThat(
            reminderListViewModel.showSnackBar.getOrAwaitValue(),
            CoreMatchers.`is`("No reminders found")
        )
    }

}