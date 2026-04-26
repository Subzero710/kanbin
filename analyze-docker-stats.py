#!/usr/bin/env python3
import csv
import json
import re
import statistics
import sys
from pathlib import Path

if len(sys.argv) != 2:
    print("Usage: python3 analyze-docker-stats.py <docker-stats.csv>", file=sys.stderr)
    sys.exit(2)

path = Path(sys.argv[1])
if not path.exists():
    print(f"File not found: {path}", file=sys.stderr)
    sys.exit(2)

PERCENT_RE = re.compile(r'^\s*([0-9]+(?:\.[0-9]+)?)%\s*$')
UNIT_FACTORS = {
    "B": 1,
    "kB": 10**3,
    "KB": 10**3,
    "MB": 10**6,
    "GB": 10**9,
    "TB": 10**12,
    "KiB": 2**10,
    "MiB": 2**20,
    "GiB": 2**30,
    "TiB": 2**40,
}
SIZE_RE = re.compile(r'^\s*([0-9]+(?:\.[0-9]+)?)\s*([A-Za-z]+)\s*$')

def parse_percent(text):
    if text is None:
        return None
    m = PERCENT_RE.match(text)
    return float(m.group(1)) if m else None

def parse_size(text):
    if text is None:
        return None
    m = SIZE_RE.match(text)
    if not m:
        return None
    value = float(m.group(1))
    unit = m.group(2)
    factor = UNIT_FACTORS.get(unit)
    return value * factor if factor is not None else None

def parse_usage_pair(text):
    if text is None or '/' not in text:
        return (None, None)
    left, right = [x.strip() for x in text.split('/', 1)]
    return (parse_size(left), parse_size(right))

def summarize(values):
    if not values:
        return None
    return {
        "samples": len(values),
        "min": min(values),
        "mean": statistics.fmean(values),
        "max": max(values),
    }

rows = []
with path.open(newline='', encoding='utf-8') as f:
    reader = csv.DictReader(f, delimiter='|')
    for row in reader:
        rows.append(row)

cpu_values = []
mem_used_values = []
mem_limit_values = []
mem_perc_values = []
net_in_values = []
net_out_values = []

for row in rows:
    cpu = parse_percent(row.get("cpu_perc"))
    if cpu is not None:
        cpu_values.append(cpu)

    used, limit = parse_usage_pair(row.get("mem_usage"))
    if used is not None:
        mem_used_values.append(used)
    if limit is not None:
        mem_limit_values.append(limit)

    memp = parse_percent(row.get("mem_perc"))
    if memp is not None:
        mem_perc_values.append(memp)

    net_in, net_out = parse_usage_pair(row.get("net_io"))
    if net_in is not None:
        net_in_values.append(net_in)
    if net_out is not None:
        net_out_values.append(net_out)

result = {
    "source_file": str(path),
    "container": rows[0]["container"] if rows else None,
    "sample_count": len(rows),
    "cpu_percent": summarize(cpu_values),
    "memory_used_bytes": summarize(mem_used_values),
    "memory_limit_bytes": summarize(mem_limit_values),
    "memory_percent": summarize(mem_perc_values),
    "network_in_bytes": summarize(net_in_values),
    "network_out_bytes": summarize(net_out_values),
}

print(json.dumps(result, indent=2))
