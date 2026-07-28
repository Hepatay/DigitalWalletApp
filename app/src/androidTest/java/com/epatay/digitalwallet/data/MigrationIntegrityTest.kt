package com.epatay.digitalwallet.data

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationIntegrityTest {

    private val context: Context =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext

    @get:Rule
    val migrationHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            TransactionDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory()
        )

    @Before
    fun prepareDatabase() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun cleanDatabase() {
        TransactionDatabase.getDatabase(context).close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun migration9To10_preservesLegacyGoldFieldsAndDate() {
        migrationHelper.createDatabase(
            DATABASE_NAME,
            9
        ).apply {
            execSQL(
                """
                INSERT INTO investments_table
                    (id, assetName, amount, buyPrice, buyDate)
                VALUES
                    (41, 'Gram Altın', 2.5, 4250.75, '14.07.2026')
                """.trimIndent()
            )
            close()
        }

        val database =
            TransactionDatabase.getDatabase(context)

        runBlocking {
            val migrated =
                database.userGoldAssetDao()
                    .getAllSnapshot()
                    .single()

            assertEquals("GRAM_GOLD", migrated.goldType)
            assertEquals(2.5, migrated.quantity, 0.000_001)
            assertEquals(
                4250.75,
                migrated.purchaseUnitPrice ?: 0.0,
                0.000_001
            )
            assertEquals(
                10_626.875,
                migrated.totalPurchaseCost ?: 0.0,
                0.000_001
            )
            assertNotNull(migrated.purchaseDate)
            assertEquals(
                0,
                database.investmentDao()
                    .getAllInvestmentsSnapshot()
                    .size
            )
        }
    }

    private companion object {
        const val DATABASE_NAME = "transaction_database"
    }
}
