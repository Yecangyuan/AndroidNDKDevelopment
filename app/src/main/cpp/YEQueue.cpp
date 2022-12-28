//
// Created by Simle Y on 2022/12/9.
//

#include "YEQueue.h"

YEQueue::YEQueue(YEPlayStatus *play_status) {
    this->play_status = play_status;
    pthread_mutex_init(&pthread_mutex, NULL);
    pthread_cond_init(&pthread_cond, NULL);
}

int YEQueue::put_av_packet(AVPacket *packet) {
    pthread_mutex_lock(&pthread_mutex);

    packets_queue.push(packet);
    pthread_cond_signal(&pthread_cond);
    pthread_mutex_unlock(&pthread_mutex);
    return 0;
}

int YEQueue::get_av_packet(AVPacket *packet) {
    pthread_mutex_lock(&pthread_mutex);
    while (play_status != NULL && !play_status->exit) {
        if (!packets_queue.empty()) {
            AVPacket *av_packet = packets_queue.front();
            if (av_packet_ref(packet, av_packet) == 0) {
                packets_queue.pop();
            }
            av_packet_free(&av_packet);
            av_free(av_packet);
            av_packet = NULL;
            break;
        } else {
            pthread_cond_wait(&pthread_cond, &pthread_mutex);
        }
    }
    pthread_mutex_unlock(&pthread_mutex);
    return 0;
}

int YEQueue::get_queue_size() {
    int size;
    pthread_mutex_lock(&pthread_mutex);
    size = packets_queue.size();
    pthread_mutex_unlock(&pthread_mutex);
    return size;
}

YEQueue::~YEQueue() {
    clear_av_packet();
}

void YEQueue::clear_av_packet() {
    pthread_cond_signal(&pthread_cond);
    pthread_mutex_lock(&pthread_mutex);
    while (!packets_queue.empty()) {
        AVPacket *packet = packets_queue.front();
        packets_queue.pop();
        av_packet_free(&packet);
        av_free(packet);
        packet = NULL;
    }
    pthread_mutex_unlock(&pthread_mutex);
}
