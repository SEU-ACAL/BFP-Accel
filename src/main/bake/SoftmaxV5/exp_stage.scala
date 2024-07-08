package exp_stage

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._
import define.function._
import define.test._

import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.annotations.MemoryLoadFileType

class input_exp extends Bundle { 
    val raw_data  = Vec(cycle_bandwidth, UInt(bitwidth.W))
}

class max_exp extends Bundle { 
    val max  = UInt(bitwidth.W)
}

class exp_div extends Bundle { 
    val sum  = UInt(AdderinBitwidth.W)
}

class exp_stage(bandwidth_in: Int) extends Module {
    val input_expu_i   = IO(Flipped(Decoupled(new input_exp)))
    val maxexp_expu_i  = IO(Flipped(Decoupled(new max_exp)))
    val expu_expdiv_o  = IO(Decoupled(new exp_div))
    val io = IO(new Bundle {
        val reset = Input(Bool()) // 系统复位信号
    })

    // ======================= FSM ==========================
    // val data_in_hs  = WireInit(false.B)
    // val data_out_hs = WireInit(false.B)

    // val SUBUDone    = WireInit(false.B)
    // val GetExpDone  = WireInit(false.B)
    // val ADDUDone    = WireInit(false.B)

    input_expu_i.ready  := true.B
    maxexp_expu_i.ready := true.B

    // data_in_hs  := maxexp_expu_i.valid && maxexp_expu_i.ready
    // data_out_hs := expu_expdiv_o.valid && expu_expdiv_o.ready

    // val state  = WireInit(sIdle)
    // state := fsm(data_in_hs, SUBUDone, ADDUDone)

    // CycleCounter(state === sRun, state === sIdle, 2)
    // // =====================================================
    // ====================== Hit Table ==========================
    val ValueTable = RegInit(VecInit(Seq.fill(ValueTable_size)(0.U(frac_bitwidth.W))))
    val HitTable   = RegInit(VecInit(Seq.fill(HitTable_size)(0.U(HitTable_bitwidth.W))))
    // ======================= DRAM ==========================
    // val multi_drams = Module(new MultiDRAM(set = dram_set, width = dram_width, depth = dram_depth, burstLength = dma_burst_len))
    val mem = SyncReadMem(128, UInt(frac_bitwidth.W))
    // val mem = Mem(128, UInt(frac_bitwidth.W))
    // ====================== load DRAM ==========================
    loadMemoryFromFileInline(mem, "/home/shiroha/Code/TETris/Backend/ml-accelerator/src/main/scala/SoftmaxV5/data/lut_data/new_lut.hex", MemoryLoadFileType.Hex); 
    when (input_expu_i.valid) { for (i <- 0 until 128) { ValueTable(i) := mem(i)}}
    // ======================= seu  ==========================
    val seu = Module(new SEU(cycle_bandwidth)).io
    seu.data_in.valid           := input_expu_i.valid
    seu.data_in.bits.batch_num  := input_expu_i.bits.batch_num
    seu.data_in.bits.data_in    := input_expu_i.bits.data_in
    seu.data_in.bits.max_in     := maxexp_expu_i.bits.max
    // ======================= sub  ==========================
    val subu = Module(new SUBU(cycle_bandwidth)).io
    subu.data_in.valid               := seu.data_out.valid
    subu.data_in.bits.batch_num      := seu.data_out.bits.batch_num   
    subu.data_in.bits.seu_sign_vec   := seu.data_out.bits.seu_sign_vec
    subu.data_in.bits.seu_exp_vec    := seu.data_out.bits.seu_exp_vec 
    subu.data_in.bits.seu_frac_vec   := seu.data_out.bits.seu_frac_vec
    subu.data_in.bits.seu_max_vec    := seu.data_out.bits.seu_max_vec 

    // val idx_buffer     = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((7).W)))) // 大表索引 e_min+matissa[9, 6]
    // val rate_buffer    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((6).W)))) // HitTable添入的值 [5, 0]

    // when (subu.data_out.valid) { 
    //     for (i <- 0 until maxBatch) {
    //         idx_buffer(subu.data_out.bits.batch_num * cycle_bandwidth.U + i.U)  := subu.data_out.bits.subu_idx_vec(i)  
    //         rate_buffer(subu.data_out.bits.batch_num * cycle_bandwidth.U + i.U) := subu.data_out.bits.subu_rate_vec(i)  
    //     }
    // } 

    // SUBUDone := subu.data_out.valid && subu.data_out.bits.batch_num === maxBatch.U
    // ======================= get exp value ==========================
    val expu = Module(new EXPU(cycle_bandwidth)).io
    expu.data_in.valid              := subu.data_out.valid
    expu.data_in.bits.batch_num     := subu.data_out.bits.batch_num   
    expu.data_in.bits.idx_buffer    := subu.data_out.bits.subu_idx_vec
    expu.data_in.bits.rate_buffer   := subu.data_out.bits.subu_rate_vec
    
    when (expu.data_out.valid) {
        for (i <- 0 until HitTable_size) {
            when (expu.data_out.bits.HitTable_vec(i) =/= 0.U) {
                HitTable(i) := HitTable(i) + expu.data_out.bits.HitTable_vec(i)
            }.otherwise {
                HitTable(i) := HitTable(i)
            }
        }
    }.otherwise {
        HitTable := VecInit(Seq.fill(HitTable_size)(0.U((HitTable_bitwidth.W))))
    }

     // ======================= add ==========================
    val AdderTree = Module(new AdderTree(128)).io
    AdderTree.data_in.valid              := expu.data_out.valid && expu.data_out.bits.batch_num === 31.U
    AdderTree.data_in.bits.ValueTable    := Mux(AdderTree.data_in.valid, ValueTable, VecInit(Seq.fill(ValueTable_size)(0.U((frac_bitwidth).W))))
    AdderTree.data_in.bits.HitTable      := Mux(AdderTree.data_in.valid, HitTable, VecInit(Seq.fill(HitTable_size)(0.U((HitTable_bitwidth.W)))))
    when (AdderTree.sum.valid) {
        expu_expdiv_o.valid              := true.B
        expu_expdiv_o.bits.sum           := AdderTree.sum.bits 
    }.otherwise {
        expu_expdiv_o.valid              := false.B
        expu_expdiv_o.bits.sum           := 0.U
    }
}


class SEU(numElements: Int) extends Module {
    val io = IO(new Bundle {
        val data_in   = Flipped(Valid(new Bundle { 
            val data_in     = Vec(cycle_bandwidth, UInt(bitwidth.W))
            val max_in      = UInt(bitwidth.W)
            val batch_num   = UInt(9.W)
        }))
        val data_out  = Valid(new Bundle { 
            val seu_sign_vec    = Vec(cycle_bandwidth, UInt(1.W))
            val seu_exp_vec     = Vec(cycle_bandwidth, UInt((exp_bitwidth).W))
            val seu_frac_vec    = Vec(cycle_bandwidth, UInt((frac_bitwidth).W))
            val seu_max_vec     = Vec(8, UInt(bitwidth.W))
            val batch_num       = UInt(9.W)
        })
    })

    val seu_sign_vec    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(1.W))))
    val seu_exp_vec     = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((exp_bitwidth).W))))
    val seu_frac_vec    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((frac_bitwidth).W))))
    
    val seu_max_vec     = RegInit(VecInit(Seq.fill(8)(0.U((bitwidth).W))))

    val subuValid       = RegInit(false.B)
    val batch_num       = RegInit(0.U(9.W))

    // 对指x_max
    when (io.data_in.valid) {
        for (i <- 0 until 8) { 
            seu_max_vec(i) := Mux(4.U*i.U >= io.data_in.bits.max_in(14, 10), 
                                    Mux(io.data_in.bits.data_in(i)(11, 10) =/= 0.U, (Cat(io.data_in.bits.max_in(15), i.U, (Cat(1.U, io.data_in.bits.data_in(i)(9,  0))) >> (4.U*i.U - (io.data_in.bits.max_in(14, 10))))), // 向高对指
                                                                                        (Cat(io.data_in.bits.max_in(15), i.U, io.data_in.bits.data_in(i)(9,  0) >> (4.U*i.U - (io.data_in.bits.max_in(14, 10)))))),// 向高对指
                                    Mux(io.data_in.bits.data_in(i)(11, 10) =/= 0.U, (Cat(io.data_in.bits.max_in(15), i.U, (Cat(1.U, io.data_in.bits.data_in(i)(9,  0))) << ((io.data_in.bits.max_in(14, 10)) - 4.U*i.U))), // 向低对指
                                                                                        (Cat(io.data_in.bits.max_in(15), i.U, io.data_in.bits.data_in(i)(9,  0) << ((io.data_in.bits.max_in(14, 10)) - 4.U*i.U)))))// // 向低对指
        }
    }

    // 对指x
    when (io.data_in.valid) {
        for (i <- 0 until cycle_bandwidth) { 
            seu_sign_vec(i) := io.data_in.bits.data_in(i)(15) 
            seu_exp_vec(i)  := io.data_in.bits.data_in(i)(14, 12) // 只取exp的前三位
            seu_frac_vec(i) := Mux(io.data_in.bits.data_in(i)(11, 10) =/= 0.U, ((Cat(1.U, io.data_in.bits.data_in(i)(9,  0))) << (io.data_in.bits.data_in(i)(11, 10))), 
                                  (io.data_in.bits.data_in(i)(9,  0) << (io.data_in.bits.data_in(i)(11, 10))))
        }

    }

    subuValid                       := Mux(io.data_in.valid, true.B, false.B)
    batch_num                       := Mux(io.data_in.valid, io.data_in.bits.batch_num, batch_num)
    io.data_out.valid               := Mux(subuValid, true.B, false.B)
    io.data_out.bits.seu_sign_vec   := Mux(io.data_out.valid, seu_sign_vec, VecInit(Seq.fill(cycle_bandwidth)(0.U((1).W)))) 
    io.data_out.bits.seu_exp_vec    := Mux(io.data_out.valid, seu_exp_vec , VecInit(Seq.fill(cycle_bandwidth)(0.U((exp_bitwidth).W)))) 
    io.data_out.bits.seu_frac_vec   := Mux(io.data_out.valid, seu_frac_vec, VecInit(Seq.fill(cycle_bandwidth)(0.U((frac_bitwidth).W)))) 
    io.data_out.bits.seu_max_vec    := Mux(io.data_out.valid, seu_max_vec , VecInit(Seq.fill(8)(0.U((bitwidth).W)))) 
    io.data_out.bits.batch_num      := Mux(io.data_out.valid, batch_num   , 0.U) 
}

class SUBU(numElements: Int) extends Module {
    val io = IO(new Bundle {
        val data_in  = Flipped(Valid(new Bundle { 
            val batch_num       = UInt(9.W)
            val seu_sign_vec    = Vec(cycle_bandwidth, UInt(1.W))
            val seu_exp_vec     = Vec(cycle_bandwidth, UInt((exp_bitwidth).W))
            val seu_frac_vec    = Vec(cycle_bandwidth, UInt((frac_bitwidth).W))
            val seu_max_vec     = Vec(8, UInt(bitwidth.W))
        }))
        val data_out  = Valid(new Bundle { 
            val batch_num       = UInt(9.W)
            val subu_idx_vec    = Vec(cycle_bandwidth, UInt(7.W))
            val subu_rate_vec   = Vec(cycle_bandwidth, UInt((6).W))
        })
    })
    val subu_tmp_vec_w   = WireInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((10).W)))) // 中间值

    val subu_idx_vec     = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((7).W)))) // 大表索引 e_min+matissa[9, 6]
    val subu_rate_vec    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((6).W)))) // HitTable添入的值 [5, 0]
    val expuValid        = RegInit(false.B)
    val batch_num        = RegInit(0.U(9.W))


    when (io.data_in.valid) {
        for (i <- 0 until cycle_bandwidth) { 
            subu_tmp_vec_w(i)   := Mux((io.data_in.bits.seu_sign_vec(i) === 0.U & io.data_in.bits.seu_max_vec(0)(15) === 0.U), io.data_in.bits.seu_max_vec(0)(9, 0) - io.data_in.bits.seu_frac_vec(i),
                                    Mux((io.data_in.bits.seu_sign_vec(i) === 1.U & io.data_in.bits.seu_max_vec(0)(15) === 1.U), io.data_in.bits.seu_frac_vec(i) - io.data_in.bits.seu_max_vec(0)(9, 0), 
                                    Mux((io.data_in.bits.seu_sign_vec(i) === 1.U) & (io.data_in.bits.seu_max_vec(0)(15) === 0.U), io.data_in.bits.seu_frac_vec(i) + io.data_in.bits.seu_max_vec(0)(9, 0), 0.U)))
            subu_idx_vec(i)     :=  Cat(io.data_in.bits.seu_exp_vec(i), subu_tmp_vec_w(i)(9, 6))
            subu_rate_vec(i)    :=  subu_tmp_vec_w(i)(5, 0)
        }
    }

    expuValid                       := Mux(io.data_in.valid, true.B, false.B)
    batch_num                       := Mux(io.data_in.valid, io.data_in.bits.batch_num, batch_num)
    io.data_out.valid               := Mux(expuValid, true.B, false.B)
    io.data_out.bits.batch_num      := Mux(io.data_out.valid, batch_num   , 0.U) 
    io.data_out.bits.subu_idx_vec   := Mux(io.data_out.valid, subu_idx_vec, VecInit(Seq.fill(cycle_bandwidth)(0.U((7).W)))) 
    io.data_out.bits.subu_rate_vec  := Mux(io.data_out.valid, subu_rate_vec, VecInit(Seq.fill(cycle_bandwidth)(0.U((6).W)))) 
}

class EXPU(numElements: Int) extends Module {
    val io = IO(new Bundle {
        val data_in  = Flipped(Valid(new Bundle { 
            val batch_num       = UInt(9.W)
            val idx_buffer      = Vec(cycle_bandwidth, UInt(7.W))
            val rate_buffer     = Vec(cycle_bandwidth, UInt((6).W))
        }))
        val data_out  = Valid(new Bundle { 
            val batch_num       = UInt(9.W)
            val HitTable_vec    = Vec(HitTable_size, UInt(6.W))
        })
    })

    val HitTable_vec    = RegInit(VecInit(Seq.fill(HitTable_size)(0.U((6).W)))) // HitTable添入的值 [5, 0]
    val HitTable_add_w  = WireInit(VecInit(Seq.fill(HitTable_size)(VecInit(Seq.fill(cycle_bandwidth)(0.U(HitTable_bitwidth.W))))))
    val adduValid       = RegInit(false.B)
    
    // when (io.data_in.valid) {        
    //     for (i <- 1 until HitTable_size) {
    //         for (j <- 0 until cycle_bandwidth) {
    //             HitTable_add_w(i)(j) := Mux(io.data_in.bits.idx_buffer(j) === i.U , io.data_in.bits.rate_buffer(j), 
    //                                         Mux(i.U =/= 0.U && io.data_in.bits.idx_buffer(j) === (i-1).U, ("h3F".U - io.data_in.bits.rate_buffer(j)), 0.U)) 
    //         }
    //         HitTable_vec(i) := HitTable_add_w(i).reduce(_ +& _)
    //     } 
    //      HitTable_vec(0) := 0.U
    // }

    when (io.data_in.valid) {        
        for (i <- 0 until HitTable_size-1) {
            for (j <- 0 until cycle_bandwidth) {
                when (io.data_in.bits.idx_buffer(j) === i.U) {
                    HitTable_vec(i)   := HitTable_vec(i)   + io.data_in.bits.rate_buffer(j)
                    HitTable_vec(i+1) := HitTable_vec(i+1) + ("h3f".U - io.data_in.bits.rate_buffer(j))
                }
            }
        } 
    }

    // when (io.data_in.valid) {
    //     val rateBufferMap = io.data_in.bits.rate_buffer.zipWithIndex.map {
    //         case (rate, j) => (io.data_in.bits.idx_buffer(j), rate)
    //     }.toMap

    //     for (i <- 0 until HitTable_size-1) {
    //         rateBufferMap.get(i.U).foreach { rate =>
    //             HitTable_vec(i)   := HitTable_vec(i) + rate
    //             HitTable_vec(i+1) := HitTable_vec(i+1) + ("h3f".U - rate)
    //         }
    //     }
    // }.otherwise{
    //     HitTable_vec := VecInit(Seq.fill(HitTable_size)(0.U((6).W)))
    // }

    // ================= 59639, 3736
    // when (io.data_in.valid) {
    //     // 初始化一个临时的 HitTable_vec
    //     val tempHitTable = RegInit(VecInit(Seq.fill(HitTable_size)(0.U(6.W))))
    //     tempHitTable := HitTable_vec

    //     // 遍历所有输入
    //     for (j <- 0 until cycle_bandwidth) {
    //         val idx = io.data_in.bits.idx_buffer(j)
    //         val rate = io.data_in.bits.rate_buffer(j)

    //         when (idx < (HitTable_size - 1).U) {
    //             tempHitTable(idx) := tempHitTable(idx) + rate
    //             tempHitTable(idx + 1.U) := tempHitTable(idx + 1.U) + ("h3f".U - rate)
    //         }
    //     }
    //     // HitTable_vec(0) := 0.U
    //     // HitTable_vec(1) := 0.U

    //     // 更新 HitTable_vec
    //     HitTable_vec := tempHitTable
    // }.otherwise {
    //     HitTable_vec := VecInit(Seq.fill(HitTable_size)(0.U((6).W)))
    // }


    adduValid                       := io.data_in.valid
    io.data_out.valid               := Mux(adduValid, true.B, false.B)
    io.data_out.bits.batch_num      := Mux(io.data_out.valid, io.data_in.bits.batch_num, 0.U) 
    io.data_out.bits.HitTable_vec   := Mux(io.data_out.valid, HitTable_vec, VecInit(Seq.fill(HitTable_size)(0.U((6).W)))) 
}


class AdderTreeLevel (AdderTreeBandwidth: Int) extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Valid(Vec(AdderTreeBandwidth, UInt(AdderinBitwidth.W))))
        val out = Valid(Vec(AdderTreeBandwidth/4, UInt(AdderinBitwidth.W)))
    })
    for (i <- 0 until AdderTreeBandwidth/4) { io.out.bits(i) := io.in.bits(i * 4) + io.in.bits(i * 4 + 1) + io.in.bits(i * 4 + 2) + io.in.bits(i * 4 + 3)}
    io.out.valid := io.in.valid
}

class AdderTree(numElements: Int) extends Module {
    val io = IO(new Bundle {
        val data_in   = Flipped(Valid(new Bundle { 
            val ValueTable = Vec(ValueTable_size, UInt(frac_bitwidth.W))
            val HitTable   = Vec(ValueTable_size, UInt(HitTable_bitwidth.W))
        }))
        val sum      = Valid(UInt(frac_bitwidth.W))
    })

    val adder_in = RegInit(VecInit(Seq.fill(ValueTable_size)(0.U(AdderinBitwidth.W))))
    when (io.data_in.valid) {
        for (i <- 0 until ValueTable_size) {
            adder_in(i) := io.data_in.bits.ValueTable(i) * io.data_in.bits.HitTable(i)
        }
    }

    val TreeLevel1 = Module(new AdderTreeLevel(128)).io
    TreeLevel1.in.bits  := adder_in
    TreeLevel1.in.valid := io.data_in.valid
    val TreeLevel2 = Module(new AdderTreeLevel(32)).io
    TreeLevel2.in.bits  := TreeLevel1.out.bits
    TreeLevel2.in.valid := TreeLevel1.out.valid

    // val TreeLevel2_out = RegInit(VecInit(Seq.fill(16)(0.U(AdderinBitwidth.W))))
    // TreeLevel2_out := TreeLevel2.out
    
    val TreeLevel3 = Module(new AdderTreeLevel(8)).io
    TreeLevel3.in.bits  := TreeLevel2.out.bits
    TreeLevel3.in.valid := TreeLevel2.out.valid
    val TreeLevel4 = Module(new AdderTreeLevel(4)).io
    TreeLevel4.in.bits := VecInit(TreeLevel3.out.bits.toSeq ++ Seq(0.U, 0.U))
    TreeLevel4.in.valid := TreeLevel3.out.valid
    


    io.sum.bits := TreeLevel4.out.bits(0)
    io.sum.valid := TreeLevel4.out.valid
}