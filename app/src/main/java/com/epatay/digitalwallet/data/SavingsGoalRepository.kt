package com.epatay.digitalwallet.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

class SavingsGoalRepository(
    private val database: TransactionDatabase
) {

    private val savingsGoalDao =
        database.savingsGoalDao()

    val goalsWithProgress:
            Flow<List<SavingsGoalProgress>> =
        savingsGoalDao.observeGoalsWithProgress()

    fun observeEntries(
        goalId: String
    ): Flow<List<SavingsGoalEntry>> {
        return savingsGoalDao.observeEntries(goalId)
    }

    suspend fun insertGoal(
        goal: SavingsGoal
    ): Long {
        return savingsGoalDao.insertGoal(
            normalizeGoal(goal)
        )
    }

    suspend fun updateGoal(
        goal: SavingsGoal
    ) {
        require(goal.uuid.isNotEmpty()) {
            "Güncellenecek hedef bulunamadı."
        }

        savingsGoalDao.updateGoal(
            normalizeGoal(goal)
        )
    }

    suspend fun setArchived(
        goalId: String,
        isArchived: Boolean
    ) {
        // HATA DÜZELTİLDİ: goalId > 0 yerine isNotEmpty() kullanıldı
        require(goalId.isNotEmpty()) {
            "Arşivlenecek hedef bulunamadı."
        }

        savingsGoalDao.setArchived(
            goalId = goalId,
            isArchived = isArchived
        )
    }

    suspend fun deleteGoal(
        goal: SavingsGoal
    ) {
        savingsGoalDao.updateGoal(
            goal.copy(is_deleted = true, is_synced = false, updated_at = System.currentTimeMillis())
        )
    }

    suspend fun addEntry(
        entry: SavingsGoalEntry
    ): Long {
        val normalized = normalizeEntry(entry)

        return database.withTransaction {
            check(
                savingsGoalDao.getGoalById(
                    normalized.goalId
                ) != null
            ) {
                "Birikim hedefi bulunamadı."
            }

            val newBalance =
                BigDecimal.valueOf(
                    DecimalMath.normalizeMoney(
                        savingsGoalDao.getSavedAmount(
                            normalized.goalId
                        )
                    ) ?: 0.0
                ).add(
                    BigDecimal.valueOf(
                        normalized.amountDelta
                    )
                )

            require(newBalance >= BigDecimal.ZERO) {
                "Birikim bakiyesi sıfırın altına düşemez."
            }

            savingsGoalDao.insertEntry(normalized)
        }
    }

    suspend fun deleteEntry(
        entry: SavingsGoalEntry
    ) {
        database.withTransaction {
            val current =
                savingsGoalDao.getEntryById(entry.uuid)
                    ?: return@withTransaction

            val balanceAfterDelete =
                BigDecimal.valueOf(
                    DecimalMath.normalizeMoney(
                        savingsGoalDao.getSavedAmount(
                            current.goalId
                        )
                    ) ?: 0.0
                ).subtract(
                    BigDecimal.valueOf(
                        current.amountDelta
                    )
                )

            require(
                balanceAfterDelete >= BigDecimal.ZERO
            ) {
                "Bu kayıt silinirse birikim bakiyesi sıfırın altına düşer."
            }

            savingsGoalDao.updateEntry(
                current.copy(is_deleted = true, is_synced = false, updated_at = System.currentTimeMillis())
            )
        }
    }

    private fun normalizeGoal(
        goal: SavingsGoal
    ): SavingsGoal {
        val normalizedTargetAmount =
            DecimalMath.normalizeMoney(goal.targetAmount)
                ?: goal.targetAmount
        val normalized =
            goal.copy(
                title = goal.title.trim(),
                targetAmount = normalizedTargetAmount
            )

        require(normalized.title.isNotEmpty()) {
            "Hedef adı boş bırakılamaz."
        }
        require(
            normalized.targetAmount.isFinite() &&
                    normalized.targetAmount > 0.0
        ) {
            "Hedef tutarı sıfırdan büyük olmalıdır."
        }
        require(
            normalized.targetDateKey == null ||
                    TransactionDateUtils.isValidDateKey(
                        normalized.targetDateKey
                    )
        ) {
            "Geçersiz hedef tarihi."
        }

        return normalized
    }

    private fun normalizeEntry(
        entry: SavingsGoalEntry
    ): SavingsGoalEntry {
        // HATA DÜZELTİLDİ: entry.goalId > 0 yerine isNotEmpty() kullanıldı
        require(entry.goalId.isNotEmpty()) {
            "Birikim hedefi bulunamadı."
        }
        require(
            entry.amountDelta.isFinite() &&
                    entry.amountDelta != 0.0
        ) {
            "Birikim hareketi sıfır olamaz."
        }
        require(
            TransactionDateUtils.isValidDateKey(
                entry.occurredOn
            )
        ) {
            "Geçersiz birikim tarihi."
        }

        val normalizedAbsoluteAmount =
            DecimalMath.normalizeMoney(
                kotlin.math.abs(entry.amountDelta)
            )
                ?: throw IllegalArgumentException(
                    "Geçersiz birikim tutarı."
                )

        return entry.copy(
            amountDelta =
                if (entry.amountDelta < 0.0) {
                    -normalizedAbsoluteAmount
                } else {
                    normalizedAbsoluteAmount
                },
            note =
                entry.note
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
        )
    }

}