package com.example.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.dao.BoxWithCounts
import com.example.data.local.entity.BoxEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ItemEntity
import com.example.data.local.entity.SearchHistoryEntity
import com.example.data.repository.CategoryStat
import com.example.data.repository.InventoryRepository
import com.example.domain.usecase.MoveBoxResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class DeleteTarget {
    data class Box(val box: BoxEntity, val subBoxesCount: Int, val itemsCount: Int) : DeleteTarget()
    data class Item(val item: ItemEntity) : DeleteTarget()
}

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InventoryRepository(AppDatabase.getDatabase(application))

    // Layout view mode (Grid vs List)
    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Navigation state: null = Root ("Mis Cajas")
    private val _currentBoxId = MutableStateFlow<String?>(null)
    val currentBoxId: StateFlow<String?> = _currentBoxId.asStateFlow()

    // Current box entity if inside a box
    private val _currentBox = MutableStateFlow<BoxEntity?>(null)
    val currentBox: StateFlow<BoxEntity?> = _currentBox.asStateFlow()

    // Breadcrumbs path
    private val _breadcrumbs = MutableStateFlow<List<BoxEntity>>(emptyList())
    val breadcrumbs: StateFlow<List<BoxEntity>> = _breadcrumbs.asStateFlow()

    // Search state
    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    // Dialog & Feedback state
    private val _boxToEdit = MutableStateFlow<BoxEntity?>(null)
    val boxToEdit: StateFlow<BoxEntity?> = _boxToEdit.asStateFlow()
    val isBoxDialogOpen = MutableStateFlow(false)

    private val _itemToEdit = MutableStateFlow<ItemEntity?>(null)
    val itemToEdit: StateFlow<ItemEntity?> = _itemToEdit.asStateFlow()
    val isItemDialogOpen = MutableStateFlow(false)

    private val _boxToMove = MutableStateFlow<BoxEntity?>(null)
    val boxToMove: StateFlow<BoxEntity?> = _boxToMove.asStateFlow()

    private val _itemToMove = MutableStateFlow<ItemEntity?>(null)
    val itemToMove: StateFlow<ItemEntity?> = _itemToMove.asStateFlow()

    // Loans and Stats
    private val _itemToLoan = MutableStateFlow<ItemEntity?>(null)
    val itemToLoan: StateFlow<ItemEntity?> = _itemToLoan.asStateFlow()

    val isStatsDialogOpen = MutableStateFlow(false)
    val isLoansSheetOpen = MutableStateFlow(false)
    val isBackupDialogOpen = MutableStateFlow(false)

    private val _categoryStats = MutableStateFlow<List<CategoryStat>>(emptyList())
    val categoryStats: StateFlow<List<CategoryStat>> = _categoryStats.asStateFlow()

    private val _deleteTarget = MutableStateFlow<DeleteTarget?>(null)
    val deleteTarget: StateFlow<DeleteTarget?> = _deleteTarget.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Dynamic queries based on currentBoxId
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentBoxesWithCounts: StateFlow<List<BoxWithCounts>> = _currentBoxId
        .flatMapLatest { boxId ->
            if (boxId == null) {
                repository.getRootBoxesWithCounts()
            } else {
                repository.getChildBoxesWithCounts(boxId)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentItems: StateFlow<List<ItemEntity>> = _currentBoxId
        .flatMapLatest { boxId ->
            if (boxId == null) {
                repository.searchItems("")
            } else {
                repository.getItemsInBox(boxId)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBoxes: StateFlow<List<BoxEntity>> = repository.getAllBoxes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loanedItems: StateFlow<List<ItemEntity>> = repository.getLoanedItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBoxesCount: StateFlow<Int> = repository.getTotalBoxesCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalItemsCount: StateFlow<Int> = repository.getTotalItemsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalItemUnitsCount: StateFlow<Int?> = repository.getTotalItemUnitsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalLoanedCount: StateFlow<Int> = repository.getTotalLoanedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResultsItems: StateFlow<List<ItemEntity>> = combine(_searchQuery, _selectedCategoryFilter) { q, cat ->
        Pair(q, cat)
    }.flatMapLatest { (q, cat) ->
        repository.searchItems(q, cat)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResultsBoxes: StateFlow<List<BoxEntity>> = _searchQuery
        .flatMapLatest { q -> repository.searchBoxes(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSearches: StateFlow<List<SearchHistoryEntity>> = repository.getRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allZones: StateFlow<List<String>> = repository.getAllZones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            refreshStats()
        }
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun openBox(boxId: String) {
        viewModelScope.launch {
            _currentBoxId.value = boxId
            _currentBox.value = repository.getBoxById(boxId)
            _breadcrumbs.value = repository.getBreadcrumbs(boxId)
            _isSearchActive.value = false
        }
    }

    fun navigateBack(): Boolean {
        if (_isSearchActive.value) {
            _isSearchActive.value = false
            return true
        }

        val currentCrumbs = _breadcrumbs.value
        return if (currentCrumbs.size > 1) {
            val parentBox = currentCrumbs[currentCrumbs.size - 2]
            openBox(parentBox.id)
            true
        } else if (currentCrumbs.size == 1) {
            navigateToRoot()
            true
        } else {
            false
        }
    }

    fun navigateToRoot() {
        _currentBoxId.value = null
        _currentBox.value = null
        _breadcrumbs.value = emptyList()
        _isSearchActive.value = false
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun recordSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                repository.recordSearch(trimmed)
            }
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            repository.deleteSearch(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun setCategoryFilter(categoryId: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == categoryId) null else categoryId
    }

    // Dialog triggers
    fun openCreateBoxDialog() {
        _boxToEdit.value = null
        isBoxDialogOpen.value = true
    }

    fun openEditBoxDialog(box: BoxEntity) {
        _boxToEdit.value = box
        isBoxDialogOpen.value = true
    }

    fun dismissBoxDialog() {
        isBoxDialogOpen.value = false
        _boxToEdit.value = null
    }

    fun openCreateItemDialog() {
        _itemToEdit.value = null
        isItemDialogOpen.value = true
    }

    fun openEditItemDialog(item: ItemEntity) {
        _itemToEdit.value = item
        isItemDialogOpen.value = true
    }

    fun dismissItemDialog() {
        isItemDialogOpen.value = false
        _itemToEdit.value = null
    }

    fun openMoveBoxDialog(box: BoxEntity) {
        _boxToMove.value = box
    }

    fun dismissMoveBoxDialog() {
        _boxToMove.value = null
    }

    fun openMoveItemDialog(item: ItemEntity) {
        _itemToMove.value = item
    }

    fun dismissMoveItemDialog() {
        _itemToMove.value = null
    }

    fun openLoanDialog(item: ItemEntity) {
        _itemToLoan.value = item
    }

    fun dismissLoanDialog() {
        _itemToLoan.value = null
    }

    fun openStatsDialog() {
        viewModelScope.launch {
            refreshStats()
            isStatsDialogOpen.value = true
        }
    }

    fun dismissStatsDialog() {
        isStatsDialogOpen.value = false
    }

    fun openLoansSheet() {
        isLoansSheetOpen.value = true
    }

    fun dismissLoansSheet() {
        isLoansSheetOpen.value = false
    }

    fun openBackupDialog() {
        isBackupDialogOpen.value = true
    }

    fun dismissBackupDialog() {
        isBackupDialogOpen.value = false
    }

    fun requestDeleteBox(boxWithCounts: BoxWithCounts) {
        _deleteTarget.value = DeleteTarget.Box(
            box = boxWithCounts.box,
            subBoxesCount = boxWithCounts.childBoxesCount,
            itemsCount = boxWithCounts.itemsCount
        )
    }

    fun requestDeleteItem(item: ItemEntity) {
        _deleteTarget.value = DeleteTarget.Item(item)
    }

    fun dismissDeleteDialog() {
        _deleteTarget.value = null
    }

    // CRUD actions
    fun saveBox(name: String, description: String?, colorHex: String, photoUri: String?, zone: String? = null) {
        viewModelScope.launch {
            val editing = _boxToEdit.value
            if (editing != null) {
                repository.updateBox(
                    editing.copy(
                        name = name.trim(),
                        description = description?.takeIf { it.isNotBlank() }?.trim(),
                        colorHex = colorHex,
                        primaryPhotoUri = photoUri ?: editing.primaryPhotoUri,
                        zone = zone?.takeIf { it.isNotBlank() }?.trim()
                    )
                )
                _snackbarMessage.value = "Caja actualizada: $name"
                if (_currentBoxId.value == editing.id) {
                    _currentBox.value = repository.getBoxById(editing.id)
                }
            } else {
                repository.createBox(
                    name = name,
                    description = description,
                    parentBoxId = _currentBoxId.value,
                    primaryPhotoUri = photoUri,
                    colorHex = colorHex,
                    zone = zone?.takeIf { it.isNotBlank() }?.trim()
                )
                _snackbarMessage.value = "Caja creada: $name"
            }
            dismissBoxDialog()
            refreshStats()
        }
    }

    fun createZone(name: String, colorHex: String = "#F59E0B") {
        viewModelScope.launch {
            repository.createZone(name, colorHex)
            _snackbarMessage.value = "Zona creada: $name"
        }
    }

    fun renameZone(oldName: String, newName: String) {
        viewModelScope.launch {
            repository.renameZone(oldName, newName)
            _snackbarMessage.value = "Zona renombrada a: $newName"
        }
    }

    fun deleteZone(name: String) {
        viewModelScope.launch {
            repository.deleteZone(name)
            _snackbarMessage.value = "Zona eliminada: $name"
        }
    }

    fun saveItem(
        name: String,
        description: String?,
        quantity: Int,
        categoryId: String?,
        tags: String,
        photoUri: String?,
        targetBoxId: String? = null
    ) {
        viewModelScope.launch {
            val currentBox = _currentBoxId.value ?: targetBoxId
            if (currentBox == null) {
                _snackbarMessage.value = "Selecciona una caja para almacenar el objeto."
                return@launch
            }

            val editing = _itemToEdit.value
            if (editing != null) {
                repository.updateItem(
                    editing.copy(
                        name = name.trim(),
                        description = description?.takeIf { it.isNotBlank() }?.trim(),
                        quantity = quantity.coerceAtLeast(1),
                        categoryId = categoryId,
                        tags = tags.trim(),
                        primaryPhotoUri = photoUri ?: editing.primaryPhotoUri
                    )
                )
                _snackbarMessage.value = "Objeto actualizado: $name"
            } else {
                repository.createItem(
                    name = name,
                    description = description,
                    boxId = currentBox,
                    categoryId = categoryId,
                    primaryPhotoUri = photoUri,
                    quantity = quantity,
                    tags = tags
                )
                _snackbarMessage.value = "Objeto guardado: $name"
            }
            dismissItemDialog()
            refreshStats()
        }
    }

    fun confirmDelete() {
        val target = _deleteTarget.value ?: return
        viewModelScope.launch {
            when (target) {
                is DeleteTarget.Box -> {
                    repository.deleteBox(target.box.id)
                    _snackbarMessage.value = "Caja eliminada: ${target.box.name}"
                    if (_currentBoxId.value == target.box.id) {
                        navigateBack()
                    }
                }
                is DeleteTarget.Item -> {
                    repository.deleteItem(target.item.id)
                    _snackbarMessage.value = "Objeto eliminado: ${target.item.name}"
                }
            }
            dismissDeleteDialog()
            refreshStats()
        }
    }

    fun executeMoveBox(boxId: String, targetParentBoxId: String?) {
        viewModelScope.launch {
            when (val result = repository.moveBox(boxId, targetParentBoxId)) {
                is MoveBoxResult.Success -> {
                    _snackbarMessage.value = "Caja movida correctamente."
                    dismissMoveBoxDialog()
                    _currentBoxId.value?.let { currentId ->
                        _breadcrumbs.value = repository.getBreadcrumbs(currentId)
                    }
                }
                is MoveBoxResult.Error -> {
                    _snackbarMessage.value = result.message
                }
            }
        }
    }

    fun canMoveBoxTo(boxId: String, targetParentBoxId: String?): Pair<Boolean, String?> {
        return repository.canMoveBox(boxId, targetParentBoxId, allBoxes.value)
    }

    fun executeMoveItem(itemId: String, targetBoxId: String) {
        viewModelScope.launch {
            repository.moveItem(itemId, targetBoxId)
            _snackbarMessage.value = "Objeto reubicado con éxito."
            dismissMoveItemDialog()
        }
    }

    fun adjustItemQuantity(item: ItemEntity, delta: Int) {
        viewModelScope.launch {
            val newQty = item.quantity + delta
            repository.updateItemQuantity(item.id, newQty)
            refreshStats()
        }
    }

    // Loans
    fun confirmLoan(item: ItemEntity, loanedTo: String, notes: String?) {
        viewModelScope.launch {
            repository.loanItem(item.id, loanedTo, notes)
            _snackbarMessage.value = "«${item.name}» prestado a $loanedTo"
            dismissLoanDialog()
            refreshStats()
        }
    }

    fun returnItem(item: ItemEntity) {
        viewModelScope.launch {
            repository.returnLoanedItem(item.id)
            _snackbarMessage.value = "«${item.name}» devuelto a su caja."
            refreshStats()
        }
    }

    // Backup
    suspend fun getExportJsonString(): String = repository.exportJson()

    suspend fun getExportCsvString(): String = repository.exportCsv()

    fun importBackup(content: String) {
        viewModelScope.launch {
            val result = repository.importJson(content)
            _snackbarMessage.value = result.message
            if (result.success) {
                refreshStats()
                dismissBackupDialog()
            }
        }
    }

    private suspend fun refreshStats() {
        _categoryStats.value = repository.getCategoryStats()
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    suspend fun getBoxBreadcrumbPath(boxId: String): String {
        val crumbs = repository.getBreadcrumbs(boxId)
        return if (crumbs.isEmpty()) "Raíz" else crumbs.joinToString(" > ") { it.name }
    }
}
