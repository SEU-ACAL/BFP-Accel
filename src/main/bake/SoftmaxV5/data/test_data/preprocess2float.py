import torch

# 加载模型
model = torch.load('input_1024x1024.pt', map_location=torch.device('cpu'))
# 将模型权重转换为float类型
lut = model.float()

# 打开文件准备写入
with open('input_1024x1024.float', 'w') as f:
    # 遍历权重矩阵的每一行
    for row in lut:
        # 将每一行的权重转换为字符串，并格式化为浮点数形式
        row_str = ' '.join(f"{num:.6f}" for num in row)
        # 写入当前行的权重到文件，并换行
        f.write(row_str + '\n')