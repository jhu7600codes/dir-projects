/* dir-projects site: projects from every branch, downloads, and a git browser.
   all data comes from data/*.json made by build.py; file contents come straight
   from raw.githubusercontent.com. */
(() => {
"use strict";
const $app = document.getElementById("app");
const $q = document.getElementById("q");
let D = null;          // index.json
let C = null;          // commits array
let byH = new Map();   // sha -> commit
let refs = new Map();  // sha -> [{name,type}]
const treeCache = new Map();
const PAGE = 80;

// ---------- utils ----------
const esc = s => String(s ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const enc = encodeURIComponent;
const encPath = p => p.split("/").map(enc).join("/");
const short = h => h.slice(0, 7);
const fmtSize = n => n == null ? "" : n < 1024 ? n + " b" : n < 1048576 ? (n / 1024).toFixed(1) + " kb" : (n / 1048576).toFixed(1) + " mb";
function ago(ts) {
  const s = Date.now() / 1000 - ts;
  if (s < 60) return "just now";
  const u = [[31536000, "y"], [2592000, "mo"], [604800, "w"], [86400, "d"], [3600, "h"], [60, "m"]];
  for (const [n, l] of u) if (s >= n) return Math.floor(s / n) + l + " ago";
}
const dateStr = ts => new Date(ts * 1000).toLocaleString(undefined, { year: "numeric", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
const raw = (sha, path) => `https://raw.githubusercontent.com/${D.meta.repo}/${sha}/${encPath(path)}`;
const gh = p => `https://github.com/${D.meta.repo}${p}`;
function hue(s) { let h = 0; for (const c of s) h = (h * 31 + c.charCodeAt(0)) % 360; return h; }
const avatar = n => `<span class="av" style="background:hsl(${hue(n)} 55% 48%)">${esc(n[0].toUpperCase())}</span>`;
const set = (html) => { $app.innerHTML = html; window.scrollTo(0, 0); };
async function json(url) { const r = await fetch(url); if (!r.ok) throw new Error(url + ": " + r.status); return r.json(); }
function bar(a, d) {
  const t = a + d; if (!t) return "";
  const n = 5, ga = Math.round(a / t * n), gd = Math.min(n - ga, Math.round(d / t * n));
  let s = '<span class="bar">';
  for (let i = 0; i < n; i++) s += `<i class="${i < ga ? "a" : i < ga + gd ? "d" : ""}"></i>`;
  return s + "</span>";
}
const kindIcon = { android: "▣", ios: "", linux: "🐧", vita: "◈", python: "py", web: "◎", "gd level": "◆", code: "{}" };
const extOf = n => (n.match(/\.(tar\.gz|[a-z0-9]+)$/i) || [, ""])[1].toLowerCase();
const branchOpts = (cur) => D.branches.map(b => `<option value="${esc(b.name)}"${b.name === cur ? " selected" : ""}>${esc(b.name)}${b.name === D.meta.default ? " (default)" : ""}</option>`).join("");

// ---------- markdown ----------
function renderMd(txt, sha, base) {
  const html = DOMPurify.sanitize(marked.parse(txt, { gfm: true }));
  const div = document.createElement("div");
  div.innerHTML = html;
  const fix = (u) => {
    if (!u || /^(https?:|mailto:|#|data:)/i.test(u)) return null;
    const parts = (base ? base + "/" + u : u).split("/");
    const out = [];
    for (const p of parts) { if (p === "..") out.pop(); else if (p && p !== ".") out.push(p); }
    return out.join("/");
  };
  div.querySelectorAll("img[src]").forEach(i => { const p = fix(i.getAttribute("src")); if (p) i.src = raw(sha, p); });
  div.querySelectorAll("a[href]").forEach(a => {
    const p = fix(a.getAttribute("href"));
    if (p) { const b = D.branches.find(b => b.sha === sha); a.href = b ? `#/tree/${enc(b.name)}/${encPath(p)}` : gh(`/blob/${sha}/${encPath(p)}`); }
    else if (/^https?:/.test(a.getAttribute("href"))) { a.target = "_blank"; a.rel = "noopener"; }
  });
  div.querySelectorAll("pre code").forEach(c => { try { hljs.highlightElement(c); } catch (e) {} });
  return div.innerHTML;
}

// ---------- graph ----------
// lane layout over a date-ordered commit list. returns per-row {col, before, after}
function layout(list) {
  const rows = []; let lanes = [];
  const inList = new Set(list.map(c => c.h));
  for (const c of list) {
    const before = lanes.slice();
    let col = lanes.indexOf(c.h);
    if (col < 0) { col = lanes.indexOf(null); if (col < 0) col = lanes.length; lanes[col] = c.h; before[col] = undefined; }
    lanes = lanes.map((l, i) => (l === c.h && i !== col) ? null : l);
    const ps = c.p.filter(p => inList.has(p));
    lanes[col] = ps[0] ?? null;
    for (const p of ps.slice(1)) {
      if (lanes.includes(p)) continue;
      let j = lanes.indexOf(null); if (j < 0) j = lanes.length; lanes[j] = p;
    }
    while (lanes.length && lanes[lanes.length - 1] == null) lanes.pop();
    rows.push({ col, before, after: lanes.slice(), c, ps });
  }
  return rows;
}
const LW = 14, RH = 52;
const laneColor = i => `hsl(${(i * 67 + 255) % 360} 70% 62%)`;
function graphSvg(r, maxL) {
  const w = Math.max(1, maxL) * LW + 10, x = i => 10 + i * LW, mid = RH / 2;
  let s = `<svg class="g" width="${w}" height="${RH}" viewBox="0 0 ${w} ${RH}">`;
  r.before.forEach((sha, i) => {
    if (sha == null) return;
    if (sha === r.c.h) { s += `<path d="M${x(i)} 0 C${x(i)} ${mid / 2} ${x(r.col)} ${mid / 2} ${x(r.col)} ${mid}" stroke="${laneColor(i)}" stroke-width="2" fill="none"/>`; return; }
    const j = r.after.indexOf(sha);
    if (j >= 0) s += `<path d="M${x(i)} 0 C${x(i)} ${mid} ${x(j)} ${mid} ${x(j)} ${RH}" stroke="${laneColor(j)}" stroke-width="2" fill="none"/>`;
  });
  r.ps.forEach(p => {
    const j = r.after.indexOf(p);
    if (j >= 0) s += `<path d="M${x(r.col)} ${mid} C${x(r.col)} ${RH * .8} ${x(j)} ${RH * .7} ${x(j)} ${RH}" stroke="${laneColor(j)}" stroke-width="2" fill="none"/>`;
  });
  const merge = r.c.p.length > 1;
  s += `<circle cx="${x(r.col)}" cy="${mid}" r="${merge ? 4 : 5}" fill="${merge ? "var(--bg2)" : laneColor(r.col)}" stroke="${laneColor(r.col)}" stroke-width="2"/></svg>`;
  return s;
}
function refBadges(h) {
  const r = refs.get(h); if (!r) return "";
  return `<span class="refs">${r.map(x => x.type === "tag" ? `<a class="ref tg" href="#/tags">${esc(x.name)}</a>` : `<a class="ref" href="#/log/${enc(x.name)}">${esc(x.name)}</a>`).join("")}</span>`;
}
function commitRow(c, g) {
  return `<li style="${g ? "padding-left:0;flex-wrap:nowrap;height:" + RH + "px;min-height:0;padding-top:0;padding-bottom:0" : ""}">${g || avatar(c.an)}
    <div class="msg" style="min-width:0"><div style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis"><a href="#/c/${c.h}">${esc(c.s)}</a>${refBadges(c.h)}</div>
    <div class="small muted" style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(c.an)} · ${ago(c.ct)}</div></div>
    <a class="sha" href="#/c/${c.h}">${short(c.h)}</a></li>`;
}
function reach(from) {
  const seen = new Set(), st = [...from];
  while (st.length) { const h = st.pop(); if (seen.has(h)) continue; seen.add(h); const c = byH.get(h); if (c) st.push(...c.p); }
  return seen;
}
function commitList(list, page, base, graph = true) {
  const total = list.length, pages = Math.ceil(total / PAGE) || 1;
  page = Math.min(Math.max(1, page), pages);
  let rows, maxL = 1;
  if (graph && innerWidth > 640) { rows = layout(list); for (const r of rows.slice((page - 1) * PAGE, page * PAGE)) maxL = Math.max(maxL, r.before.length, r.after.length, r.col + 1); }
  maxL = Math.min(maxL, 26);
  const slice = list.slice((page - 1) * PAGE, page * PAGE);
  const items = slice.map((c, i) => commitRow(c, rows ? graphSvg(rows[(page - 1) * PAGE + i], maxL) : "")).join("");
  const pager = pages > 1 ? `<div class="pager">
    ${page > 1 ? `<a class="btn" href="${base}/${page - 1}">← newer</a>` : ""}
    <span class="muted small" style="align-self:center">page ${page} of ${pages}</span>
    ${page < pages ? `<a class="btn" href="${base}/${page + 1}">older →</a>` : ""}</div>` : "";
  return `<div class="panel"><ul class="list clist">${items || '<li class="empty">no commits</li>'}</ul></div>${pager}`;
}

// ---------- views ----------
const V = {};
let kindFilter = "all", sortBy = "date";
V.projects = () => {
  const kinds = ["all", ...new Set(D.projects.flatMap(p => p.kinds))];
  const nDl = D.projects.reduce((n, p) => n + p.downloads.length + p.releases.reduce((m, r) => m + r.assets.length, 0), 0);
  let ps = D.projects.filter(p => kindFilter === "all" || p.kinds.includes(kindFilter));
  if (sortBy === "name") ps = [...ps].sort((a, b) => a.name.localeCompare(b.name));
  if (sortBy === "dl") ps = [...ps].sort((a, b) => (b.downloads.length + b.releases.length) - (a.downloads.length + a.releases.length));
  set(`<div class="hero"><div><h1>projects</h1><p class="sub">every project from every branch, merged into one list. newest version wins.</p></div>
    <div class="stats"><div class="stat"><b>${D.projects.length}</b><span>projects</span></div><div class="stat"><b>${D.branches.length}</b><span>branches</span></div>
    <div class="stat"><b>${C.length}</b><span>commits</span></div><div class="stat"><b>${nDl}</b><span>downloads</span></div></div></div>
    <div class="filters">${kinds.map(k => `<button class="chip${k === kindFilter ? " on" : ""}" data-k="${esc(k)}">${esc(k)}</button>`).join("")}
    <span class="sp"></span><select class="chip" id="sort"><option value="date">recently updated</option><option value="name">name</option><option value="dl">most downloads</option></select></div>
    <div class="grid">${ps.map(card).join("")}</div>`);
  $app.querySelectorAll("[data-k]").forEach(b => b.onclick = () => { kindFilter = b.dataset.k; V.projects(); });
  const s = $app.querySelector("#sort"); s.value = sortBy; s.onchange = () => { sortBy = s.value; V.projects(); };
};
function card(p) {
  const n = p.downloads.length + p.releases.reduce((m, r) => m + r.assets.length, 0);
  return `<a class="card" href="#/p/${enc(p.name)}"><h3>${avatar(p.name)}${esc(p.title)}</h3>
    <p>${esc(p.desc || "no readme yet")}</p>
    <div class="row">${p.kinds.map(k => `<span class="tag acc">${esc(k)}</span>`).join("")}${p.version ? `<span class="tag">v${esc(p.version)}</span>` : ""}${n ? `<span class="tag ok">↓ ${n}</span>` : ""}</div>
    <div class="meta"><span>${ago(p.date)}</span><span>·</span><span>${p.versions.length} branch${p.versions.length > 1 ? "es" : ""}</span></div></a>`;
}
function dlButton(d, pri) {
  return `<a class="btn${pri ? " pri" : ""}" href="${esc(d.url)}" download>↓ ${esc(d.name)} <small>${fmtSize(d.size)}</small></a>`;
}
V.project = async (name, tab) => {
  const p = D.projects.find(p => p.name === name);
  if (!p) return set(`<div class="empty">no project called ${esc(name)}</div>`);
  const main = p.downloads.filter(d => d.branch === p.branch);
  const other = p.downloads.filter(d => d.branch !== p.branch);
  const vers = p.versions.filter(v => !v.same);
  set(`<div class="hero"><div class="row" style="gap:14px">${avatar(p.name).replace('class="av"', 'class="av" style="width:48px;height:48px;font-size:22px;border-radius:13px;background:hsl(' + hue(p.name) + ' 55% 48%)"')}
      <div><h1>${esc(p.title)}</h1><div class="row">${p.kinds.map(k => `<span class="tag acc">${esc(k)}</span>`).join("")}${p.version ? `<span class="tag">v${esc(p.version)}</span>` : ""}
      <span class="muted small">updated ${ago(p.date)} on <a href="#/log/${enc(p.branch)}">${esc(p.branch)}</a></span></div></div></div>
      <div class="row"><a class="btn" href="#/tree/${enc(p.branch)}/${enc(p.name)}">browse files</a><a class="btn" href="${gh(`/tree/${p.sha}/${encPath(p.name)}`)}" target="_blank" rel="noopener">github ↗</a></div></div>
    ${p.desc ? `<p class="sub">${esc(p.desc)}</p>` : ""}
    ${main.length || p.releases.length ? `<div class="row" style="margin-bottom:22px">${main.map((d, i) => dlButton(d, i === 0)).join("")}${(p.releases[0]?.assets || []).slice(0, 3).map(a => dlButton(a, !main.length)).join("")}</div>` : ""}
    <div class="two"><div>
      <div class="filters"><a class="chip${tab !== "commits" ? " on" : ""}" href="#/p/${enc(p.name)}">readme</a><a class="chip${tab === "commits" ? " on" : ""}" href="#/p/${enc(p.name)}/commits">commits · ${p.commits.length}</a></div>
      <div id="pmain" class="panel"><div class="loading">loading…</div></div></div>
    <aside class="side">
      ${p.downloads.length ? `<div class="panel"><div class="hd">downloads</div><ul class="list">${p.downloads.map(d => `<li><div class="t"><a href="${esc(d.url)}" download>${esc(d.name)}</a>
        <div class="small muted">${fmtSize(d.size)} · ${ago(d.date)} · <a href="#/log/${enc(d.branch)}">${esc(d.branch)}</a></div></div></li>`).join("")}</ul></div>` : ""}
      ${p.releases.length ? `<div class="panel"><div class="hd">releases</div><ul class="list">${p.releases.map(r => `<li><div class="t"><a href="${esc(r.url)}" target="_blank" rel="noopener">${esc(r.name)}</a>${r.pre ? ' <span class="tag">pre</span>' : ""}
        <div class="small muted">${r.date ? ago(Date.parse(r.date) / 1000) : ""}</div>${r.assets.map(a => `<div><a class="small" href="${esc(a.url)}">↓ ${esc(a.name)}</a> <span class="small muted">${fmtSize(a.size)}</span></div>`).join("")}</div></li>`).join("")}</ul></div>` : ""}
      <div class="panel"><div class="hd">on ${vers.length} distinct version${vers.length > 1 ? "s" : ""} · ${p.versions.length} branches</div><ul class="list">${p.versions.map(v => `<li style="${v.same ? "opacity:.55" : ""}"><div class="t">
        <a href="#/tree/${enc(v.branch)}/${enc(p.name)}">${esc(v.branch)}</a>${v.branch === p.branch ? ' <span class="tag ok">latest</span>' : ""}${v.same ? ' <span class="tag">same</span>' : ""}
        <div class="small muted">${esc(v.subject)} · ${ago(v.date)}</div></div></li>`).join("")}</ul></div>
      <div class="panel"><dl class="kv"><dt>folder</dt><dd class="mono">${esc(p.name)}/</dd><dt>files</dt><dd>${p.files}</dd><dt>last commit</dt><dd><a class="sha" href="#/c/${p.last}">${short(p.last)}</a></dd></dl></div>
    </aside></div>`);
  const box = document.getElementById("pmain");
  if (tab === "commits") {
    const list = p.commits.map(h => byH.get(h)).filter(Boolean);
    box.outerHTML = `<div>${commitList(list, 1, "", false)}</div>`;
    return;
  }
  if (!p.readme) { box.innerHTML = `<div class="empty">no readme</div>`; return; }
  try {
    const t = await (await fetch(raw(p.sha, p.readme))).text();
    box.innerHTML = `<div class="md">${renderMd(t, p.sha, p.name)}</div>`;
  } catch (e) { box.innerHTML = `<div class="err">couldn't load readme: ${esc(e.message)}</div>`; }
};

let dlFilter = "all";
V.downloads = () => {
  const all = [];
  for (const p of D.projects) {
    for (const d of p.downloads) all.push({ ...d, project: p.name, latest: d.branch === p.branch });
    for (const r of p.releases) for (const a of r.assets) all.push({ ...a, project: p.name, branch: "release " + r.tag, date: Date.parse(r.date) / 1000, latest: true, rel: r });
  }
  for (const r of D.releases) if (!D.projects.some(p => p.releases.includes(r)))
    for (const a of r.assets) all.push({ ...a, project: r.tag, branch: "release " + r.tag, date: Date.parse(r.date) / 1000, latest: true });
  const exts = ["all", ...new Set(all.map(d => extOf(d.name)).filter(Boolean))];
  const list = all.filter(d => dlFilter === "all" || extOf(d.name) === dlFilter).sort((a, b) => b.date - a.date);
  set(`<h1>downloads</h1><p class="sub">everything built: dist/ folders, loose apk/deb/vpk files and release assets, from every branch.</p>
    <div class="filters">${exts.map(e => `<button class="chip${e === dlFilter ? " on" : ""}" data-e="${esc(e)}">${e === "all" ? "all" : "." + esc(e)}</button>`).join("")}</div>
    <div class="panel"><ul class="list">${list.map(d => `<li>${avatar(d.project)}<div class="t"><a href="${esc(d.url)}" download>${esc(d.name)}</a>
      <div class="small muted"><a href="#/p/${enc(d.project)}">${esc(d.project)}</a> · ${esc(d.branch)} · ${ago(d.date)}${d.latest ? "" : " · older build"}</div></div>
      <span class="tag">.${esc(extOf(d.name))}</span><span class="small muted mono">${fmtSize(d.size)}</span>
      <a class="btn" href="${esc(d.url)}" download>↓</a></li>`).join("")}</ul></div>`);
  $app.querySelectorAll("[data-e]").forEach(b => b.onclick = () => { dlFilter = b.dataset.e; V.downloads(); });
};

V.branches = () => {
  set(`<h1>branches</h1><p class="sub">${D.branches.length} branches. ahead/behind is against <a href="#/log/${enc(D.meta.default)}">${esc(D.meta.default)}</a>.</p>
    <div class="panel"><ul class="list">${D.branches.map(b => {
      const own = D.projects.filter(p => p.branch === b.name).map(p => p.name);
      return `<li><div class="t"><a href="#/log/${enc(b.name)}">${esc(b.name)}</a>${b.name === D.meta.default ? ' <span class="tag ok">default</span>' : ""}
      <div class="small muted">${esc(b.subject)} · ${ago(b.date)}</div>
      ${own.length ? `<div class="row" style="margin-top:4px">${own.map(n => `<a class="tag acc" href="#/p/${enc(n)}">${esc(n)}</a>`).join("")}</div>` : ""}</div>
      <span class="small mono" title="ahead / behind"><span class="plus">+${b.ahead}</span> <span class="minus">−${b.behind}</span></span>
      <a class="btn" href="#/tree/${enc(b.name)}">files</a>${b.name !== D.meta.default ? `<a class="btn" href="#/compare/${enc(D.meta.default)}/${enc(b.name)}">compare</a>` : ""}</li>`;
    }).join("")}</ul></div>`);
};

V.log = (branch, page) => {
  let list = C, title = "all branches";
  if (branch) {
    const b = D.branches.find(b => b.name === branch);
    if (!b) return set(`<div class="empty">no branch ${esc(branch)}</div>`);
    const r = reach([b.sha]); list = C.filter(c => r.has(c.h)); title = branch;
  }
  set(`<div class="row" style="margin-bottom:16px"><h1 style="margin:0">commits</h1><span class="sp"></span>
    <select class="br" id="bsel"><option value="">all branches</option>${branchOpts(branch)}</select>
    ${branch ? `<a class="btn" href="#/tree/${enc(branch)}">files</a>` : ""}</div>
    <p class="sub">${list.length} commits on ${esc(title)}</p>
    ${commitList(list, page || 1, branch ? `#/log/${enc(branch)}` : "#/log/~")}`);
  const s = document.getElementById("bsel"); s.onchange = () => location.hash = s.value ? `#/log/${enc(s.value)}` : "#/log";
};

V.commit = async (h) => {
  const c = byH.get(h) || C.find(c => c.h.startsWith(h));
  if (!c) return set(`<div class="empty">no commit ${esc(h)}</div>`);
  const kids = C.filter(x => x.p.includes(c.h));
  const onBranches = D.branches.filter(b => reach([b.sha]).has(c.h)).map(b => b.name);
  set(`<div class="panel" style="margin-bottom:18px"><div class="md" style="padding:18px 20px">
      <h1 style="font-size:21px;border:0;padding:0;margin:0 0 6px">${esc(c.s)}${refBadges(c.h)}</h1>
      ${c.b ? `<pre style="white-space:pre-wrap;background:none;padding:0;margin:8px 0;color:var(--mut)">${esc(c.b)}</pre>` : ""}
      <div class="row small muted" style="margin-top:10px">${avatar(c.an)}<b style="color:var(--fg)">${esc(c.an)}</b> committed ${ago(c.ct)} · ${dateStr(c.ct)}
      <span class="sp"></span>${c.p.map(p => `parent <a class="sha" href="#/c/${p}">${short(p)}</a>`).join(" ")} <span class="sha">${c.h}</span></div>
      <div class="row small muted" style="margin-top:8px">on ${onBranches.length} branch${onBranches.length === 1 ? "" : "es"}: ${onBranches.slice(0, 8).map(n => `<a class="ref" href="#/log/${enc(n)}">${esc(n)}</a>`).join(" ")}${onBranches.length > 8 ? ` +${onBranches.length - 8}` : ""}
      ${kids.length ? ` · ${kids.length} child${kids.length > 1 ? "ren" : ""} ${kids.slice(0, 4).map(k => `<a class="sha" href="#/c/${k.h}">${short(k.h)}</a>`).join(" ")}${kids.length > 4 ? " …" : ""}` : ""}
      <span class="sp"></span><a href="${gh("/commit/" + c.h)}" target="_blank" rel="noopener">github ↗</a></div></div></div>
    <div id="diffs"><div class="loading">loading diff…</div></div>`);
  let d;
  try { d = await json(`data/c/${c.h}.json`); } catch (e) { document.getElementById("diffs").innerHTML = `<div class="err">${esc(e.message)}</div>`; return; }
  const A = d.files.reduce((n, f) => n + f.add, 0), Dl = d.files.reduce((n, f) => n + f.del, 0);
  const big = d.files.length > 25;
  document.getElementById("diffs").innerHTML = `<div class="row" style="margin-bottom:12px"><b>${d.files.length} file${d.files.length === 1 ? "" : "s"} changed</b>
    <span class="plus">+${A}</span><span class="minus">−${Dl}</span></div>
    <div class="panel" style="margin-bottom:16px"><ul class="list ft">${d.files.map((f, i) => `<li><span class="st ${f.st}">${f.st}</span>
      <div class="t"><a href="#f${i}" class="mono" data-jump="${i}">${esc(f.old ? f.old + " → " + f.path : f.path)}</a></div>
      ${f.bin ? '<span class="tag">binary</span>' : `<span class="small mono"><span class="plus">+${f.add}</span> <span class="minus">−${f.del}</span></span>${bar(f.add, f.del)}`}</li>`).join("")}</ul></div>
    ${d.files.map((f, i) => diffBlock(f, i, c, big)).join("")}`;
  document.querySelectorAll("[data-jump]").forEach(a => a.onclick = e => { e.preventDefault(); const el = document.getElementById("f" + a.dataset.jump); el.open = true; el.scrollIntoView({ behavior: "smooth" }); });
};
function diffBlock(f, i, c, collapsed) {
  let body;
  if (f.bin) body = `<div class="empty">binary file${f.st !== "D" ? ` · <a href="${raw(c.h, f.path)}" download>download</a>` : ""}</div>`;
  else if (f.skip) body = `<div class="empty">${esc(f.skip)} · <a href="${gh("/commit/" + c.h)}" target="_blank" rel="noopener">github ↗</a></div>`;
  else if (!f.diff) body = `<div class="empty">no text changes (mode or rename only)</div>`;
  else {
    let o = 0, n = 0, rows = "";
    for (const l of f.diff.replace(/\n$/, "").split("\n")) {
      if (l.startsWith("@@")) { const m = l.match(/-(\d+)(?:,\d+)? \+(\d+)/); if (m) { o = +m[1]; n = +m[2]; } rows += `<tr class="h"><td class="n"></td><td class="n"></td><td>${esc(l)}</td></tr>`; }
      else if (l.startsWith("+")) rows += `<tr class="a"><td class="n"></td><td class="n">${n++}</td><td>${esc(l)}</td></tr>`;
      else if (l.startsWith("-")) rows += `<tr class="d"><td class="n">${o++}</td><td class="n"></td><td>${esc(l)}</td></tr>`;
      else if (l.startsWith("\\")) rows += `<tr><td class="n"></td><td class="n"></td><td class="muted">${esc(l)}</td></tr>`;
      else rows += `<tr><td class="n">${o++}</td><td class="n">${n++}</td><td>${esc(l)}</td></tr>`;
    }
    body = `<div class="body"><table>${rows}</table></div>`;
  }
  return `<details class="panel diff collapse" id="f${i}"${collapsed ? "" : " open"}><summary class="hd"><span class="st ${f.st}">${f.st}</span>${esc(f.path)}<span class="sp"></span>
    ${f.bin ? "" : `<span class="plus">+${f.add}</span><span class="minus">−${f.del}</span>`}</summary>${body}</details>`;
}

async function getTree(h) { if (!treeCache.has(h)) treeCache.set(h, json(`data/t/${h}.json`)); return treeCache.get(h); }
V.tree = async (branch, path) => {
  branch = branch || D.meta.default;
  const b = D.branches.find(b => b.name === branch);
  if (!b) return set(`<div class="empty">no branch ${esc(branch)}</div>`);
  const parts = path ? path.split("/").filter(Boolean) : [];
  set(`<div class="loading">loading…</div>`);
  let h = b.tree, ent = { t: "t", h };
  try {
    for (const p of parts) {
      const ents = await getTree(h);
      ent = ents.find(e => e.n === p);
      if (!ent) throw new Error("not found: " + path);
      h = ent.h;
    }
  } catch (e) { return set(`<div class="err">${esc(e.message)}</div>`); }
  const crumbs = `<div class="crumbs"><a href="#/tree/${enc(branch)}">${esc(D.meta.repo.split("/")[1])}</a>${parts.map((p, i) => `<span>/</span>${i === parts.length - 1 ? `<b>${esc(p)}</b>` : `<a href="#/tree/${enc(branch)}/${encPath(parts.slice(0, i + 1).join("/"))}">${esc(p)}</a>`}`).join("")}</div>`;
  const head = `<div class="row" style="margin-bottom:14px"><select class="br" id="bsel">${branchOpts(branch)}</select>
    <a class="btn" href="#/log/${enc(branch)}">${C.length && reach([b.sha]).size} commits</a><span class="sp"></span>
    <a class="btn" href="${gh(`/${ent.t === "t" ? "tree" : "blob"}/${b.sha}/${encPath(parts.join("/"))}`)}" target="_blank" rel="noopener">github ↗</a></div>${crumbs}`;
  const proj = parts.length && D.projects.find(p => p.name === parts[0]);
  if (ent.t === "t") {
    const ents = await getTree(h);
    const readme = ents.find(e => /^readme(\.md)?$/i.test(e.n) && e.t === "b");
    const last = byH.get(b.sha);
    set(`${head}${proj && parts.length === 1 ? `<p class="small muted">project page: <a href="#/p/${enc(proj.name)}">${esc(proj.title)}</a></p>` : ""}
      <div class="panel"><div class="hd">${avatar(last.an)}<a href="#/c/${last.h}" style="color:var(--fg)">${esc(last.s)}</a><span class="sp"></span><a class="sha" href="#/c/${last.h}">${short(last.h)}</a><span class="muted small">${ago(last.ct)}</span></div>
      <ul class="list ft">${parts.length ? `<li class="dir"><span class="ic">↰</span><div class="t"><a href="#/tree/${enc(branch)}/${encPath(parts.slice(0, -1).join("/"))}">..</a></div></li>` : ""}
      ${ents.map(e => `<li class="${e.t === "t" ? "dir" : ""}"><span class="ic">${e.t === "t" ? "▸" : e.t === "c" ? "⎘" : "·"}</span><div class="t">
        ${e.t === "c" ? esc(e.n) + ' <span class="tag">submodule</span>' : `<a href="#/tree/${enc(branch)}/${encPath([...parts, e.n].join("/"))}">${esc(e.n)}</a>`}</div>
        ${e.t === "b" && DLX.test(e.n) ? `<a class="small" href="${raw(b.sha, [...parts, e.n].join("/"))}" download>↓</a>` : ""}<span class="sz">${e.t === "b" ? fmtSize(e.s) : ""}</span></li>`).join("")}</ul></div>
      ${readme ? `<div class="panel" style="margin-top:18px"><div class="hd">${esc(readme.n)}</div><div class="md" id="rd"><div class="loading">loading…</div></div></div>` : ""}`);
    if (readme) {
      const fp = [...parts, readme.n].join("/");
      fetch(raw(b.sha, fp)).then(r => r.text()).then(t => { const el = document.getElementById("rd"); if (el) el.innerHTML = /\.md$/i.test(readme.n) ? renderMd(t, b.sha, parts.join("/")) : `<pre>${esc(t)}</pre>`; });
    }
  } else {
    const fp = parts.join("/"), url = raw(b.sha, fp), name = parts[parts.length - 1];
    const ext = extOf(name);
    const top = `${head}<div class="panel"><div class="hd"><span class="mono">${esc(name)}</span><span class="muted small">${fmtSize(ent.s)}</span><span class="sp"></span>
      <a class="btn" href="${url}" target="_blank" rel="noopener">raw</a><a class="btn" href="${url}" download="${esc(name)}">↓ download</a></div>`;
    if (/^(png|jpe?g|gif|webp|svg|ico|bmp|avif)$/.test(ext)) return set(`${top}<img class="preview-img" src="${url}" alt="${esc(name)}"></div>`);
    if (/^(mp3|ogg|wav|m4a|opus|flac)$/.test(ext)) return set(`${top}<div class="empty"><audio controls src="${url}"></audio></div></div>`);
    if (/^(mp4|webm)$/.test(ext)) return set(`${top}<video controls style="max-width:100%;display:block;margin:auto" src="${url}"></video></div>`);
    if (DLX.test(name) || ent.s > 2_000_000 || /^(jar|so|dex|keystore|jks|ttf|otf|woff2?|pdf|bin|o|a)$/.test(ext))
      return set(`${top}<div class="empty">binary file, ${fmtSize(ent.s)}<br><br><a class="btn pri" href="${url}" download="${esc(name)}">↓ download ${esc(name)}</a></div></div>`);
    set(`${top}<div class="loading">loading…</div></div>`);
    let t;
    try { const r = await fetch(url); if (!r.ok) throw new Error(r.status); t = await r.text(); } catch (e) { return set(`${top}<div class="err">couldn't load: ${esc(e.message)}</div></div>`); }
    if (/\.(md|markdown)$/i.test(name) && !location.hash.endsWith("?src")) {
      set(`${top}<div class="md">${renderMd(t, b.sha, parts.slice(0, -1).join("/"))}</div></div>`);
      return;
    }
    if (t.includes("\u0000")) return set(`${top}<div class="empty">binary file<br><br><a class="btn pri" href="${url}" download>↓ download</a></div></div>`);
    let hl;
    try {
      const lang = { kt: "kotlin", kts: "kotlin", gradle: "groovy", py: "python", js: "javascript", ts: "typescript", sh: "bash", yml: "yaml", md: "markdown", h: "c", m: "objectivec", xm: "objectivec", mm: "objectivec", hx: "haxe", cs: "csharp" }[ext] || ext;
      hl = t.length < 400000 && hljs.getLanguage(lang) ? hljs.highlight(t, { language: lang, ignoreIllegals: true }).value : esc(t);
    } catch (e) { hl = esc(t); }
    // split highlighted html into lines while keeping spans balanced
    const lines = []; let open = [];
    for (const ln of hl.split("\n")) {
      const pre = open.join("");
      const re = /<(\/?)span([^>]*)>/g; let m;
      while ((m = re.exec(ln))) { if (m[1]) open.pop(); else open.push(`<span${m[2]}>`); }
      lines.push(pre + ln + "</span>".repeat(open.length));
    }
    if (lines.length && lines[lines.length - 1] === "") lines.pop();
    set(`${top}<div class="code hljs"><table>${lines.map((l, i) => `<tr id="L${i + 1}"><td class="ln">${i + 1}</td><td class="lc">${l || " "}</td></tr>`).join("")}</table></div></div>`);
  }
  const s = document.getElementById("bsel"); if (s) s.onchange = () => location.hash = `#/tree/${enc(s.value)}/${encPath(parts.join("/"))}`;
};
const DLX = /\.(apk|apks|xapk|aab|deb|ipa|vpk|zip|exe|msi|dmg|appimage|tar\.gz|tgz|7z|gmd|whl)$/i;

V.compare = (a, b, page) => {
  const A = D.branches.find(x => x.name === a), B = D.branches.find(x => x.name === b);
  if (!A || !B) return set(`<div class="empty">pick two branches</div>`);
  const ra = reach([A.sha]), rb = reach([B.sha]);
  const ahead = C.filter(c => rb.has(c.h) && !ra.has(c.h)), behind = C.filter(c => ra.has(c.h) && !rb.has(c.h));
  set(`<h1>compare</h1><div class="row" style="margin:10px 0 18px"><select class="br" id="ca">${branchOpts(a)}</select><span>←</span><select class="br" id="cb">${branchOpts(b)}</select></div>
    <p class="sub"><b>${esc(b)}</b> has ${ahead.length} commit${ahead.length === 1 ? "" : "s"} not on <b>${esc(a)}</b>, and is ${behind.length} behind.</p>
    ${commitList(ahead, page || 1, `#/compare/${enc(a)}/${enc(b)}`)}`);
  const sa = document.getElementById("ca"), sb = document.getElementById("cb");
  sa.onchange = sb.onchange = () => location.hash = `#/compare/${enc(sa.value)}/${enc(sb.value)}`;
};

V.tags = () => {
  set(`<h1>tags & releases</h1><p class="sub">${D.tags.length} tags, ${D.releases.length} releases</p>
    ${D.releases.length ? `<h2>releases</h2><div class="panel"><ul class="list">${D.releases.map(r => `<li><div class="t"><a href="${esc(r.url)}" target="_blank" rel="noopener">${esc(r.name)}</a> <span class="tag">${esc(r.tag)}</span>${r.pre ? ' <span class="tag">pre</span>' : ""}
      <div class="small muted">${r.date ? ago(Date.parse(r.date) / 1000) : ""}</div>
      <div class="row" style="margin-top:6px">${r.assets.map(a => dlButton(a)).join("")}</div></div></li>`).join("")}</ul></div>` : ""}
    <h2>tags</h2><div class="panel"><ul class="list">${D.tags.map(t => { const c = byH.get(t.sha); return `<li><span class="ref tg">${esc(t.name)}</span><div class="t">
      ${c ? `<a href="#/c/${c.h}">${esc(c.s)}</a><div class="small muted">${ago(c.ct)}</div>` : '<span class="muted">commit not on any branch</span>'}</div>
      <a class="sha" href="${c ? "#/c/" + t.sha : gh("/commit/" + t.sha)}">${short(t.sha)}</a></li>`; }).join("") || '<li class="empty">no tags</li>'}</ul></div>`);
};

V.search = (q) => {
  q = q.trim().toLowerCase();
  if (!q) return V.projects();
  const hi = s => esc(s).replace(new RegExp(q.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"), "gi"), m => `<mark>${m}</mark>`);
  const ps = D.projects.filter(p => (p.name + " " + p.title + " " + p.desc + " " + p.kinds.join(" ")).toLowerCase().includes(q));
  const bs = D.branches.filter(b => b.name.toLowerCase().includes(q));
  const cs = C.filter(c => (c.s + " " + c.b + " " + c.h).toLowerCase().includes(q)).slice(0, 100);
  const ds = D.projects.flatMap(p => p.downloads.map(d => ({ ...d, project: p.name }))).filter(d => d.name.toLowerCase().includes(q));
  set(`<h1>search</h1><p class="sub">“${esc(q)}”</p>
    ${ps.length ? `<h2>projects · ${ps.length}</h2><div class="grid hit">${ps.map(card).join("")}</div>` : ""}
    ${ds.length ? `<h2>downloads · ${ds.length}</h2><div class="panel hit"><ul class="list">${ds.map(d => `<li><div class="t"><a href="${esc(d.url)}" download>${hi(d.name)}</a><div class="small muted">${esc(d.project)} · ${fmtSize(d.size)}</div></div></li>`).join("")}</ul></div>` : ""}
    ${bs.length ? `<h2>branches · ${bs.length}</h2><div class="panel hit"><ul class="list">${bs.map(b => `<li><div class="t"><a href="#/log/${enc(b.name)}">${hi(b.name)}</a><div class="small muted">${esc(b.subject)}</div></div></li>`).join("")}</ul></div>` : ""}
    ${cs.length ? `<h2>commits · ${cs.length}${cs.length === 100 ? "+" : ""}</h2><div class="panel hit"><ul class="list clist">${cs.map(c => commitRow(c, "")).join("")}</ul></div>` : ""}
    ${!ps.length && !bs.length && !cs.length && !ds.length ? '<div class="empty">nothing found</div>' : ""}`);
};

// ---------- router ----------
function route() {
  if (!D) return;
  const parts = location.hash.replace(/^#\/?/, "").replace(/\?src$/, "").split("/").map(p => { try { return decodeURIComponent(p); } catch { return p; } });
  const [r, a, ...rest] = parts;
  document.querySelectorAll("[data-nav]").forEach(n => n.classList.toggle("on", n.dataset.nav === ({ "": "projects", p: "projects", c: "log", compare: "branches" }[r] ?? r)));
  if (r !== "search" && document.activeElement !== $q) $q.value = "";
  switch (r) {
    case "": return V.projects();
    case "p": return V.project(a, rest[0]);
    case "downloads": return V.downloads();
    case "branches": return V.branches();
    case "log": return a === "~" ? V.log(null, +rest[0]) : (a && isNaN(+a)) ? V.log(a, +rest[0]) : V.log(null, +a);
    case "c": return V.commit(a);
    case "tree": return V.tree(a, rest.join("/"));
    case "compare": return V.compare(a, rest[0], +rest[1]);
    case "tags": return V.tags();
    case "search": return V.search([a, ...rest].join("/"));
    default: set(`<div class="empty">nothing here. <a href="#/">go home</a></div>`);
  }
}
let qt;
$q.addEventListener("input", () => { clearTimeout(qt); qt = setTimeout(() => { location.hash = $q.value.trim() ? "#/search/" + enc($q.value) : "#/"; }, 250); });
document.addEventListener("keydown", e => { if (e.key === "/" && document.activeElement.tagName !== "INPUT" && document.activeElement.tagName !== "SELECT") { e.preventDefault(); $q.focus(); } });

// theme
const th = document.getElementById("theme");
const applyTheme = t => {
  if (t) document.documentElement.dataset.theme = t; else delete document.documentElement.dataset.theme;
  const dark = t ? t === "dark" : matchMedia("(prefers-color-scheme: dark)").matches;
  document.getElementById("hl-dark").href = `https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github${dark ? "-dark" : ""}.min.css`;
};
let saved = null; try { saved = localStorage.getItem("theme"); } catch {}
applyTheme(saved);
th.onclick = () => {
  const dark = document.documentElement.dataset.theme ? document.documentElement.dataset.theme === "dark" : matchMedia("(prefers-color-scheme: dark)").matches;
  const t = dark ? "light" : "dark"; applyTheme(t); try { localStorage.setItem("theme", t); } catch {}
};

(async () => {
  try { [D, C] = await Promise.all([json("data/index.json"), json("data/commits.json")]); }
  catch (e) { set(`<div class="err">couldn't load the site data (${esc(e.message)}). the pages workflow probably hasn't run yet.</div>`); return; }
  for (const c of C) byH.set(c.h, c);
  for (const b of D.branches) { if (!refs.has(b.sha)) refs.set(b.sha, []); refs.get(b.sha).push({ name: b.name, type: "branch" }); }
  for (const t of D.tags) { if (!refs.has(t.sha)) refs.set(t.sha, []); refs.get(t.sha).push({ name: t.name, type: "tag" }); }
  document.getElementById("foot").innerHTML = `built ${ago(D.meta.built)} from ${D.branches.length} branches of <a href="${gh("")}" target="_blank" rel="noopener">${esc(D.meta.repo)}</a> · rebuilt on every push and every few hours`;
  window.addEventListener("hashchange", route);
  route();
})();
})();
