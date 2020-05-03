package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import kotlinx.coroutines.*

class SleepTrackerViewModel(val database: SleepDatabaseDao, application: Application) : AndroidViewModel(application) {

	/**
	 * viewModelJob allows us to cancel all coroutines started by this ViewModel.
	 */


	/**
	 *  We Manage the lifecycle of the coroutines by creating an instance of [CoroutineScope] tied
	 *  to the lifecycle of the activity
	 * */

	private var viewModelJob = Job() // Job() factory function

	/**
	 * Obtaining a standalone instance of a scope
	 * A [CoroutineScope] keeps track of all coroutines started by this ViewModel.
	 *
	 * Because we pass it [viewModelJob], any coroutine started in this uiScope can be cancelled
	 * by calling `viewModelJob.cancel()`
	 *
	 * By default, all coroutines started in uiScope will launch in [Dispatchers.Main] which is
	 * the main thread on Android. This is a sensible default because most coroutines started by
	 * a [ViewModel] update the UI after performing some processing.
	 */
	private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

	private var tonight = MutableLiveData<SleepNight?>()

	val nights = database.getAllNights()

	val startButtonIsVisible: LiveData<Boolean> = Transformations.map(tonight) {
		null == it
	}

	val stopButtonIsVisible: LiveData<Boolean> = Transformations.map(tonight) {
		null != it
	}

	val clearButtonIsVisible: LiveData<Boolean?> = Transformations.map(nights) {
		it?.isNotEmpty()
	}

	private val _showSnackbarEvent = MutableLiveData<Boolean>()
	val showSnackbarEvent: LiveData<Boolean>
		get() = _showSnackbarEvent

	private val _navigateToSleepDataQuality = MutableLiveData<Long>()
	val navigateToSleepDataQuality
		get() = _navigateToSleepDataQuality

	fun onSleepNightClicked(id: Long) {
		_navigateToSleepDataQuality.value = id
	}

	fun onSleepDataQualityNavigated() {
		_navigateToSleepDataQuality.value = null
	}

	/**
	 * Variable that tells the Fragment to navigate to a specific [SleepQualityFragment]
	 *
	 * This is private because we don't want to expose setting this value to the Fragment.
	 */
	private val _navigateToSleepQuality = MutableLiveData<SleepNight>()

	/**
	 * If this is non-null, immediately navigate to [SleepQualityFragment] and call [doneNavigating]
	 */
	val navigateToSleepQuality: LiveData<SleepNight>
		get() = _navigateToSleepQuality

	/**
	 * Call this immediately after navigating to [SleepQualityFragment]
	 *
	 * It will clear the navigation request, so if the user rotates their phone it won't navigate
	 * twice.
	 */
	fun doneNavigating() {
		_navigateToSleepQuality.value = null
	}

	init {
		initializeTonight()
	}

	private fun initializeTonight() {
		uiScope.launch {
			tonight.value = getTonightFromDatabase()
		}
	}

	/**
	 *  Handling the case of the stopped app or forgotten recording,
	 *  the start and end times will be the same.j
	 *
	 *  If the start time and end time are not the same, then we do not have an unfinished
	 *  recording.
	 */
	private suspend fun getTonightFromDatabase(): SleepNight? {
		return withContext(Dispatchers.IO) {
			var night = database.getTonight()
			if (night?.endTimeMilli != night?.startTimeMilli) {
				night = null
			}
			night
		}
	}

	/**
	 * Executes when the START button is clicked.
	 */
	fun onStartTracking() {
		uiScope.launch {
			// Create a new night, which captures the current time,
			// and insert it into the database.
			val night = SleepNight()
			insert(night)
			tonight.value = getTonightFromDatabase()
		}
	}

	private suspend fun insert(night: SleepNight) {
		withContext(Dispatchers.IO) {
			database.insert(night)
		}
	}

	/**
	 * Executes when the STOP button is clicked.
	 */
	fun onStopTracking() {
		uiScope.launch {
			// In Kotlin, the return@label syntax is used for specifying which function among
			// several nested ones this statement returns from.
			// In this case, we are specifying to return from launch(),
			// not the lambda.
			val oldNight = tonight.value ?: return@launch
			// Update the night in the database to add the end time.
			oldNight.endTimeMilli = System.currentTimeMillis()
			update(oldNight)

			// Set state to navigate to the SleepQualityFragment.
			_navigateToSleepQuality.value = oldNight

		}
	}

	private suspend fun update(night: SleepNight) {
		withContext(Dispatchers.IO) {
			database.update(night)
		}
	}

	/**
	 * Executes when the CLEAR button is clicked.
	 */
	fun onClear() {
		uiScope.launch {
			// Clear the database table.
			clear()
			// And clear tonight since it's no longer in the database
			tonight.value = null
		}
		_showSnackbarEvent.value = true
	}

	private suspend fun clear() {
		withContext(Dispatchers.IO) {
			database.clear()
		}
	}

	fun doneShowingSnackbar() {
		_showSnackbarEvent.value = false
	}

	/**
	 * Called when the ViewModel is dismantled.
	 * At this point, we want to cancel all coroutines;
	 * otherwise we end up with processes that have nowhere to return to
	 * using memory and resources.
	 */
	override fun onCleared() {
		super.onCleared()
		viewModelJob.cancel()
	}
}