package DPIC

import chisel3._
import chisel3.util._

import define.MACRO._



class softmax_input_fp16_line extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle{
        val en        = Input(UInt(1.W)) 
        val line_num  = Input(UInt(log2datain_line_num.W)) 
        val line_data = Output(Vec(datain_bandwidth, UInt(bitwidth.W)))
    })
    setInline("softmax_input_fp16_line.v",
    """
    |import "DPI-C" function void softmax_read_FP16_matrix(input bit en, input int line_num, output shortint line_data[5]);
    |module softmax_input_fp16_line (
    |   input        en,
    |   input  [3:0] line_num,
    |   output [15:0] line_data_0,
    |   output [15:0] line_data_1,
    |   output [15:0] line_data_2,
    |   output [15:0] line_data_3,
    |   output [15:0] line_data_4
    |);
    |   shortint line_data[5];
    |
    |   assign line_data_0  = line_data[ 0];
    |   assign line_data_1  = line_data[ 1];
    |   assign line_data_2  = line_data[ 2];
    |   assign line_data_3  = line_data[ 3];
    |   assign line_data_4  = line_data[ 4];
    |
    |   always @(*) begin
    |       softmax_read_FP16_matrix(en, line_num, line_data); 
    |   end
    |
    |endmodule
    """.stripMargin)
}


class softmax_input_line extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle{
        val en        = Input(UInt(1.W)) 
        val line_num  = Input(UInt(log2datain_line_num.W)) 
        val line_data = Output(Vec(datain_bandwidth, SInt(bitwidth.W)))
    })
    setInline("softmax_input_line.v",
    """
    |import "DPI-C" function void softmax_read_matrix(input bit en, input int line_num, output byte line_data[16]);
    |module softmax_input_line (
    |   input        en,
    |   input  [3:0] line_num,
    |   output [7:0] line_data_0,
    |   output [7:0] line_data_1,
    |   output [7:0] line_data_2,
    |   output [7:0] line_data_3,
    |   output [7:0] line_data_4,
    |   output [7:0] line_data_5,
    |   output [7:0] line_data_6,
    |   output [7:0] line_data_7,
    |   output [7:0] line_data_8,
    |   output [7:0] line_data_9,
    |   output [7:0] line_data_10,
    |   output [7:0] line_data_11,
    |   output [7:0] line_data_12,
    |   output [7:0] line_data_13,
    |   output [7:0] line_data_14,
    |   output [7:0] line_data_15
    |);
    |   byte line_data[16];
    |
    |   assign line_data_0  = line_data[ 0];
    |   assign line_data_1  = line_data[ 1];
    |   assign line_data_2  = line_data[ 2];
    |   assign line_data_3  = line_data[ 3];
    |   assign line_data_4  = line_data[ 4];
    |   assign line_data_5  = line_data[ 5];
    |   assign line_data_6  = line_data[ 6];
    |   assign line_data_7  = line_data[ 7];
    |   assign line_data_8  = line_data[ 8];
    |   assign line_data_9  = line_data[ 9];
    |   assign line_data_10 = line_data[10];
    |   assign line_data_11 = line_data[11];
    |   assign line_data_12 = line_data[12];
    |   assign line_data_13 = line_data[13];
    |   assign line_data_14 = line_data[14];
    |   assign line_data_15 = line_data[15];
    |
    |   always @(*) begin
    |       softmax_read_matrix(en, line_num, line_data); 
    |   end
    |
    |endmodule
    """.stripMargin)
}

class softmax_output_trace extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle{
        val en        = Input(UInt(1.W)) 
        val line_num  = Input(UInt(log2datain_line_num.W)) 
        val line_data = Input(Vec(datain_bandwidth, UInt(bitwidth.W)))
    })
    setInline("softmax_output_trace.v",
    """
    |import "DPI-C" function void softmax_output_trace(input bit en, input int line_num, input byte line_data[16]);
    |module softmax_output_trace (
    |   input        en,
    |   input  [3:0] line_num,
    |   input  [7:0] line_data_0,
    |   input  [7:0] line_data_1,
    |   input  [7:0] line_data_2,
    |   input  [7:0] line_data_3,
    |   input  [7:0] line_data_4,
    |   input  [7:0] line_data_5,
    |   input  [7:0] line_data_6,
    |   input  [7:0] line_data_7,
    |   input  [7:0] line_data_8,
    |   input  [7:0] line_data_9,
    |   input  [7:0] line_data_10,
    |   input  [7:0] line_data_11,
    |   input  [7:0] line_data_12,
    |   input  [7:0] line_data_13,
    |   input  [7:0] line_data_14,
    |   input  [7:0] line_data_15
    |);
    |   byte line_data[16];
    |
    |   assign line_data[ 0] = line_data_0 ;
    |   assign line_data[ 1] = line_data_1 ;
    |   assign line_data[ 2] = line_data_2 ;
    |   assign line_data[ 3] = line_data_3 ;
    |   assign line_data[ 4] = line_data_4 ;
    |   assign line_data[ 5] = line_data_5 ;
    |   assign line_data[ 6] = line_data_6 ;
    |   assign line_data[ 7] = line_data_7 ;
    |   assign line_data[ 8] = line_data_8 ;
    |   assign line_data[ 9] = line_data_9 ;
    |   assign line_data[10] = line_data_10;
    |   assign line_data[11] = line_data_11;
    |   assign line_data[12] = line_data_12;
    |   assign line_data[13] = line_data_13;
    |   assign line_data[14] = line_data_14;
    |   assign line_data[15] = line_data_15;
    |
    |   always @(*) begin
    |       softmax_output_trace(en, line_num, line_data); 
    |   end
    |
    |endmodule
    """.stripMargin)
}
