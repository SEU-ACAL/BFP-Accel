package tb

import chisel3._
import chisel3.util._
import chisel3.stage._

import softmax._
import define.MACRO._
import DPIC.softmax_input_line


class tb extends Module {
    val softmax = Module(new softmax)

    val softmax_input_data = Module(new softmax_input_line)

    softmax.da_input.valid := true.B
    val da_hs = softmax.da_input.valid && softmax.da_input.ready
    for (i <- 0 until 16) {
        softmax.da_input.bits.data_in(i) := Mux(da_hs, softmax_input_data.io.line_data(i), 0.S)
    }
    softmax.en_input.valid := true.B
    val en_hs = softmax.en_input.valid && softmax.en_input.ready
    for (i <- 0 until 16) {
        softmax.en_input.bits.data_in(i) := Mux(en_hs, softmax_input_data.io.line_data(i), 0.S)
    }
}


object tb extends App {
    (new ChiselStage).emitVerilog(new tb, args)
}
