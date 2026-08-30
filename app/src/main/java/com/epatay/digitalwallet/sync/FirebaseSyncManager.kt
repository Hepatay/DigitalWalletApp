package com.epatay.digitalwallet.sync

import android.content.Context
import android.util.Log
import com.epatay.digitalwallet.data.TransactionDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.epatay.digitalwallet.data.Transaction

class FirebaseSyncManager(private val context: Context) {
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val localDb = TransactionDatabase.getDatabase(context)

    /**
     * Misafir kullanıcı (guest) olarak kaydedilmiş verileri,
     * yeni giriş yapan Firebase kullanıcısının hesabına aktarır.
     */
    suspend fun assignGuestDataToUser() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val now = System.currentTimeMillis()

        Log.d("FirebaseSync", "Assigning guest records to user: $uid")
        
        try {
            localDb.transactionDao().assignUserToGuestRecords(uid, now)
            localDb.categoryBudgetDao().assignUserToGuestRecords(uid, now)
            localDb.recurringTransactionDao().assignUserToGuestRecords(uid, now)
            localDb.recurringOccurrenceDao().assignUserToGuestRecords(uid, now)
            localDb.savingsGoalDao().assignUserToGuestRecords(uid, now)
            localDb.savingsGoalDao().assignUserToGuestEntries(uid, now)
            localDb.investmentDao().assignUserToGuestRecords(uid, now)
            localDb.userGoldAssetDao().assignUserToGuestRecords(uid, now)
            
            Log.d("FirebaseSync", "Successfully assigned guest data to user $uid")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error assigning guest data", e)
        }
    }

    /**
     * Firebase'den verileri çeker ve yerel veritabanına ekler.
     * Kullanıcı yeni giriş yaptığında veya başka cihazdan gelen verileri senkronize etmek için kullanılır.
     */
    suspend fun pullDataFromFirebase(uid: String) {
        Log.d("FirebaseSync", "Pulling data from Firebase for user: $uid")
        try {
                        val txSnapshot = db.collection("transactions").whereEqualTo("user_id", uid).get().await()
            for (doc in txSnapshot.documents) {
                val tx = doc.toObject(Transaction::class.java)
                if (tx != null) {
                    val existing = localDb.transactionDao().getAllTransactionsSync().find { it.uuid == tx.uuid }
                    if (existing == null) {
                        localDb.transactionDao().insertTransaction(tx.copy(is_synced = true))
                    } else if (tx.updated_at > existing.updated_at) {
                        localDb.transactionDao().updateTransaction(tx.copy(is_synced = true))
                    }
                }
            }
            
            val invSnapshot = db.collection("investments").whereEqualTo("user_id", uid).get().await()
            for (doc in invSnapshot.documents) {
                val inv = doc.toObject(com.epatay.digitalwallet.data.InvestmentItem::class.java)
                if (inv != null) {
                    val existing = localDb.investmentDao().getAllInvestmentsSync().find { it.uuid == inv.uuid }
                    if (existing == null) {
                        localDb.investmentDao().insertInvestment(inv.copy(is_synced = true))
                    } else if (inv.updated_at > existing.updated_at) {
                        localDb.investmentDao().updateInvestment(inv.copy(is_synced = true))
                    }
                }
            }

            val goldSnapshot = db.collection("user_gold_assets").whereEqualTo("user_id", uid).get().await()
            for (doc in goldSnapshot.documents) {
                val gold = doc.toObject(com.epatay.digitalwallet.data.UserGoldAssetEntity::class.java)
                if (gold != null) {
                    val existing = localDb.userGoldAssetDao().getAllSync().find { it.uuid == gold.uuid }
                    if (existing == null) {
                        localDb.userGoldAssetDao().insert(gold.copy(is_synced = true))
                    } else if (gold.updated_at > existing.updated_at) {
                        localDb.userGoldAssetDao().update(gold.copy(is_synced = true))
                    }
                }
            }

            val budgetSnapshot = db.collection("category_budgets").whereEqualTo("user_id", uid).get().await()
            for (doc in budgetSnapshot.documents) {
                val budget = doc.toObject(com.epatay.digitalwallet.data.CategoryBudget::class.java)
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

            val goalsSnapshot = db.collection("savings_goals").whereEqualTo("user_id", uid).get().await()
            for (doc in goalsSnapshot.documents) {
                val goal = doc.toObject(com.epatay.digitalwallet.data.SavingsGoal::class.java)
                if (goal != null) {
                    val existing = localDb.savingsGoalDao().getAllGoalsSync().find { it.uuid == goal.uuid }
                    if (existing == null) {
                        localDb.savingsGoalDao().insertGoal(goal.copy(is_synced = true))
                    } else if (goal.updated_at > existing.updated_at) {
                        localDb.savingsGoalDao().updateGoal(goal.copy(is_synced = true))
                    }
                }
            }

            val entriesSnapshot = db.collection("savings_goal_entries").whereEqualTo("user_id", uid).get().await()
            for (doc in entriesSnapshot.documents) {
                val entry = doc.toObject(com.epatay.digitalwallet.data.SavingsGoalEntry::class.java)
                if (entry != null) {
                    val existing = localDb.savingsGoalDao().getAllEntriesSync().find { it.uuid == entry.uuid }
                    if (existing == null) {
                        localDb.savingsGoalDao().insertEntry(entry.copy(is_synced = true))
                    } else if (entry.updated_at > existing.updated_at) {
                        localDb.savingsGoalDao().updateEntry(entry.copy(is_synced = true))
                    }
                }
            }

            val recSnapshot = db.collection("recurring_transactions_table").whereEqualTo("user_id", uid).get().await()
            for (doc in recSnapshot.documents) {
                val rec = doc.toObject(com.epatay.digitalwallet.data.RecurringTransaction::class.java)
                if (rec != null) {
                    val existing = localDb.recurringTransactionDao().getAllSync().find { it.uuid == rec.uuid }
                    if (existing == null) {
                        localDb.recurringTransactionDao().insert(rec.copy(is_synced = true))
                    } else if (rec.updated_at > existing.updated_at) {
                        localDb.recurringTransactionDao().update(rec.copy(is_synced = true))
                    }
                }
            }

            val occSnapshot = db.collection("recurring_occurrences").whereEqualTo("user_id", uid).get().await()
            for (doc in occSnapshot.documents) {
                val occ = doc.toObject(com.epatay.digitalwallet.data.RecurringOccurrence::class.java)
                if (occ != null) {
                    val existing = localDb.recurringOccurrenceDao().getAllSync().find { it.recurringId == occ.recurringId && it.periodKey == occ.periodKey }
                    if (existing == null) {
                        localDb.recurringOccurrenceDao().insert(occ.copy(is_synced = true))
                    } else if (occ.updated_at > existing.updated_at) {
                        localDb.recurringOccurrenceDao().update(occ.copy(is_synced = true))
                    }
                }
            }

            Log.d("FirebaseSync", "Successfully pulled data from Firebase")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error pulling data from Firebase", e)
        }
    }

    /**
     * Kullanıcı çıkış yaptığında yerel veritabanını temizler.
     * Bu sayede başka birisi giriş yaptığında veya misafir olarak
     * girdiğinde eski kullanıcının verilerini göremez.
     */
    suspend fun clearDatabaseOnLogout() {
        Log.d("FirebaseSync", "Clearing local user database on logout")
        
        localDb.transactionDao().clearAll()
        localDb.investmentDao().clearAll()
        localDb.userGoldAssetDao().clearAll()
        localDb.categoryBudgetDao().clearAll()
        localDb.recurringTransactionDao().clearAll()
        localDb.recurringOccurrenceDao().clearAll()
        localDb.savingsGoalDao().clearAllGoals()
        localDb.savingsGoalDao().clearAllEntries()
        // localDb.walletDao().clearAll() // Wallet sync is not fully active so we can skip or include it if needed
        
        // DAO'ları kullandığımız için Flow'lar otomatik tetiklenecek.
    }

    /**
     * Kullanıcının buluttaki (Firestore) tüm verilerini siler.
     * Google Play kuralları gereği hesap ve veri silme işleminde çağrılır.
     */
    suspend fun deleteAllUserDataFromFirebase(uid: String) {
        Log.d("FirebaseSync", "Deleting all cloud data for user: $uid")
        val collections = listOf(
            "transactions",
            "investments",
            "user_gold_assets",
            "category_budgets",
            "savings_goals",
            "savings_goal_entries",
            "recurring_transactions_table",
            "recurring_occurrences"
        )
        for (colName in collections) {
            try {
                val snapshot = db.collection(colName).whereEqualTo("user_id", uid).get().await()
                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error deleting collection $colName for user $uid", e)
            }
        }
        Log.d("FirebaseSync", "Completed deleting cloud data for user: $uid")
    }

    suspend fun pushDataToFirebase(uid: String) {
        Log.d("FirebaseSync", "Pushing data to Firebase for user: $uid")
        try {
            // TRANSACTIONS
            val txCollection = db.collection("transactions")
            val deletedTx = localDb.transactionDao().getAllTransactionsSync().filter { it.is_deleted && !it.is_synced }
            for (tx in deletedTx) {
                txCollection.document(tx.uuid).delete().await()
                localDb.transactionDao().hardDeleteTransactionById(tx.uuid)
            }
            val unsyncedTx = localDb.transactionDao().getAllTransactionsSync().filter { !it.is_synced && !it.is_deleted && !it.uuid.startsWith("DEMO_TUTORIAL_") }
            for (tx in unsyncedTx) {
                val newTx = tx.copy(is_synced = true, user_id = uid)
                txCollection.document(newTx.uuid).set(newTx).await()
                localDb.transactionDao().updateTransaction(newTx)
            }
            val remoteTxSnapshot = txCollection.whereEqualTo("user_id", uid).get().await()
            val localTxMap = localDb.transactionDao().getAllTransactionsSync().associateBy { it.uuid }
            for (doc in remoteTxSnapshot.documents) {
                val remoteTx = doc.toObject(com.epatay.digitalwallet.data.Transaction::class.java)
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
            val invCollection = db.collection("investments")
            val deletedInv = localDb.investmentDao().getAllInvestmentsSync().filter { it.is_deleted && !it.is_synced }
            for (inv in deletedInv) {
                invCollection.document(inv.uuid).delete().await()
                localDb.investmentDao().hardDeleteInvestmentById(inv.uuid)
            }
            val unsyncedInv = localDb.investmentDao().getAllInvestmentsSync().filter { !it.is_synced && !it.is_deleted && !it.uuid.startsWith("DEMO_TUTORIAL_") }
            for (inv in unsyncedInv) {
                val newInv = inv.copy(is_synced = true, user_id = uid)
                invCollection.document(newInv.uuid).set(newInv).await()
                localDb.investmentDao().updateInvestment(newInv)
            }
            val remoteInvSnapshot = invCollection.whereEqualTo("user_id", uid).get().await()
            val localInvMap = localDb.investmentDao().getAllInvestmentsSync().associateBy { it.uuid }
            for (doc in remoteInvSnapshot.documents) {
                val remoteInv = doc.toObject(com.epatay.digitalwallet.data.InvestmentItem::class.java)
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
            val goldCollection = db.collection("user_gold_assets")
            val deletedGold = localDb.userGoldAssetDao().getAllSync().filter { it.is_deleted && !it.is_synced }
            for (gold in deletedGold) {
                goldCollection.document(gold.uuid).delete().await()
                localDb.userGoldAssetDao().hardDelete(gold.uuid)
            }
            val unsyncedGold = localDb.userGoldAssetDao().getAllSync().filter { !it.is_synced && !it.is_deleted && !it.uuid.startsWith("DEMO_TUTORIAL_") }
            for (gold in unsyncedGold) {
                val newGold = gold.copy(is_synced = true, user_id = uid)
                goldCollection.document(newGold.uuid).set(newGold).await()
                localDb.userGoldAssetDao().update(newGold)
            }
            val remoteGoldSnapshot = goldCollection.whereEqualTo("user_id", uid).get().await()
            val localGoldMap = localDb.userGoldAssetDao().getAllSync().associateBy { it.uuid }
            for (doc in remoteGoldSnapshot.documents) {
                val remoteGold = doc.toObject(com.epatay.digitalwallet.data.UserGoldAssetEntity::class.java)
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
            val budgetCollection = db.collection("category_budgets")
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
                budgetCollection.document(docId).set(newBudget).await()
                localDb.categoryBudgetDao().upsert(newBudget)
            }
            val remoteBudgetSnapshot = budgetCollection.whereEqualTo("user_id", uid).get().await()
            val localBudgetMap = localDb.categoryBudgetDao().getAllSync().associateBy { "${it.monthKey}_${it.category}" }
            for (doc in remoteBudgetSnapshot.documents) {
                val remoteBudget = doc.toObject(com.epatay.digitalwallet.data.CategoryBudget::class.java)
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
            val goalsCollection = db.collection("savings_goals")
            val deletedGoals = localDb.savingsGoalDao().getAllGoalsSync().filter { it.is_deleted && !it.is_synced }
            for (goal in deletedGoals) {
                goalsCollection.document(goal.uuid).delete().await()
                localDb.savingsGoalDao().hardDeleteGoal(goal.uuid)
            }
            val unsyncedGoals = localDb.savingsGoalDao().getAllGoalsSync().filter { !it.is_synced && !it.is_deleted }
            for (goal in unsyncedGoals) {
                val newGoal = goal.copy(is_synced = true, user_id = uid)
                goalsCollection.document(newGoal.uuid).set(newGoal).await()
                localDb.savingsGoalDao().updateGoal(newGoal)
            }
            val remoteGoalsSnapshot = goalsCollection.whereEqualTo("user_id", uid).get().await()
            val localGoalsMap = localDb.savingsGoalDao().getAllGoalsSync().associateBy { it.uuid }
            for (doc in remoteGoalsSnapshot.documents) {
                val remoteGoal = doc.toObject(com.epatay.digitalwallet.data.SavingsGoal::class.java)
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
            val entriesCollection = db.collection("savings_goal_entries")
            val deletedEntries = localDb.savingsGoalDao().getAllEntriesSync().filter { it.is_deleted && !it.is_synced }
            for (entry in deletedEntries) {
                entriesCollection.document(entry.uuid).delete().await()
                localDb.savingsGoalDao().hardDeleteEntry(entry.uuid)
            }
            val unsyncedEntries = localDb.savingsGoalDao().getAllEntriesSync().filter { !it.is_synced && !it.is_deleted }
            for (entry in unsyncedEntries) {
                val newEntry = entry.copy(is_synced = true, user_id = uid)
                entriesCollection.document(newEntry.uuid).set(newEntry).await()
                localDb.savingsGoalDao().updateEntry(newEntry)
            }
            val remoteEntriesSnapshot = entriesCollection.whereEqualTo("user_id", uid).get().await()
            val localEntriesMap = localDb.savingsGoalDao().getAllEntriesSync().associateBy { it.uuid }
            for (doc in remoteEntriesSnapshot.documents) {
                val remoteEntry = doc.toObject(com.epatay.digitalwallet.data.SavingsGoalEntry::class.java)
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
            val recurringCol = db.collection("recurring_transactions_table")
            val deletedRecurring = localDb.recurringTransactionDao().getAllSync().filter { it.is_deleted && !it.is_synced }
            for (recurring in deletedRecurring) {
                recurringCol.document(recurring.uuid).delete().await()
                localDb.recurringTransactionDao().hardDelete(recurring.uuid)
            }
            val unsyncedRecurring = localDb.recurringTransactionDao().getAllSync().filter { !it.is_synced && !it.is_deleted && !it.uuid.startsWith("DEMO_TUTORIAL_") }
            for (recurring in unsyncedRecurring) {
                val newRec = recurring.copy(is_synced = true, user_id = uid)
                recurringCol.document(newRec.uuid).set(newRec).await()
                localDb.recurringTransactionDao().update(newRec)
            }
            val remoteRecurringSnapshot = recurringCol.whereEqualTo("user_id", uid).get().await()
            val localRecurringMap = localDb.recurringTransactionDao().getAllSync().associateBy { it.uuid }
            for (doc in remoteRecurringSnapshot.documents) {
                val remoteRec = doc.toObject(com.epatay.digitalwallet.data.RecurringTransaction::class.java)
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
            val occCol = db.collection("recurring_occurrences")
            val deletedOcc = localDb.recurringOccurrenceDao().getAllSync().filter { it.is_deleted && !it.is_synced }
            for (occ in deletedOcc) {
                occCol.document("${occ.recurringId}_${occ.periodKey}").delete().await()
                localDb.recurringOccurrenceDao().hardDelete(occ.recurringId, occ.periodKey)
            }
            val unsyncedOcc = localDb.recurringOccurrenceDao().getAllSync().filter { !it.is_synced && !it.is_deleted }
            for (occ in unsyncedOcc) {
                val newOcc = occ.copy(is_synced = true, user_id = uid)
                occCol.document("${newOcc.recurringId}_${newOcc.periodKey}").set(newOcc).await()
                localDb.recurringOccurrenceDao().update(newOcc)
            }
            val remoteOccSnapshot = occCol.whereEqualTo("user_id", uid).get().await()
            val localOccMap = localDb.recurringOccurrenceDao().getAllSync().associateBy { "${it.recurringId}_${it.periodKey}" }
            for (doc in remoteOccSnapshot.documents) {
                val remoteOcc = doc.toObject(com.epatay.digitalwallet.data.RecurringOccurrence::class.java)
                if (remoteOcc != null) {
                    val localOcc = localOccMap["${remoteOcc.recurringId}_${remoteOcc.periodKey}"]
                    if (localOcc == null) {
                        localDb.recurringOccurrenceDao().insert(remoteOcc.copy(is_synced = true))
                    } else if (remoteOcc.updated_at > localOcc.updated_at) {
                        localDb.recurringOccurrenceDao().update(remoteOcc.copy(is_synced = true))
                    }
                }
            }

            Log.d("FirebaseSync", "Successfully pushed data to Firebase")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error pushing data to Firebase", e)
        }
    }
}


