package lut

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._


class ldu_lut_input extends Bundle { val lut_set_idx = UInt(log2Up(lut_set).W) }
class lut_ldu_output extends Bundle { val value_state = UInt(2.W) } // 01 normal_value, 10 overflow, 11 underflow, 00 load_unfinish 

class expu_lut_input extends Bundle { val raddr  = Vec(datain_bandwidth, UInt(frac_bitwidth.W)) }
class lut_expu_output extends Bundle { val rdata = Vec(datain_bandwidth, UInt(expvalue_bitwidth.W))}

class dma_lut_input extends Bundle { 
    val wdata  = Vec(2, UInt(bus_width.W))
    val last   = Bool()
}
class lut_dma_output extends Bundle { 
    val waddr   = UInt(log2Up(fullSet_size).W)
    val wlen    = UInt(log2Up(dma_burst_len).W)
}

class ExpLUT(depth: Int, width: Int, set: Int) extends Module {
    val io = IO(new Bundle {
        val ldu_lut_i   = Flipped(Decoupled(new ldu_lut_input))
        val lut_ldu_o   = Decoupled(new lut_ldu_output)     // write sram
        
        val expu_lut_i  = Flipped(Decoupled(new expu_lut_input))
        val lut_expu_o  = Decoupled(new lut_expu_output)   // read sram

        val dma_lut_i   = Flipped(Decoupled(new dma_lut_input))  
        val lut_dma_o   = Decoupled(new lut_dma_output) // write sram
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

    // val dma_state        = WireInit(sIdle)
    // val lut_dma_o_hs    = io.lut_dma_o.ready && io.lut_dma_o.valid      // dma接收信号握手
    val dma_lut_i_hs    = io.dma_lut_i.ready && io.dma_lut_i.valid      // dma返回信号握手
    // dma_state := fsm(wr_state === sRun && ~flow, last, dma_lut_i_hs)

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
        for (i <- 0 until set-1) {
            io.lut_expu_o.bits.rdata(i * 2)      := srams(i).sram_rd_o.bits.rdata(0)
            io.lut_expu_o.bits.rdata(i * 2 + 1)  := srams(i).sram_rd_o.bits.rdata(1)
        }
        for (i <- 0 until set-1) {
            srams(i).sram_rd_i.bits.raddr(0)    :=  io.expu_lut_i.bits.raddr(i * 2)    
            srams(i).sram_rd_i.bits.raddr(1)    :=  io.expu_lut_i.bits.raddr(i * 2 + 1)
        }
        rlast      := true.B 
    }.otherwise {
        io.lut_expu_o.valid      := false.B 
        for (i <- 0 until set-1) {
            io.lut_expu_o.bits.rdata(i * 2)       := 0.U
            io.lut_expu_o.bits.rdata(i * 2 + 1)   := 0.U
        }
        for (i <- 0 until set-1) {
            srams(i).sram_rd_i.bits.raddr(0) := 0.U
            srams(i).sram_rd_i.bits.raddr(1) := 0.U
        }
        rlast      := false.B 
    }


    // ===================== Preload(Wirte) LUT =============
    val lut_set_idx = RegInit(0.U(log2Up(lut_set).W))
    lut_set_idx     := Mux(ldu_lut_i_hs, io.ldu_lut_i.bits.lut_set_idx, lut_set_idx)
    val value_state = RegInit(0.U(2.W))
    val sram_waddr  = RegInit(0.U(log2Up(partSet_size).W))
    sram_waddr      := Mux(dma_lut_i_hs, sram_waddr + 1.U, sram_waddr)
    val dram_raddr  = RegInit(0.U(log2Up(fullSet_size).W))    

    wlast := Mux(dma_lut_i_hs, io.dma_lut_i.bits.last, false.B)

    // step 1 判断范围
    when (wr_state === sRun) {
        when (lut_set_idx >= overflow_threshold.U) {
            value_state    := 2.U
            flow           := true.B 
            dram_raddr     := 0.U
        }.elsewhen (lut_set_idx <= underflow_threshold.U) {
            value_state    := 3.U
            flow           := true.B 
            dram_raddr     := 0.U
        }.otherwise {
            value_state    := 1.U
            flow           := false.B 
            dram_raddr     := lut_set_idx * partSet_size.U
        }
    }.otherwise {
        value_state        := 0.U 
        flow               := false.B 
        dram_raddr         := 0.U
    }

    // step 2 数据送往dma 
    when (wr_state === sRun && ~flow) {
        io.lut_dma_o.valid      := true.B 
        io.lut_dma_o.bits.waddr      := dram_raddr
        io.lut_dma_o.bits.wlen       := dma_burst_len.U
    }.otherwise {
        io.lut_dma_o.valid      := false.B 
        io.lut_dma_o.bits.waddr      := 0.U
        io.lut_dma_o.bits.wlen       := 0.U
    }

    // step 3 接收dma数据 
    when (wr_state === sRun && ~flow) {
        for (sram <- srams) {
            sram.sram_wr_i.valid  := true.B 
            sram.sram_wr_i.bits.waddr  := sram_waddr
            sram.sram_wr_i.bits.wdata  := io.dma_lut_i.bits.wdata
        }
    }.otherwise {
        // sram.sram_wr_i.valid  := false.B 
        // sram.sram_wr_i.bits.waddr  := 0.U
        // sram.sram_wr_i.bits.wdata  := 0.U
        sram_waddr                 := 0.U
    }
    
    // step 4 返回给ldu
    when (wr_state === sDone) {
        io.lut_ldu_o.bits.value_state    := value_state
    }.otherwise {
        io.lut_ldu_o.bits.value_state    := 0.U
    }
    

}


class DMA extends Module {
    val io = IO(new Bundle {
        val lut_dma_i   = Flipped(Decoupled(new lut_dma_output))
        val dram_dma_i  = Flipped(Decoupled(new dram_dma_output))
        val dma_dram_o  = Decoupled(new dma_dram_input)   
        val dma_lut_o   = Decoupled(new dma_lut_input)   
    })

    // ======================= FSM ==========================
    val state           = WireInit(sIdle)
    val last            = WireInit(false.B)
    val lut_dma_i_hs    = io.lut_dma_i.ready && io.lut_dma_i.valid
    val dma_lut_o_hs    = io.dma_lut_o.ready && io.dma_lut_o.valid
    state               := fsm(lut_dma_i_hs, last, dma_lut_o_hs) // last 之后的那次out握手才会end
    io.lut_dma_i.ready  := state === sIdle
    // ======================================================
    val count      = RegInit(0.U(log2Up(dma_burst_len).W)) 
    val raddr      = RegInit(0.U(log2Up(fullSet_size).W)) 
    raddr         := raddr + count //* lut_width

    last          := count === (dma_burst_len-1).U


    io.dma_lut_o.bits.last        := last
    when (state === sRun) {
        io.dma_dram_o.valid        := true.B 
        io.dma_dram_o.bits.raddr   := raddr

        io.dma_lut_o.valid        := true.B 
        io.dma_lut_o.bits.wdata(0)  := io.dram_dma_i.bits.rdata(0)
        io.dma_lut_o.bits.wdata(1)  := io.dram_dma_i.bits.rdata(1)

        count                      := count + 1.U
    }.otherwise {
        io.dma_dram_o.valid        := false.B 
        io.dma_dram_o.bits.raddr   := 0.U

        io.dma_lut_o.valid        := false.B 
        io.dma_lut_o.bits.wdata(0) := 0.U
        io.dma_lut_o.bits.wdata(1) := 0.U

        count                      := 0.U
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
        val sram_rd_i   = Flipped(Decoupled(new sram_rd_input))
        val sram_rd_o   = Decoupled(new sram_rd_output)

        // 定义写端口
        val sram_wr_i   = Flipped(Decoupled(new sram_wr_input))

    })

    // 实例化一块同步的内存
    val mem = SyncReadMem(depth, UInt(bus_width.W))

    // 写入逻辑
    when(io.sram_wr_i.valid) {
        mem.write(io.sram_wr_i.bits.waddr, io.sram_wr_i.bits.wdata(0))
        mem.write(io.sram_wr_i.bits.waddr + 1.U, io.sram_wr_i.bits.wdata(1))
    }

    // 读取逻辑，根据选择信号读取对应的16位
    def readSegment(addr: UInt, sel: UInt): UInt = {
        val fullData = mem.read(addr)
        MuxCase(0.U(16.W), Array(
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

    io.sram_rd_o.bits.rdata(0) := readSegment(io.sram_rd_i.bits.raddr(0)(6, 0), io.sram_rd_i.bits.raddr(0)(9, 7))
    io.sram_rd_o.bits.rdata(1) := readSegment(io.sram_rd_i.bits.raddr(1)(6, 0), io.sram_rd_i.bits.raddr(1)(9, 7))
}


class dma_dram_input extends Bundle { val raddr     = UInt(log2Up(fullSet_size).W)}
class dram_dma_output extends Bundle {     
    val rdata  = Vec(2, UInt((bus_width).W))
}

class DualPortDRAM(depth: Int, width: Int) extends Module {
    val io = IO(new Bundle {
        val dma_dram_i   = Flipped(Decoupled(new dma_dram_input))
        val dram_dma_o   = Decoupled(new dram_dma_output)     // write sram
    })

    // 实例化一块同步的内存
    val mem = SyncReadMem(depth, UInt(bus_width.W))

    io.dram_dma_o.bits.rdata(0) := mem(io.dma_dram_i.bits.raddr)
    io.dram_dma_o.bits.rdata(1) := mem(io.dma_dram_i.bits.raddr + 1.U) 
}
