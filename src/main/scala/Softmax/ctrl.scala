package CTRL

import chisel3._
import chisel3.util._

import pipeline._



class CTRL extends Module {
    val io = IO(new Bundle {
        val ldu_ctrl_i      = Input(new Bundle{ val ldu_busy  = Bool()})
        val ctrl_maxshift_o = Output(new ctrl_maxshift)
    })
    


    io.ctrl_maxshift_o.stall_en := io.ldu_ctrl_i.ldu_busy

    // // 给事件进行优先编码
    // val event_code = PriorityEncoder(Cat(load_data_hit, icache_busy, dcache_busy, alu_busy, jump, NOEVENT)) // 从低到高输出第一个有1的位数 0->NOEVENT

    // //  List(pc_stall_en, if_id_stall_en, id_ex_stall_en, id_clint_stall_en, ex_mem_stall_en, ex_wb_stall_en, mem_wb_stall_en)
    // val stall_list  = ListLookup(event_code, List(false.B, false.B, false.B, false.B, false.B, false.B, false.B), Array(
    //     BitPat("b000") -> List(false.B, false.B, false.B, false.B, false.B, false.B, false.B),   // Noevent
    //     BitPat("b010") -> List(true.B,  true.B,  true.B,  false.B, false.B, false.B, false.B),    // alu_busy         
    //     BitPat("b011") -> List(true.B,  true.B,  true.B,  true.B,  false.B, false.B, false.B),   // dcache_busy
    //     BitPat("b100") -> List(true.B,  false.B, false.B, false.B, false.B, false.B, false.B),   // icache_busy
    //     BitPat("b101") -> List(true.B,  true.B,  false.B, false.B, false.B, false.B, false.B)    // load_data_hit         
    // ))

    // ctrl_pc.pc_stall_en             := stall_list(0)
    // ctrl_ifid.ifid_stall_en         := stall_list(1)
    // ctrl_idex.idex_stall_en         := stall_list(2)
    // ctrl_idclint.idclint_stall_en   := stall_list(3)
    // ctrl_exmem.exmem_stall_en       := stall_list(4)
    // ctrl_exwb.exwb_stall_en         := stall_list(5)
    // ctrl_memwb.memwb_stall_en       := stall_list(6)

}