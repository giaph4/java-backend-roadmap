#!/usr/bin/env node
// Quét toàn bộ file .md trong repo, trích các block ```mermaid ... ```,
// và thử render bằng @mermaid-js/mermaid-cli để bắt lỗi cú pháp trước khi merge.
//
// Cách chạy: node scripts/check-mermaid.mjs
// Yêu cầu: @mermaid-js/mermaid-cli đã cài sẵn (global hoặc qua `npx --yes` MỘT lần
// trước khi chạy script). Script gọi thẳng `npx --no-install` cho từng sơ đồ thay vì
// `npx --yes` lặp lại nhiều lần — gọi --yes hàng chục lần liên tiếp trong vòng lặp
// từng gây lỗi registry flaky (npm ECOMPROMISED) khi test trên máy thật.

import { readdirSync, statSync, readFileSync, writeFileSync, mkdtempSync, rmSync } from "node:fs";
import { join, relative } from "node:path";
import { tmpdir } from "node:os";
import { execFileSync } from "node:child_process";

const ROOT = process.cwd();
const EXCLUDE_DIRS = new Set([".git", ".github", ".idea", "node_modules", "out", "site", "src", "docs-site"]);

function findMarkdownFiles(dir) {
  const results = [];
  for (const entry of readdirSync(dir)) {
    if (EXCLUDE_DIRS.has(entry)) continue;
    const full = join(dir, entry);
    const stat = statSync(full);
    if (stat.isDirectory()) {
      results.push(...findMarkdownFiles(full));
    } else if (entry.toLowerCase().endsWith(".md")) {
      results.push(full);
    }
  }
  return results;
}

function extractMermaidBlocks(content) {
  const blocks = [];
  const lines = content.split("\n");
  let inBlock = false;
  let start = -1;
  let buffer = [];

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (!inBlock && /^```\s*mermaid\s*$/.test(line.trim())) {
      inBlock = true;
      start = i + 1;
      buffer = [];
      continue;
    }
    if (inBlock && /^```\s*$/.test(line.trim())) {
      inBlock = false;
      blocks.push({ startLine: start + 1, code: buffer.join("\n") });
      continue;
    }
    if (inBlock) buffer.push(line);
  }
  return blocks;
}

function resolveMmdcCommand() {
  // Ưu tiên "mmdc" đã có sẵn trên PATH (cài global — cách CI dùng, xem
  // .github/workflows/docs-ci.yml). Nếu chưa có, cài global một lần duy nhất
  // rồi dùng luôn — KHÔNG gọi "npx --yes" lặp lại trong vòng lặp vì từng gây
  // lỗi flaky với npm registry khi gọi hàng chục lần liên tiếp trên máy thật.
  try {
    execFileSync("mmdc", ["--version"], { stdio: "ignore", shell: true });
    return "mmdc";
  } catch {
    console.log("Chưa có `mmdc` trên PATH — cài @mermaid-js/mermaid-cli global (một lần)...");
    execFileSync("npm", ["install", "-g", "@mermaid-js/mermaid-cli"], {
      stdio: ["ignore", "pipe", "pipe"],
      shell: true,
    });
    return "mmdc";
  }
}

function main() {
  const files = findMarkdownFiles(ROOT);
  const tmpDir = mkdtempSync(join(tmpdir(), "mermaid-check-"));
  let totalBlocks = 0;
  let failures = [];

  const mmdcCmd = resolveMmdcCommand();

  for (const file of files) {
    const content = readFileSync(file, "utf8");
    const blocks = extractMermaidBlocks(content);
    if (blocks.length === 0) continue;

    for (const [idx, block] of blocks.entries()) {
      totalBlocks++;
      const inputPath = join(tmpDir, `block-${totalBlocks}.mmd`);
      const outputPath = join(tmpDir, `block-${totalBlocks}.svg`);
      writeFileSync(inputPath, block.code, "utf8");

      try {
        execFileSync(
          mmdcCmd,
          ["-i", inputPath, "-o", outputPath, "--quiet"],
          // shell:true cần thiết trên Windows vì bin global thực chất là "mmdc.cmd"
          // (execFileSync không tự resolve shim .cmd nếu không qua shell).
          { stdio: ["ignore", "pipe", "pipe"], shell: true }
        );
      } catch (err) {
        failures.push({
          file: relative(ROOT, file),
          line: block.startLine,
          message: err.stderr?.toString() || err.message,
        });
      }
    }
  }

  rmSync(tmpDir, { recursive: true, force: true });

  console.log(`Đã kiểm tra ${totalBlocks} sơ đồ Mermaid trong ${files.length} file .md.`);

  if (failures.length > 0) {
    console.error(`\n${failures.length} sơ đồ lỗi cú pháp:\n`);
    for (const f of failures) {
      console.error(`  ${f.file}:${f.line}`);
      console.error(`    ${f.message.split("\n")[0]}`);
    }
    process.exit(1);
  }

  console.log("Tất cả sơ đồ Mermaid hợp lệ.");
}

main();
