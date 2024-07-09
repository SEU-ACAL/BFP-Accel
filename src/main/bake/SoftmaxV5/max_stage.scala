package max_stage

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._
import define.test._

import pipeline._


class max_input extends Bundle { 
    val raw_data    = Vec(cycle_bandwidth, UInt(bitwidth.W))
    val batch_num   = UInt(9.W)
}


class max_exp extends Bundle { val max  = UInt(bitwidth.W)}


class max_stage(bandwidth_in: Int, bandwidth_out: Int) extends Module {
    val maxu_i        = IO(Flipped(Decoupled(new max_input)))
    val maxu_maxexp_o = IO(Decoupled(new max_exp))

    val FirstStageCompareDone   = WireInit(false.B)
    val SecondStageCompareDone  = WireInit(false.B)

    // ======================= FSM ==========================
    maxu_i.ready         := true.B
    // ======================================================
    val max_buffer        = RegInit(VecInit(Seq.fill(32)(0.U(bitwidth.W))))
    val max_buffer_point  = RegInit(0.U(log2Up(maxBatch).W))
    // ===================== 流水线比较 ============
    val comparator = Module(new ComparatorTree32bit).io
    comparator.in.valid          := maxu_i.valid
    comparator.in.bits.batch_num := maxu_i.bits.batch_num
    comparator.in.bits.data_in   := maxu_i.bits.raw_data

    // 记录前32次比较树的结果
    when (comparator.out.valid && comparator.out.bits.batch_num =/= maxBatch.U) { max_buffer(max_buffer_point) := comparator.out.bits.max_out} // 关键路径在这
    // ============== 小树合一 =================
    FirstStageCompareDone  := comparator.out.valid && (comparator.out.bits.batch_num === maxBatch.U - 1.U)
    when (FirstStageCompareDone) {
        comparator.in.valid          := true.B
        comparator.in.bits.batch_num := 32.U
        comparator.in.bits.data_in   := max_buffer
    }
    // ============== 输出 =================
    SecondStageCompareDone := comparator.out.valid && (comparator.out.bits.batch_num === maxBatch.U)
    when (SecondStageCompareDone) {
        maxu_maxexp_o.valid      := true.B
        maxu_maxexp_o.bits.max   := comparator.out.bits.max_out
    }.otherwise {
        maxu_maxexp_o.valid      := false.B
        maxu_maxexp_o.bits.max   := 0.U
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

// 小比较树层
class SmallComparatorTreeLevel (SmallTreeBandwidth: Int) extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Valid(new Bundle {
                            val data_in   = Vec(SmallTreeBandwidth, UInt(bitwidth.W))
                            val batch_num = UInt(9.W)}))
        val out = Valid(new Bundle {
                            val max_out   =  Vec(SmallTreeBandwidth/2, UInt(bitwidth.W))
                            val batch_num = UInt(9.W)})
    })
    val comparators = Seq.fill(SmallTreeBandwidth/2)(Module(new ComparatorFP16(16)))
    for (i <- 0 until SmallTreeBandwidth/2) {
        comparators(i).io.in   := VecInit(io.in.bits.data_in(i * 2), io.in.bits.data_in(i * 2 + 1))
        io.out.bits.max_out(i) := comparators(i).io.max
    }
    io.out.valid          := io.in.valid
    io.out.bits.batch_num := io.in.bits.batch_num
}


// class ComparatorPipline_i() extends Bundle { 
//     val data_in   = Vec(inNums, UInt(bitwidth.W))
//     val batch_num = UInt(9.W)
// }

// class ComparatorPipline_o extends Bundle { 
//     val max_out   = UInt(bitwidth.W)
//     val batch_num = UInt(9.W)
// }

// 比较树
class ComparatorTree32bit extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Valid(new Bundle {
                            val data_in   = Vec(32, UInt(bitwidth.W))
                            val batch_num = UInt(9.W)}))
        val out = Valid(new Bundle {
                            val max_out   = UInt(bitwidth.W)
                            val batch_num = UInt(9.W)})
    })
    val TreeLevel1                = Module(new SmallComparatorTreeLevel(32)).io
    TreeLevel1.in.valid          := io.in.valid
    TreeLevel1.in.bits.data_in   := io.in.bits.data_in
    TreeLevel1.in.bits.batch_num := io.in.bits.batch_num

    val TreeLevel1_out_valid     = RegInit(false.B)
    val TreeLevel1_out_max_out   = RegInit(VecInit(Seq.fill(16)(0.U(bitwidth.W))))
    val TreeLevel1_out_batch_num = RegInit(0.U(9.W))
    TreeLevel1_out_valid        := TreeLevel1.out.valid
    TreeLevel1_out_max_out      := TreeLevel1.out.bits.max_out
    TreeLevel1_out_batch_num    := TreeLevel1.out.bits.batch_num 


    val TreeLevel2                = Module(new SmallComparatorTreeLevel(16)).io
    TreeLevel2.in.valid          := TreeLevel1_out_valid     
    TreeLevel2.in.bits.data_in   := TreeLevel1_out_max_out   
    TreeLevel2.in.bits.batch_num := TreeLevel1_out_batch_num 

    val TreeLevel2_out_valid     = RegInit(false.B)
    val TreeLevel2_out_max_out   = RegInit(VecInit(Seq.fill(8)(0.U(bitwidth.W))))
    val TreeLevel2_out_batch_num = RegInit(0.U(9.W))
    TreeLevel2_out_valid        := TreeLevel2.out.valid
    TreeLevel2_out_max_out      := TreeLevel2.out.bits.max_out
    TreeLevel2_out_batch_num    := TreeLevel2.out.bits.batch_num 
    
    val TreeLevel3                = Module(new SmallComparatorTreeLevel(8)).io
    TreeLevel3.in.valid          := TreeLevel2_out_valid
    TreeLevel3.in.bits.data_in   := TreeLevel2_out_max_out
    TreeLevel3.in.bits.batch_num := TreeLevel2_out_batch_num 

    val TreeLevel3_out_valid     = RegInit(false.B)
    val TreeLevel3_out_max_out   = RegInit(VecInit(Seq.fill(4)(0.U(bitwidth.W))))
    val TreeLevel3_out_batch_num = RegInit(0.U(9.W))
    TreeLevel3_out_valid        := TreeLevel3.out.valid
    TreeLevel3_out_max_out      := TreeLevel3.out.bits.max_out
    TreeLevel3_out_batch_num    := TreeLevel3.out.bits.batch_num 

    val TreeLevel4                = Module(new SmallComparatorTreeLevel(4)).io
    TreeLevel4.in.valid          := TreeLevel3_out_valid    
    TreeLevel4.in.bits.data_in   := TreeLevel3_out_max_out  
    TreeLevel4.in.bits.batch_num := TreeLevel3_out_batch_num

    val TreeLevel4_out_valid     = RegInit(false.B)
    val TreeLevel4_out_max_out   = RegInit(VecInit(Seq.fill(2)(0.U(bitwidth.W))))
    val TreeLevel4_out_batch_num = RegInit(0.U(9.W))
    TreeLevel4_out_valid        := TreeLevel4.out.valid
    TreeLevel4_out_max_out      := TreeLevel4.out.bits.max_out
    TreeLevel4_out_batch_num    := TreeLevel4.out.bits.batch_num 
    
    val TreeLevel5                = Module(new SmallComparatorTreeLevel(2)).io
    TreeLevel5.in.valid          := TreeLevel4_out_valid    
    TreeLevel5.in.bits.data_in   := TreeLevel4_out_max_out  
    TreeLevel5.in.bits.batch_num := TreeLevel4_out_batch_num

    io.out.valid            := TreeLevel5.out.valid
    io.out.bits.max_out     := TreeLevel5.out.bits.max_out(0)
    io.out.bits.batch_num   := TreeLevel5.out.bits.batch_num
}