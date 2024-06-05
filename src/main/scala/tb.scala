package tb

import chisel3._
import chisel3.util._
import chisel3.stage._

import softmax._
import define.MACRO._
import define.FSM._

import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.annotations.MemoryLoadFileType
// import DPIC.softmax_input_fp16_line


class tb extends Module {    
    val softmax_in  = Wire(Decoupled(new SoftMax_Input))
    // val div_i       = Wire(Decoupled(new Div_i))
    val softmax_out = Wire(Flipped(Decoupled(new SoftMax_Output)))

    val softmax            = Module(new softmax)
    // val softmax_input_fp16 = Module(new softmax_input_fp16_line).io

    // div_i.bits.divisor := VecInit(Seq.fill(cycle_bandwidth)(1.U(sum_bitwidth.W)))

    softmax.data_in  <> softmax_in
    softmax.data_out <> softmax_out
    // softmax.div_in   <> div_i
    // ======================= FSM ==========================
    // val state        = WireInit(sIdle)
    val data_in_hs   = WireInit(false.B)
    val data_out_hs   = WireInit(false.B)
    data_in_hs      := softmax.data_in.valid && softmax.data_in.ready
    data_out_hs      := softmax.data_out.valid && softmax.data_out.ready

    softmax_in.valid        := true.B
    // div_i.valid             := true.B

    softmax_out.ready       := true.B
    // ======================================================
    val input_mem  = Mem(datain_bandwidth * datain_lines, UInt(16.W))
    // val input_regs = RegInit(VecInit(Seq.fill(datain_lines)(VecInit(Seq.fill(datain_bandwidth)(0.U(16.W))))))
    loadMemoryFromFileInline(input_mem, "./src/main/scala/SoftmaxV3/test_data/input64lines.hex", MemoryLoadFileType.Hex); 

    // val test_data = VecInit(Seq.tabulate(1024) { i =>
    //     // 使用 i 作为种子来生成伪随机数
    //     val rand = new scala.util.Random(i * 1234567L + 42)
    //     rand.nextInt(65536).U(16.W)  // 16位无符号整数
    // })

    // for (i <- 0 until datain_lines) {
    //     for (j <- 0 until datain_bandwidth) {
    //         input_regs(i)(j) := input_mem(i.U * datain_bandwidth.U + j.U)
    //     }
    // }
    
    val line_num = RegInit(0.U(log2Up(datain_lines).W))    

    line_num := Mux(data_in_hs, line_num + 1.U, line_num)

    when (data_in_hs) { 
        for (i <- 0 until datain_bandwidth) {
            softmax_in.bits.raw_data(i)  := input_mem(line_num * datain_bandwidth.U + i.U)
        }
            // softmax_in.bits.raw_data := input_vec
    }.otherwise {
        for (i <- 0 until datain_bandwidth) {
            softmax_in.bits.raw_data(i) := 0.U
        }
    }

    val output_regs = RegInit(VecInit(Seq.fill(dataout_bandwidth)(0.U(16.W))))
    when (data_out_hs) {
        output_regs := softmax_out.bits.res_data
        for (i <- 0 until dataout_bandwidth) {
            printf("%d ", softmax_out.bits.res_data(i))
        }
    }

}

// class tb extends Module {    
//     val softmax_in  = Wire(Decoupled(new SoftMax_Input))
//     val div_i       = Wire(Decoupled(new Div_i))
//     val softmax_out = Wire(Flipped(Decoupled(new SoftMax_Output)))

//     val softmax            = Module(new softmax)
//     val softmax_input_fp16 = Module(new softmax_input_fp16_line).io

//     div_i.bits.divisor := VecInit(Seq.fill(cycle_bandwidth)(1.U(sum_bitwidth.W)))

//     softmax.data_in  <> softmax_in
//     softmax.data_out <> softmax_out
//     softmax.div_in   <> div_i
//     // ======================= FSM ==========================
//     // val state        = WireInit(sIdle)
//     val data_in_hs   = WireInit(false.B)
//     data_in_hs      := softmax.data_in.valid && softmax_in.ready

//     softmax_in.valid        := true.B
//     div_i.valid             := true.B

//     softmax_out.ready       := true.B
//     // ======================================================
    
//     val line_num = RegInit(0.U(log2Up(datain_lines).W))
//     softmax_input_fp16.en       := true.B
//     softmax_input_fp16.line_num := line_num
    
//     softmax_in.bits.raw_data := softmax_input_fp16.line_data
    
//     val first_line_flag  = RegInit(true.B) // 屎山代码

//     when (data_in_hs && first_line_flag) { // 屎山代码
//         line_num                 := line_num
//         first_line_flag          := false.B
//     }.elsewhen (data_in_hs && ~first_line_flag) {
//         line_num                 := line_num + 1.U
//     }.otherwise {
//         line_num                 := line_num 
//     }    
// }


object tb extends App {
    (new ChiselStage).emitVerilog(new tb, args)
}
