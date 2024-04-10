#include "tet.h"



// 将float转换为FP16数据类型
// fp16_t float_to_fp16(float val) {
//     fp16_t result;
//     uint32_t float_bits;
//     int exponent;

//     // 将float转换为位表示
//     memcpy(&float_bits, &val, sizeof(float_bits));

//     // 提取符号位、指数位和尾数位
//     result.sign = (float_bits >> 31) & 0x1;
//     exponent = (int)((float_bits >> 23) & 0xff) - 127;
//     result.exp = exponent + 15;
//     result.frac = (float_bits >> 13) & 0x3ff;

//     // 处理溢出和舍入
//     if (exponent > 16) { // 无穷大或NaN
//         result.value = 0x7e00;
//     } else if (exponent > 0) { // 溢出
//         result.value = (result.sign | 0x7c00 | (0x3ff & (result.frac >> (24 - exponent))));
//     } else if (exponent >= -14) { // 正常范围
//         result.exp = exponent + 15;
//         result.value = (result.sign << 15) | (result.exp << 10) | result.frac;
//     } else { // 下溢出为0
//         result.value = 0;
//     }

//     return result;
// }

// fp16_t float_to_fp16(float val) {
//     fp16_t result;
//     uint32_t float_bits;

//     // 将float转换为位表示
//     memcpy(&float_bits, &val, sizeof(float_bits));

//     // 提取符号位
//     result.sign = (float_bits >> 31) & 0x1;

//     // 提取指数位和尾数位
//     int exp = (float_bits >> 23) & 0xff;
//     uint32_t frac = float_bits & 0x7fffff;

//     // 处理特殊情况
//     if (exp == 0xff) { // 无穷大或NaN
//         result.value = 0x7e00;
//     } else if (exp > 112) { // 溢出
//         result.value = (result.sign | 0x7c00 | (frac >> (24 - (exp - 112))));
//     } else if (exp >= 113 - 14) { // 正常范围
//         result.exp = exp - 112;
//         result.frac = frac >> (23 - (exp - 113));
//         result.value = (result.sign << 15) | (result.exp << 10) | result.frac;
//     } else { // 下溢出为0
//         result.value = 0;
//     }

//     return result;
// }


// fp16_t float_to_fp16(float val) {
//     fp16_t result = {0};
//     uint32_t float_bits;

//     memcpy(&float_bits, &val, sizeof(float_bits));

 

//     result.sign = (float_bits >> 31) & 0x1;
//     result.exp  = 
//     result.frac = 

//     // if (exp == 0xff) {
//     //     result.exp = 0x1f;
//     //     result.frac = 0x3ff;
//     // } else if (exp > 112) {
//     //     result.exp = 0x1e;
//     //     result.frac = (frac >> (24 - (exp - 112))) & 0x3ff;
//     // } else if (exp >= 113 - 14) {
//     //     result.exp = exp - 127 + 15;
//     //     result.frac = ((frac | 0x800000) >> (23 - (exp - 113))) & 0x3ff;
//     // } else {
//     //     result.exp = 0;
//     //     result.frac = 0;
//     // }

//     return result;
// }

// fp16_t float_to_fp16(float float_val) {
//     fp16_t result;
//     uint32_t val = (uint32_t)float_val;

//     // get the different parts of floating point number
//     uint16_t sign = val & 0x80000000;
//     uint16_t exp  = (val & 0x7f800000) >> 23;
//     uint16_t frac = val & 0x007fffff;
//     printf("conert:%8d(%1d, %4d, %4d)\n", val, sign, exp, frac);    
    
        
//     // case 1: Inf or NaN
//     if (exp == 255) {
//         // nothing to process, keep the original value
//     }
//     // case 2: value overflow, set to Inf or Clip to maximum fp16 value
//     // here we set to Inf
//     else if (exp > 142 || ((exp == 142) && (frac & 0x00001fff))) {
//         exp = 255;
//         frac = 0;
//     }
//     // case 3: normal value, cut the fractional part
//     else if (exp > 112) {
//         frac &= 0x007fe000;
//     }
//     // case 4: small value, dynamically cut the fractional part
//     else if (exp > 102) {
//         frac &= 0x007fe000 << (113 - exp);
//     }
//     // case 5: under flow value, set to zero
//     else {
//         exp = 0;
//         frac = 0;
//     }

//     // combine the parts together
//     result.value = sign | (exp << 23) | frac;

//     result.sign = sign;
//     result.exp  = exp;
//     result.frac = frac;

//     return result;
// }



// 将double转换为FP16数据类型
// fp16_t double_to_fp16(double data_in) {
//     fp16_t fp16_data;
//     fp16_data.sign     = high;
//     fp16_data.exponent = data_in & 0x1F;  // 取低五位
//     fp16_data.fraction = low;              
//     return fp16_data;  
// }

// 从FP16数据中解包出两个8位无符号整数
void fp16_unpack(fp16_t fp16, uint8_t* sign, uint8_t* exponent, uint16_t* fraction) {
    *sign     = fp16.sign    ;
    *exponent = fp16.exp;
    *fraction = fp16.frac;
}