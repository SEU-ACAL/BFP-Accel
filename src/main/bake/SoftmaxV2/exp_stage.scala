package softmax

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._
import define.test._

import pipeline._


import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.annotations.MemoryLoadFileType

class max_exp extends Bundle { 
    val max_exp   = UInt(exp_bitwidth.W)
}

class sort_exp extends Bundle { 
    // val max_batch = UInt(log2Up(cycle_bandwidth).W)
    val max_sign  = UInt(1.W)
    val max_exp   = UInt(exp_bitwidth.W)
    val max_frac  = UInt(frac_bitwidth.W)
    
    val batch_num = UInt(log2Up(maxBatch).W)
    val sign_vec  = Vec(cycle_bandwidth, UInt(1.W))
    val exp_vec   = Vec(cycle_bandwidth, UInt(exp_bitwidth.W))
    val frac_vec  = Vec(cycle_bandwidth, UInt(frac_bitwidth.W))
}

class EXP_stage(bandwidth_in: Int) extends Module {
    val maxu_expu_i    = IO(Flipped(Valid(new max_exp)))
    val sortexp_expu_i = IO(Flipped(Decoupled(new sort_exp)))
    val expu_expdiv_o  = IO(Decoupled(new exp_div))

    // ======================= FSM ==========================
    val preload_hs  = WireInit(false.B)
    val data_in_hs  = WireInit(false.B)
    val data_out_hs = WireInit(false.B)

    // maxu_expu_i.ready  := true.B
    sortexp_expu_i.ready := true.B

    // preload_hs  := maxu_expu_i.valid && maxu_expu_i.ready
    data_in_hs  := sortexp_expu_i.valid && sortexp_expu_i.ready
    data_out_hs := expu_expdiv_o.valid && expu_expdiv_o.ready

    val state  = WireInit(sIdle)
    state := fsm(maxu_expu_i.valid, data_in_hs, data_out_hs)

    CycleCounter(state === sRun, state === sIdle, 2)
    // =====================================================
    // ======================= SRAM ==========================
    val multi_srams = Module(new MultiSRAM(set=lut_set, width = lut_width, depth = lut_depth))
    // ======================= DRAM ==========================
    val multi_drams = Module(new MultiDRAM(set= dram_set, width = dram_width, depth = dram_depth, burstLength= dma_burst_len))
        // ====================== load DRAM ==========================
    loadMemoryFromFileInline(multi_drams.drams(0).mem, "./src/main/scala/Softmax/lut_data/lut_data.hex", MemoryLoadFileType.Hex); 
    loadMemoryFromFileInline(multi_drams.drams(1).mem, "./src/main/scala/Softmax/lut_data/lut_data.hex", MemoryLoadFileType.Hex); 
    // ======================= preload ==========================
    
    // val set_idx = RegInit(0.U(log2Up(partSet_num)))
    // set_idx := io.maxu_expu_i.bits.max_all - underflow_threshold.U

    when (maxu_expu_i.valid) {
        multi_drams.io.mdram_rd_i.valid      := true.B
        multi_drams.io.mdram_rd_i.bits.raddr := (maxu_expu_i.bits.max_exp - underflow_threshold.U) >> 3
    }.otherwise {
        multi_drams.io.mdram_rd_i.valid      := false.B
        multi_drams.io.mdram_rd_i.bits.raddr := 0.U
    }

    when (multi_drams.io.mdram_rd_o.valid) {
        multi_srams.io.msram_wr_i.valid      := true.B
        for (i <- 0 until dram_set) {
            multi_srams.io.msram_wr_i.bits.waddr(i) := multi_drams.io.mdram_rd_o.bits.rcnt
            multi_srams.io.msram_wr_i.bits.wdata(i) := multi_drams.io.mdram_rd_o.bits.rdata(i)
        }
    }.otherwise {
        multi_srams.io.msram_wr_i.valid      := false.B
        for (i <- 0 until dram_set) {
            multi_srams.io.msram_wr_i.bits.waddr(i) := 0.U
            multi_srams.io.msram_wr_i.bits.wdata(i) := 0.U
        }
    }


    // ======================= seu ==========================
    val seuValid        = RegInit(false.B)
    seuValid            := sortexp_expu_i.valid

    val seu_sign_vec    = RegInit(VecInit(Seq.fill(bandwidth_in)(0.U(1.W))))
    // val seu_exp_vec    = RegInit(VecInit(Seq.fill(bandwidth)(0.U((frac_bitwidth).W))))
    val seu_frac_vec    = RegInit(VecInit(Seq.fill(bandwidth_in)(0.U((frac_bitwidth).W))))
    val subuValid       = RegInit(false.B)
    val max_sign  = RegInit(0.U(1.W))
    // val max_exp   = RegInit(0.U(exp_bitwidth.W))
    val max_frac  = RegInit(0.U(frac_bitwidth.W))

    val seu_batch_num    = RegInit(0.U(log2Up(maxBatch).W))
    seu_batch_num := sortexp_expu_i.bits.batch_num

    when (seuValid) {
        subuValid   := true.B
        for (i <- 0 until bandwidth_in) { 
            seu_frac_vec(i) := Mux(sortexp_expu_i.bits.exp_vec(i) =/= 0.U, ((Cat(1.U, sortexp_expu_i.bits.frac_vec(i))) >> (sortexp_expu_i.bits.max_exp - sortexp_expu_i.bits.exp_vec(i))), 
                                (sortexp_expu_i.bits.frac_vec(i) >> (sortexp_expu_i.bits.max_exp - sortexp_expu_i.bits.exp_vec(i))))
            max_sign := sortexp_expu_i.bits.max_sign
            // max_exp  := sortexp_expu_i.bits.max_exp 
            max_frac := sortexp_expu_i.bits.max_frac
        }
    }.otherwise {
        subuValid   := false.B
    }

    // ======================= subu ==========================
    val subu_frac_vec    = RegInit(VecInit(Seq.fill(bandwidth_in)(0.U((frac_bitwidth).W))))
    val expuValid   = RegInit(false.B)

    val subu_batch_num    = RegInit(0.U(log2Up(maxBatch).W))
    subu_batch_num := seu_batch_num

    when (subuValid) {
        expuValid   := true.B
        for (i <- 0 until bandwidth_in) { 
            subu_frac_vec(i)          := Mux((seu_sign_vec(i) === 0.U & max_sign === 0.U), max_frac - seu_frac_vec(i),
                                         Mux((seu_sign_vec(i) === 1.U & max_sign === 1.U), seu_frac_vec(i) - max_frac, 
                                         Mux((seu_sign_vec(i) === 1.U) & (max_sign === 0.U), seu_frac_vec(i) + max_frac, 0.U)))
        }
    }.otherwise {
        expuValid   := false.B
    }

    // ======================= get exp value ==========================
    val expvalue_vec    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((expvalue_bitwidth).W))))
    val adduValid       = RegInit(false.B)
    val HitTable        = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((log2Up(datain_bandwidth)).W))))

    val expu_batch_num    = RegInit(0.U(log2Up(maxBatch).W))
    expu_batch_num := subu_batch_num

    when (expuValid) {
        adduValid   := true.B
        for (i <- 0 until cycle_bandwidth) {
            HitTable(subu_frac_vec(i)(9, 2)) := HitTable(subu_frac_vec(i)(9, 2)) + 1.U
        } 
    }.otherwise {
        adduValid   := false.B
    }

    when (expuValid) {
        for (i <- 0 until lut_set) {
            multi_srams.io.msram_rd_i.valid          := true.B
            multi_srams.io.msram_rd_i.bits.raddr(i)  := subu_batch_num + i.U
        }
    }.otherwise {
        for (i <- 0 until lut_set) {
            multi_srams.io.msram_rd_i.valid          := false.B
            multi_srams.io.msram_rd_i.bits.raddr(i)  := 0.U
        }
    }

    // ======================= add ==========================
    val sum = RegInit(0.U(16.W))

    when (adduValid) {
        val slice = MuxCase(VecInit(Seq.fill(16)(0.U(frac_bitwidth.W))), Seq(
            (expu_batch_num === 0.U)  -> VecInit(HitTable.slice(  0,  16)),
            (expu_batch_num === 1.U)  -> VecInit(HitTable.slice( 16,  32)),
            (expu_batch_num === 2.U)  -> VecInit(HitTable.slice( 32,  48)),
            (expu_batch_num === 3.U)  -> VecInit(HitTable.slice( 48,  64)),
            (expu_batch_num === 4.U)  -> VecInit(HitTable.slice( 64,  80)),
            (expu_batch_num === 5.U)  -> VecInit(HitTable.slice( 80,  96)),
            (expu_batch_num === 6.U)  -> VecInit(HitTable.slice( 96, 112)),
            (expu_batch_num === 7.U)  -> VecInit(HitTable.slice(112, 128)),
            (expu_batch_num === 8.U)  -> VecInit(HitTable.slice(128, 144)),
            (expu_batch_num === 9.U)  -> VecInit(HitTable.slice(144, 160)),
            (expu_batch_num === 10.U) -> VecInit(HitTable.slice(160, 176)),
            (expu_batch_num === 11.U) -> VecInit(HitTable.slice(176, 192)),
            (expu_batch_num === 12.U) -> VecInit(HitTable.slice(192, 208)),
            (expu_batch_num === 13.U) -> VecInit(HitTable.slice(208, 224)),
            (expu_batch_num === 14.U) -> VecInit(HitTable.slice(224, 240)),
            (expu_batch_num === 15.U) -> VecInit(HitTable.slice(240, 256))
        )) // 此处应当乘以exp_value
        
        sum := sum + slice.reduce(_ +& _)
    }

    when (expu_batch_num === (maxBatch - 1).U) {
        expu_expdiv_o.valid     := true.B
        expu_expdiv_o.bits.sum  := sum
    }.otherwise {
        expu_expdiv_o.valid     := false.B
        expu_expdiv_o.bits.sum  := 0.U
    }

    when (expu_expdiv_o.valid) {
        sum := 0.U // 输出后清零
    }
}


class sram_rd_input extends Bundle { val raddr  = UInt(log2Up(partSet_size).W)}
class sram_rd_output extends Bundle { val rdata = UInt(bitwidth.W)}
class sram_wr_input extends Bundle { 
    val wdata = UInt(bitwidth.W);
    val waddr = UInt(bus_width.W)
}
class SRAM(width: Int, depth: Int) extends Module {
    // width = bus_width
    val io = IO(new Bundle {
        val sram_rd_i   = Flipped(Valid(new sram_rd_input))
        val sram_rd_o   = Output(new sram_rd_output)
        val sram_wr_i   = Flipped(Valid(new sram_wr_input))
    })
    val mem = SyncReadMem(depth, UInt(width.W))
    when(io.sram_wr_i.valid) {
        mem.write(io.sram_wr_i.bits.waddr, io.sram_wr_i.bits.wdata)
    }
    when(io.sram_rd_i.valid) {
        io.sram_rd_o.rdata := mem.read(io.sram_rd_i.bits.raddr)
    }.otherwise {
        io.sram_rd_o.rdata := 0.U
    }
}

class msram_rd_input extends Bundle { val raddr  = Vec(lut_set, UInt(log2Up(partSet_size).W))}
class msram_rd_output extends Bundle { val rdata = Vec(lut_set, UInt(bitwidth.W))}
class msram_wr_input extends Bundle { 
    val wdata = Vec(dram_set, UInt(bitwidth.W));
    val waddr = Vec(dram_set, UInt(bus_width.W))
}
class MultiSRAM(set: Int, width: Int, depth: Int) extends Module {
    // width = bus_width
    val io = IO(new Bundle {
        val msram_rd_i   = Flipped(Valid(new msram_rd_input))
        val msram_rd_o   = Output(new msram_rd_output)
        val msram_wr_i   = Flipped(Valid(new msram_wr_input))
    })
    val srams = VecInit(Seq.fill(set)(Module(new SRAM(width, depth)).io))
    for (i <- 0 until set) {
        srams(i).sram_rd_i.valid         := io.msram_rd_i.valid  
        srams(i).sram_rd_i.bits.raddr    := io.msram_rd_i.bits.raddr(i)  
        
        io.msram_rd_o.rdata(i)        := srams(i).sram_rd_o.rdata 
    }
    for (i <- 0 until set/2) {
        srams(i).sram_wr_i.valid      := io.msram_wr_i.valid  
        srams(i).sram_wr_i.bits.waddr := io.msram_wr_i.bits.waddr(0)
        srams(i).sram_wr_i.bits.wdata := io.msram_wr_i.bits.wdata(0)
    }
    for (i <- set/2 until set) {
        srams(i).sram_wr_i.valid      := io.msram_wr_i.valid  
        srams(i).sram_wr_i.bits.waddr := io.msram_wr_i.bits.waddr(1)
        srams(i).sram_wr_i.bits.wdata := io.msram_wr_i.bits.wdata(1)
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
        // io.dram_rd_o.valid      := readValid
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