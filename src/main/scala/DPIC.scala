package DPIC

import chisel3._
import chisel3.util._

import define.MACRO._


class softmax_input_line extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle{
        val line_num  = Input(UInt(log2datain_line_num.W)) 
        val line_data = Output(Vec(datain_bandwidth, UInt(bitwidth.W)))
    })
    setInline("softmax_input_line.v",
    """
    |import "DPI-C" function void softmax_read_matrix(input int line_num, output byte line_data[16]);
    |module softmax_input_line (
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
    |       softmax_read_matrix(line_num, line_data); 
    |   end
    |
    |endmodule
    """.stripMargin)
}
