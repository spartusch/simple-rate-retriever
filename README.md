# Simple-Rate-Retriever

Retrieves current rates of ticker symbols. Supports Microsoft Excel's web queries.

## What is Simple-Rate-Retriever?

Simple-Rate-Retriever is a standalone application for the retrieval of current rates of ticker symbols. It does get the
rates from rate providers by forwarding requests and extracting rates. It does not store rates nor fetch rates other than
a currently requested rate. Simple-Rate-Retriever supports Microsoft Excel's web queries. This allows it to act as a
backend for Excel. It basically helps Excel to get current rates from the web.

## Installation

### Using Docker (preferred way)

In order to run Simple-Rate-Retriever using Docker you do need to do the following:

1) Get and install Docker (e.g., "Docker Desktop" from www.docker.com)
2) Download this repository
3) Run `./bin/run-container.sh` inside the repository
4) Go to http://localhost:18091/swagger-ui/index.html
5) Start using the endpoints

### Without using Docker

1) Ensure JDK 11 (or newer) is installed and JAVA_HOME is set
2) Clone this repository locally
3) Run `./bin/run.sh` inside the repository
4) Go to http://localhost:8080/swagger-ui/index.html
5) Select the server containing "localhost:8080" from the "Servers" dropdown
6) Start using the endpoints

## How to use it

There's an OpenAPI specification available for the endpoints provided by Simple-Rate-Retriever. This specification
is available at `src/main/resources/static/openapi.yml` or during runtime at http://localhost:18091/openapi.yml.

There is also a simple UI at http://localhost:18091/swagger-ui/index.html once Simple-Rate-Retriever is up and running.
You can query current rates using this UI as well as download web queries  to let Microsoft Excel query and import the rates.

The endpoints require you to provide: 
- providerId: ID of the provider you'd like to use (e.g., coinmarket or stockexchange)
- tickerSymbol: Symbol of which to retrieve the current rate of (e.g., BTC or IE00B4L5Y983)
- currencyCode: Currency of the rate (e.g., USD or EUR)
- locale: (Optional) The locale used for formatting, must be a language tag according to IETF BCP 47 (e.g., de-DE, defaults to en-US)

### How to use web queries in Microsoft Excel

- Download a web query using the appropriate endpoint (the one ending with `/iqy`)
- Open Microsoft Excel, go to the "Data" menu, select "Get External Data" and "Run Web Query" / "Run Saved Query", select the web query file and import it
- Microsoft Excel will now automatically import the current rate whenever it imports external data

## Rate providers

Simple-Rate-Retriever can support an arbitrary number of rate providers. You can query different providers by using their
providerIds. You can also add new providers very easily.

### Provider: CoinMarketCap

ProviderId:
`coinmarket`

Example requests:
- http://localhost:18091/rate/v1/coinmarket/BTC/USD (gets the current Bitcoin rate in USD)
- http://localhost:18091/rate/v1/coinmarket/ETH/EUR?locale=de-DE (gets the current Ethereum rate in EUR with German formatting)

One of the default providers is [CoinMarketCap](https://coinmarketcap.com/). CoinMarketCap provides rates for many cryptocurrencies.
You do need to have an API key in order to use this provider though. You can get an API key from https://coinmarketcap.com/api/. There are
paid plans for the keys but also a "basic plan" which is for free.  Let Simple-Rate-Provider know about your key once you have one.
Do this by either providing the key in an environment variable  called `CMC_API_KEY` or by creating the file `bin/secrets.vars` with
the following contents:
```
cmc_api_key=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```
`xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` should be replaced with your API key for CoinMarketCap. Please note that the file `bin/secrets.vars`
is only used when running a command from the `bin` directory, e.g., when running `./bin/run-container.sh`.

### How to add a new provider

Adding new providers is straightforward:
- Implement the interface `com.github.spartusch.rateretriever.rate.v1.provider.RateProvider` or inherit from `AbstractTimedRateProvider`.
  You can find the existing implementations at `src/main/kotlin/com/github/spartusch/rateretriever/rate/v1/provider`.
- Add your new provider to the properties at `src/main/resources/application.yml` under `simple-rate-retriever.providers`and assign it
  a providerId not yet used.
- That's it. You can now use your new providerId in requests.

## Monitoring

Simple-Rate-Retriever supports [Prometheus](https://prometheus.io/) as well as [Spring Boot Admin](https://github.com/codecentric/spring-boot-admin)
for monitoring. 

### Prometheus

An endpoint for Prometheus is provided at http://localhost:18091/actuator/prometheus.

### Spring Boot Admin

You can configure your Spring Boot Admin server in the file `bin/configuration.vars` before running `./bin/run-container.sh` or by setting
the environment variable SPRING_BOOT_ADMIN_CLIENT_URL.
By default, Simple-Rate-Retriever will try to find a Spring Boot Admin server at http://admin-server:8080 (when started
using `./bin/run-container.sh`) or http://localhost:18000 (when started using `./bin/run.sh`).  These locations will work out-of-the-box
when using the Spring Boot Admin server from https://github.com/spartusch/admin-server.
