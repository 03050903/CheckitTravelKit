package eu.szwiec.checkittravelkit.ui.info

import android.content.Context
import android.util.Patterns
import androidx.annotation.VisibleForTesting
import androidx.databinding.ObservableArrayList
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import eu.szwiec.checkittravelkit.BR
import eu.szwiec.checkittravelkit.R
import eu.szwiec.checkittravelkit.prefs.Preferences
import eu.szwiec.checkittravelkit.repository.CountryRepository
import eu.szwiec.checkittravelkit.repository.data.Country
import eu.szwiec.checkittravelkit.repository.data.Currency
import eu.szwiec.checkittravelkit.repository.data.Telephones
import eu.szwiec.checkittravelkit.repository.data.Visa
import eu.szwiec.checkittravelkit.util.NonNullLiveData
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
import me.tatarka.bindingcollectionadapter2.itembindings.OnItemBindClass
import org.threeten.bp.DateTimeException
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

class InfoViewModel(private val context: Context, private val preferences: Preferences, private val repository: CountryRepository, private val plugProvider: PlugProvider) : ViewModel() {

    val isFavorite = NonNullLiveData(false)
    val countryName = NonNullLiveData("")
    val image = NonNullLiveData("")
    val items: DiffObservableList<Any> = DiffObservableList(object : DiffObservableList.Callback<Any> {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return areContentsTheSame(oldItem, newItem)
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return if (oldItem is SimpleInfo && newItem is SimpleInfo) {
                oldItem.text == newItem.text
            } else {
                true
            }
        }
    })

    val itemBind: OnItemBindClass<Any> = OnItemBindClass<Any>()
            .map(SimpleInfo::class.java, BR.item, R.layout.row_simple_info)
            .map(ElectricityInfo::class.java, BR.item, R.layout.row_electricity_info)
            .map(TelephonesInfo::class.java, BR.item, R.layout.row_telephones_info)
            .map(VaccinationInfo::class.java, BR.item, R.layout.row_vaccination_info)
            .map(Divider::class.java, ItemBinding.VAR_NONE, R.layout.info_divider)

    fun getCountry(name: String): LiveData<Country> {
        return repository.getCountry(name)
    }

    fun toggleFavorite() {
        if (countryName.value.isNotEmpty()) {
            val updatedFavorite = !isFavorite.value
            isFavorite.postValue(updatedFavorite)

            if (updatedFavorite) {
                preferences.addFavorite(countryName.value)
            } else {
                preferences.removeFavorite(countryName.value)
            }
        }
    }

    fun setup(country: Country) {
        countryName.value = country.name
        setupFavorite(country.name)
        setupImage(country.imageUrl)

        val newItems = getNewItems(country)
        items.update(newItems)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun setupFavorite(name: String) {
        isFavorite.value = preferences.favorites.contains(name)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun setupImage(url: String) {
        if (Patterns.WEB_URL.matcher(url).matches()) {
            image.value = url
        } else {
            image.value = context.getString(R.string.default_image_url)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun getNewItems(country: Country): List<Any> {

        val items = ObservableArrayList<Any>()

        items.add(SimpleInfo(formatTime(country.timezone), context.getDrawable(R.drawable.ic_time)))
        items.add(SimpleInfo(formatCurrency(country.currency), context.getDrawable(R.drawable.ic_currency)))
        items.add(SimpleInfo(formatVisa(country.visa), context.getDrawable(R.drawable.ic_visa)))
        items.add(SimpleInfo(formatTapWater(country.tapWater), context.getDrawable(R.drawable.ic_tap)))
        items.add(ElectricityInfo(context.getString(R.string.electricity, country.electricity.voltage, country.electricity.frequency), formatPlugs(country.electricity.plugs), context.getDrawable(R.drawable.ic_plug)))
        items.add(Divider())
        items.add(getTelephones(country.telephones))
        items.add(Divider())
        items.addAll(getVaccinationItems(country.vaccinations))

        return items
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun getTelephones(telephones: Telephones): Any {
        val prefix = formatCallingCode(telephones.prefix)
        val police = telephones.policeNumber
        val ambulance = telephones.ambulanceNumber

        return if (prefix.isNotEmpty() && police.isNotEmpty() && ambulance.isNotEmpty()) {
            TelephonesInfo(prefix, police, ambulance, context.getDrawable(R.drawable.ic_call))
        } else {
            SimpleInfo(context.getString(R.string.no_info_about_telephones), context.getDrawable(R.drawable.ic_call))
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun getVaccinationItems(vaccinations: Map<String, String>): ObservableArrayList<Any> {
        val items = ObservableArrayList<Any>()

        if (vaccinations.isEmpty()) {
            items.add(SimpleInfo(context.getString(R.string.you_dont_need_vaccinations), context.getDrawable(R.drawable.ic_vaccine)))
        } else {
            items.add(SimpleInfo(context.getString(R.string.you_may_need_vaccinations_for), context.getDrawable(R.drawable.ic_vaccine)))
            vaccinations.forEach { (key, value) -> items.add(VaccinationInfo(key, value)) }
        }

        return items
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun formatVisa(visa: Visa): String {
        return if (visa.info.isNotEmpty()) visa.info else context.getString(R.string.no_info_about_visa)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun formatCallingCode(callingCode: String): String {
        return if (callingCode.toIntOrNull() != null) context.getString(R.string.calling_code, callingCode) else ""
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun formatTime(timezone: String): String {
        val time = getCurrentTime(timezone)
        return if (time.isNotEmpty()) context.getString(R.string.its_x_now, time) else context.getString(R.string.no_info_about_time)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun formatTapWater(basicInfo: String): String {
        return if (basicInfo == "safe" || basicInfo == "not safe") context.getString(R.string.tap_water_is, basicInfo) else context.getString(R.string.no_info_about_tap_water)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun formatPlugs(plugs: List<String>): List<Plug> {
        return plugs.map { plugProvider.provide(it) }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun formatCurrency(currency: Currency): String {
        val displayCurrencyName = if (currency.name.isNotEmpty()) currency.name else currency.code
        val destinationCurrencyCode = currency.code
        val rate = currency.rate.value
        val originCurrencyCode = currency.rate.fromCurrencyCode

        val currencyInfo1 = context.getString(R.string.x_is_the_currency).format(displayCurrencyName)
        val currencyInfo2 = if (rate != 0.0 && originCurrencyCode.isNotEmpty() && destinationCurrencyCode.isNotEmpty())
            context.getString(R.string.currency_rate_info).format(originCurrencyCode, rate, destinationCurrencyCode)
        else
            ""

        return listOf(currencyInfo1, currencyInfo2).filter { it.isNotEmpty() }.joinToString("\n")
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun getCurrentTime(timezoneId: String): String {
        return try {
            val timeZone = ZoneId.of(timezoneId)
            val zdt = ZonedDateTime.now(timeZone)
            val timePattern = context.getString(R.string.time_pattern)
            zdt.toLocalTime().format(DateTimeFormatter.ofPattern(timePattern))
        } catch (exception: DateTimeException) {
            ""
        }
    }
}