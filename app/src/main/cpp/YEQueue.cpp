//
// Created by Simle Y on 2022/12/9.
//

#include "YEQueue.h"

YEQueue::YEQueue(YEPlayStatus *playStatus) {
    this->playStatus = playStatus;
    pthread_mutex_init(&pthreadMutex, NULL);
    pthread_cond_init(&pthreadCond, NULL);
}

YEQueue::~YEQueue() {
    clearAvPacket();
}

int YEQueue::putAvPacket(AVPacket *packet) {
    pthread_mutex_lock(&pthreadMutex);

    packetsQueue.push(packet);
    pthread_cond_signal(&pthreadCond);
    pthread_mutex_unlock(&pthreadMutex);
    return 0;
}

int YEQueue::getAvPacket(AVPacket *packet) {
    pthread_mutex_lock(&pthreadMutex);
    while (playStatus != NULL && !playStatus->exit) {
        if (!packetsQueue.empty()) {
            AVPacket *avPacket = packetsQueue.front();
            if (av_packet_ref(packet, avPacket) == 0) {
                packetsQueue.pop();
            }
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            break;
        } else {
            pthread_cond_wait(&pthreadCond, &pthreadMutex);
        }
    }
    pthread_mutex_unlock(&pthreadMutex);
    return 0;
}

size_t YEQueue::getQueueSize() {
    size_t size;
    pthread_mutex_lock(&pthreadMutex);
    size = packetsQueue.size();
    pthread_mutex_unlock(&pthreadMutex);
    return size;
}


void YEQueue::clearAvPacket() {
    pthread_cond_signal(&pthreadCond);
    pthread_mutex_lock(&pthreadMutex);
    while (!packetsQueue.empty()) {
        AVPacket *packet = packetsQueue.front();
        packetsQueue.pop();
        av_packet_free(&packet);
        av_free(packet);
        packet = NULL;
    }
    pthread_mutex_unlock(&pthreadMutex);
}
