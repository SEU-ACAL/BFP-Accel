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

class Div_i extends Bundle {
    val divisor = Vec(cycle_bandwidth, UInt(sum_bitwidth.W))
}




class softmax extends Module {
    val data_in  = IO(Flipped(Decoupled(new SoftMax_Input)))

    // val div_in   = IO(Flipped(Decoupled(new Div_i)))

    val data_out = IO(Decoupled(new SoftMax_Output))

    // ======================= FSM ==========================
    val data_in_hs  = WireInit(false.B)
    val run_done    = WireInit(false.B)
    val data_out_hs = WireInit(false.B)

    data_in_hs  := data_in.valid && data_in.ready
    // data_out_hs := data_out.valid && data_out.ready

    // val state  = WireInit(sIdle)
    // state := fsm(data_in_hs, run_done, data_out_hs)
    // =====================================================
    val MAX_stage_inst    = Module(new MAX_stage(bandwidth_in = datain_bandwidth, bandwidth_out = cycle_bandwidth))
    // val EXP_stage_inst    = Module(new EXP_stage(bandwidth_in = cycle_bandwidth))
    // val DIV_stage_inst    = Module(new DIV_stage(bandwidth_in = cycle_bandwidth))

    val OUT_inst          = Module(new outu)

    val maxu_expu_inst     = Module(new maxu_expu   )// .io 
    // val expu_divu_inst     = Module(new expu_divu    )// .io 
    
    // data_in.ready           := Max_inst.maxu_i.ready


    // ================== max_stage ==========================
    // val test = RegInit(0.U(16.W))
    // test :=  data_in.bits.raw_data(0)
    // printf("%d ", test)
    
    MAX_stage_inst.maxu_i                  <> data_in

    MAX_stage_inst.maxu_maxexp_o          <> maxu_expu_inst.maxu_maxexp_i
    maxu_expu_inst.maxexp_expu_o.ready := true.B
    // EXP_stage_inst.maxexp_expu_i           <> maxu_expu_inst.maxexp_expu_o 

    
    // ================== EXP_stage ==========================

    
    // EXP_stage_inst.expu_expdiv_o <> expu_divu_inst.expu_expdiv_i 
    // DIV_stage_inst.expdiv_div_i  <> expu_divu_inst.expdiv_divu_o
    
    
    // ================== DIV_stage ==========================
    // DIV_stage_inst.divisor_i       <> div_in   




    // ================== outu =============================
    MAX_stage_inst.maxu_outu_o    <>  OUT_inst.maxu_outu_i
    // DIV_stage_inst.div_out_o      <>  OUT_inst.divu_outu_i
    
    OUT_inst.outu_o <> data_out



}






class outu extends Module {
    val maxu_outu_i    = IO(Flipped(Valid(new max_out)))
    // val divu_outu_i    = IO(Flipped(Valid(new Bundle{ val res_data = Vec(dataout_bandwidth, UInt(bitwidth.W))})))

    val outu_o          = IO(Decoupled(new SoftMax_Output))
    
    when (maxu_outu_i.valid) {
        outu_o.valid        := true.B
        for (i <- 0 until dataout_bandwidth) {
            outu_o.bits.res_data(i) := maxu_outu_i.bits.data_out
        }
    }.otherwise {
        outu_o.valid        := false.B
        outu_o.bits.res_data := VecInit(Seq.fill(cycle_bandwidth)(0.U(bitwidth.W)))
    }
    
    // when (divu_outu_i.valid) {
    //     // for (i <- 0 until dataout_bandwidth) {
    //         outu_o.valid        := true.B
    //         outu_o.bits.res_data := divu_outu_i.bits.res_data
    //     // }
    // }.otherwise {
    //     outu_o.valid        := false.B
    //     outu_o.bits.res_data := VecInit(Seq.fill(cycle_bandwidth)(0.U(bitwidth.W)))
    // }

}


