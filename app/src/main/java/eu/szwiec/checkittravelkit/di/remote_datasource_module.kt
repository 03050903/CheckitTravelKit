package eu.szwiec.checkittravelkit.di

import com.squareup.moshi.Moshi
import eu.szwiec.checkittravelkit.di.ApiProperties.CURRENCY_CONVERTER_URL
import eu.szwiec.checkittravelkit.di.ApiProperties.SHERPA_URL
import eu.szwiec.checkittravelkit.repository.remote.*
import org.koin.dsl.module.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val remoteDatasourceModule = module {
    single { createCurrencyConverterService() }
    single { createSherpaService() }
}

object ApiProperties {
    const val SHERPA_URL = "https://api.joinsherpa.com/v2/"
    const val CURRENCY_CONVERTER_URL = "https://free.currencyconverterapi.com/api/v5/"
}

fun createSherpaService(): SherpaService {
    return Retrofit.Builder()
            .baseUrl(SHERPA_URL)
            .addConverterFactory(MoshiConverterFactory.create(
                    Moshi.Builder().add(VisaAdapter()).build()
            ))
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .build()
            .create(SherpaService::class.java)
}

fun createCurrencyConverterService(): CurrencyConverterService {
    return Retrofit.Builder()
            .baseUrl(CURRENCY_CONVERTER_URL)
            .addConverterFactory(MoshiConverterFactory.create(
                    Moshi.Builder().add(RateAdapter()).build()
            ))
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .build()
            .create(CurrencyConverterService::class.java)
}


