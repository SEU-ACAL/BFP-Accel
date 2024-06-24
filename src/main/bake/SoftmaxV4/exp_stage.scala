package tb

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

class tb(bandwidth_in: Int) extends Module {
    val input_expu_i   = IO(Flipped(Decoupled(new input_exp)))
    val maxexp_expu_i  = IO(Flipped(Decoupled(new max_exp)))
    val expu_expdiv_o  = IO(Decoupled(new exp_div))

    // ======================= FSM ==========================
    val data_in_hs  = WireInit(false.B)
    val data_out_hs = WireInit(false.B)

    val GetExpDone            = WireInit(false.B)

    input_expu_i.ready := true.B
    maxexp_expu_i.ready := true.B

    data_in_hs  := maxexp_expu_i.valid && maxexp_expu_i.ready
    data_out_hs := expu_expdiv_o.valid && expu_expdiv_o.ready

    val state  = WireInit(sIdle)
    state := fsm(data_in_hs, GetExpDone, data_out_hs)

    CycleCounter(state === sRun, state === sIdle, 2)
    // =====================================================
    // ======================= DRAM ==========================
    val multi_drams = Module(new MultiDRAM(set= dram_set, width = dram_width, depth = dram_depth, burstLength = dma_burst_len))
    // ====================== load DRAM ==========================
    loadMemoryFromFileInline(multi_drams.drams(0).mem, "/home/shiroha/Code/Backend/ml-accelerator/src/main/scala/SoftmaxV4/data/lut_data/lut_data.hex", MemoryLoadFileType.Hex); 
    loadMemoryFromFileInline(multi_drams.drams(1).mem, "/home/shiroha/Code/Backend/ml-accelerator/src/main/scala/SoftmaxV4/data/lut_data/lut_data.hex", MemoryLoadFileType.Hex); 
    // ====================== Hit Table ==========================
    // val ValueTable = RegInit(VecInit(Seq.fill(maxBatch)(VecInit(Seq.fill(partSet_size/maxBatch)(0.U(expvalue_bitwidth.W))))))
    val ValueTable = RegInit(VecInit(Seq.fill(partSet_size)(0.U(frac_bitwidth.W))))
    val HitTable   = RegInit(VecInit(Seq.fill(partSet_size)(0.U(log2Up(datain_bandwidth).W))))
    // ======================= preload ==========================
    when (maxexp_expu_i.valid) {
        multi_drams.io.mdram_rd_i.valid      := true.B
        multi_drams.io.mdram_rd_i.bits.raddr := (maxexp_expu_i.bits.max(14, 10) - underflow_threshold.U) >> 3
    }.otherwise {
        multi_drams.io.mdram_rd_i.valid      := false.B
        multi_drams.io.mdram_rd_i.bits.raddr := 0.U
    }

    when (multi_drams.io.mdram_rd_o.valid) {
        for (i <- 0 until dram_width) {
            ValueTable(multi_drams.io.mdram_rd_o.bits.rcnt * 16.U + i.U)                      := multi_drams.io.mdram_rd_o.bits.rdata(0)(16 * (i + 1) - 1, 16 * i)(9, 0)
            ValueTable(multi_drams.io.mdram_rd_o.bits.rcnt * 16.U + (dram_width - 1).U + i.U) := multi_drams.io.mdram_rd_o.bits.rdata(1)(16 * (i + 1) - 1, 16 * i)(9, 0)
        }
    }

    // ======================= seu  ==========================
    val seu_frac_vec    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((frac_bitwidth).W))))
    val subuValid       = RegInit(false.B)

    val max_sign        = RegInit(0.U(1.W))
    val max_frac        = RegInit(0.U(frac_bitwidth.W))


    val sign_vec_w        = WireInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((1).W))))
    val exp_vec_w        = WireInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((exp_bitwidth).W))))
    val frac_vec_w        = WireInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((frac_bitwidth).W))))
    val sign_vec_r        = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((1).W))))
    // val exp_vec_r        = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((exp_bitwidth).W))))
    // val frac_vec_r        = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((frac_bitwidth).W))))
    for (i <- 0 until cycle_bandwidth) {
        sign_vec_w(i) := input_expu_i.bits.raw_data(i)(15)
        exp_vec_w(i) := input_expu_i.bits.raw_data(i)(14, 10)
        frac_vec_w(i) := input_expu_i.bits.raw_data(i)(9, 0)
    }
    when (input_expu_i.valid) {
        for (i <- 0 until cycle_bandwidth) {
            sign_vec_r(i) := sign_vec_w(i)
        }
    }

    when (maxexp_expu_i.valid) {
        subuValid   := true.B
        for (i <- 0 until bandwidth_in) { 
            seu_frac_vec(i) := Mux(exp_vec_w(i) =/= 0.U, ((Cat(1.U, frac_vec_w(i))) >> (maxexp_expu_i.bits.max(14, 10) - exp_vec_w(i))), 
                                  (frac_vec_w(i) >> (maxexp_expu_i.bits.max(14, 10) - exp_vec_w(i))))
        }
        max_sign := maxexp_expu_i.bits.max(15)
        max_frac := maxexp_expu_i.bits.max(9, 0)
    }.otherwise {
        subuValid   := false.B
    }

    // ======================= sub  ==========================
    val subu_frac_vec    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((frac_bitwidth).W))))
    val expuValid        = RegInit(false.B)

    when (subuValid) {
        expuValid   := true.B
        for (i <- 0 until cycle_bandwidth) { 
            subu_frac_vec(i)          := Mux((sign_vec_r(i) === 0.U & max_sign === 0.U), max_frac - seu_frac_vec(i),
                                         Mux((sign_vec_r(i) === 1.U & max_sign === 1.U), seu_frac_vec(i) - max_frac, 
                                         Mux((sign_vec_r(i) === 1.U) & (max_sign === 0.U), seu_frac_vec(i) + max_frac, 0.U)))
        }
    }.otherwise {
        expuValid   := false.B
    }

    // ======================= get exp value ==========================
    val frac_mem        = RegInit(VecInit(Seq.fill(1024)(0.U((8).W))))

    val expvalue_vec    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((expvalue_bitwidth).W))))
    val adduValid       = RegInit(false.B)

    val counter     = RegInit(0.U(6.W))  
    counter        := Mux(expuValid, counter + 1.U, 0.U)

    val HitAdder = Seq.fill(HitAdderNumber)(Module(new HitAdder(1024, 8)).io)

    when (expuValid) {
        adduValid   := true.B
        for (i <- 0 until HitAdderNumber) {
            HitAdder(i).data_in.valid := true.B
            HitAdder(i).data_in.bits  := frac_mem

            HitAdder(i).value := counter * 16.U + i.U

            HitTable(counter * 16.U + i.U) := HitAdder(i).out.bits
        } 
    }.otherwise {
        adduValid   := false.B
        
        for (i <- 0 until HitAdderNumber) {
            HitAdder(i).data_in.valid := false.B
            HitAdder(i).data_in.bits  := VecInit(Seq.fill(1024)(0.U((8).W)))

            HitAdder(i).value := 0.U
        }
    }
    
    GetExpDone := counter === 16.U
    adduValid  := GetExpDone
     // ======================= add ==========================
    val AdderTree = Module(new AdderTree(partSet_size)).io
    AdderTree.data_in.valid              := adduValid
    AdderTree.data_in.bits.ValueTable    := Mux(adduValid, ValueTable, VecInit(Seq.fill(partSet_size)(0.U((frac_bitwidth).W))))
    AdderTree.data_in.bits.HitTable      := Mux(adduValid, HitTable, VecInit(Seq.fill(partSet_size)(0.U((log2Up(datain_bandwidth).W)))))
    when (AdderTree.sum.valid) {
        expu_expdiv_o.valid    := true.B
        expu_expdiv_o.bits.sum := AdderTree.sum.bits 
    }.otherwise {
        expu_expdiv_o.valid    := false.B
        expu_expdiv_o.bits.sum := 0.U
    }
}


class dram_rd_input extends Bundle { val raddr  = UInt(log2Up(fullSet_size).W)}
class dram_rd_output extends Bundle { 
    val rdata = UInt(bus_width.W)
    val rcnt = UInt(log2Up(dma_burst_len).W)
}
class DRAM(width: Int, depth: Int, burstLength: Int) extends Module {
    val io = IO(new Bundle {
        val dram_rd_i   = Flipped(Valid(new dram_rd_input))
        val dram_rd_o   = Valid(new dram_rd_output)
    })

    val mem = SyncReadMem(depth, UInt(width.W))
    val readValid = RegInit(false.B)
    val raddrReg = RegInit(0.U(log2Up(depth).W))
    val readCount = RegInit(0.U(log2Up(burstLength).W))

    when(io.dram_rd_i.valid) {
        raddrReg := io.dram_rd_i.bits.raddr
        readCount := 0.U
    }

    io.dram_rd_o.valid      := readValid
    when(io.dram_rd_i.valid || (readCount =/= 0.U && readCount < burstLength.U)) {
        io.dram_rd_o.bits.rdata := mem.read(raddrReg + readCount, true.B)
        io.dram_rd_o.bits.rcnt  := readCount
        readCount               := readCount + 1.U
        readValid               := true.B
    }.otherwise {
        io.dram_rd_o.bits.rdata := 0.U
        io.dram_rd_o.bits.rcnt  := 0.U
        readValid               := false.B
    }
}

class mdram_rd_input extends Bundle { val raddr  = UInt(log2Up(fullSet_size).W)}
class mdram_rd_output extends Bundle { 
    val rdata = Vec(dram_set, UInt(bus_width.W))
    val rcnt  = UInt(log2Up(dma_burst_len).W)
}
class MultiDRAM(set: Int, width: Int, depth: Int, burstLength: Int) extends Module {
    val io = IO(new Bundle {
        val mdram_rd_i   = Flipped(Valid(new mdram_rd_input))
        val mdram_rd_o   = Valid(new mdram_rd_output)
    })

    // val drams = VecInit(Seq.fill(set)(Module(new DRAM(width, depth, burstLength))))
    val drams = Seq(
        Module(new DRAM(width, depth, burstLength)),
        Module(new DRAM(width, depth, burstLength))
    )

    drams(0).io.dram_rd_i.valid      := io.mdram_rd_i.valid  
    drams(0).io.dram_rd_i.bits.raddr := io.mdram_rd_i.bits.raddr  

    drams(1).io.dram_rd_i.valid      := io.mdram_rd_i.valid  
    drams(1).io.dram_rd_i.bits.raddr := io.mdram_rd_i.bits.raddr + dma_burst_len.U  
    

    io.mdram_rd_o.valid             := drams(0).io.dram_rd_o.valid

    io.mdram_rd_o.bits.rcnt         := drams(0).io.dram_rd_o.bits.rcnt 

    io.mdram_rd_o.bits.rdata(0)        := drams(0).io.dram_rd_o.bits.rdata 
    io.mdram_rd_o.bits.rdata(1)        := drams(1).io.dram_rd_o.bits.rdata 

}


class HitAdder (HitAdderBandwidth: Int, HitAdderBitwidth: Int) extends Module {
    val io = IO(new Bundle {
        val data_in  = Input(Valid(Vec(HitAdderBandwidth, UInt(HitAdderBitwidth.W))))
        val value    = Input(UInt(HitAdderBitwidth.W))
        val out      = Output(Valid(UInt(HitAdderBitwidth.W)))
    })

    val adder_in = WireInit(VecInit(Seq.fill(HitAdderBandwidth)(0.U(1.W))))
    for (i <- 0 until HitAdderBandwidth) { adder_in(i) := io.data_in.bits(i) === io.value}

    val TreeLevel1 = Module(new AdderTreeLevel(1024)).io
    val TreeLevel2 = Module(new AdderTreeLevel(256)).io
    val TreeLevel3 = Module(new AdderTreeLevel(64)).io
    val TreeLevel4 = Module(new AdderTreeLevel(16)).io
    val TreeLevel5 = Module(new AdderTreeLevel(4)).io
    TreeLevel1.in := adder_in

    // val TreeLevel1_out = RegInit(VecInit(Seq.fill(256)(0.U(AdderinBitwidth.W))))
    // TreeLevel1_out := TreeLevel1.out

    TreeLevel2.in := TreeLevel1.out
    TreeLevel3.in := TreeLevel2.out
    TreeLevel4.in := TreeLevel3.out
    TreeLevel5.in := TreeLevel4.out

    val counter     = RegInit(0.U(8.W))  
    counter        := Mux(io.data_in.valid, 0.U, counter + 1.U)

    io.out.valid := counter === 2.U
    io.out.bits  := TreeLevel5.out(0)
}





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
            val ValueTable = Vec(partSet_size, UInt(frac_bitwidth.W))
            val HitTable   = Vec(partSet_size, UInt(log2Up(datain_bandwidth).W))
        }))
        val sum      = Valid(UInt(frac_bitwidth.W))
    })

    val adder_in = RegInit(VecInit(Seq.fill(partSet_size)(0.U(AdderinBitwidth.W))))
    when (io.data_in.valid) {
        for (i <- 0 until partSet_size) {
            adder_in(i) := io.data_in.bits.ValueTable(i) * io.data_in.bits.HitTable(i)
        }
    }

    val TreeLevel1 = Module(new AdderTreeLevel(256)).io
    TreeLevel1.in := adder_in
    val TreeLevel2 = Module(new AdderTreeLevel(64)).io
    TreeLevel2.in := TreeLevel1.out

    val TreeLevel2_out = RegInit(VecInit(Seq.fill(16)(0.U(AdderinBitwidth.W))))
    TreeLevel2_out := TreeLevel2.out
    
    val TreeLevel3 = Module(new AdderTreeLevel(16)).io
    TreeLevel3.in := TreeLevel2_out
    val TreeLevel4 = Module(new AdderTreeLevel(4)).io
    TreeLevel4.in := TreeLevel3.out
    
    val counter     = RegInit(0.U(8.W))  
    counter        := Mux(io.data_in.valid, 0.U, counter + 1.U)

    io.sum.bits := TreeLevel4.out(0)
    io.sum.valid := counter === 2.U
}



object tb extends App {
    (new ChiselStage).emitVerilog(new tb(cycle_bandwidth), args)
}

