package com.example.mymealsuggestionapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.mymealsuggestionapp.api.openAIChatService
import com.example.mymealsuggestionapp.database.MealDatabase
import com.example.mymealsuggestionapp.database.MealRepositoryImpl
import com.example.mymealsuggestionapp.model.Ingredient
import com.example.mymealsuggestionapp.model.MealSuggestion
import com.example.mymealsuggestionapp.database.MealsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(private val repository: MealsRepository) : ViewModel() {

    private val _currentIngredients = MutableStateFlow<MutableList<Ingredient>>(ArrayList())
    private val _currentMealSuggestions = MutableStateFlow<List<MealSuggestion>>(ArrayList())
    private val _savedMealSuggestions = MutableStateFlow<List<MealSuggestion>>(ArrayList())

    val currentIngredients = _currentIngredients.asStateFlow()
    val currentMealSuggestions = _currentMealSuggestions.asStateFlow()
    val savedMealSuggestions = _savedMealSuggestions.asStateFlow()

    fun addIngredient(ingredient: Ingredient) {
        val list: MutableList<Ingredient> = ArrayList()
        list.addAll(_currentIngredients.value)
        list.add(ingredient)
        _currentIngredients.value = list
    }

    fun removeIngredient(ingredient: Ingredient) {
        val list: MutableList<Ingredient> = ArrayList()
        list.addAll(_currentIngredients.value)
        list.remove(ingredient)
        _currentIngredients.value = list
    }

    fun getSavedMealSuggestions() = viewModelScope.launch(Dispatchers.IO) {
        repository.getSavedMealSuggestions().collectLatest {
            _savedMealSuggestions.value = it
        }
    }

    fun getMealSuggestions() {
        // Old Dummy Data generation
        /*val mealSuggestions: MutableList<MealSuggestion> = ArrayList()
        for (i in 1..10) {
            mealSuggestions.add(
                MealSuggestion(
                    id = i,
                    name = "Meal $i",
                    ingredients = arrayOf("ingredient 1", "ingredient 2", "ingredient 3"),
                    preparationInstructions = arrayOf(
                        "preparationInstruction 1",
                        "preparationInstruction 2",
                        "preparationInstruction 3"
                    ),
                    isFavourite = false
                )
            )
        }*/

        // Run OpenAI Chat Service, and get the generated list of MealSuggestion objects
        viewModelScope.launch(Dispatchers.IO) {
            openAIChatService(_currentMealSuggestions)
        }
    }

    fun addToSavedMeals(mealSuggestion: MealSuggestion) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMeal(mealSuggestion)
        }

    fun removeFromSavedMeals(mealSuggestion: MealSuggestion) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMeal(mealSuggestion)
        }

    class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(
                    MealRepositoryImpl(MealDatabase.getInstance(context).mealDao)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}