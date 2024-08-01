# 反转顺序版
# import torch

# model = torch.load('fp16_exp_lut_shared.pt', map_location=torch.device('cpu'))
# lut = model.view(torch.int16)
# start_index = 3 * 256  # 忽略前 3*256 个数
# end_index = lut.numel() - 12 * 256  # 忽略最后 12*256 个数

# with open('fp16_input.hex', 'w') as f:
#     counter = 0  # 计数器用于跟踪已写入的fp16数量
#     lines = []  # 用于存储所有行数据

#     for row in lut:
#         line = []  # 用于存储当前行的数据

#         for num in row:
#             counter += 1
#             if counter <= start_index or counter > end_index:
#                 continue  # 跳过前 3*256 个和最后 12*256 个数

#             num_int = int(num.item())  # 转换为 Python int，使用 .item() 方法正确转换每个单独的元素
#             hex_str = f"{num_int & 0xFFFF:04x}"  # 将 int 转换为 16 进制，格式化为 4 位，填充 0
#             line.append(hex_str)  # 将 16 进制字符串添加到当前行列表

#             if len(line) == 8:  # 每 8 个 fp16 换行
#                 line.reverse()  # 反转当前行数据
#                 lines.append("".join(line))  # 将当前行添加到行列表，并使用空格分隔
#                 line = []  # 重置当前行列表

#     lines.reverse()  # 反转行列表

#     # 写入行到文件
#     for line in lines:
#         f.write(line + "\n")

# 正序版
# import torch
# import numpy as np

# model = torch.load('fp16_exp_lut_shared.pt', map_location=torch.device('cpu'))
# lut = model.view(torch.int16)

# start_index = 3 * 256  # 忽略前 3*256 个数
# end_index = lut.numel() - 12 * 256  # 忽略最后 11*256 个数

# with open('fp16_input.hex', 'w') as f:
#     counter = 0  # 计数器用于跟踪已写入的fp16数量
#     line = ""  # 用于存储当前行的内容

#     for row in lut:
#         for num in row:
#             counter += 1
#             if counter <= start_index or counter > end_index: # 去掉这两行是Full的版本
#                 continue  # 跳过前 3*256 个和最后 11*256 个数   # 去掉这两行是Full的版本
#             num_int = int(num.item())  # 转换为Python int，使用.item()方法正确转换每个单独的元素
#             hex_str = f"{num_int & 0xFFFF:04x}"  # 将int转换为16进制，格式化为4位，填充0
#             line += hex_str + " "  # 将16进制字符串添加到当前行

#             if (counter % 8 == 0):  # 每8个fp16换行
#                 f.write(line.strip() + "\n")  # 写入当前行并换行
#                 line = ""  # 重置当前行字符串
    
#     if line:  # 处理最后一行
#         f.write(line.strip() + "\n")

# 正序全表版
import torch
import numpy as np

model = torch.load('fp16_exp_lut_shared.pt', map_location=torch.device('cpu'))
lut = model.view(torch.int16)

start_index = 3 * 256  # 忽略前 3*256 个数
end_index = lut.numel() - 12 * 256  # 忽略最后 11*256 个数

with open('fp16_inputx.hex', 'w') as f:
    counter = 0  # 计数器用于跟踪已写入的fp16数量
    line = ""  # 用于存储当前行的内容

    for row in lut:
        for num in row:
            counter += 1
            num_int = int(num.item())  # 转换为Python int，使用.item()方法正确转换每个单独的元素
            hex_str = f"{num_int & 0xFFFF:04x}"  # 将int转换为16进制，格式化为4位，填充0
            line += hex_str + " "  # 将16进制字符串添加到当前行
            
            if (counter % 8 == 0):  # 每8个fp16换行
                f.write(line.strip() + "\n")  # 写入当前行并换行
                line = ""  # 重置当前行字符串
    
    if line:  # 处理最后一行
        f.write(line.strip() + "\n")