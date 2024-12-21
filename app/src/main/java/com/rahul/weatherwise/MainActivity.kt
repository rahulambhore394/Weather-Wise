package com.rahul.weatherwise

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.rahul.weatherwise.POJO.ModelClass
import com.rahul.weatherwise.Utilities.APIUtilities
import com.rahul.weatherwise.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        binding.mainLayout.visibility = View.GONE

        getCurrentLocation()

        binding.etSearchCity.setOnEditorActionListener { v, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                getCityWeather(binding.etSearchCity.text.toString())
                val view = this.currentFocus
                if (view != null) {
                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    binding.etSearchCity.clearFocus()
                }
                true
            } else false
        }
    }

    private fun getCityWeather(cityName: String) {
        binding.progressBar.visibility = View.VISIBLE
        APIUtilities.getAPIInterface()?.getCityWeatherData(cityName, API_KEY)
            ?.enqueue(object : Callback<ModelClass> {
                override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        setDataOnView(response.body())
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Invalid city name or no data found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(applicationContext, "Error: ${t.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    private fun getCurrentLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission()
                    return
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: android.location.Location? = task.result
                    if (location == null) {
                        Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                    } else {
                        fetchCurrentLocationWeather(
                            location.latitude.toString(),
                            location.longitude.toString()
                        )
                    }
                }
            } else {
                Toast.makeText(this, "Turn on location services", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermission()
        }
    }

    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {
        binding.progressBar.visibility = View.VISIBLE
        APIUtilities.getAPIInterface()?.getCurrentWeatherData(latitude, longitude, API_KEY)
            ?.enqueue(object : Callback<ModelClass> {
                override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        setDataOnView(response.body())
                        //Toast.makeText(applicationContext, "Setting Data on View", Toast.LENGTH_SHORT).show()

                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Error fetching weather data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(applicationContext, "Error: ${t.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    private fun setDataOnView(body: ModelClass?) {
        if (body == null) return

        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm")
        val currentDate = sdf.format(Date())

        binding.tvDateAndTime.text = currentDate
        binding.tvDayMaxTemp.text = "Day ${kelvinToCelsius(body.main.temp_max)}°C"
        binding.tvDayMinTemp.text = "Night ${kelvinToCelsius(body.main.temp_min)}°C"
        binding.tvTemp.text = "${kelvinToCelsius(body.main.temp)}°C"
        binding.tvFeelsLike.text = "Feels Like ${kelvinToCelsius(body.main.feels_like)}°C"
        binding.tvWhetherType.text = body.weather[0].main
        binding.tvSunrise.text = timeStampToLocalDateFormat(body.sys.sunrise.toLong())
        binding.tvSunset.text = timeStampToLocalDateFormat(body.sys.sunset.toLong())
        binding.tvPressure.text = body.main.pressure.toString()
        binding.tvHumidity.text = "${body.main.humidity}%"
        binding.tvWindSpeed.text = "${body.wind.speed} m/s"
        binding.tvTempFarenhite.text =
            "${(kelvinToCelsius(body.main.temp) * 1.8 + 32).roundToInt()}°F"
        binding.etSearchCity.setText(body.name)

        updateUI(body.weather[0].id)
    }

    private fun updateUI(id: Int) {
        if (id in 200..232) {
            // Thunderstorm
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            //window.statusBarColor = resources.getColor(R.color.thunderstrom)
            binding.subLayout.background =
                ContextCompat.getDrawable(this, R.drawable.thunderstorm_bg)
            binding.ivWhatherBg.setImageResource(R.drawable.thunderstorm_bg)
            binding.ivWhetherIcon.setImageResource(R.drawable.storm_icon)
        } else if (id in 300..321) {
            // Drizzle
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            binding.subLayout.background = ContextCompat.getDrawable(this, R.drawable.drizzle_bg)
            binding.ivWhatherBg.setImageResource(R.drawable.drizzle_bg)
            binding.ivWhetherIcon.setImageResource(R.drawable.cloudy)
        } else if (id in 500..531) {
            // rain
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            binding.subLayout.background = ContextCompat.getDrawable(this, R.drawable.rain_bg)
            binding.ivWhatherBg.setImageResource(R.drawable.rain_bg)
            binding.ivWhetherIcon.setImageResource(R.drawable.rain_icon)
        } else if (id in 600..620) {
            // snow
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            binding.subLayout.background = ContextCompat.getDrawable(this, R.drawable.snow_bg2)
            binding.ivWhatherBg.setImageResource(R.drawable.snow_bg2)
            binding.ivWhetherIcon.setImageResource(R.drawable.snow_icon)
        } else if (id in 700..781) {
            // atmosphare
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            binding.subLayout.background = ContextCompat.getDrawable(this, R.drawable.clouds_bg)
            binding.ivWhatherBg.setImageResource(R.drawable.clouds_bg)
            binding.ivWhetherIcon.setImageResource(R.drawable.atmosphare_icon)
        } else if (id == 800) {
            // clear
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            binding.subLayout.background = ContextCompat.getDrawable(this, R.drawable.atmosphere_bg)
            binding.ivWhatherBg.setImageResource(R.drawable.atmosphere_bg)
            binding.ivWhetherIcon.setImageResource(R.drawable.clear_icon)
        }


        binding.progressBar.visibility = View.GONE
        binding.mainLayout.visibility = View.VISIBLE
    }

    private fun timeStampToLocalDateFormat(timeStamp: Long): String {
        val localTime = Instant.ofEpochSecond(timeStamp)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return localTime.toString()
    }

    private fun kelvinToCelsius(temp: Double): Double {
        return (temp - 273.15).toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
        const val API_KEY = "dab3af44de7d24ae7ff86549334e45bd"
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
