# -*- coding: utf-8 -*-
import xlrd
p = r'C:\Users\KHRMJ\Desktop\流程(1).xls'
wb = xlrd.open_workbook(p, formatting_info=False)
print("sheets:", wb.sheet_names())
for name in wb.sheet_names():
    sh = wb.sheet_by_name(name)
    print("\n===== SHEET:", name, sh.nrows, "rows x", sh.ncols, "cols =====")
    for r in range(sh.nrows):
        vals = []
        for c in range(sh.ncols):
            v = sh.cell_value(r, c)
            if isinstance(v, float) and v == int(v):
                v = int(v)
            vals.append(str(v).strip())
        line = " | ".join(vals).rstrip(" |")
        if line.strip():
            print(f"[{r}] {line}")
