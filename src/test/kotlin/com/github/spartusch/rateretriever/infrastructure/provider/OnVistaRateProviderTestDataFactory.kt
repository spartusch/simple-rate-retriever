package com.github.spartusch.rateretriever.infrastructure.provider

private data class Instrument(
    val name: String = "_",
    val isin: String? = null,
    val wkn: String? = null,
    val symbol: String? = null
)

internal object OnVistaRateProviderTestDataFactory {
    const val assetUrl = "http://asset"
    const val searchPageContent = "\"snapshotlink\":\"$assetUrl\""

    fun searchUrl(symbol: String) = "http://search?q=$symbol"

    private fun jsonDataForInstrument(instrument: Instrument, amount: String, currency: String) = """
            <script id="__NEXT_DATA__" type="application/json">{"props":{"pageProps":{"data":{"snapshot":{
            "instrument": {
                "name": "${instrument.name}",
                "isin": "${instrument.isin}",
                "wkn": "${instrument.wkn}",
                "symbol": "${instrument.symbol}"
            },
            "quote": {
                "isoCurrency": "$currency",
                "last": $amount
            },
            "relatedInstrumentItemList": [{
                "instrument": {
                    "name": "foo",
                    "isin": "foo",
                    "wkn": "foo",
                    "symbol": "foo"
                },
                "quote": {
                    "isoCurrency": "foo",
                    "last": 123
                }
            }]
            }}}}}</script>
            """

    private fun jsonDataForRelatedInstrument(instrument: Instrument, amount: String, currency: String) = """
            <script id="__NEXT_DATA__" type="application/json">{"props":{"pageProps":{"data":{"snapshot":{
            "instrument": {
                "name": "foo",
                "isin": "foo",
                "wkn": "foo",
                "symbol": "foo"
            },
            "quote": {
                "isoCurrency": "foo",
                "last": 123
            },
            "relatedInstrumentItemList": [{
                "instrument": {
                    "name": "foo",
                    "isin": "foo",
                    "wkn": "foo",
                    "symbol": "foo"
                },
                "quote": {
                    "isoCurrency": "foo",
                    "last": 123
                }
            }, {
                "instrument": {
                    "name": "${instrument.name}",
                    "isin": "${instrument.isin}",
                    "wkn": "${instrument.wkn}",
                    "symbol": "${instrument.symbol}"
                },
                "quote": {
                    "isoCurrency": "$currency",
                    "last": $amount
                }
            }]
            }}}}}</script>
            """

    fun assetPageContents(symbol: String, amount: String, currency: String) = listOf(
        """
            foo <span property="schema:priceCurrency">$currency</span> something
            <meta property="schema:price" content="$amount"> bar
        """,
        "foo <span class=\"price\">${amount.replace('.', ',')} $currency</span> bar", // German locale
        """
            foo <data class=\"text-nowrap\" value=\"$amount\">
            ${amount.replace('.', ',')}
            <span>            
            <!-- -->
            $currency
            </span>
        """,
        jsonDataForInstrument(Instrument(name = symbol), amount, currency),
        jsonDataForInstrument(Instrument(isin = symbol), amount, currency),
        jsonDataForInstrument(Instrument(wkn = symbol), amount, currency),
        jsonDataForInstrument(Instrument(symbol = symbol), amount, currency),
        jsonDataForRelatedInstrument(Instrument(name = symbol), amount, currency),
        jsonDataForRelatedInstrument(Instrument(isin = symbol), amount, currency),
        jsonDataForRelatedInstrument(Instrument(wkn = symbol), amount, currency),
        jsonDataForRelatedInstrument(Instrument(symbol = symbol), amount, currency),
    )
}
