# Knowledge Graph

Sơ đồ này thể hiện các **liên hệ chéo thật sự** giữa 33 module — mỗi cạnh (edge) được trích trực tiếp từ những câu "liên hệ Module X" / "đã học ở Module X" đã có sẵn trong nội dung từng file, không phải suy diễn. Xem file [`graph-data.json`](graph-data.json) nếu muốn kiểm tra nguồn dữ liệu.

Kéo để xoay góc nhìn, cuộn để zoom, bấm vào một node để mở thẳng file `.md` của module đó. Màu node ứng với 7 giai đoạn trong [Lộ trình](README.MD#lộ-trình).

<div id="graph-container" style="position: relative; width: 100%; height: 640px; border: 1px solid var(--md-default-fg-color--lightest, #ccc); border-radius: 8px; overflow: hidden;">
  <div id="graph-network" style="width: 100%; height: 100%;"></div>
  <div id="graph-loading" style="position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; font-size: 0.9rem; opacity: 0.7;">
    Đang tải sơ đồ…
  </div>
</div>

<div style="margin-top: 0.75rem; display: flex; flex-wrap: wrap; gap: 0.5rem 1.25rem; font-size: 0.85rem;">
  <span><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:#5470c6;margin-right:6px;"></span>1 · Ngôn ngữ Java</span>
  <span><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:#91cc75;margin-right:6px;"></span>2 · Thư viện chuẩn & hiện đại</span>
  <span><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:#fac858;margin-right:6px;"></span>3 · Bên dưới lớp vỏ ngôn ngữ</span>
  <span><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:#ee6666;margin-right:6px;"></span>4 · Dữ liệu</span>
  <span><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:#73c0de;margin-right:6px;"></span>5 · Spring</span>
  <span><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:#3ba272;margin-right:6px;"></span>6 · Chất lượng & vận hành</span>
  <span><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:#fc8452;margin-right:6px;"></span>7 · Kiến trúc & sự nghiệp</span>
</div>

<script src="https://unpkg.com/vis-network@9/standalone/umd/vis-network.min.js"></script>
<script>
(function () {
  const PHASE_COLORS = {
    1: "#5470c6", 2: "#91cc75", 3: "#fac858", 4: "#ee6666",
    5: "#73c0de", 6: "#3ba272", 7: "#fc8452"
  };

  fetch("graph-data.json")
    .then((res) => res.json())
    .then((data) => {
      const nodes = new vis.DataSet(
        data.nodes.map((n) => ({
          id: n.id,
          label: `${String(n.id).padStart(2, "0")} · ${n.title}`,
          color: {
            background: PHASE_COLORS[n.phase],
            border: PHASE_COLORS[n.phase],
            highlight: { background: PHASE_COLORS[n.phase], border: "#222" }
          },
          font: { color: "#1a1a1a", size: 13 },
          path: n.path,
          shape: "dot",
          size: 16
        }))
      );

      const edges = new vis.DataSet(
        data.edges.map((e) => ({
          from: e.from,
          to: e.to,
          arrows: "to",
          color: { color: "#999", opacity: 0.5, highlight: "#333" },
          smooth: { type: "continuous" }
        }))
      );

      const container = document.getElementById("graph-network");
      const network = new vis.Network(
        container,
        { nodes, edges },
        {
          physics: {
            solver: "forceAtlas2Based",
            forceAtlas2Based: { gravitationalConstant: -60, springLength: 110, avoidOverlap: 0.5 },
            stabilization: { iterations: 150 }
          },
          interaction: { hover: true, tooltipDelay: 150 }
        }
      );

      network.on("click", function (params) {
        if (params.nodes.length > 0) {
          const node = nodes.get(params.nodes[0]);
          if (node && node.path) {
            window.location.href = encodeURI(node.path);
          }
        }
      });

      document.getElementById("graph-loading").style.display = "none";
    })
    .catch((err) => {
      document.getElementById("graph-loading").textContent =
        "Không tải được graph-data.json — mở trực tiếp từ trang được deploy (MkDocs/GitHub Pages), không phải xem file .md thô.";
      console.error(err);
    });
})();
</script>

## Đọc bảng thay vì đồ thị

Nếu môi trường không chạy được JavaScript (ví dụ xem trực tiếp file `.md` trên GitHub thay vì qua trang MkDocs), dưới đây là danh sách liên hệ chéo dạng bảng, nhóm theo module nguồn:

| Module | Liên hệ tới |
|---|---|
| 07 · equals/hashCode/toString | 04 |
| 08 · Collections Framework | 07 |
| 12 · Thread cơ bản | 11 |
| 13 · Concurrency Utilities | 22 |
| 14 · Java hiện đại | 11 |
| 16 · Design Patterns | 06, 13, 14 |
| 17 · Build Tools | 06 |
| 18 · Database & SQL | 12 |
| 19 · RDBMS vs NoSQL | 08, 12, 13, 18 |
| 20 · JPA/Hibernate | 18 |
| 21 · Spring Core | 22 |
| 22 · Spring Boot | 17, 18, 21 |
| 24 · Spring Data & Persistence | 17, 18, 20, 21, 23, 27 |
| 25 · Spring Security | 21, 23 |
| 26 · Testing | 18, 21, 25 |
| 27 · Caching & Messaging | 21, 23, 24 |
| 28 · Microservices | 18, 23, 24, 26, 27 |
| 29 · DevOps cơ bản | 22, 28 |
| 30 · Observability | 15, 18, 22, 28 |
| 31 · System Design | 18, 22, 23, 24, 27, 28 |
| 32 · Bảo mật OWASP | 23, 25, 29, 30 |
| 33 · Soft Skills & Career | 18, 20, 22, 23, 25, 26, 28, 29, 30 |

Module không xuất hiện trong bảng (01, 02, 03, 04, 05, 06, 09, 10, 11, 15, 23) hiện chưa có câu "liên hệ Module X" trỏ đi hướng khác được script phát hiện chắc chắn trong nội dung — chúng vẫn được các module khác trỏ **tới** (xem cột bên phải của các dòng khác). Nội dung gốc dùng cách đánh số nội bộ cũ (không khớp số thư mục trên đĩa), nên script trích edge phải quy đổi qua bảng ánh xạ số-cũ → số-thư-mục; trường hợp không quy đổi chắc chắn được thì bị bỏ qua thay vì đoán — vì vậy danh sách này ít hơn (63 liên hệ) so với khi trích trực tiếp từ nội dung đã được đào sâu hơn.
