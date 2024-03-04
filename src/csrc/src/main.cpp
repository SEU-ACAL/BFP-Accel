#include "npc.h"
#include <utils/macro.h>
#include <utils/debug.h>

// #define MAX_SIM_TIME 15000000// 最大仿真周期，中途读取到ebreak自动退出
// #define MAX_SIM_TIME 150000000000// 最大仿真周期，中途读取到ebreak自动退出
vluint64_t sim_time = 0;

VerilatedContext* contextp = NULL;
VerilatedVcdC* tfp = NULL;
static Vtb* top;


ll img_size = 0;

CPU_state cpu_npc;  // DUT
CPU_state cpu_nemu; // REF

NPCState npc_state;

// bool diff_skip_ref_flag = false;
int diff_skip_ref_flag = 0;

int nemu_step = 1; // 记录nemu一共走了多少步，出错时抛出，方便单步调试到周围 


//================ SIM FUNCTION =====================//
void step_and_dump_wave() {
    top->eval();
    contextp->timeInc(1);
    tfp->dump(contextp->time());
    sim_time++;
}

void sim_init() {
    contextp = new VerilatedContext;
    tfp = new VerilatedVcdC;
    top = new Vtb;

    contextp->traceEverOn(true);
    top->trace(tfp, 0);
    tfp->open("dump.vcd");

    top->reset = 1; top->clock = 0; step_and_dump_wave();
    top->reset = 1; top->clock = 1; step_and_dump_wave();
    top->reset = 0; top->clock = 0; step_and_dump_wave();   
} // 低电平复位

void npc_exec_once() {
#ifdef CONFIG_NPC_ITRACE 
    extern int config_npc_itrace;
    if (config_npc_itrace != NPC_xTRACE_OFF) { itrace_record(cpu_npc.pc);}
#endif
    top->clock ^= 1; step_and_dump_wave();
    top->clock ^= 1; step_and_dump_wave();
    // dump_gpr(); 
    // npc_step++;
} // 翻转两次走一条指令

void sim_exit() {
    // step_and_dump_wave();
    // top->eval();
    contextp->timeInc(1);
    tfp->dump(contextp->time());
    tfp->close();
    printf("The wave data has been saved to the dump.vcd\n");
}

void init_npc() {
    while (cpu_npc.pc != MEM_BASE) { 
        // printf("%ld\n", cpu_npc.pc); 
        npc_exec_once(); 
        // npc_step--;
    } // pc先走拍到第一条指令执行结束
}


int main(int argc, char *argv[]) {
    sim_init();

    init_monitor(argc, argv);

    sdb_mainloop();

    sim_exit();
} 