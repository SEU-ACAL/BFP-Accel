package tb

import chisel3._
import chisel3.util._
import chisel3.stage._

import softmax._
import define.MACRO._
import define.FSM._

import DPIC.softmax_input_fp16_line


class SoftMax_Input extends Bundle {
    val raw_data = Vec(datain_bandwidth, UInt(datain_bitwidth.W))
}

class SoftMax_Output extends Bundle {
    val res_data = Vec(datain_bandwidth, UInt(dataout_bitwidth.W))
}



class tb extends Module {    
    val softmax_in  = Wire(Decoupled(new SoftMax_Input))
    val softmax_out = Wire(Flipped(Decoupled(new SoftMax_Output)))

    val softmax            = Module(new softmax)
    val softmax_input_fp16 = Module(new softmax_input_fp16_line).io

    softmax.data_in  <> softmax_in
    softmax.data_out <> softmax_out
    // ======================= FSM ==========================
    // val state        = WireInit(sIdle)
    val data_in_hs   = WireInit(false.B)
    // val data_out_hs  = WireInit(false.B)
    // val testend      = RegInit(false.B)
    data_in_hs      := softmax.data_in.valid && softmax_in.ready
    // data_out_hs     := softmax.data_out.valid && softmax_out.ready
    // state           := fsm(data_in_hs, data_out_hs)

    // softmax_in.valid        := false.B
    // val softmax_in_valid_r  = RegInit(false.B)
    // softmax_in_valid_r      := true.B 
    // softmax_in.valid        := softmax_in_valid_r // 屎山代码，第一周期要空掉，要不然第一周期拿不到正确值
    softmax_in.valid        := true.B

    softmax_out.ready       := true.B
    // ======================================================
    
    val line_num = RegInit(0.U(log2Up(datain_lines).W))
    softmax_input_fp16.en       := true.B
    softmax_input_fp16.line_num := line_num
    
    softmax_in.bits.raw_data := softmax_input_fp16.line_data
    

    // line_num := Mux(data_in_hs, line_num + 1.U, line_num)
    
    val first_line_flag  = RegInit(true.B) // 屎山代码

    when (data_in_hs && first_line_flag) { // 屎山代码
        line_num                 := line_num
        first_line_flag          := false.B
    }.elsewhen (data_in_hs && ~first_line_flag) {
        line_num                 := line_num + 1.U
        // first_line_flag          := first_line_flag
    }.otherwise {
        line_num                 := line_num 
        // first_line_flag          := first_line_flag
    }


    
}


object tb extends App {
    (new ChiselStage).emitVerilog(new tb, args)
}
