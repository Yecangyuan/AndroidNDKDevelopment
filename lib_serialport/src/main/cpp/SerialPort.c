/*
 * 安卓系统串口的支持，打开串口的头文件：系统提供的
 */
#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <string.h>
#include <jni.h>
#include "SerialPort.h"
#include "android/log.h"

/**
 * 获取波特率
 * @param baudrate 传入 0~4000000
 * @return 返回波特率类型的值
 */
static speed_t getBaudrate(jint baudrate) {
    // 波特率
    switch (baudrate) {
        case 0:
            return B0;
        case 50:
            return B50;
        case 75:
            return B75;
        case 110:
            return B110;
        case 134:
            return B134;
        case 150:
            return B150;
        case 200:
            return B200;
        case 300:
            return B300;
        case 600:
            return B600;
        case 1200:
            return B1200;
        case 1800:
            return B1800;
        case 2400:
            return B2400;
        case 4800:
            return B4800;
        case 9600:
            return B9600;
        case 19200:
            return B19200;
        case 38400:
            return B38400;
        case 57600:
            return B57600;
        case 115200:
            return B115200;
        case 230400:
            return B230400;
        case 460800:
            return B460800;
        case 500000:
            return B500000;
        case 576000:
            return B576000;
        case 921600:
            return B921600;
        case 1000000:
            return B1000000;
        case 1152000:
            return B1152000;
        case 1500000:
            return B1500000;
        case 2000000:
            return B2000000;
        case 2500000:
            return B2500000;
        case 3000000:
            return B3000000;
        case 3500000:
            return B3500000;
        case 4000000:
            return B4000000;
        default:
            return -1;
    }
}

/*
 * 关闭串口
 * Class:     cedric_serial_SerialPort
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_simley_lib_1serialport_handler_SerialPort_closeNative(JNIEnv *env, jobject thiz) {
    jclass SerialPortClass = (*env)->GetObjectClass(env, thiz);
    jclass FileDescriptorClass = (*env)->FindClass(env, "java/io/FileDescriptor");

    jfieldID mFdID = (*env)->GetFieldID(env, SerialPortClass, "mFd", "Ljava/io/FileDescriptor;");
    jfieldID descriptorID = (*env)->GetFieldID(env, FileDescriptorClass, "descriptor", "I");

    jobject mFd = (*env)->GetObjectField(env, thiz, mFdID);
    jint descriptor = (*env)->GetIntField(env, mFd, descriptorID);

    LOGD("关闭串口 close(fd = %d)", descriptor);
    close(descriptor); // 把此串口文件句柄关闭掉-文件读写流（文件句柄） InputStream/OutputStream=串口 发/收
}

/*
 * 打开串口-本质是通过串口的波特率等信息，构建Java对象-FileDescriptor
 * @param env
 * @param thiz
 * @param path_     要打开的路径-串口文件的路径
 * @param baudrate 波特率（相当于：Android系统设备层 与 硬件层 通讯所共识的频率）
 * @param flags    标记，就是打开串口的时候，用了下
 * @return         文件读写流（文件句柄） InputStream/OutputStream=串口 发/收
 */
JNIEXPORT jobject
JNICALL
Java_com_simley_lib_1serialport_handler_SerialPort_openNative(JNIEnv *env, jobject thiz,
                                                              jstring path_,
                                                              jint baud_rate, jint stopBits,
                                                              jint dataBits,
                                                              jint parity, jint flowCon,
                                                              jint flags) {
    int fd; // Linux串口文件句柄（本次整个函数最终的关键成果）
    speed_t speed; // 波特率类型的值
    jobject mFileDescriptor; // 文件句柄(最终返回的成果)

    // 检查参数，获取波特率参数信息 [先确定好波特率]
    {
        speed = getBaudrate(baud_rate);
        if (speed == -1) {
            LOGE("无效的波特率，证明用户选择的波特率 是错误的");
            return NULL;
        }
    }

    // 第一步：打开串口
    {
        jboolean isCopy;
        const char *path = (*env)->GetStringUTFChars(env, path_, &isCopy);
        LOGD("打开串口 路径是:%s", path); // 打开串口 路径是:/dev/ttyS0
        fd = open(path, O_RDWR /*| flags*/); // 打开串口的函数，O_RDWR(读 和 写)
        LOGD("打开串口 open() fd = %d", fd); // open() fd = 44
        (*env)->ReleaseStringUTFChars(env, path_, path); // 释放操作
        if (fd == -1) {
            LOGE("无法打开端口");
            return NULL;
        }
    }
    LOGD("第一步：打开串口，成功了√√√");

    // 第二步：获取和设置终端属性-配置串口设备
    /* TCSANOW：不等数据传输完毕就立即改变属性。
       TCSADRAIN：等待所有数据传输结束才改变属性。
       TCSAFLUSH：清空输入输出缓冲区才改变属性。
       注意：当进行多重修改时，应当在这个函数之后再次调用 tcgetattr() 来检测是否所有修改都成功实现。
     */
    {
        struct termios cfg;
        LOGD("执行配置串口中...");
        if (tcgetattr(fd, &cfg)) { // 获取串口属性
            LOGE("配置串口tcgetattr() 失败");
            close(fd); // 关闭串口
            return NULL;
        }

        cfmakeraw(&cfg);          // 将串口设置成原始模式，并且让fd(文件句柄 对串口可读可写)
        cfsetispeed(&cfg, speed); // 设置串口读取波特率
        cfsetospeed(&cfg, speed); // 设置串口写入波特率

        // 选择数据位
        switch (dataBits) {
            case 5:
                cfg.c_cflag |= CS5;     // 使用5位数据位
                break;
            case 6:
                cfg.c_cflag |= CS6;     // 使用6位数据位
                break;
            case 7:
                cfg.c_cflag |= CS7;     // 使用7位数据位
                break;
            case 8:
                cfg.c_cflag |= CS8;     // 使用8位数据位
                break;
            default:
                cfg.c_cflag |= CS8;
                break;
        }

        // 选择校验位
        switch (parity) {
            case 0:
                cfg.c_cflag &= ~PARENB;        // 无奇偶校验
                break;
            case 1:
                cfg.c_cflag |= (PARODD | PARENB);   // 奇校验
                break;
            case 2:
                cfg.c_cflag &= ~(IGNPAR | PARMRK);  // 偶校验
                cfg.c_cflag |= INPCK;
                cfg.c_cflag |= PARENB;
                cfg.c_cflag &= ~PARODD;
                break;
            default:
                cfg.c_cflag &= ~PARENB;
                break;
        }

        // 选择停止位
        switch (stopBits) {
            case 1:
                cfg.c_cflag &= ~CSTOPB;     // 1位停止位
            case 2:
                cfg.c_cflag |= CSTOPB;      // 2位停止位
                break;
            default:
                break;
        }

        // 流控
        switch (flowCon) {
            case 0:
                cfg.c_cflag &= ~CRTSCTS;    // 不使用流控
            case 1:
                cfg.c_cflag | CRTSCTS;      // 硬件流控
            case 2:
                cfg.c_cflag |= IXON | IXOFF | IXANY;      // 软件流控
                break;
            default:
                cfg.c_cflag &= ~CRTSCTS;
                break;
        }

        if (tcsetattr(fd, TCSANOW, &cfg)) { // 根据上面的配置，再次获取串口属性
            LOGE("再配置串口tcgetattr() 失败");
            close(fd); // 关闭串口
            return NULL;
        }
    }
    LOGD("第二步：获取和设置终端属性-配置串口设备，成功了√√√");

    // 第三步：构建FileDescriptor.java对象，并赋予丰富串口相关的值
    {
        jclass cFileDescriptor = (*env)->FindClass(env, "java/io/FileDescriptor");
        jmethodID iFileDescriptor = (*env)->GetMethodID(env, cFileDescriptor, "<init>", "()V");
        jfieldID descriptorID = (*env)->GetFieldID(env, cFileDescriptor, "descriptor", "I");
        // 反射生成FileDescriptor对象，并赋值 (fd==Linux串口文件句柄) FileDescriptor的构造函数实例化
        mFileDescriptor = (*env)->NewObject(env, cFileDescriptor, iFileDescriptor);
        (*env)->SetIntField(env, mFileDescriptor, descriptorID, (jint) fd); // 这里的fd，就是打开串口的关键成果
    }
    LOGD("第三步：构建FileDescriptor.java对象，并赋予丰富串口相关的值，成功了√√√");
    return mFileDescriptor; // 把最终的成果，返回会Java层
}

