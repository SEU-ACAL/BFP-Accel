package gelu

import chisel3._
import chisel3.util._
import chisel3.stage._

import gelu_define.MACRO._



class gelu_Input extends Bundle { 
    val raw_data  = Vec(cycle_bandwidth, UInt(bitwidth.W))
    val batch_num = UInt(log2Up(maxBatch).W) 
}

class gelu_Output extends Bundle { val data_o = Vec(cycle_bandwidth, UInt(bitwidth.W))}


class gelu_top extends Module {
    val data_in         = IO(Flipped(Decoupled(new gelu_Input_Stage1)))
    val data_out        = IO(Valid(new gelu_Output))

    // =================== shift =================
    val expu_data_in_valid            = WireInit(false.B)                                                   // Input
    val expu_data_in_bits_batch_num   = WireInit(0.U(log2Up(maxBatch).W))                                   // Input
    val expu_data_in_bits_idx_vec     = WireInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(idx_bitwidth.W))))   // Input
    val expu_data_in_bits_rate_vec    = WireInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(rate_bitwidth.W))))  // Input

    val expu_data_out_valid           = RegInit(false.B)                                                        // output
    val expu_data_out_bits_batch_num  = RegInit(0.U(log2Up(maxBatch).W))                                       // output
    val exp_vec                       = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(expvalue_bitwidth.W))))   // output

    expu_data_in_valid               := subu.data_out.valid
    expu_data_in_bits_batch_num      := subu.data_out.bits.batch_num   
    expu_data_in_bits_idx_vec        := subu.data_out.bits.subu_idx_vec    
    expu_data_in_bits_rate_vec       := subu.data_out.bits.subu_rate_vec    

    


    // =================== lut ===================


    // =================== mul ===================

   

}



