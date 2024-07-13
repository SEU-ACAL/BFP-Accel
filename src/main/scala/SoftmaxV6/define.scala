package softmax_define

import chisel3._
import chisel3.util._
import chisel3.stage._


object MACRO { 
    // =============== MAXU 
    
    // =============== EXPU
    val sum_bitwidth            = 16

    val idx_bitwidth            = 5 // 取五位做索引 
    val rate_bitwidth           = 5 // 取五位做插值

    val lut_bandwidth           = 256
    val lut_datawidth           = 16

    val AdderinBitwidth         = 16
    val dataout_bitwidth        = 16

    // =============== LUT
    val partSet_size       = 128 // 有多少个数
    val partSet_num        = 19 // 有多少个小表
    val fullSet_size       = partSet_size * partSet_num // 除去underflow和overflower一共19个set可用

    val lut_width           = 8
    val lut_depth           = partSet_size / lut_width / 2 // set数翻倍了，lut深度折半了
    val lut_bitwidth        = 15
    val lut_set             = cycle_bandwidth // 实际是32个

    // =============== top
    val datain_bandwidth        = 1024
    val cycle_bandwidth         = 64
    val maxBatch                = datain_bandwidth / cycle_bandwidth // 最大batch数

    // ============== global
    val bitwidth                = 16
    val exp_bitwidth            = 5
    val frac_bitwidth           = 10
    val expvalue_bitwidth       = 16



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

object test {
    def CycleCounter(enable: Bool, reset: Bool, stage: Int): UInt = {
        val counter = RegInit(0.U(32.W))
        when(reset) {
            printf("[stage[%d]-%d-cycles]\n", stage.U, counter)
            counter := 0.U
        }.elsewhen(enable) {
            counter := counter + 1.U
        }
            counter
    }
}