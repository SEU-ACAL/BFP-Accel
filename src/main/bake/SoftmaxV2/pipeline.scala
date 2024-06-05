package pipeline

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._
import define.function._


class sort_exp extends Bundle { 
    val max_sign  = UInt(1.W)
    val max_exp   = UInt(exp_bitwidth.W)
    val max_frac  = UInt(frac_bitwidth.W)
    val batch_num = UInt(log2Up(maxBatch).W)

    val sign_vec  = Vec(cycle_bandwidth, UInt(1.W))
    val exp_vec   = Vec(cycle_bandwidth, UInt(exp_bitwidth.W))
    val frac_vec  = Vec(cycle_bandwidth, UInt(frac_bitwidth.W))
}

class sortu_expu extends Module {  
    val sortu_sortexp_i = IO(Flipped(Decoupled(new sort_exp)))
    val sortexp_expu_o  = IO(Decoupled(new sort_exp))
    
    sortexp_expu_o.valid  := sortu_sortexp_i.valid  
    sortu_sortexp_i.ready := sortexp_expu_o.ready   

    val sortu_expu_hs = sortexp_expu_o.valid & sortu_sortexp_i.ready

    sortexp_expu_o.bits.max_sign  := hs_uint_dff(sortu_expu_hs, 1.U,                       0.U,                                                       sortu_sortexp_i.bits.max_sign  )
    sortexp_expu_o.bits.max_exp   := hs_uint_dff(sortu_expu_hs, exp_bitwidth.U,            0.U,                                                       sortu_sortexp_i.bits.max_exp   )
    sortexp_expu_o.bits.max_frac  := hs_uint_dff(sortu_expu_hs, frac_bitwidth.U,           0.U,                                                       sortu_sortexp_i.bits.max_frac  )
    sortexp_expu_o.bits.batch_num := hs_uint_dff(sortu_expu_hs, log2Up(maxBatch).U,        0.U,                                                       sortu_sortexp_i.bits.batch_num )
    
    sortexp_expu_o.bits.sign_vec := hs_uvec_dff(sortu_expu_hs, 1.U,              cycle_bandwidth, VecInit(Seq.fill(cycle_bandwidth)(0.U(1.W))),             sortu_sortexp_i.bits.sign_vec)
    sortexp_expu_o.bits.exp_vec  := hs_uvec_dff(sortu_expu_hs, exp_bitwidth.U,   cycle_bandwidth, VecInit(Seq.fill(cycle_bandwidth)(0.U(exp_bitwidth.W))),  sortu_sortexp_i.bits.exp_vec )
    sortexp_expu_o.bits.frac_vec := hs_uvec_dff(sortu_expu_hs, frac_bitwidth.U,  cycle_bandwidth, VecInit(Seq.fill(cycle_bandwidth)(0.U(frac_bitwidth.W))), sortu_sortexp_i.bits.frac_vec)
}


class exp_div extends Bundle { 
    val sum       = UInt(sum_bitwidth.W)
}

class expu_divu extends Module {  
    val expu_expdiv_i  = IO(Flipped(Decoupled(new exp_div)))
    val expdiv_divu_o  = IO(Decoupled(new exp_div))
    
    expdiv_divu_o.valid := expu_expdiv_i.valid 
    expu_expdiv_i.ready := expdiv_divu_o.ready 

    val expu_divu_hs = expdiv_divu_o.valid & expu_expdiv_i.ready

    expdiv_divu_o.bits.sum   := hs_uint_dff(expu_divu_hs, sum_bitwidth.U,            0.U,                                                       expu_expdiv_i.bits.sum   )
}