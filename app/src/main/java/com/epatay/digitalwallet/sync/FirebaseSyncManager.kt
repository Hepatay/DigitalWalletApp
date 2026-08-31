package com.epatay.digitalwallet.sync

import android.content.Context
import android.util.Log
import com.epatay.digitalwallet.data.CategoryBudget
import com.epatay.digitalwallet.data.InvestmentItem
import com.epatay.digitalwallet.data.RecurringOccurrence
import com.epatay.digitalwallet.data.RecurringTransaction
import com.epatay.digitalwallet.data.SavingsGoal
import com.epatay.digitalwallet.data.SavingsGoalEntry
import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionDatabase
import com.epatay.digitalwallet.data.UserGoldAssetEntity
import com.epatay.digitalwallet.security.ZeroKnowledgeCrypto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await

/**
 * Sıfır Bilgi (Zero-Knowledge) Uçtan Uca Şifreli Firebase Senkronizasyon Yöneticisi.
 *
 * Tüm kullanıcı verileri Firebase'e yazılmadan önce cihazda AES-256-GCM ile şifrelenir.
 * Bulutta yalnızca anlamsız şifreli metin (ciphertext) saklanır; geliştirici veya
 * Firebase veritabanı yöneticisi kullanıcı verilerini kesinlikle görüntüleyemez.
 */
class FirebaseSyncManager(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val localDb = TransactionDatabase.getDatabase(context)
    private val gson = Gson()

    /**
     * Misafir kullanıcı (guest) olarak kaydedilmiş verileri,
     * yeni giriş yapan Firebase kullanıcısının hesabına aktarır.
     */
    suspend fun assignGuestDataToUser() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val now = System.currentTimeMillis()

        Log.d(TAG, "Assigning guest records to user: $uid")

        try {
            localDb.transactionDao().assignUserToGuestRecords(uid, now)
            localDb.categoryBudgetDao().assignUserToGuestRecords(uid, now)
            localDb.recurringTransactionDao().assignUserToGuestRecords(uid, now)
            localDb.recurringOccurrenceDao().assignUserToGuestRecords(uid, now)
            localDb.savingsGoalDao().assignUserToGuestRecords(uid, now)
            localDb.savingsGoalDao().assignUserToGuestEntries(uid, now)
            localDb.investmentDao().assignUserToGuestRecords(uid, now)
            localDb.userGoldAssetDao().assignUserToGuestRecords(uid, now)

            Log.d(TAG, "Successfully assigned guest data to user $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Error assigning guest data", e)
        }
    }

    /**
     * Firebase'den şifreli verileri çeker, istemci tarafında çözer ve yerel veritabanına ekler.
     */
    suspend fun pullDataFromFirebase(uid: String) {
        Log.d(TAG, "Pulling encrypted data from Firebase for user: $uid")
        try {
            // TRANSACTIONS
            val txSnapshot = db.collection(COL_TRANSACTIONS).whereEqualTo("user_id", uid).get().await()
            for (doc in txSnapshot.documents) {
                val tx = parseDocument<Transaction>(doc, uid, Transaction::class.java)
                if (tx != null) {
                    val existing = localDb.transactionDao().getAllTransactionsSync().find { it.uuid == tx.uuid }
                    if (existing == null) {
                        localDb.transactionDao().insertTransaction(tx.copy(is_synced = true))
                    } else if (tx.updated_at > existing.updated_at) {
                        localDb.transactionDao().updateTransaction(tx.copy(is_synced = true))
                    }
                }
            }

            // INVESTMENTS (CURRENCY)
            val invSnapshot = db.collection(COL_INVESTMENTS).whereEqualTo("user_id", uid).get().await()
            for (doc in invSnapshot.documents) {
                val inv = parseDocument<InvestmentItem>(doc, uid, InvestmentItem::class.java)
                if (inv != null) {
                    val existing = localDb.investmentDao().getAllInvestmentsSync().find { it.uuid == inv.uuid }
                    if (existing == null) {
                        localDb.investmentDao().insertInvestment(inv.copy(is_synced = true))
                    } else if (inv.updated_at > existing.updated_at) {
                        localDb.investmentDao().updateInvestment(inv.copy(is_synced = true))
                    }
                }
            }

            // USER GOLD ASSETS
            val goldSnapshot = db.collection(COL_GOLD_ASSETS).whereEqualTo("user_id", uid).get().await()
            for (doc in goldSnapshot.documents) {
                val gold = parseDocument<UserGoldAssetEntity>(doc, uid, UserGoldAssetEntity::class.java)
                if (gold != null) {
                    val existing = localDb.userGoldAssetDao().getAllSync().find { it.uuid == gold.uuid }
                    if (existing == null) {
                        localDb.userGoldAssetDao().insert(gold.copy(is_synced = true))
                    } else if (gold.updated_at > existing.updated_at) {
                        localDb.userGoldAssetDao().update(gold.copy(is_synced = true))
                    }
                }
            }

            // CATEGORY BUDGETS
            val budgetSnapshot = db.collection(COL_BUDGETS).whereEqualTo("user_id", uid).get().await()
            for (doc in budgetSnapshot.documents) {
                val budget = parseDocument<CategoryBudget>(doc, uid, CategoryBudget::class.java)
                if (budget != null) {
                    val existing = localDb.categoryBudgetDao().getAllSync().find {
                        it.monthKey == budget.monthKey && it.category == budget.category
                    }
                    if (existing == null) {
                        localDb.categoryBudgetDao().upsert(budget.copy(is_synced = true))
                    } else if (budget.updated_at > existing.updated_at) {
                        localDb.categoryBudgetDao().upsert(budget.copy(is_synced = true))
                    }
                }
            }

            // SAVINGS GOALS
            val goalsSnapshot = db.collection(COL_GOALS).whereEqualTo("user_id", uid).get().await()
            for (doc in goalsSnapshot.documents) {
                val goal = parseDocument<SavingsGoal>(doc, uid, SavingsGoal::class.java)
                if (goal != null) {
                    val existing = localDb.savingsGoalDao().getAllGoalsSync().find { it.uuid == goal.uuid }
                    if (existing == null) {
                        localDb.savingsGoalDao().insertGoal(goal.copy(is_synced = true))
                    } else if (goal.updated_at > existing.updated_at) {
                        localDb.savingsGoalDao().updateGoal(goal.copy(is_synced = true))
                    }
                }
            }

            // SAVINGS GOAL ENTRIES
            val entriesSnapshot = db.collection(COL_GOAL_ENTRIES).whereEqualTo("user_id", uid).get().await()
            for (doc in entriesSnapshot.documents) {
                val entry = parseDocument<SavingsGoalEntry>(doc, uid, SavingsGoalEntry::class.java)
                if (entry != null) {
                    val existing = localDb.savingsGoalDao().getAllEntriesSync().find { it.uuid == entry.uuid }
                    if (existing == null) {
                        localDb.savingsGoalDao().insertEntry(entry.copy(is_synced = true))
                    } else if (entry.updated_at > existing.updated_at) {
                        localDb.savingsGoalDao().updateEntry(entry.copy(is_synced = true))
                    }
                }
            }

            // RECURRING TRANSACTIONS
            val recSnapshot = db.collection(COL_RECURRING_TRANSACTIONS).whereEqualTo("user_id", uid).get().await()
            for (doc in recSnapshot.documents) {
                val rec = parseDocument<RecurringTransaction>(doc, uid, RecurringTransaction::class.java)
                if (rec != null) {
                    val existing = localDb.recurringTransactionDao().getAllSync().find { it.uuid == rec.uuid }
                    if (existing == null) {
                        localDb.recurringTransactionDao().insert(rec.copy(is_synced = true))
                    } else if (rec.updated_at > existing.updated_at) {
                        localDb.recurringTransactionDao().update(rec.copy(is_synced = true))
                    }
                }
            }

            // RECURRING OCCURRENCES
            val occSnapshot = db.collection(COL_RECURRING_OCCURRENCES).whereEqualTo("user_id", uid).get().await()
            for (doc in occSnapshot.documents) {
                val occ = parseDocument<RecurringOccurrence>(doc, uid, RecurringOccurrence::class.java)
                if (occ != null) {
                    val existing = localDb.recurringOccurrenceDao().getAllSync().find {
                        it.recurringId == occ.recurringId && it.periodKey == occ.periodKey
                    }
                    if (existing == null) {
                        localDb.recurringOccurrenceDao().insert(occ.copy(is_synced = true))
                    } else if (occ.updated_at > existing.updated_at) {
                        localDb.recurringOccurrenceDao().update(occ.copy(is_synced = true))
                    }
                }
            }

            Log.d(TAG, "Successfully pulled and decrypted all data from Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling data from Firebase", e)
        }
    }

    /**
     * Kullanıcı çıkış yaptığında yerel veritabanını temizler.
     */
    suspend fun clearDatabaseOnLogout() {
        Log.d(TAG, "Clearing local user database on logout")

        localDb.transactionDao().clearAll()
        localDb.investmentDao().clearAll()
        localDb.userGoldAssetDao().clearAll()
        localDb.categoryBudgetDao().clearAll()
        localDb.recurringTransactionDao().clearAll()
        localDb.recurringOccurrenceDao().clearAll()
        localDb.savingsGoalDao().clearAllGoals()
        localDb.savingsGoalDao().clearAllEntries()
    }

    /**
     * Kullanıcının buluttaki (Firestore) tüm şifreli verilerini kalıcı olarak siler.
     */
    suspend fun deleteAllUserDataFromFirebase(uid: String) {
        Log.d(TAG, "Deleting all cloud data for user: $uid")
        val collections = listOf(
            COL_TRANSACTIONS,
            COL_INVESTMENTS,
            COL_GOLD_ASSETS,
            COL_BUDGETS,
            COL_GOALS,
            COL_GOAL_ENTRIES,
            COL_RECURRING_TRANSACTIONS,
            COL_RECURRING_OCCURRENCES
        )
        for (colName in collections) {
            try {
                val snapshot = db.collection(colName).whereEqualTo("user_id", uid).get().await()
                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting collection $colName for user $uid", e)
            }
        }
        Log.d(TAG, "Completed deleting cloud data for user: $uid")
    }

    /**
     * Yerel verileri AES-256-GCM ile şifreleyerek Firestore'a yükler (Push) ve
     * buluttaki güncel kayıtları senkronize eder.
     */
    suspend fun pushDataToFirebase(uid: String) {
        Log.d(TAG, "Pushing Zero-Knowledge encrypted data to Firebase for user: $uid")
        try {
            // TRANSACTIONS
            val txCollection = db.collection(COL_TRANSACTIONS)
            val deletedTx = localDb.transactionDao().getAllTransactionsSync().filter { it.is_deleted && !it.is_synced }
            for (tx in deletedTx) {
                txCollection.document(tx.uuid).delete().await()
                localDb.transactionDao().hardDeleteTransactionById(tx.uuid)
            }
            val unsyncedTx = localDb.transactionDao().getAllTransactionsSync().filter { !it.is_synced && !it.is_deleted && !it.uuid.startsWith("DEMO_TUTORIAL_") }
            for (tx in unsyncedTx) {
                val newTx = tx.copy(is_synced = true, user_id = uid)
                val encryptedRecord = createEncryptedRecord(newTx.uuid, uid, newTx.updated_at, newTx.is_deleted, newTx)
                txCollection.document(newTx.uuid).set(encryptedRecord).await()
                localDb.transactionDao().updateTransaction(newTx)
            }
            val remoteTxSnapshot = txCollection.whereEqualTo("user_id", uid).get().await()
            val localTxMap = localDb.transactionDao().getAllTransactionsSync().associateBy { it.uuid }
            for (doc in remoteTxSnapshot.documents) {
                val remoteTx = parseDocument<Transaction>(doc, uid, Transaction::class.java)
                if (remoteTx != null) {
                    val localTx = localTxMap[remoteTx.uuid]
                    if (localTx == null) {
                        localDb.transactionDao().insertTransaction(remoteTx.copy(is_synced = true))
                    } else if (remoteTx.updated_at > localTx.updated_at) {
                        localDb.transactionDao().updateTransaction(remoteTx.copy(is_synced = true))
                    }
                }
            }

            // INVESTMENTS (CURRENCY)
            val invCollection = db.collection(COL_INVESTMENTS)
            val deletedInv = localDb.investmentDao().getAllInvestmentsSync().filter { it.is_deleted && !it.is_synced }
            for (inv in deletedInv) {
                invCollection.document(inv.uuid).delete().await()
                localDb.investmentDao().hardDeleteInvestmentById(inv.uuid)
            }
            val unsyncedInv = localDb.investmentDao().getAllInvestmentsSync().filter { !it.is_synced && !it.is_deleted && !it.uuid.startsWith("DEMO_TUTORIAL_") }
            for (inv in unsyncedInv) {
                val newInv = inv.copy(is_synced = true, user_id = uid)
                val encryptedRecord = createEncryptedRecord(newInv.uuid, uid, newInv.updated_at, newInv.is_deleted, newInv)
                invCollection.document(newInv.uuid).set(encryptedRecord).await()
                localDb.investmentDao().updateInvestment(newInv)
            }
            val remoteInvSnapshot = invCollection.whereEqualTo("user_id", uid).get().await()
            val localInvMap = localDb.investmentDao().getAllInvestmentsSync().associateBy { it.uuid }
            for (doc in remoteInvSnapshot.documents) {
                val remoteInv = parseDocument<InvestmentItem>(doc, uid, InvestmentItem::class.java)
                if (remoteInv != null) {
                    val localInv = localInvMap[remoteInv.uuid]
                    if (localInv == null) {
                        localDb.investmentDao().insertInvestment(remoteInv.copy(is_synced = true))
                    } else if (remoteInv.updated_at > localInv.updated_at) {
                        localDb.investmentDao().updateInvestment(remoteInv.copy(is_synced = true))
                    }
                }
            }

            // USER GOLD ASSETS
            val goldCollection = db.collection(COL_GOLD_ASSETS)
            val deletedGold = localDb.userGoldAssetDao().getAllSync().filter { it.is_deleted && !it.is_synced }
            for (gold in deletedGold) {
                goldCollection.document(gold.uuid).delete().await()
                localDb.userGoldAssetDao().hardDelete(gold.uuid)
            }
            val unsyncedGold = localDb.userGoldAssetDao().getAllSync().filter { !it.is_synced && !it.is_deleted && !it.uuid.startsWith("DEMO_TUTORIAL_") }
            for (gold in unsyncedGold) {
                val newGold = gold.copy(is_synced = true, user_id = uid)
                val encryptedRecord = createEncryptedRecord(newGold.uuid, uid, newGold.updated_at, newGold.is_deleted, newGold)
                goldCollection.document(newGold.uuid).set(encryptedRecord).await()
                localDb.userGoldAssetDao().update(newGold)
            }
            val remoteGoldSnapshot = goldCollection.whereEqualTo("user_id", uid).get().await()
            val localGoldMap = localDb.userGoldAssetDao().getAllSync().associateBy { it.uuid }
            for (doc in remoteGoldSnapshot.documents) {
                val remoteGold = parseDocument<UserGoldAssetEntity>(doc, uid, UserGoldAssetEntity::class.java)
                if (remoteGold != null) {
                    val localGold = localGoldMap[remoteGold.uuid]
                    if (localGold == null) {
                        localDb.userGoldAssetDao().insert(remoteGold.copy(is_synced = true))
                    } else if (remoteGold.updated_at > localGold.updated_at) {
                        localDb.userGoldAssetDao().update(remoteGold.copy(is_synced = true))
                    }
                }
            }

            // CATEGORY BUDGETS
            val budgetCollection = db.collection(COL_BUDGETS)
            val deletedBudgets = localDb.categoryBudgetDao().getAllSync().filter { it.is_deleted && !it.is_synced }
            for (budget in deletedBudgets) {
                val docId = "${budget.monthKey}_${budget.category}"
                budgetCollection.document(docId).delete().await()
                localDb.categoryBudgetDao().hardDelete(budget.monthKey, budget.category)
            }
            val unsyncedBudgets = localDb.categoryBudgetDao().getAllSync().filter { !it.is_synced && !it.is_deleted }
            for (budget in unsyncedBudgets) {
                val newBudget = budget.copy(is_synced = true, user_id = uid)
                val docId = "${budget.monthKey}_${budget.category}"
                val encryptedRecord = createEncryptedRecord(docId, uid, newBudget.updated_at, newBudget.is_deleted, newBudget)
                budgetCollection.document(docId).set(encryptedRecord).await()
                localDb.categoryBudgetDao().upsert(newBudget)
            }
            val remoteBudgetSnapshot = budgetCollection.whereEqualTo("user_id", uid).get().await()
            val localBudgetMap = localDb.categoryBudgetDao().getAllSync().associateBy { "${it.monthKey}_${it.category}" }
            for (doc in remoteBudgetSnapshot.documents) {
                val remoteBudget = parseDocument<CategoryBudget>(doc, uid, CategoryBudget::class.java)
                if (remoteBudget != null) {
                    val docId = "${remoteBudget.monthKey}_${remoteBudget.category}"
                    val localBudget = localBudgetMap[docId]
                    if (localBudget == null) {
                        localDb.categoryBudgetDao().upsert(remoteBudget.copy(is_synced = true))
                    } else if (remoteBudget.updated_at > localBudget.updated_at) {
                        localDb.categoryBudgetDao().upsert(remoteBudget.copy(is_synced = true))
                    }
                }
            }

            // SAVINGS GOALS
            val goalsCollection = db.collection(COL_GOALS)
            val deletedGoals = localDb.savingsGoalDao().getAllGoalsSync().filter { it.is_deleted && !it.is_synced }
            for (goal in deletedGoals) {
                goalsCollection.document(goal.uuid).delete().await()
                localDb.savingsGoalDao().hardDeleteGoal(goal.uuid)
            }
            val unsyncedGoals = localDb.savingsGoalDao().getAllGoalsSync().filter { !it.is_synced && !it.is_deleted }
            for (goal in unsyncedGoals) {
                val newGoal = goal.copy(is_synced = true, user_id = uid)
                val encryptedRecord = createEncryptedRecord(newGoal.uuid, uid, newGoal.updated_at, newGoal.is_deleted, newGoal)
                goalsCollection.document(newGoal.uuid).set(encryptedRecord).await()
                localDb.savingsGoalDao().updateGoal(newGoal)
            }
            val remoteGoalsSnapshot = goalsCollection.whereEqualTo("user_id", uid).get().await()
            val localGoalsMap = localDb.savingsGoalDao().getAllGoalsSync().associateBy { it.uuid }
            for (doc in remoteGoalsSnapshot.documents) {
                val remoteGoal = parseDocument<SavingsGoal>(doc, uid, SavingsGoal::class.java)
                if (remoteGoal != null) {
                    val localGoal = localGoalsMap[remoteGoal.uuid]
                    if (localGoal == null) {
                        localDb.savingsGoalDao().insertGoal(remoteGoal.copy(is_synced = true))
                    } else if (remoteGoal.updated_at > localGoal.updated_at) {
                        localDb.savingsGoalDao().updateGoal(remoteGoal.copy(is_synced = true))
                    }
                }
            }

            // SAVINGS GOAL ENTRIES
            val entriesCollection = db.collection(COL_GOAL_ENTRIES)
            val deletedEntries = localDb.savingsGoalDao().getAllEntriesSync().filter { it.is_deleted && !it.is_synced }
            for (entry in deletedEntries) {
                entriesCollection.document(entry.uuid).delete().await()
                localDb.savingsGoalDao().hardDeleteEntry(entry.uuid)
            }
            val unsyncedEntries = localDb.savingsGoalDao().getAllEntriesSync().filter { !it.is_synced && !it.is_deleted }
            for (entry in unsyncedEntries) {
                val newEntry = entry.copy(is_synced = true, user_id = uid)
                val encryptedRecord = createEncryptedRecord(newEntry.uuid, uid, newEntry.updated_at, newEntry.is_deleted, newEntry)
                entriesCollection.document(newEntry.uuid).set(encryptedRecord).await()
                localDb.savingsGoalDao().updateEntry(newEntry)
            }
            val remoteEntriesSnapshot = entriesCollection.whereEqualTo("user_id", uid).get().await()
            val localEntriesMap = localDb.savingsGoalDao().getAllEntriesSync().associateBy { it.uuid }
            for (doc in remoteEntriesSnapshot.documents) {
                val remoteEntry = parseDocument<SavingsGoalEntry>(doc, uid, SavingsGoalEntry::class.java)
                if (remoteEntry != null) {
                    val localEntry = localEntriesMap[remoteEntry.uuid]
                    if (localEntry == null) {
                        localDb.savingsGoalDao().insertEntry(remoteEntry.copy(is_synced = true))
                    } else if (remoteEntry.updated_at > localEntry.updated_at) {
                        localDb.savingsGoalDao().updateEntry(remoteEntry.copy(is_synced = true))
                    }
                }
            }

            // RECURRING TRANSACTIONS
            val recurringCol = db.collection(COL_RECURRING_TRANSACTIONS)
            val deletedRecurring = localDb.recurringTransactionDao().getAllSync().filter { it.is_deleted && !it.is_synced }
            for (recurring in deletedRecurring) {
                recurringCol.document(recurring.uuid).delete().await()
                localDb.recurringTransactionDao().hardDelete(recurring.uuid)
            }
            val unsyncedRecurring = localDb.recurringTransactionDao().getAllSync().filter { !it.is_synced && !it.is_deleted && !it.uuid.startsWith("DEMO_TUTORIAL_") }
            for (recurring in unsyncedRecurring) {
                val newRec = recurring.copy(is_synced = true, user_id = uid)
                val encryptedRecord = createEncryptedRecord(newRec.uuid, uid, newRec.updated_at, newRec.is_deleted, newRec)
                recurringCol.document(newRec.uuid).set(encryptedRecord).await()
                localDb.recurringTransactionDao().update(newRec)
            }
            val remoteRecurringSnapshot = recurringCol.whereEqualTo("user_id", uid).get().await()
            val localRecurringMap = localDb.recurringTransactionDao().getAllSync().associateBy { it.uuid }
            for (doc in remoteRecurringSnapshot.documents) {
                val remoteRec = parseDocument<RecurringTransaction>(doc, uid, RecurringTransaction::class.java)
                if (remoteRec != null) {
                    val localRec = localRecurringMap[remoteRec.uuid]
                    if (localRec == null) {
                        localDb.recurringTransactionDao().insert(remoteRec.copy(is_synced = true))
                    } else if (remoteRec.updated_at > localRec.updated_at) {
                        localDb.recurringTransactionDao().update(remoteRec.copy(is_synced = true))
                    }
                }
            }

            // RECURRING OCCURRENCES
            val occCol = db.collection(COL_RECURRING_OCCURRENCES)
            val deletedOcc = localDb.recurringOccurrenceDao().getAllSync().filter { it.is_deleted && !it.is_synced }
            for (occ in deletedOcc) {
                occCol.document("${occ.recurringId}_${occ.periodKey}").delete().await()
                localDb.recurringOccurrenceDao().hardDelete(occ.recurringId, occ.periodKey)
            }
            val unsyncedOcc = localDb.recurringOccurrenceDao().getAllSync().filter { !it.is_synced && !it.is_deleted }
            for (occ in unsyncedOcc) {
                val newOcc = occ.copy(is_synced = true, user_id = uid)
                val docId = "${newOcc.recurringId}_${newOcc.periodKey}"
                val encryptedRecord = createEncryptedRecord(docId, uid, newOcc.updated_at, newOcc.is_deleted, newOcc)
                occCol.document(docId).set(encryptedRecord).await()
                localDb.recurringOccurrenceDao().update(newOcc)
            }
            val remoteOccSnapshot = occCol.whereEqualTo("user_id", uid).get().await()
            val localOccMap = localDb.recurringOccurrenceDao().getAllSync().associateBy { "${it.recurringId}_${it.periodKey}" }
            for (doc in remoteOccSnapshot.documents) {
                val remoteOcc = parseDocument<RecurringOccurrence>(doc, uid, RecurringOccurrence::class.java)
                if (remoteOcc != null) {
                    val localOcc = localOccMap["${remoteOcc.recurringId}_${remoteOcc.periodKey}"]
                    if (localOcc == null) {
                        localDb.recurringOccurrenceDao().insert(remoteOcc.copy(is_synced = true))
                    } else if (remoteOcc.updated_at > localOcc.updated_at) {
                        localDb.recurringOccurrenceDao().update(remoteOcc.copy(is_synced = true))
                    }
                }
            }

            Log.d(TAG, "Zero-Knowledge sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing data to Firebase", e)
        }
    }

    /**
     * Herhangi bir veri modelini JSON'a serileştirir ve AES-256-GCM ile şifreleyerek
     * EncryptedCloudRecord oluşturur.
     */
    private fun <T> createEncryptedRecord(
        docId: String,
        uid: String,
        updatedAt: Long,
        isDeleted: Boolean,
        entity: T
    ): EncryptedCloudRecord {
        val json = gson.toJson(entity)
        val encryptedPayload = ZeroKnowledgeCrypto.encrypt(json, uid)
        return EncryptedCloudRecord(
            uuid = docId,
            user_id = uid,
            updated_at = updatedAt,
            is_deleted = isDeleted,
            payload = encryptedPayload,
            encrypted = true,
            version = 1
        )
    }

    /**
     * Firestore dokümanını okur. Eğer doküman şifreli (encrypted) ise AES-256 ile çözer
     * ve hedef sınıfa dönüştürür. Eski şifresiz dokümanlar için geriye dönük uyumluluk sağlar.
     */
    private fun <T> parseDocument(doc: DocumentSnapshot, uid: String, targetClass: Class<T>): T? {
        val payload = doc.getString("payload")
        val isEncrypted = doc.getBoolean("encrypted") ?: (payload != null)

        return if (isEncrypted && !payload.isNullOrBlank()) {
            try {
                val decryptedJson = ZeroKnowledgeCrypto.decrypt(payload, uid)
                gson.fromJson(decryptedJson, targetClass)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrypt document ${doc.id}", e)
                null
            }
        } else {
            // Şifresiz eski veri formatı için geriye dönük uyumluluk
            doc.toObject(targetClass)
        }
    }

    companion object {
        private const val TAG = "FirebaseSyncManager"
        private const val COL_TRANSACTIONS = "transactions"
        private const val COL_INVESTMENTS = "investments"
        private const val COL_GOLD_ASSETS = "user_gold_assets"
        private const val COL_BUDGETS = "category_budgets"
        private const val COL_GOALS = "savings_goals"
        private const val COL_GOAL_ENTRIES = "savings_goal_entries"
        private const val COL_RECURRING_TRANSACTIONS = "recurring_transactions_table"
        private const val COL_RECURRING_OCCURRENCES = "recurring_occurrences"
    }
}



