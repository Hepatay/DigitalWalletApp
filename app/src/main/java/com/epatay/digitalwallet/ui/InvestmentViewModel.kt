package com.epatay.digitalwallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.epatay.digitalwallet.data.CurrencyRateEntity
import com.epatay.digitalwallet.data.DecimalMath
import com.epatay.digitalwallet.data.GoldInputUnit
import com.epatay.digitalwallet.data.GoldRateEntity
import com.epatay.digitalwallet.data.GoldType
import com.epatay.digitalwallet.data.InvestmentItem
import com.epatay.digitalwallet.data.MarketPriceValidator
import com.epatay.digitalwallet.data.PortfolioCalculator
import com.epatay.digitalwallet.data.TcmbXmlParser
import com.epatay.digitalwallet.data.TransactionDatabase
import com.epatay.digitalwallet.data.UserGoldAssetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class InvestmentViewModel(application: Application) : AndroidViewModel(application) {

    private val database = TransactionDatabase.getDatabase(application)
    private val investmentDao = database.investmentDao()
    private val goldAssetDao = database.userGoldAssetDao()
    private val currencyRateDao = database.currencyRateDao()
    private val goldRateDao = database.goldRateDao()

    private val currencySellingPrices = ConcurrentHashMap<String, Double>()
    private val goldSellingPrices = ConcurrentHashMap<GoldType, Double>()

    val currencyCodes: LiveData<List<String>> =
        currencyRateDao.observeAllRates()
            .map { rates ->
                TcmbXmlParser.sortRates(
                    rates.map(CurrencyRateEntity::toCurrencyRate)
                ).map { it.currencyCode }
            }
            .asLiveData()

    val portfolioItems: LiveData<List<PortfolioAssetItem>> =
        combine(
            investmentDao.observeAllInvestments(),
            goldAssetDao.observeAll(),
            currencyRateDao.observeAllRates(),
            goldRateDao.observeAll()
        ) { investments, goldAssets, currencyRates, goldRates ->
            updatePurchasePriceCaches(currencyRates, goldRates)

            buildList {
                investments.forEach { investment ->
                    add(investment.toPortfolioItem(currencyRates))
                }
                goldAssets.forEach { asset ->
                    add(asset.toPortfolioItem(goldRates))
                }
            }.sortedByDescending { item ->
                item.legacyInvestment?.id?.toLong()
                    ?: item.goldAsset?.createdAt
                    ?: 0L
            }
        }.asLiveData()

    fun currentPurchasePrice(
        code: String,
        goldType: GoldType?
    ): Double? =
        goldType?.let(goldSellingPrices::get)
            ?: currencySellingPrices[code]

    fun insertCurrency(
        code: String,
        quantity: Double,
        purchaseUnitPrice: Double,
        purchaseDateText: String,
        note: String?
    ) = viewModelScope.launch(Dispatchers.IO) {
        val normalizedCode =
            code.trim().uppercase(Locale.ROOT)
        val normalizedQuantity =
            DecimalMath.normalizeQuantity(quantity)
                ?.takeIf { it > 0.0 }
                ?: return@launch
        val normalizedPurchasePrice =
            DecimalMath.normalizeUnitPrice(purchaseUnitPrice)
                ?.takeIf { it > 0.0 }
                ?: return@launch

        if (normalizedCode.isEmpty()) {
            return@launch
        }

        investmentDao.insertInvestment(
            InvestmentItem(
                assetName = normalizedCode,
                amount = normalizedQuantity,
                buyPrice = normalizedPurchasePrice,
                buyDate = purchaseDateText,
                note = note?.trim()?.takeIf(String::isNotEmpty)
            )
        )
    }

    fun insertGold(
        type: GoldType,
        quantity: Double,
        purchaseUnitPrice: Double,
        purchaseDate: Long?,
        note: String?
    ) = viewModelScope.launch(Dispatchers.IO) {
        val normalizedQuantity =
            DecimalMath.normalizeQuantity(quantity)
                ?.takeIf { it > 0.0 }
                ?: return@launch
        val normalizedPurchasePrice =
            DecimalMath.normalizeUnitPrice(purchaseUnitPrice)
                ?.takeIf { it > 0.0 }
                ?: return@launch
        val totalPurchaseCost =
            DecimalMath.multiplyMoney(
                normalizedQuantity,
                normalizedPurchasePrice
            ) ?: return@launch
        val now = System.currentTimeMillis()

        goldAssetDao.insert(
            UserGoldAssetEntity(
                goldType = type.name,
                quantity = normalizedQuantity,
                unit = type.inputUnit.name,
                purchaseUnitPrice = normalizedPurchasePrice,
                totalPurchaseCost = totalPurchaseCost,
                purchaseDate = purchaseDate,
                note = note?.trim()?.takeIf(String::isNotEmpty),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    fun update(
        item: PortfolioAssetItem,
        quantity: Double,
        purchaseUnitPrice: Double
    ) = viewModelScope.launch(Dispatchers.IO) {
        val normalizedQuantity =
            DecimalMath.normalizeQuantity(quantity)
                ?.takeIf { it > 0.0 }
                ?: return@launch
        val normalizedPurchasePrice =
            DecimalMath.normalizeUnitPrice(purchaseUnitPrice)
                ?.takeIf { it > 0.0 }
                ?: return@launch
        val totalPurchaseCost =
            DecimalMath.multiplyMoney(
                normalizedQuantity,
                normalizedPurchasePrice
            ) ?: return@launch

        item.legacyInvestment?.let {
            investmentDao.updateInvestment(
                it.copy(
                    amount = normalizedQuantity,
                    buyPrice = normalizedPurchasePrice
                )
            )
        }
        item.goldAsset?.let {
            goldAssetDao.update(
                it.copy(
                    quantity = normalizedQuantity,
                    purchaseUnitPrice = normalizedPurchasePrice,
                    totalPurchaseCost = totalPurchaseCost,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun delete(item: PortfolioAssetItem) =
        viewModelScope.launch(Dispatchers.IO) {
            item.legacyInvestment?.let { investmentDao.deleteInvestment(it) }
            item.goldAsset?.let { goldAssetDao.delete(it) }
        }

    private fun updatePurchasePriceCaches(
        currencies: List<CurrencyRateEntity>,
        goldRates: List<GoldRateEntity>
    ) {
        currencySellingPrices.clear()
        currencies.forEach { rate ->
            if (rate.unit > 0) {
                MarketPriceValidator.validate(
                    rate.forexBuying?.div(rate.unit),
                    rate.forexSelling?.div(rate.unit)
                )?.sellingPrice?.toDouble()?.let {
                    currencySellingPrices[rate.currencyCode] = it
                }
            }
        }

        goldSellingPrices.clear()
        goldRates.forEach { rate ->
            val type = runCatching { GoldType.valueOf(rate.type) }.getOrNull()
            val prices = MarketPriceValidator.validate(rate.buyingPrice, rate.sellingPrice)
            if (type != null && prices != null) {
                goldSellingPrices[type] = prices.sellingPrice.toDouble()
            }
        }
    }

    private fun InvestmentItem.toPortfolioItem(
        rates: List<CurrencyRateEntity>
    ): PortfolioAssetItem {
        val code = assetName.trim().uppercase(Locale.ROOT)
        val rate = rates.firstOrNull { it.currencyCode == code }
        val prices =
            rate?.takeIf { it.unit > 0 }?.let {
                MarketPriceValidator.validate(
                    it.forexBuying?.div(it.unit),
                    it.forexSelling?.div(it.unit)
                )
            }
        val calculation =
            PortfolioCalculator.calculate(
                quantity = amount,
                userPurchaseUnitPrice = buyPrice,
                marketBuyingPrice = prices?.buyingPrice?.toDouble()
            )

        return PortfolioAssetItem(
            stableKey = "currency-$id",
            kind = PortfolioAssetKind.CURRENCY,
            displayName = code,
            code = code,
            quantity = amount,
            unitLabel = "birim",
            purchaseUnitPrice = buyPrice,
            totalPurchaseCost = calculation?.totalCost?.toDouble(),
            purchaseDateText = buyDate,
            note = note,
            marketBuyingPrice = prices?.buyingPrice?.toDouble(),
            marketSellingPrice = prices?.sellingPrice?.toDouble(),
            spread = prices?.spread?.toDouble(),
            spreadPercentage = prices?.spreadPercentage?.toDouble(),
            currentValue = calculation?.estimatedSaleValue?.toDouble(),
            profitLoss = calculation?.profitLoss?.toDouble(),
            profitLossPercentage = calculation?.profitLossPercentage?.toDouble(),
            source = rate?.let { "TCMB" },
            sourceUpdatedAt = rate?.fetchedAtMillis,
            sourceFetchedAt = rate?.fetchedAtMillis,
            legacyInvestment = this
        )
    }

    private fun UserGoldAssetEntity.toPortfolioItem(
        rates: List<GoldRateEntity>
    ): PortfolioAssetItem {
        val type = runCatching { GoldType.valueOf(goldType) }.getOrNull()
        val rate = rates.firstOrNull { it.type == goldType }
        val prices =
            rate?.let {
                MarketPriceValidator.validate(it.buyingPrice, it.sellingPrice)
            }
        val calculation =
            PortfolioCalculator.calculate(
                quantity = quantity,
                userPurchaseUnitPrice = purchaseUnitPrice,
                marketBuyingPrice = prices?.buyingPrice?.toDouble()
            )

        return PortfolioAssetItem(
            stableKey = "gold-$id",
            kind = PortfolioAssetKind.GOLD,
            displayName = type?.displayName ?: goldType,
            code = goldType,
            quantity = quantity,
            unitLabel =
                if (type?.inputUnit == GoldInputUnit.GRAM) "gram" else "adet",
            purchaseUnitPrice = purchaseUnitPrice,
            totalPurchaseCost = calculation?.totalCost?.toDouble(),
            purchaseDateText = purchaseDate.formatDate(),
            note = note,
            marketBuyingPrice = prices?.buyingPrice?.toDouble(),
            marketSellingPrice = prices?.sellingPrice?.toDouble(),
            spread = prices?.spread?.toDouble(),
            spreadPercentage = prices?.spreadPercentage?.toDouble(),
            currentValue = calculation?.estimatedSaleValue?.toDouble(),
            profitLoss = calculation?.profitLoss?.toDouble(),
            profitLossPercentage = calculation?.profitLossPercentage?.toDouble(),
            source = rate?.source,
            sourceUpdatedAt = rate?.sourceUpdatedAt,
            sourceFetchedAt = rate?.fetchedAt,
            goldAsset = this,
            goldType = type
        )
    }

    private fun Long?.formatDate(): String {
        if (this == null) return "Alış tarihi belirtilmedi"
        return SimpleDateFormat(
            "dd.MM.yyyy",
            Locale.forLanguageTag("tr-TR")
        ).format(Date(this))
    }
}
