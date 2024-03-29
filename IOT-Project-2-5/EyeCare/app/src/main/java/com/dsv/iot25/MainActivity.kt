package com.dsv.iot25

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator
import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator.ProgressTextAdapter
import ch.ethz.ssh2.Connection
import ch.ethz.ssh2.Session
import ch.ethz.ssh2.StreamGobbler
import com.github.mikephil.charting.charts.LineChart
import com.google.android.material.chip.Chip
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
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader


class MainActivity : AppCompatActivity() {


    val room_humidity_threshold_low = 30
    val room_humidity_threshold_high = 50
    val eye_blinking_rate_threshold_low = 15
    val eye_blinking_rate_threshold_high = 50

    private val SERVER_URI = "tcp://test.mosquitto.org:1883"
    private val TAG = "MainActivity"


    val pub_topic_humidity = "group/sensors/humidity"

    //    val pub_topic_temerature = "group/sensors/temperature"
    val pub_topic_eye_blink_rate = "group/eye_blink/counter"
    val pub_topic_humiditifyer_state = "group/humiditifyer/state"

    private val pub_topics = arrayOf(pub_topic_humidity, pub_topic_eye_blink_rate,pub_topic_humiditifyer_state)

    private var client: MqttAndroidClient? = null

    private var circularProgress: CircularProgressIndicator? = null
    private var blinkRate: TextView? = null
    private var startServiceBtn: Button? = null
    private var stopServiceBtn: Button? = null
    private var humiditifier: SwitchCompat? = null
    private var chart: LineChart? = null
    private var humStatusChip : Chip? = null

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
        humiditifier = findViewById(R.id.automate_humiditifier_switch)
        humStatusChip = findViewById(R.id.chip_status_active)


        humiditifier?.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // Switch is ON
                CoroutineScope(IO).launch {
                    runProcessToRaspberry("python3 eyecare/humiditifier-on.py")
                }
            } else {
                // Switch is OFF
                CoroutineScope(IO).launch {
                    runProcessToRaspberry("python3 eyecare/humiditifier-off.py")
                }
            }
        }

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
                                        println("###### Incoming message: $messageString $topic")
                                        setHumidityValues(messageString.toDouble())
                                    }
//                                    pub_topic_temerature-> {
//                                        val messageString: String = message.toString()
//                                        println("###### Incoming message: $messageString")
//                                        setTemperatureValues(messageString.toDouble())
//                                    }

                                    pub_topic_eye_blink_rate -> {
                                        val messageString: String = message.toString()
                                        println("###### Incoming message: $messageString $topic")
                                        setBlinkRateValue(messageString.toDouble())
                                    }

                                    pub_topic_humiditifyer_state -> {
                                        val messageString: String = message.toString()
                                        println("###### Incoming message: $messageString $topic")
                                        var isActuatorTurnOn = messageString.toInt()
                                        if ( isActuatorTurnOn == 1 ){
                                            humStatusChip?.setChipBackgroundColorResource(R.color.green)
                                            humStatusChip?.text = "Active"
                                        }else{
                                            humStatusChip?.setChipBackgroundColorResource(R.color.grey_12)
                                            humStatusChip?.text = "Deactivated"
                                        }
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
    }

    private fun runProcessToRaspberry(command: String?): String? {
        updateThreadPolicy()
        var responseData: String? = null
        val hostname = "192.168.0.116"
        val username = "user"
        val password = "Test@1234"
        try {
            val conn = Connection(hostname) //init connection
            conn.connect() //start connection to the hostname
            val isAuthenticated: Boolean = conn.authenticateWithPassword(
                username,
                password
            )
            if (isAuthenticated == false) throw IOException("Authentication failed.")
            val sess: Session = conn.openSession()
            sess.execCommand(command)
            val stdout: InputStream = StreamGobbler(sess.getStdout())
            val br = BufferedReader(InputStreamReader(stdout))
            val stringBuilder = StringBuilder()
            //reads text
            while (true) {
                val line = br.readLine() ?: break // read line
                println(line)
                stringBuilder.append(line.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray())
            }
            responseData = stringBuilder.toString()
            /* Show exit status, if available (otherwise "null") */System.out.println("ExitCode: " + sess.getExitStatus())
            sess.close() // Close this session
            conn.close()
        } catch (e: IOException) {
            e.printStackTrace(System.err)
        } catch (e: Exception){
            e.printStackTrace(System.err)
        }
        return responseData
    }

    var blinkingRate = 0.0
    var humidityValue = 0.0

    private fun getPendingIntent (): NotificationCompat.Action {
        val okIntent = Intent(this, MainActivity::class.java)
        okIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        okIntent.action = "OK_ACTION" // Add a unique action string
        val okPendingIntent = PendingIntent.getActivity(
            this, 0, okIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val okAction: NotificationCompat.Action = NotificationCompat.Action(
            R.drawable.baseline_check_circle_24,
            "OK",
            okPendingIntent
        )
        return okAction
    }

    private fun setBlinkRateValue(rate: Double) {
        if(rate!= 0.0 && blinkingRate == rate){
            return
        }

        blinkingRate = rate
        if(rate > 0){
            blinkRate?.text = "$rate Per Min"
        }
        if(blinkingRate != 0.0 && blinkingRate < eye_blinking_rate_threshold_low) {

            val okIntent = getPendingIntent()
            // remind user to blink
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notification =
                NotificationCompat.Builder(this, "notification_running_channel_alert")
                    .setSmallIcon(R.drawable.ic_eye_notification)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentText("Hydrated?")
                    .addAction(okIntent)
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("Please blink more often! You may be experiencing dehydration. Your eye blinking rate is " +
                                " $rate Per Min. This is not healthy!")
                    )
                    .build()

            notificationManager.notify(1, notification)
        }else if(blinkingRate != 0.0 && blinkingRate > eye_blinking_rate_threshold_high){
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val okIntent = getPendingIntent()
            val notification =
                NotificationCompat.Builder(this, "notification_running_channel_alert")
                    .setSmallIcon(R.drawable.ic_eye_notification)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentText("Feel tired?")
                    .addAction(okIntent)
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("You seem to be fed up. Take a break and get some rest!")
                    )
                    .build()

            notificationManager.notify(2, notification)
        } else if(blinkingRate != 0.0) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notification =
                NotificationCompat.Builder(this, "notification_running_channel")
                    .setSmallIcon(R.drawable.ic_eye_notification)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentText("Eye Blinking Rate Per min : $rate")
                    .build()

            notificationManager.notify(1, notification)
        }
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