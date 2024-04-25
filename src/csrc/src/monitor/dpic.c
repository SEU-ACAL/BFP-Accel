#include "tet.h"
#include <utils/macro.h>
#include <utils/debug.h>


extern uint8_t softmax_input[16][16];

extern "C" void softmax_read_matrix(svBit en, int line_num, char *line_data) {
    // printf("en = %d, numline = %d\n", en, line_num);
    if (en) {
        for (int i = 0; i < 16; i++) {
            line_data[i] = softmax_input[line_num][i];
        }
    } else {
        for (int i = 0; i < 16; i++) {
            line_data[i] = 0;
        }
    }
}

extern fp16_t softmax_input_fp16[5][5];

extern "C" void softmax_read_FP16_matrix(svBit en, int line_num, short int *line_data) {
    // printf("en = %d, numline = %d\n", en, line_num);
    if (en) {
        for (int i = 0; i < 5; i++) {
            line_data[i] = softmax_input_fp16[line_num][i].val;
        }
    } else {
        for (int i = 0; i < 5; i++) {
            line_data[i] = 0;
        }
    }
}



extern uint8_t softmax_output[16][16];

extern "C" void softmax_output_trace(svBit en, int line_num, const char *line_data) {
    // printf("en = %d, numline = %d\n", en, line_num);
    if (en) {
        for (int i = 0; i < 16; i++) {
            softmax_output[line_num][i] = line_data[i];
        }
    }
}

