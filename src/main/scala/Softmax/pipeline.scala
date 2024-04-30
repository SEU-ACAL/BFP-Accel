package pipeline

import chisel3._
import chisel3.util._
import chisel3.stage._

import define.MACRO._
import define.FSM._
import define.function._



// class Ctrl_Input extends Bundle {
//     val idex_flush_en  = Input(Bool())
//     val idex_stall_en  = Input(Bool())
// }

// class IDEX_Input extends Bundle {
//     val inst        = Input(UInt(32.W))
//     val pc          = Input(UInt(64.W))
//     val op1         = Input(UInt(64.W))
//     val op2         = Input(UInt(64.W))
//     val rd_addr     = Input(UInt(5.W))
//     val rd_wen      = Input(Bool())
//     val base_addr   = Input(UInt(64.W))
//     val offset_addr = Input(UInt(64.W))
//     val opcode      = Input(UInt(7.W))
//     val func3       = Input(UInt(3.W))
//     val func7       = Input(UInt(7.W))
//     val csr_wen     = Input(Bool())
//     val csr_waddr   = Input(UInt(12.W))
// }

// class maxu_shiftu extends Module {  
//     val id_idex   = IO(new IDEX_Input())
//     val idex_ex   = IO(Flipped(new IDEX_Input()))
//     val ctrl_idex = IO(new Ctrl_Input())

//     idex_ex.inst        := hs_uint_dff(ctrl_idex.idex_flush_en, ctrl_idex.idex_stall_en, 32.U,  INST_NOP,  id_idex.inst       )
//     idex_ex.pc          := hs_uint_dff(ctrl_idex.idex_flush_en, ctrl_idex.idex_stall_en, 64.U,  0.U(64.W), id_idex.pc         )
//     idex_ex.op1         := hs_uint_dff(ctrl_idex.idex_flush_en, ctrl_idex.idex_stall_en, 64.U,  0.U(64.W), id_idex.op1        )
//     idex_ex.op2         := hs_uint_dff(ctrl_idex.idex_flush_en, ctrl_idex.idex_stall_en, 64.U,  0.U(64.W), id_idex.op2        )
// }