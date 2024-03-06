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

    val end_happened = RegInit(false.B)
    when (softmax.output.end) {end_happened:= true.B}

    softmax.da_input.valid := ~end_happened
    val da_hs = softmax.da_input.valid && softmax.da_input.ready
    val running_da_line_num = RegInit(0.U(log2datain_line_num.W))
    when (da_hs) {
        running_da_line_num := running_da_line_num + 1.U
        softmax_input_data.io.line_num := running_da_line_num
    }
    for (i <- 0 until datain_line_num) {
        softmax.da_input.bits.data_in(i) := softmax_input_data.io.line_data(i)
    }

    softmax.en_input.valid := ~end_happened
    val en_hs = softmax.en_input.valid && softmax.en_input.ready
    val running_en_line_num = RegInit(0.U(log2datain_line_num.W))
    when (en_hs) {
        running_en_line_num := running_en_line_num + 1.U
        softmax_input_data.io.line_num := running_en_line_num
    }

    softmax_input_data.io.en := da_hs || en_hs
    for (i <- 0 until datain_line_num) {
        softmax.en_input.bits.data_in(i) := softmax_input_data.io.line_data(i)
    }

}


object tb extends App {
    (new ChiselStage).emitVerilog(new tb, args)
}
