package com.nominalista.expenses.userinterface.addeditexpense.tagselection

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nominalista.expenses.Application
import com.nominalista.expenses.data.Tag
import com.nominalista.expenses.data.database.DatabaseDataSource
import com.nominalista.expenses.infrastructure.extensions.plusAssign
import com.nominalista.expenses.infrastructure.utils.DataEvent
import com.nominalista.expenses.infrastructure.utils.Event
import com.nominalista.expenses.infrastructure.utils.SchedulerTransformer
import com.nominalista.expenses.infrastructure.utils.Variable
import io.reactivex.disposables.CompositeDisposable

class TagSelectionFragmentModel(private val databaseDataSource: DatabaseDataSource) : ViewModel() {

    val itemModels = Variable(emptyList<TagSelectionItemModel>())
    val showNewTagDialog = Event()
    val delegateSelectedTags = DataEvent<List<Tag>>()
    val finish = Event()

    private val checkedTags: MutableSet<Tag> = HashSet()

    private val disposables = CompositeDisposable()

    // Lifecycle start

    init {
        observeTags()
    }

    private fun observeTags() {
        disposables += databaseDataSource.observeTags()
            .compose(SchedulerTransformer())
            .subscribe({ tags ->
                Log.d(TAG, "Tag observation updated.")
                updateItemModels(tags)
            }, { error ->
                Log.d(TAG, "Tag observation failed (${error.message}).")
            })
    }

    private fun updateItemModels(tags: List<Tag>) {
        itemModels.value = tags
            .sortedBy { it.name }
            .let { createTagSection(it) + createAddTagSection() }
    }

    private fun createTagSection(tags: List<Tag>) = tags.map { createTagItemModel(it) }

    private fun createTagItemModel(tag: Tag): TagItemModel {
        val itemModel = TagItemModel(tag)
        itemModel.checkClick = { checkTag(itemModel) }
        itemModel.deleteClick = { deleteTag(itemModel) }
        return itemModel
    }

    private fun checkTag(itemModel: TagItemModel) {
        if (itemModel.isChecked) {
            itemModel.isChecked = false
            checkedTags.remove(itemModel.tag)
        } else {
            itemModel.isChecked = true
            checkedTags.add(itemModel.tag)
        }
    }

    private fun deleteTag(itemModel: TagItemModel) {
        val tag = itemModel.tag

        checkedTags.remove(tag)

        disposables += databaseDataSource.deleteTag(tag)
            .compose(SchedulerTransformer<Any>())
            .subscribe({
                Log.d(TAG, "Tag #${tag.id} deletion succeeded.")
            }, { error ->
                Log.d(TAG, "Tag #${tag.id} deletion failed (${error.message}..")
            })
    }

    private fun createAddTagSection() = listOf(createAddTagItemModel())

    private fun createAddTagItemModel(): AddTagItemModel {
        val itemModel = AddTagItemModel()
        itemModel.click = { showNewTagDialog.next() }
        return itemModel
    }

    // Lifecycle end

    override fun onCleared() {
        super.onCleared()
        clearDisposables()
    }

    private fun clearDisposables() {
        disposables.clear()
    }

    // Public

    fun createTag(tag: Tag) {
        disposables += databaseDataSource.insertTag(tag)
            .compose(SchedulerTransformer())
            .subscribe({ id ->
                Log.d(TAG, "Tag insertion succeeded. Id: $id.")
            }, { error ->
                Log.d(TAG, "Tag insertion failed (${error.localizedMessage}).")
            })
    }

    fun confirm() {
        delegateSelectedTags.next(checkedTags.toList())
        finish.next()
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val application: Application) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val databaseDataSource = DatabaseDataSource(application.database)
            return TagSelectionFragmentModel(databaseDataSource) as T
        }
    }

    companion object {
        private val TAG = TagSelectionFragmentModel::class.java.simpleName
    }
}