# Đóng góp cho Java Backend Roadmap

Repo này là ghi chú cá nhân đã mở để cộng đồng tham khảo, nhưng vẫn nhận đóng góp nếu bạn phát hiện sai sót kỹ thuật hoặc có góp ý cải thiện nội dung.

## Trước khi đóng góp

- Đọc [Bộ quy tắc ứng xử](CODE_OF_CONDUCT.md).
- Kiểm tra [Issues](../../issues) và [Pull Requests](../../pulls) đang mở để tránh trùng lặp.

## Báo lỗi / góp ý (Issue)

Tạo Issue mới, nêu rõ:

1. **Module nào** (số thứ tự + tên, ví dụ: `20 — JPA / Hibernate`).
2. **Phần nào trong module** (mục lớn, đoạn code, hoặc bài tập cụ thể).
3. **Vì sao cho là chưa đúng** — trích dẫn nguồn tham khảo nếu có thể (Javadoc, spec, RFC...).

## Gửi thay đổi (Pull Request)

1. Fork repo, tạo nhánh mới từ `main` theo quy ước:
   ```text
   docs/<mô-tả-ngắn>     # cập nhật nội dung một module
   fix/<mô-tả-ngắn>      # sửa lỗi kỹ thuật/chính tả
   ```
2. Sửa nội dung. Nếu thêm sơ đồ Mermaid, kiểm tra cú pháp trước bằng:
   ```bash
   node scripts/check-mermaid.mjs
   ```
3. Nếu thêm/xoá module, chạy kiểm tra cấu trúc:
   ```bash
   python scripts/check-structure.py
   ```
4. Mở Pull Request kèm mô tả ngắn gọn thay đổi và lý do.

Không có convention commit bắt buộc, nhưng ưu tiên message mô tả rõ *module nào* thay đổi, ví dụ: `docs(module-20): sửa ví dụ N+1 query`.

## CI sẽ tự kiểm tra PR của bạn

`.github/workflows/docs-ci.yml` chạy 4 việc trên mỗi PR:

| Job | Kiểm tra gì |
|---|---|
| `markdownlint` | Định dạng Markdown (`.markdownlint.yml`) |
| `mermaid` | Cú pháp mọi sơ đồ Mermaid trong `.md` |
| `link-check` | Liên kết nội bộ giữa các file `.md` không bị gãy |
| `structure` | Mỗi thư mục module có đúng 1 file `.md` chính, code fence không bị hở |

Nếu PR fail, xem log ở tab **Actions** để biết lỗi cụ thể trước khi hỏi.

## Phạm vi đóng góp phù hợp

- ✅ Sửa lỗi kỹ thuật, bổ sung ví dụ, làm rõ khái niệm còn mơ hồ.
- ✅ Cải thiện bài tập / đáp án trong `dap-an-day-du/`.
- ✅ Cải thiện tooling (`scripts/`, CI, trang MkDocs).
- ⚠️ Đổi cấu trúc thư mục hoặc đánh số lại module — mở Issue thảo luận trước, vì ảnh hưởng liên kết chéo (`graph-data.json`, `knowledge-graph.md`).
- ❌ Dịch toàn bộ sang tiếng Anh — đang là ý tưởng chưa có lộ trình, thảo luận qua Issue trước khi bắt tay làm để tránh trùng công sức.
