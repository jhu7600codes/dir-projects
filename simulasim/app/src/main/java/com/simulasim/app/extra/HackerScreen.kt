package com.simulasim.app.extra

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simulasim.app.ui.TopBar
import kotlinx.coroutines.delay
import kotlin.random.Random

private val CODE = """
#include <linux/kernel.h>
#include <pentagon/mainframe.h>
static int bypass_firewall(struct node *n) {
    for (int i = 0; i < n->ports; i++) {
        if (scan_port(n, i) == OPEN) {
            inject_payload(n, i, &shellcode);
            log("[+] port %d compromised", i);
        }
    }
    return decrypt_rsa_4096(n->key, BRUTEFORCE_TURBO);
}
void download_ram(size_t gb) {
    while (gb--) memcpy(local_ram, remote_ram, 1 << 30);
}
>>> import nasa
>>> nasa.satellites.reroute(target="my_house")
[OK] satellite 7 now orbiting your backyard
$ sudo hack --target mainframe --stealth --no-cap
[*] tracing ip 127.0.0.1 ... it's you. it was always you
[*] enhancing image... enhance... ENHANCE
SELECT * FROM users WHERE password = 'password123';
-- 4,029,118 rows returned
fn quantum_decrypt(key: &[u8]) -> Result<Secrets, Fbi> {
    let cipher = Cipher::new(key).invert().double_invert();
    cipher.yeet()?.into_mainframe()
}
git push --force origin cia/main
[!] firewall detected. deploying counter-firewall
[!] counter-firewall detected. deploying counter-counter-firewall
while (!access_granted) { type_faster(); wear_hoodie(); }
""".trimIndent()

@Composable
fun HackerScreen(onBack: () -> Unit) {
    var pos by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }
    var taps by remember { mutableIntStateOf(0) }
    var banner by remember { mutableStateOf<Pair<String, Color>?>(null) }
    val scroll = rememberScrollState()

    LaunchedEffect(text) { scroll.scrollTo(scroll.maxValue) }
    LaunchedEffect(banner) {
        if (banner != null) {
            delay(1800)
            banner = null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopBar("hacker sim", onBack)
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    val n = Random.nextInt(4, 12)
                    val sb = StringBuilder(text)
                    repeat(n) {
                        sb.append(CODE[pos % CODE.length])
                        pos++
                    }
                    text = sb.toString().takeLast(6000)
                    taps++
                    if (taps % 45 == 0) {
                        banner = if (Random.nextDouble() < 0.7) "ACCESS GRANTED" to Color(0xFF00FF66) else "ACCESS DENIED" to Color(0xFFFF3344)
                    }
                }
                .padding(12.dp)
        ) {
            Column(Modifier.verticalScroll(scroll).fillMaxWidth()) {
                Text(
                    if (text.isEmpty()) "tap anywhere. fast. like in the movies._" else text + "█",
                    color = Color(0xFF00FF66),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
            }
            banner?.let { (label, color) ->
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .background(Color.Black, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(label, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 30.sp)
                }
            }
        }
    }
}
