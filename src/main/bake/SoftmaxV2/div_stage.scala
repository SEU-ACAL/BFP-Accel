package softmax

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._
import define.test._

import pipeline._


// class DIV_stage extends Module {
//     val expdiv_div_i  = IO(Flipped(Decoupled(new expu_divu)))
//     val hit_table_i   = IO(Flipped(Decoupled(new table_divu)))
//     val div_o         = IO(Decoupled(new divu_outu))

//     // ======================= FSM ==========================
//     val data_in_hs  = WireInit(false.B)
//     val run_done    = WireInit(false.B)
//     val data_out_hs = WireInit(false.B)

//     data_in_hs  := expdiv_div_i.valid && expdiv_div_i.ready
//     data_out_hs := div_o.valid && div_o.ready

//     val state  = WireInit(sIdle)
//     state := fsm(data_in_hs, run_done, data_out_hs)
//     // =====================================================

//     for (i <- 0 until cycle_bandwidth) {
//         div_o.bits.frac(i) := hit_table_i.bits.frac(i) / expdiv_div_i.bits.sum 
//     }
    

// }


// class Div_i extends Bundle {
//     val divisor = Vec(cycle_bandwidth, UInt(sum_bitwidth.W))
// }

class DIV_stage(bandwidth_in: Int) extends Module{
    val expdiv_div_i   = IO(Flipped(Decoupled(new Bundle{val sum = UInt(16.W)}))) // 分子
    val divisor_i      = IO(Flipped(Decoupled(new Div_i))) // 分母
    val div_out_o      = IO(Valid(new Bundle{ val res_data = Vec(dataout_bandwidth, UInt(bitwidth.W))}))

    // ======================= FSM ==========================
    val data_in_hs  = WireInit(false.B)
    val outEnd      = WireInit(false.B)
    val data_out_hs = WireInit(false.B)

    data_in_hs  := expdiv_div_i.valid && expdiv_div_i.ready
    // data_out_hs := div_out_o.valid && div_out_o.ready

    divisor_i.ready := true.B
    expdiv_div_i.ready := true.B


    val state  = WireInit(sIdle)
    state := fsm(data_in_hs, div_out_o.valid, outEnd)

    CycleCounter(state === sRun, state === sIdle, 3)
    // =====================================================

    // val divus = VecInit(Seq.fill(cycle_bandwidth)(Module(new divu.io)))
    val batch_num = RegInit(0.U(log2Up(maxBatch).W))

    when (state =/= sDone) {
        batch_num := 0.U
    }.otherwise {
        batch_num := batch_num + 1.U
    }

    outEnd := batch_num === (maxBatch - 1).U 
    
    when (state === sDone) {
        div_out_o.valid         := true.B
        for (i <- 0 until cycle_bandwidth) {
            div_out_o.bits.res_data(i) := divisor_i.bits.divisor(i) / expdiv_div_i.bits.sum
        }
    }.otherwise {
        div_out_o.valid         := false.B
        batch_num := batch_num + 1.U
        for (i <- 0 until cycle_bandwidth) {
            div_out_o.bits.res_data(i) := 0.U
        }
    }


}

// class divu extends Module{
//     val dividend_float = IO(Flipped(Decoupled(new Bundle{val dividend = UInt(16.W)}))) // 分子
//     val divisor_float  = IO(Flipped(Decoupled(new Bundle{val divisor = UInt(sum_bitwidth.W)}))) // 分母
//     val exception      = IO(Valid(UInt(2.W)))
//     val div_out        = IO(Valid(new Bundle{ val data_out = UInt(16.W)}))
    
//     val separate_float  = Module(new sign_exp)
//     val temp_exp        = WireInit(0.U(5.W))
//     val u_fix_div       = Module(new fix_div)
//     val temp_m          = WireInit(0.U(11.W))
//     val cnt             = RegInit(0.U(2.W))
    
//     cnt := Mux(cnt === 1.U, 0.U, cnt + 1.U)
//         dividend_float.ready := false.B
//         divisor_float.ready  := false.B
//         exception.valid      := false.B
//         div_out.valid        := false.B

//     when(cnt === 0.U){
//         dividend_float.ready :=true.B
//         divisor_float.ready  :=true.B
//         exception.valid      :=false.B
//         div_out.valid        :=false.B
//     }
   
//     separate_float.fpin1 := dividend_float.bits
//     separate_float.fpin2 := divisor_float.bits
//     exception.bits       := separate_float.except_code

//     u_fix_div.dividend   := separate_float.mout1
//     u_fix_div.divisor     := separate_float.mout2

//     when(u_fix_div.out(10)===1.U){
//         temp_m              := u_fix_div.out
//         temp_exp            := separate_float.exp_out
//     }.otherwise{
//         temp_m              := u_fix_div.out<<1.U
//         temp_exp            := separate_float.exp_out-1.U
//     }
//     when(cnt===1.U){
//         exception.valid      := true.B
//         div_out.valid        := true.B
//         dividend_float.ready := false.B
//         divisor_float.ready   := false.B
//     }
//     div_out.bits        := separate_float.sign##temp_exp##temp_m(9,0)
// }


// //make two exp part sub ,
// class sign_exp extends Module {
//     val io=IO(new Bundle{
//         val fpin1=Input(UInt(16.W))
//         val fpin2=Input(UInt(16.W))
//         val exp_out=Output(UInt(5.W))
//         val mout1=Output(UInt(10.W))
//         val mout2=Output(UInt(10.W))
//         val sign=Output(UInt(1.W))
//         val except_code=Output(UInt(2.W)) //00 normal   01 shangyi 10 xiayi  11 undefined
//     })
//     val exp_temp=WireInit(0.U(6.W))
//     val in1temp=WireInit(0.U(6.W))
//     val in2temp=WireInit(0.U(6.W))
//     in1temp:=io.fpin1(14,10)
//     in2temp:=io.fpin2(14,10)
//     exp_temp:=in1temp-in2temp+15.U
//     io.except_code:=Mux(exp_temp(5)===1.U,Mux(in1temp>in2temp,1.U,2.U),0.U)
//     io.exp_out:=exp_temp(4,0)
//     io.mout1:=io.fpin1(9,0)
//     io.mout2:=io.fpin2(9,0)
//     io.sign:=Mux(io.fpin1(15)===io.fpin2(15),1.U,0.U)
// }

// // 尾数除法
// class fix_div extends Module{
//     val io=IO(new Bundle{
//         val dividend=Input(UInt(10.W))
//         val divisor =Input(UInt(10.W))
//         val out=Output(UInt(11.W))
//     })
//     val mem             = Module(new init_mem)
//     val dividend_temp   = WireInit(0.U(11.W))
//     val out_temp        = WireInit(0.U(11.W))
//     val mem_out         = VecInit.fill(4)(0.U(4.W))
    
//     val temp1           = WireInit(0.U(11.W))
//     val temp2           = WireInit(0.U(11.W))
//     val temp3           = WireInit(0.U(11.W))
//     val temp4           = WireInit(0.U(11.W))
//     temp1               := dividend_temp>>mem_out(0)
//     temp2               := dividend_temp>>mem_out(1)
//     temp3               := dividend_temp>>mem_out(2)
//     temp4               := dividend_temp>>mem_out(3)
    
//     dividend_temp   :=  "b1".U##io.dividend
//     mem.io.addr     :=  io.divisor(9,6)
//     mem_out         :=  mem.io.dataOut
//     out_temp        :=  temp1+temp2+temp3+temp4
//     io.out          :=  out_temp
// }











