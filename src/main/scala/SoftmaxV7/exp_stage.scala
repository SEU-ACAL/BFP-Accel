package exp_stage

import chisel3._
import chisel3.util._
import chisel3.stage._

import softmax_define.MACRO._
import softmax_define.FSM._
import softmax_define.function._
import softmax_define.test._

import pipeline._


import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.annotations.MemoryLoadFileType

class exp_input extends Bundle { 
    val data_in     = Vec(cycle_bandwidth, UInt(bitwidth.W))
    val batch_num   = UInt(log2Up(maxBatch).W)
}

class exp_stage(bandwidth_in: Int, bandwidth_out: Int) extends Module {
    val input_expu_i   = IO(Flipped(Decoupled(new exp_input)))
    val maxexp_expu_i  = IO(Flipped(Decoupled(new max_exp)))
    val expu_expdiv_o  = IO(Decoupled(new exp_div))
    // ======================= FSM ==========================
    input_expu_i.ready  := true.B
    maxexp_expu_i.ready := true.B
    // ======================= SRAM ==========================
    // val lut = VecInit((Seq.fill(cycle_bandwidth))(SyncReadMem(lut_depth, UInt(lut_width.W)))) // 模拟双端口，后端替换为双端口综合
    val lut = Seq.fill(cycle_bandwidth)(SyncReadMem(lut_depth, UInt((lut_width * lut_bitwidth).W)))
    for (i <- 0 until 32) {
      loadMemoryFromFileInline(lut(2*i), "/home/shiroha/Code/experiment/lut_8x120_1.hex", MemoryLoadFileType.Hex); 
      loadMemoryFromFileInline(lut(2*i+1), "/home/shiroha/Code/experiment/lut_8x120_2.hex", MemoryLoadFileType.Hex); 
    }
    // ====================== 非均匀对表 =========================
    // val share_exp = WireInit(0.U(log2Up(exp_bitwidth).W))
    // when (maxexp_expu_i.valid)  { share_exp := maxexp_expu_i.bits.max >> 1} // dived 2
    // ======================= seu  ==========================
    val seu = Module(new SEU(cycle_bandwidth)).io
    seu.data_in.valid                := input_expu_i.valid
    seu.data_in.bits.batch_num       := input_expu_i.bits.batch_num
    seu.data_in.bits.data_in         := input_expu_i.bits.data_in
    seu.data_in.bits.max_in          := maxexp_expu_i.bits.max
    
    // ======================= sub  ==========================
    val subu = Module(new SUBU(cycle_bandwidth)).io
    subu.data_in.valid               := seu.data_out.valid
    subu.data_in.bits.batch_num      := seu.data_out.bits.batch_num   
    subu.data_in.bits.seu_sign_vec   := seu.data_out.bits.seu_sign_vec
    subu.data_in.bits.seu_exp_vec    := seu.data_out.bits.seu_exp_vec 
    subu.data_in.bits.seu_frac_vec   := seu.data_out.bits.seu_frac_vec
    subu.data_in.bits.seu_max        := seu.data_out.bits.seu_max 

    // ======================= get exp value ==========================
    val expu_data_in_valid            = WireInit(false.B)                                                   // Input
    val expu_data_in_bits_batch_num   = WireInit(0.U(log2Up(maxBatch).W))                                   // Input
    val expu_data_in_bits_idx_vec     = WireInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(7.W))))   // Input
    // val expu_data_in_bits_rate_vec    = WireInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(rate_bitwidth.W))))  // Input

    val expu_data_out_valid           = RegInit(false.B)                                                        // output
    val expu_data_out_bits_batch_num  = RegInit(0.U(log2Up(maxBatch).W))                                        // output
    // val exp_vec                       = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(expvalue_bitwidth.W))))   // output

    expu_data_in_valid               := subu.data_out.valid
    expu_data_in_bits_batch_num      := subu.data_out.bits.batch_num   
    expu_data_in_bits_idx_vec        := subu.data_out.bits.subu_idx_vec    

    val seu_exp_vec_w     = WireInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((lut_width * expvalue_bitwidth).W))))
    val seu_exp_vec_r     = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(expvalue_bitwidth.W))))
    

    when (subu.data_out.valid) {
        for (i <- 0 until cycle_bandwidth) {
            seu_exp_vec_w(i)   :=     lut(i).read(expu_data_in_bits_idx_vec(i)(5, 2) , true.B)//(expu_data_in_bits_idx_vec(i)(2,0))
        }
    }

    when (subu.data_out.valid) {
        for (i <- 0 until cycle_bandwidth) {
            seu_exp_vec_r(i) := MuxCase(0.U(expvalue_bitwidth.W), Seq(
                (expu_data_in_bits_idx_vec(i)(2, 0) === 0.U)  -> seu_exp_vec_w(i)(14, 0),
                (expu_data_in_bits_idx_vec(i)(2, 0) === 1.U)  -> seu_exp_vec_w(i)(29, 15),
                (expu_data_in_bits_idx_vec(i)(2, 0) === 2.U)  -> seu_exp_vec_w(i)(44, 30),
                (expu_data_in_bits_idx_vec(i)(2, 0) === 3.U)  -> seu_exp_vec_w(i)(59, 45),
                (expu_data_in_bits_idx_vec(i)(2, 0) === 4.U)  -> seu_exp_vec_w(i)(74, 60),
                (expu_data_in_bits_idx_vec(i)(2, 0) === 5.U)  -> seu_exp_vec_w(i)(89, 75),
                (expu_data_in_bits_idx_vec(i)(2, 0) === 6.U)  -> seu_exp_vec_w(i)(104, 90),
                (expu_data_in_bits_idx_vec(i)(2, 0) === 7.U)  -> seu_exp_vec_w(i)(119, 105),
            ))
        }
    }
    val expu_data_out_valid_tmp      = RegInit(false.B)      
    // expu_data_out_valid             := subu.data_out.valid
    expu_data_out_valid_tmp         := expu_data_in_valid
    expu_data_out_valid             := expu_data_out_valid_tmp

    val expu_data_out_bits_batch_num_tmp  = RegInit(0.U(log2Up(maxBatch).W)) 

    // expu_data_out_bits_batch_num        := subu.data_out.bits.batch_num   
    expu_data_out_bits_batch_num_tmp    := expu_data_in_bits_batch_num 
    expu_data_out_bits_batch_num        := expu_data_out_bits_batch_num_tmp   // 屎山代码，这里要缓一周期

     // ======================= add ==========================
    val sum                 = RegInit((0.U(sum_bitwidth.W)))

    val AdderTree = Module(new AdderTree(cycle_bandwidth)).io
    AdderTree.in.valid           := expu_data_out_valid    
    AdderTree.in.bits.batch_num  := expu_data_out_bits_batch_num
    AdderTree.in.bits.data_in    := Mux(expu_data_out_valid, seu_exp_vec_r, VecInit(Seq.fill(cycle_bandwidth)(0.U((expvalue_bitwidth).W))))

    when (AdderTree.out.valid & (AdderTree.out.bits.batch_num =/= (maxBatch - 1).U)) { sum := sum + AdderTree.out.bits.sum}
    when (AdderTree.out.valid & (AdderTree.out.bits.batch_num === (maxBatch - 1).U)) {
        expu_expdiv_o.valid              := true.B
        expu_expdiv_o.bits.sum           := sum
    }.otherwise {
        expu_expdiv_o.valid              := false.B
        expu_expdiv_o.bits.sum           := 0.U
    }
}


class SEU(numElements: Int) extends Module {
    val io = IO(new Bundle {
        val data_in   = Flipped(Valid(new Bundle { 
            val data_in     = Vec(cycle_bandwidth, UInt(bitwidth.W))
            val max_in      = UInt(bitwidth.W)
            val batch_num   = UInt(log2Up(maxBatch).W)
        }))
        val data_out  = Valid(new Bundle { 
            val seu_sign_vec    = Vec(cycle_bandwidth, UInt(1.W))
            val seu_exp_vec     = Vec(cycle_bandwidth, UInt((exp_bitwidth).W))
            val seu_frac_vec    = Vec(cycle_bandwidth, UInt((frac_bitwidth).W))
            val seu_max         = UInt(bitwidth.W)
            val batch_num       = UInt(log2Up(maxBatch).W)
        })
    })

    val seu_sign_vec    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(1.W))))
    val seu_exp_vec     = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((exp_bitwidth).W))))
    val seu_frac_vec    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((frac_bitwidth).W))))
    
    val seu_max         = RegInit(0.U((bitwidth).W))

    val subuValid       = RegInit(false.B)
    val batch_num       = RegInit(0.U(log2Up(maxBatch).W))


    // 对指x
    when (io.data_in.valid) {
        for (i <- 0 until cycle_bandwidth) { 
            seu_sign_vec(i) := io.data_in.bits.data_in(i)(15) 
            seu_exp_vec(i)  := io.data_in.bits.data_in(i)(14, 10) 
            seu_frac_vec(i) := Mux(io.data_in.bits.data_in(i)(11, 10) =/= 0.U, ((Cat(1.U, io.data_in.bits.data_in(i)(9,  0))) << (io.data_in.bits.data_in(i)(11, 10))), 
                                  (io.data_in.bits.data_in(i)(9,  0) << (io.data_in.bits.data_in(i)(11, 10))))
        }

    }

    seu_max := io.data_in.bits.max_in

    subuValid                       := Mux(io.data_in.valid, true.B, false.B)
    batch_num                       := Mux(io.data_in.valid, io.data_in.bits.batch_num, batch_num)
    io.data_out.valid               := Mux(subuValid, true.B, false.B)
    io.data_out.bits.seu_sign_vec   := Mux(io.data_out.valid, seu_sign_vec, VecInit(Seq.fill(cycle_bandwidth)(0.U((1).W)))) 
    io.data_out.bits.seu_exp_vec    := Mux(io.data_out.valid, seu_exp_vec , VecInit(Seq.fill(cycle_bandwidth)(0.U((exp_bitwidth).W)))) 
    io.data_out.bits.seu_frac_vec   := Mux(io.data_out.valid, seu_frac_vec, VecInit(Seq.fill(cycle_bandwidth)(0.U((frac_bitwidth).W)))) 
    io.data_out.bits.seu_max        := Mux(io.data_out.valid, seu_max     , 0.U((bitwidth).W)) 
    io.data_out.bits.batch_num      := Mux(io.data_out.valid, batch_num   , 0.U) 
}

class SUBU(numElements: Int) extends Module {
    val io = IO(new Bundle {
        val data_in  = Flipped(Valid(new Bundle { 
            val batch_num       = UInt(log2Up(maxBatch).W)
            val seu_sign_vec    = Vec(cycle_bandwidth, UInt(1.W))
            val seu_exp_vec     = Vec(cycle_bandwidth, UInt((exp_bitwidth).W))
            val seu_frac_vec    = Vec(cycle_bandwidth, UInt((frac_bitwidth).W))
            val seu_max         = UInt(bitwidth.W)
        }))
        val data_out  = Valid(new Bundle { 
            val batch_num       = UInt(log2Up(maxBatch).W)
            val subu_idx_vec    = Vec(cycle_bandwidth, UInt(7.W))
        })
    })

    val subu_tmp_vec_w   = WireInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(frac_bitwidth.W)))) // 中间值
    val subu_idx_vec     = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(idx_bitwidth.W))))

    val expuValid        = RegInit(false.B)
    val batch_num        = RegInit(0.U(log2Up(maxBatch).W))


    when (io.data_in.valid) {
        for (i <- 0 until cycle_bandwidth) { 
            subu_tmp_vec_w(i)   := Mux((io.data_in.bits.seu_sign_vec(i) === 0.U & io.data_in.bits.seu_max(15) === 0.U), io.data_in.bits.seu_max(9, 0) - io.data_in.bits.seu_frac_vec(i),
                                    Mux((io.data_in.bits.seu_sign_vec(i) === 1.U & io.data_in.bits.seu_max(15) === 1.U), io.data_in.bits.seu_frac_vec(i) - io.data_in.bits.seu_max(9, 0), 
                                    Mux((io.data_in.bits.seu_sign_vec(i) === 1.U) & (io.data_in.bits.seu_max(15) === 0.U), io.data_in.bits.seu_frac_vec(i) + io.data_in.bits.seu_max(9, 0), 0.U)))
            // subu_idx_vec(i)     :=  Cat(io.data_in.bits.seu_exp_vec(i), subu_tmp_vec_w(i)(9, 6))
            subu_idx_vec(i)     :=  subu_tmp_vec_w(i)(9, 2) // 取七位做索引
        }
    }

    expuValid                       := Mux(io.data_in.valid, true.B, false.B)
    batch_num                       := Mux(io.data_in.valid, io.data_in.bits.batch_num, batch_num)
    io.data_out.valid               := Mux(expuValid, true.B, false.B)
    io.data_out.bits.batch_num      := Mux(io.data_out.valid,     batch_num, 0.U) 
    io.data_out.bits.subu_idx_vec   := Mux(io.data_out.valid,  subu_idx_vec,  VecInit(Seq.fill(cycle_bandwidth)(0.U(idx_bitwidth.W)))) 
}

class AdderTreeLevel (AdderTreeBandwidth: Int) extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Valid(new Bundle {
                            val data_in   = Vec(AdderTreeBandwidth, UInt(AdderinBitwidth.W))
                            val batch_num = UInt((log2Up(maxBatch)).W)}))
        val out = Valid(new Bundle {
                            val sum       = Vec(AdderTreeBandwidth/4, UInt(AdderinBitwidth.W))
                            val batch_num = UInt((log2Up(maxBatch)).W)})
    })
    for (i <- 0 until AdderTreeBandwidth/4) { io.out.bits.sum(i) := io.in.bits.data_in(i * 4) + io.in.bits.data_in(i * 4 + 1) + io.in.bits.data_in(i * 4 + 2) + io.in.bits.data_in(i * 4 + 3)}
    
    io.out.valid          := io.in.valid
    io.out.bits.batch_num := io.in.bits.batch_num
}

class AdderTree(numElements: Int) extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(Valid(new Bundle {
                            val data_in   = Vec(numElements, UInt(exp_bitwidth.W))
                            val batch_num = UInt((log2Up(maxBatch)).W)}))
        val out = Valid(new Bundle {
                            val sum       = UInt(sum_bitwidth.W)
                            val batch_num = UInt((log2Up(maxBatch)).W)})
    })

    val TreeLevel1_in_valid             = RegInit(false.B)
    val TreeLevel1_in_bits_data_in      = RegInit(VecInit(Seq.fill(64)(0.U(exp_bitwidth.W))))
    val TreeLevel1_in_bits_batch_num    = RegInit(0.U((log2Up(maxBatch)).W))
    TreeLevel1_in_valid                := io.in.valid
    TreeLevel1_in_bits_data_in         := io.in.bits.data_in
    TreeLevel1_in_bits_batch_num       := io.in.bits.batch_num

    val TreeLevel1                 = Module(new AdderTreeLevel(64)).io
    TreeLevel1.in.valid           := TreeLevel1_in_valid  
    TreeLevel1.in.bits.data_in    := TreeLevel1_in_bits_data_in
    TreeLevel1.in.bits.batch_num  := TreeLevel1_in_bits_batch_num

    val TreeLevel1_out_valid             = RegInit(false.B)
    val TreeLevel1_out_bits_sum          = RegInit(VecInit(Seq.fill(16)(0.U(exp_bitwidth.W))))
    val TreeLevel1_out_bits_batch_num    = RegInit(0.U((log2Up(maxBatch)).W))
    TreeLevel1_out_valid                := TreeLevel1.out.valid
    TreeLevel1_out_bits_sum             := TreeLevel1.out.bits.sum
    TreeLevel1_out_bits_batch_num       := TreeLevel1.out.bits.batch_num

    val TreeLevel2       = Module(new AdderTreeLevel(16)).io
    TreeLevel2.in.valid           := TreeLevel1_out_valid  
    TreeLevel2.in.bits.data_in    := TreeLevel1_out_bits_sum
    TreeLevel2.in.bits.batch_num  := TreeLevel1_out_bits_batch_num
    
    val TreeLevel2_out_valid             = RegInit(false.B)
    val TreeLevel2_out_bits_sum          = RegInit(VecInit(Seq.fill(4)(0.U(exp_bitwidth.W))))
    val TreeLevel2_out_bits_batch_num    = RegInit(0.U((log2Up(maxBatch)).W))
    TreeLevel2_out_valid                := TreeLevel2.out.valid
    TreeLevel2_out_bits_sum             := TreeLevel2.out.bits.sum
    TreeLevel2_out_bits_batch_num       := TreeLevel2.out.bits.batch_num


    val TreeLevel3 = Module(new AdderTreeLevel(4)).io
    TreeLevel3.in.valid           := TreeLevel2_out_valid  
    TreeLevel3.in.bits.data_in    := TreeLevel2_out_bits_sum
    TreeLevel3.in.bits.batch_num  := TreeLevel2_out_bits_batch_num


    io.out.valid            := TreeLevel3.out.valid
    io.out.bits.sum         := TreeLevel3.out.bits.sum(0)
    io.out.bits.batch_num   := TreeLevel3.out.bits.batch_num
}