package define

import chisel3._
import chisel3.util._
import chisel3.stage._


object MACRO {
    val datain_bandwidth    = 16
    val datain_line_num     = 16
    val log2datain_line_num = log2Up(datain_line_num)
    val dataout_bandwidth   = 16
    val bitwidth            = 8
    val log2bitwidth        = log2Up(bitwidth)
    val numElements         = datain_bandwidth
}