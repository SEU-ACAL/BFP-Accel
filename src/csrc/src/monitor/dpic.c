#include "tet.h"
#include <utils/macro.h>
#include <utils/debug.h>


uint8_t softmax_input[16][16] = {{1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2},
                                 {2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 4},
                                 {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 8},
                                 {1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 8, 8, 8, 8, 4},
                                 {2, 2, 2, 2, 2, 2, 2, 2, 4, 4, 4, 4, 4, 4, 4, 4},
                                 {4, 4, 4, 4, 4, 4, 4, 4, 2, 2, 2, 2, 2, 2, 2, 2},
                                 {4, 4, 4, 4, 4, 4, 4, 4, 2, 2, 2, 2, 2, 2, 2, 2},
                                 {2, 2, 2, 2, 2, 2, 2, 2, 4, 4, 4, 4, 4, 4, 4, 4},
                                 {4, 4, 4, 4, 4, 4, 4, 4, 2, 2, 2, 2, 2, 2, 2, 2},
                                 {1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 8, 8, 8, 8, 4},
                                 {2, 2, 2, 2, 2, 2, 2, 2, 4, 4, 4, 4, 4, 4, 4, 4},
                                 {4, 4, 4, 4, 4, 4, 4, 4, 2, 2, 2, 2, 2, 2, 2, 2},
                                 {1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 8, 8, 8, 8, 4},
                                 {2, 2, 2, 2, 2, 2, 2, 2, 4, 4, 4, 4, 4, 4, 4, 4},
                                 {4, 4, 4, 4, 4, 4, 4, 4, 2, 2, 2, 2, 2, 2, 2, 2},
                                 {8, 1, 8, 1, 8, 1, 8, 1, 8, 1, 8, 1, 8, 1, 8, 1}};


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


uint8_t softmax_output[16][16];

extern "C" void softmax_output_trace(svBit en, int line_num, const char *line_data) {
    // printf("en = %d, numline = %d\n", en, line_num);
    if (en) {
        for (int i = 0; i < 16; i++) {
            softmax_output[line_num][i] = line_data[i];
        }
    }
}
