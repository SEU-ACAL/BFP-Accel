package divu

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._


class addu_div_input extends Bundle {   
    val sum  = UInt((frac_bitwidth*2).W)
    val frac = Vec(datain_bandwidth, UInt(frac_bitwidth.W))
}
class div_output extends Bundle { val result = (Vec(datain_bandwidth, UInt(expvalue_bitwidth.W)))}
class div (val bitwidth: Int, val numElements: Int) extends Module {
    val io = IO(new Bundle {
        val addu_divu_i = Flipped(Decoupled(new addu_div_input))
        val divu_o = Decoupled(new div_output)
    })


    val is_end       = WireInit(false.B)

    // ======================= FSM ==========================
    val state           = WireInit(sIdle)
    val addu_divu_i_hs  = io.addu_divu_i.ready && io.addu_divu_i.valid
    val divu_o_hs       = io.divu_o.ready && io.divu_o.valid
    state := fsm(addu_divu_i_hs, is_end, divu_o_hs)
    io.addu_divu_i.ready := state === sIdle
    // ======================================================

    val frac_vec    = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))
    frac_vec        := Mux(addu_divu_i_hs, io.addu_divu_i.bits.frac, frac_vec)
    val sum         = RegInit(0.U((frac_bitwidth*2).W))
    sum             := Mux(addu_divu_i_hs, io.addu_divu_i.bits.sum, sum) 

    val div_res_vec = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))


    when (state === sRun) {
        for (i <- 0 until numElements) {
            div_res_vec(i)      := frac_vec(i) / sum// 最终输出的sign都是-1所以此处忽略 
            is_end              := Mux(i.U === (numElements-1).U, true.B, false.B)
        } 
        io.divu_o.valid         := false.B 
        io.divu_o.bits.result   := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))
    }.elsewhen (state === sDone) {
        io.divu_o.valid         := true.B 
        io.divu_o.bits.result   := div_res_vec
    }.otherwise {
        io.divu_o.valid         := false.B 
        io.divu_o.bits.result   := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))
        div_res_vec             := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))        
    }

}