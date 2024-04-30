package subu

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._



class max_sub_input extends Bundle { 
    // val max = UInt(exp_bitwidth.W)
    val idx  = UInt(log2datain_bandwidth.W)
    val sign = Vec(datain_bandwidth, UInt(1.W))
}
class shift_sub_input extends Bundle {   
    val idx  = UInt(log2datain_bandwidth.W)
    val sign = Vec(datain_bandwidth, UInt(1.W))
    val frac = Vec(datain_bandwidth, UInt(frac_bitwidth.W))
}
class sub_exp_output extends Bundle { val frac = (Vec(datain_bandwidth, UInt(frac_bitwidth.W)))}
class sub_max(val bitwidth: Int, val numElements: Int) extends Module {
    val io = IO(new Bundle  {
        val max_subu_i   = Flipped(Decoupled(new max_sub_input))
        val shift_subu_i = Flipped(Decoupled(new shift_sub_input))
        val subu_expu_o  = Decoupled(new sub_exp_output)
    })

    val is_end       = WireInit(false.B)

    // ======================= FSM ==========================
    val state       = WireInit(sIdle)
    val max_subu_i_hs   = io.shift_subu_i.ready && io.shift_subu_i.valid
    val shift_subu_i_hs   = io.shift_subu_i.ready && io.shift_subu_i.valid
    val subu_expu_o_hs   = io.subu_expu_o.ready && io.subu_expu_o.valid
    state := fsm(shift_subu_i_hs, is_end, subu_expu_o_hs)
    // ======================================================
    io.shift_subu_i.ready := state === sIdle

    val sign_vec = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U(1.W))))
    sign_vec     := Mux(max_subu_i_hs, io.max_subu_i.bits.sign, sign_vec)
    val max_sign = RegInit(0.U(1.W))
    max_sign     := Mux(max_subu_i_hs, io.max_subu_i.bits.sign(io.max_subu_i.bits.idx), max_sign) 
    val max_idx = RegInit(0.U(log2datain_bandwidth.W))
    max_idx     := Mux(max_subu_i_hs, io.max_subu_i.bits.idx, max_idx) 
    
    val frac_vec = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))
    frac_vec     := Mux(shift_subu_i_hs, io.shift_subu_i.bits.frac, frac_vec)
    val max_frac = RegInit(0.U((frac_bitwidth+1).W))
    max_frac     := Mux(shift_subu_i_hs, frac_vec(max_idx), max_frac) 


    val sub_frac_vec = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))

    when (state === sRun) {
        for (i <- 0 until numElements) {
            sub_frac_vec(i) := Mux((sign_vec(i) === 1.U & max_sign === 1.U), max_frac - frac_vec(i),
                                Mux((sign_vec(i) === 1.U & max_sign === 0.U), frac_vec(i) - max_frac, 
                                Mux((sign_vec(i) === 1.U) & (max_sign === 0.U), frac_vec(i) + max_frac, 0.U))) // 最终输出的sign都是-1所以此处忽略 
            is_end          := Mux(i.U === (numElements-1).U, true.B, false.B)
        }
        io.subu_expu_o.valid     := false.B 
        io.subu_expu_o.bits.frac := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))
    }.elsewhen (state === sDone) {
        io.subu_expu_o.valid     := true.B 
        io.subu_expu_o.bits.frac := sub_frac_vec
    }.otherwise {
        io.subu_expu_o.valid     := false.B 
        io.subu_expu_o.bits.frac := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))
        sub_frac_vec             := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))        
    }

}