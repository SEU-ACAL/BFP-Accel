# import struct
# from collections import defaultdict

# file_path = 'input_1024x1024.pt'

# # 初始化一个字典来统计每个指数的出现次数
# exponent_count = defaultdict(int)

# # 读取文件并解析FP16格式的数
# with open(file_path, 'rb') as file:
#     # 总共有1024*1024个FP16格式的数
#     for _ in range(1024 * 1024):
#         # 读取2个字节（16位）
#         half_float_bytes = file.read(2)
#         if not half_float_bytes:
#             break  # 如果到达文件末尾，退出循环
        
#         # 解析FP16格式的数
#         half_float = struct.unpack('<H', half_float_bytes)[0]
        
#         # 提取指数部分（5位）
#         exponent = (half_float >> 10) & 0x1F
        
#         # 在字典中累加指数计数
#         exponent_count[exponent] += 1

# # 输出每个指数的出现次数
# for exponent in range(32):  # 指数范围是0到31
#     print(f"Exponent {exponent}: {exponent_count[exponent]} times")

# import struct

# # 假设你的.pt文件名为"float16_data.pt"
# file_path = 'input_1024x1024.pt'

# # 初始化一个列表来存储每1024个数中的最大数的指数
# max_exponent_per_chunk = []

# # 读取文件并解析FP16格式的数
# with open(file_path, 'rb') as file:
#     chunk = []
#     for _ in range(1024 * 1024):  # 总共有1024*1024个FP16格式的数
#         half_float_bytes = file.read(2)
#         if not half_float_bytes:
#             break  # 如果到达文件末尾，退出循环
        
#         # 解析FP16格式的数
#         half_float = struct.unpack('<H', half_float_bytes)[0]
        
#         # 将FP16数存储到当前chunk中
#         chunk.append(half_float)
        
#         # 每1024个数处理一次
#         if len(chunk) == 1024:
#             # 找到当前chunk中的最大数
#             max_half_float = max(chunk)
            
#             # 提取最大数的指数部分（5位）
#             max_exponent = (max_half_float >> 10) & 0x1F
            
#             # 存储最大数的指数
#             max_exponent_per_chunk.append(max_exponent)
            
#             # 打印当前chunk中的最大数和它的指数，用于调试
#             print(f"Chunk max value: {max_half_float}, Max exponent: {max_exponent}")
            
#             # 重置chunk以开始下一个1024数的批次
#             chunk = []

# # 打印每1024个数中最大数的指数
# for max_exponent in max_exponent_per_chunk:
#     print(f"Max exponent in chunk: {max_exponent}")

# file_path = 'input_1024x1024.hex'

# def parse_fp16(hex_str):
#     """将16进制字符串转换为FP16数值，并提取指数部分"""
#     fp16_value = int(hex_str, 16)
#     exponent = (fp16_value >> 10) & 0x1F  # 提取5位指数
#     return exponent

# # 初始化一个列表来存储每行的最大指数
# max_exponent_per_row = []

# # 读取文件并处理每一行
# with open(file_path, 'r') as file:
#     for line in file:
#         # 分割行中的数值字符串，假设数值两两用空格隔开
#         hex_values = line.split()
#         if len(hex_values) != 1024:
#             raise ValueError("行中的数值数量不等于1024")

#         # 计算当前行的最大指数
#         max_exponent = max(parse_fp16(hex_str) for hex_str in hex_values)
        
#         # 存储每行的最大指数
#         max_exponent_per_row.append(max_exponent)

# # 打印每行的最大指数
# for index, max_exponent in enumerate(max_exponent_per_row):
#     print(f"Row {index + 1} max exponent: {max_exponent}")



# import struct

# def fp16_to_exponent(fp16_hex):
#     """Extract the exponent from a fp16 hexadecimal number."""
#     int_val = int(fp16_hex, 16)
#     # Extract the exponent bits (bits 10 to 14) and shift to the right
#     exponent = (int_val >> 10) & 0x1F
#     return exponent

# def process_hex_file(input_file, output_file):
#     with open(input_file, 'r') as infile, open(output_file, 'w') as outfile:
#         for line in infile:
#             hex_values = line.strip().split(' ')
#             exponents = [str(fp16_to_exponent(h)) for h in hex_values if h]
#             outfile.write(' '.join(exponents) + '\n')

# # 使用示例
# input_file = 'input_1024x1024.hex'
# output_file = 'output_exponents.txt'
# process_hex_file(input_file, output_file)


# def fp16_to_signed_exponent(fp16_hex):
#     """Extract the signed exponent from a fp16 hexadecimal number."""
#     int_val = int(fp16_hex, 16)
#     # Extract the sign bit (bit 15)
#     sign = (int_val >> 15) & 0x1
#     # Extract the exponent bits (bits 10 to 14) and shift to the right
#     exponent = (int_val >> 10) & 0x1F
#     # Apply the sign
#     if sign == 1:
#         exponent = -exponent
#     return exponent

# def process_hex_file(input_file, output_file):
#     with open(input_file, 'r') as infile, open(output_file, 'w') as outfile:
#         for line in infile:
#             hex_values = line.strip().split(' ')
#             exponents = [str(fp16_to_signed_exponent(h)) for h in hex_values if h]
#             outfile.write(' '.join(exponents) + '\n')

# # 使用示例
# input_file = 'input_1024x1024.hex'
# output_file = 'output_exponents.txt'
# process_hex_file(input_file, output_file)

def fp16_to_signed_exponent(fp16_hex):
    """Extract the signed exponent from a fp16 hexadecimal number."""
    int_val = int(fp16_hex, 16)
    # Extract the sign bit (bit 15)
    sign = (int_val >> 15) & 0x1
    # Extract the exponent bits (bits 10 to 14) and shift to the right
    exponent = (int_val >> 10) & 0x1F
    # Apply the sign
    if sign == 1:
        exponent = -exponent
    return exponent

def process_hex_file(input_file, output_file):
    max_exponent_count = {}

    with open(input_file, 'r') as infile, open(output_file, 'w') as outfile:
        for line in infile:
            hex_values = line.strip().split(' ')
            exponents = [fp16_to_signed_exponent(h) for h in hex_values if h]
            max_exponent = max(exponents)
            
            # Update the count of the maximum exponent
            if max_exponent in max_exponent_count:
                max_exponent_count[max_exponent] += 1
            else:
                max_exponent_count[max_exponent] = 1
            
            outfile.write(' '.join(map(str, exponents)) + '\n')
    
    return max_exponent_count

# 使用示例
input_file = 'input_1024x1024.hex'
output_file = 'output_exponents.txt'
max_exponent_count = process_hex_file(input_file, output_file)

with open('max_exponent_counts.txt', 'w') as count_file:
    for exp, count in max_exponent_count.items():
        count_file.write(f"{exp}: {count}\n")

print("每个最大值出现的次数已记录在 'max_exponent_counts.txt' 文件中")


