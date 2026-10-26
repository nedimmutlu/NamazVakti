package com.example.namazvakti

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Mesaj Modeli
data class IslamicMessage(
    val id: Int,
    val category: String, // "Cuma", "Kandil", "Ayetli", "Bayram"
    val text: String,
    val sourceOrNote: String = ""
)

val ISLAMIC_MESSAGES = listOf(
    // --- CUMA MESAJLARI ---
    IslamicMessage(
        id = 1,
        category = "Cuma",
        text = "Gününüz bereketli, kalbiniz huzurlu, dualarınız kabul olsun. Yüce Mevla bizi ve sevdiklerimizi iki cihanda aziz eylesin. Hayırlı Cumalar. 🌹",
        sourceOrNote = "Cumanız Mübarek Olsun"
    ),
    IslamicMessage(
        id = 2,
        category = "Cuma",
        text = "Rabbim dualarınızı kabul, sağlığınızı daim, yuvanızı huzurlu eylesin. Gönlünüzden geçen tüm hayırlı muratlarınız gerçek olsun. Cumanız mübarek olsun. 🤲",
        sourceOrNote = "Hayırlı ve Bereketli Cumalar"
    ),
    IslamicMessage(
        id = 3,
        category = "Cuma",
        text = "Ey Rabbimiz! Bize dünyada da iyilik ver, ahirette de iyilik ver ve bizi cehennem azabından koru. Dualarda buluşmak ümidiyle hayırlı nurlu cumalar dilerim.",
        sourceOrNote = "Bakara Suresi 201"
    ),
    IslamicMessage(
        id = 4,
        category = "Cuma",
        text = "Ömrümüze bereket, gönlümüze inşirah, hanelerimize huzur ver Allah'ım. Cuma gününün bereketi ve affı üzerinize olsun.",
        sourceOrNote = "Mübarek Cuma Duası"
    ),

    // --- KANDİL MESAJLARI ---
    IslamicMessage(
        id = 5,
        category = "Kandil",
        text = "Allah'ın rahmeti, bereketi ve mağfireti üzerinize olsun. Bu mübarek kandil gecesinde edilen tüm dualar dergâh-ı izzette kabul olsun. Kandiliniz mübarek olsun. ✨",
        sourceOrNote = "Mübarek Kandil Tebriği"
    ),
    IslamicMessage(
        id = 6,
        category = "Kandil",
        text = "Ellerin semaya, dillerin duaya, gönüllerin Mevla'ya yöneldiği bu nurlu gecede dualarınız kabul, kandiliniz mübarek olsun.",
        sourceOrNote = "Hayırlı Kandiller"
    ),
    IslamicMessage(
        id = 7,
        category = "Kandil",
        text = "Gecenin karanlığını dualarla aydınlatan Rabbim, kalbimizdeki kederleri sevince tebdil eylesin. Hayırlı ve nurlu kandiller dilerim.",
        sourceOrNote = "Kandil Gecesi Duası"
    ),

    // --- AYET & HADİSLİ DUALAR ---
    IslamicMessage(
        id = 8,
        category = "Ayetli",
        text = "\"Şüphesiz güçlükle beraber bir kolaylık vardır. Gerçekten güçlükle beraber bir kolaylık vardır.\" Rabbim tüm zorluklarımızı hayırla kolaylaştırsın.",
        sourceOrNote = "İnşirah Suresi, 5-6"
    ),
    IslamicMessage(
        id = 9,
        category = "Ayetli",
        text = "\"Rabbimiz! Bizi doğru yola ilettikten sonra kalplerimizi eğriltme. Bize katından bir rahmet bağışla. Şüphesiz Sen çok bağışlayansın.\"",
        sourceOrNote = "Âl-i İmrân Suresi, 8"
    ),
    IslamicMessage(
        id = 10,
        category = "Ayetli",
        text = "\"Müminin mümine gıyabında yaptığı dua en çabuk kabul olan duadır.\" Dualarınızda yer bulmak dileğiyle, hayırlı günler dilerim.",
        sourceOrNote = "Hadis-i Şerif (Tirmizî)"
    ),

    // --- BAYRAM MESAJLARI ---
    IslamicMessage(
        id = 11,
        category = "Bayram",
        text = "Dargınlıkların bittiği, sevginin ve kardeşliğin pekiştiği, neşe dolu nice bayramlara... Bayramınız mübarek, haneniz bereketli olsun. 🎉",
        sourceOrNote = "Hayırlı Bayramlar"
    ),
    IslamicMessage(
        id = 12,
        category = "Bayram",
        text = "Rabbim kestiğimiz kurbanları, tuttuğumuz oruçları ve yaptığımız tüm ibadetleri kabul buyursun. Sevdiklerinizle birlikte huzur dolu bayramlar dilerim.",
        sourceOrNote = "Bayram Tebriği"
    )
)

class MessagesActivity : ComponentActivity() {

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Cuma Mesajı", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Mesaj kopyalandı! İstediğiniz yere yapıştırabilirsiniz.", Toast.LENGTH_SHORT).show()
    }

    private fun shareMessage(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Mesajı Paylaş"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MessagesScreen(
                onBack = { finish() },
                onCopy = { copyToClipboard(it) },
                onShare = { shareMessage(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onBack: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Cuma") }
    var favoriteIds by remember { mutableStateOf(setOf<Int>()) }

    val categories = listOf("Cuma", "Kandil", "Ayetli", "Bayram")

    val filteredMessages = remember(selectedCategory) {
        ISLAMIC_MESSAGES.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Tebrik & Cuma Mesajları",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Tek tıkla kopyala ve WhatsApp'ta paylaş",
                            fontSize = 11.sp,
                            color = Color(0xFFC8E6C9)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F3E2C)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D251A),
                            Color(0xFF081710)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            // 1. KATEGORİ SEÇİM ŞERİDİ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = (selectedCategory == cat)
                    val label = when (cat) {
                        "Cuma" -> "🕌 Cuma"
                        "Kandil" -> "🏮 Kandil"
                        "Ayetli" -> "📖 Dualar"
                        else -> "🎉 Bayram"
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFF1B5E20) else Color.White.copy(alpha = 0.08f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = label,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (isSelected) Color.White else Color(0xFFB0BEC5),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. MESAJ KARTLARI LİSTESİ
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredMessages, key = { it.id }) { msg ->
                    val isFav = favoriteIds.contains(msg.id)

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isFav) Color(0xFFFFD54F) else Color(0xFF2E7D32).copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Üst Başlık (Kaynak ve Favori Butonu)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1B5E20).copy(alpha = 0.4f)
                                ) {
                                    Text(
                                        text = msg.sourceOrNote,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF81C784),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                // Favori Butonu
                                IconButton(
                                    onClick = {
                                        favoriteIds = if (isFav) favoriteIds - msg.id else favoriteIds + msg.id
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favori",
                                        tint = if (isFav) Color(0xFFFF5252) else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Mesaj Metni
                            Text(
                                text = msg.text,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Normal
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Alt Butonlar (Kopyala ve Paylaş)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Kopyala Butonu
                                Button(
                                    onClick = { onCopy(msg.text) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.12f)
                                    )
                                ) {
                                    Text("📋 Kopyala", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                }

                                // WhatsApp / Paylaş Butonu
                                Button(
                                    onClick = { onShare(msg.text) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1B5E20)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Paylaş", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}