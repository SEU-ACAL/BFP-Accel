package experiment

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._
import define.test._

// import pipeline._
class max_exp extends Bundle { 
    // val max_batch = UInt(log2Up(cycle_bandwidth).W)
    val max_sign  = UInt(1.W)
    val max_exp   = UInt(exp_bitwidth.W)
    val max_frac  = UInt(frac_bitwidth.W)
    
    val batch_num = UInt(log2Up(maxBatch).W)
    val sign_vec  = Vec(cycle_bandwidth, UInt(1.W))
    val exp_vec   = Vec(cycle_bandwidth, UInt(exp_bitwidth.W))
    val frac_vec  = Vec(cycle_bandwidth, UInt(frac_bitwidth.W))
}


class max_input extends Bundle { 
    val raw_data    = Vec(datain_bandwidth, UInt(bitwidth.W))
}

class max_out extends Bundle { 
    val data_out   = UInt(bitwidth.W) // 落在无效区间，结果为统一的固定值
}

class MAX_stage(bandwidth_in: Int, bandwidth_out: Int) extends Module {
    val maxu_i        = IO(Flipped(Decoupled(new max_input)))
    val maxu_maxexp_o = IO(Decoupled(new max_exp))
    val maxu_outu_o   = IO(Valid(new max_out))

    val CompareDone   = WireInit(false.B)
    val MaxmemEmpty   = WireInit(false.B)

    // ======================= FSM ==========================
    val state              = WireInit(sIdle)
    val maxu_i_hs          = maxu_i.ready && maxu_i.valid
    val maxu_maxexp_o_hs   = maxu_maxexp_o.ready && maxu_maxexp_o.valid
    // val maxu_outu_o_hs     = maxu_outu_o.ready && maxu_outu_o.valid
    // state := fsm(maxu_i_hs, CompareDone, maxu_shiftu_o_hs)
    state := fsm(maxu_i_hs, maxu_maxexp_o_hs || maxu_outu_o.valid, MaxmemEmpty || maxu_outu_o.valid)

    maxu_i.ready         := state === sIdle
    // maxu_maxexp_o.valid    := state === sDone
    // maxu_outu_o.valid    := state === sDone
    CycleCounter(state === sRun, state === sIdle, 1)
    // ========================= 输入+比较 =============================
    val sign_mem = Mem(maxmem_depth, Vec(maxmem_width, UInt(1.W)))
    val exp_mem  = Mem(maxmem_depth, Vec(maxmem_width, UInt(exp_bitwidth.W)))
    val frac_mem = Mem(maxmem_depth, Vec(maxmem_width, UInt(frac_bitwidth.W)))
    // 以datain_bandwidth为bandwidth
    val datain_sign     = WireInit(VecInit(Seq.fill(datain_bandwidth)(0.U(1.W))))
    val datain_exp      = WireInit(VecInit(Seq.fill(datain_bandwidth)(0.U((exp_bitwidth).W))))

    for (i <- 0 until datain_bandwidth) {
        datain_sign(i) := maxu_i.bits.raw_data(i)(bitwidth - 1)              
        datain_exp(i)  := maxu_i.bits.raw_data(i)(bitwidth - 2, bitwidth - 6)  
    }
    
    val count        = RegInit(0.U(log2Up(maxmem_depth).W))

    val comparators = VecInit(Seq.fill(CompareTreeNums)(Module(new TreeComparator(exp_bitwidth, maxin_bandwidth)).io))
    
    for (i <- 0 until CompareTreeNums) {
        comparators(i).sign_vec := WireInit(VecInit(Seq.fill(maxin_bandwidth)(0.U(1.W))))
        comparators(i).data_vec := WireInit(VecInit(Seq.fill(maxin_bandwidth)(0.U((exp_bitwidth).W))))
    }

    // comparators(0).sign_vec := datain_sign
    // comparators(0).data_vec := datain_exp

    CompareDone         := comparators(0).max_o.valid

    when (state === sRun) {
        maxu_maxexp_o.valid          := false.B 
        maxu_maxexp_o.bits.max_sign  := 0.U
        maxu_maxexp_o.bits.max_exp   := 0.U
        maxu_maxexp_o.bits.max_frac  := 0.U
        maxu_maxexp_o.bits.batch_num := 0.U
        maxu_maxexp_o.bits.sign_vec  := VecInit(Seq.fill(cycle_bandwidth)(0.U(1.W)))           
        maxu_maxexp_o.bits.exp_vec   := VecInit(Seq.fill(cycle_bandwidth)(0.U(exp_bitwidth.W)))
        maxu_maxexp_o.bits.frac_vec  := VecInit(Seq.fill(cycle_bandwidth)(0.U(frac_bitwidth.W)))
        count                         := 0.U

        maxu_outu_o.valid          := false.B 
        maxu_outu_o.bits.data_out  := 0.U
    }.elsewhen (state === sDone) {
        maxu_maxexp_o.valid          := true.B 
        maxu_maxexp_o.bits.max_sign  := maxu_i.bits.raw_data(0)(bitwidth - 1)              
        maxu_maxexp_o.bits.max_exp   := maxu_i.bits.raw_data(0)(bitwidth - 2, bitwidth - 6)
        maxu_maxexp_o.bits.max_frac  := maxu_i.bits.raw_data(0)(bitwidth - 7, 0)           
        maxu_maxexp_o.bits.batch_num := count
        maxu_maxexp_o.bits.sign_vec  := sign_mem(count)
        maxu_maxexp_o.bits.exp_vec   := exp_mem(count)   
        maxu_maxexp_o.bits.frac_vec  := frac_mem(count)
        count                        := count + 1.U
        
        maxu_outu_o.valid          := true.B 
        maxu_outu_o.bits.data_out  := comparators(0).max_o.bits
    }.otherwise {
        maxu_maxexp_o.valid          := false.B 
        maxu_maxexp_o.bits.max_sign  := 0.U
        maxu_maxexp_o.bits.max_exp   := 0.U
        maxu_maxexp_o.bits.max_frac  := 0.U
        maxu_maxexp_o.bits.batch_num := 0.U
        maxu_maxexp_o.bits.sign_vec  := VecInit(Seq.fill(cycle_bandwidth)(0.U(1.W))) 
        maxu_maxexp_o.bits.exp_vec   := VecInit(Seq.fill(cycle_bandwidth)(0.U(exp_bitwidth.W)))
        maxu_maxexp_o.bits.frac_vec  := VecInit(Seq.fill(cycle_bandwidth)(0.U(frac_bitwidth.W)))
        count                         := 0.U

        maxu_outu_o.valid          := false.B 
        maxu_outu_o.bits.data_out  := 0.U
    }


    // ====================== max mem 输出 ================== 


    when (maxu_i_hs) {
        for (i <- 0 until maxmem_depth) {
            for (j <- 0 until maxmem_width) {
                sign_mem(i)(j) := maxu_i.bits.raw_data(i*maxmem_width + j)(bitwidth - 1)              
                exp_mem(i)(j)  := maxu_i.bits.raw_data(i*maxmem_width + j)(bitwidth - 2, bitwidth - 6)  
                frac_mem(i)(j) := maxu_i.bits.raw_data(i*maxmem_width + j)(bitwidth - 7, 0)                           
            }
        }
    }

    MaxmemEmpty     := count === maxmem_depth.U

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
        ((io.sign1 === 1.U) & (io.sign2 === 1.U) & (io.data1 < io.data2)) ||
         ((io.sign1 === 0.U) & (io.sign2 === 0.U) & (io.data1 > io.data2))) 
    
    io.data_o   := Mux(greater, io.data1, io.data2)
    io.idx_o    := Mux(greater, io.idx1, io.idx2)
}



class TreeComparator(val bitwidth: Int, val numElements: Int) extends Module {
    val io = IO(new Bundle {
        val data_vec = Input(Vec(numElements, UInt(bitwidth.W)))
        val sign_vec = Input(Vec(numElements, UInt(1.W)))
        val max_o    = Valid(UInt(bitwidth.W))
        val idx_o    = Output(UInt(log2Up(numElements).W))
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
        io.max_o.valid        := nextLevel.io.max_o.valid
        io.max_o.bits         := nextLevel.io.max_o.bits
        io.idx_o              := nextLevel.io.idx_o
    } else {
        io.max_o.valid        := true.B
        io.max_o.bits         := io.data_vec.head
        io.idx_o              := 0.U
    }
}



object MAX_stage extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new MAX_stage(1024, cycle_bandwidth))
}

// object MAX_stage extends App {
//     println(getVerilogString(new MAX_stage(1024, cycle_bandwidth) ))
//     // emitVerilog(new float_div_lut)
// }
