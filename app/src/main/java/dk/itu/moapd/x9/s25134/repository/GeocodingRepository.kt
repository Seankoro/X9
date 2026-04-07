package dk.itu.moapd.x9.s25134.repository

import android.util.Log
import com.google.gson.annotations.SerializedName
import dk.itu.moapd.x9.s25134.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private data class ReverseGeocodeResponse(
    @SerializedName("display_name") val displayName: String?
)

private interface GeocodingService {
    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("api_key") apiKey: String
    ): ReverseGeocodeResponse
}

class GeocodingRepository {

    companion object {
        private const val TAG = "GeocodingRepository"
        private const val BASE_URL = "https://geocode.maps.co/"

        private val service: GeocodingService = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeocodingService::class.java)
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): String? {
        return try {
            service.reverseGeocode(lat, lng, BuildConfig.GEOCODING_API_KEY).displayName
        } catch (e: Exception) {
            Log.e(TAG, "Reverse geocoding failed for ($lat, $lng)", e)
            null
        }
    }
}
