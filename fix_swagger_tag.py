import os
import re

directory = r'd:\re_teamproject\server\src\main\java\com\today\fridge'

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    if '@RestController' not in content:
        return False
        
    updated = False
    filename = os.path.basename(filepath)
    classname = filename.replace('.java', '')

    if '@Tag(' not in content:
        content = content.replace('@RestController', f'@RestController\n@Tag(name = "{classname.replace("Controller", "")}", description = "{classname} API")')
        updated = True

    if updated:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Added @Tag to {filepath}")
    return updated

count = 0
for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith('Controller.java'):
            filepath = os.path.join(root, file)
            if process_file(filepath):
                count += 1

print(f"Processed {count} controllers for @Tag.")
