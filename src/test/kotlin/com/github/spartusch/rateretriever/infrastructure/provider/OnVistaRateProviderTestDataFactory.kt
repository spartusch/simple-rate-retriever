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
        jsonDataForInstrument(Instrument(name = symbol), amount = amount, currency = currency),
        jsonDataForInstrument(Instrument(isin = symbol), amount = amount, currency = currency),
        jsonDataForInstrument(Instrument(wkn = symbol), amount = amount, currency = currency),
        jsonDataForInstrument(Instrument(symbol = symbol), amount = amount, currency = currency),
        jsonDataForRelatedInstrument(Instrument(name = symbol), amount = amount, currency = currency),
        jsonDataForRelatedInstrument(Instrument(isin = symbol), amount = amount, currency = currency),
        jsonDataForRelatedInstrument(Instrument(wkn = symbol), amount = amount, currency = currency),
        jsonDataForRelatedInstrument(Instrument(symbol = symbol), amount = amount, currency = currency)
    )
}
