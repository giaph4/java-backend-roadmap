#!/usr/bin/env python3
"""
Kiểm tra cấu trúc file .md của từng module:
  1. Heading H1 đầu file có chứa đúng số thứ tự module (khớp tiền tố thư mục NN)
     không — CHỈ CẢNH BÁO, không chặn CI. Hiện tại nhiều module (27/33) còn dùng
     số thứ tự cũ theo cách đánh số 24-module trước đây (vd thư mục "20 jpa
     hibernate" nhưng heading ghi "Module 11") — đây là việc đồng bộ nội dung
     cần làm riêng, không phải lỗi kỹ thuật cần chặn merge.
  2. Số dấu ``` trong file phải chẵn (không có code fence bị hở) — ĐÂY LÀ LỖI
     CHẶN CI vì một fence hở sẽ phá toàn bộ phần render của file sau điểm đó.

Không bắt buộc mỗi fence phải khai báo ngôn ngữ — nhiều module dùng ``` trần để vẽ
sơ đồ ASCII/cây thư mục (không phải code), đây là cách dùng hợp lệ, không phải lỗi.

Cách chạy: python scripts/check-structure.py
Exit code khác 0 CHỈ khi có lỗi fence hở — dùng trong CI để chặn PR làm hỏng render.
"""

import re
import sys
from pathlib import Path

# Ép stdout/stderr dùng UTF-8 — tránh UnicodeEncodeError khi chạy trên
# console Windows mặc định cp1252 (script in tiếng Việt có dấu).
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

ROOT = Path(__file__).resolve().parent.parent
EXCLUDE_DIRS = {".git", ".github", ".idea", "out", "site", "node_modules", "src", "docs-site"}

# Thư mục module có dạng "NN..." hoặc "NN-..." — hai chữ số đầu là số thứ tự.
MODULE_DIR_RE = re.compile(r"^(\d{2})[\s-]")


def find_module_dirs():
    dirs = []
    for entry in ROOT.iterdir():
        if not entry.is_dir() or entry.name in EXCLUDE_DIRS:
            continue
        m = MODULE_DIR_RE.match(entry.name)
        if m:
            dirs.append((int(m.group(1)), entry))
    return sorted(dirs, key=lambda x: x[0])


def find_main_md(module_dir: Path, number: int):
    """File .md chính của module — ưu tiên file có tên bắt đầu bằng số module."""
    candidates = sorted(module_dir.glob("*.md"))
    for c in candidates:
        if c.name.startswith(f"{number:02d}"):
            return c
    return candidates[0] if candidates else None


def check_heading_number(md_path: Path, number: int, errors: list):
    with md_path.open("r", encoding="utf-8") as f:
        for line in f:
            stripped = line.strip()
            if stripped.startswith("# "):
                if f"{number:02d}" not in stripped and str(number) not in stripped:
                    errors.append(
                        f"{md_path.relative_to(ROOT)}: heading H1 đầu tiên "
                        f"({stripped!r}) không chứa số module {number:02d}"
                    )
                return
    errors.append(f"{md_path.relative_to(ROOT)}: không tìm thấy heading H1 (dòng bắt đầu bằng '# ')")


FENCE_OPEN_RE = re.compile(r"^```(\S*)\s*$")


def check_code_fences(md_path: Path, errors: list):
    with md_path.open("r", encoding="utf-8") as f:
        lines = f.readlines()

    fence_count = 0
    open_stack = []  # list of (line_no, lang)

    for i, raw_line in enumerate(lines, start=1):
        line = raw_line.rstrip("\n")
        m = FENCE_OPEN_RE.match(line.strip())
        if not m:
            continue
        fence_count += 1
        lang = m.group(1)
        if not open_stack:
            open_stack.append((i, lang))
        else:
            # đóng fence đang mở
            open_stack.pop()

    if fence_count % 2 != 0:
        errors.append(
            f"{md_path.relative_to(ROOT)}: số dấu ``` là số lẻ ({fence_count}) — có code fence bị hở"
        )
    if open_stack:
        for line_no, _ in open_stack:
            errors.append(f"{md_path.relative_to(ROOT)}:{line_no}: code fence chưa được đóng")


def main():
    errors = []
    warnings = []
    modules = find_module_dirs()

    if not modules:
        print("Không tìm thấy thư mục module nào (kiểm tra lại EXCLUDE_DIRS / cấu trúc repo).")
        sys.exit(1)

    for number, module_dir in modules:
        md_path = find_main_md(module_dir, number)
        if md_path is None:
            errors.append(f"{module_dir.relative_to(ROOT)}: không có file .md nào trong thư mục")
            continue
        check_heading_number(md_path, number, warnings)
        check_code_fences(md_path, errors)

    print(f"Đã kiểm tra {len(modules)} module.")

    if warnings:
        print(
            f"\n{len(warnings)} CẢNH BÁO (không chặn CI) — heading H1 dùng số thứ tự cũ, "
            f"khác số thư mục hiện tại. Đây là việc đổi tên/đánh số nội dung riêng, "
            f"ngoài phạm vi kiểm tra cấu trúc kỹ thuật của script này:\n"
        )
        for w in warnings:
            print(f"  - {w}")

    if errors:
        print(f"\n{len(errors)} LỖI (chặn CI):\n")
        for e in errors:
            print(f"  - {e}")
        sys.exit(1)

    print("\nKhông có lỗi cấu trúc chặn CI (code fence cân đối ở mọi module).")


if __name__ == "__main__":
    main()
