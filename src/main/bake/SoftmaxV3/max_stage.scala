package MAX_stage

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._
import define.test._

import pipeline._



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



// class ComparatorFP16 (bitwidth: Int) extends Module {
//     val io = IO(new Bundle {
//         val fpA = Input(UInt(bitwidth.W))
//         val fpB = Input(UInt(bitwidth.W))
//         val min = Output(UInt(bitwidth.W))
//         val max = Output(UInt(bitwidth.W))
//     })

//     val AGreaterThanB = Mux(io.fpA(15) =/= io.fpB(15), io.fpA(15) < io.fpB(15), 
//                         Mux(io.fpA(14, 10) =/= io.fpB(14, 10), io.fpA(14, 10) > io.fpB(14, 10), io.fpA(9, 0) > io.fpB(9, 0))) 
//     io.min := Mux(AGreaterThanB, io.fpB, io.fpA)
//     io.max := Mux(AGreaterThanB, io.fpA, io.fpB)
// }


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

    // val comparators = VecInit(Seq.fill(CompareTreeNums)(Module(new TreeComparator(exp_bitwidth, datain_bandwidth)).io))

    val comparator = Module(new TreeComparator(exp_bitwidth, datain_bandwidth)).io
    comparator.sign_vec := datain_sign
    comparator.data_vec := datain_exp

    CompareDone         := comparator.max_o.valid

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
        maxu_maxexp_o.bits.max_sign  := maxu_i.bits.raw_data(comparator.idx_o)(bitwidth - 1)              
        maxu_maxexp_o.bits.max_exp   := maxu_i.bits.raw_data(comparator.idx_o)(bitwidth - 2, bitwidth - 6)
        maxu_maxexp_o.bits.max_frac  := maxu_i.bits.raw_data(comparator.idx_o)(bitwidth - 7, 0)           
        maxu_maxexp_o.bits.batch_num := count
        maxu_maxexp_o.bits.sign_vec  := sign_mem(count)
        maxu_maxexp_o.bits.exp_vec   := exp_mem(count)   
        maxu_maxexp_o.bits.frac_vec  := frac_mem(count)
        count                         := count + 1.U
        
        maxu_outu_o.valid          := true.B 
        maxu_outu_o.bits.data_out  := comparator.max_o.bits
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

    // io.maxu_shiftu_o.bits.sign_vec :=  sign_vec
    // io.maxu_shiftu_o.bits.exp_vec  :=  exp_vec 
    // io.maxu_shiftu_o.bits.frac_vec :=  frac_vec // 连线




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

    // 以cycle_bandwidth为bandwidth
    // val sign_vec     = WireInit(VecInit(Seq.fill(bandwidth_in)(0.U(1.W))))
    // val exp_vec      = WireInit(VecInit(Seq.fill(bandwidth_in)(0.U((exp_bitwidth).W))))
    // val frac_vec     = WireInit(VecInit(Seq.fill(bandwidth_in)(0.U((frac_bitwidth).W))))

    // sign_vec := sign_mem(count)
    // exp_vec  := exp_mem(count)   
    // frac_vec := frac_mem(count)

    // maxu_maxexp_o.sign_vec := sign_mem(count)
    // maxu_maxexp_o.exp_vec  := exp_mem(count)   
    // maxu_maxexp_o.frac_vec := frac_mem(count)
}



/*
class CompareUnit(bitwidth: Int) extends Module {
    val io = IO(new Bundle {
        val data1     = Input(UInt(bitwidth.W))
        val data2     = Input(UInt(bitwidth.W))
        val sign1     = Input(UInt(1.W))
        val sign2     = Input(UInt(1.W))
        val idx1      = Input(UInt(10.W))  // log2Up(1024) = 10
        val idx2      = Input(UInt(10.W))
        val data_o    = Output(UInt(bitwidth.W))
        val sign_o    = Output(UInt(1.W))  // 添加这个输出
        val idx_o     = Output(UInt(10.W))
    })

    val greater = ((io.sign1 < io.sign2)  || 
        ((io.sign1 === 1.U) & (io.sign2 === 1.U) & (io.data1 < io.data2)) ||
         ((io.sign1 === 0.U) & (io.sign2 === 0.U) & (io.data1 > io.data2))) 
    
    io.data_o   := Mux(greater, io.data2, io.data1)
    io.sign_o   := Mux(greater, io.sign2, io.sign1)  // 添加这一行
    io.idx_o    := Mux(greater, io.idx2, io.idx1)
}

class NonRecursiveTreeComparator(val bitwidth: Int) extends Module {
    val io = IO(new Bundle {
        val data_vec = Input(Vec(1024, UInt(bitwidth.W)))
        val sign_vec = Input(Vec(1024, UInt(1.W)))
        val max_o    = Valid(UInt(bitwidth.W))
        val idx_o    = Output(UInt(10.W))
    })

    // 创建足够的比较器单元
    val stage1 = VecInit(Seq.fill(512)(Module(new CompareUnit(bitwidth)).io))
    val stage2 = VecInit(Seq.fill(256)(Module(new CompareUnit(bitwidth)).io))
    val stage3 = VecInit(Seq.fill(128)(Module(new CompareUnit(bitwidth)).io))
    val stage4 = VecInit(Seq.fill(64)(Module(new CompareUnit(bitwidth)).io))
    val stage5 = VecInit(Seq.fill(32)(Module(new CompareUnit(bitwidth)).io))
    val stage6 = VecInit(Seq.fill(16)(Module(new CompareUnit(bitwidth)).io))
    val stage7 = VecInit(Seq.fill(8)(Module(new CompareUnit(bitwidth)).io))
    val stage8 = VecInit(Seq.fill(4)(Module(new CompareUnit(bitwidth)).io))
    val stage9 = VecInit(Seq.fill(2)(Module(new CompareUnit(bitwidth)).io))
    val stage10 = Module(new CompareUnit(bitwidth)).io

    // 第1阶段：512个比较器，比较1024个输入
    for (i <- 0 until 512) {
        stage1(i).data1 := io.data_vec(i * 2)
        stage1(i).data2 := io.data_vec(i * 2 + 1)
        stage1(i).sign1 := io.sign_vec(i * 2)
        stage1(i).sign2 := io.sign_vec(i * 2 + 1)
        stage1(i).idx1  := (i * 2).U(10.W)
        stage1(i).idx2  := (i * 2 + 1).U(10.W)
    }

    // 第2阶段到第9阶段
    for (j <- 0 until 8) {
        val input = j match {
            case 0 => stage1
            case 1 => stage2
            case 2 => stage3
            case 3 => stage4
            case 4 => stage5
            case 5 => stage6
            case 6 => stage7
            case 7 => stage8
        }
        val output = j match {
            case 0 => stage2
            case 1 => stage3
            case 2 => stage4
            case 3 => stage5
            case 4 => stage6
            case 5 => stage7
            case 6 => stage8
            case 7 => stage9
        }
        for (i <- 0 until output.length) {
            output(i).data1 := input(i * 2).data_o
            output(i).data2 := input(i * 2 + 1).data_o
            output(i).sign1 := input(i * 2).sign_o
            output(i).sign2 := input(i * 2 + 1).sign_o
            output(i).idx1  := input(i * 2).idx_o
            output(i).idx2  := input(i * 2 + 1).idx_o
        }
    }

    // 第10阶段：最后一个比较器
    stage10.data1 := stage9(0).data_o
    stage10.data2 := stage9(1).data_o
    stage10.sign1 := stage9(0).sign_o
    stage10.sign2 := stage9(1).sign_o
    stage10.idx1  := stage9(0).idx_o
    stage10.idx2  := stage9(1).idx_o

    // 设置输出
    io.max_o.valid := true.B  // 总是有效，因为我们总是输出一个结果
    io.max_o.bits  := stage10.data_o
    io.idx_o      := stage10.idx_o
}
*/
/*
class CompareUnit(bitwidth: Int) extends Module {
  val io = IO(new Bundle {
    val data1 = Input(UInt(bitwidth.W))
    val data2 = Input(UInt(bitwidth.W))
    val sign1 = Input(UInt(1.W))
    val sign2 = Input(UInt(1.W))
    val idx1 = Input(UInt(10.W))
    val idx2 = Input(UInt(10.W))
    val data_o = Output(UInt(bitwidth.W))
    val sign_o = Output(UInt(1.W))
    val idx_o = Output(UInt(10.W))
  })

  val greater = ((io.sign1 < io.sign2) ||
    ((io.sign1 === 1.U) & (io.sign2 === 1.U) & (io.data1 < io.data2)) ||
    ((io.sign1 === 0.U) & (io.sign2 === 0.U) & (io.data1 > io.data2)))

  io.data_o := Mux(greater, io.data2, io.data1)
  io.sign_o := Mux(greater, io.sign2, io.sign1)
  io.idx_o := Mux(greater, io.idx2, io.idx1)
}

class NonRecursiveTreeComparator(val bitwidth: Int) extends Module {
  val io = IO(new Bundle {
    val data_vec = Input(Vec(1024, UInt(bitwidth.W)))
    val sign_vec = Input(Vec(1024, UInt(1.W)))
    val max_o = Valid(UInt(bitwidth.W))
    val idx_o = Output(UInt(10.W))
  })

  val compareUnit = Module(new CompareUnit(bitwidth))
  val dataReg = Reg(Vec(1024, UInt(bitwidth.W)))
  val signReg = Reg(Vec(1024, UInt(1.W)))
  val idxReg = Reg(Vec(1024, UInt(10.W)))

  val numStages = log2Ceil(1024)
  val stageCounter = RegInit(0.U(log2Ceil(numStages + 1).W))
  val elementCounter = RegInit(0.U(log2Ceil(1024).W))

  val dataOutReg = Reg(UInt(bitwidth.W))
  val signOutReg = Reg(UInt(1.W))
  val idxOutReg = Reg(UInt(10.W))

  // 设置 compareUnit 的默认值
  compareUnit.io.data1 := 0.U
  compareUnit.io.data2 := 0.U
  compareUnit.io.sign1 := 0.U
  compareUnit.io.sign2 := 0.U
  compareUnit.io.idx1 := 0.U
  compareUnit.io.idx2 := 0.U

  when(stageCounter === 0.U) {
    for (i <- 0 until 1024) {
      dataReg(i) := io.data_vec(i)
      signReg(i) := io.sign_vec(i)
      idxReg(i) := i.U
    }
    stageCounter := stageCounter + 1.U
  } .otherwise {
    compareUnit.io.data1 := dataReg(elementCounter)
    compareUnit.io.data2 := dataReg(elementCounter + 1.U)
    compareUnit.io.sign1 := signReg(elementCounter)
    compareUnit.io.sign2 := signReg(elementCounter + 1.U)
    compareUnit.io.idx1 := idxReg(elementCounter)
    compareUnit.io.idx2 := idxReg(elementCounter + 1.U)

    dataReg(elementCounter / 2.U) := compareUnit.io.data_o
    signReg(elementCounter / 2.U) := compareUnit.io.sign_o
    idxReg(elementCounter / 2.U) := compareUnit.io.idx_o

    elementCounter := elementCounter + 2.U

    when(elementCounter === (1024.U - 2.U)) {
      elementCounter := 0.U
      stageCounter := stageCounter + 1.U
    }
  }

  when(stageCounter === numStages.U) {
    dataOutReg := dataReg(0)
    signOutReg := signReg(0)
    idxOutReg := idxReg(0)
  }

  io.max_o.valid := (stageCounter === numStages.U)
  io.max_o.bits := dataOutReg
  io.idx_o := idxOutReg
}
*/

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


// class TreeComparator(val bitwidth: Int, val numElements: Int) extends Module {
//     val io = IO(new Bundle {
//         val data_vec = Input(Vec(numElements, UInt(bitwidth.W)))
//         val sign_vec = Input(Vec(numElements, UInt(1.W)))
//         val max_o    = Valid(UInt(bitwidth.W))
//         val idx_o    = Output(UInt(log2Up(numElements).W))
//     })

//     if (numElements <= 1) {
//         // 基本情况：0 或 1 个元素
//         io.max_o.valid := numElements.U > 0.U  // 如果有 1 个元素，则为 true，否则为 false
//         io.max_o.bits  := io.data_vec.headOption.getOrElse(0.U)
//         io.idx_o      := 0.U
//     } else {
//         // 有多个元素的情况
//         val comparators = VecInit(Seq.fill(numElements/2)(Module(new CompareUnit(bitwidth)).io))

//         for (i <- 0 until numElements/2) {
//             comparators(i).data1 := io.data_vec(i*2)
//             comparators(i).data2 := io.data_vec(i*2+1)
//             comparators(i).sign1 := io.sign_vec(i*2)
//             comparators(i).sign2 := io.sign_vec(i*2+1)
//             comparators(i).idx1 := (i*2).U
//             comparators(i).idx2 := (i*2+1).U
//         }

//         val nextLevel = Module(new TreeComparator(bitwidth, (numElements + 1) / 2))

//         nextLevel.io.data_vec := VecInit(
//             comparators.map(_.data_o) ++ 
//             (if (numElements%2 == 1) Seq(io.data_vec.last) else Seq())
//         )
//         nextLevel.io.sign_vec := VecInit(
//             comparators.map(_.sign1) ++ 
//             (if (numElements%2 == 1) Seq(io.sign_vec.last) else Seq())
//         )

//         io.max_o <> nextLevel.io.max_o
//         io.idx_o := nextLevel.io.idx_o
//     }
// }

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



