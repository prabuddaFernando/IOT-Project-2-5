package com.dsv.iot25

import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator
import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator.ProgressTextAdapter
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

    private val pub_topics = arrayOf("group/sensors/humidity")

    private var client: MqttAndroidClient? = null

    private var circularProgress : CircularProgressIndicator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        circularProgress = findViewById(R.id.circular_progress)

        var textAdapter =
            ProgressTextAdapter { time ->
                "${time.toInt()} %"
            }
        circularProgress?.setProgressTextAdapter(textAdapter)

        val seekBar = findViewById<SeekBar>(R.id.humiditifiSeekBar)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        connectToServer()
    }

    private fun connectToServer(){
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

                if(client != null){
                    client?.setCallback(object : MqttCallbackExtended {



                        override fun connectionLost(cause: Throwable?) {
                            println("The Connection was lost.")
                        }

                        override fun messageArrived(topic: String?, message: MqttMessage?) {
                            if (message?.payload!= null){
                                val messageString: String = message.toString()
                                println("###### Incoming message: $messageString")
                                setHumidityValues(messageString.toDouble())
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

    private fun setHumidityValues(humidity:Double){
        circularProgress?.setProgress(humidity, 100.0)
    }

    private fun subscribe(topicToSubscribe: Array<String>) {
        val qos = intArrayOf(1)
        try {
            val subToken = client!!.subscribe(topicToSubscribe, qos)
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


    fun updateThreadPolicy() {
        val policy = ThreadPolicy.Builder()
            .permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

//    fun run(command: String?): String? {
//        updateThreadPolicy()
//        var responseData: String? = null
//        val hostname = "130.237.177.216"
//        val username = "pi"
//        val password = "IoT@2021"
//        try {
//            val conn = Connection(hostname) //init connection
//            conn.connect() //start connection to the hostname
//            val isAuthenticated: Boolean = conn.authenticateWithPassword(
//                username,
//                password
//            )
//            if (isAuthenticated == false) throw IOException("Authentication failed.")
//            val sess: Session = conn.openSession()
//            sess.execCommand(command)
//            val stdout: InputStream = StreamGobbler(sess.getStdout())
//            val br = BufferedReader(InputStreamReader(stdout))
//            val stringBuilder = StringBuilder()
//            //reads text
//            while (true) {
//                val line = br.readLine() ?: break // read line
//                println(line)
//                stringBuilder.append(line.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
//                    .toTypedArray())
//            }
//            responseData = stringBuilder.toString()
//            /* Show exit status, if available (otherwise "null") */System.out.println("ExitCode: " + sess.getExitStatus())
//            sess.close() // Close this session
//            conn.close()
//        } catch (e: IOException) {
//            e.printStackTrace(System.err)
//            //            System.exit(2);
//        }
//        return responseData
//    }




}