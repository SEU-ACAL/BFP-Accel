package maxu

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._



class max_input extends Bundle { 
    val data = Vec(numElements, UInt(exp_bitwidth.W))
    val sign = Vec(datain_bandwidth, UInt(1.W))
}

class max_shift_output extends Bundle { 
    val max = UInt(exp_bitwidth.W)
    val idx = UInt(log2datain_bandwidth.W)
    val sign = Vec(datain_bandwidth, UInt(1.W))
}

class max_load_output extends Bundle { 
    val max_exp = UInt(exp_bitwidth.W)
}

class MaxComparator(val bitwidth: Int, val numElements: Int) extends Module {
    val io = IO(new Bundle {
        val maxu_i          = Flipped(Decoupled(new max_input))
        val maxu_shiftu_o   = Decoupled(new max_shift_output)
        val maxu_ldu_o      = Decoupled(new max_load_output)
    })

    val max        = RegInit(31.U(bitwidth.W))
    val max_sign   = RegInit(1.U(1.W))
    val idx        = RegInit(0.U(log2datain_bandwidth.W))
    val count      = RegInit(0.U(log2Ceil(numElements + 1).W))

    // ======================= FSM ==========================
    val state              = WireInit(sIdle)
    val maxu_i_hs          = io.maxu_i.ready && io.maxu_i.valid
    val maxu_shiftu_o_hs   = io.maxu_shiftu_o.ready && io.maxu_shiftu_o.valid
    state := fsm(maxu_i_hs, (count === numElements.U-1.U), maxu_shiftu_o_hs)
    // ======================================================

    io.maxu_i.ready := state === sIdle

    val data_vec     = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((exp_bitwidth).W))))
    data_vec        := Mux(maxu_i_hs, io.maxu_i.bits.data, data_vec)
    val sign_vec     = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U(1.W))))
    sign_vec        := Mux(maxu_i_hs, io.maxu_i.bits.sign, sign_vec)

    when (state === sRun) {
        for (i <- 0 until numElements) {
            when ((sign_vec(i) < max_sign)  || 
                    ((sign_vec(i) === 1.U) & (sign_vec(i) === 1.U) & (max > data_vec(i))) ||
                     ((sign_vec(i) === 0.U) & (sign_vec(i) === 0.U) & (max < data_vec(i)))) {
                max      := data_vec(i)
                max_sign := sign_vec(i)
                idx      := i.U
            }
            count := count + 1.U
        }
        io.maxu_shiftu_o.valid     := false.B 
        io.maxu_shiftu_o.bits.max  := max
        io.maxu_ldu_o.valid        := false.B
        io.maxu_ldu_o.bits.max_exp := max
    }.elsewhen (state === sDone) {
        io.maxu_shiftu_o.valid     := true.B 
        io.maxu_shiftu_o.bits.max  := max
        io.maxu_shiftu_o.bits.idx  := idx
        io.maxu_ldu_o.valid        := true.B
        io.maxu_ldu_o.bits.max_exp := max
        count          := 0.U
    }.otherwise {
        io.maxu_shiftu_o.valid     := false.B 
        io.maxu_shiftu_o.bits.max  := 0.U
        io.maxu_shiftu_o.bits.idx  := 0.U
        io.maxu_ldu_o.valid        := false.B 
        io.maxu_ldu_o.bits.max_exp := 0.U
        count                      := 0.U        
        max                        := 0.U        
    }

    io.maxu_shiftu_o.bits.sign := sign_vec //连线



}