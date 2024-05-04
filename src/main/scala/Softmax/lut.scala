package lut

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._


import pipeline._


class ExpLUT(depth: Int, width: Int, set: Int) extends Module {
    val io = IO(new Bundle {
        val ldu_lut_i   = Flipped(Decoupled(new ld_lut))
        val lut_ldu_o   = Decoupled(new lut_ld)    // write sram
        
        val expu_lut_i  = Flipped(Decoupled(new exp_lut))
        val lut_expu_o  = Decoupled(new lut_exp)    // read sram

        val dma_lut_i   = Flipped(Decoupled(new dma_lut))  
        val lut_dma_o   = Decoupled(new lut_dma)    // write sram
    })

    val wlast  = RegInit(false.B)
    val flow   = RegInit(false.B)
    val rlast  = RegInit(false.B)
    // ======================= FSM ==========================
    val wr_state        = WireInit(sIdle)
    val ldu_lut_i_hs    = io.ldu_lut_i.ready && io.ldu_lut_i.valid      // 输入写信号握手
    val lut_ldu_o_hs    = io.lut_ldu_o.ready && io.lut_ldu_o.valid      // 输出写信号握手
    wr_state  := fsm(ldu_lut_i_hs, wlast || flow, lut_ldu_o_hs)
    io.ldu_lut_i.ready  := wr_state === sIdle

    val dma_state       = WireInit(sIdle)
    val lut_dma_o_hs    = io.lut_dma_o.ready && io.lut_dma_o.valid   // dma接收信号握手
    val dma_lut_i_hs    = io.dma_lut_i.ready && io.dma_lut_i.valid      // dma返回信号握手
    dma_state := fsm(lut_dma_o_hs, dma_lut_i_hs, wlast)
    io.dma_lut_i.ready  := dma_state === sIdle || dma_state === sRun


    val rd_state        = WireInit(sIdle)
    val expu_lut_i_hs   = io.expu_lut_i.ready && io.expu_lut_i.valid    // 输入读信号握手
    val lut_expu_o_hs   = io.lut_expu_o.ready && io.lut_expu_o.valid    // 输出读信号握手
    rd_state := fsm(expu_lut_i_hs, rlast, lut_expu_o_hs)
    io.expu_lut_i.ready  := rd_state === sIdle
    // ======================================================
    // 实例化n个双端口SRAM
    val srams = Seq.fill(set)(Module(new DualPortSRAM(depth, width)).io)

    // ===================== Read LUT =============
     
    when (rd_state === sRun) {
        io.lut_expu_o.valid      := true.B 
        for (i <- 0 until set) {
            io.lut_expu_o.bits.rdata(i * 2)      := srams(i).sram_rd_o.bits.rdata(0)
            io.lut_expu_o.bits.rdata(i * 2 + 1)  := srams(i).sram_rd_o.bits.rdata(1)
        }
        for (i <- 0 until set) {
            srams(i).sram_rd_i.valid            :=  true.B    
            srams(i).sram_rd_i.bits.raddr(0)    :=  io.expu_lut_i.bits.raddr(i * 2)    
            srams(i).sram_rd_i.bits.raddr(1)    :=  io.expu_lut_i.bits.raddr(i * 2 + 1)
        }
        rlast      := true.B 
    }.otherwise {
        io.lut_expu_o.valid      := false.B 
        for (i <- 0 until set) {
            io.lut_expu_o.bits.rdata(i * 2)       := 0.U
            io.lut_expu_o.bits.rdata(i * 2 + 1)   := 0.U
        }
        for (i <- 0 until set) {
            srams(i).sram_rd_i.valid         := false.B    
            srams(i).sram_rd_i.bits.raddr(0) := 0.U
            srams(i).sram_rd_i.bits.raddr(1) := 0.U
        }
        rlast      := false.B 
    }


    // ===================== Preload(Wirte) LUT =============
    val value_state = RegInit(0.U(2.W))
    val sram_waddr  = RegInit(0.U(log2Up(partSet_size).W))
    sram_waddr      := Mux(dma_lut_i_hs, sram_waddr + 1.U, sram_waddr)
    val dram_raddr  = RegInit(0.U(log2Up(fullSet_size).W))    

    wlast := Mux(dma_lut_i_hs, io.dma_lut_i.bits.wlast, false.B)

    // step 1 判断范围
    when (wr_state === sRun) {
        when (io.ldu_lut_i.bits.lut_set_idx >= overflow_threshold.U) {
            value_state    := 2.U
            flow           := true.B 
            dram_raddr     := 0.U
        }.elsewhen (io.ldu_lut_i.bits.lut_set_idx <= underflow_threshold.U) {
            value_state    := 3.U
            flow           := true.B 
            dram_raddr     := 0.U
        }.otherwise {
            value_state    := 1.U
            flow           := false.B 
            dram_raddr     := io.ldu_lut_i.bits.lut_set_idx * partSet_size.U
        }
    }.otherwise {
        value_state        := 0.U 
        flow               := false.B 
        dram_raddr         := 0.U
    }

    // step 2 数据送往dma 
    when (wr_state === sRun && ~flow) {
        io.lut_dma_o.valid           := true.B 
        io.lut_dma_o.bits.waddr      := dram_raddr
        io.lut_dma_o.bits.wlen       := dma_burst_len.U
    }.otherwise {
        io.lut_dma_o.valid           := false.B 
        io.lut_dma_o.bits.waddr      := 0.U
        io.lut_dma_o.bits.wlen       := 0.U
    }

    // step 3 接收dma数据 
    when (dma_state === sRun) {
        for (sram <- srams) {
            sram.sram_wr_i.valid       := true.B 
            sram.sram_wr_i.bits.waddr  := sram_waddr
            sram.sram_wr_i.bits.wdata  := io.dma_lut_i.bits.wdata
        }
    }.otherwise {
        for (sram <- srams) {
            sram.sram_wr_i.valid       := false.B 
            sram.sram_wr_i.bits.waddr  := 0.U
            sram.sram_wr_i.bits.wdata  := VecInit(Seq.fill(2)(0.U(bus_width.W)))
        }
            sram_waddr                 := 0.U
    }
    
    // step 4 返回给ldu
    when (wr_state === sDone) {
        io.lut_ldu_o.valid               := true.B
        io.lut_ldu_o.bits.value_state    := value_state
    }.otherwise {
        io.lut_ldu_o.valid               := false.B
        io.lut_ldu_o.bits.value_state    := 0.U
    }
    

}





class sram_wr_input extends Bundle { 
    val waddr     = UInt(log2Up(partSet_size).W)
    val wdata     = Vec(2, UInt(bus_width.W))
}
class sram_rd_input extends Bundle { val raddr     = Vec(2, UInt(log2Up(fullSet_size).W))}
class sram_rd_output extends Bundle { val rdata     = Vec(2, UInt(bitwidth.W))}


class DualPortSRAM(depth: Int, width: Int = 64) extends Module {
    val io = IO(new Bundle {
        // 定义读端口
        val sram_rd_i   = Flipped(Valid(new sram_rd_input))
        val sram_rd_o   = Valid(new sram_rd_output)

        // 定义写端口
        val sram_wr_i   = Flipped(Valid(new sram_wr_input))

    })
    io.sram_rd_o.valid := true.B

    // 实例化一块同步的内存
    val mem = SyncReadMem(depth, UInt(bus_width.W))

    // 写入逻辑
    when (io.sram_wr_i.valid) {
        mem.write(io.sram_wr_i.bits.waddr, io.sram_wr_i.bits.wdata(0))
        mem.write(io.sram_wr_i.bits.waddr + 1.U, io.sram_wr_i.bits.wdata(1))
    }

    // 读取逻辑，根据选择信号读取对应的16位
    def readSegment(addr: UInt, sel: UInt): UInt = {
        val fullData = mem.read(addr)
        MuxCase(0.U(16.W), Seq( 
          (sel === 0.U) -> fullData(15, 0),
          (sel === 1.U) -> fullData(31, 16),
          (sel === 2.U) -> fullData(47, 32),
          (sel === 3.U) -> fullData(63, 48),
          (sel === 4.U) -> fullData(79, 64),
          (sel === 5.U) -> fullData(97, 80),
          (sel === 6.U) -> fullData(111, 98),
          (sel === 7.U) -> fullData(127, 112)
        ))
    }

    when (io.sram_rd_i.valid) {
        io.sram_rd_o.bits.rdata(0) := readSegment(io.sram_rd_i.bits.raddr(0)(6, 0), io.sram_rd_i.bits.raddr(0)(9, 7))
        io.sram_rd_o.bits.rdata(1) := readSegment(io.sram_rd_i.bits.raddr(1)(6, 0), io.sram_rd_i.bits.raddr(1)(9, 7))
    }.otherwise {
        io.sram_rd_o.bits.rdata(0) := 0.U
        io.sram_rd_o.bits.rdata(1) := 0.U
    }
}


