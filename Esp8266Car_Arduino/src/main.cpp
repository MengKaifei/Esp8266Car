#include <Arduino.h>
#include "ESP8266WiFi.h"
#include <Servo.h>

const char  *ssid = "Esp8266Car";   // WiFi名
const char *password = "cqipc601";  // WiFi密码

WiFiServer server(6666);    // 设置Server服务端端口号
WiFiClient client;  // 创建客户端对象

Servo myServo;  // 定义Servo对象来控制

int leftPos[2] = {30,90};       // 舵机左转角度参数
int rightPos[2] = {150,90};     // 舵机右转角度参数

bool Pwm = false;       // 电机开关控制变量
bool Servo = false;     // 舵机开关控制变量

int speed =  50;

void setup() {
    /***********************************<<<   串口初始化   >>>************************************/

    // 设置串口波特率
    Serial.begin(9600);

    /***********************************<<<   电机初始化   >>>************************************/

    // 设置串口为输出模式（pin为GPIO端口号）
    pinMode(16,OUTPUT); // 电机PWM口（D0）
    pinMode(5,OUTPUT);  // 电机PWM口（D1）
    pinMode(4,OUTPUT);  // 电机PWM口（D2）
    pinMode(LED_BUILTIN,OUTPUT);

    /***********************************<<<   舵机初始化   >>>************************************/

    myServo.attach(14); // 舵机控制口（D5）
    myServo.write(90);  // 设置舵机初始角度
    delay(15);  // 等待转动到指定角度

    /***********************************<<<   WiFi初始化   >>>************************************/

    // 设置Esp8266 WiFi模式为AP模式
    WiFi.softAP(ssid,password);
    Serial.println();
    Serial.print("WiFi名称：");
    Serial.println(ssid);               // 输出WiFi名称
    Serial.print("IP address：");
    Serial.println(WiFi.softAPIP());    // 输出WiFi模块IP
    // 开启服务器
    server.begin();     // Server开启服务端
    Serial.println("服务器已开启！");
    Serial.println("连接到 " + WiFi.softAPIP().toString());
    // 接收客户端连接
    // client.connected()函数判断是否有客户端连接
    while (client.connected() == false) {
        // server.accept()函数用来得到客户端对象
        client = server.accept();
        delay(5);
    }
    if (client.connected()){
        Serial.println("客户端连接成功");
        delay(1000);
        client.write("客户端连接成功\n");
    }

}

void loop() {
    char buf[100];
    int i = 0;
    // 当客户端连接时，完成串口和TCP/IP的桥接
    if (client.connected()) {
//        Serial.println("客户端在线中...");
        if (client.available()) {
            // 按字节依次读取客户端数据
            delay(2);
            while (client.available()) {
                buf[i++] = client.read();
            }
            buf[i] = '\0';
            // 一次性发送客户端数据到串口
            Serial.println(buf);

            // 临时变量存放串口数据
            String str = buf;

            /***********************************<<<   客户端接收到的消息处理   >>>************************************/

            if (str.substring(0,2) == "DD"){
                String value = str.substring(2,str.length());
                speed = value.toInt();
            }

            // 电机正向旋转
            if (str == "FF001DD"){
                Pwm = !Pwm;
                if (Pwm){
                    // D0设置为高，D1设置为低则正转（D0，D1相等时不转）
                    digitalWrite(16,HIGH);  // D0
                    digitalWrite(5,LOW);    // D1
//                    digitalWrite(4,HIGH);
                    analogWrite(4,speed);
                    client.write("电机控制反馈：电机正转中（前进）....\n");
                }
                else{
                    // 设置电机停止转动
                    digitalWrite(16,HIGH);   // D0
                    digitalWrite(5,HIGH);    // D1
                    digitalWrite(4,LOW);
                    client.write("电机控制反馈：电机已停止\n");
                }
            }

            // 电机反向旋转
            if (str == "FF002DD"){

                Pwm = !Pwm;
                if (Pwm){
                    // D0设置为低，D1设置为高则反转（D0，D1相等时不转）
                    digitalWrite(16,LOW);   // D0
                    digitalWrite(5,HIGH);   // D1
//                    digitalWrite(4,HIGH);
                    analogWrite(4,speed);
                    client.write("电机控制反馈：电机正转中（后退）....\n");
                }
                else{
                    // 设置电机停止转动
                    digitalWrite(16,HIGH);   // D0
                    digitalWrite(5,HIGH);    // D1
                    digitalWrite(4,LOW);
                    client.write("电机控制反馈：电机已停止\n");
                }
            }

            // 舵机左转
            if (str == "FF003DD"){
                Servo = !Servo;
                if (Servo){
                    // 设置舵机角度
                    myServo.write(leftPos[0]);
                    client.write("舵机控制反馈：舵机左转中....\n");
                }
                else{
                    // 设置舵机角度
                    myServo.write(leftPos[1]);
                    client.write("舵机控制反馈：舵机已停止\n");
                }
            }

            // 舵机右转
            if (str == "FF004DD"){
                Servo = !Servo;
                if (Servo){
                    // 设置舵机角度
                    myServo.write(rightPos[0]);
                    client.write("舵机控制反馈：舵机右转中....\n");
                }
                else{
                    // 设置舵机角度
                    myServo.write(rightPos[1]);
                    client.write("舵机控制反馈：舵机已停止\n");
                }
            }

            /****************************************<<<   处理结束   >>>*******************************************/

        }
    }
    // 当客户端断开连接时，等待客户端重新连接
    else {
        Serial.println("客户端已离线！");
        client.stop();
        while (client.connected() == false) {
            // 接收客户端的连接
            client = server.accept();
            delay(5);
        }
        Serial.println("客户端连接成功！");
        delay(1000);
        client.write("客户端连接成功\n");
    }

}
