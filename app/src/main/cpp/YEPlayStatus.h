//
// Created by Simle Y on 2022/12/9.
//

#ifndef NDK_DAY78_YEPLAYSTATUS_H
#define NDK_DAY78_YEPLAYSTATUS_H


class YEPlayStatus {
public:
    bool exit;
    bool seek = false;
    bool pause = false;
    bool load = true;
public:
    YEPlayStatus();
};


#endif //NDK_DAY78_YEPLAYSTATUS_H
