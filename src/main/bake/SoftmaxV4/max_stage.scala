package tb

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._
import define.function._
import define.test._


class max_input extends Bundle { 
    val raw_data    = Vec(MaxinBandwidth, UInt(bitwidth.W))
}

// class max_out extends Bundle { 
//     val data_out   = UInt(bitwidth.W) // 落在无效区间，结果为统一的固定值
// }

class max_exp extends Bundle { 
    val max_sign  = UInt(1.W)
    val max_exp   = UInt(exp_bitwidth.W)
    val max_frac  = UInt(frac_bitwidth.W)
}

class tb(bandwidth_in: Int) extends Module {
    val maxu_i        = IO(Flipped(Decoupled(new max_input)))
    val maxu_maxexp_o = IO(Decoupled(new max_exp))
    // val maxu_outu_o   = IO(Valid(new max_out))

    val SmallTreeCompareDone   = WireInit(false.B)
    val CompareDone            = WireInit(false.B)

    // ======================= FSM ==========================
    val state              = WireInit(sIdle)
    val maxu_i_hs          = maxu_i.ready && maxu_i.valid
    val maxu_maxexp_o_hs   = maxu_maxexp_o.ready && maxu_maxexp_o.valid
    // val maxu_outu_o_hs     = maxu_outu_o.ready && maxu_outu_o.valid
    state := fsm(maxu_i_hs, SmallTreeCompareDone, CompareDone)

    maxu_i.ready         := state === sIdle
    // CycleCounter(state === sRun, state === sIdle, 1)
    // ======================================================
    val counter     = RegInit(0.U(8.W))  
    counter        := Mux(maxu_i_hs, counter + 1.U, 0.U)

    // val max_buffer  = RegInit(VecInit(Seq.fill(32)(0.U(bitwidth.W))))
    val max_buffer  = RegInit(VecInit(Seq.fill(SmallTreeNums)(VecInit(Seq.fill(6)(0.U(bitwidth.W))))))
    val buffer_idx  = RegInit(VecInit(Seq.fill(SmallTreeNums)(0.U(log2Up(6).W))))
    // ===================== 小树比较 ============
    val arbiter   = Module(new RRArbiter(MaxinBandwidth, SmallTreeNums, SmallTreeBandwidth)).io
    arbiter.en   := maxu_i.valid
    // arbiter.data_in.bits    := maxu_i.bits.raw_data

    val comparators = Seq.fill(SmallTreeNums)(Module(new SmallComparatorTree32bit()).io)

    // when (state === sRun) {
        for (i <- 0 until SmallTreeNums) {
            comparators(i).in.valid := Mux(maxu_i_hs && (arbiter.rid.bits === i.U), arbiter.rid.valid, false.B)
            comparators(i).in.bits  := Mux(maxu_i_hs && (arbiter.rid.bits === i.U), maxu_i.bits.raw_data, VecInit(Seq.fill(MaxinBandwidth)(0.U(bitwidth.W))))
            // comparators(i).in.bits  := maxu_i.bits.raw_data
        }
    // }

    // 记录前32次比较树的结果
    when (state === sRun) {
        for (i <- 0 until SmallTreeNums) {
            when (comparators(i).out.valid) {
                max_buffer(i)(buffer_idx(i))  := comparators(i).out.bits
                buffer_idx(i)                 := buffer_idx(i) + 1.U
            }
        }
    }
    // ============== 小树合一 =================
    when (counter === 31.U) { 
        SmallTreeCompareDone := true.B
    }

    when (state === sDone) { // 开启小树归一
        comparators(0).in.valid := true.B
        comparators(0).in.bits  := VecInit(Seq(max_buffer(0), max_buffer(1), 
                                           max_buffer(2).slice(0, 5), max_buffer(3).slice(0, 5), 
                                           max_buffer(4).slice(0, 5), max_buffer(5).slice(0, 5)).flatten)
    }
    
    when (counter === 36.U) {
        CompareDone             := true.B
        SmallTreeCompareDone    := false.B
    }.otherwise { 
        CompareDone := false.B
    }

    val max_reg  = RegInit(0.U(bitwidth.W))  
    when (CompareDone) {
        max_reg := comparators(0).out.bits 
    }

    // ============== 输出 =================
    when (state === sDone) {
        maxu_maxexp_o.valid          := true.B
        maxu_maxexp_o.bits.max_sign  := max_reg(15)    
        maxu_maxexp_o.bits.max_exp   := max_reg(14, 10)
        maxu_maxexp_o.bits.max_frac  := max_reg(9, 0)  
    }.otherwise {
        maxu_maxexp_o.valid          := false.B
        maxu_maxexp_o.bits.max_sign  := 0.U    
        maxu_maxexp_o.bits.max_exp   := 0.U
        maxu_maxexp_o.bits.max_frac  := 0.U
    }
}



// FP16比较器
class ComparatorFP16 (bitwidth: Int) extends Module {
    val io = IO(new Bundle {
        val in  = Input(Vec(2, UInt(bitwidth.W)))
        val max = Output(UInt(bitwidth.W))
    })

    val fpA = io.in(0) 
    val fpB = io.in(1)

    val AGreaterThanB = Mux(fpA(15) =/= fpB(15), fpA(15) < fpB(15), 
                        Mux(fpA(14, 10) =/= fpB(14, 10), fpA(14, 10) > fpB(14, 10), fpA(9, 0) > fpB(9, 0))) 
    io.max := Mux(AGreaterThanB, fpA, fpB)
}

// 小比较树
class SmallComparatorTreeLevel (SmallTreeBandwidth: Int) extends Module {
    val io = IO(new Bundle {
        val in  = Input(Vec(SmallTreeBandwidth, UInt(bitwidth.W)))
        val out = Output(Vec(SmallTreeBandwidth/2, UInt(bitwidth.W)))
    })
    val comparators = Seq.fill(SmallTreeBandwidth/2)(Module(new ComparatorFP16(16)))
    for (i <- 0 until SmallTreeBandwidth/2) {
        comparators(i).io.in := VecInit(io.in(i * 2), io.in(i * 2 + 1))
        io.out(i)            := comparators(i).io.max
    }
}

// 小比较树
class SmallComparatorTree32bit extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Valid(Vec(32, UInt(bitwidth.W))))
        val out = Valid(UInt(bitwidth.W))
    })
    val TreeLevel1 = Module(new SmallComparatorTreeLevel(32)).io
    TreeLevel1.in := io.in.bits

    val TreeLevel2 = Module(new SmallComparatorTreeLevel(16)).io
    TreeLevel2.in := TreeLevel1.out
    
    val TreeLevel2_out = RegInit(VecInit(Seq.fill(8)(0.U(bitwidth.W))))
    TreeLevel2_out := TreeLevel2.out

    val TreeLevel3 = Module(new SmallComparatorTreeLevel(8)).io
    TreeLevel3.in := TreeLevel2_out
    
    val TreeLevel4 = Module(new SmallComparatorTreeLevel(4)).io
    TreeLevel4.in := TreeLevel3.out
    
    val TreeLevel5 = Module(new SmallComparatorTreeLevel(2)).io
    TreeLevel5.in := TreeLevel4.out

    val counter     = RegInit(0.U(8.W))  
    counter        := Mux(io.in.valid, 0.U, counter + 1.U)

    io.out.valid    := counter === 3.U
    io.out.bits     := TreeLevel5.out(0)
}

class RRArbiter (MaxinBandwidth: Int, ComparatorTreeNums: Int, UseComparatorTreeTimes: Int)  extends Module {
    val io = IO(new Bundle{
        // val data_in  = Flipped(Valid(Vec(MaxinBandwidth, UInt(bitwidth.W))))
        val en  = Input(Bool())
        val rid = Valid(UInt(log2Up(ComparatorTreeNums).W))
        // val data_out = Valid(Vec(MaxinBandwidth, UInt(bitwidth.W)))
    })
    val rid  = RegInit(0.U(log2Up(ComparatorTreeNums).W))
    rid    := Mux(~io.en, rid, 
                Mux(rid === (ComparatorTreeNums-1).U, 0.U, rid + 1.U))

    io.rid.bits             := rid   
    io.rid.valid            := io.en   
}

object tb extends App {
    (new ChiselStage).emitVerilog(new tb(MaxinBandwidth), args)
}

