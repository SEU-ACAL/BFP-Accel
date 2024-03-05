package softmax

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._


class SoftMax_Input extends Bundle {
    val data_in = Input(Vec(datain_bandwidth, UInt(8.W)))

}

class SoftMax_Output extends Bundle {
    val data_out = Output(Vec(dataout_bandwidth, UInt(8.W)))

}

class softmax extends Module {
    val input  = IO(new SoftMax_Input)
    val output = IO(Valid(new SoftMax_Output))

    output.valid := false.B
    for(i <- 0 until 16) { output.bits.data_out(i) := 0.U }

}


