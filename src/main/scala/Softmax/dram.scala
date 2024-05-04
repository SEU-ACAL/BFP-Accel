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
        val dram_dma_o   = Decoupled(new dram_dma)     // write sram
    })

    io.dma_dram_i.ready := true.B

    // 实例化一块同步的内存
    val mem = SyncReadMem(depth, UInt(bus_width.W))
    when (io.dma_dram_i.valid) {
        io.dram_dma_o.bits.rdata(0) := mem(io.dma_dram_i.bits.raddr)
        io.dram_dma_o.bits.rdata(1) := mem(io.dma_dram_i.bits.raddr + 1.U) 
        io.dram_dma_o.valid         := true.B
    }.otherwise {
        io.dram_dma_o.bits.rdata(0) := 0.U
        io.dram_dma_o.bits.rdata(1) := 0.U
        io.dram_dma_o.valid         := false.B
    } 
}
