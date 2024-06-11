// package experiment

// import chisel3._
// import chisel3.util._
// import chisel3.stage._

// import define.MACRO._
// import define.FSM._
// import define.function._




// class exp_div extends Bundle { 
//     val sum       = UInt(sum_bitwidth.W)
// }

// class expu_divu extends Module {  
//     val expu_expdiv_i  = IO(Flipped(Decoupled(new exp_div)))
//     val expdiv_divu_o  = IO(Decoupled(new exp_div))
    
//     expdiv_divu_o.valid := expu_expdiv_i.valid 
//     expu_expdiv_i.ready := expdiv_divu_o.ready 

//     val expu_divu_hs = expdiv_divu_o.valid & expu_expdiv_i.ready

//     expdiv_divu_o.bits.sum   := hs_uint_dff(expu_divu_hs, sum_bitwidth.U,            0.U,                                                       expu_expdiv_i.bits.sum   )
// }

// object expu_divu extends App {
//   (new chisel3.stage.ChiselStage).emitVerilog(new expu_divu())
// }