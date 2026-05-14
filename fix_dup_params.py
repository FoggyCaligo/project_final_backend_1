import os
import re

directory = r'd:\re_teamproject\server\src\main\java\com\today\fridge'

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Find cases where there are multiple @Parameter on the same parameter definition line
    # For example: @Parameter(description = "사용자 ID") @Parameter(description = "userId") @RequestHeader...
    # We will remove the second one that has simple description = "varName"
    
    # Regex to match lines with more than one @Parameter
    lines = content.split('\n')
    updated = False
    
    for i in range(len(lines)):
        line = lines[i]
        if line.count('@Parameter') > 1:
            # We want to remove the one that was added by the script, which is usually right before @RequestParam/etc.
            # e.g., @Parameter(description = "userId") @RequestHeader
            new_line = re.sub(r'@Parameter\(description\s*=\s*"[^"]+"\)\s*(@(?:RequestParam|PathVariable|RequestHeader|RequestBody))', r'\1', line)
            if new_line != line:
                lines[i] = new_line
                updated = True
        elif '@Parameter' in line and ('@RequestParam' in line or '@PathVariable' in line or '@RequestHeader' in line):
            # check if previous line has @Parameter
            if i > 0 and '@Parameter' in lines[i-1]:
                new_line = re.sub(r'@Parameter\(description\s*=\s*"[^"]+"\)\s*(@(?:RequestParam|PathVariable|RequestHeader|RequestBody))', r'\1', line)
                if new_line != line:
                    lines[i] = new_line
                    updated = True

    if updated:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines))
        print(f"Fixed @Parameter in {filepath}")
        return True
    return False

for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith('Controller.java'):
            filepath = os.path.join(root, file)
            process_file(filepath)
