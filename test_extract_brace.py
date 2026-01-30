def extractBraceContent(str):
    braceCount = 0
    result = []

    for char in str:
        if char == '{':
            braceCount += 1
            result.append(char)
        elif char == '}':
            braceCount -= 1
            result.append(char)
            if braceCount == 0:
                return ''.join(result)
        else:
            if braceCount > 0:
                result.append(char)

    return None

# 测试
test_cases = [
    "scores={kills=!5}",
    "{kills=!5}",
    "kills=!5",
    "scores={kills=!5,deaths=!10}",
]

for test in test_cases:
    print(f"输入: {test}")
    print(f"输出: {extractBraceContent(test)}")
    print()