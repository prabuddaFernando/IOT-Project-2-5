package com.dsv.iot25

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage


class EyeTrackingService : Service() {


    val pub_topic_eye_blink_rate = "group/eye_blink/counter"
    private val SERVER_URI = "tcp://test.mosquitto.org:1883"
    private val TAG = "EyeTrackingService"

    private var client: MqttAndroidClient? = null

    private var notificationManager: NotificationManager? = null

    override fun onCreate() {
        super.onCreate()
         notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action){
            Actions.START.toString()-> {
                start()
            }
            Actions.STOP.toString() -> {
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }



    private fun start() {
        val notification =
            NotificationCompat.Builder(this,"notification_running_channel")
                .setSmallIcon(R.drawable.ic_eye_notification)
                .setContentText("Eye Blinking Rate")
                .setContentText("Eye Blinking Fetching Data")
                .build()

        startForeground(1,notification)
//        connectToServer()
    }

    private fun connectToServer() {
        CoroutineScope(Dispatchers.IO).launch {
            val clientId = MqttClient.generateClientId()
            client = MqttAndroidClient(
                this@EyeTrackingService, SERVER_URI,
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
                                val messageString: String = message.toString()
                                println("###### Incoming message: $messageString")
                                updateNotificationMessage(messageString)
                            }
                        }

                        override fun deliveryComplete(token: IMqttDeliveryToken?) {

                        }

                        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                            if (reconnect) {
                                println("Reconnected to : $serverURI")
                                // Re-subscribe as we lost it due to new session
                                subscribe()
                            } else {
                                println("Connected to: $serverURI")
                                subscribe()
                            }
                        }

                    })
                }
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    private fun updateNotificationMessage(message: String) {
       // TODO

    }


    private fun subscribe() {
        try {
            val subToken = client!!.subscribe(pub_topic_eye_blink_rate, 1)
            subToken.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    println("Subscription successful to topic: $pub_topic_eye_blink_rate")
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    println("Failed to subscribe to topic: $pub_topic_eye_blink_rate")
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using  wildcards
                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    enum class Actions{
        START,STOP
    }
}