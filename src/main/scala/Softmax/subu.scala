package subu

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._


import pipeline._



class sub_max(val bitwidth: Int, val bandwidth: Int) extends Module {
    val io = IO(new Bundle  {
        val shift_subu_i = Flipped(Decoupled(new shift_sub))
        val subu_expu_o  = Decoupled(new sub_exp)
    })

    val SubDone       = WireInit(false.B)

    // ======================= FSM ==========================
    val state           = WireInit(sIdle)
    val shift_subu_i_hs = io.shift_subu_i.ready && io.shift_subu_i.valid
    val subu_expu_o_hs  = io.subu_expu_o.ready && io.subu_expu_o.valid
    state := fsm(shift_subu_i_hs, SubDone, subu_expu_o_hs)
    io.shift_subu_i.ready := state === sIdle
    // ======================================================

    val sub_frac_vec = RegInit(VecInit(Seq.fill(bandwidth)(0.U((frac_bitwidth).W))))
    val max_sign     = io.shift_subu_i.bits.sign_vec(io.shift_subu_i.bits.idx)
    val max_frac     = io.shift_subu_i.bits.frac_vec(io.shift_subu_i.bits.idx)

    when (state === sRun) {
        for (i <- 0 until bandwidth) {
            sub_frac_vec(i)          := Mux((io.shift_subu_i.bits.sign_vec(i) === 1.U & max_sign === 1.U), max_frac - io.shift_subu_i.bits.frac_vec(i),
                                            Mux((io.shift_subu_i.bits.sign_vec(i) === 1.U & max_sign === 0.U), io.shift_subu_i.bits.frac_vec(i) - max_frac, 
                                            Mux((io.shift_subu_i.bits.sign_vec(i) === 1.U) & (max_sign === 0.U), io.shift_subu_i.bits.frac_vec(i) + max_frac, 0.U))) // 最终输出的sign都是-1所以此处忽略 
            SubDone                  := Mux(i.U === (bandwidth-1).U, true.B, false.B)
        }
        io.subu_expu_o.valid         := false.B 
        io.subu_expu_o.bits.frac_vec := VecInit(Seq.fill(bandwidth)(0.U(frac_bitwidth.W)))
    }.elsewhen (state === sDone) {
        io.subu_expu_o.valid         := true.B 
        io.subu_expu_o.bits.frac_vec := sub_frac_vec
    }.otherwise {
        io.subu_expu_o.valid         := false.B 
        io.subu_expu_o.bits.frac_vec := VecInit(Seq.fill(bandwidth)(0.U(frac_bitwidth.W)))
        sub_frac_vec                 := VecInit(Seq.fill(bandwidth)(0.U(frac_bitwidth.W)))        
    }

}