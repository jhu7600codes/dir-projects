// the throw: your answer gets launched from a slingshot and lands as many meters away as it scored.
// other answers are flags in the field at their own distances. canvas, no libraries.
'use strict'
window.Fling = (() => {
  const box = document.getElementById('fling'), cv = document.getElementById('flc'), cx = cv.getContext('2d')
  const distEl = document.getElementById('flDist')
  let W = 0, H = 0, ppm = 8, ground = 0
  let st = null, raf = 0

  function size() {
    W = innerWidth; H = innerHeight
    cv.width = W * devicePixelRatio; cv.height = H * devicePixelRatio
    ppm = Math.min(W, 520) / 42 // about 42 m of field on screen
    ground = H * 0.66
  }

  // camera follows the ball; sx/sy turn meters into screen pixels
  const sx = (m) => (m - st.cam) * ppm
  const sy = (h) => ground - h * ppm
  const ease = (k) => 1 - Math.pow(1 - k, 3)

  function open(o) {
    size()
    const flags = o.flags.map((f, i) => ({ ...f, lvl: 0 }))
    // stack labels of flags that stand close together
    flags.sort((a, b) => a.dist - b.dist)
    // put each label on the lowest row where it doesn't overlap the one before it
    cx.font = '600 13px Roboto,sans-serif'
    const ends = []
    for (const f of flags) {
      const w = (cx.measureText(f.name + '  ' + f.pct + '%').width + 22) / ppm
      let lvl = 0
      while (ends[lvl] != null && ends[lvl] > f.dist) lvl++
      f.lvl = lvl; ends[lvl] = f.dist + w
    }
    const d = o.mine ? o.mine.dist : 0
    st = {
      ...o, flags, d, t0: performance.now(), done: false, cam: -6, x: 0, h: 1.4, rot: 0, trail: [], puffs: [],
      land: d * 0.82, apex: Math.max(3, d * 0.33), fly: o.mine ? 650 + d * 13 : 500,
      far: Math.max(...flags.map((f) => f.dist), 20),
      clouds: Array.from({ length: 9 }, (_, i) => ({ x: i * 22 + Math.random() * 12, y: 0.1 + Math.random() * 0.25, s: 0.6 + Math.random() * 0.8 })),
    }
    distEl.textContent = '0'
    box.classList.add('on')
    box.classList.remove('landed')
    cancelAnimationFrame(raf)
    raf = requestAnimationFrame(frame)
    if (o.onLaunch) o.onLaunch()
  }

  function close() { cancelAnimationFrame(raf); box.classList.remove('on', 'landed'); st = null }

  function skip() { if (st && !st.done) { st.t0 = -1e9 } }

  // where the ball is at time t (ms since launch)
  function pose(t) {
    const s = st
    if (!s.mine) {
      // a miss: the ball flops out of the slingshot, then the camera tours the flags
      const k = Math.min(1, t / s.fly)
      const x = 2.2 * k, h = 1.4 + 2.5 * k * (1 - k) * 4 - 1.4 * k * k
      const tour = Math.max(0, t - s.fly - 250) / (900 + s.far * 16)
      const cam = -6 + (s.far - 22) * ease(Math.min(1, tour))
      return { x, h: Math.max(0.35, h), cam, end: tour >= 1, k }
    }
    const fly = s.fly, b1 = 320, roll = 450
    if (t < fly) {
      const k = t / fly
      return { x: s.land * k, h: 1.4 * (1 - k) + 4 * s.apex * k * (1 - k) + 0.35 * k, k }
    }
    const bx = s.land + (s.d - s.land) * 0.65
    if (t < fly + b1) {
      const k = (t - fly) / b1
      return { x: s.land + (bx - s.land) * k, h: 0.35 + 4 * Math.max(0.6, s.d * 0.035) * k * (1 - k), k: 1 }
    }
    const k = Math.min(1, (t - fly - b1) / roll)
    return { x: bx + (s.d - bx) * ease(k), h: 0.35, k: 1, end: k >= 1 }
  }

  function frame(now) {
    const s = st; if (!s) return
    const t = now - s.t0
    const p = pose(t)
    const prevX = s.x
    s.x = p.x; s.h = p.h
    s.rot += (s.x - prevX) * 0.9
    if (s.mine) s.cam = Math.max(-6, s.x - 14)
    else s.cam = p.cam
    if (t < s.fly && s.mine) s.trail.push([s.x, s.h])
    if (!s.landedOnce && s.mine && t >= s.fly) { s.landedOnce = true; puff(s.land, 14); if (s.onLand) s.onLand() }
    distEl.textContent = Math.round(s.mine ? Math.min(s.x, s.d) : 0)
    draw()
    if (p.end && !s.done) {
      s.done = true
      if (s.mine) puff(s.d, 6)
      box.classList.add('landed')
      if (s.onDone) s.onDone()
    }
    raf = requestAnimationFrame(frame)
  }

  function puff(x, n) { for (let i = 0; i < n; i++) st.puffs.push({ x, h: 0.2, vx: (Math.random() - 0.5) * 0.25, vh: Math.random() * 0.18, life: 40 + Math.random() * 20 }) }

  function draw() {
    const s = st
    cx.setTransform(devicePixelRatio, 0, 0, devicePixelRatio, 0, 0)
    // sky
    const sky = cx.createLinearGradient(0, 0, 0, ground)
    sky.addColorStop(0, '#0e2a4a'); sky.addColorStop(0.6, '#2c6aa0'); sky.addColorStop(1, '#79b8d9')
    cx.fillStyle = sky; cx.fillRect(0, 0, W, ground)
    // clouds and hills move slower than the field (parallax)
    cx.fillStyle = 'rgba(255,255,255,.12)'
    for (const c of s.clouds) {
      const x = ((c.x * ppm - s.cam * ppm * 0.25) % (W + 200) + W + 200) % (W + 200) - 100
      const y = H * c.y
      cx.beginPath(); cx.ellipse(x, y, 46 * c.s, 13 * c.s, 0, 0, 7); cx.ellipse(x + 26 * c.s, y - 8 * c.s, 30 * c.s, 12 * c.s, 0, 0, 7); cx.fill()
    }
    cx.fillStyle = '#1d4a5e'
    cx.beginPath(); cx.moveTo(0, ground)
    for (let x = 0; x <= W + 20; x += 20) { const w = x + s.cam * ppm * 0.45; cx.lineTo(x, ground - 34 - Math.sin(w / 90) * 16 - Math.sin(w / 37) * 6) }
    cx.lineTo(W, ground); cx.fill()

    // field, striped every 10 m, colored by zone
    const zones = [[0, '#2f6b3a'], [40, '#357a3c'], [70, '#3c8a3f'], [90, '#4a9c3a']]
    for (let m = Math.floor(s.cam / 10) * 10; m < s.cam + W / ppm + 10; m += 10) {
      if (m < 0) { cx.fillStyle = '#2a5a33'; cx.fillRect(sx(m), ground, 10 * ppm + 1, H - ground); continue }
      const z = zones.filter((zz) => m >= zz[0]).pop()
      cx.fillStyle = z[1]; cx.fillRect(sx(m), ground, 10 * ppm + 1, H - ground)
      cx.fillStyle = (m / 10) % 2 ? 'rgba(0,0,0,.06)' : 'rgba(255,255,255,.04)'; cx.fillRect(sx(m), ground, 10 * ppm + 1, H - ground)
    }
    cx.fillStyle = '#5fb35a'; cx.fillRect(0, ground - 2, W, 4)
    // distance lines and numbers
    cx.font = '600 13px Roboto,sans-serif'; cx.textAlign = 'center'
    for (let m = Math.max(0, Math.floor(s.cam / 10) * 10); m < s.cam + W / ppm + 10; m += 10) {
      const x = sx(m)
      cx.fillStyle = 'rgba(255,255,255,.55)'; cx.fillRect(x - 1, ground, 2, 16)
      cx.fillStyle = 'rgba(255,255,255,.75)'; cx.fillText(m + s.unit, x, ground + 32)
    }
    // zone names along the field
    cx.font = '500 12px Roboto,sans-serif'; cx.textAlign = 'left'
    s.zones.forEach((name, i) => { const m = [0, 40, 70, 90][i]; cx.fillStyle = 'rgba(255,255,255,.4)'; cx.fillText(name, sx(m) + 6, H - 18) })
    // slingshot
    const ox = sx(0)
    cx.strokeStyle = '#8a5a2b'; cx.lineWidth = 6; cx.lineCap = 'round'
    cx.beginPath(); cx.moveTo(ox, ground); cx.lineTo(ox, ground - 1.0 * ppm); cx.lineTo(ox - 0.5 * ppm, ground - 1.7 * ppm); cx.moveTo(ox, ground - 1.0 * ppm); cx.lineTo(ox + 0.5 * ppm, ground - 1.7 * ppm); cx.stroke()

    // flags of the other answers
    for (const f of s.flags) {
      const x = sx(f.dist); if (x < -120 || x > W + 120) continue
      const top = ground - 46 - f.lvl * 30
      cx.strokeStyle = 'rgba(255,255,255,.8)'; cx.lineWidth = 2; cx.beginPath(); cx.moveTo(x, ground); cx.lineTo(x, top); cx.stroke()
      label(f.name + '  ' + f.pct + '%', x + 3, top, f.me ? '#7ed957' : 'rgba(12,15,19,.75)', f.me ? '#04131b' : '#fff')
    }
    // trail
    cx.fillStyle = 'rgba(255,255,255,.5)'
    for (let i = 0; i < s.trail.length; i += 3) { const [x, h] = s.trail[i]; cx.beginPath(); cx.arc(sx(x), sy(h), 2, 0, 7); cx.fill() }
    // dust
    s.puffs = s.puffs.filter((q) => --q.life > 0)
    for (const q of s.puffs) { q.x += q.vx; q.h += q.vh; q.vh -= 0.004; cx.fillStyle = `rgba(230,220,190,${q.life / 80})`; cx.beginPath(); cx.arc(sx(q.x), sy(Math.max(0, q.h)), 5, 0, 7); cx.fill() }
    // the ball, with your answer riding on it
    const bx = sx(s.x), by = sy(s.h) - 9
    cx.save(); cx.translate(bx, by); cx.rotate(s.rot)
    const g = cx.createRadialGradient(-3, -3, 1, 0, 0, 11); g.addColorStop(0, '#fff'); g.addColorStop(1, s.mine ? '#33b5e5' : '#ff5466')
    cx.fillStyle = g; cx.beginPath(); cx.arc(0, 0, 10, 0, 7); cx.fill()
    cx.strokeStyle = 'rgba(0,0,0,.25)'; cx.lineWidth = 1.5; cx.beginPath(); cx.arc(0, 0, 10, 0.3, 2.6); cx.stroke()
    cx.restore()
    cx.fillStyle = 'rgba(0,0,0,.25)'; cx.beginPath(); cx.ellipse(bx, ground + 2, 9 - Math.min(6, s.h * 0.3), 3, 0, 0, 7); cx.fill()
    if (s.label) label(s.label, bx - 4, by - 18, s.mine ? '#33b5e5' : '#ff5466', '#04131b', true)
  }

  function label(txt, x, y, bg, fg, centered) {
    cx.font = '600 13px Roboto,sans-serif'
    const w = cx.measureText(txt).width + 14
    const lx = centered ? x - w / 2 + 4 : x
    cx.fillStyle = bg; rr(lx, y - 12, w, 22, 7); cx.fill()
    cx.fillStyle = fg; cx.textAlign = 'left'; cx.fillText(txt, lx + 7, y + 4)
  }
  function rr(x, y, w, h, r) { cx.beginPath(); cx.moveTo(x + r, y); cx.arcTo(x + w, y, x + w, y + h, r); cx.arcTo(x + w, y + h, x, y + h, r); cx.arcTo(x, y + h, x, y, r); cx.arcTo(x, y, x + w, y, r); cx.closePath() }

  addEventListener('resize', () => { if (st) size() })
  cv.addEventListener('click', skip)
  return { open, close, skip, get busy() { return !!st && !st.done }, get on() { return !!st } }
})()
