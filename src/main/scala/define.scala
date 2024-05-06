package define

import chisel3._
import chisel3.util._
import chisel3.stage._


object MACRO { 
    val datain_bandwidth    = 8
    val cycle_bandwidth     = 8
    val datain_lines        = 5
    val datain_bitwidth     = 16    
    val dataout_bitwidth    = 16    
    val bitwidth            = 16
    val exp_bitwidth        = 5
    val frac_bitwidth       = 10
    val expvalue_bitwidth   = 16
    
    val lutidx_bitwidth    = 8

    val partSet_size       = 256 // 有多少个数
    val fullSet_size       = partSet_size * 17 // 除去underflow和overflower一共17个set可用

    val lut_width           = 8
    val lut_depth           = partSet_size / lut_width
    val lut_bitwidth        = 16
    val lut_set             = 4

    val dram_width           = lut_width
    val dram_depth           = fullSet_size / lut_width  
    val dram_bitwidth        = 16

    val dma_burst_len        = lut_depth / 2 // dual port
    val bus_width            = lut_width * lut_bitwidth

    val underflow_threshold = 11     // 0~11
    val overflow_threshold  = 28    // 28~30

    val sum_bitwidth        = 16

    val numElements         = datain_bandwidth
}

class FSM extends Module {
    val io = IO(new Bundle {
        val is_start = Input(Bool())
        val is_end   = Input(Bool())
        val is_done  = Input(Bool())
        val state    = Output(UInt(2.W))
    })

    val sIdle :: sRun :: sDone :: Nil = Enum(3)
    val state      = RegInit(sIdle)
    val next_state = WireInit(sIdle)


    io.state := state

    switch (state) {
        is (sIdle) {
            when (io.is_start) {
                next_state := sRun
            }.otherwise {
                next_state := sIdle
            } 
        }
        is (sRun) {
            when (io.is_done) {
                next_state := sDone
            }.otherwise {
                next_state := sRun
            }
        }
        is (sDone) {
            when (io.is_end) {
                next_state := sIdle
            }.otherwise {
                next_state := sDone
            }
        }
    }
    state := next_state 
}

object FSM {
    def fsm(is_start: Bool, is_done: Bool, is_end: Bool): UInt = {
        val fsmModule = Module(new FSM)
        fsmModule.io.is_start   := is_start
        fsmModule.io.is_done    := is_done
        fsmModule.io.is_end     := is_end
        fsmModule.io.state
    }

    val sIdle = 0.U   
    val sRun  = 1.U
    val sDone = 2.U   // sIdle 00 sRun 01 sDone 10 
}

object function {
    def hs_uint_dff(handshake: Bool, DataWidth: UInt, data_default: UInt, data_i: UInt): UInt = {
        val data_o = RegInit(data_default)
        data_o := Mux(handshake, data_i, data_o)
        data_o
    }
    def hs_uvec_dff(handshake: Bool, DataWidth: UInt, BandWidth: Int, data_default: Vec[UInt], data_i: Vec[UInt]): Vec[UInt] = {
        val data_o = RegInit(data_default)
        data_o := Mux(handshake, data_i, data_o)
        data_o
    }
    def hs_bool_dff(handshake: Bool, bool_default: Bool, data_i: Bool): UInt = {
        val data_o = RegInit(bool_default)
        data_o := Mux(handshake, data_i, data_o)
        data_o
    }
}