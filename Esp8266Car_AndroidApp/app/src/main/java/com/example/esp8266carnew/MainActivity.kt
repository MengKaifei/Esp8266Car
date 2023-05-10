package com.example.esp8266carnew

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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


private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(),CoroutineScope by MainScope() {

    private lateinit var upBt : Button
    private lateinit var downBt : Button
    private lateinit var leftBt : Button
    private lateinit var rightBt : Button
    private lateinit var refreshBt : Button
    private lateinit var clearBt : Button
    private lateinit var portEdit : EditText
    private lateinit var stateText : TextView
    private lateinit var seekBar : SeekBar

    private val ip = "192.168.4.1"  // ESP8266 服务端 IP
    private val port = 6666 // ESP8266 服务端 端口号
    private lateinit var socket : Socket; // 创建Socket对象
    private var render:BufferedReader? = null;

    // 通信协议（自定义）
    private val UP_DATA = "FF001DD";    // 前进（控制电机正转）
    private val DOWN_DATA = "FF002DD";  // 后退（控制电机反转）
    private val LEFT_DATA = "FF003DD";  // 左转（控制舵机左转）
    private val RIGHT_DATA = "FF004DD"; // 右转（控制舵机右转）

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 设置创建界面时横屏显示
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        portEdit = findViewById(R.id.portEdit)
        portEdit.keyListener = null;

        stateText = findViewById(R.id.textView2)

        launch(Dispatchers.IO) {
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(ip, port), 3000);
                runOnUiThread(){
                    portEdit.text.append("连接中.....\n")
                    Toast.makeText(applicationContext, "小车连接中...", Toast.LENGTH_SHORT).show()
                }
                if (socket.isConnected){
                    // 1：创建输入流对象InputStream
                    val str : InputStream = socket.getInputStream()

                    // 2：创建输入流读取器对象 并传入输入流对象
                    // 该对象作用：获取服务器返回的数据
                    val isr = InputStreamReader(str)
                    render = BufferedReader(isr)
                    while (true){
                        val msg = render!!.readLine()
                        if (msg.equals("客户端连接成功")){
                            Log.i(TAG, "onCreate: 小车连接成功")
                            runOnUiThread(){
                                portEdit.text.append("小车连接成功！\n")
                                stateText.text = "状态：已连接"
                                stateText.setTextColor(Color.GREEN)
                                Toast.makeText(applicationContext, "小车连接成功", Toast.LENGTH_SHORT).show()
                            }
                            break
                        }else {
                            Log.i(TAG, "onCreate: 小车连接中....")
                        }
                    }

                }else{
                    Log.i(TAG, "onCreate: 小车连接失败")
                }
            }catch (e : IOException){
                e.printStackTrace()
            }
        }

        seekBar = findViewById(R.id.seekBar)
        seekBar.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.action == MotionEvent.ACTION_UP){
                    val seekBar = v as SeekBar

                    launch(Dispatchers.IO) {
                        try {
                            // 1：从Socket 获得输出流对象OutputStream
                            // 该对象作用：发送数据
                            val outputStream = socket.getOutputStream()

                            // 2：写入需要发送的数据到输出流对象中
                            outputStream.write(
                                "DD${seekBar.progress}".toByteArray(charset("utf-8"))
                            )
                            Log.i(TAG, "发送成功！")
                            // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞

                            // 3：发送数据到服务端
                            outputStream.flush()

                            editUiDisplay()

                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    return true
                }
                return false
            }

        })

        // 刷新按钮
        refreshBt = findViewById(R.id.refreshBt)
        refreshBt.setOnClickListener(){
            launch(Dispatchers.IO) {
                try {
                    socket = Socket()
                    socket.connect(InetSocketAddress(ip, port), 3000)
                    runOnUiThread(){
                        Toast.makeText(applicationContext, "刷新成功，重连中...", Toast.LENGTH_SHORT).show()
                        portEdit.text = null
                        stateText.text = "状态：未连接"
                        stateText.setTextColor(Color.RED)
                        portEdit.text.append("连接中.....\n")
                    }
                    if (socket.isConnected){
                        // 1：创建输入流对象InputStream
                        val str : InputStream = socket.getInputStream()

                        // 2：创建输入流读取器对象 并传入输入流对象
                        // 该对象作用：获取服务器返回的数据
                        val isr = InputStreamReader(str)
                        render = BufferedReader(isr)
                        while (true){
                            val msg = render!!.readLine()
                            if (msg.equals("客户端连接成功")){
                                Log.i(TAG, "onCreate: 小车连接成功")
                                runOnUiThread(){
                                    portEdit.text.append("小车连接成功！\n")
                                    stateText.text = "状态：已连接"
                                    stateText.setTextColor(Color.GREEN)
                                    Toast.makeText(applicationContext, "小车连接成功", Toast.LENGTH_SHORT).show()
                                }
                                break
                            }else {
                                Log.i(TAG, "onCreate: 小车连接中....")
                            }
                        }
                    }else{
                        Log.i(TAG, "onCreate: 小车连接失败")
                    }
                }catch (e : IOException){
                    e.printStackTrace()
                }
            }
        }

        clearBt = findViewById(R.id.clearBt)
        clearBt.setOnClickListener(){
            portEdit.text = null
            Toast.makeText(applicationContext, "已清空", Toast.LENGTH_SHORT).show()
        }

        // 前进按钮
        upBt = findViewById(R.id.upBt)
        upBt.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.action == MotionEvent.ACTION_DOWN || event?.action == MotionEvent.ACTION_UP) {
                    launch(Dispatchers.IO) {
                        try {
                            // 1：从Socket 获得输出流对象OutputStream
                            // 该对象作用：发送数据
                            val outputStream = socket.getOutputStream()

                            // 2：写入需要发送的数据到输出流对象中
                            outputStream.write(
                                UP_DATA.toByteArray(charset("utf-8"))
                            )
                            Log.i(TAG, "发送成功！")
                            // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞

                            // 3：发送数据到服务端
                            outputStream.flush()

                            editUiDisplay()

                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    return true
                }

                return false
            }

        })
//        upBt.setOnClickListener(){
//            launch(Dispatchers.IO) {
//                try {
//                    // 1：从Socket 获得输出流对象OutputStream
//                    // 该对象作用：发送数据
//                    val outputStream = socket.getOutputStream()
//
//                    // 2：写入需要发送的数据到输出流对象中
//                    outputStream.write(
//                        UP_DATA.toByteArray(charset("utf-8"))
//                    )
//                    Log.i(TAG, "发送成功！")
//                    // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞
//
//                    // 3：发送数据到服务端
//                    outputStream.flush()
//
//                    editUiDisplay()
//
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//            }
//        }

        // 后退按钮
        downBt = findViewById(R.id.downBt)
        downBt.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.action == MotionEvent.ACTION_DOWN || event?.action == MotionEvent.ACTION_UP) {
                    launch(Dispatchers.IO) {
                        try {
                            // 1：从Socket 获得输出流对象OutputStream
                            // 该对象作用：发送数据
                            val outputStream = socket.getOutputStream()

                            // 2：写入需要发送的数据到输出流对象中
                            outputStream.write(
                                DOWN_DATA.toByteArray(charset("utf-8"))
                            )
                            Log.i(TAG, "发送成功！")
                            // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞

                            // 3：发送数据到服务端
                            outputStream.flush()

                            editUiDisplay()

                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    return true
                }

                return false
            }
        })
//        downBt.setOnClickListener(){
//            launch(Dispatchers.IO) {
//                try {
//                    // 1：从Socket 获得输出流对象OutputStream
//                    // 该对象作用：发送数据
//                    val outputStream = socket.getOutputStream()
//
//                    // 2：写入需要发送的数据到输出流对象中
//                    outputStream.write(
//                        DOWN_DATA.toByteArray(charset("utf-8"))
//                    )
//                    Log.i(TAG, "发送成功！")
//                    // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞
//
//                    // 3：发送数据到服务端
//                    outputStream.flush()
//
//                    editUiDisplay()
//
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//            }
//        }

        // 左转按钮
        leftBt = findViewById(R.id.leftBt)
        leftBt.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.action == MotionEvent.ACTION_DOWN || event?.action == MotionEvent.ACTION_UP) {
                    launch(Dispatchers.IO) {
                        try {
                            // 1：从Socket 获得输出流对象OutputStream
                            // 该对象作用：发送数据
                            val outputStream = socket.getOutputStream()

                            // 2：写入需要发送的数据到输出流对象中
                            outputStream.write(
                                LEFT_DATA.toByteArray(charset("utf-8"))
                            )
                            Log.i(TAG, "发送成功！")
                            // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞

                            // 3：发送数据到服务端
                            outputStream.flush()

                            editUiDisplay()

                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    return true
                }

                return false
            }

        })
//        leftBt.setOnClickListener(){
//            launch(Dispatchers.IO) {
//                try {
//                    // 1：从Socket 获得输出流对象OutputStream
//                    // 该对象作用：发送数据
//                    val outputStream = socket.getOutputStream()
//
//                    // 2：写入需要发送的数据到输出流对象中
//                    outputStream.write(
//                        LEFT_DATA.toByteArray(charset("utf-8"))
//                    )
//                    Log.i(TAG, "发送成功！")
//                    // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞
//
//                    // 3：发送数据到服务端
//                    outputStream.flush()
//
//                    editUiDisplay()
//
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//            }
//        }

        // 右转按钮
        rightBt = findViewById(R.id.rightBt)
        rightBt.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.action == MotionEvent.ACTION_DOWN || event?.action == MotionEvent.ACTION_UP) {
                    launch(Dispatchers.IO) {
                        try {
                            // 1：从Socket 获得输出流对象OutputStream
                            // 该对象作用：发送数据
                            val outputStream = socket.getOutputStream()

                            // 2：写入需要发送的数据到输出流对象中
                            outputStream.write(
                                RIGHT_DATA.toByteArray(charset("utf-8"))
                            )
                            Log.i(TAG, "发送成功！")
                            // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞

                            // 3：发送数据到服务端
                            outputStream.flush()

                            editUiDisplay()

                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    return true
                }

                return false
            }

        })
//        rightBt.setOnClickListener(){
//            launch(Dispatchers.IO) {
//                try {
//                    // 1：从Socket 获得输出流对象OutputStream
//                    // 该对象作用：发送数据
//                    val outputStream = socket.getOutputStream()
//
//                    // 2：写入需要发送的数据到输出流对象中
//                    outputStream.write(
//                        RIGHT_DATA.toByteArray(charset("utf-8"))
//                    )
//                    Log.i(TAG, "发送成功！")
//                    // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞
//
//                    // 3：发送数据到服务端
//                    outputStream.flush()
//
//                    editUiDisplay()
//
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//            }
//        }

    }

    override fun onResume() {
        super.onResume()
        // 设置在界面中时横屏显示
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    private fun editUiDisplay(){
        Thread{
            try {
                // 3：通过输入流读取器对象 接收服务器发送过来的数据
                val msg = render!!.readLine()
                if (msg != null){
                    runOnUiThread(){
                        portEdit.text.append(msg+"\n")
                    }
                }
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

}