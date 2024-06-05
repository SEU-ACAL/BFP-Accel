package pipeline

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._
import define.function._


class max_shift extends Bundle { 
    val max      = UInt(exp_bitwidth.W)
    val idx      = UInt(log2Up(cycle_bandwidth).W)
    val sign_vec = Vec(cycle_bandwidth, UInt(1.W))
    val exp_vec  = Vec(cycle_bandwidth, UInt(exp_bitwidth.W))
    val frac_vec = Vec(cycle_bandwidth, UInt(frac_bitwidth.W))
}

class ctrl_maxshift extends Bundle {val stall_en = Bool()}
class ctrl_shiftsub extends Bundle {val stall_en = Bool()}

class maxu_shiftu extends Module {  
    val ctrl_maxshift_i   = IO(Input(new ctrl_maxshift))
    val maxu_maxshift_i   = IO(Flipped(Decoupled(new max_shift)))
    val maxshift_shiftu_o = IO(Decoupled(new max_shift))
    
    maxshift_shiftu_o.valid         := Mux(~ctrl_maxshift_i.stall_en, maxu_maxshift_i.valid, false.B)
    maxu_maxshift_i.ready           := Mux(~ctrl_maxshift_i.stall_en, maxshift_shiftu_o.ready, false.B)

    val maxu_shiftu_hs = maxshift_shiftu_o.valid & maxu_maxshift_i.ready

    maxshift_shiftu_o.bits.max      := hs_uint_dff(maxu_shiftu_hs, exp_bitwidth.U,                       0.U,                                                       maxu_maxshift_i.bits.max )
    maxshift_shiftu_o.bits.idx      := hs_uint_dff(maxu_shiftu_hs, log2Up(cycle_bandwidth).U,           0.U,                                                       maxu_maxshift_i.bits.idx )
    maxshift_shiftu_o.bits.sign_vec := hs_uvec_dff(maxu_shiftu_hs, 1.U,              cycle_bandwidth, VecInit(Seq.fill(cycle_bandwidth)(0.U(1.W))),             maxu_maxshift_i.bits.sign_vec)
    maxshift_shiftu_o.bits.exp_vec  := hs_uvec_dff(maxu_shiftu_hs, exp_bitwidth.U,   cycle_bandwidth, VecInit(Seq.fill(cycle_bandwidth)(0.U(exp_bitwidth.W))),  maxu_maxshift_i.bits.exp_vec )
    maxshift_shiftu_o.bits.frac_vec := hs_uvec_dff(maxu_shiftu_hs, frac_bitwidth.U,  cycle_bandwidth, VecInit(Seq.fill(cycle_bandwidth)(0.U(frac_bitwidth.W))), maxu_maxshift_i.bits.frac_vec)
}


class max_ld extends Bundle { val max_exp = UInt(exp_bitwidth.W)}
class maxu_ldu extends Module {  
    val maxu_maxld_i = IO(Flipped(Decoupled(new max_ld)))
    val maxld_ldu_o  = IO(Decoupled(new max_ld))

    maxld_ldu_o.valid   := maxu_maxld_i.valid
    maxu_maxld_i.ready  := maxld_ldu_o.ready

    val maxu_ldu_hs = maxld_ldu_o.valid & maxu_maxld_i.ready

    maxld_ldu_o.bits.max_exp  := hs_uint_dff(maxu_ldu_hs, exp_bitwidth.U,  0.U, maxu_maxld_i.bits.max_exp )
}


class shift_sub extends Bundle { 
    val idx      = UInt(log2Up(cycle_bandwidth).W)
    val sign_vec = Vec(cycle_bandwidth, UInt(1.W))
    val frac_vec = Vec(cycle_bandwidth, UInt(frac_bitwidth.W))
}
class shiftu_subu extends Module {  
    val ctrl_shiftsub_i  = IO(Input(new ctrl_maxshift))
    val shift_shiftsub_i = IO(Flipped(Decoupled(new shift_sub)))
    val shiftsub_sub_o   = IO(Decoupled(new shift_sub))

    shiftsub_sub_o.valid    := Mux(~ctrl_shiftsub_i.stall_en, shift_shiftsub_i.valid,   false.B)
    shift_shiftsub_i.ready  := Mux(~ctrl_shiftsub_i.stall_en, shiftsub_sub_o.ready, false.B)

    val shiftu_subu_hs = shiftsub_sub_o.valid & shift_shiftsub_i.ready

    shiftsub_sub_o.bits.idx      := hs_uint_dff(shiftu_subu_hs, log2Up(cycle_bandwidth).U,           0.U,                                                       shift_shiftsub_i.bits.idx )
    shiftsub_sub_o.bits.sign_vec := hs_uvec_dff(shiftu_subu_hs, 1.U,              cycle_bandwidth, VecInit(Seq.fill(cycle_bandwidth)(0.U(1.W))),             shift_shiftsub_i.bits.sign_vec)
    shiftsub_sub_o.bits.frac_vec := hs_uvec_dff(shiftu_subu_hs, frac_bitwidth.U,  cycle_bandwidth, VecInit(Seq.fill(cycle_bandwidth)(0.U(frac_bitwidth.W))), shift_shiftsub_i.bits.frac_vec)
}


class sub_exp extends Bundle { val frac_vec = (Vec(cycle_bandwidth, UInt(frac_bitwidth.W)))}
class subu_expu extends Module {  
    val subu_subexp_i  = IO(Flipped(Decoupled(new sub_exp)))
    val subexp_expu_o  = IO(Decoupled(new sub_exp))

    subexp_expu_o.valid   := subu_subexp_i.valid
    subu_subexp_i.ready   := subexp_expu_o.ready

    val subu_expu_hs = subexp_expu_o.valid & subu_subexp_i.ready

    subexp_expu_o.bits.frac_vec  := hs_uvec_dff(subu_expu_hs, exp_bitwidth.U, cycle_bandwidth, VecInit(Seq.fill(cycle_bandwidth)(0.U(frac_bitwidth.W))), subu_subexp_i.bits.frac_vec )
}


class ld_lut  extends Bundle { val lut_set_idx = UInt(exp_bitwidth.W) }
class ldu_lut extends Module {  
    val ldu_ldlut_i  = IO(Flipped(Decoupled(new ld_lut)))
    val ldlut_lut_o  = IO(Decoupled(new ld_lut))

    ldlut_lut_o.valid  := ldu_ldlut_i.valid
    ldu_ldlut_i.ready  := ldlut_lut_o.ready

    val ldu_lut_hs = ldlut_lut_o.valid & ldu_ldlut_i.ready

    ldlut_lut_o.bits.lut_set_idx  := hs_uint_dff(ldu_lut_hs, exp_bitwidth.U,  0.U, ldu_ldlut_i.bits.lut_set_idx)
}

class lut_ld  extends Bundle { val value_state = UInt(2.W) } // 01 normal_value, 10 overflow, 11 underflow, 00 load_unfinish 
class lut_ldu extends Module {  
    val lut_lutld_i  = IO(Flipped(Decoupled(new lut_ld)))
    val lutld_ldu_o  = IO(Decoupled(new lut_ld))

    lutld_ldu_o.valid  := lut_lutld_i.valid
    lut_lutld_i.ready  := lutld_ldu_o.ready

    val lut_ldu_hs = lutld_ldu_o.valid & lut_lutld_i.ready

    lutld_ldu_o.bits.value_state  := hs_uint_dff(lut_ldu_hs, 2.U,  0.U, lut_lutld_i.bits.value_state )
}


class exp_lut extends Bundle { val raddr  = Vec(cycle_bandwidth, UInt(lutidx_bitwidth.W)) }
class expu_lut extends Module {  
    val expu_explut_i  = IO(Flipped(Decoupled(new exp_lut)))
    val explut_lut_o   = IO(Decoupled(new exp_lut))

    explut_lut_o.valid   := expu_explut_i.valid
    expu_explut_i.ready  := explut_lut_o.ready

    val expu_lut_hs = expu_explut_i.valid & expu_explut_i.ready

    explut_lut_o.bits.raddr  := hs_uvec_dff(expu_lut_hs, lutidx_bitwidth.U, cycle_bandwidth,  VecInit(Seq.fill(cycle_bandwidth)(0.U(lutidx_bitwidth.W))), expu_explut_i.bits.raddr )
}

class lut_exp extends Bundle { val rdata = Vec(cycle_bandwidth, UInt(expvalue_bitwidth.W))}
class lut_expu extends Module {  
    val lut_lutexp_i  = IO(Flipped(Decoupled(new lut_exp)))
    val lutexp_exp_o  = IO(Decoupled(new lut_exp))

    lutexp_exp_o.valid  := lut_lutexp_i.valid
    lut_lutexp_i.ready  := lutexp_exp_o.ready

    val lut_expu_hs = lutexp_exp_o.valid & lut_lutexp_i.ready

    lutexp_exp_o.bits.rdata  := hs_uvec_dff(lut_expu_hs, expvalue_bitwidth.U, cycle_bandwidth, VecInit(Seq.fill(cycle_bandwidth)(0.U(expvalue_bitwidth.W))), lut_lutexp_i.bits.rdata )
}


class lut_dma extends Bundle { 
    val waddr   = UInt(log2Up(fullSet_size).W)
    val wlen    = UInt(log2Up(dma_burst_len).W)
}
class lut_dma_regs extends Module {  
    val lut_lutdma_i  = IO(Flipped(Decoupled(new lut_dma)))
    val lutdma_dma_o  = IO(Decoupled(new lut_dma))

    lutdma_dma_o.valid  := lut_lutdma_i.valid
    lut_lutdma_i.ready  := lutdma_dma_o.ready

    val lut_dma_hs = lutdma_dma_o.valid & lut_lutdma_i.ready

    lutdma_dma_o.bits.waddr := hs_uint_dff(lut_dma_hs, log2Up(fullSet_size).U, 0.U, lut_lutdma_i.bits.waddr )
    lutdma_dma_o.bits.wlen  := hs_uint_dff(lut_dma_hs, log2Up(dma_burst_len).U, 0.U, lut_lutdma_i.bits.wlen )
}

class dma_lut extends Bundle { 
    val wdata   = Vec(2, UInt(bus_width.W))
    val wlast   = Bool()
}
class dma_lut_regs extends Module {  
    val dma_dmalut_i  = IO(Flipped(Decoupled(new dma_lut)))
    val dmalut_lut_o  = IO(Decoupled(new dma_lut))

    dmalut_lut_o.valid  := dma_dmalut_i.valid
    dma_dmalut_i.ready  := dmalut_lut_o.ready

    val dma_lut_hs = dmalut_lut_o.valid & dma_dmalut_i.ready

    dmalut_lut_o.bits.wdata  := hs_uvec_dff(dma_lut_hs, bus_width.U, 2, VecInit(Seq.fill(2)(0.U(bus_width.W))), dma_dmalut_i.bits.wdata )
    dmalut_lut_o.bits.wlast  := hs_bool_dff(dma_lut_hs, false.B, dma_dmalut_i.bits.wlast )
}

class dma_dram extends Bundle { val raddr  = UInt(log2Up(fullSet_size).W)}
class dma_dram_regs extends Module {  
    val dma_dmadram_i    = IO(Flipped(Decoupled(new dma_dram)))
    val dmadram_dram_o   = IO(Decoupled(new dma_dram))

    dmadram_dram_o.valid   := dma_dmadram_i.valid
    dma_dmadram_i.ready  := dmadram_dram_o.ready

    val dma_dram_hs = dmadram_dram_o.valid & dma_dmadram_i.ready

    dmadram_dram_o.bits.raddr  := hs_uint_dff(dma_dram_hs, frac_bitwidth.U, 0.U, dma_dmadram_i.bits.raddr )
}

class dram_dma extends Bundle { val rdata  = Vec(2, UInt((bus_width).W))}
class dram_dma_regs extends Module {  
    val dram_dramdma_i  = IO(Flipped(Decoupled(new dram_dma)))
    val dramdma_dma_o   = IO(Decoupled(new dram_dma))

    dramdma_dma_o.valid  := dram_dramdma_i.valid
    dram_dramdma_i.ready  := dramdma_dma_o.ready

    val dram_dma_hs = dramdma_dma_o.valid & dram_dramdma_i.ready

    dramdma_dma_o.bits.rdata  := hs_uvec_dff(dram_dma_hs, bus_width.U, 2, VecInit(Seq.fill(2)(0.U(bus_width.W))), dram_dramdma_i.bits.rdata )
}


class ld_exp extends Bundle { val value_state = UInt(2.W)} // 01 normal_value, 10 overflow, 11 underflow, 00 load_unfinish 
class ldu_expu extends Module {  
    val ldu_ldexp_i     = IO(Flipped(Decoupled(new ld_exp)))
    val lutexp_exp_o    = IO(Decoupled(new ld_exp))

    lutexp_exp_o.valid  := ldu_ldexp_i.valid
    ldu_ldexp_i.ready   := lutexp_exp_o.ready

    val ldu_expu_hs = lutexp_exp_o.valid & ldu_ldexp_i.ready

    lutexp_exp_o.bits.value_state  := hs_uint_dff(ldu_expu_hs, 2.U,  0.U, ldu_ldexp_i.bits.value_state )
}




class exp_add extends Bundle { val frac_vec = Vec(cycle_bandwidth, UInt(frac_bitwidth.W)) }
class expu_addu extends Module {  
    val expu_expadd_i  = IO(Flipped(Decoupled(new exp_add)))
    val expadd_addu_o  = IO(Decoupled(new exp_add))

    expadd_addu_o.valid  := expu_expadd_i.valid
    expu_expadd_i.ready  := expadd_addu_o.ready

    val expu_addu_hs = expadd_addu_o.valid & expu_expadd_i.ready

    expadd_addu_o.bits.frac_vec  := hs_uvec_dff(expu_addu_hs, frac_bitwidth.U, cycle_bandwidth, VecInit(Seq.fill(cycle_bandwidth)(0.U(frac_bitwidth.W))), expu_expadd_i.bits.frac_vec )
}



class add_div extends Bundle { 
    val sum      = UInt(frac_bitwidth.W)
    val frac_vec = Vec(cycle_bandwidth, UInt(frac_bitwidth.W))
}
class addu_divu extends Module {  
    val addu_adddiv_i  = IO(Flipped(Decoupled(new add_div)))
    val adddiv_divu_o  = IO(Decoupled(new add_div))

    adddiv_divu_o.valid  := addu_adddiv_i.valid
    addu_adddiv_i.ready  := adddiv_divu_o.ready

    val addu_divu_hs = adddiv_divu_o.valid & addu_adddiv_i.ready

    adddiv_divu_o.bits.sum        := hs_uint_dff(addu_divu_hs, frac_bitwidth.U,                     0.U, addu_adddiv_i.bits.sum )
    adddiv_divu_o.bits.frac_vec   := hs_uvec_dff(addu_divu_hs, frac_bitwidth.U, cycle_bandwidth, VecInit(Seq.fill(cycle_bandwidth)(0.U(frac_bitwidth.W))), addu_adddiv_i.bits.frac_vec )
}