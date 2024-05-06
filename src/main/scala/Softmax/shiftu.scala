package shiftu

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._


import pipeline._



class frac_shift(val bandwidth: Int) extends Module {
    /*
       单元功能：对齐指数后，尾数移位
       输入：指数、尾数（向量），最大值
       输出：移位后尾数（向量）
    */
    /* shiftu_subu_o
    val idx  = UInt(log2Up(datain_bandwidth).W)
    val sign = Vec(datain_bandwidth, UInt(1.W))
    val frac = Vec(datain_bandwidth, UInt(frac_bitwidth.W))
     */
    val io = IO(new Bundle  {
        val maxu_shiftu_i = Flipped(Decoupled(new max_shift))
        val shiftu_subu_o = Decoupled(new shift_sub)
    })

    // ======================= FSM ==========================
    val state               = WireInit(sIdle)
    val ShiftDone           = WireInit(false.B)
    val maxu_shiftu_i_hs    = io.maxu_shiftu_i.ready && io.maxu_shiftu_i.valid
    val shiftu_subu_o_hs       = io.shiftu_subu_o.ready && io.shiftu_subu_o.valid
    state := fsm(maxu_shiftu_i_hs, ShiftDone, shiftu_subu_o_hs)
    io.maxu_shiftu_i.ready := state === sIdle
    // ======================================================

    val frac_vec    = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))

    when (state === sRun) {
        io.shiftu_subu_o.valid       := false.B 
        for (i <- 0 until bandwidth) { 
            frac_vec(i)     := Mux(io.maxu_shiftu_i.bits.exp_vec(i) =/= 0.U, ((Cat(1.U, io.maxu_shiftu_i.bits.frac_vec(i))) >> (io.maxu_shiftu_i.bits.max - io.maxu_shiftu_i.bits.exp_vec(i))), 
                                (io.maxu_shiftu_i.bits.frac_vec(i) >> (io.maxu_shiftu_i.bits.max - io.maxu_shiftu_i.bits.exp_vec(i))))
            ShiftDone   := Mux(i.U === (datain_bandwidth-1).U, true.B, false.B)
        }
        io.shiftu_subu_o.bits.frac_vec   := VecInit(Seq.fill(bandwidth)(0.U(frac_bitwidth.W)))
    }.elsewhen (state === sDone) {
        io.shiftu_subu_o.valid           := true.B 
        io.shiftu_subu_o.bits.frac_vec   := frac_vec
    }.otherwise {
        io.shiftu_subu_o.valid           := false.B 
        io.shiftu_subu_o.bits.frac_vec   := VecInit(Seq.fill(bandwidth)(0.U(frac_bitwidth.W)))
    }

    io.shiftu_subu_o.bits.idx      :=  io.maxu_shiftu_i.bits.idx
    io.shiftu_subu_o.bits.sign_vec :=  io.maxu_shiftu_i.bits.sign_vec
    io.shiftu_subu_o.bits.frac_vec :=  frac_vec
}

