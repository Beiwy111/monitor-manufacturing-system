# -*- coding: utf-8 -*-
"""方案B：8模块合并 + 课堂作业风格（ArrayList、//注释、RequestMapping、无Result包装）"""
import os

BASE = r"d:\soft\idea\computer\backend\src\main\java\com\upc\computer"
SCHEMA = r"d:\soft\idea\computer\schema.tsv"

MODULES = [
    {
        "name": "System",
        "prefix": "/system",
        "comment": "系统管理",
        "tables": [
            ("role", "role", "角色"),
            ("user", "user", "用户"),
            ("permission", "permission", "权限"),
            ("operation_log", "operationLog", "操作日志"),
        ],
    },
    {
        "name": "Material",
        "prefix": "/material",
        "comment": "物料库存",
        "tables": [
            ("material", "material", "物料"),
            ("bom", "bom", "BOM"),
            ("inventory", "inventory", "库存"),
            ("inventory_transaction", "transaction", "库存流水"),
        ],
    },
    {
        "name": "Order",
        "prefix": "/order",
        "comment": "订单发货",
        "tables": [
            ("customer_order", "customerOrder", "客户订单"),
            ("customer_order_item", "orderItem", "订单明细"),
            ("delivery_order", "delivery", "发货单"),
        ],
    },
    {
        "name": "Production",
        "prefix": "/production",
        "comment": "生产管理",
        "tables": [
            ("production_plan", "plan", "生产计划"),
            ("production_plan_item", "planItem", "计划明细"),
            ("process_route", "route", "工艺路线"),
            ("process_step", "step", "工序"),
            ("work_order", "workOrder", "工单"),
            ("dispatch_task", "dispatch", "派工任务"),
            ("work_report", "report", "报工"),
            ("work_progress", "progress", "生产进度"),
        ],
    },
    {
        "name": "Purchase",
        "prefix": "/purchase",
        "comment": "采购管理",
        "tables": [
            ("purchase_order", "purchaseOrder", "采购订单"),
            ("purchase_order_item", "purchaseOrderItem", "采购明细"),
        ],
    },
    {
        "name": "Quality",
        "prefix": "/quality",
        "comment": "质量管理",
        "tables": [
            ("quality_inspection", "inspection", "质量检验"),
            ("nonconforming_product", "nonconforming", "不合格品"),
        ],
    },
    {
        "name": "Equipment",
        "prefix": "/equipment",
        "comment": "设备管理",
        "tables": [
            ("equipment", "equipment", "设备"),
            ("andon_alarm", "alarm", "安灯报警"),
            ("equipment_maintenance_record", "maintenance", "设备维护记录"),
        ],
    },
    {
        "name": "AfterSales",
        "prefix": "/afterSales",
        "comment": "售后成本",
        "tables": [
            ("after_sales_case", "afterSalesCase", "售后案例"),
            ("cost_settlement", "settlement", "成本结算"),
        ],
    },
]


def snake_to_camel(s, cap_first=False):
    parts = s.split("_")
    if cap_first:
        return parts[0].capitalize() + "".join(p.capitalize() for p in parts[1:])
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def table_to_class(table):
    return snake_to_camel(table, True)


def col_to_prop(col):
    return snake_to_camel(col, False)


def path_cap(path):
    return path[0].upper() + path[1:]


def java_type(data_type, column_type):
    if data_type == "bigint":
        return "Long"
    if data_type == "varchar":
        return "String"
    if data_type == "decimal":
        return "BigDecimal"
    if data_type == "datetime":
        return "LocalDateTime"
    if data_type == "date":
        return "LocalDate"
    if data_type in ("tinyint", "int"):
        return "Integer"
    return "String"


def needs_imports(cols):
    imports = set()
    for c in cols:
        jt = java_type(c["data_type"], c["column_type"])
        if jt == "BigDecimal":
            imports.add("java.math.BigDecimal")
        elif jt == "LocalDateTime":
            imports.add("java.time.LocalDateTime")
        elif jt == "LocalDate":
            imports.add("java.time.LocalDate")
    return sorted(imports)


def load_schema():
    tables = {}
    with open(SCHEMA, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            parts = line.split("\t")
            while len(parts) < 8:
                parts.append("")
            table, col, dtype, ctype, nullable, key, default, extra = parts[:8]
            tables.setdefault(table, []).append({
                "name": col,
                "data_type": dtype,
                "column_type": ctype,
                "nullable": nullable,
                "key": key,
                "default": default,
                "extra": extra,
            })
    return tables


def pk_col(cols):
    for c in cols:
        if c["key"] == "PRI":
            return c
    return cols[0]


def quote_table(table):
    if table == "user":
        return "`user`"
    return table


def write_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)


def gen_mapper(table, cols, comment, path):
    cls = table_to_class(table)
    cap = path_cap(path)
    pk = pk_col(cols)
    pk_col_name = pk["name"]
    pk_prop = col_to_prop(pk_col_name)
    pk_type = java_type(pk["data_type"], pk["column_type"])
    tq = quote_table(table)
    col_names = [c["name"] for c in cols]
    props = [col_to_prop(c["name"]) for c in cols]
    select_cols = ", ".join(col_names)
    insert_cols = ", ".join(col_names)
    insert_vals = ", ".join(f"#{{{p}}}" for p in props)
    update_sets = ", ".join(f"{c}=#{{{col_to_prop(c)}}}" for c in col_names if c != pk_col_name)
    param_name = path if path[0].islower() else path[0].lower() + path[1:]

    lines = [
        "package com.upc.computer.mapper;",
        "",
        f"import com.upc.computer.entity.{cls};",
        "import java.util.ArrayList;",
        "import org.apache.ibatis.annotations.*;",
    ]
    for imp in needs_imports(cols):
        lines.append(f"import {imp};")
    lines.append("")
    lines.append("@Mapper")
    lines.append(f"public interface {cls}Mapper {{")
    lines.append("")
    lines.append(f"    // 查询所有{comment}")
    lines.append(f'    @Select("SELECT {select_cols} FROM {tq}")')
    lines.append(f"    public ArrayList<{cls}> {path}List();")
    lines.append("")
    lines.append(f"    // 根据主键查询{comment}")
    lines.append(f'    @Select("SELECT {select_cols} FROM {tq} WHERE {pk_col_name} = #{{{pk_prop}}}")')
    lines.append(f"    public {cls} get{cap}ById({pk_type} {pk_prop});")
    lines.append("")
    lines.append(f"    // 新增{comment}")
    lines.append(f'    @Insert("INSERT INTO {tq} ({insert_cols}) VALUES ({insert_vals})")')
    lines.append(f'    @Options(useGeneratedKeys = true, keyProperty = "{pk_prop}")')
    lines.append(f"    public void insert{cap}({cls} {param_name});")
    lines.append("")
    lines.append(f"    // 修改{comment}")
    lines.append(f'    @Update("UPDATE {tq} SET {update_sets} WHERE {pk_col_name} = #{{{pk_prop}}}")')
    lines.append(f"    public void update{cap}({cls} {param_name});")
    lines.append("")
    lines.append(f"    // 删除{comment}")
    lines.append(f'    @Delete("DELETE FROM {tq} WHERE {pk_col_name} = #{{{pk_prop}}}")')
    lines.append(f"    public void delete{cap}({pk_type} {pk_prop});")
    lines.append("")
    lines.append("}")
    lines.append("")
    write_file(os.path.join(BASE, "mapper", f"{cls}Mapper.java"), "\n".join(lines))


def gen_service_interface(module):
    name = module["name"]
    lines = ["package com.upc.computer.service;", ""]
    for table, path, tcomment in module["tables"]:
        lines.append(f"import com.upc.computer.entity.{table_to_class(table)};")
    lines.append("import java.util.ArrayList;")
    lines.append("")
    lines.append(f"public interface {name}Service {{")
    lines.append("")
    for table, path, tcomment in module["tables"]:
        cls = table_to_class(table)
        cap = path_cap(path)
        pk = pk_col(schema[table])
        pk_prop = col_to_prop(pk["name"])
        pk_type = java_type(pk["data_type"], pk["column_type"])
        param_name = path if path[0].islower() else path[0].lower() + path[1:]
        lines.append(f"    public ArrayList<{cls}> {path}List();")
        lines.append("")
        lines.append(f"    public {cls} get{cap}ById({pk_type} {pk_prop});")
        lines.append("")
        lines.append(f"    public void insert{cap}({cls} {param_name});")
        lines.append("")
        lines.append(f"    public void update{cap}({cls} {param_name});")
        lines.append("")
        lines.append(f"    public void delete{cap}({pk_type} {pk_prop});")
        lines.append("")
    lines.append("}")
    lines.append("")
    write_file(os.path.join(BASE, "service", f"{name}Service.java"), "\n".join(lines))


def gen_service_impl(module):
    name = module["name"]
    svc_var = name[0].lower() + name[1:]
    lines = [
        "package com.upc.computer.service.impl;",
        "",
        f"import com.upc.computer.service.{name}Service;",
    ]
    for table, path, tcomment in module["tables"]:
        cls = table_to_class(table)
        lines.append(f"import com.upc.computer.entity.{cls};")
        lines.append(f"import com.upc.computer.mapper.{cls}Mapper;")
    lines.extend([
        "import org.springframework.beans.factory.annotation.Autowired;",
        "import org.springframework.stereotype.Service;",
        "import java.util.ArrayList;",
        "",
        "@Service",
        f"public class {name}ServiceImpl implements {name}Service {{",
        "",
    ])
    for table, path, tcomment in module["tables"]:
        cls = table_to_class(table)
        var = cls[0].lower() + cls[1:] + "Mapper"
        lines.append("    @Autowired")
        lines.append(f"    private {cls}Mapper {var};")
        lines.append("")
    for table, path, tcomment in module["tables"]:
        cls = table_to_class(table)
        cap = path_cap(path)
        var = cls[0].lower() + cls[1:] + "Mapper"
        pk = pk_col(schema[table])
        pk_prop = col_to_prop(pk["name"])
        pk_type = java_type(pk["data_type"], pk["column_type"])
        param_name = path if path[0].islower() else path[0].lower() + path[1:]
        lines.append(f"    // 查询所有{tcomment}")
        lines.append("    @Override")
        lines.append(f"    public ArrayList<{cls}> {path}List() {{")
        lines.append(f"        return {var}.{path}List();")
        lines.append("    }")
        lines.append("")
        lines.append(f"    // 根据主键查询{tcomment}")
        lines.append("    @Override")
        lines.append(f"    public {cls} get{cap}ById({pk_type} {pk_prop}) {{")
        lines.append(f"        return {var}.get{cap}ById({pk_prop});")
        lines.append("    }")
        lines.append("")
        lines.append(f"    // 新增{tcomment}")
        lines.append("    @Override")
        lines.append(f"    public void insert{cap}({cls} {param_name}) {{")
        lines.append(f"        {var}.insert{cap}({param_name});")
        lines.append("    }")
        lines.append("")
        lines.append(f"    // 修改{tcomment}")
        lines.append("    @Override")
        lines.append(f"    public void update{cap}({cls} {param_name}) {{")
        lines.append(f"        {var}.update{cap}({param_name});")
        lines.append("    }")
        lines.append("")
        lines.append(f"    // 删除{tcomment}")
        lines.append("    @Override")
        lines.append(f"    public void delete{cap}({pk_type} {pk_prop}) {{")
        lines.append(f"        {var}.delete{cap}({pk_prop});")
        lines.append("    }")
        lines.append("")
    lines.append("}")
    lines.append("")
    write_file(os.path.join(BASE, "service", "impl", f"{name}ServiceImpl.java"), "\n".join(lines))


def gen_controller(module):
    name = module["name"]
    prefix = module["prefix"]
    svc_var = name[0].lower() + name[1:] + "Service"
    lines = [
        "package com.upc.computer.controller;",
        "",
        f"import com.upc.computer.service.{name}Service;",
    ]
    for table, path, tcomment in module["tables"]:
        lines.append(f"import com.upc.computer.entity.{table_to_class(table)};")
    lines.extend([
        "import org.springframework.beans.factory.annotation.Autowired;",
        "import org.springframework.web.bind.annotation.RequestMapping;",
        "import org.springframework.web.bind.annotation.RestController;",
        "import java.util.ArrayList;",
        "",
        "@RestController",
        f'@RequestMapping("{prefix}")',
        f"public class {name}Controller {{",
        "",
        "    @Autowired",
        f"    private {name}Service {svc_var};",
        "",
    ])
    for table, path, tcomment in module["tables"]:
        cls = table_to_class(table)
        cap = path_cap(path)
        pk = pk_col(schema[table])
        pk_prop = col_to_prop(pk["name"])
        pk_type = java_type(pk["data_type"], pk["column_type"])
        param_name = path if path[0].islower() else path[0].lower() + path[1:]
        lines.append(f"    // 查询{tcomment}列表")
        lines.append(f'    @RequestMapping("/{path}/list")')
        lines.append(f"    public ArrayList<{cls}> {path}List() {{")
        lines.append(f"        return {svc_var}.{path}List();")
        lines.append("    }")
        lines.append("")
        lines.append(f"    // 根据主键查询{tcomment}")
        lines.append(f'    @RequestMapping("/{path}/get")')
        lines.append(f"    public {cls} get{cap}ById({pk_type} {pk_prop}) {{")
        lines.append(f"        return {svc_var}.get{cap}ById({pk_prop});")
        lines.append("    }")
        lines.append("")
        lines.append(f"    // 新增{tcomment}")
        lines.append(f'    @RequestMapping("/{path}/insert")')
        lines.append(f"    public void insert{cap}({cls} {param_name}) {{")
        lines.append(f"        {svc_var}.insert{cap}({param_name});")
        lines.append("    }")
        lines.append("")
        lines.append(f"    // 修改{tcomment}")
        lines.append(f'    @RequestMapping("/{path}/update")')
        lines.append(f"    public void update{cap}({cls} {param_name}) {{")
        lines.append(f"        {svc_var}.update{cap}({param_name});")
        lines.append("    }")
        lines.append("")
        lines.append(f"    // 删除{tcomment}")
        lines.append(f'    @RequestMapping("/{path}/delete")')
        lines.append(f"    public void delete{cap}({pk_type} {pk_prop}) {{")
        lines.append(f"        {svc_var}.delete{cap}({pk_prop});")
        lines.append("    }")
        lines.append("")
    lines.append("}")
    lines.append("")
    write_file(os.path.join(BASE, "controller", f"{name}Controller.java"), "\n".join(lines))


schema = load_schema()

all_tables = {}
table_paths = {}
for m in MODULES:
    for table, path, comment in m["tables"]:
        all_tables[table] = comment
        table_paths[table] = path

for table, comment in all_tables.items():
    gen_mapper(table, schema[table], comment, table_paths[table])

for module in MODULES:
    gen_service_interface(module)
    gen_service_impl(module)
    gen_controller(module)

print("Plan B generated successfully!")
