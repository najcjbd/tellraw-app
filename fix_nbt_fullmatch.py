import re

# 读取文件
with open('app/src/main/java/com/tellraw/app/util/SelectorConverter.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 替换 fullMatch 的构造
old_line = 'val fullMatch = "nbt{$nbtContent}"'
new_line = 'val fullMatch = "nbt={" + nbtContent.substring(1, nbtContent.length - 1) + "}"'

content = content.replace(old_line, new_line)

# 写入文件
with open('app/src/main/java/com/tellraw/app/util/SelectorConverter.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("修复完成！")