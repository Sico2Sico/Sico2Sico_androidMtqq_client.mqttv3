package www.aiiage.com.mqttdemo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.zyyoona7.lib.hmacSHA1
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import okhttp3.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.jetbrains.anko.toast
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttClient
import com.google.gson.Gson


class MainActivity : AppCompatActivity() {

    //用户唯一id
    val clientId = "ailage"+System.currentTimeMillis()

    // 产品key
    val productKey = ""

    // 设备名称  唯一
    val deviceName = ""

    val timestamp = "789"

    //deviceSecret
    val  deviceSecret = ""


    // 端口号
    val host =  1883

    //域名
    val  tcpHost ="tcp://"+productKey+".iot-as-mqtt.cn-shanghai.aliyuncs.com"+":"+host

    val httpHost = "https://iot-auth.cn-shanghai.aliyuncs.com/auth/devicename"



    //需要订阅的主题
    val topic = "/"+productKey+"/"+deviceName+"/"+"map"



    //需要签名的参数
    val maplist = mapOf(Pair("productKey",productKey),
            Pair("deviceName",deviceName),
            Pair("clientId",clientId),
            Pair("timestamp",timestamp))


    var client : MqttClient?  = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        TcpConnect()
    }


    private fun initListen() {
        //按钮点击监听
        connect_tcp.setOnClickListener {
            //TcpConnect()
        }

    }


    /**
     *  Tcp 直连
     */
    private fun TcpConnect(){

        val mqttClientId = clientId + "|securemode=3,signmethod=hmacsha1,timestamp=${timestamp}|"
        val mqtt = MqttAndroidClient(applicationContext,tcpHost,mqttClientId)
        val mqttUsername = deviceName + "&" + productKey
        val mqttPassword = KeyToString(maplist).hmacSHA1(deviceSecret)

        //设置入参
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = false
        options.userName = mqttUsername
        options.password = mqttPassword.toCharArray()
        options.keepAliveInterval = 90

        //连接
        mqtt.setCallback(object :MqttCallbackExtended{
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                Log.i(this.toString(),"连接完成")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.i(this.toString(),"收到消息")
                Log.i(this.toString(),message.toString())

            }

            override fun connectionLost(cause: Throwable?) {
                Log.i(this.toString(),"连接断开")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.i(this.toString(),"连接deliveryComplete")
            }
        })


        //连接  状态回调监听
        mqtt.connect(options,null,object : IMqttActionListener{
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(this.toString(),"连接成功")

                //订阅主题
                mqtt.subscribe(topic,1,applicationContext,object :IMqttActionListener{
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.i(this.toString(),"订阅成功")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.i(this.toString(),"订阅失败")
                    }
                })
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(this.toString(),"连接失败")
            }
        })

    }


    /**
     * https 认证连接
     */
    private fun HttpsConnect() {

        //httpclient 请求初始化
        val okHttpClient =  OkHttpClient()

        val sign = KeyToString(maplist).hmacSHA1(deviceSecret)

        val requestBoy = FormBody.Builder()
                .add("productKey",productKey)
                .add("deviceName",deviceName)
                .add("clientId",clientId)
                .add("sign",sign)
                .add("timestamp",timestamp)
                .add("signmethod","hmacsha1")
                .add("resources","mqtt")
                .build()


        val requset = Request.Builder()
                .url(httpHost)
                .post(requestBoy)
                .build()

        val call = okHttpClient.newCall(requset)
        call.enqueue(object : okhttp3.Callback{
            override fun onFailure(call: Call?, e: IOException?) {
                Log.e("error","错误信息为"+e)
            }

            override fun onResponse(call: Call?, response: Response?) {
                if (response?.isSuccessful!!) {
                    try {
                        val result = response.body().string()
                        val gson = Gson()
                        val list = gson.fromJson(result, mapjson::class.java)

                        val iotid = list.data?.iotId
                        val iotToken = list.data?.iotToken
                        val host = list.data?.resources?.mqtt?.host
                        val port = list.data?.resources?.mqtt?.port

                        HttpsToTcpConnect(iotid!!,iotToken!!,host!!,port!!)

                    } catch (e: Exception) {
                       Log.e(this.toString(),e.toString())
                    }

                }

            }
        })
    }



    // Https认证连接 - ---》2
    private  fun  HttpsToTcpConnect(iotId:String,iotToken:String,host:String,port:Int){
        val mqtt = MqttAndroidClient(applicationContext,tcpHost,clientId)

        //设置入参
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = false
        options.userName = iotId
        options.password = iotToken.toCharArray()
        options.keepAliveInterval = 90

        //连接  状态回调监听
        mqtt.connect(options,null,object : IMqttActionListener{
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(this.toString(),"连接成功")

                //订阅主题
                mqtt.subscribe(topic,1,applicationContext,object :IMqttActionListener{
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.i(this.toString(),"订阅成功")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.i(this.toString(),"订阅失败")
                    }
                })
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(this.toString(),"连接失败")
            }
        })


        // 设置消息回调3
        mqtt.setCallback(object : MqttCallback{
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.i(this.toString(), "收到消息")
            }

            override fun connectionLost(cause: Throwable?) {
                Log.i(this.toString(), "连接丢失")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.i(this.toString(), "完成")
            }
        })

    }


    /**
     * 发送消息
     */
    private  fun sendMessage(msg:String){

    }


    /**
     * 处理接收到的消息
     */
    private fun dealMessage(topic: String, payload: String) {

    }



    /**
     * 字典Key排序
     */
    private  fun KeyToString(map:Map<String,String>) : String {
        val keys = map.keys.sorted()
        var valueStr = ""
        for (key in keys){
            valueStr += key
            valueStr += map[key]
        }
        return valueStr
    }


    public fun myToast(msg:String){
        runOnUiThread { toast(msg) }
    }

}


