package shiftu

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._



class max_shift_input extends Bundle {   
    val max  = UInt(exp_bitwidth.W)
    val exp  = Vec(datain_bandwidth, UInt(exp_bitwidth.W))
    val frac = Vec(datain_bandwidth, UInt(frac_bitwidth.W))
}

class shift_sub_output extends Bundle { 
    val frac = Vec(datain_bandwidth, UInt(frac_bitwidth.W))
}

class frac_shift1 extends Module {
    /*
       单元功能：对齐指数后，尾数移位
       输入：指数、尾数（向量），最大值
       输出：移位后尾数（向量）
    */
    val io = IO(new Bundle  {
        val maxu_shiftu_i = Flipped(Decoupled(new max_shift_input))
        val shiftu_subu_o = Decoupled(new shift_sub_output)
    })

    // ======================= FSM ==========================
    val state         = WireInit(sIdle)
    val is_end        = WireInit(false.B)
    val maxu_shiftu_i_hs   = io.maxu_shiftu_i.ready && io.maxu_shiftu_i.valid
    val shiftu_subu_o   = io.shiftu_subu_o.ready && io.shiftu_subu_o.valid
    state := fsm(maxu_shiftu_i_hs, is_end, shiftu_subu_o)
    // ======================================================

    val max         = RegInit(0.U(exp_bitwidth.W))
    val frac_vec    = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))
    val exp_vec     = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((exp_bitwidth).W))))
    
    max      := Mux(maxu_shiftu_i_hs, io.maxu_shiftu_i.bits.max , max) 
    exp_vec  := Mux(maxu_shiftu_i_hs, io.maxu_shiftu_i.bits.exp , exp_vec)  
    frac_vec := Mux(maxu_shiftu_i_hs, io.maxu_shiftu_i.bits.frac, frac_vec)

    // val shift   = WireInit(VecInit(Seq.fill(datain_bandwidth)(0.U((exp_bitwidth).W))))
    val frac    = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))

    io.maxu_shiftu_i.ready := state === sIdle

    when (state === sRun) {
        for (i <- 0 until datain_bandwidth) { 
            // shift(i)    := Mux(max === exp_vec(i), 0.U, max - exp_vec(i))
            frac(i)     := Mux(exp_vec(i) =/= 0.U, ((Cat(1.U, frac_vec(i))) >> (max - exp_vec(i))), 
                                (frac_vec(i) >> (max - exp_vec(i))))
            is_end      := Mux(i.U === (datain_bandwidth-1).U, true.B, false.B)
        }
        io.shiftu_subu_o.valid       := false.B 
        io.shiftu_subu_o.bits.frac   := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))
    }.elsewhen (state === sDone) {
        io.shiftu_subu_o.valid       := true.B 
        io.shiftu_subu_o.bits.frac   := frac
    }.otherwise {
        io.shiftu_subu_o.valid       := false.B 
        io.shiftu_subu_o.bits.frac   := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))
        // for (i <- 0 until datain_bandwidth) { io.frac_o.bits(i)  := 0.U }
    }
}



// class exp_shift_input extends Bundle {   
//     val frac = Vec(datain_bandwidth, UInt(frac_bitwidth.W))
// }

// class shift_add_output extends Bundle { 
//     val frac = (Vec(datain_bandwidth, UInt(frac_bitwidth.W)))
// }

// class frac_shift2 extends Module {
//     /*
//        单元功能：对齐指数后，尾数移位
//        输入：指数、尾数（向量），最大值
//        输出：移位后尾数（向量）
//     */
//     val io = IO(new Bundle  {
//         val expu_shiftu_i = Flipped(Decoupled(new exp_shift_input))
//         val shiftu_addu_o = Decoupled(new shift_add_output)
//     })

//     // ======================= FSM ==========================
//     val state              = WireInit(sIdle)
//     val is_end             = WireInit(false.B)
//     val expu_shiftu_i_hs   = io.expu_shiftu_i.ready && io.expu_shiftu_i.valid
//     val shiftu_addu_o_hs   = io.shiftu_subu_o.ready && io.shiftu_subu_o.valid
//     state := fsm(expu_shiftu_i_hs, is_end, shiftu_addu_o_hs)
//     // ======================================================

//     val max         = RegInit(0.U(exp_bitwidth.W))
//     val frac_vec    = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))
//     val exp_vec     = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((exp_bitwidth).W))))
    
//     max      := Mux(expu_shiftu_i_hs, io.expu_shiftu_i.bits.max , max) 
//     exp_vec  := Mux(expu_shiftu_i_hs, io.expu_shiftu_i.bits.exp , exp_vec)  
//     frac_vec := Mux(expu_shiftu_i_hs, io.expu_shiftu_i.bits.frac, frac_vec)

//     // val shift   = WireInit(VecInit(Seq.fill(datain_bandwidth)(0.U((exp_bitwidth).W))))
//     val frac    = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))

//     io.expu_shiftu_i.ready := state === sIdle

//     when (state === sRun) {
//         for (i <- 0 until datain_bandwidth) { 
//             // shift(i)    := Mux(max === exp_vec(i), 0.U, max - exp_vec(i))
//             frac(i)     := Mux(exp_vec(i) =/= 0.U, ((Cat(1.U, frac_vec(i))) >> (max - exp_vec(i))), 
//                                 (frac_vec(i) >> (max - exp_vec(i))))
//             is_end      := Mux(i.U === (datain_bandwidth-1).U, true.B, false.B)
//         }
//         io.shiftu_addu_o.valid       := false.B 
//         io.shiftu_addu_o.bits.frac   := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))
//     }.elsewhen (state === sDone) {
//         io.shiftu_addu_o.valid       := true.B 
//         io.shiftu_addu_o.bits.frac   := frac
//     }.otherwise {
//         io.shiftu_addu_o.valid       := false.B 
//         io.shiftu_addu_o.bits.frac   := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))
//         // for (i <- 0 until datain_bandwidth) { io.frac_o.bits(i)  := 0.U }
//     }
// }