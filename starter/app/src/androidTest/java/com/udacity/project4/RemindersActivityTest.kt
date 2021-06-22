package com.udacity.project4

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.EspressoIdlingResource
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.not
import org.hamcrest.core.IsNot
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var reminderOktoberfest : ReminderDTO
    private lateinit var reminderMtbTrailsSintra : ReminderDTO
    private lateinit var reminderMeetChristian : ReminderDTO
    // An Idling Resource that waits for Data Binding to have no pending bindings
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Rule
    @JvmField
    var mActivityRule = ActivityTestRule(RemindersActivity::class.java)

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
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

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

//    DONE: add End to End testing to the app
    @ExperimentalCoroutinesApi
    @Test
    fun endToEnd_saveReminderWillDisplayReminder() = runBlocking {

        // Start up Tasks screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // click on add new reminder button
        Espresso.onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

        // click on save button
        Espresso.onView(withId(R.id.save_button)).perform(ViewActions.click())

        // expect Toast is shown
        onView(withText("Please select all fields first")).inRoot(withDecorView(not( mActivityRule.activity.window.decorView))).check(matches(isDisplayed()))

        Espresso.onView(withId(R.id.reminderTitle)).perform(
            ViewActions.typeText("TITLE1"),
            ViewActions.closeSoftKeyboard()
        )
        Espresso.onView(withId(R.id.reminderDescription)).perform(
            ViewActions.typeText("DESCRIPTION1"),
            ViewActions.closeSoftKeyboard()
        )

        // click on save button
        Espresso.onView(withId(R.id.save_button)).perform(ViewActions.click())

        // expect Toast is shown
        onView(withText("Please select all fields first")).inRoot(withDecorView(not( mActivityRule.activity.window.decorView))).check(matches(isDisplayed()))

        // TODO select POI in Google Maps

        // click on save button
        Espresso.onView(withId(R.id.save_button)).perform(ViewActions.click())

        Espresso.onView(withId(R.id.title))
            .check(ViewAssertions.matches(ViewMatchers.withText(reminderOktoberfest.title)))
        Espresso.onView(withId(R.id.description))
            .check(ViewAssertions.matches(ViewMatchers.withText(reminderOktoberfest.description)))
        Espresso.onView(withId(R.id.location))
            .check(ViewAssertions.matches(ViewMatchers.withText(reminderOktoberfest.location)))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }

}
