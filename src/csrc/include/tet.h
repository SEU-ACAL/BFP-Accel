#ifndef _TET_H_
#define _TET_H_

// DPI-C
#include "verilated_dpi.h"
#include "Vtb__Dpi.h"
#include "svdpi.h"
// verilator
#include "verilated.h"
#include "verilated_vcd_c.h"
#include "Vtb.h"


// ================ DataType ====================
#include <input/input.h>
#include <utils/display.h>


// ================ RISCV CPU ===================

// ================ SDB Config ===================
void init_monitor(int argc, char *argv[]);
void sdb_mainloop();
void tet_exec_once();
void sdb_set_batch_mode(); 

// ================ SoftMax ===================
// extern uint8_t softmax_input[16][16];
void display_result(uint8_t (*dut_matrix)[16], double (*ref_matrix)[16]);
void softmax(uint8_t (*softmax_matrix)[16]);


#endif