import torch
import numpy as np

model = torch.load('output_64x64.pt', map_location=torch.device('cpu'))
lut = model.view(torch.int16)

with open('output_64x64.hex', 'w') as f:
    counter = 0  # 计数器用于跟踪已写入的fp16数量
    line = ""  # 用于存储当前行的内容

    for row in lut:
        for num in row:
            counter += 1
            num_int = int(num.item())  # 转换为Python int，使用.item()方法正确转换每个单独的元素
            hex_str = f"{num_int & 0xFFFF:04x}"  # 将int转换为16进制，格式化为4位，填充0
            line += hex_str + " "  # 将16进制字符串添加到当前行

            if (counter % 64 == 0):  # 每64个fp16换行
                f.write(line.strip() + "\n")  # 写入当前行并换行
                line = ""  # 重置当前行字符串
    
    if line:  # 处理最后一行
        f.write(line.strip() + "\n")