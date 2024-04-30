package softmax

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._

import addu._
import divu._
import expu._
import ldu._
import lut._
import maxu._
import shiftu._
import subu._

import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.annotations.MemoryLoadFileType



class SoftMax_Input extends Bundle {
    val raw_data = Vec(datain_bandwidth, UInt(bitwidth.W))
}

class SoftMax_Output extends Bundle {
    val res_data = Vec(datain_bandwidth, UInt(bitwidth.W))
}

class softmax extends Module {
    val data_in  = IO(Flipped(Decoupled(new SoftMax_Input)))
    val data_out = IO(Decoupled(new SoftMax_Output))

    // ======================= FSM ==========================
    val data_in_hs  = WireInit(false.B)
    val run_done    = WireInit(false.B)
    val data_out_hs = WireInit(false.B)

    data_in_hs  := data_in.valid && data_in.ready
    data_out_hs := data_out.valid && data_out.ready

    val state  = WireInit(sIdle)
    state := fsm(data_in_hs, run_done, data_out_hs)
    // =====================================================

    val sign_vec  = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U(1.W))))
    val exp_vec   = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((exp_bitwidth).W))))
    val frac_vec  = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))
    
    for (i <- 0 until datain_bandwidth) {
        sign_vec(i) := Mux(data_in_hs, data_in.bits.raw_data(i)(bitwidth - 1),               sign_vec(i)) 
        exp_vec(i)  := Mux(data_in_hs, data_in.bits.raw_data(i)(bitwidth - 2, bitwidth - 6), exp_vec(i))  
        frac_vec(i) := Mux(data_in_hs, data_in.bits.raw_data(i)(bitwidth - 7, 0),            frac_vec(i))
    }

    val Max_inst    = Module(new MaxComparator(bitwidth = exp_bitwidth, numElements = datain_bandwidth)).io 
    val Shift_inst1 = Module(new frac_shift1).io 
    val LDU_inst    = Module(new load_exp(bitwidth = exp_bitwidth, numElements = datain_bandwidth)).io 
    val Sub_inst    = Module(new sub_max(bitwidth = exp_bitwidth, numElements = datain_bandwidth)).io 
    val LUT_inst    = Module(new ExpLUT(depth = lut_depth, width = lut_width, set = lut_set)).io 
    val EXP_inst    = Module(new get_exp(bitwidth = exp_bitwidth, numElements = datain_bandwidth)).io 
    // val Shift_inst2 = Module(new frac_shift2).io 
    val ADD_inst    = Module(new adder_tree(bitwidth = exp_bitwidth, numElements = datain_bandwidth)).io 
    val DIV_inst    = Module(new div(bitwidth = exp_bitwidth, numElements = datain_bandwidth)).io 
    val DMA_inst    = Module(new DMA).io 
    val DRAM_inst   = Module(new DualPortDRAM(depth = dram_depth, width = bus_width)) 
    
    data_in.ready           := Max_inst.maxu_i.ready
    // ====================== load DRAM ==========================
    loadMemoryFromFileInline(DRAM_inst.mem, "./fp16_input.hex", MemoryLoadFileType.Hex); 

    // ======================= 一次对指 ===========================
    Max_inst.maxu_i.valid   := data_in.valid && data_in.ready
    for (i <- 0 until datain_bandwidth) { 
        Max_inst.maxu_i.bits.data(i)  := data_in.bits.raw_data(i)(bitwidth - 2, bitwidth - 6)
        Max_inst.maxu_i.bits.sign(i)  := data_in.bits.raw_data(i)(bitwidth - 1)
    }
    Shift_inst1.maxu_shiftu_i <> Max_inst.maxu_shiftu_o  

    when (Shift_inst1.shiftu_subu_o.valid) {
        printf("step 1 一次对指[");
        for (i <- 0 until datain_bandwidth) { printf("(%d) ", Shift_inst1.shiftu_subu_o.bits.frac(i));}
        printf("]\n");
    }

    // ======================= 减最大值 ===========================
    // Sub_inst.max_subu_i    <> Max_inst.max_subu_o
    Sub_inst.shift_subu_i  <> Shift_inst1.shiftu_subu_o 


    // ======================= 预加载表 ===========================
    // maxu to ldu
    LDU_inst.maxu_ldu_i     <> Max_inst.maxu_ldu_o     
    // lut and ldu
    LDU_inst.lut_ldu_i      <> LUT_inst.lut_ldu_o  
    LUT_inst.ldu_lut_i      <> LDU_inst.ldu_lut_o 
    // lut and expu
    EXP_inst.ldu_expu_i     <> LDU_inst.ldu_expu_o 
    // lut and dma
    LUT_inst.dma_lut_i      <> DMA_inst.dma_lut_o
    DMA_inst.lut_dma_i      <> LUT_inst.lut_dma_o
    // dram and dma
    DRAM_inst.io.dma_dram_i <> DMA_inst.dma_dram_o  
    DMA_inst.dram_dma_i     <> DRAM_inst.io.dram_dma_o 


    // ======================= 查表 ===========================
    Sub_inst.subu_expu_o   <> EXP_inst.sub_expu_i   

    LUT_inst.expu_lut_i    <> EXP_inst.expu_lut_o   
    EXP_inst.lut_expu_i    <> LUT_inst.lut_expu_o




    // ======================= 二次对指 ===========================
    // Shift_inst2.expu_shiftu_i <> EXP_inst.expu_shiftu_o
    // ======================= 求分母 ===========================    
    ADD_inst.expu_addu_i     <> EXP_inst.expu_addu_o



    
    // ======================= 除法 ===========================
    DIV_inst.addu_divu_i   <> ADD_inst.addu_divu_o

     


    // ==================== Output ! =========================
    run_done                 := DIV_inst.divu_o.valid
    data_out.valid           := DIV_inst.divu_o.valid
    data_out.bits.res_data   := DIV_inst.divu_o.bits.result
}









