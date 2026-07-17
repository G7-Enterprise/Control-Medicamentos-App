package com.carlos.controlmedicamentos

data class CountryCurrency(
    val country: String,
    val currencyCode: String,
    val currencySymbol: String
)

object CountryCurrencyCatalog {
    const val DEFAULT_COUNTRY = "Nicaragua"
    const val DEFAULT_CURRENCY_CODE = "NIO"
    const val DEFAULT_CURRENCY_SYMBOL = "C$"

    val spanishSpeakingCountries: List<CountryCurrency> = listOf(
        CountryCurrency("Argentina", "ARS", "$"),
        CountryCurrency("Bolivia", "BOB", "Bs"),
        CountryCurrency("Chile", "CLP", "$"),
        CountryCurrency("Colombia", "COP", "$"),
        CountryCurrency("Costa Rica", "CRC", "₡"),
        CountryCurrency("Cuba", "CUP", "$"),
        CountryCurrency("Ecuador", "USD", "$"),
        CountryCurrency("El Salvador", "USD", "$"),
        CountryCurrency("España", "EUR", "€"),
        CountryCurrency("Guatemala", "GTQ", "Q"),
        CountryCurrency("Honduras", "HNL", "L"),
        CountryCurrency("México", "MXN", "$"),
        CountryCurrency(DEFAULT_COUNTRY, DEFAULT_CURRENCY_CODE, DEFAULT_CURRENCY_SYMBOL),
        CountryCurrency("Panamá", "PAB", "B/."),
        CountryCurrency("Paraguay", "PYG", "₲"),
        CountryCurrency("Perú", "PEN", "S/"),
        CountryCurrency("Puerto Rico", "USD", "$"),
        CountryCurrency("República Dominicana", "DOP", "RD$"),
        CountryCurrency("Uruguay", "UYU", "\$U"),
        CountryCurrency("Venezuela", "VES", "Bs")
    )

    fun forCountry(country: String): CountryCurrency {
        return spanishSpeakingCountries.firstOrNull { it.country == country }
            ?: spanishSpeakingCountries.first { it.country == DEFAULT_COUNTRY }
    }

    fun symbolFor(country: String, storedSymbol: String): String {
        return storedSymbol.ifBlank { forCountry(country).currencySymbol }
    }
}

fun formatMoney(amount: Double, currencySymbol: String): String {
    return "%s %.2f".format(java.util.Locale.getDefault(), currencySymbol.ifBlank { CountryCurrencyCatalog.DEFAULT_CURRENCY_SYMBOL }, amount)
}
