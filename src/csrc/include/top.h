// #include <stdio.h> 
// #include <assert.h> 
// #include <stdlib.h> 
#ifndef _NPC_H_
#define _NPC_H_

#include <common.h>

// DPI-C
#include "verilated_dpi.h"
#include "Vtb__Dpi.h"
#include "svdpi.h"
// verilator
#include "verilated.h"
#include "verilated_vcd_c.h"
#include "Vtb.h"
// difftest
#include <dlfcn.h>
// MACRO
// #include <utils/macro.h>
// #include <utils/debug.h>

// ================ Typedef ===============
// typedef long long ll;
// typedef uint32_t paddr_t;
// typedef uint64_t word_t;
// typedef word_t vaddr_t;

// ============== 调试选项 ============= //
#define CONFIG_NPC_ITRACE    1
#define CONFIG_NPC_DIFFTEST  1
#define CONFIG_NPC_MTRACE    1
#define CONFIG_NPC_DTRACE    1
#define CONFIG_NPC_ETRACE    1
#define CONFIG_NPC_CTRACE    1
#define CONFIG_NPC_GPRDUMP   1

#define NPC_xTRACE_SHOW      2
#define NPC_xTRACE_HIDE      1
#define NPC_xTRACE_OFF       0

// ================ CPU ===================
static int status = 0;
typedef struct {
    word_t gpr[32];
    vaddr_t pc;
    word_t csr[4];      
} CPU_state;

// extern CPU_state cpu_npc;
// extern CPU_state cpu_nemu;

void npc_exit(int status);
void init_npc(); // npc启动后打三拍到EXU

// =============== Memory ===============
#define MEM_BASE           0x80000000
#define MEM_SIZE           0x30000000
// #define INTERFACE_MEM_BASE 0xa0000000
// #define INTERFACE_MEM_SIZE 0x10000000

extern uint8_t mem[MEM_SIZE];
// extern uint8_t interface_mem[INTERFACE_MEM_SIZE];

// Memory transfer
uint8_t* cpu2mem(ll addr);
long load_image(char const *img_file); 


// ============= Trace Config ================= //
void display_trace_status();
void set_trace_status(int *config_npc_xtrace, char *trace_opt);
// ============= ITRACE ================
void init_disasm(const char *triple);
void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
void itrace_record(uint64_t pc);
void itrace_output();

// ============= MTRACE ===============
#define SIZE_MTRACEBUF 20  // mtrace_buf环形里单次存储指令条数目
extern char mtrace_buf[SIZE_MTRACEBUF][100];
extern int mtrace_count;
void print_mtrace();

// ============= CTRACE ==============
void print_ctrace();

// ============= REG ===================
void dump_gpr(); // 打印寄存器
void dump_csr(); // 打印异常寄存器

// ============= Difftest ==============
// #ifdef CONFIG_NPC_DIFFTEST

void init_difftest(const char *ref_so_file, ll img_size);
void difftest_exec_once();
int  check_regs_npc(CPU_state cpu_nemu);

enum {
    DIFFTEST_TO_DUT,
    DIFFTEST_TO_REF
};

extern void (*ref_difftest_memcpy)(paddr_t addr, void *buf, size_t n, bool direction);
extern void (*ref_difftest_regcpy)(void *dut, bool direction);
extern void (*ref_difftest_exec)(uint64_t n);
extern void (*ref_difftest_raise_intr)(uint64_t NO);
extern void (*ref_difftest_init)();
// void difftest_skip_ref();
// extern bool diff_skip_ref_flag;
extern int diff_skip_ref_flag;


// #endif

// ============== npc runs ============= //
void npc_exec_once();
void sim_exit(); // 在异常跳出时记录波形结束

// ============== monitor and sdb ======= // 
void init_monitor(int argc, char *argv[]);
void sdb_set_batch_mode();
void sdb_mainloop();

// ============== DEVICE =============== //
void init_device();
void device_update();


// // ============== DEVICE MAP ===============
// #define DEVICE_BASE 0xa0000000

// #define SERIAL_MMIO         (DEVICE_BASE + 0x000003f8)
// #define KBD_ADDR            (DEVICE_BASE + 0x0000060)
// #define RTC_MMIO            (DEVICE_BASE + 0x00000048) // 映射的地址

#endif