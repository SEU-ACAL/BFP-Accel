// package experiment

// import chisel3._
// import chisel3.util._
// import chisel3.stage._

// import define.MACRO._
// import define.FSM._
// import define.test._

// import pipeline._


// import chisel3.util.experimental.loadMemoryFromFileInline
// import firrtl.annotations.MemoryLoadFileType

// // class max_exp extends Bundle { 
// //     val max_exp   = UInt(exp_bitwidth.W)
// // }

// class max_exp extends Bundle { 
//     // val max_batch = UInt(log2Up(cycle_bandwidth).W)
//     val max_sign  = UInt(1.W)
//     val max_exp   = UInt(exp_bitwidth.W)
//     val max_frac  = UInt(frac_bitwidth.W)
    
//     val batch_num = UInt(log2Up(maxBatch).W)
//     val sign_vec  = Vec(cycle_bandwidth, UInt(1.W))
//     val exp_vec   = Vec(cycle_bandwidth, UInt(exp_bitwidth.W))
//     val frac_vec  = Vec(cycle_bandwidth, UInt(frac_bitwidth.W))
// }

// class EXP_stage(bandwidth_in: Int) extends Module {
//   val maxexp_expu_i = IO(Flipped(Decoupled(new max_exp)))
//   val expu_expdiv_o = IO(Decoupled(new exp_div))

//   // ======================= FSM ==========================
//   val preload_hs = WireInit(false.B)
//   val data_in_hs = WireInit(false.B)
//   val data_out_hs = WireInit(false.B)

//   maxexp_expu_i.ready := true.B

//   data_in_hs := maxexp_expu_i.valid && maxexp_expu_i.ready
//   data_out_hs := expu_expdiv_o.valid && expu_expdiv_o.ready

//   val state = WireInit(sIdle)
//   state := fsm(data_in_hs, data_out_hs, data_out_hs)

//   CycleCounter(state === sRun, state === sIdle, 2)

//   // ======================= DRAM ==========================
//   val multi_drams = Module(new MultiDRAM(set = dram_set, width = dram_width, depth = dram_depth, burstLength = dma_burst_len))

//   // ====================== load DRAM ==========================
//   loadMemoryFromFileInline(multi_drams.drams(0).mem, "./src/main/scala/SoftmaxV3/lut_data/lut_data.hex", MemoryLoadFileType.Hex)
//   loadMemoryFromFileInline(multi_drams.drams(1).mem, "./src/main/scala/SoftmaxV3/lut_data/lut_data.hex", MemoryLoadFileType.Hex)

//   // ====================== Hit Table ==========================
//   val ValueTable = RegInit(VecInit(Seq.fill(maxBatch)(VecInit(Seq.fill(partSet_size / maxBatch)(0.U(log2Up(expvalue_bitwidth).W))))))
//   val HitTable = RegInit(VecInit(Seq.fill(partSet_size)(0.U(log2Up(datain_bandwidth).W))))

//   // ======================= preload ==========================
//   when(maxexp_expu_i.valid) {
//     multi_drams.io.mdram_rd_i.valid := true.B
//     multi_drams.io.mdram_rd_i.bits.raddr := (maxexp_expu_i.bits.max_exp - underflow_threshold.U) >> 3
//   }.otherwise {
//     multi_drams.io.mdram_rd_i.valid := false.B
//     multi_drams.io.mdram_rd_i.bits.raddr := 0.U
//   }

//   when(multi_drams.io.mdram_rd_o.valid) {
//     for (i <- 0 until dram_width) {
//       ValueTable(multi_drams.io.mdram_rd_o.bits.rcnt)(i) := multi_drams.io.mdram_rd_o.bits.rdata(0)(16 * (i + 1) - 1, 16 * i)
//       ValueTable(multi_drams.io.mdram_rd_o.bits.rcnt)(dram_width - 1 + i) := multi_drams.io.mdram_rd_o.bits.rdata(1)(16 * (i + 1) - 1, 16 * i)
//     }
//   }

//   // ======================= seu ==========================
//   val seuValid = RegInit(false.B)
//   seuValid := maxexp_expu_i.valid

//   val seu_sign_vec = RegInit(VecInit(Seq.fill(bandwidth_in)(0.U(1.W))))
//   val seu_frac_vec = RegInit(VecInit(Seq.fill(bandwidth_in)(0.U((frac_bitwidth).W))))
//   val subuValid = RegInit(false.B)
//   val max_sign = RegInit(0.U(1.W))
//   val max_frac = RegInit(0.U(frac_bitwidth.W))

//   val seu_batch_num = RegInit(0.U(log2Up(maxBatch).W))
//   seu_batch_num := maxexp_expu_i.bits.batch_num

//   when(seuValid) {
//     subuValid := true.B
//     for (i <- 0 until bandwidth_in) {
//       seu_frac_vec(i) := Mux(maxexp_expu_i.bits.exp_vec(i) =/= 0.U,
//         ((Cat(1.U, maxexp_expu_i.bits.frac_vec(i))) >> (maxexp_expu_i.bits.max_exp - maxexp_expu_i.bits.exp_vec(i))),
//         (maxexp_expu_i.bits.frac_vec(i) >> (maxexp_expu_i.bits.max_exp - maxexp_expu_i.bits.exp_vec(i))))
//       max_sign := maxexp_expu_i.bits.max_sign
//       max_frac := maxexp_expu_i.bits.max_frac
//     }
//   }.otherwise {
//     subuValid := false.B
//   }

//   // ======================= subu ==========================
//   val subu_frac_vec = RegInit(VecInit(Seq.fill(bandwidth_in)(0.U((frac_bitwidth).W))))
//   val expuValid = RegInit(false.B)

//   val subu_batch_num = RegInit(0.U(log2Up(maxBatch).W))
//   subu_batch_num := seu_batch_num

//   when(subuValid) {
//     expuValid := true.B
//     for (i <- 0 until bandwidth_in) {
//       subu_frac_vec(i) := Mux((seu_sign_vec(i) === 0.U & max_sign === 0.U), max_frac - seu_frac_vec(i),
//         Mux((seu_sign_vec(i) === 1.U & max_sign === 1.U), seu_frac_vec(i) - max_frac,
//           Mux((seu_sign_vec(i) === 1.U) & (max_sign === 0.U), seu_frac_vec(i) + max_frac, 0.U)))
//     }
//   }.otherwise {
//     expuValid := false.B
//   }

//   // ======================= get exp value ==========================
//   val expvalue_vec = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((expvalue_bitwidth).W))))
//   val adduValid = RegInit(false.B)

//   val expu_batch_num = RegInit(0.U(log2Up(maxBatch).W))
//   expu_batch_num := subu_batch_num

//   when(expuValid) {
//     adduValid := true.B
//     for (i <- 0 until cycle_bandwidth) {
//       HitTable(subu_frac_vec(i)(9, 2)) := HitTable(subu_frac_vec(i)(9, 2)) + 1.U
//     }
//   }.otherwise {
//     adduValid := false.B
//   }

//   adduValid := (expu_batch_num === (maxBatch - 1).U)

//   // ======================= add ==========================
//   val sum = RegInit(0.U(16.W))
//   val partialSum = RegInit(VecInit(Seq.fill(4)(0.U(16.W))))
//   val addState = RegInit(0.U(2.W))

//   when(adduValid) {
//     switch(addState) {
//       is(0.U) {
//         partialSum(0) := HitTable.take(64).reduce(_ +& _)
//         addState := 1.U
//       }
//       is(1.U) {
//         partialSum(1) := HitTable.slice(64, 128).reduce(_ +& _)
//         addState := 2.U
//       }
//       is(2.U) {
//         partialSum(2) := HitTable.slice(128, 192).reduce(_ +& _)
//         addState := 3.U
//       }
//       is(3.U) {
//         partialSum(3) := HitTable.slice(192, 256).reduce(_ +& _)
//         sum := partialSum.reduce(_ +& _)
//         addState := 0.U
//       }
//     }
//   }

//   when(expu_batch_num === (maxBatch - 1).U) {
//     expu_expdiv_o.valid := true.B
//     expu_expdiv_o.bits.sum := sum
//   }.otherwise {
//     expu_expdiv_o.valid := false.B
//     expu_expdiv_o.bits.sum := 0.U
//   }

//   when(expu_expdiv_o.valid) {
//     sum := 0.U // 输出后清零
//   }
// }



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
//         // io.dram_rd_o.valid      := readValid
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


// object EXP_stage extends App {
//   (new chisel3.stage.ChiselStage).emitVerilog(new EXP_stage(64))
// }