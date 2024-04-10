package tb

import chisel3._
import chisel3.util._
import chisel3.stage._

import softmax._
import define.MACRO._



class tb extends Module {
    val softmax = Module(new softmax)
    
}


object tb extends App {
    (new ChiselStage).emitVerilog(new tb, args)
}
