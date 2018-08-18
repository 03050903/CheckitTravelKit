package eu.szwiec.checkittravelkit.vo

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.TypeConverters
import com.squareup.moshi.Json
import eu.szwiec.checkittravelkit.db.Converters

@Entity(primaryKeys = arrayOf("name"))
@TypeConverters(Converters::class)
data class Country(

        @Json(name = "id")
        val id: String,

        @Json(name = "name")
        val name: String,

        @Json(name = "timezone")
        val timezone: String = "",

        @Embedded(prefix = "electricity_")
        @Json(name = "electricity")
        val electricity: Electricity = Electricity(),

        @Embedded(prefix = "telephones_")
        @Json(name = "telephones")
        val telephones: Telephones = Telephones(),

        @Json(name = "tap_water")
        val tapWater: String = "",

        @Json(name = "vaccinations")
        val vaccinations: Map<String, String> = emptyMap(),

        @Embedded(prefix = "currency_")
        @Json(name = "currency")
        val currency: Currency = Currency(),

        @Json(name = "image_url")
        val imageUrl: String = "",

        @Json(name = "visa")
        var visa: String = ""
)