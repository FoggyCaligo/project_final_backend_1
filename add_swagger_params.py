import os
import re

directory = r'd:\re_teamproject\server\src\main\java\com\today\fridge'

# Matches @RequestParam(...) type paramName or @RequestParam type paramName
param_pattern = re.compile(r'(@(?:RequestParam|PathVariable|RequestHeader)(?:\s*\([^)]*\))?\s*)([A-Za-z0-9_<>]+)\s+([A-Za-z0-9_]+)')

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    if '@RestController' not in content:
        return False
        
    if 'io.swagger.v3.oas.annotations.Parameter' not in content:
        content = content.replace('import io.swagger.v3.oas.annotations.Operation;', 'import io.swagger.v3.oas.annotations.Operation;\nimport io.swagger.v3.oas.annotations.Parameter;')
        
    updated_content = param_pattern.sub(r'@Parameter(description = "\3") \1\2 \3', content)
    
    if updated_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(updated_content)
        print(f"Added @Parameter to {filepath}")
        return True
    return False

count = 0
for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith('Controller.java'):
            filepath = os.path.join(root, file)
            if process_file(filepath):
                count += 1

print(f"Processed {count} controllers for @Parameter.")
