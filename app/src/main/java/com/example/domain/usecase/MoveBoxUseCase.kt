package com.example.domain.usecase

import com.example.data.local.dao.BoxDao
import com.example.data.local.entity.BoxEntity

sealed class MoveBoxResult {
    data object Success : MoveBoxResult()
    data class Error(val message: String) : MoveBoxResult()
}

class MoveBoxUseCase(private val boxDao: BoxDao) {

    /**
     * Checks whether moving [boxId] to [targetParentBoxId] is valid without creating a cycle.
     */
    fun isValidMove(boxId: String, targetParentBoxId: String?, allBoxes: List<BoxEntity>): Pair<Boolean, String?> {
        if (targetParentBoxId == null) {
            return Pair(true, null)
        }

        if (boxId == targetParentBoxId) {
            return Pair(false, "No puedes mover una caja dentro de sí misma.")
        }

        val boxMap = allBoxes.associateBy { it.id }
        var currentParentId: String? = targetParentBoxId

        while (currentParentId != null) {
            if (currentParentId == boxId) {
                return Pair(
                    false,
                    "Operación inválida: la caja destino es una subcaja de esta caja (crearía un ciclo jerárquico)."
                )
            }
            currentParentId = boxMap[currentParentId]?.parentBoxId
        }

        return Pair(true, null)
    }

    suspend fun execute(boxId: String, targetParentBoxId: String?): MoveBoxResult {
        val sourceBox = boxDao.getBoxById(boxId)
            ?: return MoveBoxResult.Error("La caja que intentas mover no existe.")

        // Moving to root
        if (targetParentBoxId == null) {
            if (sourceBox.parentBoxId == null) {
                return MoveBoxResult.Success // already at root
            }
            boxDao.updateBox(sourceBox.copy(parentBoxId = null))
            return MoveBoxResult.Success
        }

        // Cannot move into itself
        if (boxId == targetParentBoxId) {
            return MoveBoxResult.Error("No se puede mover una caja dentro de sí misma.")
        }

        // Validate target exists
        val targetBox = boxDao.getBoxById(targetParentBoxId)
            ?: return MoveBoxResult.Error("La caja destino no existe.")

        // Check for cycles
        val allBoxes = boxDao.getAllBoxesSync()
        val (isValid, errorMessage) = isValidMove(boxId, targetParentBoxId, allBoxes)
        if (!isValid) {
            return MoveBoxResult.Error(errorMessage ?: "Movimiento no permitido.")
        }

        boxDao.updateBox(sourceBox.copy(parentBoxId = targetParentBoxId))
        return MoveBoxResult.Success
    }
}
