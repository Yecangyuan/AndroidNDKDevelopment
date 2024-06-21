package com.simley.mqtt

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

/**
 * MqttManager
 */
class MqttManager(context: Context) {

    private val context = context.applicationContext

    private val mqttClient: MqttAndroidClient by lazy {
        MqttAndroidClient(context, MQTT_URL, "android_mqtt")
    }

    /**
     * 建立连接
     */
    fun connect(context: Context) {
        /*
            connectionLost()：连接断开丢失了，会触发此回调方法，可以在此方法中进行重连和主题的重新订阅；
            messageArrived()：订阅主题对应的消息送达时，会触发此回调方法，可以在此方法中处理接收到的消息；
            deliveryComplete()：这个回调方法主要是告诉客户端发布的消息已经发布成功。
         */
        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(p0: Throwable?) {
                // Reconnect
                connect(context)
            }

            override fun messageArrived(p0: String?, p1: MqttMessage?) {

            }

            override fun deliveryComplete(p0: IMqttDeliveryToken?) {
            }
        })

        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.apply {
            userName = MQTT_USER_NAME
            password = MQTT_USER_PWD.toCharArray()
        }

        mqttClient.connect(mqttConnectOptions, null, object : IMqttActionListener {
            override fun onSuccess(p0: IMqttToken?) {
                Log.d(TAG, "connect success")
            }

            override fun onFailure(p0: IMqttToken?, p1: Throwable?) {
                Log.d(TAG, "onFailure: ${p1?.stackTraceToString()}")
            }

        })
    }

    /**
     * 订阅主题
     * @usage mqttClient.subscribe("chat/person/receiver/1", 0)
     */
    fun subscribeTopic(topic: String) {
        /*
            qos全称Quality of Service（服务质量），它是一个int值，它一共有三种类型，分别为0，1和2
                qos=0：最多送达一次，消息有可能在传输过程中丢失；
                qos=1：至少送达一次，消息至少保证送达到接受者一次，可能会送达多次；
                qos=2：恰好送达一次，消息只会送达一次到接受方，不会出现多次或者0次的情况，此qos最为复杂，涉及发送者和接受者之间多次交互。
         */
        mqttClient.subscribe(topic, 0)
    }

    /**
     * 取消订阅主题
     */
    fun unSubscribeTopic(topic: String) {
        mqttClient.unsubscribe(topic)
    }


    /**
     * 发布消息
     */
    fun publishMsg(topic: String, message: String) {
        mqttClient.publish(topic, message.toByteArray(), 0, false)
    }

    /**
     * 单向认证：只有客户端会根据根证书来验证服务端的证书，而服务端不会验证客户端的真实性；
     * 双向认证：在客户端验证服务端之后，服务端也需要验证客户端的证书是否正确。
     * TSL 单项认证
     */
//    fun getOneWayAuthSocketFactory(): SSLSocketFactory {
//        // 读取证书
//        val caInputStream: InputStream = context.resources.openRawResource(R.raw.ca)
//        Security.addProvider(BouncyCastleProvider())
//        var caCert: X509Certificate? = null
//        val bis = BufferedInputStream(caInputStream)
//        val cf = CertificateFactory.getInstance("X.509")
//        while (bis.available() > 0) {
//            caCert = cf.generateCertificate(bis) as X509Certificate
//        }
//        val caKs = KeyStore.getInstance(KeyStore.getDefaultType())
//        caKs.load(null, null)
//        caKs.setCertificateEntry("cert-certificate", caCert)
//        val tmf: TrustManagerFactory =
//            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
//        tmf.init(caKs)
//        val sslContext: SSLContext = SSLContext.getInstance("TLSv1.2")
//        sslContext.init(null, tmf.trustManagers, null)
//        return sslContext.socketFactory
//    }

//    fun getTwoWayAuthSocketFactory(): SSLSocketFactory {
//        // 读取证书和密钥
//        val caCrtFileInputStream: InputStream = context.resources.openRawResource(R.raw.ca)
//        val clientCrtFileInputStream: InputStream = context.resources.openRawResource(R.raw.client)
//        val clientKeyFileInputStream: InputStream = context.resources.openRawResource(R.raw.client_key)
//        Security.addProvider(BouncyCastleProvider())
//
//        // 加载根证书ca.crt
//        var caCert: X509Certificate? = null
//        var bis = BufferedInputStream(caCrtFileInputStream)
//        val cf = CertificateFactory.getInstance("X.509")
//        while (bis.available() > 0) {
//            caCert = cf.generateCertificate(bis) as X509Certificate
//        }
//
//        // 加载客户端证书client.crt
//        bis = BufferedInputStream(clientCrtFileInputStream)
//        var cert: X509Certificate? = null
//        while (bis.available() > 0) {
//            cert = cf.generateCertificate(bis) as X509Certificate
//        }
//
//        // 加载客户端KEY client.key
//        val pemParser = PEMParser(InputStreamReader(clientKeyFileInputStream))
//        val `object` = pemParser.readObject()
//        val converter = JcaPEMKeyConverter().setProvider("BC")
//        val key = converter.getKeyPair(`object` as PEMKeyPair)
//        val caKs = KeyStore.getInstance(KeyStore.getDefaultType())
//        caKs.load(null, null)
//        caKs.setCertificateEntry("cert-certificate", caCert)
//        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
//        tmf.init(caKs)
//        val ks = KeyStore.getInstance(KeyStore.getDefaultType())
//        ks.load(null, null)
//        ks.setCertificateEntry("certificate", cert)
//        ks.setKeyEntry("private-cert", key.private, "".toCharArray(), arrayOf<Certificate?>(cert))
//        val kmf: KeyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
//        kmf.init(ks, "".toCharArray())
//        val context = SSLContext.getInstance("TLSv1.2")
//        context.init(kmf.getKeyManagers(), tmf.trustManagers, null)
//        return context.socketFactory
//    }



    /**
     * 断开连接
     * @usage mqttManager.publishMsg("chat/person/receiver/1", "Hello MQTT")
     */
    fun disConnect() {
        mqttClient.disconnect()
        mqttClient.unregisterResources()
    }

    companion object {
        private const val TAG = "MqttManager"
        private const val MQTT_URL = "tcp://broker.emqx.io:1883"
        private const val MQTT_USER_NAME = ""
        private const val MQTT_USER_PWD = ""
    }

}