#include "tet.h"
#include <stdio.h>

void display_float_matrix(float (*float_matrix)[5], int row, int col) {
    printf("[(");
    for (int j = 0; j < row; j++) {
        for (int k = 0; k < col; k++) {
            printf("%16f, ", float_matrix[j][k]);    
        }
        printf(")\n  ");    
    }
    printf("]\n");
}

void display_fp16_matrix(fp16_t (*fp16_matrix)[5], int row, int col) {
    printf("[(");
    for (int j = 0; j < row; j++) {
        for (int k = 0; k < col; k++) {
            printf("(%1d, %4d, %4d) ", fp16_matrix[j][k].sign, fp16_matrix[j][k].exp, fp16_matrix[j][k].frac);    
        }
        printf(")\n  ");    
    }
    printf("]\n");
}