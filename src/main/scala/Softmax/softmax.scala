package softmax

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import os.truncate


class Comparator(bitwidth: Int) extends Module {
    val io = IO(new Bundle{
        val data_i = Input(Vec(datain_bandwidth, UInt(bitwidth.W)))
        val max_o  = Output(Valid(UInt(bitwidth.W)))
    })
    io.max_o.bits := io.data_i(0)
    for (i <- 0 until (datain_bandwidth-1)) {
        when (io.data_i(i + 1) > io.data_i(i)) { io.max_o.bits := io.data_i(i + 1)}
        io.max_o.valid := Mux(i.U === (datain_bandwidth-2).U, true.B, false.B)
    }
}

class Counter(t: Int) extends Module {
    val io  = IO(new Bundle{
        val en      = Input(Bool())
        val cnt_num = Output(UInt(8.W))
        val cnt_end = Output(Bool())
    })
    val cnt     = RegInit(0.U(10.W))
    val nextCnt = cnt + 1.U
    when(io.en) {
        when(cnt < t.U) {
            cnt := nextCnt
        }.otherwise {
            cnt := cnt
        }
    }.otherwise {
        cnt := 0.U
    }
    io.cnt_num  := cnt
    io.cnt_end  := (cnt === t.U).asBool
}

class DelayRegister(gen: Data, n: Int) extends Module {
    val io = IO(new Bundle {
        val data_in = Input(gen)
        val data_out = Output(gen)
    })

    val regs = Reg(Vec(n, gen))
    regs(0) := io.data_in
    for (i <- 1 until n)
        regs(i) := regs(i-1)
    io.data_out := regs(n-1)
}

class SoftMaxDA_Input extends Bundle {
    val data_in = Input(Vec(datain_bandwidth, UInt(bitwidth.W)))
}

class SoftMaxEN_Input extends Bundle {
    val data_in = Input(Vec(datain_bandwidth, UInt(bitwidth.W)))
}

class SoftMax_Output extends Bundle {
    val data_out = Output(Vec(dataout_bandwidth, UInt(bitwidth.W)))
}

class softmax extends Module {
    val da_input  = IO(Flipped(Decoupled(new SoftMaxDA_Input)))
    val en_input  = IO(Flipped(Decoupled(new SoftMaxEN_Input)))
    val output    = IO(Valid(new SoftMax_Output))

    val Max_Reg            = RegInit(0.U(bitwidth.W))
    val Partial_Sum_buffer = RegInit(VecInit(Seq.fill(datain_line_num)(0.U((bitwidth*2).W))))
    val v_buffer           = RegInit(0.U(datain_line_num.W))   
    val inv_buffer         = RegInit(0.U(datain_line_num.W))   
    
    // 2. FSM
    val sIdle :: sRunDA :: sRunEN :: sEnd ::  Nil = Enum(4)   // sIdle 00 sHit 01 sMiss 10 sWb 11
    val state      = RegInit(sIdle)
    val next_state = WireInit(sIdle)

    val start = WireInit(false.B)
    val end   = WireInit(false.B)
    val buffer_full = WireInit(false.B)
    start    := da_input.valid & da_input.ready
    end      := 0.U

    switch (state) {
        is (sIdle) {
            when (start) {
                next_state := sRunDA
            }.otherwise{
                next_state := sIdle
            } 
        }
        is (sRunDA) {
            when (buffer_full) {
                next_state := sRunEN
            }.otherwise {
                next_state := sRunDA
            }
        }
        is (sRunEN) {
            when (end) {
                next_state := sEnd
            }.otherwise {
                next_state := sRunEN
            }
        }
        is (sEnd) {
            when (start) {
                next_state := sRunDA
            }.otherwise {
                next_state := sIdle
            }
        }
    }
    state := next_state

    // ================== DA stage =========================================== // 
    // ================ step 0 init counter 
    da_input.ready := 1.U
    val Counter_inst  = Module(new Counter(t = datain_line_num-1))
    Counter_inst.io.en := Mux(state === sRunDA, true.B, false.B)
    val da_line_num = RegInit(0.U(log2datain_line_num.W)) 
    val di_line_num = RegInit(0.U(log2datain_line_num.W)) 
    da_line_num :=  Counter_inst.io.cnt_num
    di_line_num :=  da_line_num

    val DA_inst = Module(new DAUnit(bitwidth)).io
    DA_inst.en          := state === sRunDA
    DA_inst.line_data_i := da_input.bits.data_in
    DA_inst.line_idx_i  := da_line_num
    DA_inst.max_reg_i   := Max_Reg

    when (DA_inst.max_reg_o.valid) {
        Max_Reg  := DA_inst.max_reg_o.bits
    }

    when (DA_inst.partial_sum_o.valid) {
        Partial_Sum_buffer(DA_inst.line_idx_o) := DA_inst.partial_sum_o.bits
        v_buffer := v_buffer.bitSet(DA_inst.line_idx_o, true.B)
    }

    // ================== DI stage =========================================== // 
    val DI_inst = Module(new DIUnit(bitwidth)).io
    DI_inst.en          := v_buffer(di_line_num) === true.B  
    DI_inst.ps_i        := Partial_Sum_buffer(di_line_num)
    DI_inst.line_idx_i  := di_line_num

    when (DI_inst.inv_o.valid) {
        Partial_Sum_buffer(DI_inst.line_idx_o) := DI_inst.inv_o.bits
        inv_buffer                      := inv_buffer.bitSet(DI_inst.line_idx_o, true.B)
    }

    buffer_full := (inv_buffer(datain_line_num.U-1.U) === 1.U)
    // ================== EN stage =========================================== // 
    // val max = WireInit(0.U(bitwidth.W))
    // max := Max_Reg
    val en_insts = VecInit(Seq.fill(numElements)(Module(new ENUnit(bitwidth)).io))
    en_input.ready := state === sRunEN 
    for (i <- 0 until numElements) {
        en_insts(i).max_i   := Max_Reg
        en_insts(i).inv_i   := Partial_Sum_buffer(i)
        en_insts(i).x_i     := en_input.bits.data_in(i) 
        output.valid        := true.B
        output.bits.data_out(i) := en_insts(i).softmax_o 
    }
    // ===
    output.valid := false.B
    for(i <- 0 until dataout_bandwidth) { output.bits.data_out(i) := 0.U }
}

class DAUnit(bitwidth: Int) extends Module {
    val io = IO(new Bundle {
        val en          = Input(Bool())
        val line_data_i = Input(Vec(dataout_bandwidth, UInt(bitwidth.W)))
        val line_idx_i  = Input(UInt(log2datain_line_num.W))
        val max_reg_i   = Input(UInt(bitwidth.W))
        val line_idx_o  = Output(UInt(log2datain_line_num.W))
        val max_reg_o       = Output(Valid(UInt(bitwidth.W)))
        val partial_sum_o   = Output(Valid(UInt(bitwidth.W)))
    })
    // =============== step 1 find current_max
    val current_max  = WireInit(0.U(bitwidth.W)) 
    val previous_max = WireInit(0.U(bitwidth.W))
    val Comparator_inst  = Module(new Comparator(bitwidth = bitwidth))
    Comparator_inst.io.data_i := io.line_data_i
    current_max  := Mux(Comparator_inst.io.max_o.valid, Comparator_inst.io.max_o.bits, 0.U) 
    previous_max := io.max_reg_i
    // ================= step 2 compare with previous_max (clk)
    // val delta        = current_max - previous_max
    io.max_reg_o.valid := io.en && (current_max - previous_max > 0.U)   
    io.max_reg_o.bits  := Mux(io.max_reg_o.valid, current_max, previous_max)
    // ================= step 3 max-sub trick (clk)
    val subVec           = io.line_data_i.map(i => i -& io.max_reg_o.bits) // Vec内所有值减去最大值
    val partial_sum      = WireInit(0.U(bitwidth.W));
    io.partial_sum_o.bits  := subVec.reduce(_ +& _)  // 对Vec内所有值求和
    io.partial_sum_o.valid := io.en
    io.line_idx_o := Mux(io.en, io.line_idx_i, 0.U)
    printf("[DA] line %d, partial_sum_o = %d, valid = %d\n", io.line_idx_o, io.partial_sum_o.bits, io.partial_sum_o.valid );
    // ================= step 4 update Partial Sum buffer
    // when (update_max) {
    //     for(i <- 0 until datain_bandwidth) { 
    //         when (inv_buffer(i) === 1.U) {
    //             Partial_Sum_buffer(i) >> delta
    //         }.elsewhen (v_buffer(i) === 1.U) {
    //             Partial_Sum_buffer(i) << delta
    //         }
    //     }
    // }
}

class DIUnit(bitwidth: Int) extends Module {
    val io = IO(new Bundle {
        val en          = Input(Bool())
        val ps_i        = Input(UInt((bitwidth*2).W))
        val line_idx_i  = Input(UInt(log2datain_line_num.W))
        val line_idx_o  = Output(UInt(log2datain_line_num.W))
        val inv_o       = Output(Valid(UInt((bitwidth*2).W)))
    })

    io.inv_o.valid   := io.en
    io.inv_o.bits    := Mux(io.ps_i =/= 0.U, ~0.U/io.ps_i, 0.U)  // ~0.U = 0xFF
    io.line_idx_o    := io.line_idx_i
    printf("[DI] line %d, inverse_o = %d valid = %d\n", io.line_idx_o, io.inv_o.bits, io.inv_o.valid);
}

class ENUnit(bitwidth: Int) extends Module {
    val io = IO(new Bundle {
        val max_i     = Input(UInt(bitwidth.W))
        val inv_i     = Input(UInt((bitwidth*2).W))
        val x_i       = Input(UInt(bitwidth.W))
        val softmax_o = Output(UInt(bitwidth.W))
    })
    // calculate x_q
    val x_q = io.x_i - io.max_i
    // calculation shift
    val shift = x_q >> (bitwidth - log2bitwidth)

    // 对inv进行右移shift位
    io.softmax_o := (io.inv_i >> shift)(bitwidth-1, 0)
}

