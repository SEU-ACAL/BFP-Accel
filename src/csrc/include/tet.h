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

// ================ RISCV CPU ===================

// ================ SDB Config ===================
void init_monitor(int argc, char *argv[]);
void sdb_mainloop();
void tet_exec_once();
void sdb_set_batch_mode(); 

// ================ SoftMax ===================


#endif