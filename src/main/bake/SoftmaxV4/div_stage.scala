package tb

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._
import define.function._
import define.test._


class exp_div extends Bundle { 
    val sum_frac = UInt(frac_bitwidth.W)
    val sum_exp  = UInt(exp_bitwidth.W) // 尾数匀进去，这样和原最大指数不同，但是尾数保证在1~2
    val data_exp  = UInt(exp_bitwidth.W) // 对完指的
    val data_frac = Vec(cycle_bandwidth, UInt(frac_bitwidth.W)) // 对完指的
}

class div_o extends Bundle { 
    val data_o    = Vec(cycle_bandwidth, UInt(bitwidth.W)) // 对完指的
}

class tb (bandwidth_in: Int) extends Module {
    val expdiv_divu_i  = IO(Flipped(Decoupled(new exp_div)))
    val divu_o         = IO(Decoupled(new div_o))

    // ======================= FSM ==========================
    val data_in_hs  = WireInit(false.B)
    val data_out_hs = WireInit(false.B)

    val dataoutEnd = RegInit(false.B)

    expdiv_divu_i.ready  := true.B

    data_in_hs  := expdiv_divu_i.valid && expdiv_divu_i.ready
    data_out_hs := divu_o.valid && divu_o.ready

    val state  = WireInit(sIdle)
    state := fsm(data_in_hs, data_out_hs, dataoutEnd)

    CycleCounter(state === sRun, state === sIdle, 2)
    // =====================================================
    // ======================== sign ============================
    val sign_o = RegInit(0.U(1.W))
    // ======================== Exp =============================
    val exp_o = RegInit(0.U(exp_bitwidth.W))
    exp_o   := expdiv_divu_i.bits.data_exp - expdiv_divu_i.bits.sum_exp // 没考虑下溢

    // ======================== Mantissa ========================
    val data_frac_w = WireInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(frac_bitwidth.W))))
    val frac_o      = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(frac_bitwidth.W))))
    
    data_frac_w := expdiv_divu_i.bits.data_frac
    
    for (i <- 0 until cycle_bandwidth) {
        frac_o(i) := Lookup(expdiv_divu_i.bits.sum_frac(9, 5), 0.U, Seq(
                                BitPat("b0000011") -> (data_frac_w(i) >> 0.U + data_frac_w(i) >> 0.U + data_frac_w(i) >>  0.U + data_frac_w(i) >>  0.U + data_frac_w(i) >>  0.U),
                                BitPat("b0000011") -> (data_frac_w(i) >> 0.U + data_frac_w(i) >> 0.U + data_frac_w(i) >>  0.U + data_frac_w(i) >>  0.U + data_frac_w(i) >>  0.U),
                                BitPat("b0000111") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  3.U + data_frac_w(i) >>  4.U + data_frac_w(i) >>  5.U),
                                BitPat("b0001011") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  3.U + data_frac_w(i) >>  4.U + data_frac_w(i) >>  9.U),
                                BitPat("b0001111") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  3.U + data_frac_w(i) >>  5.U + data_frac_w(i) >>  7.U),
                                BitPat("b0010011") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  3.U + data_frac_w(i) >>  7.U + data_frac_w(i) >>  8.U),
                                BitPat("b0010111") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  4.U + data_frac_w(i) >>  5.U + data_frac_w(i) >>  6.U),
                                BitPat("b0011011") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  4.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  7.U),
                                BitPat("b0011111") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  4.U + data_frac_w(i) >>  7.U + data_frac_w(i) >> 11.U),
                                BitPat("b0100011") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  5.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  9.U),
                                BitPat("b0100111") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  7.U + data_frac_w(i) >>  8.U),
                                BitPat("b0101011") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  7.U + data_frac_w(i) >>  8.U + data_frac_w(i) >> 11.U),
                                BitPat("b0101111") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  4.U + data_frac_w(i) >>  5.U + data_frac_w(i) >>  6.U),
                                BitPat("b0110011") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  4.U + data_frac_w(i) >>  5.U + data_frac_w(i) >>  7.U),
                                BitPat("b0110111") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  4.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  7.U),
                                BitPat("b0111011") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  4.U + data_frac_w(i) >>  7.U + data_frac_w(i) >> 11.U),
                                BitPat("b0111111") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  5.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  7.U),
                                BitPat("b1000011") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  5.U + data_frac_w(i) >>  7.U + data_frac_w(i) >>  9.U),
                                BitPat("b1000111") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  7.U + data_frac_w(i) >>  8.U),
                                BitPat("b1001011") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  7.U + data_frac_w(i) >>  8.U + data_frac_w(i) >>  9.U),
                                BitPat("b1001111") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  9.U + data_frac_w(i) >> 11.U + data_frac_w(i) >> 11.U),
                                BitPat("b1010011") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 4.U + data_frac_w(i) >>  5.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  8.U),
                                BitPat("b1010111") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 4.U + data_frac_w(i) >>  5.U + data_frac_w(i) >>  7.U + data_frac_w(i) >>  9.U),
                                BitPat("b1011011") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 4.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  7.U + data_frac_w(i) >>  8.U),
                                BitPat("b1011111") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 4.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  9.U + data_frac_w(i) >> 10.U),
                                BitPat("b1100011") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 4.U + data_frac_w(i) >>  7.U + data_frac_w(i) >> 10.U + data_frac_w(i) >> 11.U),
                                BitPat("b1100111") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 5.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  7.U + data_frac_w(i) >>  8.U),
                                BitPat("b1101011") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 5.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  8.U + data_frac_w(i) >> 11.U),
                                BitPat("b1101111") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 5.U + data_frac_w(i) >>  7.U + data_frac_w(i) >>  9.U + data_frac_w(i) >> 10.U),
                                BitPat("b1110011") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 5.U + data_frac_w(i) >>  9.U + data_frac_w(i) >> 11.U + data_frac_w(i) >> 11.U),
                                BitPat("b1110111") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 6.U + data_frac_w(i) >>  7.U + data_frac_w(i) >> 10.U + data_frac_w(i) >> 11.U),
                                BitPat("b1111011") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 6.U + data_frac_w(i) >> 11.U + data_frac_w(i) >> 11.U + data_frac_w(i) >> 11.U),
                                BitPat("b1111111") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 7.U + data_frac_w(i) >> 11.U + data_frac_w(i) >> 11.U + data_frac_w(i) >> 11.U),

                                BitPat("b00000??") -> (data_frac_w(i) >> 0.U + data_frac_w(i) >> 0.U + data_frac_w(i) >>  0.U + data_frac_w(i) >>  0.U),
                                BitPat("b00000??") -> (data_frac_w(i) >> 0.U + data_frac_w(i) >> 0.U + data_frac_w(i) >>  0.U + data_frac_w(i) >>  0.U),
                                BitPat("b00001??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  3.U + data_frac_w(i) >>  4.U),
                                BitPat("b00010??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  3.U + data_frac_w(i) >>  4.U),
                                BitPat("b00011??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  3.U + data_frac_w(i) >>  5.U),
                                BitPat("b00100??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  3.U + data_frac_w(i) >>  7.U),
                                BitPat("b00101??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  4.U + data_frac_w(i) >>  5.U),
                                BitPat("b00110??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  4.U + data_frac_w(i) >>  6.U),
                                BitPat("b00111??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  4.U + data_frac_w(i) >>  7.U),
                                BitPat("b01000??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  5.U + data_frac_w(i) >>  6.U),
                                BitPat("b01001??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  7.U),
                                BitPat("b01010??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 2.U + data_frac_w(i) >>  7.U + data_frac_w(i) >>  8.U),
                                BitPat("b01011??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  4.U + data_frac_w(i) >>  5.U),
                                BitPat("b01100??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  4.U + data_frac_w(i) >>  5.U),
                                BitPat("b01101??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  4.U + data_frac_w(i) >>  6.U),
                                BitPat("b01110??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  4.U + data_frac_w(i) >>  7.U),
                                BitPat("b01111??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  5.U + data_frac_w(i) >>  6.U),
                                BitPat("b10000??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  5.U + data_frac_w(i) >>  7.U),
                                BitPat("b10001??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  7.U),
                                BitPat("b10010??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  7.U + data_frac_w(i) >>  8.U),
                                BitPat("b10011??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 3.U + data_frac_w(i) >>  9.U + data_frac_w(i) >> 11.U),
                                BitPat("b10100??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 4.U + data_frac_w(i) >>  5.U + data_frac_w(i) >>  6.U),
                                BitPat("b10101??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 4.U + data_frac_w(i) >>  5.U + data_frac_w(i) >>  7.U),
                                BitPat("b10110??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 4.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  7.U),
                                BitPat("b10111??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 4.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  9.U),
                                BitPat("b11000??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 4.U + data_frac_w(i) >>  7.U + data_frac_w(i) >> 10.U),
                                BitPat("b11001??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 5.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  7.U),
                                BitPat("b11010??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 5.U + data_frac_w(i) >>  6.U + data_frac_w(i) >>  8.U),
                                BitPat("b11011??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 5.U + data_frac_w(i) >>  7.U + data_frac_w(i) >>  9.U),
                                BitPat("b11100??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 5.U + data_frac_w(i) >>  9.U + data_frac_w(i) >> 11.U),
                                BitPat("b11101??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 6.U + data_frac_w(i) >>  7.U + data_frac_w(i) >> 10.U),
                                BitPat("b11110??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 6.U + data_frac_w(i) >> 11.U + data_frac_w(i) >> 11.U),
                                BitPat("b11111??") -> (data_frac_w(i) >> 1.U + data_frac_w(i) >> 7.U + data_frac_w(i) >> 11.U + data_frac_w(i) >> 11.U)
                        ))
    }






    // ======================== Output ==========================
    when (state === sRun) {
        divu_o.valid := true.B
        for (i <- 0 until cycle_bandwidth) {
            divu_o.bits.data_o(i) := Cat(sign_o, exp_o, frac_o(i))
        }
    }.otherwise {
        divu_o.valid          := false.B
        divu_o.bits.data_o := VecInit(Seq.fill(cycle_bandwidth)(0.U(bitwidth.W))) 
    }

    val counter     = RegInit(0.U(8.W))  
    counter        := Mux(expdiv_divu_i.valid, 0.U, counter + 1.U)

    dataoutEnd := counter === 31.U
}


object tb extends App {
    (new ChiselStage).emitVerilog(new tb(cycle_bandwidth), args)
}

