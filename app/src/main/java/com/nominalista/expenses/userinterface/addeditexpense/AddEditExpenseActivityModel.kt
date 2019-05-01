package com.nominalista.expenses.userinterface.addeditexpense

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nominalista.expenses.data.Tag

class AddEditExpenseActivityModel : ViewModel() {

    val selectedTags = MutableLiveData<List<Tag>>()

    fun selectTags(tags: List<Tag>) {
        selectedTags.value = tags
    }
}