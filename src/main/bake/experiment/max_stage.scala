package experiment

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._
import define.test._


class max_input extends Bundle { 
    val raw_data    = Vec(datain_bandwidth, UInt(bitwidth.W))
}

class max_out extends Bundle { 
    val data_out   = UInt(bitwidth.W) // 落在无效区间，结果为统一的固定值
}

// class max_exp extends Bundle { 
//     val max_sign  = UInt(1.W)
//     val max_exp   = UInt(exp_bitwidth.W)
//     val max_frac  = UInt(frac_bitwidth.W)

//     val batch_num = UInt(log2Up(maxBatch).W)

//     val sign_vec  = Vec(cycle_bandwidth, UInt(1.W))
//     val exp_vec   = Vec(cycle_bandwidth, UInt(exp_bitwidth.W))
//     val frac_vec  = Vec(cycle_bandwidth, UInt(frac_bitwidth.W))
// }

class max_exp extends Bundle { 
    val max_sign  = UInt(1.W)
    val max_exp   = UInt(exp_bitwidth.W)
    val max_frac  = UInt(frac_bitwidth.W)

    val batch_num = UInt(log2Up(maxBatch).W)
}


class MAX_stage(bandwidth_in: Int, bandwidth_out: Int) extends Module {
    val maxu_i        = IO(Flipped(Decoupled(new max_input)))
    val maxu_maxexp_o = IO(Decoupled(new max_exp))
    // val maxu_outu_o   = IO(Valid(new max_out))

    val CompareDone   = WireInit(false.B)
    val MaxmemEmpty   = WireInit(false.B)

    // ======================= FSM ==========================
    val state              = WireInit(sIdle)
    val maxu_i_hs          = maxu_i.ready && maxu_i.valid
    val maxu_maxexp_o_hs   = maxu_maxexp_o.ready && maxu_maxexp_o.valid
    // val maxu_outu_o_hs     = maxu_outu_o.ready && maxu_outu_o.valid
    state := fsm(maxu_i_hs, CompareDone, MaxmemEmpty)

    maxu_i.ready         := state === sIdle
    CycleCounter(state === sRun, state === sIdle, 1)
    // ======================================================
    val counter  = RegInit(0.U(8.W))  
    counter  := Mux(maxu_i_hs, counter + 1.U, 0.U)

    // val sign_mem = Mem(maxmem_depth, Vec(maxmem_width, UInt(1.W)))
    // val exp_mem  = Mem(maxmem_depth, Vec(maxmem_width, UInt(exp_bitwidth.W)))
    // val frac_mem = Mem(maxmem_depth, Vec(maxmem_width, UInt(frac_bitwidth.W)))

    // when (maxu_i_hs) {
    //     sign_mem()()    := maxu_i.bits.raw_data(15)              
    //     exp_mem()()     := maxu_i.bits.raw_data(14, 10)
    //     frac_mem()()    := maxu_i.bits.raw_data(9, 0)           
    // }

    // ===================== 小树比较 ============
    val arbiter              = Module(new RRArbiter(MaxinBandwidth, SmallTreeNums, SmallTreeBandwidth)).io
    arbiter.data_in.valid   := maxu_i.valid
    arbiter.data_in.bits    := maxu_i.bits.raw_data

    val comparators = VecInit(Seq.fill(SmallTreeNums)(Module(new SmallComparatorTree32bit.io)))
    when (maxu_i_hs) {
        comparators(arbiter.rid).in.valid := arbiter.data_out.valid
        comparators(arbiter.rid).in.bits  := arbiter.data_out.bits 
    }.otherwise {
        for (i <- 0 until SmallTreeNums) {
            comparators(i).in.valid := false.B
            comparators(i).in.bits  := VecInit(Seq.fill(SmallTreeBandwidth)(0.U(bitwidth.W)))
        }
    }


    // ============== 小树合一 =================
    when (counter === 31.U) {// 开启小树归一
        comparators(0).in.valid := arbiter.data_out.valid
        comparators(0).in.bits  := arbiter.data_out.bits  
    }
    
    when (counter === 36.U) {
        CompareDone := true.B
    }.otherwise { 
        CompareDone := false.B
    }

    val max_reg  = RegInit(0.U(bitwidth.W))  
    when (CompareDone) {
        max_reg := comparators(0).out.bits 
    }

    // ============== 输出 =================
    when (state === sDone) {
        maxu_maxexp_o.valid     := true.B
        maxu_maxexp_o.max_sign  := max_reg(15)    
        maxu_maxexp_o.max_exp   := max_reg(14, 10)
        maxu_maxexp_o.max_frac  := max_reg(9, 0)  
        maxu_maxexp_o.batch_num := 0.U
    }.otherwise {
        maxu_maxexp_o.valid     := false.B
        maxu_maxexp_o.max_sign  := 0.U    
        maxu_maxexp_o.max_exp   := 0.U
        maxu_maxexp_o.max_frac  := 0.U
        maxu_maxexp_o.batch_num := 0.U
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
        comparators(i).io.in := Vec(io.in(i * 2), io.in(i * 2 + 1))
        io.out(i)            := comparators(i).io.out
    }
}

// 小比较树
class SmallComparatorTree32bit extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Valid(Vec(32, UInt(16.W))))
        val out = Valid(UInt(16.W))
    })
    val TreeLevel1 = Module(new SmallComparatorTreeLevel(32)).io
    for (i <- 0 until 15) { TreeLevel1(i).io.in := Vec(in.out.bits(i*2), in.out.bits(i*2+1))}

    val TreeLevel2 = Module(new SmallComparatorTreeLevel(16)).io
    for (i <- 0 until 7) { TreeLevel2(i).io.in := Vec(TreeLevel1(i*2).in.out.bits, TreeLevel1(i*2+1).in.out.bits)}
    
    val TreeLevel3 = Module(new SmallComparatorTreeLevel(8)).io
    for (i <- 0 until 3) { TreeLevel3(i).io.in := Vec(TreeLevel2(i*2).in.out.bits, TreeLevel2(i*2+1).in.out.bits)}
    
    val TreeLevel4 = Module(new SmallComparatorTreeLevel(4)).io
    for (i <- 0 until 1) { TreeLevel4(i).io.in := Vec(TreeLevel3(i*2).in.out.bits, TreeLevel3(i*2+1).in.out.bits)}
    
    val TreeLevel5 = Module(new SmallComparatorTreeLevel(2)).io
    TreeLevel5.io.in := Vec(TreeLevel4(0).in.out.bits, TreeLevel4(1).in.out.bits)

    // val comparators = Seq.fill(32)(Module(new Comparator))

    // // ============== 小树归一 
    // for (i <- 0 until 32) {
    //     comparators(i).io.a := smallTrees(0).io.out(i)
    //     comparators(i).io.b := smallTrees(1).io.out(i)
    //     io.out(i) := Mux(i < 16, comparators(i).io.out,   
    //                     Mux(i < 24, smallTrees(2).io.out(i - 16), 
    //                     Mux(i < 28, smallTrees(3).io.out(i - 24), smallTrees(4).io.out(i - 28))))
    // }
}

class RRArbiter (MaxinBandwidth: Int, ComparatorTreeNums: Int, UseComparatorTreeTimes: Int)  extends Module {
    val io = IO(new Bundle{
        val data_in  = Flipped(Valid(Vec(MaxinBandwidth, UInt(bitwidth.W))))
        val rid      = Output(UInt(log2Up(ComparatorTreeNums).W))
        val data_out = Valid(Vec(MaxinBandwidth, UInt(bitwidth.W)))
    })
    val rid  = RegInit(0.U(log2Up(ComparatorTreeNums).W))
    rid    := Mux(~io.data_in.valid, rid, 
                Mux(rid === ComparatorTreeNums.U, 0.U, rid + 1.U))

    io.rid            := rid   
    io.data_out.valid := io.data_in.valid 
    io.data_out.bits  := io.data_in.bits 
}


// class MaxValueFinder extends Module {
//     val io = IO(new Bundle {
//         val in = Input(Vec(1024, UInt(32.W)))
//         val maxValue = Output(UInt(32.W))
//     })
    
//     val largeTrees = Seq.fill(32)(Module(new LargeComparatorTree))
//     for (i <- 0 until 32) {
//         largeTrees(i).io.in := io.in.slice(i * 32, (i + 1) * 32)
//     }
    
//     val finalComparator = Module(new Comparator)
//     for (i <- 0 until 31) {
//         finalComparator.io.a := Mux(i.U === 0.U, largeTrees(0).io.out(0), finalComparator.io.out)
//         finalComparator.io.b := largeTrees(i + 1).io.out(0)
//     }
//     io.maxValue := finalComparator.io.out
// }



object MAX_stage extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new MAX_stage(1024, cycle_bandwidth))
}

// object MAX_stage extends App {
//     println(getVerilogString(new MAX_stage(1024, cycle_bandwidth) ))
//     // emitVerilog(new float_div_lut)
// }
