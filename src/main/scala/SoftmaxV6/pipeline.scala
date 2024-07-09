package pipeline

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._
import define.function._


class max_exp extends Bundle { val max  = UInt(bitwidth.W) }

class maxu_expu extends Module {  
    val maxu_maxexp_i  = IO(Flipped(Decoupled(new max_exp)))
    val maxexp_expu_o  = IO(Decoupled(new max_exp))
    
    maxexp_expu_o.valid  := maxu_maxexp_i.valid  
    maxu_maxexp_i.ready  := maxexp_expu_o.ready   

    val maxu_expu_hs = maxexp_expu_o.valid & maxu_maxexp_i.ready

    maxexp_expu_o.bits.max   := hs_uint_dff(maxu_expu_hs, bitwidth.U, 0.U, maxu_maxexp_i.bits.max)
}


class exp_div extends Bundle { val sum       = UInt(sum_bitwidth.W)}

class expu_divu extends Module {  
    val expu_expdiv_i  = IO(Flipped(Decoupled(new exp_div)))
    val expdiv_divu_o  = IO(Decoupled(new exp_div))
    
    expdiv_divu_o.valid := expu_expdiv_i.valid 
    expu_expdiv_i.ready := expdiv_divu_o.ready 

    val expu_divu_hs = expdiv_divu_o.valid & expu_expdiv_i.ready

    expdiv_divu_o.bits.sum   := hs_uint_dff(expu_divu_hs, sum_bitwidth.U, 0.U, expu_expdiv_i.bits.sum)
}

