import os
import re

directory = r'd:\re_teamproject\server\src\main\java\com\today\fridge'

class_pattern = re.compile(r'(@RestController\s*\n\s*(?:@[A-Za-z0-9_()."=\s-]*\s*\n\s*)*)(public class \w+)')
mapping_pattern = re.compile(r'(@(?:Get|Post|Put|Delete|Patch)Mapping.*?\n\s*)(public)')

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    if '@RestController' not in content:
        return False
    if 'io.swagger.v3.oas.annotations.tags.Tag' in content:
        return False

    # Add imports
    imports = "import io.swagger.v3.oas.annotations.Operation;\nimport io.swagger.v3.oas.annotations.tags.Tag;\n"
    if 'import ' in content:
        content = content.replace('import ', imports + 'import ', 1)
    else:
        content = content.replace('package ', 'package ', 1) # simple fallback

    filename = os.path.basename(filepath)
    classname = filename.replace('.java', '')
    
    # Add @Tag
    content = class_pattern.sub(r'\1@Tag(name = "' + classname.replace('Controller', '') + r'", description = "' + classname + r' API")\n\2', content)

    # Add @Operation
    def operation_repl(match):
        mapping_str = match.group(1)
        public_str = match.group(2)
        # extract method name from the next lines
        return mapping_str + '@Operation(summary = "' + classname.replace('Controller', '') + ' API")\n    ' + public_str

    content = mapping_pattern.sub(operation_repl, content)

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"Updated {filepath}")
    return True

count = 0
for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith('Controller.java'):
            filepath = os.path.join(root, file)
            if process_file(filepath):
                count += 1

print(f"Processed {count} controllers.")
