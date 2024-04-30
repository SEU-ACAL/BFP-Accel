package addu

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._

// object WallceTree_generator {
//     def addOneColumn(col: Seq[Bool], cin: Seq[Bool]):(Seq[Bool], Seq[Bool], Seq[Bool]) = {
//         var sum = Seq[Bool]()
//         var cout1 = Seq[Bool]()
//         var cout2 = Seq[Bool]()
//         col.size match {
//             case 1 =>  // do nothing
//                 sum = col ++ cin
//             case 2 =>
//                 val c22 = Module(new C22)
//                 c22.io.in := col
//                 sum = c22.io.out(0).asBool() +: cin
//                 cout2 = Seq(c22.io.out(1).asBool())
//             case 3 =>
//                 val c32 = Module(new C32)
//                 c32.io.in := col
//                 sum = c32.io.out(0).asBool() +: cin
//                 cout2 = Seq(c32.io.out(1).asBool())
//             case 4 =>
//                 val c53 = Module(new C53)
//                 for((x, y) <- c53.io.in.take(4) zip col){
//                     x := y
//                 }
//                 c53.io.in.last := (if(cin.nonEmpty) cin.head else 0.U)
//                 sum = Seq(c53.io.out(0).asBool()) ++ (if(cin.nonEmpty) cin.drop(1) else Nil)
//                 cout1 = Seq(c53.io.out(1).asBool())
//                 cout2 = Seq(c53.io.out(2).asBool())
//             case n =>
//                 val cin_1 = if(cin.nonEmpty) Seq(cin.head) else Nil
//                 val cin_2 = if(cin.nonEmpty) cin.drop(1) else Nil
//                 val (s_1, c_1_1, c_1_2) = addOneColumn(col take 4, cin_1)
//                 val (s_2, c_2_1, c_2_2) = addOneColumn(col drop 4, cin_2)
//                 sum = s_1 ++ s_2
//                 cout1 = c_1_1 ++ c_2_1
//                 cout2 = c_1_2 ++ c_2_2
//         }
//         (sum, cout1, cout2)
//     }

//     def addAll(cols: Array[Seq[Bool]], depth: Int):(UInt, UInt) = {
//         if (max(cols.map(_.size)) <= 2){
//             val sum = Cat(cols.map(_(0)).reverse)
//             var k = 0
//             while(cols(k).size == 1) k = k+1
//             val carry = Cat(cols.drop(k).map(_(1)).reverse)
//             (sum, Cat(carry, 0.U(k.W)))
//         } else {
//             val columns_next = Array.fill(2*len)(Seq[Bool]())
//             var cout1, cout2 = Seq[Bool]()
//             for( i <- cols.indices){
//             val (s, c1, c2) = addOneColumn(cols(i), cout1)
//             columns_next(i) = s ++ cout2
//             cout1 = c1
//             cout2 = c2
//         }

//             val needReg = depth == 4
//             val toNextLayer = if(needReg)
//             columns_next.map(_.map(
//                 x => RegEnable(x, io.regEnables(1))))
//             else
//             columns_next

//             addAll(toNextLayer, depth+1)
//         }
//         }

//         val columns_reg = columns.map(col => col.map(
//             b => RegEnable(b, io.regEnables(0))))
//         val (sum, carry) = addAll(columns_reg, 0)

//         io.result := sum + carry
// }



class exp_adder_input extends Bundle {   
    val frac    = Vec(datain_bandwidth, UInt(frac_bitwidth.W))
}
class adder_div_output extends Bundle { 
    val sum  = UInt((frac_bitwidth*2).W)
    val frac = Vec(datain_bandwidth, UInt(frac_bitwidth.W))
}
class adder_tree (val bitwidth: Int, val numElements: Int) extends Module {
    val io = IO(new Bundle {
        val expu_addu_i  = Flipped(Decoupled(new exp_adder_input))
        val addu_divu_o  = Decoupled(new adder_div_output)
    })

    val is_end         = RegInit(false.B)
    // ======================= FSM ==========================
    val state       = WireInit(sIdle)
    val expu_addu_i_hs   = io.expu_addu_i.ready && io.expu_addu_i.valid
    val addu_divu_o_hs   = io.addu_divu_o.ready && io.addu_divu_o.valid
    state := fsm(expu_addu_i_hs, is_end, addu_divu_o_hs)
    // ======================================================
    io.expu_addu_i.ready := state === sIdle

    val frac_vec = RegInit(VecInit(Seq.fill(datain_bandwidth)(0.U((frac_bitwidth).W))))
    frac_vec := Mux(expu_addu_i_hs, io.expu_addu_i.bits.frac, frac_vec)
    
    val sum         = RegInit(0.U((frac_bitwidth*2).W))

    when (state === sRun) {
        sum                      := frac_vec(0) + frac_vec(1) + frac_vec(2) + frac_vec(3) + frac_vec(4) + frac_vec(5) + frac_vec(6) + frac_vec(7)   
        is_end                   := true.B
        io.addu_divu_o.valid     := false.B 
        io.addu_divu_o.bits.frac := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))
    }.elsewhen (state === sDone) {
        io.addu_divu_o.valid     := true.B 
        io.addu_divu_o.bits.frac := frac_vec
        io.addu_divu_o.bits.sum  := sum
    }.otherwise {
        io.addu_divu_o.valid     := false.B 
        io.addu_divu_o.bits.frac := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))
        io.addu_divu_o.bits.sum  := 0.U
        frac_vec        := VecInit(Seq.fill(datain_bandwidth)(0.U(frac_bitwidth.W)))        
    }

}


