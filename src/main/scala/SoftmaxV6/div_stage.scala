package div_stage

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._
import define.test._

import pipeline._


class div_input extends Bundle { 
    val data_exp  = UInt(exp_bitwidth.W) // 对完指的
    val data_frac = Vec(cycle_bandwidth, UInt(frac_bitwidth.W)) // 对完指的
}

class div_o extends Bundle { val data_o    = Vec(cycle_bandwidth, UInt(bitwidth.W))}

class div_stage(bandwidth_in: Int, bandwidth_out: Int) extends Module {
    val expdiv_divu_i  = IO(Flipped(Decoupled(new exp_div)))
    val input_divu_i   = IO(Flipped(Decoupled(new div_input)))
    val divu_o         = IO(Valid(new div_o))

    // ======================= FSM ==========================
    expdiv_divu_i.ready  := true.B
    input_divu_i.ready   := true.B
    // ======================== sign ============================
    val sign_o = RegInit(0.U(1.W))
    // ======================== Exp =============================
    val exp_o = RegInit(0.U(exp_bitwidth.W))
    exp_o   := input_divu_i.bits.data_exp - expdiv_divu_i.bits.sum(14, 10) // 没考虑下溢

    // ======================== Mantissa ========================
    val data_frac_w = WireInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(frac_bitwidth.W))))
    val frac_o      = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(frac_bitwidth.W))))
    
    data_frac_w := input_divu_i.bits.data_frac
    val shiftLookup = ListLookup(expdiv_divu_i.bits.sum(9, 5), List(0.U, 0.U, 0.U, 0.U), Array(
            BitPat("b00000") -> List( 0.U,  0.U,  0.U,  0.U),
            BitPat("b00001") -> List( 1.U,  2.U,  3.U,  4.U),
            BitPat("b00010") -> List( 1.U,  2.U,  3.U,  4.U),
            BitPat("b00011") -> List( 1.U,  2.U,  3.U,  5.U),
            BitPat("b00100") -> List( 1.U,  2.U,  3.U,  7.U),
            BitPat("b00101") -> List( 1.U,  2.U,  4.U,  5.U),
            BitPat("b00110") -> List( 1.U,  2.U,  4.U,  6.U),
            BitPat("b00111") -> List( 1.U,  2.U,  4.U,  7.U),
            BitPat("b01000") -> List( 1.U,  2.U,  5.U,  6.U),
            BitPat("b01001") -> List( 1.U,  2.U,  6.U,  7.U),
            BitPat("b01010") -> List( 1.U,  2.U,  7.U,  8.U),
            BitPat("b01011") -> List( 1.U,  3.U,  4.U,  5.U),
            BitPat("b01100") -> List( 1.U,  3.U,  4.U,  5.U),
            BitPat("b01101") -> List( 1.U,  3.U,  4.U,  6.U),
            BitPat("b01110") -> List( 1.U,  3.U,  4.U,  7.U),
            BitPat("b01111") -> List( 1.U,  3.U,  5.U,  6.U),
            BitPat("b10000") -> List( 1.U,  3.U,  5.U,  7.U),
            BitPat("b10001") -> List( 1.U,  3.U,  6.U,  7.U),
            BitPat("b10010") -> List( 1.U,  3.U,  7.U,  8.U),
            BitPat("b10011") -> List( 1.U,  3.U,  9.U, 11.U),
            BitPat("b10100") -> List( 1.U,  4.U,  5.U,  6.U),
            BitPat("b10101") -> List( 1.U,  4.U,  5.U,  7.U),
            BitPat("b10110") -> List( 1.U,  4.U,  6.U,  7.U),
            BitPat("b10111") -> List( 1.U,  4.U,  6.U,  9.U),
            BitPat("b11000") -> List( 1.U,  4.U,  7.U, 10.U),
            BitPat("b11001") -> List( 1.U,  5.U,  6.U,  7.U),
            BitPat("b11010") -> List( 1.U,  5.U,  6.U,  8.U),
            BitPat("b11011") -> List( 1.U,  5.U,  7.U,  9.U),
            BitPat("b11100") -> List( 1.U,  5.U,  9.U, 11.U),
            BitPat("b11101") -> List( 1.U,  6.U,  7.U, 10.U),
            BitPat("b11110") -> List( 1.U,  6.U, 11.U, 11.U),
            BitPat("b11111") -> List( 1.U,  7.U, 11.U, 11.U)
        )
    )

    def calculate(data: UInt, shifts: List[UInt]): UInt = {
        (data >> shifts(0)) + 
        (data >> shifts(1)) + 
        (data >> shifts(2)) + 
        (data >> shifts(3))
    }

    for (i <- 0 until cycle_bandwidth) {
        frac_o(i) := calculate(data_frac_w(i), shiftLookup)
    }

    // ======================== Output ==========================

    val dataoutValid = RegInit(false.B)

    dataoutValid := expdiv_divu_i.valid 
    
    divu_o.valid        := dataoutValid
    when (divu_o.valid) { 
        for (i <- 0 until cycle_bandwidth) { 
            divu_o.bits.data_o(i) := Cat(sign_o, exp_o, frac_o(i))
        }
    }.otherwise { 
        divu_o.bits.data_o := VecInit(Seq.fill(cycle_bandwidth)(0.U(bitwidth.W)))
    }
}