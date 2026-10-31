package com.example.gymlog.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.data.ExerciseCatalogQuery
import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseMediaRepository
import com.example.gymlog.data.ExerciseMediaView
import com.example.gymlog.data.ExerciseRepository
import com.example.gymlog.data.MuscleLink
import com.example.gymlog.data.WeightMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExerciseLibraryUiState(
    val query: String = "",
    val bodyPart: ExerciseClassification.BodyCategory = ExerciseClassification.ALL_BODY,
    val equipmentType: ExerciseClassification.EquipmentCategory = ExerciseClassification.ALL_EQUIPMENT,
    val searchingAll: Boolean = false,
    val exercises: List<ExerciseEntity> = emptyList(),
    val media: Map<String, ExerciseMediaView> = emptyMap(),
    val storedLinks: List<MuscleLink> = emptyList(),
    val isAdding: Boolean = false,
    val errorMessage: String? = null,
    val ready: Boolean = false,
)

class ExerciseLibraryViewModel(
    private val repository: ExerciseRepository,
    private val mediaRepository: ExerciseMediaRepository,
) : ViewModel() {
    private val catalog = MutableStateFlow(ExerciseCatalogQuery())
    private val isAdding = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ExerciseLibraryUiState> = catalog.flatMapLatest { selection ->
        val args = selection.repositoryArguments()
        repository.observeExercises(
            args.query,
            args.bodyCategoryId,
            args.equipmentCategoryId,
        ).combine(
            repository.observeMuscleLinks(),
        ) { exercises, links ->
            exercises to links
        }.combine(mediaRepository.observeMedia()) { (exercises, links), media ->
            ExerciseLibraryUiState(
                query = selection.text,
                bodyPart = selection.bodyPart,
                equipmentType = selection.equipment,
                searchingAll = selection.searchingAll,
                exercises = exercises,
                media = media,
                storedLinks = links,
                ready = true,
            )
        }.combine(
            combine(isAdding, errorMessage) { adding, error -> adding to error },
        ) { state, (adding, error) ->
            state.copy(isAdding = adding, errorMessage = error)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExerciseLibraryUiState(),
    )

    fun onQueryChange(value: String) {
        catalog.value = catalog.value.copy(text = value)
        errorMessage.value = null
    }

    fun onBodyPartSelected(value: ExerciseClassification.BodyCategory) {
        catalog.value = catalog.value.copy(bodyPart = value)
    }

    fun onEquipmentSelected(value: ExerciseClassification.EquipmentCategory) {
        catalog.value = catalog.value.copy(equipment = value, refinement = null)
    }

    fun addCustomExercise(
        name: String,
        bodyCategory: ExerciseClassification.BodyCategory,
        equipmentCategory: ExerciseClassification.EquipmentCategory,
        weightMode: WeightMode,
        aliases: String,
        muscles: List<MuscleLink> = emptyList(),
    ) {
        viewModelScope.launch {
            isAdding.value = true
            val result = repository.addCustomExercise(
                name = name,
                bodyCategory = bodyCategory,
                equipmentCategory = equipmentCategory,
                weightMode = weightMode,
                aliases = aliases,
                muscles = muscles,
            )
            isAdding.value = false
            errorMessage.value = result.exceptionOrNull()?.let { error ->
                when (error.message) {
                    "EMPTY_NAME" -> "请填写动作名称"
                    "DUPLICATE_NAME" -> "已有同名动作，请换一个名称"
                    "INVALID_CATEGORY" -> "请选择有效分类"
                    else -> "保存失败，请重试"
                }
            }
        }
    }

    fun replaceCustomMuscles(exerciseId: String, muscles: List<MuscleLink>) {
        viewModelScope.launch {
            repository.replaceCustomMuscles(exerciseId, muscles)
        }
    }

    fun deleteExercise(id: String) {
        viewModelScope.launch {
            repository.deleteExercise(id)
        }
    }

    /** 收藏 / 取消收藏：isFavorite 与 priority（10/0）由仓库层同一条 UPDATE 联动。 */
    fun setFavorite(id: String, favorite: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(id, favorite)
        }
    }

    fun clearError() {
        errorMessage.value = null
    }

    companion object {
        fun factory(
            repository: ExerciseRepository,
            mediaRepository: ExerciseMediaRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ExerciseLibraryViewModel(repository, mediaRepository) as T
                }
            }
        }
    }
}
