package softmax

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._
import define.test._

import pipeline._



class sortu_input extends Bundle { 
    val raw_data    = Vec(datain_bandwidth, UInt(bitwidth.W))
}

class maxu_expu extends Bundle { 
    val max_exp   = UInt(exp_bitwidth.W)
}

class sort_out extends Bundle { 
    val data_out   = UInt(bitwidth.W) // 落在无效区间，结果为统一的固定值
}


class SORT_stage(bandwidth_in: Int, bandwidth_out: Int) extends Module {
    val sortu_i         = IO(Flipped(Decoupled(new SoftMax_Input)))
    val maxu_expu_o     = IO(Valid(new maxu_expu))
    val sortu_sortexp_o = IO(Decoupled(new sort_exp))

    val sortu_outu_o    = IO(Valid(new sort_out))


    // ======================= FSM ==========================
    val sortu_i_hs  = WireInit(false.B)
    val sortu_o_hs = WireInit(false.B)
    val ditribute_end    = WireInit(false.B)
    // val bypass_end    = WireInit(false.B)

    sortu_i.ready := true.B 

    sortu_i_hs  := sortu_i.valid && sortu_i.ready
    sortu_o_hs := sortu_sortexp_o.valid && sortu_sortexp_o.ready

    val state  = WireInit(sIdle)
    state := fsm(sortu_i_hs, sortu_o_hs, ditribute_end )

    CycleCounter(state === sRun, state === sIdle, 1)
    // =====================================================

    val sorter = Module(new BitonicSorter)
    // val sorter = Module(new BitonicSorter(bitwidth= bitwidth, bandwidth= datain_bandwidth, groupSize = 4))

    sorter.io.in.valid         := sortu_i.valid
    sortu_i.ready              := true.B
    sorter.io.in.bits.raw_data := sortu_i.bits.raw_data
    

    // val sortEnd = RegInit(false.B)
    // sortEnd := sorter.io.out.valid



    // ================== bypass ===================
    val lut_exp  = RegInit(0.U(exp_bitwidth.W))  // 记录的现在sram里面是哪张表，现在lut中数对应的exp值(12 ~ 27)
    

    when (sorter.io.maxOut.valid) {
        when (sorter.io.maxOut.bits.max >= overflow_threshold.U) {
            sortu_outu_o.valid         := true.B
            sortu_outu_o.bits.data_out := 123.U

            maxu_expu_o.valid          := false.B
            maxu_expu_o.bits.max_exp   := 0.U
        }.elsewhen (sorter.io.maxOut.bits.max <= underflow_threshold.U) {
            sortu_outu_o.valid         := true.B
            sortu_outu_o.bits.data_out := 321.U
            
            maxu_expu_o.valid          := false.B
            maxu_expu_o.bits.max_exp   := 0.U
        }.elsewhen (sorter.io.maxOut.bits.max === lut_exp) {
            sortu_outu_o.valid         := false.B
            sortu_outu_o.bits.data_out := 0.U

            maxu_expu_o.valid          := false.B
            maxu_expu_o.bits.max_exp   := 0.U
        }.otherwise {
            sortu_outu_o.valid         := false.B
            sortu_outu_o.bits.data_out := 0.U

            maxu_expu_o.valid          := true.B
            maxu_expu_o.bits.max_exp   := sorter.io.maxOut.bits.max

            lut_exp                    := sorter.io.maxOut.bits.max
        } // 需要preload
    }.otherwise {
        sortu_outu_o.valid         := false.B
        sortu_outu_o.bits.data_out := 0.U
        
        maxu_expu_o.valid          := false.B
        maxu_expu_o.bits.max_exp   := 0.U
    }

    // ================== 分发 ======================
    val sorted      = RegInit(VecInit(Seq.fill(bandwidth_in)(0.U(bitwidth.W))))
    val maxReg      = RegInit(0.U(bitwidth.W))
    val batch_num   = RegInit(0.U(log2Up(maxBatch).W))
    when (sorter.io.out.valid) { sorted := sorter.io.out.bits.sorted_data}
    when (sorter.io.maxOut.valid) { maxReg := sorter.io.maxOut.bits.max}

    sortu_sortexp_o.valid := sorter.io.out.valid
    when (state === sDone) {
        sortu_sortexp_o.bits.max_sign   := maxReg(15)
        sortu_sortexp_o.bits.max_exp    := maxReg(14, 10)
        sortu_sortexp_o.bits.max_frac   := maxReg(9, 0)
        sortu_sortexp_o.bits.batch_num  := batch_num

        val slice = MuxCase(VecInit(Seq.fill(cycle_bandwidth)(0.U(16.W))), Seq(
            (batch_num === 0.U)  -> VecInit(sorted.slice(0,    64)),
            (batch_num === 1.U)  -> VecInit(sorted.slice(64,  128)),
            (batch_num === 2.U)  -> VecInit(sorted.slice(128, 192)),
            (batch_num === 3.U)  -> VecInit(sorted.slice(192, 256)),
            (batch_num === 4.U)  -> VecInit(sorted.slice(256, 320)),
            (batch_num === 5.U)  -> VecInit(sorted.slice(320, 384)),
            (batch_num === 6.U)  -> VecInit(sorted.slice(384, 448)),
            (batch_num === 7.U)  -> VecInit(sorted.slice(448, 512)),
            (batch_num === 8.U)  -> VecInit(sorted.slice(512, 576)),
            (batch_num === 9.U)  -> VecInit(sorted.slice(576, 640)),
            (batch_num === 10.U) -> VecInit(sorted.slice(640, 704)),
            (batch_num === 11.U) -> VecInit(sorted.slice(704, 768)),
            (batch_num === 12.U) -> VecInit(sorted.slice(768, 832)),
            (batch_num === 13.U) -> VecInit(sorted.slice(832, 896)),
            (batch_num === 14.U) -> VecInit(sorted.slice(896, 960)),
            (batch_num === 15.U) -> VecInit(sorted.slice(960, 1024))
        ))
        
        for (i <- 0 until cycle_bandwidth) {
            sortu_sortexp_o.bits.sign_vec(i)   := slice(i)(15)
            sortu_sortexp_o.bits.exp_vec(i)    := slice(i)(14, 10)
            sortu_sortexp_o.bits.frac_vec(i)   := slice(i)(9, 0)
        }

        batch_num := batch_num + 1.U
    }.otherwise {
        sortu_sortexp_o.bits.max_sign   := 0.U
        sortu_sortexp_o.bits.max_exp    := 0.U
        sortu_sortexp_o.bits.max_frac   := 0.U
        sortu_sortexp_o.bits.batch_num  := 0.U
        
        for (i <- 0 until cycle_bandwidth) {
            sortu_sortexp_o.bits.sign_vec(i)   := 0.U
            sortu_sortexp_o.bits.exp_vec(i)    := 0.U
            sortu_sortexp_o.bits.frac_vec(i)   := 0.U
        }

        batch_num := 0.U
    }

    ditribute_end := batch_num === (maxBatch - 1).U
}


class BitonicSorter extends Module {
    val bitwidth = 16
    val bandwidth = 1024
    val groupSize = 4 // 可以根据需要调整
    val io = IO(new Bundle {
        val in      = Flipped(Valid(new Bundle { val raw_data = Vec(bandwidth, UInt(bitwidth.W))}))
        val out     = Valid(new Bundle { val sorted_data = Vec(bandwidth, UInt(bitwidth.W))})
        val maxOut  = Valid(new Bundle { val max = UInt(bitwidth.W)})
    })

    val data        = Reg(Vec(bandwidth, UInt(bitwidth.W)))
    val sorted      = RegInit(VecInit(Seq.fill(bandwidth)(0.U(bitwidth.W))))
    val validReg    = RegInit(false.B)
    val step        = RegInit(0.U(log2Up(bandwidth).W))
    val maxReg      = RegInit(0.U(bitwidth.W))
    val maxValidReg = RegInit(false.B)

    io.out.valid  := validReg
    io.out.bits.sorted_data   := sorted

    io.maxOut.valid  := maxValidReg
    io.maxOut.bits.max   := maxReg(14, 10) // max_exp

    when(io.in.valid) {
        data := io.in.bits.raw_data
    }

def bitonicMerge(data: Vec[UInt], stage: Int, direction: Bool): Vec[UInt] = {
  val merged = WireInit(VecInit(Seq.fill(data.length)(0.U(bitwidth.W))))
  val half = 1 << stage
  for (i <- 0 until half by groupSize) {
    val cmp = Module(new ComparatorFP16(bitwidth))
    cmp.io.fpA := data(i)
    if (i + half < data.length) {
      cmp.io.fpB := data(i + half)
    } else {
      cmp.io.fpB := 0.U
    }
    merged(i) := Mux(direction, cmp.io.min, cmp.io.max)
    if (i + half < data.length) {
      merged(i + half) := Mux(direction, cmp.io.max, cmp.io.min)
    }
  }
  merged
}
    def bitonicSort(data: Vec[UInt], stage: Int): Vec[UInt] = {
        var sortedData = data
        for (s <- 1 to stage) {
            for (i <- 0 until bandwidth by (1 << s)) {
                val subVec = Wire(Vec(1 << s, UInt(bitwidth.W)))
                for (j <- 0 until (1 << s)) {
                    subVec(j) := sortedData(i + j)
                }
                val mergedSubVec = bitonicMerge(subVec, s, true.B)
                for (j <- 0 until (1 << s)) {
                    sortedData(i + j) := mergedSubVec(j)
                }
            }
        }
        sortedData
    }

    def findMax(data: Vec[UInt]): UInt = {
        data.reduce((a, b) => Mux(a > b, a, b))
    }

    when (step === (log2Up(bandwidth) - 2).U) {
        maxReg := findMax(data)
        maxValidReg := true.B
    } .elsewhen (step === log2Up(bandwidth).U) {
        sorted := bitonicSort(data, log2Up(bandwidth))
        validReg := true.B
        maxValidReg := false.B
    } .otherwise {
        validReg := false.B
        maxValidReg := false.B
        step := step + 1.U
    }
}



// class BitonicSorter(bitwidth: Int, bandwidth: Int, groupSize: Int) extends Module {
//   val io = IO(new Bundle {
//     val in = Flipped(Valid(new Bundle { val raw_data = Vec(bandwidth, UInt(bitwidth.W))}))
//     val out = Valid(new Bundle { val sorted_data = Vec(bandwidth, UInt(bitwidth.W))})
//     val maxOut = Valid(new Bundle { val max = UInt(bitwidth.W)})
//   })

//   val data = Reg(Vec(bandwidth, UInt(bitwidth.W)))
//   val sorted = RegInit(VecInit(Seq.fill(bandwidth)(0.U(bitwidth.W))))
//   val validReg = RegInit(false.B)
//   val step = RegInit(0.U(log2Up(bandwidth).W))
//   val maxReg = RegInit(0.U(bitwidth.W))
//   val maxValidReg = RegInit(false.B)

//   io.out.valid := validReg
//   io.out.bits.sorted_data := sorted
//   io.maxOut.valid := maxValidReg
//   io.maxOut.bits.max := maxReg(14, 10) // max_exp

//   when(io.in.valid) {
//     data := io.in.bits.raw_data
//   }

// def bitonicMerge(data: Vec[UInt], stage: Int, direction: Bool): Vec[UInt] = {
//   val merged = WireInit(VecInit(Seq.fill(data.length)(0.U(bitwidth.W))))
//   val half = 1 << stage
//   for (i <- 0 until half by groupSize) {
//     val cmp = Module(new ComparatorFP16(bitwidth))
//     cmp.io.fpA := data(i)
//     if (i + half < data.length) {
//       cmp.io.fpB := data(i + half)
//     } else {
//       cmp.io.fpB := 0.U
//     }
//     merged(i) := Mux(direction, cmp.io.min, cmp.io.max)
//     if (i + half < data.length) {
//       merged(i + half) := Mux(direction, cmp.io.max, cmp.io.min)
//     }
//   }
//   merged
// }

//   def bitonicSort(data: Vec[UInt]): Vec[UInt] = {
//     var sorted = data
//     for (stage <- log2Up(bandwidth) - 1 to 0 by -1) {
//       val half = 1 << stage
//       sorted = VecInit(sorted.grouped(half * 2).flatMap { group =>
//         val group1 = bitonicMerge(VecInit(group.take(half)), stage, true.B)
//         val group2 = bitonicMerge(VecInit(group.drop(half)), stage, false.B)
//         group1 ++ group2
//       }.toIndexedSeq)
//     }
//     sorted
//   }


//   def findMax(data: Vec[UInt]): UInt = {
//     data.reduce((a, b) => Mux(a > b, a, b))
//   }

//   when (step === (log2Up(bandwidth) - 2).U) {
//     maxReg := findMax(data)
//     maxValidReg := true.B
//   }.elsewhen (step === log2Up(bandwidth).U) {
//     sorted := bitonicSort(data)
//     validReg := true.B
//     maxValidReg := false.B
//   }.otherwise {
//     validReg := false.B
//     maxValidReg := false.B
//     step := step + 1.U
//   }
// }

class ComparatorFP16 (bitwidth: Int) extends Module {
    val io = IO(new Bundle {
        val fpA = Input(UInt(bitwidth.W))
        val fpB = Input(UInt(bitwidth.W))
        val min = Output(UInt(bitwidth.W))
        val max = Output(UInt(bitwidth.W))
    })

    val AGreaterThanB = Mux(io.fpA(15) =/= io.fpB(15), io.fpA(15) < io.fpB(15), 
                        Mux(io.fpA(14, 10) =/= io.fpB(14, 10), io.fpA(14, 10) > io.fpB(14, 10), io.fpA(9, 0) > io.fpB(9, 0))) 
    io.min := Mux(AGreaterThanB, io.fpB, io.fpA)
    io.max := Mux(AGreaterThanB, io.fpA, io.fpB)
}








