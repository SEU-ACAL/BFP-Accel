//~ `New testbench
`timescale  1ns / 1ps

module tb_tb();
// tb Parameters
parameter PERIOD  = 10;




// tb Inputs
// softmax_top Inputs
reg   clock                                   ;
reg   reset                                   ;
reg   data_in_stage1_valid                    ;
reg   [15:0]  data_in_stage1_bits_raw_data_0  ;
reg   [15:0]  data_in_stage1_bits_raw_data_1  ;
reg   [15:0]  data_in_stage1_bits_raw_data_2  ;
reg   [15:0]  data_in_stage1_bits_raw_data_3  ;
reg   [15:0]  data_in_stage1_bits_raw_data_4  ;
reg   [15:0]  data_in_stage1_bits_raw_data_5  ;
reg   [15:0]  data_in_stage1_bits_raw_data_6  ;
reg   [15:0]  data_in_stage1_bits_raw_data_7  ;
reg   [15:0]  data_in_stage1_bits_raw_data_8  ;
reg   [15:0]  data_in_stage1_bits_raw_data_9  ;
reg   [15:0]  data_in_stage1_bits_raw_data_10 ;
reg   [15:0]  data_in_stage1_bits_raw_data_11 ;
reg   [15:0]  data_in_stage1_bits_raw_data_12 ;
reg   [15:0]  data_in_stage1_bits_raw_data_13 ;
reg   [15:0]  data_in_stage1_bits_raw_data_14 ;
reg   [15:0]  data_in_stage1_bits_raw_data_15 ;
reg   [15:0]  data_in_stage1_bits_raw_data_16 ;
reg   [15:0]  data_in_stage1_bits_raw_data_17 ;
reg   [15:0]  data_in_stage1_bits_raw_data_18 ;
reg   [15:0]  data_in_stage1_bits_raw_data_19 ;
reg   [15:0]  data_in_stage1_bits_raw_data_20 ;
reg   [15:0]  data_in_stage1_bits_raw_data_21 ;
reg   [15:0]  data_in_stage1_bits_raw_data_22 ;
reg   [15:0]  data_in_stage1_bits_raw_data_23 ;
reg   [15:0]  data_in_stage1_bits_raw_data_24 ;
reg   [15:0]  data_in_stage1_bits_raw_data_25 ;
reg   [15:0]  data_in_stage1_bits_raw_data_26 ;
reg   [15:0]  data_in_stage1_bits_raw_data_27 ;
reg   [15:0]  data_in_stage1_bits_raw_data_28 ;
reg   [15:0]  data_in_stage1_bits_raw_data_29 ;
reg   [15:0]  data_in_stage1_bits_raw_data_30 ;
reg   [15:0]  data_in_stage1_bits_raw_data_31 ;
reg   [15:0]  data_in_stage1_bits_raw_data_32 ;
reg   [15:0]  data_in_stage1_bits_raw_data_33 ;
reg   [15:0]  data_in_stage1_bits_raw_data_34 ;
reg   [15:0]  data_in_stage1_bits_raw_data_35 ;
reg   [15:0]  data_in_stage1_bits_raw_data_36 ;
reg   [15:0]  data_in_stage1_bits_raw_data_37 ;
reg   [15:0]  data_in_stage1_bits_raw_data_38 ;
reg   [15:0]  data_in_stage1_bits_raw_data_39 ;
reg   [15:0]  data_in_stage1_bits_raw_data_40 ;
reg   [15:0]  data_in_stage1_bits_raw_data_41 ;
reg   [15:0]  data_in_stage1_bits_raw_data_42 ;
reg   [15:0]  data_in_stage1_bits_raw_data_43 ;
reg   [15:0]  data_in_stage1_bits_raw_data_44 ;
reg   [15:0]  data_in_stage1_bits_raw_data_45 ;
reg   [15:0]  data_in_stage1_bits_raw_data_46 ;
reg   [15:0]  data_in_stage1_bits_raw_data_47 ;
reg   [15:0]  data_in_stage1_bits_raw_data_48 ;
reg   [15:0]  data_in_stage1_bits_raw_data_49 ;
reg   [15:0]  data_in_stage1_bits_raw_data_50 ;
reg   [15:0]  data_in_stage1_bits_raw_data_51 ;
reg   [15:0]  data_in_stage1_bits_raw_data_52 ;
reg   [15:0]  data_in_stage1_bits_raw_data_53 ;
reg   [15:0]  data_in_stage1_bits_raw_data_54 ;
reg   [15:0]  data_in_stage1_bits_raw_data_55 ;
reg   [15:0]  data_in_stage1_bits_raw_data_56 ;
reg   [15:0]  data_in_stage1_bits_raw_data_57 ;
reg   [15:0]  data_in_stage1_bits_raw_data_58 ;
reg   [15:0]  data_in_stage1_bits_raw_data_59 ;
reg   [15:0]  data_in_stage1_bits_raw_data_60 ;
reg   [15:0]  data_in_stage1_bits_raw_data_61 ;
reg   [15:0]  data_in_stage1_bits_raw_data_62 ;
reg   [15:0]  data_in_stage1_bits_raw_data_63 ;
reg   [4:0]  data_in_stage1_bits_batch_num    ;
reg   data_in_stage2_valid                    ;
reg   [15:0]  data_in_stage2_bits_data_in_0   ;
reg   [15:0]  data_in_stage2_bits_data_in_1   ;
reg   [15:0]  data_in_stage2_bits_data_in_2   ;
reg   [15:0]  data_in_stage2_bits_data_in_3   ;
reg   [15:0]  data_in_stage2_bits_data_in_4   ;
reg   [15:0]  data_in_stage2_bits_data_in_5   ;
reg   [15:0]  data_in_stage2_bits_data_in_6   ;
reg   [15:0]  data_in_stage2_bits_data_in_7   ;
reg   [15:0]  data_in_stage2_bits_data_in_8   ;
reg   [15:0]  data_in_stage2_bits_data_in_9   ;
reg   [15:0]  data_in_stage2_bits_data_in_10  ;
reg   [15:0]  data_in_stage2_bits_data_in_11  ;
reg   [15:0]  data_in_stage2_bits_data_in_12  ;
reg   [15:0]  data_in_stage2_bits_data_in_13  ;
reg   [15:0]  data_in_stage2_bits_data_in_14  ;
reg   [15:0]  data_in_stage2_bits_data_in_15  ;
reg   [15:0]  data_in_stage2_bits_data_in_16  ;
reg   [15:0]  data_in_stage2_bits_data_in_17  ;
reg   [15:0]  data_in_stage2_bits_data_in_18  ;
reg   [15:0]  data_in_stage2_bits_data_in_19  ;
reg   [15:0]  data_in_stage2_bits_data_in_20  ;
reg   [15:0]  data_in_stage2_bits_data_in_21  ;
reg   [15:0]  data_in_stage2_bits_data_in_22  ;
reg   [15:0]  data_in_stage2_bits_data_in_23  ;
reg   [15:0]  data_in_stage2_bits_data_in_24  ;
reg   [15:0]  data_in_stage2_bits_data_in_25  ;
reg   [15:0]  data_in_stage2_bits_data_in_26  ;
reg   [15:0]  data_in_stage2_bits_data_in_27  ;
reg   [15:0]  data_in_stage2_bits_data_in_28  ;
reg   [15:0]  data_in_stage2_bits_data_in_29  ;
reg   [15:0]  data_in_stage2_bits_data_in_30  ;
reg   [15:0]  data_in_stage2_bits_data_in_31  ;
reg   [15:0]  data_in_stage2_bits_data_in_32  ;
reg   [15:0]  data_in_stage2_bits_data_in_33  ;
reg   [15:0]  data_in_stage2_bits_data_in_34  ;
reg   [15:0]  data_in_stage2_bits_data_in_35  ;
reg   [15:0]  data_in_stage2_bits_data_in_36  ;
reg   [15:0]  data_in_stage2_bits_data_in_37  ;
reg   [15:0]  data_in_stage2_bits_data_in_38  ;
reg   [15:0]  data_in_stage2_bits_data_in_39  ;
reg   [15:0]  data_in_stage2_bits_data_in_40  ;
reg   [15:0]  data_in_stage2_bits_data_in_41  ;
reg   [15:0]  data_in_stage2_bits_data_in_42  ;
reg   [15:0]  data_in_stage2_bits_data_in_43  ;
reg   [15:0]  data_in_stage2_bits_data_in_44  ;
reg   [15:0]  data_in_stage2_bits_data_in_45  ;
reg   [15:0]  data_in_stage2_bits_data_in_46  ;
reg   [15:0]  data_in_stage2_bits_data_in_47  ;
reg   [15:0]  data_in_stage2_bits_data_in_48  ;
reg   [15:0]  data_in_stage2_bits_data_in_49  ;
reg   [15:0]  data_in_stage2_bits_data_in_50  ;
reg   [15:0]  data_in_stage2_bits_data_in_51  ;
reg   [15:0]  data_in_stage2_bits_data_in_52  ;
reg   [15:0]  data_in_stage2_bits_data_in_53  ;
reg   [15:0]  data_in_stage2_bits_data_in_54  ;
reg   [15:0]  data_in_stage2_bits_data_in_55  ;
reg   [15:0]  data_in_stage2_bits_data_in_56  ;
reg   [15:0]  data_in_stage2_bits_data_in_57  ;
reg   [15:0]  data_in_stage2_bits_data_in_58  ;
reg   [15:0]  data_in_stage2_bits_data_in_59  ;
reg   [15:0]  data_in_stage2_bits_data_in_60  ;
reg   [15:0]  data_in_stage2_bits_data_in_61  ;
reg   [15:0]  data_in_stage2_bits_data_in_62  ;
reg   [15:0]  data_in_stage2_bits_data_in_63  ;
reg   [3:0]  data_in_stage2_bits_batch_num    ;
reg   data_in_stage3_valid                    ;
reg   [4:0]  data_in_stage3_bits_data_exp     ;
reg   [9:0]  data_in_stage3_bits_data_frac_0  ;
reg   [9:0]  data_in_stage3_bits_data_frac_1  ;
reg   [9:0]  data_in_stage3_bits_data_frac_2  ;
reg   [9:0]  data_in_stage3_bits_data_frac_3  ;
reg   [9:0]  data_in_stage3_bits_data_frac_4  ;
reg   [9:0]  data_in_stage3_bits_data_frac_5  ;
reg   [9:0]  data_in_stage3_bits_data_frac_6  ;
reg   [9:0]  data_in_stage3_bits_data_frac_7  ;
reg   [9:0]  data_in_stage3_bits_data_frac_8  ;
reg   [9:0]  data_in_stage3_bits_data_frac_9  ;
reg   [9:0]  data_in_stage3_bits_data_frac_10 ;
reg   [9:0]  data_in_stage3_bits_data_frac_11 ;
reg   [9:0]  data_in_stage3_bits_data_frac_12 ;
reg   [9:0]  data_in_stage3_bits_data_frac_13 ;
reg   [9:0]  data_in_stage3_bits_data_frac_14 ;
reg   [9:0]  data_in_stage3_bits_data_frac_15 ;
reg   [9:0]  data_in_stage3_bits_data_frac_16 ;
reg   [9:0]  data_in_stage3_bits_data_frac_17 ;
reg   [9:0]  data_in_stage3_bits_data_frac_18 ;
reg   [9:0]  data_in_stage3_bits_data_frac_19 ;
reg   [9:0]  data_in_stage3_bits_data_frac_20 ;
reg   [9:0]  data_in_stage3_bits_data_frac_21 ;
reg   [9:0]  data_in_stage3_bits_data_frac_22 ;
reg   [9:0]  data_in_stage3_bits_data_frac_23 ;
reg   [9:0]  data_in_stage3_bits_data_frac_24 ;
reg   [9:0]  data_in_stage3_bits_data_frac_25 ;
reg   [9:0]  data_in_stage3_bits_data_frac_26 ;
reg   [9:0]  data_in_stage3_bits_data_frac_27 ;
reg   [9:0]  data_in_stage3_bits_data_frac_28 ;
reg   [9:0]  data_in_stage3_bits_data_frac_29 ;
reg   [9:0]  data_in_stage3_bits_data_frac_30 ;
reg   [9:0]  data_in_stage3_bits_data_frac_31 ;
reg   [9:0]  data_in_stage3_bits_data_frac_32 ;
reg   [9:0]  data_in_stage3_bits_data_frac_33 ;
reg   [9:0]  data_in_stage3_bits_data_frac_34 ;
reg   [9:0]  data_in_stage3_bits_data_frac_35 ;
reg   [9:0]  data_in_stage3_bits_data_frac_36 ;
reg   [9:0]  data_in_stage3_bits_data_frac_37 ;
reg   [9:0]  data_in_stage3_bits_data_frac_38 ;
reg   [9:0]  data_in_stage3_bits_data_frac_39 ;
reg   [9:0]  data_in_stage3_bits_data_frac_40 ;
reg   [9:0]  data_in_stage3_bits_data_frac_41 ;
reg   [9:0]  data_in_stage3_bits_data_frac_42 ;
reg   [9:0]  data_in_stage3_bits_data_frac_43 ;
reg   [9:0]  data_in_stage3_bits_data_frac_44 ;
reg   [9:0]  data_in_stage3_bits_data_frac_45 ;
reg   [9:0]  data_in_stage3_bits_data_frac_46 ;
reg   [9:0]  data_in_stage3_bits_data_frac_47 ;
reg   [9:0]  data_in_stage3_bits_data_frac_48 ;
reg   [9:0]  data_in_stage3_bits_data_frac_49 ;
reg   [9:0]  data_in_stage3_bits_data_frac_50 ;
reg   [9:0]  data_in_stage3_bits_data_frac_51 ;
reg   [9:0]  data_in_stage3_bits_data_frac_52 ;
reg   [9:0]  data_in_stage3_bits_data_frac_53 ;
reg   [9:0]  data_in_stage3_bits_data_frac_54 ;
reg   [9:0]  data_in_stage3_bits_data_frac_55 ;
reg   [9:0]  data_in_stage3_bits_data_frac_56 ;
reg   [9:0]  data_in_stage3_bits_data_frac_57 ;
reg   [9:0]  data_in_stage3_bits_data_frac_58 ;
reg   [9:0]  data_in_stage3_bits_data_frac_59 ;
reg   [9:0]  data_in_stage3_bits_data_frac_60 ;
reg   [9:0]  data_in_stage3_bits_data_frac_61 ;
reg   [9:0]  data_in_stage3_bits_data_frac_62 ;
reg   [9:0]  data_in_stage3_bits_data_frac_63 ;





// tb Outputs
wire  data_in_stage1_ready                 ;
wire  data_in_stage2_ready                 ;
wire  data_in_stage3_ready                 ;
wire  data_out_valid                       ;
wire  [15:0]  data_out_bits_data_o_0       ;
wire  [15:0]  data_out_bits_data_o_1       ;
wire  [15:0]  data_out_bits_data_o_2       ;
wire  [15:0]  data_out_bits_data_o_3       ;
wire  [15:0]  data_out_bits_data_o_4       ;
wire  [15:0]  data_out_bits_data_o_5       ;
wire  [15:0]  data_out_bits_data_o_6       ;
wire  [15:0]  data_out_bits_data_o_7       ;
wire  [15:0]  data_out_bits_data_o_8       ;
wire  [15:0]  data_out_bits_data_o_9       ;
wire  [15:0]  data_out_bits_data_o_10      ;
wire  [15:0]  data_out_bits_data_o_11      ;
wire  [15:0]  data_out_bits_data_o_12      ;
wire  [15:0]  data_out_bits_data_o_13      ;
wire  [15:0]  data_out_bits_data_o_14      ;
wire  [15:0]  data_out_bits_data_o_15      ;
wire  [15:0]  data_out_bits_data_o_16      ;
wire  [15:0]  data_out_bits_data_o_17      ;
wire  [15:0]  data_out_bits_data_o_18      ;
wire  [15:0]  data_out_bits_data_o_19      ;
wire  [15:0]  data_out_bits_data_o_20      ;
wire  [15:0]  data_out_bits_data_o_21      ;
wire  [15:0]  data_out_bits_data_o_22      ;
wire  [15:0]  data_out_bits_data_o_23      ;
wire  [15:0]  data_out_bits_data_o_24      ;
wire  [15:0]  data_out_bits_data_o_25      ;
wire  [15:0]  data_out_bits_data_o_26      ;
wire  [15:0]  data_out_bits_data_o_27      ;
wire  [15:0]  data_out_bits_data_o_28      ;
wire  [15:0]  data_out_bits_data_o_29      ;
wire  [15:0]  data_out_bits_data_o_30      ;
wire  [15:0]  data_out_bits_data_o_31      ;
wire  [15:0]  data_out_bits_data_o_32      ;
wire  [15:0]  data_out_bits_data_o_33      ;
wire  [15:0]  data_out_bits_data_o_34      ;
wire  [15:0]  data_out_bits_data_o_35      ;
wire  [15:0]  data_out_bits_data_o_36      ;
wire  [15:0]  data_out_bits_data_o_37      ;
wire  [15:0]  data_out_bits_data_o_38      ;
wire  [15:0]  data_out_bits_data_o_39      ;
wire  [15:0]  data_out_bits_data_o_40      ;
wire  [15:0]  data_out_bits_data_o_41      ;
wire  [15:0]  data_out_bits_data_o_42      ;
wire  [15:0]  data_out_bits_data_o_43      ;
wire  [15:0]  data_out_bits_data_o_44      ;
wire  [15:0]  data_out_bits_data_o_45      ;
wire  [15:0]  data_out_bits_data_o_46      ;
wire  [15:0]  data_out_bits_data_o_47      ;
wire  [15:0]  data_out_bits_data_o_48      ;
wire  [15:0]  data_out_bits_data_o_49      ;
wire  [15:0]  data_out_bits_data_o_50      ;
wire  [15:0]  data_out_bits_data_o_51      ;
wire  [15:0]  data_out_bits_data_o_52      ;
wire  [15:0]  data_out_bits_data_o_53      ;
wire  [15:0]  data_out_bits_data_o_54      ;
wire  [15:0]  data_out_bits_data_o_55      ;
wire  [15:0]  data_out_bits_data_o_56      ;
wire  [15:0]  data_out_bits_data_o_57      ;
wire  [15:0]  data_out_bits_data_o_58      ;
wire  [15:0]  data_out_bits_data_o_59      ;
wire  [15:0]  data_out_bits_data_o_60      ;
wire  [15:0]  data_out_bits_data_o_61      ;
wire  [15:0]  data_out_bits_data_o_62      ;
wire  [15:0]  data_out_bits_data_o_63      ;


// initial begin
// clock=1'b1;
// reset=1'b0;
// #100;//延时100ns
// reset=1'b1;//撤销复位

// data_in_stage1_valid = 1'b1;
// data_in_stage2_valid = 1'b1;
// data_in_stage3_valid = 1'b1;
// data_in_stage1_bits_batch_num=16'b0;
// data_in_stage2_bits_batch_num=16'b0;
// end

always @(posedge clock)  begin 
	data_in_stage1_bits_batch_num   <=  data_in_stage1_bits_batch_num + 1'b1;
    if (data_in_stage1_bits_batch_num == 5'd15) begin
        data_in_stage1_bits_batch_num <= data_in_stage1_bits_batch_num;
    end
end

always @(posedge clock)  begin 
	data_in_stage2_bits_batch_num   <=  data_in_stage2_bits_batch_num + 1'b1;
    if (data_in_stage2_bits_batch_num == 5'd15) begin
        data_in_stage2_bits_batch_num <= data_in_stage2_bits_batch_num;
    end
end



initial
begin
    forever #(PERIOD/2)  clock=~clock;
end


softmax_top  u_softmax_top (
    .clock                                                        ( clock                                                               ),
    .reset                                                        ( reset                                                               ),
    .data_in_stage1_valid                                         ( data_in_stage1_valid                                                ),
    .data_in_stage1_bits_raw_data_0                               ( data_in_stage1_bits_raw_data_0                               [15:0] ),
    .data_in_stage1_bits_raw_data_1                               ( data_in_stage1_bits_raw_data_1                               [15:0] ),
    .data_in_stage1_bits_raw_data_2                               ( data_in_stage1_bits_raw_data_2                               [15:0] ),
    .data_in_stage1_bits_raw_data_3                               ( data_in_stage1_bits_raw_data_3                               [15:0] ),
    .data_in_stage1_bits_raw_data_4                               ( data_in_stage1_bits_raw_data_4                               [15:0] ),
    .data_in_stage1_bits_raw_data_5                               ( data_in_stage1_bits_raw_data_5                               [15:0] ),
    .data_in_stage1_bits_raw_data_6                               ( data_in_stage1_bits_raw_data_6                               [15:0] ),
    .data_in_stage1_bits_raw_data_7                               ( data_in_stage1_bits_raw_data_7                               [15:0] ),
    .data_in_stage1_bits_raw_data_8                               ( data_in_stage1_bits_raw_data_8                               [15:0] ),
    .data_in_stage1_bits_raw_data_9                               ( data_in_stage1_bits_raw_data_9                               [15:0] ),
    .data_in_stage1_bits_raw_data_10                              ( data_in_stage1_bits_raw_data_10                              [15:0] ),
    .data_in_stage1_bits_raw_data_11                              ( data_in_stage1_bits_raw_data_11                              [15:0] ),
    .data_in_stage1_bits_raw_data_12                              ( data_in_stage1_bits_raw_data_12                              [15:0] ),
    .data_in_stage1_bits_raw_data_13                              ( data_in_stage1_bits_raw_data_13                              [15:0] ),
    .data_in_stage1_bits_raw_data_14                              ( data_in_stage1_bits_raw_data_14                              [15:0] ),
    .data_in_stage1_bits_raw_data_15                              ( data_in_stage1_bits_raw_data_15                              [15:0] ),
    .data_in_stage1_bits_raw_data_16                              ( data_in_stage1_bits_raw_data_16                              [15:0] ),
    .data_in_stage1_bits_raw_data_17                              ( data_in_stage1_bits_raw_data_17                              [15:0] ),
    .data_in_stage1_bits_raw_data_18                              ( data_in_stage1_bits_raw_data_18                              [15:0] ),
    .data_in_stage1_bits_raw_data_19                              ( data_in_stage1_bits_raw_data_19                              [15:0] ),
    .data_in_stage1_bits_raw_data_20                              ( data_in_stage1_bits_raw_data_20                              [15:0] ),
    .data_in_stage1_bits_raw_data_21                              ( data_in_stage1_bits_raw_data_21                              [15:0] ),
    .data_in_stage1_bits_raw_data_22                              ( data_in_stage1_bits_raw_data_22                              [15:0] ),
    .data_in_stage1_bits_raw_data_23                              ( data_in_stage1_bits_raw_data_23                              [15:0] ),
    .data_in_stage1_bits_raw_data_24                              ( data_in_stage1_bits_raw_data_24                              [15:0] ),
    .data_in_stage1_bits_raw_data_25                              ( data_in_stage1_bits_raw_data_25                              [15:0] ),
    .data_in_stage1_bits_raw_data_26                              ( data_in_stage1_bits_raw_data_26                              [15:0] ),
    .data_in_stage1_bits_raw_data_27                              ( data_in_stage1_bits_raw_data_27                              [15:0] ),
    .data_in_stage1_bits_raw_data_28                              ( data_in_stage1_bits_raw_data_28                              [15:0] ),
    .data_in_stage1_bits_raw_data_29                              ( data_in_stage1_bits_raw_data_29                              [15:0] ),
    .data_in_stage1_bits_raw_data_30                              ( data_in_stage1_bits_raw_data_30                              [15:0] ),
    .data_in_stage1_bits_raw_data_31                              ( data_in_stage1_bits_raw_data_31                              [15:0] ),
    .data_in_stage1_bits_raw_data_32                              ( data_in_stage1_bits_raw_data_32                              [15:0] ),
    .data_in_stage1_bits_raw_data_33                              ( data_in_stage1_bits_raw_data_33                              [15:0] ),
    .data_in_stage1_bits_raw_data_34                              ( data_in_stage1_bits_raw_data_34                              [15:0] ),
    .data_in_stage1_bits_raw_data_35                              ( data_in_stage1_bits_raw_data_35                              [15:0] ),
    .data_in_stage1_bits_raw_data_36                              ( data_in_stage1_bits_raw_data_36                              [15:0] ),
    .data_in_stage1_bits_raw_data_37                              ( data_in_stage1_bits_raw_data_37                              [15:0] ),
    .data_in_stage1_bits_raw_data_38                              ( data_in_stage1_bits_raw_data_38                              [15:0] ),
    .data_in_stage1_bits_raw_data_39                              ( data_in_stage1_bits_raw_data_39                              [15:0] ),
    .data_in_stage1_bits_raw_data_40                              ( data_in_stage1_bits_raw_data_40                              [15:0] ),
    .data_in_stage1_bits_raw_data_41                              ( data_in_stage1_bits_raw_data_41                              [15:0] ),
    .data_in_stage1_bits_raw_data_42                              ( data_in_stage1_bits_raw_data_42                              [15:0] ),
    .data_in_stage1_bits_raw_data_43                              ( data_in_stage1_bits_raw_data_43                              [15:0] ),
    .data_in_stage1_bits_raw_data_44                              ( data_in_stage1_bits_raw_data_44                              [15:0] ),
    .data_in_stage1_bits_raw_data_45                              ( data_in_stage1_bits_raw_data_45                              [15:0] ),
    .data_in_stage1_bits_raw_data_46                              ( data_in_stage1_bits_raw_data_46                              [15:0] ),
    .data_in_stage1_bits_raw_data_47                              ( data_in_stage1_bits_raw_data_47                              [15:0] ),
    .data_in_stage1_bits_raw_data_48                              ( data_in_stage1_bits_raw_data_48                              [15:0] ),
    .data_in_stage1_bits_raw_data_49                              ( data_in_stage1_bits_raw_data_49                              [15:0] ),
    .data_in_stage1_bits_raw_data_50                              ( data_in_stage1_bits_raw_data_50                              [15:0] ),
    .data_in_stage1_bits_raw_data_51                              ( data_in_stage1_bits_raw_data_51                              [15:0] ),
    .data_in_stage1_bits_raw_data_52                              ( data_in_stage1_bits_raw_data_52                              [15:0] ),
    .data_in_stage1_bits_raw_data_53                              ( data_in_stage1_bits_raw_data_53                              [15:0] ),
    .data_in_stage1_bits_raw_data_54                              ( data_in_stage1_bits_raw_data_54                              [15:0] ),
    .data_in_stage1_bits_raw_data_55                              ( data_in_stage1_bits_raw_data_55                              [15:0] ),
    .data_in_stage1_bits_raw_data_56                              ( data_in_stage1_bits_raw_data_56                              [15:0] ),
    .data_in_stage1_bits_raw_data_57                              ( data_in_stage1_bits_raw_data_57                              [15:0] ),
    .data_in_stage1_bits_raw_data_58                              ( data_in_stage1_bits_raw_data_58                              [15:0] ),
    .data_in_stage1_bits_raw_data_59                              ( data_in_stage1_bits_raw_data_59                              [15:0] ),
    .data_in_stage1_bits_raw_data_60                              ( data_in_stage1_bits_raw_data_60                              [15:0] ),
    .data_in_stage1_bits_raw_data_61                              ( data_in_stage1_bits_raw_data_61                              [15:0] ),
    .data_in_stage1_bits_raw_data_62                              ( data_in_stage1_bits_raw_data_62                              [15:0] ),
    .data_in_stage1_bits_raw_data_63                              ( data_in_stage1_bits_raw_data_63                              [15:0] ),
    .data_in_stage1_bits_batch_num                                ( data_in_stage1_bits_batch_num                                [3:0]  ),
    .data_in_stage2_valid                                         ( data_in_stage2_valid                                                ),
    .data_in_stage2_bits_data_in_0                                ( data_in_stage2_bits_data_in_0                                [15:0] ),
    .data_in_stage2_bits_data_in_1                                ( data_in_stage2_bits_data_in_1                                [15:0] ),
    .data_in_stage2_bits_data_in_2                                ( data_in_stage2_bits_data_in_2                                [15:0] ),
    .data_in_stage2_bits_data_in_3                                ( data_in_stage2_bits_data_in_3                                [15:0] ),
    .data_in_stage2_bits_data_in_4                                ( data_in_stage2_bits_data_in_4                                [15:0] ),
    .data_in_stage2_bits_data_in_5                                ( data_in_stage2_bits_data_in_5                                [15:0] ),
    .data_in_stage2_bits_data_in_6                                ( data_in_stage2_bits_data_in_6                                [15:0] ),
    .data_in_stage2_bits_data_in_7                                ( data_in_stage2_bits_data_in_7                                [15:0] ),
    .data_in_stage2_bits_data_in_8                                ( data_in_stage2_bits_data_in_8                                [15:0] ),
    .data_in_stage2_bits_data_in_9                                ( data_in_stage2_bits_data_in_9                                [15:0] ),
    .data_in_stage2_bits_data_in_10                               ( data_in_stage2_bits_data_in_10                               [15:0] ),
    .data_in_stage2_bits_data_in_11                               ( data_in_stage2_bits_data_in_11                               [15:0] ),
    .data_in_stage2_bits_data_in_12                               ( data_in_stage2_bits_data_in_12                               [15:0] ),
    .data_in_stage2_bits_data_in_13                               ( data_in_stage2_bits_data_in_13                               [15:0] ),
    .data_in_stage2_bits_data_in_14                               ( data_in_stage2_bits_data_in_14                               [15:0] ),
    .data_in_stage2_bits_data_in_15                               ( data_in_stage2_bits_data_in_15                               [15:0] ),
    .data_in_stage2_bits_data_in_16                               ( data_in_stage2_bits_data_in_16                               [15:0] ),
    .data_in_stage2_bits_data_in_17                               ( data_in_stage2_bits_data_in_17                               [15:0] ),
    .data_in_stage2_bits_data_in_18                               ( data_in_stage2_bits_data_in_18                               [15:0] ),
    .data_in_stage2_bits_data_in_19                               ( data_in_stage2_bits_data_in_19                               [15:0] ),
    .data_in_stage2_bits_data_in_20                               ( data_in_stage2_bits_data_in_20                               [15:0] ),
    .data_in_stage2_bits_data_in_21                               ( data_in_stage2_bits_data_in_21                               [15:0] ),
    .data_in_stage2_bits_data_in_22                               ( data_in_stage2_bits_data_in_22                               [15:0] ),
    .data_in_stage2_bits_data_in_23                               ( data_in_stage2_bits_data_in_23                               [15:0] ),
    .data_in_stage2_bits_data_in_24                               ( data_in_stage2_bits_data_in_24                               [15:0] ),
    .data_in_stage2_bits_data_in_25                               ( data_in_stage2_bits_data_in_25                               [15:0] ),
    .data_in_stage2_bits_data_in_26                               ( data_in_stage2_bits_data_in_26                               [15:0] ),
    .data_in_stage2_bits_data_in_27                               ( data_in_stage2_bits_data_in_27                               [15:0] ),
    .data_in_stage2_bits_data_in_28                               ( data_in_stage2_bits_data_in_28                               [15:0] ),
    .data_in_stage2_bits_data_in_29                               ( data_in_stage2_bits_data_in_29                               [15:0] ),
    .data_in_stage2_bits_data_in_30                               ( data_in_stage2_bits_data_in_30                               [15:0] ),
    .data_in_stage2_bits_data_in_31                               ( data_in_stage2_bits_data_in_31                               [15:0] ),
    .data_in_stage2_bits_data_in_32                               ( data_in_stage2_bits_data_in_32                               [15:0] ),
    .data_in_stage2_bits_data_in_33                               ( data_in_stage2_bits_data_in_33                               [15:0] ),
    .data_in_stage2_bits_data_in_34                               ( data_in_stage2_bits_data_in_34                               [15:0] ),
    .data_in_stage2_bits_data_in_35                               ( data_in_stage2_bits_data_in_35                               [15:0] ),
    .data_in_stage2_bits_data_in_36                               ( data_in_stage2_bits_data_in_36                               [15:0] ),
    .data_in_stage2_bits_data_in_37                               ( data_in_stage2_bits_data_in_37                               [15:0] ),
    .data_in_stage2_bits_data_in_38                               ( data_in_stage2_bits_data_in_38                               [15:0] ),
    .data_in_stage2_bits_data_in_39                               ( data_in_stage2_bits_data_in_39                               [15:0] ),
    .data_in_stage2_bits_data_in_40                               ( data_in_stage2_bits_data_in_40                               [15:0] ),
    .data_in_stage2_bits_data_in_41                               ( data_in_stage2_bits_data_in_41                               [15:0] ),
    .data_in_stage2_bits_data_in_42                               ( data_in_stage2_bits_data_in_42                               [15:0] ),
    .data_in_stage2_bits_data_in_43                               ( data_in_stage2_bits_data_in_43                               [15:0] ),
    .data_in_stage2_bits_data_in_44                               ( data_in_stage2_bits_data_in_44                               [15:0] ),
    .data_in_stage2_bits_data_in_45                               ( data_in_stage2_bits_data_in_45                               [15:0] ),
    .data_in_stage2_bits_data_in_46                               ( data_in_stage2_bits_data_in_46                               [15:0] ),
    .data_in_stage2_bits_data_in_47                               ( data_in_stage2_bits_data_in_47                               [15:0] ),
    .data_in_stage2_bits_data_in_48                               ( data_in_stage2_bits_data_in_48                               [15:0] ),
    .data_in_stage2_bits_data_in_49                               ( data_in_stage2_bits_data_in_49                               [15:0] ),
    .data_in_stage2_bits_data_in_50                               ( data_in_stage2_bits_data_in_50                               [15:0] ),
    .data_in_stage2_bits_data_in_51                               ( data_in_stage2_bits_data_in_51                               [15:0] ),
    .data_in_stage2_bits_data_in_52                               ( data_in_stage2_bits_data_in_52                               [15:0] ),
    .data_in_stage2_bits_data_in_53                               ( data_in_stage2_bits_data_in_53                               [15:0] ),
    .data_in_stage2_bits_data_in_54                               ( data_in_stage2_bits_data_in_54                               [15:0] ),
    .data_in_stage2_bits_data_in_55                               ( data_in_stage2_bits_data_in_55                               [15:0] ),
    .data_in_stage2_bits_data_in_56                               ( data_in_stage2_bits_data_in_56                               [15:0] ),
    .data_in_stage2_bits_data_in_57                               ( data_in_stage2_bits_data_in_57                               [15:0] ),
    .data_in_stage2_bits_data_in_58                               ( data_in_stage2_bits_data_in_58                               [15:0] ),
    .data_in_stage2_bits_data_in_59                               ( data_in_stage2_bits_data_in_59                               [15:0] ),
    .data_in_stage2_bits_data_in_60                               ( data_in_stage2_bits_data_in_60                               [15:0] ),
    .data_in_stage2_bits_data_in_61                               ( data_in_stage2_bits_data_in_61                               [15:0] ),
    .data_in_stage2_bits_data_in_62                               ( data_in_stage2_bits_data_in_62                               [15:0] ),
    .data_in_stage2_bits_data_in_63                               ( data_in_stage2_bits_data_in_63                               [15:0] ),
    .data_in_stage2_bits_batch_num                                ( data_in_stage2_bits_batch_num                                [3:0]  ),
    .data_in_stage3_valid                                         ( data_in_stage3_valid                                                ),
    .data_in_stage3_bits_data_exp                                 ( data_in_stage3_bits_data_exp                                 [4:0]  ),
    .data_in_stage3_bits_data_frac_0                              ( data_in_stage3_bits_data_frac_0                              [9:0]  ),
    .data_in_stage3_bits_data_frac_1                              ( data_in_stage3_bits_data_frac_1                              [9:0]  ),
    .data_in_stage3_bits_data_frac_2                              ( data_in_stage3_bits_data_frac_2                              [9:0]  ),
    .data_in_stage3_bits_data_frac_3                              ( data_in_stage3_bits_data_frac_3                              [9:0]  ),
    .data_in_stage3_bits_data_frac_4                              ( data_in_stage3_bits_data_frac_4                              [9:0]  ),
    .data_in_stage3_bits_data_frac_5                              ( data_in_stage3_bits_data_frac_5                              [9:0]  ),
    .data_in_stage3_bits_data_frac_6                              ( data_in_stage3_bits_data_frac_6                              [9:0]  ),
    .data_in_stage3_bits_data_frac_7                              ( data_in_stage3_bits_data_frac_7                              [9:0]  ),
    .data_in_stage3_bits_data_frac_8                              ( data_in_stage3_bits_data_frac_8                              [9:0]  ),
    .data_in_stage3_bits_data_frac_9                              ( data_in_stage3_bits_data_frac_9                              [9:0]  ),
    .data_in_stage3_bits_data_frac_10                             ( data_in_stage3_bits_data_frac_10                             [9:0]  ),
    .data_in_stage3_bits_data_frac_11                             ( data_in_stage3_bits_data_frac_11                             [9:0]  ),
    .data_in_stage3_bits_data_frac_12                             ( data_in_stage3_bits_data_frac_12                             [9:0]  ),
    .data_in_stage3_bits_data_frac_13                             ( data_in_stage3_bits_data_frac_13                             [9:0]  ),
    .data_in_stage3_bits_data_frac_14                             ( data_in_stage3_bits_data_frac_14                             [9:0]  ),
    .data_in_stage3_bits_data_frac_15                             ( data_in_stage3_bits_data_frac_15                             [9:0]  ),
    .data_in_stage3_bits_data_frac_16                             ( data_in_stage3_bits_data_frac_16                             [9:0]  ),
    .data_in_stage3_bits_data_frac_17                             ( data_in_stage3_bits_data_frac_17                             [9:0]  ),
    .data_in_stage3_bits_data_frac_18                             ( data_in_stage3_bits_data_frac_18                             [9:0]  ),
    .data_in_stage3_bits_data_frac_19                             ( data_in_stage3_bits_data_frac_19                             [9:0]  ),
    .data_in_stage3_bits_data_frac_20                             ( data_in_stage3_bits_data_frac_20                             [9:0]  ),
    .data_in_stage3_bits_data_frac_21                             ( data_in_stage3_bits_data_frac_21                             [9:0]  ),
    .data_in_stage3_bits_data_frac_22                             ( data_in_stage3_bits_data_frac_22                             [9:0]  ),
    .data_in_stage3_bits_data_frac_23                             ( data_in_stage3_bits_data_frac_23                             [9:0]  ),
    .data_in_stage3_bits_data_frac_24                             ( data_in_stage3_bits_data_frac_24                             [9:0]  ),
    .data_in_stage3_bits_data_frac_25                             ( data_in_stage3_bits_data_frac_25                             [9:0]  ),
    .data_in_stage3_bits_data_frac_26                             ( data_in_stage3_bits_data_frac_26                             [9:0]  ),
    .data_in_stage3_bits_data_frac_27                             ( data_in_stage3_bits_data_frac_27                             [9:0]  ),
    .data_in_stage3_bits_data_frac_28                             ( data_in_stage3_bits_data_frac_28                             [9:0]  ),
    .data_in_stage3_bits_data_frac_29                             ( data_in_stage3_bits_data_frac_29                             [9:0]  ),
    .data_in_stage3_bits_data_frac_30                             ( data_in_stage3_bits_data_frac_30                             [9:0]  ),
    .data_in_stage3_bits_data_frac_31                             ( data_in_stage3_bits_data_frac_31                             [9:0]  ),
    .data_in_stage3_bits_data_frac_32                             ( data_in_stage3_bits_data_frac_32                             [9:0]  ),
    .data_in_stage3_bits_data_frac_33                             ( data_in_stage3_bits_data_frac_33                             [9:0]  ),
    .data_in_stage3_bits_data_frac_34                             ( data_in_stage3_bits_data_frac_34                             [9:0]  ),
    .data_in_stage3_bits_data_frac_35                             ( data_in_stage3_bits_data_frac_35                             [9:0]  ),
    .data_in_stage3_bits_data_frac_36                             ( data_in_stage3_bits_data_frac_36                             [9:0]  ),
    .data_in_stage3_bits_data_frac_37                             ( data_in_stage3_bits_data_frac_37                             [9:0]  ),
    .data_in_stage3_bits_data_frac_38                             ( data_in_stage3_bits_data_frac_38                             [9:0]  ),
    .data_in_stage3_bits_data_frac_39                             ( data_in_stage3_bits_data_frac_39                             [9:0]  ),
    .data_in_stage3_bits_data_frac_40                             ( data_in_stage3_bits_data_frac_40                             [9:0]  ),
    .data_in_stage3_bits_data_frac_41                             ( data_in_stage3_bits_data_frac_41                             [9:0]  ),
    .data_in_stage3_bits_data_frac_42                             ( data_in_stage3_bits_data_frac_42                             [9:0]  ),
    .data_in_stage3_bits_data_frac_43                             ( data_in_stage3_bits_data_frac_43                             [9:0]  ),
    .data_in_stage3_bits_data_frac_44                             ( data_in_stage3_bits_data_frac_44                             [9:0]  ),
    .data_in_stage3_bits_data_frac_45                             ( data_in_stage3_bits_data_frac_45                             [9:0]  ),
    .data_in_stage3_bits_data_frac_46                             ( data_in_stage3_bits_data_frac_46                             [9:0]  ),
    .data_in_stage3_bits_data_frac_47                             ( data_in_stage3_bits_data_frac_47                             [9:0]  ),
    .data_in_stage3_bits_data_frac_48                             ( data_in_stage3_bits_data_frac_48                             [9:0]  ),
    .data_in_stage3_bits_data_frac_49                             ( data_in_stage3_bits_data_frac_49                             [9:0]  ),
    .data_in_stage3_bits_data_frac_50                             ( data_in_stage3_bits_data_frac_50                             [9:0]  ),
    .data_in_stage3_bits_data_frac_51                             ( data_in_stage3_bits_data_frac_51                             [9:0]  ),
    .data_in_stage3_bits_data_frac_52                             ( data_in_stage3_bits_data_frac_52                             [9:0]  ),
    .data_in_stage3_bits_data_frac_53                             ( data_in_stage3_bits_data_frac_53                             [9:0]  ),
    .data_in_stage3_bits_data_frac_54                             ( data_in_stage3_bits_data_frac_54                             [9:0]  ),
    .data_in_stage3_bits_data_frac_55                             ( data_in_stage3_bits_data_frac_55                             [9:0]  ),
    .data_in_stage3_bits_data_frac_56                             ( data_in_stage3_bits_data_frac_56                             [9:0]  ),
    .data_in_stage3_bits_data_frac_57                             ( data_in_stage3_bits_data_frac_57                             [9:0]  ),
    .data_in_stage3_bits_data_frac_58                             ( data_in_stage3_bits_data_frac_58                             [9:0]  ),
    .data_in_stage3_bits_data_frac_59                             ( data_in_stage3_bits_data_frac_59                             [9:0]  ),
    .data_in_stage3_bits_data_frac_60                             ( data_in_stage3_bits_data_frac_60                             [9:0]  ),
    .data_in_stage3_bits_data_frac_61                             ( data_in_stage3_bits_data_frac_61                             [9:0]  ),
    .data_in_stage3_bits_data_frac_62                             ( data_in_stage3_bits_data_frac_62                             [9:0]  ),
    .data_in_stage3_bits_data_frac_63                             ( data_in_stage3_bits_data_frac_63                             [9:0]  ),
    .data_in_stage1_ready                                         ( data_in_stage1_ready                                                ),
    .data_in_stage2_ready                                         ( data_in_stage2_ready                                                ),
    .data_in_stage3_ready                                         ( data_in_stage3_ready                                                ),
    .data_out_valid                                               ( data_out_valid                                                      ),
    .data_out_bits_data_o_0                                       ( data_out_bits_data_o_0                                       [15:0] ),
    .data_out_bits_data_o_1                                       ( data_out_bits_data_o_1                                       [15:0] ),
    .data_out_bits_data_o_2                                       ( data_out_bits_data_o_2                                       [15:0] ),
    .data_out_bits_data_o_3                                       ( data_out_bits_data_o_3                                       [15:0] ),
    .data_out_bits_data_o_4                                       ( data_out_bits_data_o_4                                       [15:0] ),
    .data_out_bits_data_o_5                                       ( data_out_bits_data_o_5                                       [15:0] ),
    .data_out_bits_data_o_6                                       ( data_out_bits_data_o_6                                       [15:0] ),
    .data_out_bits_data_o_7                                       ( data_out_bits_data_o_7                                       [15:0] ),
    .data_out_bits_data_o_8                                       ( data_out_bits_data_o_8                                       [15:0] ),
    .data_out_bits_data_o_9                                       ( data_out_bits_data_o_9                                       [15:0] ),
    .data_out_bits_data_o_10                                      ( data_out_bits_data_o_10                                      [15:0] ),
    .data_out_bits_data_o_11                                      ( data_out_bits_data_o_11                                      [15:0] ),
    .data_out_bits_data_o_12                                      ( data_out_bits_data_o_12                                      [15:0] ),
    .data_out_bits_data_o_13                                      ( data_out_bits_data_o_13                                      [15:0] ),
    .data_out_bits_data_o_14                                      ( data_out_bits_data_o_14                                      [15:0] ),
    .data_out_bits_data_o_15                                      ( data_out_bits_data_o_15                                      [15:0] ),
    .data_out_bits_data_o_16                                      ( data_out_bits_data_o_16                                      [15:0] ),
    .data_out_bits_data_o_17                                      ( data_out_bits_data_o_17                                      [15:0] ),
    .data_out_bits_data_o_18                                      ( data_out_bits_data_o_18                                      [15:0] ),
    .data_out_bits_data_o_19                                      ( data_out_bits_data_o_19                                      [15:0] ),
    .data_out_bits_data_o_20                                      ( data_out_bits_data_o_20                                      [15:0] ),
    .data_out_bits_data_o_21                                      ( data_out_bits_data_o_21                                      [15:0] ),
    .data_out_bits_data_o_22                                      ( data_out_bits_data_o_22                                      [15:0] ),
    .data_out_bits_data_o_23                                      ( data_out_bits_data_o_23                                      [15:0] ),
    .data_out_bits_data_o_24                                      ( data_out_bits_data_o_24                                      [15:0] ),
    .data_out_bits_data_o_25                                      ( data_out_bits_data_o_25                                      [15:0] ),
    .data_out_bits_data_o_26                                      ( data_out_bits_data_o_26                                      [15:0] ),
    .data_out_bits_data_o_27                                      ( data_out_bits_data_o_27                                      [15:0] ),
    .data_out_bits_data_o_28                                      ( data_out_bits_data_o_28                                      [15:0] ),
    .data_out_bits_data_o_29                                      ( data_out_bits_data_o_29                                      [15:0] ),
    .data_out_bits_data_o_30                                      ( data_out_bits_data_o_30                                      [15:0] ),
    .data_out_bits_data_o_31                                      ( data_out_bits_data_o_31                                      [15:0] ),
    .data_out_bits_data_o_32                                      ( data_out_bits_data_o_32                                      [15:0] ),
    .data_out_bits_data_o_33                                      ( data_out_bits_data_o_33                                      [15:0] ),
    .data_out_bits_data_o_34                                      ( data_out_bits_data_o_34                                      [15:0] ),
    .data_out_bits_data_o_35                                      ( data_out_bits_data_o_35                                      [15:0] ),
    .data_out_bits_data_o_36                                      ( data_out_bits_data_o_36                                      [15:0] ),
    .data_out_bits_data_o_37                                      ( data_out_bits_data_o_37                                      [15:0] ),
    .data_out_bits_data_o_38                                      ( data_out_bits_data_o_38                                      [15:0] ),
    .data_out_bits_data_o_39                                      ( data_out_bits_data_o_39                                      [15:0] ),
    .data_out_bits_data_o_40                                      ( data_out_bits_data_o_40                                      [15:0] ),
    .data_out_bits_data_o_41                                      ( data_out_bits_data_o_41                                      [15:0] ),
    .data_out_bits_data_o_42                                      ( data_out_bits_data_o_42                                      [15:0] ),
    .data_out_bits_data_o_43                                      ( data_out_bits_data_o_43                                      [15:0] ),
    .data_out_bits_data_o_44                                      ( data_out_bits_data_o_44                                      [15:0] ),
    .data_out_bits_data_o_45                                      ( data_out_bits_data_o_45                                      [15:0] ),
    .data_out_bits_data_o_46                                      ( data_out_bits_data_o_46                                      [15:0] ),
    .data_out_bits_data_o_47                                      ( data_out_bits_data_o_47                                      [15:0] ),
    .data_out_bits_data_o_48                                      ( data_out_bits_data_o_48                                      [15:0] ),
    .data_out_bits_data_o_49                                      ( data_out_bits_data_o_49                                      [15:0] ),
    .data_out_bits_data_o_50                                      ( data_out_bits_data_o_50                                      [15:0] ),
    .data_out_bits_data_o_51                                      ( data_out_bits_data_o_51                                      [15:0] ),
    .data_out_bits_data_o_52                                      ( data_out_bits_data_o_52                                      [15:0] ),
    .data_out_bits_data_o_53                                      ( data_out_bits_data_o_53                                      [15:0] ),
    .data_out_bits_data_o_54                                      ( data_out_bits_data_o_54                                      [15:0] ),
    .data_out_bits_data_o_55                                      ( data_out_bits_data_o_55                                      [15:0] ),
    .data_out_bits_data_o_56                                      ( data_out_bits_data_o_56                                      [15:0] ),
    .data_out_bits_data_o_57                                      ( data_out_bits_data_o_57                                      [15:0] ),
    .data_out_bits_data_o_58                                      ( data_out_bits_data_o_58                                      [15:0] ),
    .data_out_bits_data_o_59                                      ( data_out_bits_data_o_59                                      [15:0] ),
    .data_out_bits_data_o_60                                      ( data_out_bits_data_o_60                                      [15:0] ),
    .data_out_bits_data_o_61                                      ( data_out_bits_data_o_61                                      [15:0] ),
    .data_out_bits_data_o_62                                      ( data_out_bits_data_o_62                                      [15:0] ),
    .data_out_bits_data_o_63                                      ( data_out_bits_data_o_63                                      [15:0] )
);

initial
begin

    clock=1'b1;
    reset=1'b1;
    #100; //延时100ns
    reset=1'b0;

    data_in_stage1_valid = 1'b1;
    data_in_stage2_valid = 1'b1;
    data_in_stage3_valid = 1'b1;
    data_in_stage1_bits_batch_num=16'b0;
    data_in_stage2_bits_batch_num=16'b0;

    // data_in_stage1_valid=1'b1;
    // data_in_stage2_valid=1'b1;
    // data_in_stage3_valid=1'b1;

    data_in_stage1_bits_raw_data_0  = 16'd0;
    data_in_stage1_bits_raw_data_1  = 16'd1;
    data_in_stage1_bits_raw_data_2  = 16'd2;
    data_in_stage1_bits_raw_data_3  = 16'd3;
    data_in_stage1_bits_raw_data_4  = 16'd4;
    data_in_stage1_bits_raw_data_5  = 16'd5;
    data_in_stage1_bits_raw_data_6  = 16'd6;
    data_in_stage1_bits_raw_data_7  = 16'd7;
    data_in_stage1_bits_raw_data_8  = 16'd8;
    data_in_stage1_bits_raw_data_9  = 16'd9;
    data_in_stage1_bits_raw_data_10 = 16'd10;
    data_in_stage1_bits_raw_data_11 = 16'd11;
    data_in_stage1_bits_raw_data_12 = 16'd12;
    data_in_stage1_bits_raw_data_13 = 16'd13;
    data_in_stage1_bits_raw_data_14 = 16'd14;
    data_in_stage1_bits_raw_data_15 = 16'd15;
    data_in_stage1_bits_raw_data_16 = 16'd16;
    data_in_stage1_bits_raw_data_17 = 16'd17;
    data_in_stage1_bits_raw_data_18 = 16'd18;
    data_in_stage1_bits_raw_data_19 = 16'd19;
    data_in_stage1_bits_raw_data_20 = 16'd20;
    data_in_stage1_bits_raw_data_21 = 16'd21;
    data_in_stage1_bits_raw_data_22 = 16'd22;
    data_in_stage1_bits_raw_data_23 = 16'd23;
    data_in_stage1_bits_raw_data_24 = 16'd24;
    data_in_stage1_bits_raw_data_25 = 16'd25;
    data_in_stage1_bits_raw_data_26 = 16'd26;
    data_in_stage1_bits_raw_data_27 = 16'd27;
    data_in_stage1_bits_raw_data_28 = 16'd28;
    data_in_stage1_bits_raw_data_29 = 16'd29;
    data_in_stage1_bits_raw_data_30 = 16'd6555;
    data_in_stage1_bits_raw_data_31 = 16'd31;
    data_in_stage1_bits_raw_data_32 = 16'd0;
    data_in_stage1_bits_raw_data_33 = 16'd1;
    data_in_stage1_bits_raw_data_34 = 16'd2;
    data_in_stage1_bits_raw_data_35 = 16'd3;
    data_in_stage1_bits_raw_data_36 = 16'd4;
    data_in_stage1_bits_raw_data_37 = 16'd5;
    data_in_stage1_bits_raw_data_38 = 16'd6;
    data_in_stage1_bits_raw_data_39 = 16'd7;
    data_in_stage1_bits_raw_data_40 = 16'd8;
    data_in_stage1_bits_raw_data_41 = 16'd9;
    data_in_stage1_bits_raw_data_42 = 16'd10;
    data_in_stage1_bits_raw_data_43 = 16'd11;
    data_in_stage1_bits_raw_data_44 = 16'd12;
    data_in_stage1_bits_raw_data_45 = 16'd13;
    data_in_stage1_bits_raw_data_46 = 16'd14;
    data_in_stage1_bits_raw_data_47 = 16'd15;
    data_in_stage1_bits_raw_data_48 = 16'd16;
    data_in_stage1_bits_raw_data_49 = 16'd17;
    data_in_stage1_bits_raw_data_50 = 16'd18;
    data_in_stage1_bits_raw_data_51 = 16'd19;
    data_in_stage1_bits_raw_data_52 = 16'd20;
    data_in_stage1_bits_raw_data_53 = 16'd21;
    data_in_stage1_bits_raw_data_54 = 16'd22;
    data_in_stage1_bits_raw_data_55 = 16'd23;
    data_in_stage1_bits_raw_data_56 = 16'd24;
    data_in_stage1_bits_raw_data_57 = 16'd25;
    data_in_stage1_bits_raw_data_58 = 16'd26;
    data_in_stage1_bits_raw_data_59 = 16'd27;
    data_in_stage1_bits_raw_data_60 = 16'd28;
    data_in_stage1_bits_raw_data_61 = 16'd29;
    data_in_stage1_bits_raw_data_62 = 16'd6555;
    data_in_stage1_bits_raw_data_63 = 16'd31;


    data_in_stage2_bits_data_in_0  = 16'd0;
    data_in_stage2_bits_data_in_1  = 16'd1;
    data_in_stage2_bits_data_in_2  = 16'd2;
    data_in_stage2_bits_data_in_3  = 16'd3;
    data_in_stage2_bits_data_in_4  = 16'd4;
    data_in_stage2_bits_data_in_5  = 16'd5;
    data_in_stage2_bits_data_in_6  = 16'd6;
    data_in_stage2_bits_data_in_7  = 16'd7;
    data_in_stage2_bits_data_in_8  = 16'd8;
    data_in_stage2_bits_data_in_9  = 16'd9;
    data_in_stage2_bits_data_in_10 = 16'd10;
    data_in_stage2_bits_data_in_11 = 16'd11;
    data_in_stage2_bits_data_in_12 = 16'd12;
    data_in_stage2_bits_data_in_13 = 16'd13;
    data_in_stage2_bits_data_in_14 = 16'd14;
    data_in_stage2_bits_data_in_15 = 16'd15;
    data_in_stage2_bits_data_in_16 = 16'd16;
    data_in_stage2_bits_data_in_17 = 16'd17;
    data_in_stage2_bits_data_in_18 = 16'd18;
    data_in_stage2_bits_data_in_19 = 16'd19;
    data_in_stage2_bits_data_in_20 = 16'd20;
    data_in_stage2_bits_data_in_21 = 16'd21;
    data_in_stage2_bits_data_in_22 = 16'd22;
    data_in_stage2_bits_data_in_23 = 16'd23;
    data_in_stage2_bits_data_in_24 = 16'd24;
    data_in_stage2_bits_data_in_25 = 16'd25;
    data_in_stage2_bits_data_in_26 = 16'd26;
    data_in_stage2_bits_data_in_27 = 16'd27;
    data_in_stage2_bits_data_in_28 = 16'd28;
    data_in_stage2_bits_data_in_29 = 16'd29;
    data_in_stage2_bits_data_in_30 = 16'd6555;
    data_in_stage2_bits_data_in_31 = 16'd31;
    data_in_stage2_bits_data_in_32 = 16'd0;
    data_in_stage2_bits_data_in_33 = 16'd1;
    data_in_stage2_bits_data_in_34 = 16'd2;
    data_in_stage2_bits_data_in_35 = 16'd3;
    data_in_stage2_bits_data_in_36 = 16'd4;
    data_in_stage2_bits_data_in_37 = 16'd5;
    data_in_stage2_bits_data_in_38 = 16'd6;
    data_in_stage2_bits_data_in_39 = 16'd7;
    data_in_stage2_bits_data_in_40 = 16'd8;
    data_in_stage2_bits_data_in_41 = 16'd9;
    data_in_stage2_bits_data_in_42 = 16'd10;
    data_in_stage2_bits_data_in_43 = 16'd11;
    data_in_stage2_bits_data_in_44 = 16'd12;
    data_in_stage2_bits_data_in_45 = 16'd13;
    data_in_stage2_bits_data_in_46 = 16'd14;
    data_in_stage2_bits_data_in_47 = 16'd15;
    data_in_stage2_bits_data_in_48 = 16'd16;
    data_in_stage2_bits_data_in_49 = 16'd17;
    data_in_stage2_bits_data_in_50 = 16'd18;
    data_in_stage2_bits_data_in_51 = 16'd19;
    data_in_stage2_bits_data_in_52 = 16'd20;
    data_in_stage2_bits_data_in_53 = 16'd21;
    data_in_stage2_bits_data_in_54 = 16'd22;
    data_in_stage2_bits_data_in_55 = 16'd23;
    data_in_stage2_bits_data_in_56 = 16'd24;
    data_in_stage2_bits_data_in_57 = 16'd25;
    data_in_stage2_bits_data_in_58 = 16'd26;
    data_in_stage2_bits_data_in_59 = 16'd27;
    data_in_stage2_bits_data_in_60 = 16'd28;
    data_in_stage2_bits_data_in_61 = 16'd29;
    data_in_stage2_bits_data_in_62 = 16'd6555;
    data_in_stage2_bits_data_in_63 = 16'd31;


    data_in_stage3_bits_data_exp     = 5'd0;
    data_in_stage3_bits_data_frac_0  = 10'd0;
    data_in_stage3_bits_data_frac_1  = 10'd1;
    data_in_stage3_bits_data_frac_2  = 10'd2;
    data_in_stage3_bits_data_frac_3  = 10'd3;
    data_in_stage3_bits_data_frac_4  = 10'd4;
    data_in_stage3_bits_data_frac_5  = 10'd5;
    data_in_stage3_bits_data_frac_6  = 10'd6;
    data_in_stage3_bits_data_frac_7  = 10'd7;
    data_in_stage3_bits_data_frac_8  = 10'd8;
    data_in_stage3_bits_data_frac_9  = 10'd9;
    data_in_stage3_bits_data_frac_10 = 10'd10;
    data_in_stage3_bits_data_frac_11 = 10'd11;
    data_in_stage3_bits_data_frac_12 = 10'd12;
    data_in_stage3_bits_data_frac_13 = 10'd13;
    data_in_stage3_bits_data_frac_14 = 10'd14;
    data_in_stage3_bits_data_frac_15 = 10'd15;
    data_in_stage3_bits_data_frac_16 = 10'd16;
    data_in_stage3_bits_data_frac_17 = 10'd17;
    data_in_stage3_bits_data_frac_18 = 10'd18;
    data_in_stage3_bits_data_frac_19 = 10'd19;
    data_in_stage3_bits_data_frac_20 = 10'd20;
    data_in_stage3_bits_data_frac_21 = 10'd21;
    data_in_stage3_bits_data_frac_22 = 10'd22;
    data_in_stage3_bits_data_frac_23 = 10'd23;
    data_in_stage3_bits_data_frac_24 = 10'd24;
    data_in_stage3_bits_data_frac_25 = 10'd25;
    data_in_stage3_bits_data_frac_26 = 10'd26;
    data_in_stage3_bits_data_frac_27 = 10'd27;
    data_in_stage3_bits_data_frac_28 = 10'd28;
    data_in_stage3_bits_data_frac_29 = 10'd29;
    data_in_stage3_bits_data_frac_30 = 10'd6555;
    data_in_stage3_bits_data_frac_31 = 10'd31;
    data_in_stage3_bits_data_frac_32 = 10'd0;
    data_in_stage3_bits_data_frac_33 = 10'd1;
    data_in_stage3_bits_data_frac_34 = 10'd2;
    data_in_stage3_bits_data_frac_35 = 10'd3;
    data_in_stage3_bits_data_frac_36 = 10'd4;
    data_in_stage3_bits_data_frac_37 = 10'd5;
    data_in_stage3_bits_data_frac_38 = 10'd6;
    data_in_stage3_bits_data_frac_39 = 10'd7;
    data_in_stage3_bits_data_frac_40 = 10'd8;
    data_in_stage3_bits_data_frac_41 = 10'd9;
    data_in_stage3_bits_data_frac_42 = 10'd10;
    data_in_stage3_bits_data_frac_43 = 10'd11;
    data_in_stage3_bits_data_frac_44 = 10'd12;
    data_in_stage3_bits_data_frac_45 = 10'd13;
    data_in_stage3_bits_data_frac_46 = 10'd14;
    data_in_stage3_bits_data_frac_47 = 10'd15;
    data_in_stage3_bits_data_frac_48 = 10'd16;
    data_in_stage3_bits_data_frac_49 = 10'd17;
    data_in_stage3_bits_data_frac_50 = 10'd18;
    data_in_stage3_bits_data_frac_51 = 10'd19;
    data_in_stage3_bits_data_frac_52 = 10'd20;
    data_in_stage3_bits_data_frac_53 = 10'd21;
    data_in_stage3_bits_data_frac_54 = 10'd22;
    data_in_stage3_bits_data_frac_55 = 10'd23;
    data_in_stage3_bits_data_frac_56 = 10'd24;
    data_in_stage3_bits_data_frac_57 = 10'd25;
    data_in_stage3_bits_data_frac_58 = 10'd26;
    data_in_stage3_bits_data_frac_59 = 10'd27;
    data_in_stage3_bits_data_frac_60 = 10'd28;
    data_in_stage3_bits_data_frac_61 = 10'd29;
    data_in_stage3_bits_data_frac_62 = 10'd6555;
    data_in_stage3_bits_data_frac_63 = 10'd31;
end

endmodule