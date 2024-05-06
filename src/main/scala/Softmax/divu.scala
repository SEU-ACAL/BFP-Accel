package divu

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._

import pipeline._

class div_output extends Bundle { val res_data = (Vec(cycle_bandwidth, UInt(dataout_bitwidth.W)))}
class div (val bitwidth: Int, val bandwidth: Int) extends Module {
    val io = IO(new Bundle {
        val addu_divu_i = Flipped(Decoupled(new add_div))
        val divu_o      = Decoupled(new div_output)
    })


    val DivDone       = WireInit(false.B)

    // ======================= FSM ==========================
    val state           = WireInit(sIdle)
    val addu_divu_i_hs  = io.addu_divu_i.ready && io.addu_divu_i.valid
    val divu_o_hs       = io.divu_o.ready && io.divu_o.valid
    state := fsm(addu_divu_i_hs, DivDone, divu_o_hs)
    io.addu_divu_i.ready := state === sIdle
    // ======================================================

    val div_res_vec = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))


    when (state === sRun) {
        for (i <- 0 until bandwidth) {
            div_res_vec(i)       := io.addu_divu_i.bits.frac_vec(i) / io.addu_divu_i.bits.sum// 最终输出的sign都是-1所以此处忽略 
            DivDone              := Mux(i.U === (bandwidth-1).U, true.B, false.B)
        } 
        io.divu_o.valid         := false.B 
        io.divu_o.bits.res_data   := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))
    }.elsewhen (state === sDone) {
        io.divu_o.valid         := true.B 
        io.divu_o.bits.res_data   := div_res_vec
    }.otherwise {
        io.divu_o.valid         := false.B 
        io.divu_o.bits.res_data   := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))
        div_res_vec             := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))        
    }

}