package ldu

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._

import pipeline._

class load_exp (val bitwidth: Int, val bandwidth: Int) extends Module {
    val io = IO(new Bundle {
        val maxu_ldu_i  = Flipped(Decoupled(new max_ld))
        val lut_ldu_i   = Flipped(Decoupled(new lut_ld))
        val ldu_lut_o   = Decoupled(new ld_lut)
        val ldu_expu_o  = Decoupled(new ld_exp)
    })
    // ======================= FSM ==========================
    val state            = WireInit(sIdle)
    val maxu_ldu_i_hs    = io.maxu_ldu_i.ready && io.maxu_ldu_i.valid
    val lut_ldu_i_hs     = io.lut_ldu_i.ready && io.lut_ldu_i.valid
    val ldu_expu_o_hs    = io.ldu_expu_o.ready && io.ldu_expu_o.valid
    state := fsm(maxu_ldu_i_hs, lut_ldu_i_hs, ldu_expu_o_hs) // lut load完才算Done
    io.maxu_ldu_i.ready := state === sIdle
    io.lut_ldu_i.ready  := state === sRun
    // ======================================================
        
    when (state === sRun) {
        io.ldu_lut_o.valid            := true.B 
        io.ldu_lut_o.bits.lut_set_idx := io.maxu_ldu_i.bits.max_exp
    }.otherwise {
        io.ldu_lut_o.valid            := false.B 
        io.ldu_lut_o.bits.lut_set_idx := 0.U
    }

    when (state === sDone) {
        io.ldu_expu_o.valid            := true.B 
        io.ldu_expu_o.bits.value_state := io.lut_ldu_i.bits.value_state
    }.otherwise {
        io.ldu_expu_o.valid            := false.B 
        io.ldu_expu_o.bits.value_state := 0.U
    }
}
