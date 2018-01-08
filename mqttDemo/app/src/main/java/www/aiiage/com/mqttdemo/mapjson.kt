package www.aiiage.com.mqttdemo

import android.R.attr.key
import android.content.res.Resources


/**
 * Created by wudezhi on 2017/12/26.
 */
public class mapjson {

    var code : Int = 0
    var message : String = ""
    var data : dataBoy? = null

    class dataBoy {
        var resources: Resources? = null
        class  Resources {
            var mqtt : MqttData? = null
            class  MqttData{
                var port : Int = 0
                var host : String = ""
            }
        }

        var iotId: String = ""
        var iotToken: String = ""
    }

}