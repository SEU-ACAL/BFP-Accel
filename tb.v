module softmax(
  input        clock,
  input        reset,
  input  [7:0] input_data_in_0,
  input  [7:0] input_data_in_1,
  input  [7:0] input_data_in_2,
  input  [7:0] input_data_in_3,
  input  [7:0] input_data_in_4,
  input  [7:0] input_data_in_5,
  input  [7:0] input_data_in_6,
  input  [7:0] input_data_in_7,
  input  [7:0] input_data_in_8,
  input  [7:0] input_data_in_9,
  input  [7:0] input_data_in_10,
  input  [7:0] input_data_in_11,
  input  [7:0] input_data_in_12,
  input  [7:0] input_data_in_13,
  input  [7:0] input_data_in_14,
  input  [7:0] input_data_in_15,
  output       output_valid,
  output [7:0] output_bits_data_out_0,
  output [7:0] output_bits_data_out_1,
  output [7:0] output_bits_data_out_2,
  output [7:0] output_bits_data_out_3,
  output [7:0] output_bits_data_out_4,
  output [7:0] output_bits_data_out_5,
  output [7:0] output_bits_data_out_6,
  output [7:0] output_bits_data_out_7,
  output [7:0] output_bits_data_out_8,
  output [7:0] output_bits_data_out_9,
  output [7:0] output_bits_data_out_10,
  output [7:0] output_bits_data_out_11,
  output [7:0] output_bits_data_out_12,
  output [7:0] output_bits_data_out_13,
  output [7:0] output_bits_data_out_14,
  output [7:0] output_bits_data_out_15
);
  assign output_valid = 1'h0; // @[softmax.scala 24:18]
  assign output_bits_data_out_0 = 8'h0; // @[softmax.scala 25:52]
  assign output_bits_data_out_1 = 8'h0; // @[softmax.scala 25:52]
  assign output_bits_data_out_2 = 8'h0; // @[softmax.scala 25:52]
  assign output_bits_data_out_3 = 8'h0; // @[softmax.scala 25:52]
  assign output_bits_data_out_4 = 8'h0; // @[softmax.scala 25:52]
  assign output_bits_data_out_5 = 8'h0; // @[softmax.scala 25:52]
  assign output_bits_data_out_6 = 8'h0; // @[softmax.scala 25:52]
  assign output_bits_data_out_7 = 8'h0; // @[softmax.scala 25:52]
  assign output_bits_data_out_8 = 8'h0; // @[softmax.scala 25:52]
  assign output_bits_data_out_9 = 8'h0; // @[softmax.scala 25:52]
  assign output_bits_data_out_10 = 8'h0; // @[softmax.scala 25:52]
  assign output_bits_data_out_11 = 8'h0; // @[softmax.scala 25:52]
  assign output_bits_data_out_12 = 8'h0; // @[softmax.scala 25:52]
  assign output_bits_data_out_13 = 8'h0; // @[softmax.scala 25:52]
  assign output_bits_data_out_14 = 8'h0; // @[softmax.scala 25:52]
  assign output_bits_data_out_15 = 8'h0; // @[softmax.scala 25:52]
endmodule
module tb(
  input   clock,
  input   reset
);
  wire  softmax_clock; // @[tb.scala 12:25]
  wire  softmax_reset; // @[tb.scala 12:25]
  wire [7:0] softmax_input_data_in_0; // @[tb.scala 12:25]
  wire [7:0] softmax_input_data_in_1; // @[tb.scala 12:25]
  wire [7:0] softmax_input_data_in_2; // @[tb.scala 12:25]
  wire [7:0] softmax_input_data_in_3; // @[tb.scala 12:25]
  wire [7:0] softmax_input_data_in_4; // @[tb.scala 12:25]
  wire [7:0] softmax_input_data_in_5; // @[tb.scala 12:25]
  wire [7:0] softmax_input_data_in_6; // @[tb.scala 12:25]
  wire [7:0] softmax_input_data_in_7; // @[tb.scala 12:25]
  wire [7:0] softmax_input_data_in_8; // @[tb.scala 12:25]
  wire [7:0] softmax_input_data_in_9; // @[tb.scala 12:25]
  wire [7:0] softmax_input_data_in_10; // @[tb.scala 12:25]
  wire [7:0] softmax_input_data_in_11; // @[tb.scala 12:25]
  wire [7:0] softmax_input_data_in_12; // @[tb.scala 12:25]
  wire [7:0] softmax_input_data_in_13; // @[tb.scala 12:25]
  wire [7:0] softmax_input_data_in_14; // @[tb.scala 12:25]
  wire [7:0] softmax_input_data_in_15; // @[tb.scala 12:25]
  wire  softmax_output_valid; // @[tb.scala 12:25]
  wire [7:0] softmax_output_bits_data_out_0; // @[tb.scala 12:25]
  wire [7:0] softmax_output_bits_data_out_1; // @[tb.scala 12:25]
  wire [7:0] softmax_output_bits_data_out_2; // @[tb.scala 12:25]
  wire [7:0] softmax_output_bits_data_out_3; // @[tb.scala 12:25]
  wire [7:0] softmax_output_bits_data_out_4; // @[tb.scala 12:25]
  wire [7:0] softmax_output_bits_data_out_5; // @[tb.scala 12:25]
  wire [7:0] softmax_output_bits_data_out_6; // @[tb.scala 12:25]
  wire [7:0] softmax_output_bits_data_out_7; // @[tb.scala 12:25]
  wire [7:0] softmax_output_bits_data_out_8; // @[tb.scala 12:25]
  wire [7:0] softmax_output_bits_data_out_9; // @[tb.scala 12:25]
  wire [7:0] softmax_output_bits_data_out_10; // @[tb.scala 12:25]
  wire [7:0] softmax_output_bits_data_out_11; // @[tb.scala 12:25]
  wire [7:0] softmax_output_bits_data_out_12; // @[tb.scala 12:25]
  wire [7:0] softmax_output_bits_data_out_13; // @[tb.scala 12:25]
  wire [7:0] softmax_output_bits_data_out_14; // @[tb.scala 12:25]
  wire [7:0] softmax_output_bits_data_out_15; // @[tb.scala 12:25]
  softmax softmax ( // @[tb.scala 12:25]
    .clock(softmax_clock),
    .reset(softmax_reset),
    .input_data_in_0(softmax_input_data_in_0),
    .input_data_in_1(softmax_input_data_in_1),
    .input_data_in_2(softmax_input_data_in_2),
    .input_data_in_3(softmax_input_data_in_3),
    .input_data_in_4(softmax_input_data_in_4),
    .input_data_in_5(softmax_input_data_in_5),
    .input_data_in_6(softmax_input_data_in_6),
    .input_data_in_7(softmax_input_data_in_7),
    .input_data_in_8(softmax_input_data_in_8),
    .input_data_in_9(softmax_input_data_in_9),
    .input_data_in_10(softmax_input_data_in_10),
    .input_data_in_11(softmax_input_data_in_11),
    .input_data_in_12(softmax_input_data_in_12),
    .input_data_in_13(softmax_input_data_in_13),
    .input_data_in_14(softmax_input_data_in_14),
    .input_data_in_15(softmax_input_data_in_15),
    .output_valid(softmax_output_valid),
    .output_bits_data_out_0(softmax_output_bits_data_out_0),
    .output_bits_data_out_1(softmax_output_bits_data_out_1),
    .output_bits_data_out_2(softmax_output_bits_data_out_2),
    .output_bits_data_out_3(softmax_output_bits_data_out_3),
    .output_bits_data_out_4(softmax_output_bits_data_out_4),
    .output_bits_data_out_5(softmax_output_bits_data_out_5),
    .output_bits_data_out_6(softmax_output_bits_data_out_6),
    .output_bits_data_out_7(softmax_output_bits_data_out_7),
    .output_bits_data_out_8(softmax_output_bits_data_out_8),
    .output_bits_data_out_9(softmax_output_bits_data_out_9),
    .output_bits_data_out_10(softmax_output_bits_data_out_10),
    .output_bits_data_out_11(softmax_output_bits_data_out_11),
    .output_bits_data_out_12(softmax_output_bits_data_out_12),
    .output_bits_data_out_13(softmax_output_bits_data_out_13),
    .output_bits_data_out_14(softmax_output_bits_data_out_14),
    .output_bits_data_out_15(softmax_output_bits_data_out_15)
  );
  assign softmax_clock = clock;
  assign softmax_reset = reset;
  assign softmax_input_data_in_0 = 8'h0; // @[tb.scala 14:54]
  assign softmax_input_data_in_1 = 8'h0; // @[tb.scala 14:54]
  assign softmax_input_data_in_2 = 8'h0; // @[tb.scala 14:54]
  assign softmax_input_data_in_3 = 8'h0; // @[tb.scala 14:54]
  assign softmax_input_data_in_4 = 8'h0; // @[tb.scala 14:54]
  assign softmax_input_data_in_5 = 8'h0; // @[tb.scala 14:54]
  assign softmax_input_data_in_6 = 8'h0; // @[tb.scala 14:54]
  assign softmax_input_data_in_7 = 8'h0; // @[tb.scala 14:54]
  assign softmax_input_data_in_8 = 8'h0; // @[tb.scala 14:54]
  assign softmax_input_data_in_9 = 8'h0; // @[tb.scala 14:54]
  assign softmax_input_data_in_10 = 8'h0; // @[tb.scala 14:54]
  assign softmax_input_data_in_11 = 8'h0; // @[tb.scala 14:54]
  assign softmax_input_data_in_12 = 8'h0; // @[tb.scala 14:54]
  assign softmax_input_data_in_13 = 8'h0; // @[tb.scala 14:54]
  assign softmax_input_data_in_14 = 8'h0; // @[tb.scala 14:54]
  assign softmax_input_data_in_15 = 8'h0; // @[tb.scala 14:54]
endmodule
