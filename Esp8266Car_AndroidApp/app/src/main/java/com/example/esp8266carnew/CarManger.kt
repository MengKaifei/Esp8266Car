package com.example.esp8266car

import android.util.Log
import com.example.esp8266car.exception.CarException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

private const val TAG = "CarManger"

class CarManger(IP: String,port : Int) {

    // 通信协议（自定义）
    val UP_DATA = "FF001DD";    // 前进（控制电机正转）
    val DOWN_DATA = "FF002DD";  // 后退（控制电机反转）
    val LEFT_DATA = "FF003DD";  // 左转（控制舵机左转）
    val RIGHT_DATA = "FF004DD"; // 右转（控制舵机右转）

    private var socket : Socket = Socket();
    private var render: BufferedReader? = null;

    init {
        try {
            socket.connect(InetSocketAddress(IP, port), 3000);
            if (socket.isConnected){
                Log.i(TAG, "onCreate: 连接成功")
                // 1：创建输入流对象InputStream
                val str : InputStream = socket.getInputStream()

                // 2：创建输入流读取器对象 并传入输入流对象
                // 该对象作用：获取服务器返回的数据
                val isr = InputStreamReader(str)
                render = BufferedReader(isr)
            }else{
                Log.i(TAG, "onCreate: 连接失败")
            }
        } catch (timeout: IOException) {
            throw CarException("Car Not FOUND!")
        }
    }

    fun getConnection(){

    }

    fun Send(str : String){
        Thread{
            try {
                // 1：从Socket 获得输出流对象OutputStream
                // 该对象作用：发送数据
                val outputStream = socket.getOutputStream()

                // 2：写入需要发送的数据到输出流对象中
                outputStream.write(
                    str.toByteArray(charset("utf-8"))
                )
                Log.i(TAG, "发送成功！")
                // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞

                // 3：发送数据到服务端
                outputStream.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    fun Read() {
        Thread{
            while(true) {
                try {
                    if (socket.isClosed || !socket.isConnected || render == null) continue
                    // 3：通过输入流读取器对象 接收服务器发送过来的数据
                    val msg = render!!.readLine() ?: continue
                }catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

}