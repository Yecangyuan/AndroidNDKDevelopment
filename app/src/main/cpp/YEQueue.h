//
// Created by Simle Y on 2022/12/9.
//

#ifndef NDK_DAY78_YEQUEUE_H
#define NDK_DAY78_YEQUEUE_H

#include "queue"
#include "pthread.h"
#include "YEPlayStatus.h"

extern "C" {
#include "libavcodec/avcodec.h"
};

using namespace std;


class YEQueue {
public:
    queue<AVPacket *> packetsQueue;
    pthread_mutex_t pthreadMutex;
    pthread_cond_t pthreadCond;
    YEPlayStatus *playStatus = NULL;

public:
    YEQueue(YEPlayStatus *playStatus);

    ~YEQueue();

    int putAvPacket(AVPacket *packet);

    int getAvPacket(AVPacket *packet);

    size_t getQueueSize();

    void clearAvPacket();
};


#endif //NDK_DAY78_YEQUEUE_H
