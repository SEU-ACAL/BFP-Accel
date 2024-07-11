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
    val data_in    = Vec(cycle_bandwidth, UInt(bitwidth.W))
    val batch_num   = UInt(log2Up(maxBatch).W)
}

class exp_stage(bandwidth_in: Int, bandwidth_out: Int) extends Module {
    val input_expu_i   = IO(Flipped(Decoupled(new exp_input)))
    val maxexp_expu_i  = IO(Flipped(Decoupled(new max_exp)))
    val expu_expdiv_o  = IO(Decoupled(new exp_div))
    // ======================= FSM ==========================
    input_expu_i.ready  := true.B
    maxexp_expu_i.ready := true.B
    // ======================= DRAM ==========================
    val mem = Mem(16, UInt(lut_bandwidth.W))
    // val mem = SyncReadMem(16, UInt(lut_bandwidth.W))
    loadMemoryFromFileInline(mem, "/home/shiroha/Code/TETris/Backend/ml-accelerator/src/main/scala/SoftmaxV6/data/lut_16x256", MemoryLoadFileType.Hex); 
    // ======================= SRAM ==========================
    val lut = VecInit((Seq.fill(cycle_bandwidth/2))(SRAM(2, UInt(lut_bandwidth.W), 2, 2, 0))) // Declare a 2 read, 2 write, 0 read-write ported SRAM with 16-bit UInt data members
    // ====================== 非均匀对表 =========================
    val share_exp = RegInit(0.U(log2Up(exp_bitwidth).W))
    when (input_expu_i.valid) {
        share_exp := Lookup(maxexp_expu_i.bits.max(14, 10), 0.U, Seq(
            BitPat("b00000") -> 0.U, BitPat("b00001") -> 0.U, BitPat("b00010") -> 0.U, BitPat("b00011") -> 0.U,  BitPat("b00100") -> 0.U, BitPat("b00101") -> 0.U, BitPat("b00110") -> 0.U, BitPat("b00111") -> 0.U, BitPat("b01000") -> 0.U, BitPat("b01001") -> 0.U, 
            BitPat("b01010") -> 1.U, BitPat("b01011") -> 1.U,
            BitPat("b01100") -> 2.U, BitPat("b01101") -> 2.U, 
            BitPat("b01110") -> 3.U, 
            BitPat("b01111") -> 4.U, 
            BitPat("b10000") -> 5.U, 
            BitPat("b10001") -> 6.U, 
            BitPat("b10010") -> 7.U, BitPat("b10011") -> 7.U, BitPat("b10100") -> 7.U, BitPat("b10101") -> 7.U, BitPat("b10110") -> 7.U, BitPat("b10111") -> 7.U, BitPat("b11000") -> 7.U, BitPat("b11001") -> 7.U, BitPat("b11010") -> 7.U, BitPat("b11011") -> 7.U, BitPat("b11100") -> 7.U, BitPat("b11101") -> 7.U, BitPat("b11110") -> 7.U, BitPat("b11111") -> 7.U,  
        ))    
    }
    // ======================= seu  ==========================
    val seu = Module(new SEU(cycle_bandwidth)).io
    seu.data_in.valid                := input_expu_i.valid
    seu.data_in.bits.batch_num       := input_expu_i.bits.batch_num
    seu.data_in.bits.data_in         := input_expu_i.bits.data_in
    seu.data_in.bits.max_in          := maxexp_expu_i.bits.max
    
    // ====================== preload ==========================
    when (seu.data_out.valid) {
        for (i <- 0 until cycle_bandwidth/2) {
            lut(i).writePorts(0).enable  := true.B
            lut(i).writePorts(1).enable  := true.B
            lut(i).writePorts(0).address := 0.U
            lut(i).writePorts(1).address := 1.U

            lut(i).writePorts(0).data    := mem(share_exp*2.U) // 对到对应索引
            lut(i).writePorts(1).data    := mem(share_exp*2.U+1.U) // 对到对应索引
        }
    }.otherwise {
        for (i <- 0 until cycle_bandwidth/2) {
            lut(i).writePorts(0).enable  := false.B
            lut(i).writePorts(1).enable  := false.B
            lut(i).writePorts(0).address := 0.U
            lut(i).writePorts(1).address := 0.U

            lut(i).writePorts(0).data    := 0.U
            lut(i).writePorts(1).data    := 0.U
        }
    }
    
    // ======================= sub  ==========================
    val subu = Module(new SUBU(cycle_bandwidth)).io
    subu.data_in.valid               := seu.data_out.valid
    subu.data_in.bits.batch_num      := seu.data_out.bits.batch_num   
    subu.data_in.bits.seu_sign_vec   := seu.data_out.bits.seu_sign_vec
    subu.data_in.bits.seu_exp_vec    := seu.data_out.bits.seu_exp_vec 
    subu.data_in.bits.seu_frac_vec   := seu.data_out.bits.seu_frac_vec
    subu.data_in.bits.seu_max_vec    := seu.data_out.bits.seu_max_vec 

    

    // ======================= get exp value ==========================
    val expu_data_in_valid            = WireInit(false.B)                                                   // Input
    val expu_data_in_bits_batch_num   = WireInit(0.U(log2Up(maxBatch).W))                                   // Input
    val expu_data_in_bits_idx_vec     = WireInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(idx_bitwidth.W))))   // Input
    val expu_data_in_bits_rate_vec    = WireInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(rate_bitwidth.W))))  // Input

    val expu_data_out_valid           = RegInit(false.B)                                                        // output
    val expu_data_out_bits_batch_num  = RegInit(0.U(log2Up(maxBatch).W))                                       // output
    val exp_vec                       = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(expvalue_bitwidth.W))))   // output

    expu_data_in_valid               := subu.data_out.valid
    expu_data_in_bits_batch_num      := subu.data_out.bits.batch_num   
    expu_data_in_bits_idx_vec        := subu.data_out.bits.subu_idx_vec    
    expu_data_in_bits_rate_vec       := subu.data_out.bits.subu_rate_vec    

    when (expu_data_in_valid) {
        for (i <- 0 until cycle_bandwidth/2) {
            lut(i).readPorts(0).enable  := true.B
            lut(i).readPorts(1).enable  := true.B
            lut(i).readPorts(0).address := subu.data_out.bits.subu_idx_vec(i*2)(idx_bitwidth-1)
            lut(i).readPorts(1).address := subu.data_out.bits.subu_idx_vec(i*2+1)(idx_bitwidth-1)

            val rdata0 = lut(i).readPorts(0).data
            val rdata1 = lut(i).readPorts(1).data
            
            exp_vec(i * 2) := MuxCase(0.U(expvalue_bitwidth.W), Seq(
                (subu.data_out.bits.subu_idx_vec(i * 2)(idx_bitwidth - 2, 0) === 0.U) -> rdata0(15, 0),
                (subu.data_out.bits.subu_idx_vec(i * 2)(idx_bitwidth - 2, 0) === 1.U) -> rdata0(31, 16),
                (subu.data_out.bits.subu_idx_vec(i * 2)(idx_bitwidth - 2, 0) === 2.U) -> rdata0(47, 32),
                (subu.data_out.bits.subu_idx_vec(i * 2)(idx_bitwidth - 2, 0) === 3.U) -> rdata0(63, 48),
                (subu.data_out.bits.subu_idx_vec(i * 2)(idx_bitwidth - 2, 0) === 4.U) -> rdata0(79, 64),
                (subu.data_out.bits.subu_idx_vec(i * 2)(idx_bitwidth - 2, 0) === 5.U) -> rdata0(95, 80),
                (subu.data_out.bits.subu_idx_vec(i * 2)(idx_bitwidth - 2, 0) === 6.U) -> rdata0(111, 96),
                (subu.data_out.bits.subu_idx_vec(i * 2)(idx_bitwidth - 2, 0) === 7.U) -> rdata0(127, 112),
                (subu.data_out.bits.subu_idx_vec(i * 2)(idx_bitwidth - 2, 0) === 8.U) -> rdata0(143, 128),
                (subu.data_out.bits.subu_idx_vec(i * 2)(idx_bitwidth - 2, 0) === 9.U) -> rdata0(159, 144),
                (subu.data_out.bits.subu_idx_vec(i * 2)(idx_bitwidth - 2, 0) === 10.U) -> rdata0(175, 160),
                (subu.data_out.bits.subu_idx_vec(i * 2)(idx_bitwidth - 2, 0) === 11.U) -> rdata0(191, 176),
                (subu.data_out.bits.subu_idx_vec(i * 2)(idx_bitwidth - 2, 0) === 12.U) -> rdata0(207, 192),
                (subu.data_out.bits.subu_idx_vec(i * 2)(idx_bitwidth - 2, 0) === 13.U) -> rdata0(223, 208),
                (subu.data_out.bits.subu_idx_vec(i * 2)(idx_bitwidth - 2, 0) === 14.U) -> rdata0(239, 224),
                (subu.data_out.bits.subu_idx_vec(i * 2)(idx_bitwidth - 2, 0) === 15.U) -> rdata0(255, 240)
            ))

            exp_vec(i * 2 + 1) := MuxCase(0.U(expvalue_bitwidth.W), Seq(
                (subu.data_out.bits.subu_idx_vec(i * 2 + 1)(idx_bitwidth - 2, 0) === 0.U) -> rdata1(15, 0),
                (subu.data_out.bits.subu_idx_vec(i * 2 + 1)(idx_bitwidth - 2, 0) === 1.U) -> rdata1(31, 16),
                (subu.data_out.bits.subu_idx_vec(i * 2 + 1)(idx_bitwidth - 2, 0) === 2.U) -> rdata1(47, 32),
                (subu.data_out.bits.subu_idx_vec(i * 2 + 1)(idx_bitwidth - 2, 0) === 3.U) -> rdata1(63, 48),
                (subu.data_out.bits.subu_idx_vec(i * 2 + 1)(idx_bitwidth - 2, 0) === 4.U) -> rdata1(79, 64),
                (subu.data_out.bits.subu_idx_vec(i * 2 + 1)(idx_bitwidth - 2, 0) === 5.U) -> rdata1(95, 80),
                (subu.data_out.bits.subu_idx_vec(i * 2 + 1)(idx_bitwidth - 2, 0) === 6.U) -> rdata1(111, 96),
                (subu.data_out.bits.subu_idx_vec(i * 2 + 1)(idx_bitwidth - 2, 0) === 7.U) -> rdata1(127, 112),
                (subu.data_out.bits.subu_idx_vec(i * 2 + 1)(idx_bitwidth - 2, 0) === 8.U) -> rdata1(143, 128),
                (subu.data_out.bits.subu_idx_vec(i * 2 + 1)(idx_bitwidth - 2, 0) === 9.U) -> rdata1(159, 144),
                (subu.data_out.bits.subu_idx_vec(i * 2 + 1)(idx_bitwidth - 2, 0) === 10.U) -> rdata1(175, 160),
                (subu.data_out.bits.subu_idx_vec(i * 2 + 1)(idx_bitwidth - 2, 0) === 11.U) -> rdata1(191, 176),
                (subu.data_out.bits.subu_idx_vec(i * 2 + 1)(idx_bitwidth - 2, 0) === 12.U) -> rdata1(207, 192),
                (subu.data_out.bits.subu_idx_vec(i * 2 + 1)(idx_bitwidth - 2, 0) === 13.U) -> rdata1(223, 208),
                (subu.data_out.bits.subu_idx_vec(i * 2 + 1)(idx_bitwidth - 2, 0) === 14.U) -> rdata1(239, 224),
                (subu.data_out.bits.subu_idx_vec(i * 2 + 1)(idx_bitwidth - 2, 0) === 15.U) -> rdata1(255, 240)
            ))        
        }
    }.otherwise {
        for (i <- 0 until cycle_bandwidth/2) {
            lut(i).readPorts(0).enable  := false.B
            lut(i).readPorts(1).enable  := false.B
            lut(i).readPorts(0).address := 0.U
            lut(i).readPorts(1).address := 0.U
        }
    }

    val expu_data_out_valid_tmp      = RegInit(false.B)      
    expu_data_out_valid_tmp         := expu_data_in_valid
    expu_data_out_valid             := expu_data_out_valid_tmp

    val expu_data_out_bits_batch_num_tmp  = RegInit(0.U(log2Up(maxBatch).W)) 

    expu_data_out_bits_batch_num_tmp    := expu_data_in_bits_batch_num  
    expu_data_out_bits_batch_num        := expu_data_out_bits_batch_num_tmp   // 屎山代码，这里要缓一周期

    // ========================== Interpolation =====================================
    // val itru = Module(new InterpolationU(cycle_bandwidth)).io
    // itru.data_in.valid              := subu.data_out.valid
    // itru.data_in.bits.batch_num     := subu.data_out.bits.batch_num   
    // itru.data_in.bits.idx_buffer    := subu.data_out.bits.subu_idx_vec
    // itru.data_in.bits.rate_buffer   := subu.data_out.bits.subu_rate_vec
    


     // ======================= add ==========================
    val sum                 = RegInit((0.U(sum_bitwidth.W)))

    val AdderTree = Module(new AdderTree(64)).io
    AdderTree.in.valid           := expu_data_out_valid    
    AdderTree.in.bits.batch_num  := expu_data_out_bits_batch_num
    AdderTree.in.bits.data_in    := Mux(AdderTree.in.valid, exp_vec, VecInit(Seq.fill(cycle_bandwidth)(0.U((expvalue_bitwidth).W))))


    when (AdderTree.out.valid) { sum := sum + AdderTree.out.bits.sum}


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
            val seu_max_vec     = Vec(8, UInt(bitwidth.W))
            val batch_num       = UInt(log2Up(maxBatch).W)
        })
    })

    val seu_sign_vec    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(1.W))))
    val seu_exp_vec     = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((exp_bitwidth).W))))
    val seu_frac_vec    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U((frac_bitwidth).W))))
    
    val seu_max_vec     = RegInit(VecInit(Seq.fill(8)(0.U((bitwidth).W))))

    val subuValid       = RegInit(false.B)
    val batch_num       = RegInit(0.U(log2Up(maxBatch).W))

    // 对指x_max
    when (io.data_in.valid) {
        for (i <- 0 until 8) { 
            seu_max_vec(i) := Mux(4.U*i.U >= io.data_in.bits.max_in(14, 10), 
                                    Mux(io.data_in.bits.data_in(i)(11, 10) =/= 0.U, (Cat(io.data_in.bits.max_in(15), i.U, (Cat(1.U, io.data_in.bits.data_in(i)(9,  0))) >> (4.U*i.U - (io.data_in.bits.max_in(14, 10))))), // 向高对指
                                                                                        (Cat(io.data_in.bits.max_in(15), i.U, io.data_in.bits.data_in(i)(9,  0) >> (4.U*i.U - (io.data_in.bits.max_in(14, 10)))))),// 向高对指
                                    Mux(io.data_in.bits.data_in(i)(11, 10) =/= 0.U, (Cat(io.data_in.bits.max_in(15), i.U, (Cat(1.U, io.data_in.bits.data_in(i)(9,  0))) << ((io.data_in.bits.max_in(14, 10)) - 4.U*i.U))), // 向低对指
                                                                                        (Cat(io.data_in.bits.max_in(15), i.U, io.data_in.bits.data_in(i)(9,  0) << ((io.data_in.bits.max_in(14, 10)) - 4.U*i.U)))))// // 向低对指
        }
    }

    // 对指x
    when (io.data_in.valid) {
        for (i <- 0 until cycle_bandwidth) { 
            seu_sign_vec(i) := io.data_in.bits.data_in(i)(15) 
            seu_exp_vec(i)  := io.data_in.bits.data_in(i)(14, 12) // 只取exp的前三位
            seu_frac_vec(i) := Mux(io.data_in.bits.data_in(i)(11, 10) =/= 0.U, ((Cat(1.U, io.data_in.bits.data_in(i)(9,  0))) << (io.data_in.bits.data_in(i)(11, 10))), 
                                  (io.data_in.bits.data_in(i)(9,  0) << (io.data_in.bits.data_in(i)(11, 10))))
        }

    }

    subuValid                       := Mux(io.data_in.valid, true.B, false.B)
    batch_num                       := Mux(io.data_in.valid, io.data_in.bits.batch_num, batch_num)
    io.data_out.valid               := Mux(subuValid, true.B, false.B)
    io.data_out.bits.seu_sign_vec   := Mux(io.data_out.valid, seu_sign_vec, VecInit(Seq.fill(cycle_bandwidth)(0.U((1).W)))) 
    io.data_out.bits.seu_exp_vec    := Mux(io.data_out.valid, seu_exp_vec , VecInit(Seq.fill(cycle_bandwidth)(0.U((exp_bitwidth).W)))) 
    io.data_out.bits.seu_frac_vec   := Mux(io.data_out.valid, seu_frac_vec, VecInit(Seq.fill(cycle_bandwidth)(0.U((frac_bitwidth).W)))) 
    io.data_out.bits.seu_max_vec    := Mux(io.data_out.valid, seu_max_vec , VecInit(Seq.fill(8)(0.U((bitwidth).W)))) 
    io.data_out.bits.batch_num      := Mux(io.data_out.valid, batch_num   , 0.U) 
}

class SUBU(numElements: Int) extends Module {
    val io = IO(new Bundle {
        val data_in  = Flipped(Valid(new Bundle { 
            val batch_num       = UInt(log2Up(maxBatch).W)
            val seu_sign_vec    = Vec(cycle_bandwidth, UInt(1.W))
            val seu_exp_vec     = Vec(cycle_bandwidth, UInt((exp_bitwidth).W))
            val seu_frac_vec    = Vec(cycle_bandwidth, UInt((frac_bitwidth).W))
            val seu_max_vec     = Vec(8, UInt(bitwidth.W))
        }))
        val data_out  = Valid(new Bundle { 
            val batch_num       = UInt(log2Up(maxBatch).W)
            val subu_idx_vec    = Vec(cycle_bandwidth, UInt(7.W))
            val subu_rate_vec   = Vec(cycle_bandwidth, UInt((6).W))
        })
    })
    val subu_tmp_vec_w   = WireInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(frac_bitwidth.W)))) // 中间值

    val subu_idx_vec     = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(idx_bitwidth.W))))
    val subu_rate_vec    = RegInit(VecInit(Seq.fill(cycle_bandwidth)(0.U(rate_bitwidth.W))))

    val expuValid        = RegInit(false.B)
    val batch_num        = RegInit(0.U(log2Up(maxBatch).W))


    when (io.data_in.valid) {
        for (i <- 0 until cycle_bandwidth) { 
            subu_tmp_vec_w(i)   := Mux((io.data_in.bits.seu_sign_vec(i) === 0.U & io.data_in.bits.seu_max_vec(0)(15) === 0.U), io.data_in.bits.seu_max_vec(0)(9, 0) - io.data_in.bits.seu_frac_vec(i),
                                    Mux((io.data_in.bits.seu_sign_vec(i) === 1.U & io.data_in.bits.seu_max_vec(0)(15) === 1.U), io.data_in.bits.seu_frac_vec(i) - io.data_in.bits.seu_max_vec(0)(9, 0), 
                                    Mux((io.data_in.bits.seu_sign_vec(i) === 1.U) & (io.data_in.bits.seu_max_vec(0)(15) === 0.U), io.data_in.bits.seu_frac_vec(i) + io.data_in.bits.seu_max_vec(0)(9, 0), 0.U)))
            // subu_idx_vec(i)     :=  Cat(io.data_in.bits.seu_exp_vec(i), subu_tmp_vec_w(i)(9, 6))
            subu_idx_vec(i)     :=  subu_tmp_vec_w(i)(9, 5) // 取五位做索引
            subu_rate_vec(i)    :=  subu_tmp_vec_w(i)(4, 0) // 取五位做插值
        }
    }

    expuValid                       := Mux(io.data_in.valid, true.B, false.B)
    batch_num                       := Mux(io.data_in.valid, io.data_in.bits.batch_num, batch_num)
    io.data_out.valid               := Mux(expuValid, true.B, false.B)
    io.data_out.bits.batch_num      := Mux(io.data_out.valid,     batch_num, 0.U) 
    io.data_out.bits.subu_idx_vec   := Mux(io.data_out.valid,  subu_idx_vec,  VecInit(Seq.fill(cycle_bandwidth)(0.U(idx_bitwidth.W)))) 
    io.data_out.bits.subu_rate_vec  := Mux(io.data_out.valid, subu_rate_vec, VecInit(Seq.fill(cycle_bandwidth)(0.U(rate_bitwidth.W)))) 
}

// class EXPU(numElements: Int) extends Module {
//     val io = IO(new Bundle {
//         val data_in  = Flipped(Valid(new Bundle { 
//             val batch_num       = UInt(log2Up(maxBatch).W)
//             val idx_buffer      = Vec(cycle_bandwidth,  UInt(idx_bitwidth.W)) // 5
//             val rate_buffer     = Vec(cycle_bandwidth, UInt(rate_bitwidth.W)) // 5
//         }))
//         val data_out  = Valid(new Bundle { 
//             val batch_num       = UInt(log2Up(maxBatch).W)
//             val HitTable_vec    = Vec(cycle_bandwidth, UInt(HitTable_bitwidth.W))
//         })
//     })

//     val HitTable_vec    = RegInit(VecInit(Seq.fill(HitTable_size)(0.U((6).W)))) // HitTable添入的值 [5, 0]
//     val HitTable_add_w  = WireInit(VecInit(Seq.fill(HitTable_size)(VecInit(Seq.fill(cycle_bandwidth)(0.U(HitTable_bitwidth.W))))))
//     val adduValid       = RegInit(false.B)
    

//     when (io.data_in.valid) {        
//         for (i <- 0 until HitTable_size-1) {
//             for (j <- 0 until cycle_bandwidth) {
//                 when (io.data_in.bits.idx_buffer(j) === i.U) {
//                     HitTable_vec(i)   := HitTable_vec(i)   + io.data_in.bits.rate_buffer(j)
//                     HitTable_vec(i+1) := HitTable_vec(i+1) + ("h3f".U - io.data_in.bits.rate_buffer(j))
//                 }
//             }
//         } 
//     }


//     adduValid                       := io.data_in.valid
//     io.data_out.valid               := Mux(adduValid, true.B, false.B)
//     io.data_out.bits.batch_num      := Mux(io.data_out.valid, io.data_in.bits.batch_num, 0.U) 
//     io.data_out.bits.HitTable_vec   := Mux(io.data_out.valid, HitTable_vec, VecInit(Seq.fill(HitTable_size)(0.U((6).W)))) 
// }


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