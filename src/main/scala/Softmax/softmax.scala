package softmax

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._







class SoftMax_Input extends Bundle {
    val raw_data = Vec(datain_bandwidth, UInt(bitwidth.W))
}

class SoftMax_Output extends Bundle {
    val res_data = Vec(datain_bandwidth, UInt(bitwidth.W))
}



class softmax extends Module {
    val data_in  = IO(Flipped(Decoupled(new SoftMax_Input)))
    val data_out = IO(Decoupled(new SoftMax_Output))

    // ======================= FSM ==========================
    val data_in_hs  = WireInit(false.B)
    val run_done    = WireInit(false.B)
    val data_out_hs = WireInit(false.B)

    data_in_hs  := data_in.valid && data_in.ready
    data_out_hs := data_out.valid && data_out.ready

    val state  = WireInit(sIdle)
    state := fsm(data_in_hs, run_done, data_out_hs)
    // =====================================================

    val sign_vec  = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U(1.W))))
    val exp_vec   = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((exp_bitwidth).W))))
    val frac_vec  = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))
    
    for (i <- 0 until datain_bandwidth) {
        sign_vec(i) := Mux(data_in_hs, data_in.bits.raw_data(i)(bitwidth - 1),               sign_vec(i)) 
        exp_vec(i)  := Mux(data_in_hs, data_in.bits.raw_data(i)(bitwidth - 2, bitwidth - 6), exp_vec(i))  
        frac_vec(i) := Mux(data_in_hs, data_in.bits.raw_data(i)(bitwidth - 7, 0),            frac_vec(i))
    }
    // val Preprocess_inst = Module(new PreprocessUnit(bitwidth)).io
    // Preprocess_inst.preu_i <> data_in

    val Max_inst   = Module(new MaxComparator(bitwidth = exp_bitwidth, numElements = datain_bandwidth)).io 
    Max_inst.maxu_i.valid           := data_in.valid && data_in.ready
    data_in.ready                   := Max_inst.maxu_i.ready
    // Max_inst.maxu_i.bits.data       := exp_vec
    for (i <- 0 until datain_bandwidth) { Max_inst.maxu_i.bits.data (i)  := data_in.bits.raw_data(i)(bitwidth - 2, bitwidth - 6)}
    

    val Shift_inst = Module(new frac_shift).io 
    Shift_inst.shiftu_i.bits.max    := Max_inst.maxu_o.bits.max
    Shift_inst.shiftu_i.valid       := Max_inst.maxu_o.valid
    Max_inst.maxu_o.ready           := Shift_inst.shiftu_i.ready 
    Shift_inst.shiftu_i.bits.frac   := frac_vec
    Shift_inst.shiftu_i.bits.exp    := exp_vec
    Shift_inst.shiftu_o.ready       := data_out.ready

    run_done    := Shift_inst.shiftu_o.valid

    // when (Preprocess_inst.sign_o.valid) {
    //     printf("result after precess [");
    //     for (i <- 0 until datain_bandwidth) {
    //         printf("(%d, %d, %d) ", Preprocess_inst.sign_o.bits(i), Preprocess_inst.exp_o.bits(i), Preprocess_inst.frac_o.bits(i));
    //     }
    //     printf("]\n");
    // }
    data_out.valid           := Shift_inst.shiftu_o.valid
    data_out.bits.res_data   := Shift_inst.shiftu_o.bits.frac

}

class max_input extends Bundle { val data  = Vec(numElements, UInt(exp_bitwidth.W))}
class max_output extends Bundle { val max = UInt(exp_bitwidth.W)}
class MaxComparator(val bitwidth: Int, val numElements: Int) extends Module {
    val io = IO(new Bundle {
        val maxu_i  = Flipped(Decoupled(new max_input))
        val maxu_o  = Decoupled(new max_output)
    })

    val max   = RegInit(0.U(bitwidth.W))
    val count = RegInit(0.U(log2Ceil(numElements + 1).W))

    // ======================= FSM ==========================
    val state       = WireInit(sIdle)
    val maxu_i_hs   = io.maxu_i.ready && io.maxu_i.valid
    val maxu_o_hs   = io.maxu_o.ready && io.maxu_o.valid
    state := fsm(maxu_i_hs, (count === numElements.U-1.U), maxu_o_hs)
    // ======================================================

    io.maxu_i.ready := state === sIdle

    val data_vec     = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((exp_bitwidth).W))))
    data_vec        := Mux(maxu_i_hs, io.maxu_i.bits.data, data_vec)

    when (state === sRun) {
        for (i <- 0 until numElements) {
            when (data_vec(i) > max) {
                max := data_vec(i)
            }
            count := count + 1.U
        }
        io.maxu_o.valid     := false.B 
        io.maxu_o.bits.max  := max
    }.elsewhen (state === sDone) {
        io.maxu_o.valid := true.B 
        io.maxu_o.bits.max  := max
        count          := 0.U
    }.otherwise {
        io.maxu_o.valid     := false.B 
        io.maxu_o.bits.max  := 0.U
        count               := 0.U        
        max                 := 0.U        
    }
}

class shift_input extends Bundle {   
    val max  = UInt(exp_bitwidth.W)
    val exp  = Vec(datain_bandwidth, UInt(exp_bitwidth.W))
    val frac = Vec(datain_bandwidth, UInt(frac_bitwidth.W))
}
class shift_output extends Bundle { val frac = (Vec(datain_bandwidth, UInt(frac_bitwidth.W)))}
class frac_shift extends Module {
    /*
       单元功能：对齐指数后，尾数移位
       输入：指数、尾数（向量），最大值
       输出：移位后尾数（向量）
    */
    val io = IO(new Bundle  {
        val shiftu_i = Flipped(Decoupled(new shift_input))
        val shiftu_o = Decoupled(new shift_output)
    })

    // ======================= FSM ==========================
    val state       = WireInit(sIdle)
    val is_end      = WireInit(false.B)
    val shiftu_i_hs   = io.shiftu_i.ready && io.shiftu_i.valid
    val shiftu_o_hs   = io.shiftu_o.ready && io.shiftu_o.valid
    state := fsm(shiftu_i_hs, is_end, shiftu_o_hs)
    // ======================================================

    val max         = RegInit(0.U(exp_bitwidth.W))
    val frac_vec    = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))
    val exp_vec     = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((exp_bitwidth).W))))
    
    max      := Mux(shiftu_i_hs, io.shiftu_i.bits.max , max) 
    exp_vec  := Mux(shiftu_i_hs, io.shiftu_i.bits.exp , exp_vec)  
    frac_vec := Mux(shiftu_i_hs, io.shiftu_i.bits.frac, frac_vec)

    // val shift   = WireInit(VecInit(Seq.fill(datain_bandwidth)(0.U((exp_bitwidth).W))))
    val frac    = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))

    io.shiftu_i.ready := state === sIdle

    when (state === sRun) {
        for (i <- 0 until datain_bandwidth) { 
            // shift(i)    := Mux(max === exp_vec(i), 0.U, max - exp_vec(i))
            frac(i)     := Mux(exp_vec(i) =/= 0.U, ((Cat(1.U, frac_vec(i))) >> (max - exp_vec(i))), 
                                (frac_vec(i) >> (max - exp_vec(i))))
            is_end      := Mux(i.U === (datain_bandwidth-1).U, true.B, false.B)
        }
        io.shiftu_o.valid       := false.B 
        io.shiftu_o.bits.frac   := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))
    }.elsewhen (state === sDone) {
        io.shiftu_o.valid       := true.B 
        io.shiftu_o.bits.frac   := frac
    }.otherwise {
        io.shiftu_o.valid       := false.B 
        io.shiftu_o.bits.frac   := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))
        // for (i <- 0 until datain_bandwidth) { io.frac_o.bits(i)  := 0.U }
    }

}

// class PreprocessUnit(bitwidth: Int) extends Module {
//     /*
//        单元功能：预处理（对齐指数+提取三个部分+找最大值）
//        输入：输入的FP16数据（向量）
//        输出：对齐后的符号位,指数,尾数,最大值（向量）
//     */
//     class pre_input extends Bundle { val raw_data  = Vec(datain_bandwidth, UInt(bitwidth.W))}
    
//     class pre_output extends Bundle {   
//         val max   = UInt(bitwidth.W)
//         val sign  = Vec(datain_bandwidth, UInt(1.W))
//         val exp   = Vec(datain_bandwidth, UInt(exp_bitwidth.W))
//         val frac  = Vec(datain_bandwidth, UInt(frac_bitwidth.W))
//     }

//     val io = IO(new Bundle {
//         val preu_i = Flipped(Decoupled(new pre_input))
//         val preu_o = Decoupled(new pre_output)
//     })

//     // ==============================================
//     // step 1: find the max value

//     // val sign_vec  = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U(1.W))))
//     // val exp_vec   = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((exp_bitwidth).W))))
//     // val frac_vec  = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))

//     // for (i <- 0 until datain_bandwidth) {
//     //     sign_vec(i) :=  io.preu_i.raw_data(i)(bitwidth - 1)
//     //     exp_vec(i)  :=  io.preu_i.raw_data(i)(bitwidth - 2, bitwidth - 6)
//     //     frac_vec(i) :=  io.preu_i.raw_data(i)(bitwidth - 7, 0)
//     // }

//     // for (i <- 0 until datain_bandwidth) {
//     //     sign_vec(i) :=  sign_vec0(i)
//     //     exp_vec(i)  :=  exp_vec0(i) 
//     //     frac_vec(i) :=  frac_vec0(i)
//     // }
    
//     val Comparator_inst  = Module(new MaxComparator(bitwidth = bitwidth, numElements = datain_bandwidth)).io 
//     io.preu_i <> Comparator_inst.maxu_i

//     // val max         = RegInit(0.U)
//     // val max_valid   = RegInit(false.B)
//     // max       := Mux(Comparator_inst.io.max_o.valid, Comparator_inst.io.max_o.bits, 0.U)
//     // max_valid := Mux(Comparator_inst.io.max_o.valid, true.B, false.B)
//     Comparator_inst.maxu_o <>  frac_shift_inst.shiftu_i
    
//     // io.max_o  := max
//     // val maxPwridx: UInt = vec.indexWhere((x: UInt) => x === vec.max)

//     // ==============================================
//     // step 2: share exp generate
//     val Shift_inst  = Module(new frac_shift).io 


//     io.preu_o.bits.valid := Shift_inst.shiftu_o.valid
//     io.preu_o.bits.frac  := Shift_inst.shiftu_o.bits.frac
//     io.preu_o.bits.max   := Comparator_inst.maxu_o.bits.max
//     io.preu_o.bits.sign  := Comparator_inst.maxu_o.bits.sign
//     io.preu_o.bits.exp   := Comparator_inst.maxu_o.bits.exp
//     // frac_shift_inst.en      := max_valid
//     // frac_shift_inst.max_i   := max
//     // frac_shift_inst.exp_i   := exp_vec
//     // frac_shift_inst.frac_i  := frac_vec


//     // for (i <- 0 until datain_bandwidth) {
//     //     io.frac_o.valid     := frac_shift_inst.frac_o.valid 
//     //     io.frac_o.bits(i)   := Mux(!io.frac_o.valid, 0.U, frac_shift_inst.frac_o.bits(i))
//     // }

//     // for (i <- 0 until datain_bandwidth) {
//     //     io.exp_o.valid      := frac_shift_inst.frac_o.valid  
//     //     io.exp_o.bits(i)    := Mux(!io.exp_o.valid, 0.U, 
//     //                             Mux(io.max_o === exp_vec(i), io.max_o, io.max_o - 1.U))
//     //     // when (io.max_o === exp_vec(i))  {printf("here\n");}                      
//     //     // when (io.max_o =/= exp_vec(i))  {printf("%d ,%d\n", io.max_o, exp_vec(i));}                      
//     // }

//     // for (i <- 0 until datain_bandwidth) {
//     //     io.sign_o.valid    := frac_shift_inst.frac_o.valid  
//     //     io.sign_o.bits(i)  := Mux(!io.sign_o.valid, 0.U, sign_vec(i))
//     // }

    
//     // raw_data_i.io.ready := Comparator_inst.io.
// }

