package DRAM

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._


import pipeline._

class DualPortDRAM(depth: Int, width: Int) extends Module {
    val io = IO(new Bundle {
        val dma_dram_i   = Flipped(Decoupled(new dma_dram))
        val dram_dma_o   = Decoupled(new dram_dma)     // write dram
    })


    // ======================= FSM ==========================
    // val state           = WireInit(sIdle)
    // val lantency        = RegInit(false.B) // 屎山代码
    // val dma_dram_i_hs   = io.dma_dram_i.ready && io.dma_dram_i.valid
    // val dram_dma_o_hs   = io.dram_dma_o.ready && io.dram_dma_o.valid
    // state               := fsm(dma_dram_i_hs, lantency, dram_dma_o_hs) // 接受到读信号，read_lantency结束，output出去
    // io.dma_dram_i.ready := state === sIdle
    io.dma_dram_i.ready := true.B
    // ======================================================
    // 实例化一块同步的内存
    val mem = SyncReadMem(depth, UInt(bus_width.W))
    
    val lantency0        = RegInit(false.B) // 屎山代码
    val lantency1     = RegInit(false.B) // 屎山代码
    // val lantency2     = RegInit(false.B) // 屎山代码
    // val lantency3     = RegInit(false.B) // 屎山代码
    // val lantency4     = RegInit(false.B) // 屎山代码
    // val lantency5     = RegInit(false.B) // 屎山代码

    val ren           = RegInit(false.B)
    val rdata1        = RegInit(0.U(bus_width.W))
    val rdata2        = RegInit(0.U(bus_width.W))

    when (io.dma_dram_i.valid) {
        lantency0   := true.B
    }.otherwise {
        lantency0    := false.B
    } 

    when (lantency0) {
        io.dram_dma_o.bits.rdata(0) := mem(io.dma_dram_i.bits.raddr)
        io.dram_dma_o.bits.rdata(1) := mem(io.dma_dram_i.bits.raddr + 1.U) 
        lantency1   := true.B
    }.otherwise {
        io.dram_dma_o.bits.rdata(0) := 0.U
        io.dram_dma_o.bits.rdata(1) := 0.U
        lantency1   := false.B
    }

    when (lantency1) {
        io.dram_dma_o.valid         := lantency0
    }.otherwise {
        io.dram_dma_o.valid         := false.B
    }  
}
