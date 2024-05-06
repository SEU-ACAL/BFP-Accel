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
import DMA._
import DRAM._
import pipeline._
import CTRL._

import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.annotations.MemoryLoadFileType



class SoftMax_Input extends Bundle { val raw_data = Vec(datain_bandwidth, UInt(bitwidth.W)) }

class SoftMax_Output extends Bundle { val res_data = Vec(datain_bandwidth, UInt(bitwidth.W)) }

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

    val Max_inst    = Module(new MaxComparator(numElements = datain_bandwidth)).io 
    val Shift_inst  = Module(new frac_shift(bandwidth = cycle_bandwidth)).io 
    val LDU_inst    = Module(new load_exp(bitwidth = exp_bitwidth, bandwidth = cycle_bandwidth)).io 
    val Sub_inst    = Module(new sub_max(bitwidth = exp_bitwidth, bandwidth = cycle_bandwidth)).io 
    val LUT_inst    = Module(new ExpLUT(depth = lut_depth, width = lut_width, set = lut_set)).io 
    val EXP_inst    = Module(new get_exp(bitwidth = exp_bitwidth, bandwidth = cycle_bandwidth)).io 
    // val Shift_inst2 = Module(new frac_shift2).io 
    val ADD_inst    = Module(new adder_tree(bitwidth = exp_bitwidth, bandwidth = cycle_bandwidth)).io 
    val DIV_inst    = Module(new div(bitwidth = exp_bitwidth, bandwidth = cycle_bandwidth)).io 
    val DMA_inst    = Module(new DMA).io 
    val DRAM_inst   = Module(new DualPortDRAM(depth = dram_depth, width = bus_width)) 
    val CTRL_inst   = Module(new CTRL).io 


    val maxu_shiftu_inst = Module(new maxu_shiftu   )// .io 
    val maxu_ldu_inst    = Module(new maxu_ldu      )// .io 
    val shiftu_subu_inst = Module(new shiftu_subu   )// .io 
    val subu_expu_inst   = Module(new subu_expu     )// .io 
    val ldu_lut_inst     = Module(new ldu_lut       )// .io 
    val lut_ldu_inst     = Module(new lut_ldu       )// .io 
    val ldu_expu_inst    = Module(new ldu_expu      )// .io 
    val expu_lut_inst    = Module(new expu_lut      )// .io 
    val lut_expu_inst    = Module(new lut_expu      )// .io 
    val lut_dma_inst     = Module(new lut_dma_regs  )// .io 
    val dma_lut_inst     = Module(new dma_lut_regs  )// .io 
    val dma_dram_inst    = Module(new dma_dram_regs )// .io
    val dram_dma_inst    = Module(new dram_dma_regs )// .io
    val expu_addu_inst   = Module(new expu_addu     )// .io 
    val addu_divu_inst   = Module(new addu_divu     )// .io 
    
    data_in.ready           := Max_inst.maxu_i.ready
    // ====================== load DRAM ==========================
    loadMemoryFromFileInline(DRAM_inst.mem, "./src/main/scala/Softmax/lut_data/lut_data.hex", MemoryLoadFileType.Hex); 

    // ======================= 一次对指 ===========================
    Max_inst.maxu_i                  <>  data_in

    Max_inst.maxu_shiftu_o           <> maxu_shiftu_inst.maxu_maxshift_i
    Shift_inst.maxu_shiftu_i         <> maxu_shiftu_inst.maxshift_shiftu_o


    // when (Shift_inst.shiftu_subu_o.valid) {
    //     printf("step 1 share exp [");
    //     for (i <- 0 until cycle_bandwidth) { printf("(%d) ", Shift_inst.shiftu_subu_o.bits.frac_vec(i));}
    //     printf("]\n");
    // }

    // ======================= 减最大值 ===========================
    Shift_inst.shiftu_subu_o    <> shiftu_subu_inst.shift_shiftsub_i 
    Sub_inst.shift_subu_i       <> shiftu_subu_inst.shiftsub_sub_o

    // when (Shift_inst.shiftu_subu_o.valid) {
    //     printf("step 2 Minus the max value [");
    //     for (i <- 0 until cycle_bandwidth) { printf("(%d) ", Sub_inst.subu_expu_o.bits.frac_vec(i));}
    //     printf("]\n");
    // }

    // ======================= 预加载表 ===========================
    // maxu to ldu
    Max_inst.maxu_ldu_o             <> maxu_ldu_inst.maxu_maxld_i
    LDU_inst.maxu_ldu_i             <> maxu_ldu_inst.maxld_ldu_o
    // ldu to lut 
    LUT_inst.lut_ldu_o              <> lut_ldu_inst.lut_lutld_i 
    LDU_inst.lut_ldu_i              <> lut_ldu_inst.lutld_ldu_o  
    // lut to dma
    LUT_inst.lut_dma_o              <> lut_dma_inst.lut_lutdma_i
    DMA_inst.lut_dma_i              <> lut_dma_inst.lutdma_dma_o
    // dma to lut
    DMA_inst.dma_lut_o              <> dma_lut_inst.dma_dmalut_i
    LUT_inst.dma_lut_i              <> dma_lut_inst.dmalut_lut_o
    // dma to dram
    DMA_inst.dma_dram_o             <> dma_dram_inst.dma_dmadram_i 
    DRAM_inst.io.dma_dram_i         <> dma_dram_inst.dmadram_dram_o
    // dram to dma
    DRAM_inst.io.dram_dma_o         <> dram_dma_inst.dram_dramdma_i
    DMA_inst.dram_dma_i             <> dram_dma_inst.dramdma_dma_o 
    // lut to ldu
    LDU_inst.ldu_lut_o              <> ldu_lut_inst.ldu_ldlut_i    
    LUT_inst.ldu_lut_i              <> ldu_lut_inst.ldlut_lut_o 
    // ldu to expu
    LDU_inst.ldu_expu_o             <> ldu_expu_inst.ldu_ldexp_i 
    EXP_inst.ldu_expu_i             <> ldu_expu_inst.lutexp_exp_o


    // ======================= 查表 ===========================
    Sub_inst.subu_expu_o   <> subu_expu_inst.subu_subexp_i
    EXP_inst.sub_expu_i    <> subu_expu_inst.subexp_expu_o   

    EXP_inst.expu_lut_o    <> expu_lut_inst.expu_explut_i
    LUT_inst.expu_lut_i    <> expu_lut_inst.explut_lut_o
    LUT_inst.lut_expu_o    <> lut_expu_inst.lut_lutexp_i
    EXP_inst.lut_expu_i    <> lut_expu_inst.lutexp_exp_o

    // when (Shift_inst.shiftu_subu_o.valid) {
    //     printf("step 3 find data in lut [");
    //     for (i <- 0 until cycle_bandwidth) { printf("(%d) ", EXP_inst.expu_addu_o.bits.frac_vec(i));}
    //     printf("]\n");
    // }



    // ======================= 二次对指 ===========================
    // Shift_inst2.expu_shiftu_i <> EXP_inst.expu_shiftu_o
    // ======================= 求分母 ===========================    
    EXP_inst.expu_addu_o     <> expu_addu_inst.expu_expadd_i
    ADD_inst.expu_addu_i     <> expu_addu_inst.expadd_addu_o

    // when (ADD_inst.addu_divu_o.valid) {
    //     printf("step 4 caculate sum = (%d)\n", ADD_inst.addu_divu_o.bits.sum);
    // }

    
    // ======================= 除法 ===========================
    ADD_inst.addu_divu_o    <> addu_divu_inst.addu_adddiv_i
    DIV_inst.addu_divu_i    <> addu_divu_inst.adddiv_divu_o

    when (DIV_inst.divu_o.valid) {
        printf("step 5 div results [");
        for (i <- 0 until cycle_bandwidth) { printf("(%d) ", DIV_inst.divu_o.bits.res_data(i));}
        printf("]\n");
    }


    // ==================== Output ! =========================
    run_done                 := DIV_inst.divu_o.valid
    // data_out.valid           := DIV_inst.divu_o.valid
    // data_out.bits.res_data   := DIV_inst.divu_o.bits.result

    data_out                <>  DIV_inst.divu_o


   // ==================== CTRL =========================

    CTRL_inst.ldu_ctrl_i             <> LDU_inst.ldu_ctrl_o
    maxu_shiftu_inst.ctrl_maxshift_i <> CTRL_inst.ctrl_maxshift_o
    shiftu_subu_inst.ctrl_shiftsub_i <> CTRL_inst.ctrl_shiftsub_o
}









