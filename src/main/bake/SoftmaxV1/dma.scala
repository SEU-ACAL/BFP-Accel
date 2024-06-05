package DMA

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._


import pipeline._

class DMA extends Module {
    val io = IO(new Bundle {
        val lut_dma_i   = Flipped(Decoupled(new lut_dma))
        val dram_dma_i  = Flipped(Decoupled(new dram_dma))
        val dma_dram_o  = Decoupled(new dma_dram)   
        val dma_lut_o   = Decoupled(new dma_lut)   
    })

    // ======================= FSM ==========================
    val state           = WireInit(sIdle)
    val last            = WireInit(false.B)
    val lut_dma_i_hs    = io.lut_dma_i.ready && io.lut_dma_i.valid
    val dma_lut_o_hs    = io.dma_lut_o.ready && io.dma_lut_o.valid
    state               := fsm(lut_dma_i_hs, last, dma_lut_o_hs) // last 之后的那次out握手才会end
    io.lut_dma_i.ready  := state === sIdle
    io.dram_dma_i.ready := state === sRun
    // ======================================================
    val count      = RegInit(0.U((log2Up(dma_burst_len)+1).W)) 
    val raddr      = WireInit(0.U(log2Up(fullSet_size).W)) 
    raddr         := (io.lut_dma_i.bits.waddr >> 3) + count //* lut_width
    last          := count === (dma_burst_len-1).U // 延时一个周期，屎山代码


    when (state === sRun) {
        io.dma_dram_o.valid        := true.B 
        io.dma_dram_o.bits.raddr   := (io.lut_dma_i.bits.waddr >> 3) + count

    }.otherwise {
        io.dma_dram_o.valid        := false.B 
        io.dma_dram_o.bits.raddr   := 0.U

    }

    val dma_lut_o_valid = RegInit(false.B)
    val dma_lut_o_wdata = RegInit(VecInit(Seq.fill(2)(0.U(bus_width.W))))
    
    when (state =/= sIdle && io.dram_dma_i.valid) {
        dma_lut_o_valid             := true.B 
        io.dma_lut_o.bits.wdata(0)  := io.dram_dma_i.bits.rdata(0)
        io.dma_lut_o.bits.wdata(1)  := io.dram_dma_i.bits.rdata(1)
        count                       := count + 1.U
    }.otherwise {
        dma_lut_o_valid             := false.B 
        io.dma_lut_o.bits.wdata(0)  := 0.U
        io.dma_lut_o.bits.wdata(1)  := 0.U
        count                       := 0.U
    }

    io.dma_lut_o.valid          := dma_lut_o_valid
    // io.dma_lut_o.bits.wdata(0)  := dma_lut_o_wdata(0)
    // io.dma_lut_o.bits.wdata(1)  := dma_lut_o_wdata(1)
    io.dma_lut_o.bits.wlast     := last
}