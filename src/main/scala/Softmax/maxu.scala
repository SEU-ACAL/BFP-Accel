package maxu

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._

import pipeline._



class max_input extends Bundle { 
    val raw_data = Vec(numElements, UInt(bitwidth.W))
}

class MaxComparator(val numElements: Int) extends Module {
/* output
maxu_shiftu_o
    max      = UInt(exp_bitwidth.W)
    idx      = UInt(log2Up(datain_bandwidth).W)
    sign_vec = Vec(datain_bandwidth, UInt(1.W))
    exp_vec  = Vec(datain_bandwidth, UInt(exp_bitwidth.W))
    frac_vec = Vec(datain_bandwidth, UInt(frac_bitwidth.W))
*/
    val io = IO(new Bundle {
        val maxu_i          = Flipped(Decoupled(new max_input))
        val maxu_shiftu_o   = Decoupled(new max_shift)
        val maxu_ldu_o      = Decoupled(new max_ld)
    })


    val    CompareDone   = WireInit(false.B)

    // ======================= FSM ==========================
    val state              = WireInit(sIdle)
    val maxu_i_hs          = io.maxu_i.ready && io.maxu_i.valid
    val maxu_shiftu_o_hs   = io.maxu_shiftu_o.ready && io.maxu_shiftu_o.valid
    state := fsm(maxu_i_hs, CompareDone, maxu_shiftu_o_hs)

    io.maxu_i.ready        := state === sIdle
    io.maxu_shiftu_o.valid := state === sDone
    io.maxu_ldu_o.valid    := state === sDone
    // ======================================================
    val sign_vec     = WireInit(VecInit(Seq.fill(datain_bandwidth)(0.U(1.W))))
    val exp_vec      = WireInit(VecInit(Seq.fill(datain_bandwidth)(0.U((exp_bitwidth).W))))
    val frac_vec     = WireInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))

    for (i <- 0 until datain_bandwidth) {
        sign_vec(i) := io.maxu_i.bits.raw_data(i)(bitwidth - 1)              
        exp_vec(i)  := io.maxu_i.bits.raw_data(i)(bitwidth - 2, bitwidth - 6)  
        frac_vec(i) := io.maxu_i.bits.raw_data(i)(bitwidth - 7, 0)          
    }
    
    val comparator = Module(new TreeComparator(exp_bitwidth, numElements)).io
    comparator.sign_vec := sign_vec
    comparator.data_vec := exp_vec

    CompareDone         := comparator.done

    when (state === sRun) {
        io.maxu_shiftu_o.valid     := false.B 
        io.maxu_shiftu_o.bits.max       := 0.U
        io.maxu_shiftu_o.bits.idx       := 0.U

        io.maxu_ldu_o.valid        := false.B
        io.maxu_ldu_o.bits.max_exp := 0.U
    }.elsewhen (state === sDone) {
        io.maxu_shiftu_o.valid     := true.B 
        io.maxu_shiftu_o.bits.max  := comparator.max_o
        io.maxu_shiftu_o.bits.idx  := comparator.idx_o

        io.maxu_ldu_o.valid        := true.B
        io.maxu_ldu_o.bits.max_exp := comparator.max_o
    }.otherwise {
        io.maxu_shiftu_o.valid     := false.B 
        io.maxu_shiftu_o.bits.max  := 0.U
        io.maxu_shiftu_o.bits.idx  := 0.U

        io.maxu_ldu_o.valid        := false.B 
        io.maxu_ldu_o.bits.max_exp := 0.U   
    }

    io.maxu_shiftu_o.bits.sign_vec :=  sign_vec
    io.maxu_shiftu_o.bits.exp_vec  :=  exp_vec 
    io.maxu_shiftu_o.bits.frac_vec :=  frac_vec //连线


}




class CompareUnit(bitwidth: Int) extends Module {
    val io = IO(new Bundle {
        val data1     = Input(UInt(bitwidth.W))
        val data2     = Input(UInt(bitwidth.W))
        val sign1     = Input(UInt(1.W))
        val sign2     = Input(UInt(1.W))
        val idx1      = Input(UInt(log2Up(bitwidth).W))
        val idx2      = Input(UInt(log2Up(bitwidth).W))
        val data_o    = Output(UInt(bitwidth.W))
        val idx_o     = Output(UInt(log2Up(bitwidth).W))
    })

    val greater = ((io.sign1 < io.sign2)  || 
        ((io.sign1 === 1.U) & (io.sign2 === 1.U) & (io.data1 > io.data2)) ||
         ((io.sign1 === 0.U) & (io.sign2 === 0.U) & (io.data1 < io.data2))) 
    
    io.data_o   := Mux(greater, io.data1, io.data2)
    io.idx_o    := Mux(greater, io.idx1, io.idx2)
}

class TreeComparator(val bitwidth: Int, val numElements: Int) extends Module {
    val io = IO(new Bundle {
        val data_vec = Input(Vec(numElements, UInt(bitwidth.W)))
        val sign_vec = Input(Vec(numElements, UInt(1.W)))
        val max_o = Output(UInt(bitwidth.W))
        val idx_o = Output(UInt(log2Up(numElements).W))
        val done = Output(Bool())
    })

    val comparators = if (numElements > 1) {
        VecInit(Seq.fill(numElements/2)(Module(new CompareUnit(bitwidth)).io))
    } else {
        Seq()
    }

    val nextLevel = if (numElements > 1) Module(new TreeComparator(bitwidth, numElements/2 + numElements%2)) else null

    if (numElements > 1) {
        for (i <- 0 until numElements/2) {
        comparators(i).data1 := io.data_vec(i*2)
        comparators(i).data2 := io.data_vec(i*2+1)
        comparators(i).sign1 := io.sign_vec(i*2)
        comparators(i).sign2 := io.sign_vec(i*2+1)
        comparators(i).idx1 := (i*2).U
        comparators(i).idx2 := (i*2+1).U
        }
    }

    if (numElements > 1) {
        nextLevel.io.data_vec := VecInit.tabulate(numElements/2)(i => comparators(i).data_o) ++ (if (numElements%2 == 1) Seq(io.data_vec.last) else Seq())
        nextLevel.io.sign_vec := VecInit.tabulate(numElements/2)(i => comparators(i).sign1) ++ (if (numElements%2 == 1) Seq(io.sign_vec.last) else Seq())
        io.max_o := nextLevel.io.max_o
        io.idx_o := nextLevel.io.idx_o
        io.done := nextLevel.io.done
    } else {
        io.max_o := io.data_vec.head
        io.idx_o := 0.U
        io.done := true.B
    }
}