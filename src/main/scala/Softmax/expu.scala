package expu

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._


class sub_expu_input extends Bundle { val frac        = Vec(datain_bandwidth, UInt(frac_bitwidth.W)) }
class ldu_expu_input extends Bundle { val value_state = UInt(2.W) } // 01 normal_value, 10 overflow, 11 underflow, 00 load_unfinish 
class lut_expu_input extends Bundle { val rdata       = Vec(datain_bandwidth, UInt(expvalue_bitwidth.W))}
class expu_lut_output extends Bundle { val raddr      = Vec(datain_bandwidth, UInt(expvalue_bitwidth.W)) }
class expu_addu_output extends Bundle { val exp_value = Vec(datain_bandwidth, UInt(frac_bitwidth.W)) }

class get_exp (val bitwidth: Int, val numElements: Int) extends Module {
    val io = IO(new Bundle {
        val sub_expu_i    = Flipped(Decoupled(new sub_expu_input))
        val ldu_expu_i    = Flipped(Decoupled(new ldu_expu_input))
        val lut_expu_i    = Flipped(Decoupled(new lut_expu_input))
        val expu_lut_o    = Decoupled(new expu_lut_output)
        val expu_addu_o   = Decoupled(new expu_addu_output)
    })
    // ======================= FSM ==========================
    val state           = WireInit(sIdle)
    val sub_expu_i_hs   = io.sub_expu_i.ready && io.sub_expu_i.valid
    val lut_expu_i_hs   = io.lut_expu_i.ready && io.lut_expu_i.valid
    val expu_addu_o   = io.expu_addu_o.ready && io.expu_addu_o.valid
    state := fsm(sub_expu_i_hs, lut_expu_i_hs, expu_addu_o)
    // ======================================================
    io.sub_expu_i.ready := state === sIdle


    val raddr = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W))))
    raddr := Mux(sub_expu_i_hs, io.sub_expu_i.bits.frac, raddr)

    val rdata = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U(expvalue_bitwidth.W))))
    rdata := Mux(lut_expu_i_hs, io.lut_expu_i.bits.rdata, rdata)

    // to lut
    when (state === sRun) {
        io.expu_lut_o.valid         := true.B 
        io.expu_lut_o.bits.raddr    := raddr
    }.otherwise {
        io.expu_lut_o.valid         := false.B 
        io.expu_lut_o.bits.raddr    := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))
    }

    // from lut
    when (state === sRun) {
        io.lut_expu_i.ready    := true.B 
    }.otherwise {
        io.lut_expu_i.ready    := false.B 
    }

    // to shift
    when (state === sDone) {
        io.expu_addu_o.valid            := true.B 
        io.expu_addu_o.bits.exp_value   := rdata
    }.otherwise {
        io.expu_addu_o.valid            := false.B 
        io.expu_addu_o.bits.exp_value   := VecInit(Seq.fill(datain_bandwidth)(0.U(expvalue_bitwidth.W)))
    }


}