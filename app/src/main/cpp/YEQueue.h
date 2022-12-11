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
    queue<AVPacket *> packets_queue;
    pthread_mutex_t pthread_mutex;
    pthread_cond_t pthread_cond;
    YEPlayStatus *play_status = NULL;

public:
    YEQueue(YEPlayStatus *play_status);

    ~YEQueue();

    int put_av_packet(AVPacket *packet);

    int get_av_packet(AVPacket *packet);

    int get_queue_size();

    void clear_av_packet();
};


#endif //NDK_DAY78_YEQUEUE_H
