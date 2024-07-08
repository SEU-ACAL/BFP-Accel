package softmax

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._

import MAX_stage._
import EXP_stage._
import DIV_stage._
import pipeline._

import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.annotations.MemoryLoadFileType



class SoftMax_Input extends Bundle {
    val raw_data = Vec(datain_bandwidth, UInt(datain_bitwidth.W))
}

class SoftMax_Output extends Bundle {
    val res_data = Vec(dataout_bandwidth, UInt(dataout_bitwidth.W))
}



class softmax extends Module {
    val data_in  = IO(Flipped(Decoupled(new SoftMax_Input)))


    val data_out = IO(Decoupled(new SoftMax_Output))

   
    val MAX_stage_inst    = Module(new MAX_stage(bandwidth_in = cycle_bandwidth, bandwidth_out = cycle_bandwidth))
    val EXP_stage_inst    = Module(new EXP_stage(bandwidth_in = cycle_bandwidth, bandwidth_out = cycle_bandwidth))
    val DIV_stage_inst    = Module(new DIV_stage(bandwidth_in = cycle_bandwidth, bandwidth_out = cycle_bandwidth))

    val maxu_expu_inst    = Module(new maxu_expu)// .io 
    val expu_divu_inst    = Module(new expu_divu)// .io 
    



}


object softmax extends App {
    (new ChiselStage).emitVerilog(new softmax, args)
}

