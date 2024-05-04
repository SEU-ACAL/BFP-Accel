package expu

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._


import pipeline._


class get_exp (val bitwidth: Int, val bandwidth: Int) extends Module {
    val io = IO(new Bundle {
        val sub_expu_i    = Flipped(Decoupled(new sub_exp))
        val ldu_expu_i    = Flipped(Decoupled(new ld_exp))
        val lut_expu_i    = Flipped(Decoupled(new lut_exp))
        val expu_lut_o    = Decoupled(new exp_lut)
        val expu_addu_o   = Decoupled(new exp_add)
    })
    // ======================= FSM ==========================
    val state           = WireInit(sIdle)
    val sub_expu_i_hs   = io.sub_expu_i.ready && io.sub_expu_i.valid
    val lut_expu_i_hs   = io.lut_expu_i.ready && io.lut_expu_i.valid
    val expu_addu_o   = io.expu_addu_o.ready && io.expu_addu_o.valid
    state := fsm(sub_expu_i_hs, lut_expu_i_hs, expu_addu_o)
    io.sub_expu_i.ready := state === sIdle
    io.ldu_expu_i.ready := true.B
    // ======================================================


    val raddr = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W))))
    raddr := Mux(sub_expu_i_hs, io.sub_expu_i.bits.frac_vec, raddr)

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

    // to addu
    when (state === sDone) {
        io.expu_addu_o.valid            := true.B 
        io.expu_addu_o.bits.frac_vec    := rdata
    }.otherwise {
        io.expu_addu_o.valid            := false.B 
        io.expu_addu_o.bits.frac_vec    := VecInit(Seq.fill(datain_bandwidth)(0.U(expvalue_bitwidth.W)))
    }


}