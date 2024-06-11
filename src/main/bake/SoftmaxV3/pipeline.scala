package pipeline

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._
import define.function._


class max_exp extends Bundle { 
    val max_sign  = UInt(1.W)
    val max_exp   = UInt(exp_bitwidth.W)
    val max_frac  = UInt(frac_bitwidth.W)
    val batch_num = UInt(log2Up(maxBatch).W)

    val sign_vec  = Vec(cycle_bandwidth, UInt(1.W))
    val exp_vec   = Vec(cycle_bandwidth, UInt(exp_bitwidth.W))
    val frac_vec  = Vec(cycle_bandwidth, UInt(frac_bitwidth.W))
}

class maxu_expu extends Module {  
    val maxu_maxexp_i = IO(Flipped(Decoupled(new max_exp)))
    val maxexp_expu_o  = IO(Decoupled(new max_exp))
    
    maxexp_expu_o.valid  := maxu_maxexp_i.valid  
    maxu_maxexp_i.ready := maxexp_expu_o.ready   

    val maxu_expu_hs = maxexp_expu_o.valid & maxu_maxexp_i.ready

    maxexp_expu_o.bits.max_sign  := hs_uint_dff(maxu_expu_hs, 1.U,                       0.U,                                                       maxu_maxexp_i.bits.max_sign  )
    maxexp_expu_o.bits.max_exp   := hs_uint_dff(maxu_expu_hs, exp_bitwidth.U,            0.U,                                                       maxu_maxexp_i.bits.max_exp   )
    maxexp_expu_o.bits.max_frac  := hs_uint_dff(maxu_expu_hs, frac_bitwidth.U,           0.U,                                                       maxu_maxexp_i.bits.max_frac  )
    maxexp_expu_o.bits.batch_num := hs_uint_dff(maxu_expu_hs, log2Up(maxBatch).U,        0.U,                                                       maxu_maxexp_i.bits.batch_num )
    
    maxexp_expu_o.bits.sign_vec := hs_uvec_dff(maxu_expu_hs, 1.U,              cycle_bandwidth, VecInit(Seq.fill(cycle_bandwidth)(0.U(1.W))),             maxu_maxexp_i.bits.sign_vec)
    maxexp_expu_o.bits.exp_vec  := hs_uvec_dff(maxu_expu_hs, exp_bitwidth.U,   cycle_bandwidth, VecInit(Seq.fill(cycle_bandwidth)(0.U(exp_bitwidth.W))),  maxu_maxexp_i.bits.exp_vec )
    maxexp_expu_o.bits.frac_vec := hs_uvec_dff(maxu_expu_hs, frac_bitwidth.U,  cycle_bandwidth, VecInit(Seq.fill(cycle_bandwidth)(0.U(frac_bitwidth.W))), maxu_maxexp_i.bits.frac_vec)
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

