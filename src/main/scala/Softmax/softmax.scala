package softmax

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._


class SoftMax_Input extends Bundle {
    val data_in = Input(Vec(datain_bandwidth, UInt(bitwidth.W)))
}


class softmax extends Module {
    val data_in  = IO(Flipped(Decoupled(new SoftMax_Input)))

    val SEFP_Generator = Module(new SEFP_GeneratorUnit(bitwidth)).io

    printf("Hello, start from this\n");
}

class SEFP_GeneratorUnit(bitwidth: Int) extends Module {
    /*
       单元功能：对齐指数
       输入：输入的FP16数据（向量）
       输出：对齐后的指数、尾数，最大值
    */
    val io = IO(new Bundle {
        val en          = Input(Bool())
        val raw_data_i  = Input(Vec(datain_bandwidth, UInt(bitwidth.W)))
        val max_o       = Output(UInt(bitwidth.W))
        val sign_o      = Output(Valid(UInt(1.W)))
        val exp_o       = Output(Valid(UInt(exp_bitwidth.W)))
        val frac_o      = Output(Valid(UInt(frac_bitwidth.W)))
    })


}
