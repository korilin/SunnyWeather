package com.korilin.sunnyweather

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import com.korilin.sunnyweather.databinding.*
import com.korilin.sunnyweather.logic.model.DailyResponse
import com.korilin.sunnyweather.logic.model.RealtimeResponse
import com.korilin.sunnyweather.logic.model.Weather
import com.korilin.sunnyweather.logic.model.getSky
import com.korilin.sunnyweather.ui.weather.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.*

const val INTENT_PARAM_LNG = "location_lng"
const val INTENT_PARAM_LAT = "location_lat"
const val INTENT_PARAM_PLACE_NAME = "place_name"

class WeatherActivity : BaseActivity() {

    val viewModel by lazy { ViewModelProvider(this).get(WeatherViewModel::class.java) }
    lateinit var viewBinding: ActivityWeatherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        windowStatusBarSetting()

        viewBinding = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if (viewModel.locationLng.isEmpty() || viewModel.locationLng.isBlank())
            viewModel.locationLng = intent.getStringExtra(INTENT_PARAM_LNG) ?: ""
        if (viewModel.locationLat.isEmpty() || viewModel.locationLat.isBlank())
            viewModel.locationLat = intent.getStringExtra(INTENT_PARAM_LAT) ?: ""
        if (viewModel.placeName.isEmpty() || viewModel.placeName.isBlank())
            viewModel.placeName = intent.getStringExtra(INTENT_PARAM_PLACE_NAME) ?: ""

        viewModel.weatherLiveData.observe(this) { result ->
            val weather = result.getOrNull()
            if (weather != null)
                showWeatherInfo(weather)
            else {
                Toast.makeText(this, "无法成功获取天气信息", Toast.LENGTH_LONG).show()
                result.exceptionOrNull()?.printStackTrace()
            }
            viewBinding.swipeRefresh.isRefreshing = false
        }

        viewBinding.apply {
            swipeRefresh.apply {
                setColorSchemeResources(R.color.colorPrimary)
                setOnRefreshListener {
                    refreshWeather()
                }
            }

            now.navBtn.setOnClickListener {
                drawerLayout.openDrawer(GravityCompat.START)
            }

            drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                }

                override fun onDrawerOpened(drawerView: View) {
                }

                override fun onDrawerClosed(drawerView: View) {
                    // 不懂
                    val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    manager.hideSoftInputFromWindow(drawerView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                }

                override fun onDrawerStateChanged(newState: Int) {
                }
            })
        }

        refreshWeather()
    }

    fun refreshWeather() {
        viewModel.refreshWeather(viewModel.locationLng, viewModel.locationLat)
        viewBinding.swipeRefresh.isRefreshing = true
    }

    private fun showWeatherInfo(weather: Weather) {
        val realtime = weather.realtime
        val daily = weather.daily

        viewBinding.apply {
            showNowInfo(now, realtime)
            showForecastInfo(forecast, daily)
            showLifeIndexInfo(lifeIndex, daily.lifeIndex)
            weatherLayout.visibility = View.VISIBLE
        }
    }

    private fun showNowInfo(nowBinding: NowBinding, realtime: RealtimeResponse.Realtime) = nowBinding.apply {
        placeName.text = viewModel.placeName
        currentTemperature.text = getString(R.string.temperature, realtime.temperature.toInt())
        currentSky.text = getSky(realtime.skycon).info
        currentAqi.text = getString(R.string.aqi, realtime.airQuality.aqi.chn.toInt())

        nowLayout.setBackgroundResource(getSky(realtime.skycon).bg)
    }

    private fun showForecastInfo(forecastBinding: ForecastBinding, daily: DailyResponse.Daily) = forecastBinding.apply {
        forecastLayout.removeAllViews()
        val days = daily.skycon.size
        for (i in 0 until days) {
            val skycon = daily.skycon[i]
            val temperature = daily.temperature[i]

            ForecastItemBinding.inflate(layoutInflater, forecastLayout, false).apply {
                dateInfo.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(skycon.date)

                getSky(skycon.value).also {
                    skyIcon.setImageResource(it.icon)
                    skyInfo.text = it.info
                }

                temperatureInfo.text =
                    getString(R.string.temperature_info, temperature.min.toInt(), temperature.max.toInt())

                forecastLayout.addView(root)
            }
        }
    }

    private fun showLifeIndexInfo(lifeIndexBinding: LifeIndexBinding, lifeIndex: DailyResponse.LifeIndex) =
        lifeIndexBinding.apply {
            coldRiskText.text = lifeIndex.coldRisk[0].desc
            dressingText.text = lifeIndex.dressing[0].desc
            ultravioletText.text = lifeIndex.ultraviolet[0].desc
            carWashingText.text = lifeIndex.carWashing[0].desc
        }

    companion object {
        fun getStartIntent(context: Context?, lng: String, lat: String, placeName: String) =
            Intent(context, WeatherActivity::class.java).apply {
                putExtra(INTENT_PARAM_LNG, lng)
                putExtra(INTENT_PARAM_LAT, lat)
                putExtra(INTENT_PARAM_PLACE_NAME, placeName)
            }
    }
}