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


float softmax_input_float[16][5] = {{666.9843750, 3.4628906, 6.5468750, 7.3398438, 7.5585938},
                                       {128.9843750, 254.4628906, 6.5468750, 7.3398438, 7.5585938},
                                       {8.9843750, 3.4628906, 6.5468750, 7.3398438, 7.5585938},
                                       {8.9843750, 3.4628906, 0.5468750, 7.3398438, 7.5585938},
                                       {8.9843750, 3.4628906, 6.5468750, -7.3398438, 7.5585938},
                                       {8.9843750, 3.4628906, 6.5468750, 7.3398438, 7.5585938},
                                       {8.9843750, 3.4628906, 6.5468750, 7.3398438, 7.5585938},
                                       {8.9843750, 3.4628906, 6.5468750, 7.3398438, 7.5585938},
                                       {8.9843750, 3.4628906, 6.5468750, 7.3398438, 7.5585938},
                                       {8.9843750, 3.4628906, 6.5468750, 7.3398438, 7.5585938},
                                       {8.9843750, 3.4628906, 6.5468750, 7.3398438, 7.5585938},
                                       {8.9843750, 3.4628906, 6.5468750, 7.3398438, 7.5585938},
                                       {8.9843750, 3.4628906, 6.5468750, 7.3398438, 7.5585938},
                                       {8.9843750, 3.4628906, 6.5468750, 7.3398438, 7.5585938},
                                       {8.9843750, 3.4628906, 6.5468750, 7.3398438, 7.5585938},
                                       {8.9843750, 3.4628906, 6.5468750, -7.3398438, 123.5585938}};

fp16_t softmax_input_fp16[5][5] = {{0}};

uint16_t softmax_input_fp16_sign[5][5] = {{0, 0, 0, 0, 0},
                                        {1, 0, 0, 0, 0},
                                        {0, 1, 0, 0, 0},
                                        {0, 0, 1, 0, 0},
                                        {0, 0, 0, 1, 0}};
uint16_t softmax_input_fp16_exp[5][5] = { {18, 16, 18, 18, 18},
                                        {18, 16, 18, 18, 18},
                                        {18, 16, 18, 18, 18},
                                        {18, 16, 18, 18, 18},
                                        {18, 16, 18, 18, 18}};
uint16_t softmax_input_fp16_frac[5][5] = {{126, 749, 652, 855, 911},
                                        {126, 749, 652, 855, 911},
                                        {126, 749, 652, 855, 911},
                                        {126, 749, 652, 855, 911},
                                        {126, 749, 652, 855, 911}};

uint8_t softmax_output[16][16];