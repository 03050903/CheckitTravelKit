package eu.szwiec.checkittravelkit.ui.search

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import eu.szwiec.checkittravelkit.R
import eu.szwiec.checkittravelkit.prefs.Preferences
import eu.szwiec.checkittravelkit.repository.CountryRepository
import eu.szwiec.checkittravelkit.util.NonNullLiveData

sealed class State {
    object ChooseOrigin : State()
    object ChooseDestination : State()
    data class ShowInfo(val countryName: String) : State()
}

class SearchViewModel(private val context: Context, private val preferences: Preferences, private val repository: CountryRepository) : ViewModel() {

    val state = MutableLiveData<State>()
    val origin = NonNullLiveData("")
    val destination = NonNullLiveData("")
    val originHint = NonNullLiveData("")
    val countryNames = repository.getCountryNames()

    fun initState() {
        if (preferences.origin.isEmpty()) {
            setState(State.ChooseOrigin)
        } else {
            setState(State.ChooseDestination)
            origin.value = preferences.origin
        }
    }

    fun setState(newState: State) {

        when (newState) {
            is State.ChooseOrigin -> {
                originHint.value = context.getString(R.string.where_are_you_from)
            }
            is State.ChooseDestination -> {
                originHint.value = ""
            }
            is State.ShowInfo -> {
                destination.value = ""
            }
        }

        state.value = newState
    }

    fun submitOrigin() {
        if (isValid(origin.value)) {
            setState(State.ChooseDestination)
            preferences.origin = origin.value
        }
    }

    fun submitDestination() {
        if (isValid(destination.value)) {
            setState(State.ShowInfo(destination.value))
        }
    }

    private fun isValid(countryName: String): Boolean {
        countryNames.value?.let {
            return it.contains(countryName)
        }
        return false
    }
}