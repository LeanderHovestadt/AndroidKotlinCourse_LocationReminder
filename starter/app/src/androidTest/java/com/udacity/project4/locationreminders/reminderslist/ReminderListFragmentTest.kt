package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import android.view.View
import androidx.annotation.NonNull
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.FakeDataSource
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest :
    AutoCloseKoinTest() {

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var reminderListViewModel: RemindersListViewModel

    @Before
    fun setUp() {
        fakeDataSource = FakeDataSource()
        reminderListViewModel =
            RemindersListViewModel(getApplicationContext(), fakeDataSource)

        stopKoin()
        val myModule = module {
            single {
                reminderListViewModel
            }
        }
        // new koin module
        startKoin {
            modules(listOf(myModule))
        }
    }

    @After
    fun tearDown() {
        //stopKoin()
    }

    @Test
    fun clickAddReminderButton_navigateToSaveReminderFragment() = runBlockingTest {

        // GIVEN - on the reminder list screen
        val navController = mock(NavController::class.java)
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme) {
            ReminderListFragment().also { fragment ->
                // In addition to returning a new instance of our Fragment,
                // get a callback whenever the fragment’s view is created
                // or destroyed so that we can set the mock NavController
                fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                    if (viewLifecycleOwner != null) {
                        // The fragment’s view has just been created
                        Navigation.setViewNavController(fragment.requireView(), navController)
                    }
                }
            }
        }

        // WHEN - Click on the "+" button
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN - Verify that we navigate to the add screen
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }

//    DONE: test the navigation of the fragments.
//    DONE: test the displayed data on the UI.

    @Test
    fun onSaveReminder_recyclerViewShowsReminders() = runBlockingTest {

        // GIVEN - on the reminder list screen saving several reminders
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

        val list = mutableListOf(reminderOktoberfest, reminderMtbTrailsSintra, reminderMeetChristian)

        // WHEN - tasks are added to the data source
        fakeDataSource.setTasks(list)

        val navController = mock(NavController::class.java)
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme) {
            ReminderListFragment().also { fragment ->
                // In addition to returning a new instance of our Fragment,
                // get a callback whenever the fragment’s view is created
                // or destroyed so that we can set the mock NavController
                fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                    if (viewLifecycleOwner != null) {
                        // The fragment’s view has just been created
                        Navigation.setViewNavController(fragment.requireView(), navController)
                    }
                }
            }
        }

        // THEN - recycler view shall display them
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(0, hasDescendant(withText(reminderOktoberfest.title)))))
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(0, hasDescendant(withText(reminderOktoberfest.description)))))
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(0, hasDescendant(withText(reminderOktoberfest.location)))))

        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(1, hasDescendant(withText(reminderMtbTrailsSintra.title)))))
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(1, hasDescendant(withText(reminderMtbTrailsSintra.description)))))
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(1, hasDescendant(withText(reminderMtbTrailsSintra.location)))))

        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(2, hasDescendant(withText(reminderMeetChristian.title)))))
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(2, hasDescendant(withText(reminderMeetChristian.description)))))
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(2, hasDescendant(withText(reminderMeetChristian.location)))))
    }
//    DONE: add testing for the error messages.

    @Test
    fun onNoTasksAdded_errorSnackBackShown() = runBlockingTest {

        // GIVEN - on the reminder list screen
        val navController = mock(NavController::class.java)
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme) {
            ReminderListFragment().also { fragment ->
                // In addition to returning a new instance of our Fragment,
                // get a callback whenever the fragment’s view is created
                // or destroyed so that we can set the mock NavController
                fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                    if (viewLifecycleOwner != null) {
                        // The fragment’s view has just been created
                        Navigation.setViewNavController(fragment.requireView(), navController)
                    }
                }
            }
        }

        // WHEN - No Tasks are added

        // THEN - No reminders found is shown
        onView(withId(R.id.noDataTextView))
            .check(ViewAssertions.matches(isDisplayed()))
    }

    private fun atPosition(position: Int, @NonNull itemMatcher: Matcher<View?>): Matcher<View?>? {
        return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has item at position $position: ")
                itemMatcher.describeTo(description)
            }

            override fun matchesSafely(view: RecyclerView): Boolean {
                val viewHolder = view.findViewHolderForAdapterPosition(position)
                    ?: // has no item on such position
                    return false
                return itemMatcher.matches(viewHolder.itemView)
            }
        }
    }
}