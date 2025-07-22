package example.micronaut.weather;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherClientServerMockTest {
    public static final String FORECAST_EXAMPLE = """
        {
            "@context": [
                "https://geojson.org/geojson-ld/geojson-context.jsonld",
                {
                    "@version": "1.1",
                    "wx": "https://api.weather.gov/ontology#",
                    "geo": "http://www.opengis.net/ont/geosparql#",
                    "unit": "http://codes.wmo.int/common/unit/",
                    "@vocab": "https://api.weather.gov/ontology#"
                }
            ],
            "type": "Feature",
            "geometry": {
                "type": "Polygon",
                "coordinates": [
                    [
                        [
                            -74.0009,
                            40.702
                        ],
                        [
                            -73.9965,
                            40.7237
                        ],
                        [
                            -74.0251,
                            40.7269999
                        ],
                        [
                            -74.02959999999999,
                            40.705400000000004
                        ],
                        [
                            -74.0009,
                            40.702
                        ]
                    ]
                ]
            },
            "properties": {
                "units": "us",
                "forecastGenerator": "BaselineForecastGenerator",
                "generatedAt": "2025-07-22T09:38:04+00:00",
                "updateTime": "2025-07-22T05:48:44+00:00",
                "validTimes": "2025-07-21T23:00:00+00:00/P8DT2H",
                "elevation": {
                    "unitCode": "wmoUnit:m",
                    "value": 2.1336
                },
                "periods": [
                    {
                        "number": 1,
                        "name": "Overnight",
                        "startTime": "2025-07-22T05:00:00-04:00",
                        "endTime": "2025-07-22T06:00:00-04:00",
                        "isDaytime": false,
                        "temperature": 68,
                        "temperatureUnit": "F",
                        "temperatureTrend": "",
                        "probabilityOfPrecipitation": {
                            "unitCode": "wmoUnit:percent",
                            "value": 0
                        },
                        "windSpeed": "10 mph",
                        "windDirection": "NE",
                        "icon": "https://api.weather.gov/icons/land/night/few?size=medium",
                        "shortForecast": "Mostly Clear",
                        "detailedForecast": "Mostly clear, with a low around 68. Northeast wind around 10 mph."
                    },
                    {
                        "number": 2,
                        "name": "Tuesday",
                        "startTime": "2025-07-22T06:00:00-04:00",
                        "endTime": "2025-07-22T18:00:00-04:00",
                        "isDaytime": true,
                        "temperature": 77,
                        "temperatureUnit": "F",
                        "temperatureTrend": "",
                        "probabilityOfPrecipitation": {
                            "unitCode": "wmoUnit:percent",
                            "value": 15
                        },
                        "windSpeed": "10 mph",
                        "windDirection": "E",
                        "icon": "https://api.weather.gov/icons/land/day/rain_showers,20?size=medium",
                        "shortForecast": "Slight Chance Rain Showers",
                        "detailedForecast": "A slight chance of rain showers between 11am and 5pm. Mostly sunny. High near 77, with temperatures falling to around 75 in the afternoon. East wind around 10 mph. Chance of precipitation is 20%."
                    },
                    {
                        "number": 3,
                        "name": "Tuesday Night",
                        "startTime": "2025-07-22T18:00:00-04:00",
                        "endTime": "2025-07-23T06:00:00-04:00",
                        "isDaytime": false,
                        "temperature": 71,
                        "temperatureUnit": "F",
                        "temperatureTrend": "",
                        "probabilityOfPrecipitation": {
                            "unitCode": "wmoUnit:percent",
                            "value": 12
                        },
                        "windSpeed": "2 to 10 mph",
                        "windDirection": "E",
                        "icon": "https://api.weather.gov/icons/land/night/few?size=medium",
                        "shortForecast": "Mostly Clear",
                        "detailedForecast": "Mostly clear, with a low around 71. East wind 2 to 10 mph."
                    },
                    {
                        "number": 4,
                        "name": "Wednesday",
                        "startTime": "2025-07-23T06:00:00-04:00",
                        "endTime": "2025-07-23T18:00:00-04:00",
                        "isDaytime": true,
                        "temperature": 82,
                        "temperatureUnit": "F",
                        "temperatureTrend": "",
                        "probabilityOfPrecipitation": {
                            "unitCode": "wmoUnit:percent",
                            "value": 2
                        },
                        "windSpeed": "2 to 14 mph",
                        "windDirection": "SE",
                        "icon": "https://api.weather.gov/icons/land/day/sct?size=medium",
                        "shortForecast": "Mostly Sunny",
                        "detailedForecast": "Mostly sunny, with a high near 82. Southeast wind 2 to 14 mph."
                    },
                    {
                        "number": 5,
                        "name": "Wednesday Night",
                        "startTime": "2025-07-23T18:00:00-04:00",
                        "endTime": "2025-07-24T06:00:00-04:00",
                        "isDaytime": false,
                        "temperature": 72,
                        "temperatureUnit": "F",
                        "temperatureTrend": "",
                        "probabilityOfPrecipitation": {
                            "unitCode": "wmoUnit:percent",
                            "value": 0
                        },
                        "windSpeed": "7 to 14 mph",
                        "windDirection": "S",
                        "icon": "https://api.weather.gov/icons/land/night/sct?size=medium",
                        "shortForecast": "Partly Cloudy",
                        "detailedForecast": "Partly cloudy, with a low around 72. South wind 7 to 14 mph."
                    },
                    {
                        "number": 6,
                        "name": "Thursday",
                        "startTime": "2025-07-24T06:00:00-04:00",
                        "endTime": "2025-07-24T18:00:00-04:00",
                        "isDaytime": true,
                        "temperature": 86,
                        "temperatureUnit": "F",
                        "temperatureTrend": "",
                        "probabilityOfPrecipitation": {
                            "unitCode": "wmoUnit:percent",
                            "value": 0
                        },
                        "windSpeed": "7 to 15 mph",
                        "windDirection": "SW",
                        "icon": "https://api.weather.gov/icons/land/day/few?size=medium",
                        "shortForecast": "Sunny",
                        "detailedForecast": "Sunny, with a high near 86."
                    },
                    {
                        "number": 7,
                        "name": "Thursday Night",
                        "startTime": "2025-07-24T18:00:00-04:00",
                        "endTime": "2025-07-25T06:00:00-04:00",
                        "isDaytime": false,
                        "temperature": 76,
                        "temperatureUnit": "F",
                        "temperatureTrend": "",
                        "probabilityOfPrecipitation": {
                            "unitCode": "wmoUnit:percent",
                            "value": 0
                        },
                        "windSpeed": "14 mph",
                        "windDirection": "SW",
                        "icon": "https://api.weather.gov/icons/land/night/few?size=medium",
                        "shortForecast": "Mostly Clear",
                        "detailedForecast": "Mostly clear, with a low around 76."
                    },
                    {
                        "number": 8,
                        "name": "Friday",
                        "startTime": "2025-07-25T06:00:00-04:00",
                        "endTime": "2025-07-25T18:00:00-04:00",
                        "isDaytime": true,
                        "temperature": 92,
                        "temperatureUnit": "F",
                        "temperatureTrend": "",
                        "probabilityOfPrecipitation": {
                            "unitCode": "wmoUnit:percent",
                            "value": 10
                        },
                        "windSpeed": "12 mph",
                        "windDirection": "SW",
                        "icon": "https://api.weather.gov/icons/land/day/few?size=medium",
                        "shortForecast": "Sunny",
                        "detailedForecast": "Sunny, with a high near 92."
                    },
                    {
                        "number": 9,
                        "name": "Friday Night",
                        "startTime": "2025-07-25T18:00:00-04:00",
                        "endTime": "2025-07-26T06:00:00-04:00",
                        "isDaytime": false,
                        "temperature": 79,
                        "temperatureUnit": "F",
                        "temperatureTrend": "",
                        "probabilityOfPrecipitation": {
                            "unitCode": "wmoUnit:percent",
                            "value": 24
                        },
                        "windSpeed": "6 to 12 mph",
                        "windDirection": "W",
                        "icon": "https://api.weather.gov/icons/land/night/tsra_hi,20?size=medium",
                        "shortForecast": "Slight Chance Showers And Thunderstorms",
                        "detailedForecast": "A slight chance of showers and thunderstorms between 8pm and 2am. Mostly cloudy, with a low around 79."
                    },
                    {
                        "number": 10,
                        "name": "Saturday",
                        "startTime": "2025-07-26T06:00:00-04:00",
                        "endTime": "2025-07-26T18:00:00-04:00",
                        "isDaytime": true,
                        "temperature": 86,
                        "temperatureUnit": "F",
                        "temperatureTrend": "",
                        "probabilityOfPrecipitation": {
                            "unitCode": "wmoUnit:percent",
                            "value": 37
                        },
                        "windSpeed": "6 to 9 mph",
                        "windDirection": "NE",
                        "icon": "https://api.weather.gov/icons/land/day/bkn/tsra_hi,40?size=medium",
                        "shortForecast": "Partly Sunny then Chance Showers And Thunderstorms",
                        "detailedForecast": "A chance of showers and thunderstorms after 2pm. Partly sunny, with a high near 86. Chance of precipitation is 40%."
                    },
                    {
                        "number": 11,
                        "name": "Saturday Night",
                        "startTime": "2025-07-26T18:00:00-04:00",
                        "endTime": "2025-07-27T06:00:00-04:00",
                        "isDaytime": false,
                        "temperature": 76,
                        "temperatureUnit": "F",
                        "temperatureTrend": "",
                        "probabilityOfPrecipitation": {
                            "unitCode": "wmoUnit:percent",
                            "value": 39
                        },
                        "windSpeed": "8 mph",
                        "windDirection": "E",
                        "icon": "https://api.weather.gov/icons/land/night/tsra_sct,40?size=medium",
                        "shortForecast": "Chance Showers And Thunderstorms",
                        "detailedForecast": "A chance of showers and thunderstorms before 8pm, then a chance of showers and thunderstorms. Mostly cloudy, with a low around 76. Chance of precipitation is 40%."
                    },
                    {
                        "number": 12,
                        "name": "Sunday",
                        "startTime": "2025-07-27T06:00:00-04:00",
                        "endTime": "2025-07-27T18:00:00-04:00",
                        "isDaytime": true,
                        "temperature": 82,
                        "temperatureUnit": "F",
                        "temperatureTrend": "",
                        "probabilityOfPrecipitation": {
                            "unitCode": "wmoUnit:percent",
                            "value": 43
                        },
                        "windSpeed": "6 to 10 mph",
                        "windDirection": "E",
                        "icon": "https://api.weather.gov/icons/land/day/rain_showers,40/tsra_hi,40?size=medium",
                        "shortForecast": "Chance Rain Showers then Chance Showers And Thunderstorms",
                        "detailedForecast": "A chance of rain showers before 2pm, then a chance of showers and thunderstorms. Partly sunny, with a high near 82. Chance of precipitation is 40%."
                    },
                    {
                        "number": 13,
                        "name": "Sunday Night",
                        "startTime": "2025-07-27T18:00:00-04:00",
                        "endTime": "2025-07-28T06:00:00-04:00",
                        "isDaytime": false,
                        "temperature": 74,
                        "temperatureUnit": "F",
                        "temperatureTrend": "",
                        "probabilityOfPrecipitation": {
                            "unitCode": "wmoUnit:percent",
                            "value": 43
                        },
                        "windSpeed": "6 to 9 mph",
                        "windDirection": "NE",
                        "icon": "https://api.weather.gov/icons/land/night/tsra_hi,40/tsra_hi,30?size=medium",
                        "shortForecast": "Chance Showers And Thunderstorms",
                        "detailedForecast": "A chance of showers and thunderstorms before 8pm, then a chance of showers and thunderstorms. Partly cloudy, with a low around 74. Chance of precipitation is 40%."
                    },
                    {
                        "number": 14,
                        "name": "Monday",
                        "startTime": "2025-07-28T06:00:00-04:00",
                        "endTime": "2025-07-28T18:00:00-04:00",
                        "isDaytime": true,
                        "temperature": 85,
                        "temperatureUnit": "F",
                        "temperatureTrend": "",
                        "probabilityOfPrecipitation": {
                            "unitCode": "wmoUnit:percent",
                            "value": 18
                        },
                        "windSpeed": "6 to 9 mph",
                        "windDirection": "NE",
                        "icon": "https://api.weather.gov/icons/land/day/rain_showers,20/tsra_hi,20?size=medium",
                        "shortForecast": "Slight Chance Rain Showers then Slight Chance Showers And Thunderstorms",
                        "detailedForecast": "A slight chance of rain showers before 8am, then a slight chance of showers and thunderstorms. Mostly sunny, with a high near 85."
                    }
                ]
            }
        }
        """;
    public static final String POINTS_EXAMPLE = """
        {
            "@context": [
                "https://geojson.org/geojson-ld/geojson-context.jsonld",
                {
                    "@version": "1.1",
                    "wx": "https://api.weather.gov/ontology#",
                    "s": "https://schema.org/",
                    "geo": "http://www.opengis.net/ont/geosparql#",
                    "unit": "http://codes.wmo.int/common/unit/",
                    "@vocab": "https://api.weather.gov/ontology#",
                    "geometry": {
                        "@id": "s:GeoCoordinates",
                        "@type": "geo:wktLiteral"
                    },
                    "city": "s:addressLocality",
                    "state": "s:addressRegion",
                    "distance": {
                        "@id": "s:Distance",
                        "@type": "s:QuantitativeValue"
                    },
                    "bearing": {
                        "@type": "s:QuantitativeValue"
                    },
                    "value": {
                        "@id": "s:value"
                    },
                    "unitCode": {
                        "@id": "s:unitCode",
                        "@type": "@id"
                    },
                    "forecastOffice": {
                        "@type": "@id"
                    },
                    "forecastGridData": {
                        "@type": "@id"
                    },
                    "publicZone": {
                        "@type": "@id"
                    },
                    "county": {
                        "@type": "@id"
                    }
                }
            ],
            "id": "https://api.weather.gov/points/40.7128,-74.006",
            "type": "Feature",
            "geometry": {
                "type": "Point",
                "coordinates": [
                    -74.006,
                    40.7128
                ]
            },
            "properties": {
                "@id": "https://api.weather.gov/points/40.7128,-74.006",
                "@type": "wx:Point",
                "cwa": "OKX",
                "forecastOffice": "https://api.weather.gov/offices/OKX",
                "gridId": "OKX",
                "gridX": 33,
                "gridY": 35,
                "forecast": "https://api.weather.gov/gridpoints/OKX/33,35/forecast",
                "forecastHourly": "https://api.weather.gov/gridpoints/OKX/33,35/forecast/hourly",
                "forecastGridData": "https://api.weather.gov/gridpoints/OKX/33,35",
                "observationStations": "https://api.weather.gov/gridpoints/OKX/33,35/stations",
                "relativeLocation": {
                    "type": "Feature",
                    "geometry": {
                        "type": "Point",
                        "coordinates": [
                            -74.0279259,
                            40.745251
                        ]
                    },
                    "properties": {
                        "city": "Hoboken",
                        "state": "NJ",
                        "distance": {
                            "unitCode": "wmoUnit:m",
                            "value": 4053.8855920378
                        },
                        "bearing": {
                            "unitCode": "wmoUnit:degree_(angle)",
                            "value": 152
                        }
                    }
                },
                "forecastZone": "https://api.weather.gov/zones/forecast/NYZ072",
                "county": "https://api.weather.gov/zones/county/NYC061",
                "fireWeatherZone": "https://api.weather.gov/zones/fire/NYZ212",
                "timeZone": "America/New_York",
                "radarStation": "KDIX"
            }
        }""";

    @Test
    void weatherWithServerMock() {
        Map<String, Object> mockServerConfig = Map.of(
            "spec.name", "WeatherClientServerMockTest"
        );
        try (EmbeddedServer mockServer = ApplicationContext.run(EmbeddedServer.class, mockServerConfig)) {
            Map<String, Object> serverConfig = Map.of(
                "micronaut.http.services.weather.url", mockServer.getURL().toString()
            );
            try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class, serverConfig)) {
                double newYorkLatitude = 40.712776;
                double newYorkLongitude = -74.005974;  // Negative for West
                WeatherClient weatherClient = server.getApplicationContext().getBean(WeatherClient.class);
                Map<String, Object> rsp = assertDoesNotThrow(() -> weatherClient.getPoints(newYorkLatitude, newYorkLongitude));
                assertNotNull(rsp);

                Optional<Forecast> forecastOptional = assertDoesNotThrow(() -> weatherClient.getForecast(newYorkLatitude, newYorkLongitude));
                assertTrue(forecastOptional.isPresent());
                Forecast forecast = forecastOptional.get();
                assertNotNull(forecast);

                String forecastTxt = WeatherUtils.formatForecast(forecast);
                assertNotNull(forecastTxt);
            }
        }
    }

    @Requires(property = "spec.name", value = "WeatherClientServerMockTest")
    @Controller
    static class WeatherController {
        @Get("/points/{latitude},{longitude}")
        String getPoints(@PathVariable double latitude, @PathVariable double longitude) {
            return POINTS_EXAMPLE;
        }

        @Get("/gridpoints/{gridId}/{gridX},{gridY}/forecast")
        String getForecast(@PathVariable String gridId,
                             @PathVariable String gridX,
                             @PathVariable String gridY) {
            return FORECAST_EXAMPLE;
        }
    }
}
