package com.simulasim.app.crypto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simulasim.app.ui.Accent
import com.simulasim.app.ui.Blue
import com.simulasim.app.ui.Chip
import com.simulasim.app.ui.Green
import com.simulasim.app.ui.Hspace
import com.simulasim.app.ui.Orange
import com.simulasim.app.ui.Red
import com.simulasim.app.ui.RowGap
import com.simulasim.app.ui.SimButton
import com.simulasim.app.ui.SimCard
import com.simulasim.app.ui.Sparkline
import com.simulasim.app.ui.StatBar
import com.simulasim.app.ui.Surface2
import com.simulasim.app.ui.TextDim
import com.simulasim.app.ui.TextMain
import com.simulasim.app.ui.TopBar
import com.simulasim.app.ui.Vspace
import com.simulasim.app.ui.Yellow

@Composable
fun CryptoScreen(onBack: () -> Unit) {
    val vm: CryptoViewModel = viewModel()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TopBar("rugpull sim", onBack) {
            Text(money(vm.cash), color = Green, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = RowGap) {
            Chip("market", tab == 0) { tab = 0 }
            Chip("launch a coin", tab == 1) { tab = 1 }
            Chip("news", tab == 2) { tab = 2 }
        }
        Vspace(8)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            when (tab) {
                0 -> MarketTab(vm)
                1 -> LaunchTab(vm)
                else -> NewsTab(vm)
            }
            Vspace(24)
        }
    }
}

@Composable
private fun MarketTab(vm: CryptoViewModel) {
    val pv = vm.portfolioValue()
    SimCard(Modifier.fillMaxWidth()) {
        Row {
            Column(Modifier.weight(1f)) {
                Text("cash", color = TextDim, fontSize = 12.sp)
                Text(money(vm.cash), color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text("in bags", color = TextDim, fontSize = 12.sp)
                Text(money(pv), color = Green, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (vm.cash + pv < 5) {
            Vspace(10)
            Text("you're down bad. like, really bad.", color = Red, fontSize = 13.sp)
            Vspace(6)
            SimButton("ask mom for \$1000", Modifier.fillMaxWidth(), color = Orange) { vm.bailout() }
        }
    }
    Vspace(6)
    vm.news.firstOrNull()?.let {
        Text("› $it", color = Yellow, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
    }
    vm.coins.forEach { c ->
        CoinCard(c, vm.holdings[c.ticker] ?: 0.0, vm)
        Vspace(10)
    }
}

@Composable
private fun CoinCard(c: Coin, held: Double, vm: CryptoViewModel) {
    val change = (c.price / c.history.first() - 1) * 100
    SimCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\$${c.ticker}", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextMain)
                    if (c.rugged) {
                        Hspace(8)
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Red)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) { Text("RUGGED", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMain) }
                    }
                }
                Text(c.name, color = TextDim, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$" + fmtPrice(c.price), color = TextMain, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Text(
                    "%+.1f%%".format(change),
                    color = if (change >= 0) Green else Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Vspace(8)
        Sparkline(c.history, Modifier.fillMaxWidth().height(60.dp))
        Vspace(8)
        if (held > 0) {
            Text("you hold ${money(held * c.price)}", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Vspace(6)
        }
        Row(horizontalArrangement = RowGap) {
            SimButton("buy 10%", Modifier.weight(1f), enabled = !c.rugged && vm.cash > 0.01) { vm.buy(c.ticker, 0.1) }
            SimButton("ape 50%", Modifier.weight(1f), color = Green, enabled = !c.rugged && vm.cash > 0.01) { vm.buy(c.ticker, 0.5) }
            SimButton("sell", Modifier.weight(1f), color = Red, enabled = held > 0) { vm.sell(c.ticker) }
        }
    }
}

@Composable
private fun LaunchTab(vm: CryptoViewModel) {
    val l = vm.launch
    if (l == null) {
        LaunchForm(vm)
        return
    }
    SimCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("\$${l.ticker}", fontWeight = FontWeight.Black, fontSize = 22.sp, color = TextMain)
                Text(l.name, color = TextDim, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("mcap ${money(l.marketCap)}", color = TextMain, fontWeight = FontWeight.Bold)
                Text("${l.holders} holders", color = TextDim, fontSize = 12.sp)
            }
        }
        Vspace(10)
        Sparkline(l.history, Modifier.fillMaxWidth().height(110.dp))
        Vspace(10)
        Row {
            Info("price", "$" + fmtPrice(l.price), Modifier.weight(1f))
            Info("liquidity", money(l.poolUsd), Modifier.weight(1f))
            Info("your bag", money(l.devValue), Modifier.weight(1f))
        }
        Vspace(10)
        StatBar("hype", (l.hype / 100).toFloat(), Green)
        Vspace(6)
        StatBar("suspicion (100 = exposed)", (l.suspicion / 100).toFloat(), Red)
        if (l.locked) {
            Vspace(6)
            Text("liquidity \"locked\": you can only dump your bag, not pull the pool", color = Yellow, fontSize = 12.sp)
        }
    }
    Vspace(10)

    if (l.phase == LaunchPhase.LIVE) {
        Text("grow it", color = TextDim, fontSize = 13.sp)
        Vspace(6)
        Row(horizontalArrangement = RowGap) {
            SimButton("shill on x\nfree", Modifier.weight(1f)) { vm.shill() }
            SimButton("influencer\n\$200", Modifier.weight(1f), color = Blue) { vm.influencer() }
        }
        Vspace(6)
        Row(horizontalArrangement = RowGap) {
            SimButton("bot army\n\$100", Modifier.weight(1f), color = Blue) { vm.bots() }
            SimButton("fake audit\n\$150", Modifier.weight(1f), color = Surface2) { vm.fakeAudit() }
        }
        Vspace(6)
        Row(horizontalArrangement = RowGap) {
            SimButton("lock liquidity", Modifier.weight(1f), color = Surface2, enabled = !l.locked) { vm.lockLiquidity() }
            SimButton("sell 10% of bag", Modifier.weight(1f), color = Orange) { vm.sellSome() }
        }
        Vspace(14)
        SimButton(
            if (l.locked) "RUG IT (dump bag)" else "RUG IT (pull liquidity)",
            Modifier.fillMaxWidth().height(58.dp),
            color = Red,
        ) { vm.rug() }
        Vspace(10)
        vm.news.take(4).forEach {
            Text("› $it", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
        }
    } else {
        SimCard(Modifier.fillMaxWidth(), color = if (l.phase == LaunchPhase.RUGGED) Surface2 else Red.copy(alpha = 0.2f)) {
            Text(
                if (l.phase == LaunchPhase.RUGGED) "RUGGED" else "EXPOSED",
                fontWeight = FontWeight.Black, fontSize = 26.sp,
                color = if (l.phase == LaunchPhase.RUGGED) Green else Red,
            )
            Vspace(6)
            Text(l.result ?: "", color = TextMain, fontSize = 15.sp)
            Vspace(12)
            SimButton("launch another one", Modifier.fillMaxWidth()) { vm.clearLaunch() }
        }
    }
}

@Composable
private fun Info(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = TextDim, fontSize = 11.sp)
        Text(value, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun LaunchForm(vm: CryptoViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var ticker by rememberSaveable { mutableStateOf("") }
    SimCard(Modifier.fillMaxWidth()) {
        Text("launch your own memecoin", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextMain)
        Vspace(4)
        Text(
            "seed the pool with \$${LAUNCH_COST.toInt()}, keep 50% of supply for yourself, hype it up, then rug before " +
                "the internet figures you out. if suspicion hits 100 you get exposed and everyone dumps on you.",
            color = TextDim, fontSize = 13.sp,
        )
        Vspace(12)
        val colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextMain, unfocusedTextColor = TextMain,
            focusedBorderColor = Accent, unfocusedBorderColor = Surface2,
            focusedLabelColor = Accent, unfocusedLabelColor = TextDim,
        )
        OutlinedTextField(name, { name = it.take(24) }, label = { Text("coin name") }, singleLine = true, colors = colors, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Safe Moon Inu") })
        Vspace(8)
        OutlinedTextField(ticker, { ticker = it.filter { ch -> ch.isLetterOrDigit() }.take(6).uppercase() }, label = { Text("ticker") }, singleLine = true, colors = colors, modifier = Modifier.fillMaxWidth(), placeholder = { Text("SAFE") })
        Vspace(12)
        SimButton(
            if (vm.cash >= LAUNCH_COST) "deploy contract (\$${LAUNCH_COST.toInt()})" else "need \$${LAUNCH_COST.toInt()} to launch",
            Modifier.fillMaxWidth(),
            color = Green,
            enabled = vm.cash >= LAUNCH_COST,
        ) { vm.startLaunch(name, ticker) }
    }
    Vspace(12)
    SimCard(Modifier.fillMaxWidth()) {
        Text("hall of shame", fontWeight = FontWeight.Bold, color = TextMain)
        Vspace(4)
        Text("coins launched & ended: ${vm.timesRugged}", color = TextDim, fontSize = 13.sp)
        Text("best rug: ${money(vm.bestRug)}", color = TextDim, fontSize = 13.sp)
        Vspace(10)
        SimButton("reset wallet", Modifier.fillMaxWidth(), color = Surface2) { vm.resetEverything() }
    }
    Vspace(10)
    Text(
        "fake money, fake coins. doing this for real is fraud and people go to prison for it.",
        color = TextDim.copy(alpha = 0.6f), fontSize = 11.sp,
    )
}

@Composable
private fun NewsTab(vm: CryptoViewModel) {
    vm.news.forEach {
        SimCard(Modifier.fillMaxWidth()) { Text(it, color = TextMain, fontSize = 14.sp) }
        Vspace(6)
    }
}
