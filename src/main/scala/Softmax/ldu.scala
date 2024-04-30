package ldu

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._


class max_ldu_input extends Bundle { val max_exp = UInt(exp_bitwidth.W) }
class lut_ldu_input extends Bundle { val value_state = UInt(2.W) } // 01 normal_value, 10 overflow, 11 underflow, 00 load_unfinish 
class ldu_lut_output extends Bundle { val lut_set_idx = UInt(log2Up(lut_set).W) }
class ldu_expu_output extends Bundle { val load_finish = Bool()}

class load_exp (val bitwidth: Int, val numElements: Int) extends Module {
    val io = IO(new Bundle {
        val maxu_ldu_i  = Flipped(Decoupled(new max_ldu_input))
        val lut_ldu_i   = Flipped(Decoupled(new lut_ldu_input))
        val ldu_lut_o   = Decoupled(new ldu_lut_output)
        val ldu_expu_o  = Decoupled(new ldu_expu_output)
    })



    // ======================= FSM ==========================
    val state          = WireInit(sIdle)
    // val data_back      = WireInit(false.B)
    val lut_ldu_i_hs   = io.lut_ldu_i.ready && io.lut_ldu_i.valid
    val ldu_lut_o_hs   = io.ldu_lut_o.ready && io.ldu_lut_o.valid
    val ldu_expu_o_hs  = io.ldu_expu_o.ready && io.ldu_expu_o.valid
    state := fsm(lut_ldu_i_hs, ldu_lut_o_hs, ldu_expu_o_hs)
    // ======================================================
    io.maxu_ldu_i.ready := state === sIdle
    io.lut_ldu_i.ready := state === sIdle

    val lut_set_idx = RegInit(0.U(log2Up(lut_set).W))
    lut_set_idx := Mux(lut_ldu_i_hs, io.maxu_ldu_i.bits.max_exp, lut_set_idx)

        
    when (state === sRun) {
        io.ldu_lut_o.valid        := true.B 
        io.ldu_lut_o.bits.lut_set_idx := lut_set_idx
    }.otherwise {
        io.ldu_lut_o.valid        := false.B 
        io.ldu_lut_o.bits.lut_set_idx := 0.U
    }

    when (state === sDone) {
        io.ldu_expu_o.valid            := true.B 
        io.ldu_expu_o.bits.load_finish := true.B 
    }.otherwise {
        io.ldu_expu_o.valid            := false.B 
        io.ldu_expu_o.bits.load_finish := false.B
    }
}
