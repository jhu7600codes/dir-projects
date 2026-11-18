// checks answers that aren't on the list against real wikis, so only real answers count as super rare.
// normal questions: wikipedia (the article has to match the answer and be about the topic).
// gamer questions: the game's own wiki (fandom, minecraft.wiki, terraria.wiki.gg), title match is enough.
// rhymes: wiktionary + the ending has to rhyme. all mediawiki apis, no keys, origin=* for cors.
'use strict'
window.Verify = (() => {
  const hasDroid = typeof droid !== 'undefined'
  const cache = new Map()

  const low = (s) => s.toLowerCase().replace(/ё/g, 'е')
  const words = (s) => low(s).replace(/[^a-zа-я0-9+#&]+/gi, ' ').trim().split(/\s+/).filter(Boolean)
  const flat = (s) => words(s).join('')
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

  // "Тупик (птица)" -> base "тупик", note "птица"; "Create (community)/untitled tag game" -> "untitled tag game"
  function splitTitle(t) {
    t = t.split('/').pop()
    const note = (t.match(/\(([^)]*)\)/) || [, ''])[1]
    const base = t.replace(/\([^)]*\)/g, '').split(',')[0].trim()
    return { base, note }
  }
  function titleMatches(title, answer) {
    const st = splitTitle(title), a = flat(answer), b = flat(st.base)
    if (!a || !b) return false
    // "Ак Барс (банк)" also counts for "ак барс банк"
    if (a === b || (st.note && (a === flat(st.base + ' ' + st.note) || a === flat(st.note + ' ' + st.base)))) return true
    const allow = a.length >= 9 ? 2 : a.length >= 5 ? 1 : 0
    if (allow && lev(a, b) <= allow) return true
    // plural/case forms: тупик ~ тупики, puffin ~ puffins
    if (a.length >= 4 && (b.startsWith(a) || a.startsWith(b)) && Math.abs(a.length - b.length) <= 2) return true
    // the answer is a whole word run inside the title: "j++" in "visual j++", "grumble" in "queen grumble"
    const tw = ' ' + words(splitTitle(title).base).join(' ') + ' ', aw = ' ' + words(answer).join(' ') + ' '
    return a.length >= 3 && tw.includes(aw)
  }

  // topic words from the question, cut down to a stem
  const STOP = new Set('назови то что из в на или не и с можно найти бывает есть лежит носят берут почти у всех россии российский российского российскую русских русского русской русскую советский современный известный известного отечественную европейской страны мира name a an the something that you in of or from famous type starts with word bring take'.split(' '))
  const SYN = {
    фрукт: ['фрукт', 'плод', 'цитрус', 'ягод'], овощ: ['овощ', 'корнеплод', 'огородн'], птиц: ['птиц'], рек: ['рек', 'приток'], озер: ['озер'],
    банк: ['банк'], писател: ['писател', 'поэт', 'прозаик', 'драматург'], поэт: ['поэт', 'писател'], блюд: ['блюд', 'кухн', 'суп', 'кушан', 'салат'],
    цвет: ['цвет', 'оттен', 'краск'], авто: ['автомоб', 'марка', 'внедорож', 'грузов'], валют: ['валют', 'денежн', 'монет'], спорт: ['спорт'],
    игрушк: ['игрушк', 'игр'], професси: ['професси', 'специальн', 'работ', 'должност'], город: ['город'], живот: ['живот', 'вид', 'млекопит', 'птиц', 'рыб', 'насеком', 'рептил', 'род'],
    напит: ['напит', 'пить', 'чай', 'сок', 'газиров'], праздн: ['праздн', 'день', 'торжеств'], инструмент: ['инструмент', 'музык'], учен: ['учен', 'физик', 'хими', 'математ', 'изобрет', 'инженер', 'конструктор', 'биолог'],
    изобрет: ['изобрет', 'инженер', 'учен', 'конструктор'], мультфильм: ['мульт', 'анимац'], фильм: ['фильм', 'кино'], смартфон: ['смартфон', 'телефон', 'мобильн', 'электрон', 'производ'],
    магазин: ['магазин', 'сеть', 'ритейл', 'торгов', 'супермаркет'], сладост: ['шоколад', 'конфет', 'кондитер', 'сладост', 'батончик'], шоколадк: ['шоколад', 'конфет', 'кондитер'],
    язык: ['язык'], сми: ['сми', 'газет', 'агентств', 'издани', 'телеканал', 'журнал', 'новост', 'сайт', 'радио'], клуб: ['клуб', 'футбол'], спортсмен: ['спортсмен', 'хоккеист', 'футболист', 'теннисист', 'боксер', 'боец', 'чемпион', 'фигурист', 'гимнаст', 'олимп', 'лыжник', 'биатлонист'],
    бренд: ['бренд', 'марка', 'одежд', 'обув', 'компан'], одежд: ['одежд', 'обув', 'бренд', 'мод'], птицу: ['птиц'], персонаж: ['персонаж', 'сказ', 'героин', 'герой', 'фольклор', 'мифолог'], сказочн: ['сказ', 'персонаж'],
    имя: ['имя', 'имен'], мужск: ['имя', 'имен'], женск: ['имя', 'имен'], сущност: [], суперспособност: ['способност', 'сверхспособ', 'суперсил', 'сил'],
    fruit: ['fruit', 'berry', 'citrus', 'plant'], vegetabl: ['vegetabl', 'plant', 'root', 'crop'], bird: ['bird', 'species', 'genus'], river: ['river', 'tributar'], dessert: ['dessert', 'cake', 'pastry', 'confection', 'sweet', 'pudding'],
    candy: ['candy', 'confection', 'chocolate', 'sweet', 'gum'], color: ['colo', 'hue', 'shade', 'pigment'], sport: ['sport', 'game'], job: ['occupation', 'profession', 'job', 'work', 'worker'],
    animal: ['animal', 'species', 'mammal', 'bird', 'fish', 'insect', 'reptile', 'genus'], languag: ['language'], holiday: ['holiday', 'festival', 'celebrat', 'observ', 'day'],
    weather: ['weather', 'meteorolog', 'precipitation', 'storm', 'wind', 'climate'], emotion: ['emotion', 'feeling', 'mood', 'affect'], superpower: ['power', 'ability', 'superhuman', 'fiction'],
    painter: ['painter', 'artist', 'paint'], scientist: ['scientist', 'physicist', 'chemist', 'mathematician', 'biologist', 'inventor', 'astronomer', 'engineer'], villain: ['villain', 'antagonist', 'character', 'film'],
    superhero: ['superhero', 'hero', 'comic', 'marvel', 'dc'], car: ['car', 'automobile', 'automaker', 'manufacturer', 'vehicle'], boy: ['name', 'given'], name: ['name', 'given'],
  }
  function stem(w) {
    w = low(w)
    if (/[а-я]/.test(w)) w = w.replace(/(ами|ями|ого|его|ому|ему|ыми|ими|ой|ей|ий|ый|ая|яя|ое|ее|ую|юю|ов|ев|ах|ях|ам|ям|ом|ем|ы|и|а|я|у|ю|е|о|ь|й)$/, '')
    else w = w.replace(/(ies|es|s)$/, '')
    return w.slice(0, 8)
  }
  function topicWords(q) {
    const out = new Set()
    for (const w of words(q)) {
      if (STOP.has(w) || w.length < 3) continue
      const s = stem(w)
      out.add(s)
      for (const k in SYN) if (s.startsWith(k) || k.startsWith(s)) SYN[k].forEach((x) => out.add(x))
    }
    return [...out].filter(Boolean)
  }

  async function get(url) {
    const opts = /wikipedia|wiktionary/.test(url) ? { headers: { 'Api-User-Agent': 'raritet-trivia-game/2.4' } } : {}
    const req = hasDroid && droid.net ? droid.net.fetch(url, opts) : fetch(url, opts)
    const res = await Promise.race([req, new Promise((_, no) => setTimeout(() => no(new Error('timeout')), 7000))])
    if (!res.ok) throw new Error('http ' + res.status)
    return res.json()
  }
  const api = (host, params) => `https://${host}/${host.endsWith('wikipedia.org') || host.endsWith('wiktionary.org') ? 'w/' : ''}api.php?` + new URLSearchParams({ format: 'json', origin: '*', ...params })
  const search = async (host, q, n = 10) => ((await get(api(host, { action: 'query', list: 'search', srsearch: q, srlimit: n, srprop: '' }))).query || {}).search || []

  // a wiki that is all about one game: the answer just has to be a page there
  async function onGameWiki(host, answer) {
    for (const r of await search(host, answer, 10)) if (titleMatches(r.title, answer)) return { ok: true, title: splitTitle(r.title).base, where: host }
    return { ok: false, where: host }
  }

  // wikipedia: a page with a matching title, whose intro or categories are about the topic
  async function onWikipedia(lang, answer, topics, second) {
    const host = lang + '.wikipedia.org'
    const cands = (await search(host, second ? answer + ' ' + topics[0] : answer, 10)).filter((r) => titleMatches(r.title, answer)).slice(0, 4)
    // nothing yet: search again with the topic added ("shannon" -> "shannon river")
    if (!cands.length) return !second && topics.length ? onWikipedia(lang, answer, topics, true) : { ok: false, where: host }
    const j = await get(api(host, { action: 'query', prop: 'extracts|categories', exintro: 1, explaintext: 1, exchars: 900, cllimit: 60, clshow: '!hidden', redirects: 1, titles: cands.map((c) => c.title).join('|') }))
    for (const pg of Object.values((j.query || {}).pages || {})) {
      const text = low(pg.title + ' ' + (pg.extract || '') + ' ' + (pg.categories || []).map((c) => c.title).join(' '))
      if (!topics.length || topics.some((t) => text.includes(t))) return { ok: true, title: splitTitle(pg.title).base, where: host }
    }
    return !second && topics.length ? onWikipedia(lang, answer, topics, true) : { ok: false, where: host }
  }

  // rhymes: a real word (wiktionary) with the right ending
  async function rhyme(lang, answer, re) {
    const w = low(answer).trim()
    if (!re.test(w) || /\s/.test(w)) return { ok: false, where: lang + '.wiktionary.org' }
    const host = lang + '.wiktionary.org'
    const j = await get(api(host, { action: 'query', titles: w, redirects: 1 }))
    const ok = Object.keys((j.query || {}).pages || {}).some((k) => +k > 0)
    return { ok, title: w, where: host }
  }

  // returns { ok, title, where } or { error } when there is no internet
  async function check(prompt, answer, lang) {
    const key = prompt.id + ':' + flat(answer)
    if (cache.has(key)) return cache.get(key)
    let r
    try {
      const q = prompt.q
      if (/«кот»/.test(q)) r = await rhyme('ru', answer, /(от|од|ет|ед)$/)
      else if (/"cat"/.test(q)) r = await rhyme('en', answer, /(at|att)$/)
      else if (/на букву «К»/.test(q) && !/^к/.test(low(answer))) r = { ok: false, where: 'правила' }
      else if (/starts with C/.test(q) && !/^c/.test(low(answer))) r = { ok: false, where: 'rules' }
      else if (prompt.src && !prompt.src.startsWith('wp:')) r = await onGameWiki(prompt.src, answer)
      else {
        const topics = prompt.src ? prompt.src.slice(3).split(',') : topicWords(q)
        r = await onWikipedia(prompt.src ? 'en' : lang, answer, topics)
        if (!r.ok && prompt.src && lang === 'ru') r = await onWikipedia('ru', answer, topics)
      }
    } catch (e) {
      return { error: String(e.message || e) }
    }
    cache.set(key, r)
    return r
  }

  return { check, topicWords, titleMatches }
})()
