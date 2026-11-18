// Раритет 2.2: rare-answer trivia. a round is a list of prompts; each answer has a rough
// "how many players would say this" percent and scores 100 - 2 * percent (min 10).
'use strict'
const $ = (id) => document.getElementById(id)
const hasDroid = typeof droid !== 'undefined'
if (hasDroid) { droid.system.orientation('portrait').catch(() => {}); droid.actionbar.hide().catch(() => {}) }

/* ---------- storage: one json blob, droid.prefs inside droid, localStorage elsewhere ---------- */
const KEY = 'raritet.state'
const fresh = () => ({
  v: 2, lang: (navigator.language || 'ru').startsWith('ru') ? 'ru' : 'en', timer: 20, sound: true, vibe: true, fling: true,
  games: 0, answers: 0, points: 0, rare: 0, langsPlayed: [],
  best: { endless: 0, blitz: 0, gamer: 0 }, days: {}, streak: 0, bestStreak: 0, lastDay: '',
  rarest: null, ach: [],
})
let S = fresh()
async function load() {
  let raw = null
  try { raw = hasDroid ? await droid.prefs.get(KEY) : localStorage.getItem(KEY) } catch {}
  try { if (raw) S = Object.assign(fresh(), JSON.parse(raw)) } catch {}
  if (!raw && !hasDroid) {
    // carry over the 1.0 stats (separate localStorage keys)
    try {
      const g = (k) => localStorage.getItem('raritet.' + k)
      if (g('games')) { S.games = +g('games') || 0; S.streak = S.bestStreak = +g('streak') || 0; S.lastDay = g('lastDay') || ''; if (g('lastDay')) S.days['ru:' + g('lastDay')] = +g('dayScore') || 0 }
    } catch {}
  }
}
function save() {
  const raw = JSON.stringify(S)
  try { if (hasDroid) droid.prefs.set(KEY, raw).catch(() => {}); else localStorage.setItem(KEY, raw) } catch {}
}

/* ---------- i18n ---------- */
const L = () => I18N[S.lang]
const t = (k, vars) => { let s = L()[k]; if (vars) for (const v in vars) s = s.replace('{' + v + '}', vars[v]); return s }
function applyLang() {
  document.documentElement.lang = S.lang
  document.title = t('title')
  for (const el of document.querySelectorAll('[data-t]')) el.textContent = t(el.dataset.t)
  $('ans').placeholder = t('placeholder')
  for (const b of $('langSw').children) b.classList.toggle('on', b.dataset.l === S.lang)
}

/* ---------- answers ---------- */
const norm = (s) => s.toLowerCase().replace(/ё/g, 'е').replace(/[«»"'`’.,!?()\-–—_/\\:;]+/g, ' ').replace(/\s+/g, ' ').trim()
const tight = (s) => norm(s).replace(/ /g, '')
const LAT = { shch: 'щ', sch: 'щ', yo: 'е', zh: 'ж', kh: 'х', ts: 'ц', ch: 'ч', sh: 'ш', yu: 'ю', ya: 'я', a: 'а', b: 'б', v: 'в', g: 'г', d: 'д', e: 'е', z: 'з', i: 'и', y: 'ы', j: 'й', k: 'к', l: 'л', m: 'м', n: 'н', o: 'о', p: 'п', r: 'р', s: 'с', t: 'т', u: 'у', f: 'ф', h: 'х', c: 'к', w: 'в', x: 'кс', q: 'к' }
const translit = (s) => s.replace(/shch|sch|yo|zh|kh|ts|ch|sh|yu|ya|[a-z]/g, (m) => LAT[m])
function lev(a, b) {
  if (Math.abs(a.length - b.length) > 2) return 9
  let p = Array.from({ length: b.length + 1 }, (_, i) => i)
  for (let i = 1; i <= a.length; i++) {
    const c = [i]
    for (let j = 1; j <= b.length; j++) c[j] = Math.min(p[j] + 1, c[j - 1] + 1, p[j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1))
    p = c
  }
  return p[b.length]
}
function parse(bank, lang, pre = '') {
  return bank.map(([q, raw], i) => {
    const seen = new Set()
    const answers = []
    for (const part of raw.split(/,\s*(?=[^,=]+=\d)/)) {
      const eq = part.lastIndexOf('=')
      const all = part.slice(0, eq).split('|').map((n) => n.trim()).filter(Boolean)
      if (!all.length || seen.has(tight(all[0]))) continue
      seen.add(tight(all[0]))
      const keys = new Set(all.map(tight))
      if (lang === 'ru') for (const n of all) keys.add(tight(translit(norm(n))))
      answers.push({ name: all[0], keys: [...keys], pct: +part.slice(eq + 1) })
    }
    answers.sort((a, b) => b.pct - a.pct)
    return { id: pre + lang + i, q, answers }
  })
}
const BANKS = {
  ru: parse(BANK_RU, 'ru'), en: parse(BANK_EN, 'en'),
  gamer: { ru: parse(BANK_GAMER.map(([r, , a]) => [r, a]), 'ru', 'g'), en: parse(BANK_GAMER.map(([, e, a]) => [e, a]), 'en', 'g') },
}
function match(prompt, input) {
  const t1 = tight(input), t2 = S.lang === 'ru' ? tight(translit(norm(input))) : t1
  if (!t1) return null
  for (const a of prompt.answers) if (a.keys.includes(t1) || a.keys.includes(t2)) return a
  let best = null, bd = 9
  for (const a of prompt.answers) for (const k of a.keys) {
    const allow = k.length >= 9 ? 2 : k.length >= 5 ? 1 : 0
    if (!allow) continue
    for (const x of [t1, t2]) { const d = lev(x, k); if (d <= allow && d < bd) { bd = d; best = a } }
  }
  return best
}
const points = (a) => (a ? Math.max(10, 100 - 2 * a.pct) : 0)
const tier = (p) => (p >= 90 ? 1 : p >= 70 ? 2 : p >= 40 ? 3 : p > 0 ? 4 : 0)
const tierCls = ['bad', 'good', 'good', 'mid', 'low']
const dotCls = ['b', 'g', 'g', 'm', 'l']

/* ---------- dates & seeded picks ---------- */
const ymd = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
const today = () => ymd(new Date())
const dayBefore = (s) => { const d = new Date(s + 'T12:00:00'); d.setDate(d.getDate() - 1); return ymd(d) }
const hash = (s) => [...s].reduce((h, c) => (Math.imul(h, 31) + c.charCodeAt(0)) >>> 0, 7)
function rng(seed) { return () => { seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0; return seed / 4294967296 } }
function shuffled(list, r = Math.random) {
  const a = list.slice()
  for (let i = a.length - 1; i > 0; i--) { const j = Math.floor(r() * (i + 1)); [a[i], a[j]] = [a[j], a[i]] }
  return a
}
// endless/zen/blitz avoid repeating the prompts you saw most recently
let recent = []
function pickFresh(n, pool = BANKS[S.lang]) {
  const notRecent = pool.filter((p) => !recent.includes(p.id))
  const out = shuffled(notRecent.length >= n ? notRecent : pool).slice(0, n)
  recent = [...recent, ...out.map((p) => p.id)].slice(-Math.floor(pool.length * 0.6))
  return out
}

/* ---------- sound & haptics ---------- */
let ac
function tone(freq, dur = 0.12, type = 'sine', when = 0, vol = 0.12) {
  if (!S.sound) return
  try {
    ac = ac || new (window.AudioContext || window.webkitAudioContext)()
    const o = ac.createOscillator(), g = ac.createGain(), t0 = ac.currentTime + when
    o.type = type; o.frequency.value = freq
    g.gain.setValueAtTime(vol, t0); g.gain.exponentialRampToValueAtTime(0.001, t0 + dur)
    o.connect(g).connect(ac.destination); o.start(t0); o.stop(t0 + dur)
  } catch {}
}
const sfx = {
  good: () => { tone(660); tone(880, 0.15, 'sine', 0.08) },
  rare: () => [523, 659, 784, 1047].forEach((f, i) => tone(f, 0.18, 'triangle', i * 0.07)),
  bad: () => tone(180, 0.25, 'sawtooth', 0, 0.06),
  tick: () => tone(1200, 0.04, 'square', 0, 0.03),
  end: () => [392, 523, 659].forEach((f, i) => tone(f, 0.25, 'triangle', i * 0.12)),
  whoosh: () => sweep(260, 900, 0.35),
  thud: () => { tone(90, 0.18, 'sine', 0, 0.25); tone(140, 0.08, 'triangle', 0, 0.1) },
}
function sweep(f1, f2, dur) {
  if (!S.sound) return
  try {
    ac = ac || new (window.AudioContext || window.webkitAudioContext)()
    const o = ac.createOscillator(), g = ac.createGain(), t0 = ac.currentTime
    o.type = 'triangle'; o.frequency.setValueAtTime(f1, t0); o.frequency.exponentialRampToValueAtTime(f2, t0 + dur)
    g.gain.setValueAtTime(0.07, t0); g.gain.exponentialRampToValueAtTime(0.001, t0 + dur)
    o.connect(g).connect(ac.destination); o.start(t0); o.stop(t0 + dur)
  } catch {}
}
function buzz(ms) { if (!S.vibe) return; if (hasDroid) droid.system.vibrate(ms).catch(() => {}); else if (navigator.vibrate) navigator.vibrate(ms) }

/* ---------- little ui helpers ---------- */
const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c])
let cur = 'home'
function show(id) { cur = id; for (const s of document.querySelectorAll('.scr')) s.classList.toggle('on', s.id === id) }
let toastT
function toast(msg) { const el = $('toast'); el.textContent = msg; el.classList.add('on'); clearTimeout(toastT); toastT = setTimeout(() => el.classList.remove('on'), 2200) }
function flash(txt, cls) { const el = $('flash'); el.className = 'flash ' + cls; el.textContent = txt; void el.offsetWidth; el.classList.add('on') }
function countUp(el, to, ms = 500) {
  const t0 = performance.now()
  const step = (n) => { const k = Math.min(1, (n - t0) / ms); el.textContent = Math.round(to * (1 - Math.pow(1 - k, 3))); if (k < 1) requestAnimationFrame(step) }
  requestAnimationFrame(step)
}
// confetti for rare answers and big scores
const fx = $('fx'), ctx = fx.getContext('2d')
let parts = [], fxOn = false
function confetti(n = 60) {
  fx.width = innerWidth * devicePixelRatio; fx.height = innerHeight * devicePixelRatio
  const cols = ['#33b5e5', '#7c5cff', '#7ed957', '#ffc542', '#ff5466']
  for (let i = 0; i < n; i++) parts.push({ x: innerWidth / 2, y: innerHeight * 0.35, vx: (Math.random() - 0.5) * 9, vy: -Math.random() * 9 - 3, r: Math.random() * 360, c: cols[i % cols.length], life: 90 })
  if (!fxOn) { fxOn = true; requestAnimationFrame(fxStep) }
}
function fxStep() {
  ctx.setTransform(devicePixelRatio, 0, 0, devicePixelRatio, 0, 0)
  ctx.clearRect(0, 0, innerWidth, innerHeight)
  parts = parts.filter((p) => --p.life > 0)
  for (const p of parts) {
    p.vy += 0.25; p.x += p.vx; p.y += p.vy; p.r += 8
    ctx.save(); ctx.translate(p.x, p.y); ctx.rotate(p.r * Math.PI / 180); ctx.globalAlpha = Math.min(1, p.life / 30)
    ctx.fillStyle = p.c; ctx.fillRect(-4, -2, 8, 4); ctx.restore()
  }
  if (parts.length) requestAnimationFrame(fxStep); else { fxOn = false; ctx.clearRect(0, 0, innerWidth, innerHeight) }
}

/* ---------- home ---------- */
function goHome() {
  stopTimer()
  if (Fling.on) { Fling.close(); $('play').appendChild($('reveal')) }
  applyLang()
  const d = S.days[S.lang + ':' + today()]
  const h = 24 - new Date().getHours()
  $('dailySub').textContent = d != null ? t('dailyDone', { s: d }) : t('dailyNew')
  $('dailyBadge').textContent = d != null ? t('dailyLeft', { h }) : today().slice(5).split('-').reverse().join('.')
  $('blitzBadge').textContent = S.best.blitz ? S.best.blitz : ''
  $('blitzBadge').style.display = S.best.blitz ? '' : 'none'
  $('gamerBadge').textContent = S.best.gamer ? S.best.gamer : 'NEW'
  $('hStreak').textContent = S.streak && (S.lastDay === today() || S.lastDay === dayBefore(today())) ? S.streak : 0
  $('hBest').textContent = Math.max(0, ...Object.values(S.days))
  $('hFound').textContent = S.rare
  show('home')
}

/* ---------- game ---------- */
let G = null, timer = null
const C = 175.9 // ring circumference
function stopTimer() { clearInterval(timer); timer = null }

function start(mode) {
  const round = mode === 'daily' ? shuffled(BANKS[S.lang], rng(hash(S.lang + today()))).slice(0, 7)
    : mode === 'blitz' ? pickFresh(40) : mode === 'gamer' ? pickFresh(7, BANKS.gamer[S.lang]) : pickFresh(7)
  G = { mode, round, at: 0, total: 0, log: [], hintUsed: false, hinted: false, locked: false, end: 0, qEnd: 0 }
  if (mode === 'blitz') G.end = Date.now() + 60000
  $('modeLbl').textContent = L().modeNames[mode]
  $('hint').style.display = mode === 'blitz' ? 'none' : ''
  $('ring').style.visibility = mode === 'zen' ? 'hidden' : ''
  if (!S.langsPlayed.includes(S.lang)) S.langsPlayed.push(S.lang)
  show('play')
  ask()
}

function drawDots() {
  if (G.mode === 'blitz') { $('dots').innerHTML = G.log.slice(-14).map((l) => `<i class="${dotCls[tier(l.pts)]}"></i>`).join('') + '<i class="cur"></i>'; return }
  $('dots').innerHTML = G.round.map((_, i) => {
    const l = G.log[i]
    return `<i class="${l ? dotCls[tier(l.pts)] : i === G.at ? 'cur' : ''}"></i>`
  }).join('')
}

function ask() {
  if (G.at >= G.round.length) return finish()
  const p = G.round[G.at]
  G.locked = false; G.hinted = false
  if (Fling.on) { Fling.close(); $('play').appendChild($('reveal')) }
  $('qNum').textContent = G.mode === 'blitz' ? t('blitzQ', { n: G.at + 1 }) : t('question', { n: G.at + 1, m: G.round.length })
  $('qText').textContent = p.q
  $('hintLine').textContent = ''
  $('scoreLbl').textContent = `${G.total} ${t('pts')}`
  $('reveal').classList.remove('on')
  $('ans').value = ''; $('ans').disabled = false; $('ans').classList.remove('shake')
  $('go').disabled = false; $('skip').disabled = false
  $('hint').disabled = G.hintUsed
  $('hint').textContent = G.hintUsed ? t('hintUsed') : t('hint')
  drawDots()
  $('ans').focus()
  if (G.mode !== 'blitz') G.qEnd = Date.now() + S.timer * 1000
  stopTimer()
  if (G.mode !== 'zen') { timer = setInterval(tick, 100); tick() }
}

let lastSec = -1
function tick() {
  const total = G.mode === 'blitz' ? 60000 : S.timer * 1000
  const end = G.mode === 'blitz' ? G.end : G.qEnd
  const left = Math.max(0, end - Date.now())
  $('ringPr').style.strokeDashoffset = C * (1 - left / total)
  const sec = Math.ceil(left / 1000)
  $('ringTxt').textContent = sec
  $('ring').classList.toggle('low', left < 5000)
  if (sec !== lastSec && sec <= 5 && sec > 0) sfx.tick()
  lastSec = sec
  if (!left) {
    stopTimer()
    if (G.mode === 'blitz') finish(); else submit('time')
  }
}

function useHint() {
  if (G.hintUsed || G.locked) return
  const p = G.round[G.at]
  const rare = p.answers.filter((a) => a.pct <= 2)
  const a = (rare.length ? rare : p.answers)[Math.floor(Math.random() * (rare.length || p.answers.length))]
  const w = a.name.split(' ').map((word, wi) => [...word].map((c, ci) => (wi === 0 && ci === 0 ? c.toUpperCase() : c === '-' ? '-' : '_')).join(' ')).join('   ')
  $('hintLine').textContent = t('hintText', { w, n: a.name.replace(/[\s-]/g, '').length })
  G.hintUsed = true; G.hinted = true
  $('hint').disabled = true; $('hint').textContent = t('hintUsed')
  $('ans').focus()
}

function record(a, raw, why) {
  const p = G.round[G.at]
  let pts = points(a)
  if (G.hinted) pts = Math.round(pts / 2)
  G.total += pts
  G.log.push({ p, a, raw, pts, why, hinted: G.hinted })
  if (a) {
    S.answers++; S.points += pts
    if (pts >= 90) S.rare++
    if (!S.rarest || a.pct < S.rarest.pct || (a.pct === S.rarest.pct && Math.random() < 0.3)) S.rarest = { name: a.name, q: p.q, pct: a.pct }
  }
  return pts
}

function submit(why) {
  if (G.locked) return
  const raw = $('ans').value.trim()
  if (why === 'answer' && !raw) { $('ans').classList.remove('shake'); void $('ans').offsetWidth; $('ans').classList.add('shake'); return }
  const p = G.round[G.at]
  const a = why === 'skip' ? null : match(p, raw)

  if (G.mode === 'blitz') {
    // blitz: wrong answers just shake, so you can try again; skip moves on
    if (!a && why === 'answer') { $('ans').classList.remove('shake'); void $('ans').offsetWidth; $('ans').classList.add('shake'); buzz(60); sfx.bad(); return }
    const pts = record(a, raw, why)
    if (a) { flash('+' + pts, tierCls[tier(pts)]); pts >= 90 ? sfx.rare() : sfx.good(); buzz(25) }
    G.at++
    if (G.at >= G.round.length) G.round.push(...pickFresh(20))
    return ask()
  }

  G.locked = true
  stopTimer()
  const pts = record(a, raw, why)
  $('ans').disabled = true; $('go').disabled = true; $('skip').disabled = true; $('hint').disabled = true
  $('scoreLbl').textContent = `${G.total} ${t('pts')}`
  drawDots()
  const tr = tier(pts)
  const v = $('vPts')
  v.className = 'pts ' + tierCls[tr]
  if (a) { v.textContent = '+0'; countUp({ set textContent(x) { v.textContent = '+' + x } }, pts) } else v.textContent = '0'
  $('vLbl').textContent = a ? t('v' + tr) : why === 'time' ? t('timeUp') : why === 'skip' ? t('skipped') : t('notFound')
  $('vLbl').className = 'lbl ' + tierCls[tr]
  $('vSub').textContent = a ? t('saidBy', { a: a.name, p: a.pct }) : ''
  $('vList').innerHTML = answerBars(p, a)
  $('next').textContent = G.at === G.round.length - 1 ? t('results') : t('next')
  $('reveal').classList.add('on')
  const react = () => { if (a && pts >= 90) { sfx.rare(); confetti(50); buzz(80) } else if (a) { sfx.good(); buzz(25) } else { sfx.bad(); buzz(120) } $('next').focus() }
  if (!S.fling) {
    requestAnimationFrame(() => { for (const f of $('vList').querySelectorAll('.fill')) f.style.width = f.dataset.w })
    return react()
  }
  // the throw: distance in meters = points
  $('flSheet').appendChild($('reveal'))
  $('flQ').textContent = p.q
  $('flUnit').textContent = t('m')
  const shown = [...p.answers.slice(0, 5), p.answers[p.answers.length - 1]].filter((x, i, arr) => x !== a && arr.indexOf(x) === i)
  Fling.open({
    flags: shown.map((x) => ({ name: x.name, pct: x.pct, dist: points(x) })),
    mine: a ? { dist: pts } : null,
    label: a ? a.name : raw || '...',
    unit: t('m'), zones: L().zones,
    onLaunch: () => { sfx.whoosh(); buzz(15) },
    onLand: () => { sfx.thud(); buzz(40) },
    onDone: react,
  })
}

function answerBars(p, mine) {
  const max = p.answers[0].pct
  const row = (a) => `<div class="ans${a === mine ? ' me' : ''}"><div class="fill" style="width:0" data-w="${Math.max(4, a.pct / max * 100)}%"></div><span>${esc(a.name)}${a === mine ? ' · ' + t('yours') : ''}</span><span class="pc">${a.pct}% · ${points(a)}</span></div>`
  const top = p.answers.slice(0, 5)
  let h = top.map(row).join('')
  if (mine && !top.includes(mine)) h += `<div class="sep">···</div>` + row(mine)
  else {
    const rare = p.answers[p.answers.length - 1]
    if (!top.includes(rare)) h += `<div class="sep">···</div>` + row(rare)
  }
  return h
}

function nextQ() { if (!G || !G.locked) return; G.at++; ask() }

function finish() {
  stopTimer()
  if (Fling.on) { Fling.close(); $('play').appendChild($('reveal')) }
  const g = G, n = g.mode === 'blitz' ? g.log.length : g.round.length
  // stats
  S.games++
  const newAch = []
  const unlock = (k) => { if (!S.ach.includes(k)) { S.ach.push(k); newAch.push(k) } }
  if (g.mode === 'daily') {
    const k = S.lang + ':' + today()
    const firstToday = S.lastDay !== today()
    S.days[k] = Math.max(S.days[k] || 0, g.total)
    if (firstToday) { S.streak = S.lastDay === dayBefore(today()) ? S.streak + 1 : 1; S.lastDay = today(); S.bestStreak = Math.max(S.bestStreak, S.streak) }
  } else if (g.mode === 'endless') S.best.endless = Math.max(S.best.endless, g.total)
  else if (g.mode === 'gamer') S.best.gamer = Math.max(S.best.gamer || 0, g.total)
  else if (g.mode === 'blitz') S.best.blitz = Math.max(S.best.blitz, g.total)
  const correct = g.log.filter((l) => l.a).length
  unlock('first')
  if (g.log.some((l) => l.pts >= 98)) unlock('rare')
  if (g.log.filter((l) => l.pts >= 80).length >= 5) unlock('five')
  if (g.mode !== 'blitz' && g.total >= 400) unlock('s400')
  if (g.mode !== 'blitz' && g.total >= 550) unlock('s550')
  if (g.mode !== 'blitz' && correct === 7 && !g.hintUsed) unlock('perfect')
  if (S.streak >= 3) unlock('streak3')
  if (S.streak >= 7) unlock('streak7')
  if (g.mode === 'blitz' && correct >= 10) unlock('blitz10')
  if (S.games >= 25) unlock('games25')
  if (S.answers >= 100) unlock('found100')
  if (S.langsPlayed.length >= 2) unlock('bilingual')
  save()

  // screen
  const max = n * 100
  const avg = n ? g.total / n : 0
  $('rMode').textContent = L().modeNames[g.mode] + (g.mode === 'daily' ? ' · ' + today() : '')
  $('rTotal').textContent = '0'; countUp($('rTotal'), g.total, 900)
  $('rRank').textContent = L().ranks[avg >= 80 ? 4 : avg >= 65 ? 3 : avg >= 50 ? 2 : avg >= 30 ? 1 : 0]
  $('rOf').textContent = g.mode === 'blitz' ? `${correct} ✓ · ${t('of', { m: max })}` : t('of', { m: max })
  $('rList').innerHTML = g.log.map((l, i) => {
    const rare = l.p.answers[l.p.answers.length - 1]
    return `<div class="ri" data-i="${i}"><div class="top"><span class="q">${esc(l.p.q)}</span><span class="p ${tierCls[tier(l.pts)]}">${l.pts}</span></div>` +
      `<div class="a">${t('yourAns', { a: esc(l.a ? l.a.name + ' (' + l.a.pct + '%)' : l.raw || t('noAns')) })} · ${t('rarest', { a: esc(rare.name) })}</div>` +
      `<div class="more">${answerBars(l.p, l.a)}</div></div>`
  }).join('')
  show('res')
  sfx.end()
  if (avg >= 65) setTimeout(() => confetti(90), 300)
  newAch.forEach((k, i) => setTimeout(() => toast(t('newAch', { a: L().ach[k][0] })), 900 + i * 2400))
}

$('rList').addEventListener('click', (e) => {
  const it = e.target.closest('.ri'); if (!it) return
  it.classList.toggle('open')
  for (const f of it.querySelectorAll('.fill')) f.style.width = it.classList.contains('open') ? f.dataset.w : '0'
})

function shareText() {
  const g = G, marks = g.log.map((l) => ['·', '■', '■', '▣', '□'][tier(l.pts)]).join('')
  const head = `${t('title')} · ${L().modeNames[g.mode]}${g.mode === 'daily' ? ' ' + today() : ''}`
  return `${head}\n${g.total} ${t('pts')} · ${$('rRank').textContent}\n${marks}`
}
async function share() {
  const txt = shareText()
  try {
    if (hasDroid) { await droid.system.clipboard.write(txt); toast(t('copied')) }
    else if (window.RaritetApp) RaritetApp.share(txt)
    else if (navigator.share) await navigator.share({ text: txt })
    else { await navigator.clipboard.writeText(txt); toast(t('copied')) }
  } catch {}
}

/* ---------- stats ---------- */
function renderStats() {
  const avg = S.answers ? Math.round(S.points / S.answers) : 0
  const dayVals = Object.entries(S.days).filter(([k]) => k.startsWith(S.lang + ':'))
  const bestDaily = Math.max(0, ...dayVals.map(([, v]) => v))
  const stat = (v, k) => `<div class="stat"><b>${v}</b><small>${t(k)}</small></div>`
  let days = []
  for (let i = 13, d = today(); i >= 0; i--) { days.unshift(d); d = dayBefore(d) }
  days = days.slice(-14)
  const vals = days.map((d) => S.days[S.lang + ':' + d] || 0)
  const top = Math.max(700, ...vals)
  const chart = days.map((d, i) => `<div class="c${vals[i] ? '' : ' z'}" style="height:${vals[i] ? Math.max(6, vals[i] / top * 100) : 3}%"><em>${+d.slice(8)}</em></div>`).join('')
  const ach = Object.entries(L().ach).map(([k, [name, desc]]) => `<div class="${S.ach.includes(k) ? 'on' : ''}"><b>${esc(name)}</b><small>${esc(desc)}</small></div>`).join('')
  $('statsBody').innerHTML =
    `<div class="grid2">${stat(S.games, 'sGames')}${stat(S.answers, 'sAnswers')}${stat(avg, 'sAvg')}${stat(S.rare, 'sRare')}${stat(bestDaily, 'sBestDaily')}${stat(S.best.endless, 'sBestEndless')}${stat(S.best.blitz, 'sBestBlitz')}${stat(S.best.gamer || 0, 'sBestGamer')}${stat(S.bestStreak, 'sStreak')}</div>` +
    `<div class="section">${t('last14')}</div><div class="chart">${chart}</div>` +
    `<div class="section">${t('rarestFind')}</div><div class="stat">${S.rarest ? `<b class="good">${esc(S.rarest.name)}</b><small>${esc(S.rarest.q)} · ${S.rarest.pct}%</small>` : `<small>${t('none')}</small>`}</div>` +
    `<div class="section">${t('achievements')} · ${S.ach.length}/${Object.keys(L().ach).length}</div><div class="ach">${ach}</div>`
  show('stats')
}

function renderHow() {
  $('howBody').innerHTML = L().howText.map(([h, p]) => `<div class="stat" style="margin-bottom:8px"><b style="font-size:16px;margin-bottom:4px">${esc(h)}</b><small style="font-size:14px;line-height:1.5">${esc(p)}</small></div>`).join('')
  show('how')
}

/* ---------- settings ---------- */
function renderSettings() {
  const seg = (key, opts, fmt) => `<div class="seg">${opts.map((o) => `<button data-k="${key}" data-v="${o}" class="${S[key] === o ? 'on' : ''}">${fmt(o)}</button>`).join('')}</div>`
  const sw = (key) => `<button class="sw${S[key] ? ' on' : ''}" data-sw="${key}"></button>`
  $('setBody').innerHTML =
    `<div class="set"><span>${t('language')}</span>${seg('lang', ['ru', 'en'], (o) => (o === 'ru' ? 'Русский' : 'English'))}</div>` +
    `<div class="set"><span>${t('timer')}</span>${seg('timer', [15, 20, 30], (o) => o + ' ' + t('sec'))}</div>` +
    `<div class="set"><span>${t('sound')}</span>${sw('sound')}</div>` +
    `<div class="set"><span>${t('vibration')}</span>${sw('vibe')}</div>` +
    `<div class="set"><span>${t('flingSet')}</span>${sw('fling')}</div>` +
    `<button class="set danger" id="reset" style="width:100%">${t('reset')}</button>` +
    `<div class="about">${t('about')}<br>RU: ${BANKS.ru.length} · EN: ${BANKS.en.length} · ${t('mGamer')}: ${BANKS.gamer.ru.length}</div>`
  show('settings')
}
$('setBody').addEventListener('click', (e) => {
  const b = e.target.closest('button'); if (!b) return
  if (b.dataset.k) { S[b.dataset.k] = isNaN(b.dataset.v) ? b.dataset.v : +b.dataset.v; recent = [] }
  else if (b.dataset.sw) S[b.dataset.sw] = !S[b.dataset.sw]
  else if (b.id === 'reset') { if (!confirm(t('resetQ'))) return; const keep = { lang: S.lang, timer: S.timer, sound: S.sound, vibe: S.vibe }; S = Object.assign(fresh(), keep); toast(t('resetDone')) }
  save(); applyLang(); renderSettings()
  if (b.dataset.sw === 'sound' && S.sound) sfx.good()
})

/* ---------- wiring ---------- */
for (const b of document.querySelectorAll('[data-mode]')) b.onclick = () => start(b.dataset.mode)
$('langSw').onclick = (e) => { const l = e.target.dataset.l; if (l && l !== S.lang) { S.lang = l; recent = []; save(); goHome() } }
$('toStats').onclick = renderStats
$('toHow').onclick = renderHow
$('toSettings').onclick = renderSettings
for (const b of document.querySelectorAll('.back')) b.onclick = goHome
$('go').onclick = () => submit('answer')
$('skip').onclick = () => submit('skip')
$('hint').onclick = useHint
$('next').onclick = nextQ
$('ans').addEventListener('keydown', (e) => { if (e.key === 'Enter') { e.preventDefault(); e.stopPropagation(); submit('answer') } })
document.addEventListener('keydown', (e) => { if (e.key === 'Enter' && cur === 'play' && G && G.locked) { e.preventDefault(); if (Fling.busy) Fling.skip(); else nextQ() } })
$('quit').onclick = () => { if (!G.log.length || confirm(t('quitQ'))) goHome() }
$('again').onclick = () => start(G.mode)
$('home2').onclick = goHome
$('share').onclick = share
// android back button (the apk calls this; true = handled)
window.androidBack = () => {
  if (cur === 'home') return false
  if (cur === 'play' && Fling.busy) { Fling.skip(); return true }
  if (cur === 'play') { $('quit').click(); return true }
  goHome(); return true
}

load().then(goHome)
