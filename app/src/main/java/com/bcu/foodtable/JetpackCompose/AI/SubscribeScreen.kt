package com.bcu.foodtable.JetpackCompose.AI

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.bcu.foodtable.R

@Composable
fun SubscribeScreen(navController: NavController) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color.White)
    ) {
        CardSection(
            backgroundColor = Color.White,
            imageRes = R.drawable.food_image,
            text = stringResource(id = R.string.ai_recipe),
            textAlign = TextAlign.Start,
            onClick = {
                navController.navigate("ai_recommend")
            }
        )

        CardSection(
            backgroundColor = Color(0xFFC3C3C3),
            imageRes = R.drawable.chat_image,
            text = stringResource(id = R.string.ai_chat),
            textAlign = TextAlign.End,
            onClick = {
                navController.navigate("ai_chat")
            }
        )

        CardSection(
            backgroundColor = Color.White,
            imageRes = R.drawable.cookpot_image,
            text = stringResource(id = R.string.ai_assistant),
            textAlign = TextAlign.Start,
            onClick = {
                navController.navigate("ai_helper")
            }
        )
    }
}

@Composable
fun CardSection(
    backgroundColor: Color,
    imageRes: Int,
    text: String,
    textAlign: TextAlign,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 15.dp)
            .background(backgroundColor)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(50.dp),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = FontFamily(Font(R.font.the_jamsil_bold)),
                    textAlign = textAlign,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp)
                        .background(Color(0xA6505050))
                        .zIndex(1f)
                        .let {
                            if (textAlign == TextAlign.Start) it.padding(start = 65.dp)
                            else it.padding(end = 65.dp)
                        }
                )
            }
        }
    }
}
