package tb

import chisel3._
import chisel3.util._
import chisel3.stage._

import softmax._
import define.MACRO._

import DPIC.softmax_input_fp16_line


class SoftMax_Input extends Bundle {
    val raw_data = Vec(datain_bandwidth, UInt(bitwidth.W))
}

class SoftMax_Output extends Bundle {
    val res_data = Vec(datain_bandwidth, UInt(bitwidth.W))
}



class tb extends Module {    
    val softmax_in  = Wire(Decoupled(new SoftMax_Input))
    val softmax_out = Wire(Flipped(Decoupled(new SoftMax_Output)))

    val softmax            = Module(new softmax)
    val softmax_input_fp16 = Module(new softmax_input_fp16_line).io

    softmax.data_in  <> softmax_in

    val data_in_hs  = WireInit(false.B)
    val data_out_hs = WireInit(false.B)
    data_in_hs      := softmax.data_in.valid && softmax_in.ready
    data_out_hs     := softmax.data_out.valid && softmax_out.ready
    
    val line_num = RegInit(0.U(log2datain_line_num.W))
    softmax_input_fp16.en       := true.B
    softmax_input_fp16.line_num := line_num
    
    val softmax_in_valid_r = RegInit(false.B)
    softmax_in_valid_r := true.B 
    softmax_in.valid := softmax_in_valid_r // 屎山代码，第一周期要空掉，要不然第一周期拿不到正确值
    // softmax_in.valid := true.B // 屎山代码，第一周期要空掉，要不然第一周期拿不到正确值
    
    // when (softmax.data_in.valid && softmax_in.ready) { // 屎山代码
    softmax_in.bits.raw_data := softmax_input_fp16.line_data
    // }.otherwise {
        // softmax_in.bits.raw_data := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))
    // }

    when (data_in_hs) { // 屎山代码
        line_num                 := line_num + 1.U
    }.otherwise {
        line_num                 := line_num 
    }


    softmax.data_out <> softmax_out
    softmax_out.ready := true.B
    when (softmax.data_out.valid && softmax_out.ready) {
        // printf("result after precess [");
        //     for (i <- 0 until datain_bandwidth) {
        //         printf("(%d, %d, %d) ", Preprocess_inst.sign_o.bits(i), Preprocess_inst.exp_o.bits(i), Preprocess_inst.frac_o.bits(i));
        //     }
        // printf("]\n");
    }

}


object tb extends App {
    (new ChiselStage).emitVerilog(new tb, args)
}
