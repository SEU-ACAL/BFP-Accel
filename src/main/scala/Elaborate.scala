// import circt.stage._
import chisel3._
import _root_.circt.stage.ChiselStage

import softmax._ // replace with your top module

// object Elaborate extends App {
//     def top = new softmax_top()
//     val useMFC = false // true = use MLIR-based firrtl compiler
//     // val useMFC = true // true = use MLIR-based firrtl compiler
//     val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
//     if (useMFC) {
//     	(new ChiselStage).execute(args, generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
//     } else {
//       	(new chisel3.stage.ChiselStage).execute(args, generator)
//     }
// }


object Elaborate extends App {
    ChiselStage.emitSystemVerilogFile(
        new softmax_top, // replace with your top module
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
}