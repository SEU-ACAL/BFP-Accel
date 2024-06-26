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
    val sum  = UInt(frac_bitwidth.W)
}

class exp_stage(bandwidth_in: Int) extends Module {
    val input_expu_i   = IO(Flipped(Decoupled(new input_exp)))
    val maxexp_expu_i  = IO(Flipped(Decoupled(new max_exp)))
    val expu_expdiv_o  = IO(Decoupled(new exp_div))
    val io = IO(new Bundle {
        val reset = Input(Bool()) // 系统复位信号
    })

    // ======================= FSM ==========================
    val data_in_hs  = WireInit(false.B)
    val data_out_hs = WireInit(false.B)

    val GetExpDone            = WireInit(false.B)

    input_expu_i.ready  := true.B
    maxexp_expu_i.ready := true.B

    data_in_hs  := maxexp_expu_i.valid && maxexp_expu_i.ready
    data_out_hs := expu_expdiv_o.valid && expu_expdiv_o.ready

    val state  = WireInit(sIdle)
    state := fsm(data_in_hs, GetExpDone, data_out_hs)

    CycleCounter(state === sRun, state === sIdle, 2)
    // =====================================================
    // ======================= DRAM ==========================
    // val multi_drams = Module(new MultiDRAM(set = dram_set, width = dram_width, depth = dram_depth, burstLength = dma_burst_len))
    val mem = SyncReadMem(128, UInt(frac_bitwidth.W))
    // ====================== load DRAM ==========================
    loadMemoryFromFileInline(mem, "/home/shiroha/Code/TETris/Backend/ml-accelerator/src/main/scala/SoftmaxV5/data/lut_data/new_lut.hex", MemoryLoadFileType.Hex); 
    // ====================== Hit Table ==========================
    // val ValueTable = RegInit(VecInit(Seq.fill(maxBatch)(VecInit(Seq.fill(partSet_size/maxBatch)(0.U(expvalue_bitwidth.W))))))
    val ValueTable = RegInit(VecInit(Seq.fill(ValueTable_size)(0.U(frac_bitwidth.W))))
    val HitTable   = RegInit(VecInit(Seq.fill(HitTable_size)(0.U(HitTable_bitwidth.W))))
    // ======================= preload ==========================
    when (io.reset) {
        for (i <- 0 until 128) {
            ValueTable(i) := mem(i)
        }
    }
    // when (maxexp_expu_i.valid) {
    //     multi_drams.io.mdram_rd_i.valid      := true.B
    //     multi_drams.io.mdram_rd_i.bits.raddr := (maxexp_expu_i.bits.max(14, 10) - underflow_threshold.U) >> 3
    // }.otherwise {
    //     multi_drams.io.mdram_rd_i.valid      := false.B
    //     multi_drams.io.mdram_rd_i.bits.raddr := 0.U
    // }

    // when (multi_drams.io.mdram_rd_o.valid) {
    //     for (i <- 0 until dram_width) {
    //         ValueTable(multi_drams.io.mdram_rd_o.bits.rcnt * 16.U + i.U)                      := multi_drams.io.mdram_rd_o.bits.rdata(0)(16 * (i + 1) - 1, 16 * i)(9, 0)
    //         ValueTable(multi_drams.io.mdram_rd_o.bits.rcnt * 16.U + (dram_width - 1).U + i.U) := multi_drams.io.mdram_rd_o.bits.rdata(1)(16 * (i + 1) - 1, 16 * i)(9, 0)
    //     }
    // }

    // ======================= seu  ==========================
    val seu_sign_vec    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(1.W))))
    val seu_exp_vec     = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((exp_bitwidth).W))))
    val seu_frac_vec    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((frac_bitwidth).W))))
    
    val seu_max_vec     = RegInit(VecInit(Seq.fill(8)(0.U((bitwidth).W))))

    val subuValid       = RegInit(false.B)

    // 对指x_max
    when (maxexp_expu_i.valid) {
        for (i <- 0 until 8) { 
            seu_max_vec(i) := Mux(4.U*i.U >= maxexp_expu_i.bits.max(14, 10), 
                                    Mux(input_expu_i.bits.raw_data(i)(11, 10) =/= 0.U, (Cat(maxexp_expu_i.bits.max(15), i.U, (Cat(1.U, input_expu_i.bits.raw_data(i)(9,  0))) >> (4.U*i.U - (maxexp_expu_i.bits.max(14, 10))))), // 向高对指
                                                                                        (Cat(maxexp_expu_i.bits.max(15), i.U, input_expu_i.bits.raw_data(i)(9,  0) >> (4.U*i.U - (maxexp_expu_i.bits.max(14, 10)))))),// 向高对指
                                    Mux(input_expu_i.bits.raw_data(i)(11, 10) =/= 0.U, (Cat(maxexp_expu_i.bits.max(15), i.U, (Cat(1.U, input_expu_i.bits.raw_data(i)(9,  0))) << ((maxexp_expu_i.bits.max(14, 10)) - 4.U*i.U))), // 向低对指
                                                                                        (Cat(maxexp_expu_i.bits.max(15), i.U, input_expu_i.bits.raw_data(i)(9,  0) << ((maxexp_expu_i.bits.max(14, 10)) - 4.U*i.U)))))// // 向低对指
        }
    }

    // 对指x
    when (maxexp_expu_i.valid) {
        for (i <- 0 until bandwidth_in) { 
            seu_sign_vec(i) := input_expu_i.bits.raw_data(i)(15) 
            seu_exp_vec(i)  := input_expu_i.bits.raw_data(i)(14, 12) // 只取exp的前三位
            seu_frac_vec(i) := Mux(input_expu_i.bits.raw_data(i)(11, 10) =/= 0.U, ((Cat(1.U, input_expu_i.bits.raw_data(i)(9,  0))) << (input_expu_i.bits.raw_data(i)(11, 10))), 
                                  (input_expu_i.bits.raw_data(i)(9,  0) << (input_expu_i.bits.raw_data(i)(11, 10))))
        }

    }

    subuValid := Mux(maxexp_expu_i.valid, true.B, false.B)
    // ======================= sub  ==========================
    val subu_tmp_vec_w   = WireInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((10).W)))) // 中间值

    val subu_idx_vec     = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((7).W)))) // 大表索引 e_min+matissa[9, 6]
    val subu_rate_vec    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((6).W)))) // HitTable添入的值 [5, 0]
    val expuValid        = RegInit(false.B)

    when (subuValid) {
        for (i <- 0 until cycle_bandwidth) { 
            subu_tmp_vec_w(i)         := Mux((seu_sign_vec(i) === 0.U & seu_max_vec(0)(15) === 0.U), seu_max_vec(0)(9, 0) - seu_frac_vec(i),
                                         Mux((seu_sign_vec(i) === 1.U & seu_max_vec(0)(15) === 1.U), seu_frac_vec(i) - seu_max_vec(0)(9, 0), 
                                         Mux((seu_sign_vec(i) === 1.U) & (seu_max_vec(0)(15) === 0.U), seu_frac_vec(i) + seu_max_vec(0)(9, 0), 0.U)))
            subu_idx_vec(i)     :=  Cat(seu_exp_vec(i), subu_tmp_vec_w(i)(9, 6))
            subu_rate_vec(i)    :=  subu_tmp_vec_w(i)(5, 0)
        }
    }

    expuValid := Mux(subuValid, true.B, false.B)
    // ======================= get exp value ==========================
    val expvalue_vec    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((expvalue_bitwidth).W))))
    val adduValid       = RegInit(false.B)

    val HitTable_add_w = WireInit(VecInit(Seq.fill(HitTable_size)(0.U(HitTable_bitwidth.W))))

    when (expuValid) {        
        for (i <- 0 until HitTable_size-1) {
            for (j <- 0 until cycle_bandwidth) {
                when (subu_idx_vec(j) === i.U) {
                    HitTable(i)   := HitTable(i)   + subu_rate_vec(j)
                    HitTable(i+1) := HitTable(i+1) + ("h3f".U - subu_rate_vec(j))
                }
                // HitTable_add_w(i) := Mux(subu_idx_vec(j) === i.U , subu_rate_vec(j), 
                //                         Mux(i.U =/= 0.U && subu_idx_vec(j) === (i-1).U, (("3F").U - subu_rate_vec(j)), 0.U)) 
                // HitTable_add_w(subu_idx_vec(j)) :
            }
            // HitTable(i) := HitTable(i) + HitTable_add_w.reduce(_ +& _)
        } 
    }
    val exp_counter = RegInit(0.U(8.W))
    exp_counter := Mux(expuValid === true.B, exp_counter + 1.U, 0.U) 
    GetExpDone := exp_counter === 32.U
    adduValid  := GetExpDone
     // ======================= add ==========================
    val AdderTree = Module(new AdderTree(partSet_size)).io
    AdderTree.data_in.valid              := adduValid
    AdderTree.data_in.bits.ValueTable    := Mux(adduValid, ValueTable, VecInit(Seq.fill(ValueTable_size)(0.U((frac_bitwidth).W))))
    AdderTree.data_in.bits.HitTable      := Mux(adduValid, HitTable, VecInit(Seq.fill(HitTable_size)(0.U((HitTable_bitwidth.W)))))
    when (AdderTree.sum.valid) {
        expu_expdiv_o.valid    := true.B
        expu_expdiv_o.bits.sum := AdderTree.sum.bits 
    }.otherwise {
        expu_expdiv_o.valid    := false.B
        expu_expdiv_o.bits.sum := 0.U
    }
}


// class dram_rd_input extends Bundle { val raddr  = UInt(log2Up(fullSet_size).W)}
// class dram_rd_output extends Bundle { 
//     val rdata = UInt(bus_width.W)
//     val rcnt = UInt(log2Up(dma_burst_len).W)
// }
// class DRAM(width: Int, depth: Int, burstLength: Int) extends Module {
//     val io = IO(new Bundle {
//         val dram_rd_i   = Flipped(Valid(new dram_rd_input))
//         val dram_rd_o   = Valid(new dram_rd_output)
//     })

//     val mem = SyncReadMem(depth, UInt(width.W))
//     val readValid = RegInit(false.B)
//     val raddrReg = RegInit(0.U(log2Up(depth).W))
//     val readCount = RegInit(0.U(log2Up(burstLength).W))

//     when(io.dram_rd_i.valid) {
//         raddrReg := io.dram_rd_i.bits.raddr
//         readCount := 0.U
//     }

//     io.dram_rd_o.valid      := readValid
//     when(io.dram_rd_i.valid || (readCount =/= 0.U && readCount < burstLength.U)) {
//         io.dram_rd_o.bits.rdata := mem.read(raddrReg + readCount, true.B)
//         io.dram_rd_o.bits.rcnt  := readCount
//         readCount               := readCount + 1.U
//         readValid               := true.B
//     }.otherwise {
//         io.dram_rd_o.bits.rdata := 0.U
//         io.dram_rd_o.bits.rcnt  := 0.U
//         readValid               := false.B
//     }
// }

// class mdram_rd_input extends Bundle { val raddr  = UInt(log2Up(fullSet_size).W)}
// class mdram_rd_output extends Bundle { 
//     val rdata = Vec(dram_set, UInt(bus_width.W))
//     val rcnt  = UInt(log2Up(dma_burst_len).W)
// }
// class MultiDRAM(set: Int, width: Int, depth: Int, burstLength: Int) extends Module {
//     val io = IO(new Bundle {
//         val mdram_rd_i   = Flipped(Valid(new mdram_rd_input))
//         val mdram_rd_o   = Valid(new mdram_rd_output)
//     })

//     // val drams = VecInit(Seq.fill(set)(Module(new DRAM(width, depth, burstLength))))
//     val drams = Seq(
//         Module(new DRAM(width, depth, burstLength)),
//         Module(new DRAM(width, depth, burstLength))
//     )

//     drams(0).io.dram_rd_i.valid      := io.mdram_rd_i.valid  
//     drams(0).io.dram_rd_i.bits.raddr := io.mdram_rd_i.bits.raddr  

//     drams(1).io.dram_rd_i.valid      := io.mdram_rd_i.valid  
//     drams(1).io.dram_rd_i.bits.raddr := io.mdram_rd_i.bits.raddr + dma_burst_len.U  
    

//     io.mdram_rd_o.valid             := drams(0).io.dram_rd_o.valid

//     io.mdram_rd_o.bits.rcnt         := drams(0).io.dram_rd_o.bits.rcnt 

//     io.mdram_rd_o.bits.rdata(0)        := drams(0).io.dram_rd_o.bits.rdata 
//     io.mdram_rd_o.bits.rdata(1)        := drams(1).io.dram_rd_o.bits.rdata 

// }


// class HitAdder (HitAdderBandwidth: Int, HitAdderBitwidth: Int) extends Module {
//     val io = IO(new Bundle {
//         val data_in  = Input(Valid(Vec(HitAdderBandwidth, UInt(HitAdderBitwidth.W))))
//         val value    = Input(UInt(HitAdderBitwidth.W))
//         val out      = Output(Valid(UInt(HitAdderBitwidth.W)))
//     })

//     val adder_in = WireInit(VecInit(Seq.fill(HitAdderBandwidth)(0.U(1.W))))
//     for (i <- 0 until HitAdderBandwidth) { adder_in(i) := io.data_in.bits(i) === io.value}

//     val TreeLevel1 = Module(new AdderTreeLevel(1024)).io
//     val TreeLevel2 = Module(new AdderTreeLevel(256)).io
//     val TreeLevel3 = Module(new AdderTreeLevel(64)).io
//     val TreeLevel4 = Module(new AdderTreeLevel(16)).io
//     val TreeLevel5 = Module(new AdderTreeLevel(4)).io
//     TreeLevel1.in := adder_in

//     // val TreeLevel1_out = RegInit(VecInit(Seq.fill(256)(0.U(AdderinBitwidth.W))))
//     // TreeLevel1_out := TreeLevel1.out

//     TreeLevel2.in := TreeLevel1.out
//     TreeLevel3.in := TreeLevel2.out
//     TreeLevel4.in := TreeLevel3.out
//     TreeLevel5.in := TreeLevel4.out

//     val counter     = RegInit(0.U(8.W))  
//     counter        := Mux(io.data_in.valid, 0.U, counter + 1.U)

//     io.out.valid := counter === 2.U
//     io.out.bits  := TreeLevel5.out(0)
// }





class AdderTreeLevel (AdderTreeBandwidth: Int) extends Module {
    val io = IO(new Bundle {
        val in  = Input(Vec(AdderTreeBandwidth, UInt(AdderinBitwidth.W)))
        val out = Output(Vec(AdderTreeBandwidth/4, UInt(AdderinBitwidth.W)))
    })
    for (i <- 0 until AdderTreeBandwidth/4) { io.out(i) := io.in(i * 4) + io.in(i * 4 + 1) + io.in(i * 4 + 2) + io.in(i * 4 + 3)}
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
    TreeLevel1.in := adder_in
    val TreeLevel2 = Module(new AdderTreeLevel(32)).io
    TreeLevel2.in := TreeLevel1.out

    // val TreeLevel2_out = RegInit(VecInit(Seq.fill(16)(0.U(AdderinBitwidth.W))))
    // TreeLevel2_out := TreeLevel2.out
    
    val TreeLevel3 = Module(new AdderTreeLevel(8)).io
    TreeLevel3.in := TreeLevel2.out
    val TreeLevel4 = Module(new AdderTreeLevel(4)).io
    TreeLevel4.in := VecInit(TreeLevel3.out.toSeq ++ Seq(0.U, 0.U))
    
    val counter     = RegInit(0.U(8.W))  
    counter        := Mux(io.data_in.valid, 0.U, counter + 1.U)

    io.sum.bits := TreeLevel4.out(0)
    io.sum.valid := counter === 2.U
}
