#include "tet.h"
#include <stdio.h>


void display_result(uint8_t (*dut_matrix)[16], double (*ref_matrix)[16]) {
    printf("softmax hardware result:\n");
    for (int j = 0; j < 16; j++) {
        for (int k = 0; k < 16; k++) {
            printf("%4d ", dut_matrix[j][k]);    
        }
        printf("\n");    
    }

    printf("softmax software result:\n");
    for (int j = 0; j < 16; j++) {
        for (int k = 0; k < 16; k++) {
            printf("%.3lf ", ref_matrix[j][k]);    
        }
        printf("\n");    
    }
}
