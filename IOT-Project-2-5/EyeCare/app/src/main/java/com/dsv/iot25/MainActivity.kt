package com.dsv.iot25

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator
import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator.ProgressTextAdapter
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage


class MainActivity : AppCompatActivity() {


    private val SERVER_URI = "tcp://test.mosquitto.org:1883"
    private val TAG = "MainActivity"


    val pub_topic_humidity = "group/sensors/humidity"

    //    val pub_topic_temerature = "group/sensors/temperature"
    val pub_topic_eye_blink_rate = "group/eye_blink/counter"

    private val pub_topics = arrayOf(pub_topic_humidity, pub_topic_eye_blink_rate)

    private var client: MqttAndroidClient? = null

    private var circularProgress: CircularProgressIndicator? = null
    private var blinkRate: TextView? = null
    private var startServiceBtn: Button? = null
    private var stopServiceBtn: Button? = null
    private var humiditifier: SwitchCompat? = null
    private var chart: LineChart? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.my_toolbar))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        circularProgress = findViewById(R.id.circular_progress)
        blinkRate = findViewById(R.id.textViewBlinkRate)
        startServiceBtn = findViewById(R.id.startServiceButton)
        stopServiceBtn = findViewById(R.id.stopServiceButton)
        humiditifier = findViewById(R.id.switch1)

        var textAdapter =
            ProgressTextAdapter { time ->
                "${time.toInt()} %"
            }
        circularProgress?.setProgressTextAdapter(textAdapter)


        connectToServer()
        startServiceBtn?.setOnClickListener {
            startService()
        }
        stopServiceBtn?.setOnClickListener {
            stopService()
        }

//        chart = findViewById<LineChart>(R.id.chart)
//
//        val entries: ArrayList<Entry> = ArrayList()
//
//        var list = listOf<Pair<Float, Float>>(
//            Pair(0.5f, 0.5f),
//            Pair(1.5f, 0.8f),
//            Pair(2.5f, 1.5f),
//            Pair(3.5f, 2.9f),
//            Pair(4.5f, 2.5f),
//            Pair(5.5f, 3.5f),
//        )
//        for (data in list) {
//            entries.add(Entry(data.first, data.second))
//        }
//
//        var  dataSet =  LineDataSet(entries, "Blinking Rate Vs Humidity") // add entries to dataset
//        dataSet.color = Color.GREEN
//        dataSet.valueTextColor = Color.BLUE
//
//
//        val lineData = LineData(dataSet)
//        lineData.dataSets.first().label = "Humidity"
//        lineData.dataSets.last().label = "Blinking Rate"
//        chart?.background = getDrawable(R.color.white)
//        chart?.data = lineData
//        chart?.invalidate()

    }

    private fun startService() {
        Intent(applicationContext, EyeTrackingService::class.java).also {
            it.action = EyeTrackingService.Actions.START.toString()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(it);
            } else {
                startService(it);
            }
        }
    }

    private fun stopService() {
        Intent(applicationContext, EyeTrackingService::class.java).also {
            it.action = EyeTrackingService.Actions.STOP.toString()
            stopService(it)
        }
    }

    private fun connectToServer() {
        CoroutineScope(IO).launch {
            val clientId = MqttClient.generateClientId()
            client = MqttAndroidClient(
                this@MainActivity, SERVER_URI,
                clientId
            )
            try {
                val token = client?.connect()
                token?.actionCallback = object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken) {

                        // We are connected
                        Log.d(TAG, "onSuccess")
                        println(TAG + " Success. Connected to " + SERVER_URI)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                        // Something went wrong e.g. connection timeout or firewall problems
                        Log.d(TAG, "onFailure")
                        println(
                            TAG + " Oh no! Failed to connect to " +
                                    SERVER_URI
                        )
                    }
                }

                if (client != null) {
                    client?.setCallback(object : MqttCallbackExtended {

                        override fun connectionLost(cause: Throwable?) {
                            println("The Connection was lost.")
                        }

                        override fun messageArrived(topic: String?, message: MqttMessage?) {
                            if (message?.payload != null) {

                                when (topic) {
                                    pub_topic_humidity -> {
                                        val messageString: String = message.toString()
                                        println("###### Incoming message: $messageString")
                                        setHumidityValues(messageString.toDouble())
                                    }
//                                    pub_topic_temerature-> {
//                                        val messageString: String = message.toString()
//                                        println("###### Incoming message: $messageString")
//                                        setTemperatureValues(messageString.toDouble())
//                                    }

                                    pub_topic_eye_blink_rate -> {
                                        val messageString: String = message.toString()
                                        println("###### Incoming message: $messageString")
                                        setBlinkRateValue(messageString.toDouble())
                                    }

                                }


                            }
                        }

                        override fun deliveryComplete(token: IMqttDeliveryToken?) {

                        }

                        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                            if (reconnect) {
                                println("Reconnected to : $serverURI")
                                // Re-subscribe as we lost it due to new session
                                subscribe(pub_topics)
                            } else {
                                println("Connected to: $serverURI")
                                subscribe(pub_topics)
                            }
                        }

                    })
                }
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    private fun setHumidityValues(humidity: Double) {
        humidityValue = humidity
        circularProgress?.setProgress(humidity, 100.0)
        humiditifier?.isChecked = humidity < 35
        if(humidity != 0.0 && humidity != 0.0) {
            
        }
    }

    private fun setTemperatureValues(humidity: Double) {
        // TODO
    }

    var blinkingRate = 0.0
    var humidityValue = 0.0

    private fun setBlinkRateValue(rate: Double) {
        blinkingRate = rate
        blinkRate?.text = "$rate Per Min"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification =
            NotificationCompat.Builder(this, "notification_running_channel")
                .setSmallIcon(R.drawable.ic_eye_notification)
                .setContentText("Eye Blinking Rate")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentText("Eye Blinking Rate Per min : $rate")
                .build()

        notificationManager?.notify(1, notification)
    }

    private fun subscribe(topicToSubscribe: Array<String>) {
        topicToSubscribe.forEach { topic ->
            try {
                val subToken = client!!.subscribe(topic, 1)
                subToken.actionCallback = object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken) {
                        println("Subscription successful to topic: $topicToSubscribe")
                    }

                    override fun onFailure(
                        asyncActionToken: IMqttToken,
                        exception: Throwable
                    ) {
                        println("Failed to subscribe to topic: $topicToSubscribe")
                        // The subscription could not be performed, maybe the user was not
                        // authorized to subscribe on the specified topic e.g. using  wildcards
                    }
                }
            } catch (e: MqttException) {
                e.printStackTrace()
            }

        }
    }


    fun updateThreadPolicy() {
        val policy = ThreadPolicy.Builder()
            .permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

}