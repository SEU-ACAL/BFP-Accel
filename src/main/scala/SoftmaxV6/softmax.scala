package softmax

import chisel3._
import chisel3.util._
import chisel3.stage._

import softmax_define.MACRO._
import softmax_define.FSM._

import max_stage._
import exp_stage._
import div_stage._
import pipeline._

// import chisel3.util.experimental.loadMemoryFromFileInline
// import firrtl.annotations.LoadMemoryAnnotation

class SoftMax_Input_Stage1 extends Bundle { 
    val raw_data = Vec(cycle_bandwidth, UInt(bitwidth.W))
    val batch_num = UInt(log2Up(maxBatch).W) 
}
class SoftMax_Input_Stage2 extends Bundle { 
    val data_in = Vec(cycle_bandwidth, UInt(bitwidth.W))
    val batch_num = UInt(log2Up(maxBatch).W) 
}
class SoftMax_Input_Stage3 extends Bundle { 
    val data_exp  = UInt(exp_bitwidth.W) // 对完指的
    val data_frac = Vec(cycle_bandwidth, UInt(frac_bitwidth.W)) // 对完指的
}

class SoftMax_Output extends Bundle { val data_o = Vec(cycle_bandwidth, UInt(dataout_bitwidth.W))}


class softmax_top extends Module {
    val data_in_stage1  = IO(Flipped(Decoupled(new SoftMax_Input_Stage1)))
    val data_in_stage2  = IO(Flipped(Decoupled(new SoftMax_Input_Stage2)))
    val data_in_stage3  = IO(Flipped(Decoupled(new SoftMax_Input_Stage3)))

    val data_out        = IO(Valid(new SoftMax_Output))

   
    val MAX_stage_inst    = Module(new max_stage(bandwidth_in = cycle_bandwidth, bandwidth_out = cycle_bandwidth))
    val EXP_stage_inst    = Module(new exp_stage(bandwidth_in = cycle_bandwidth, bandwidth_out = cycle_bandwidth))
    val DIV_stage_inst    = Module(new div_stage(bandwidth_in = cycle_bandwidth, bandwidth_out = cycle_bandwidth))

    val maxu_expu_inst    = Module(new maxu_expu)// .io 
    val expu_divu_inst    = Module(new expu_divu)// .io 


    MAX_stage_inst.maxu_i        <> data_in_stage1
    MAX_stage_inst.maxu_maxexp_o <> maxu_expu_inst.maxu_maxexp_i
    
    EXP_stage_inst.input_expu_i  <> data_in_stage2
    EXP_stage_inst.maxexp_expu_i <> maxu_expu_inst.maxexp_expu_o 
    EXP_stage_inst.expu_expdiv_o <> expu_divu_inst.expu_expdiv_i

    DIV_stage_inst.input_divu_i  <> data_in_stage3
    DIV_stage_inst.expdiv_divu_i <> expu_divu_inst.expdiv_divu_o
    DIV_stage_inst.divu_o        <> data_out
}


// object softmax_top extends App {
//     (new ChiselStage).emitVerilog(new softmax_top, args)
// }

