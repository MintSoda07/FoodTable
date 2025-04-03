import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bcu.foodtable.R

@Composable
fun LoginScreen(
    idInput: String,
    onIdChange: (String) -> Unit,
    pwdInput: String,
    onPwdChange: (String) -> Unit,
    warningText: String,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 40.dp), // 상단 여백만 살짝
        verticalArrangement = Arrangement.Top,             // 위쪽 정렬
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단 로고 + 타이틀
        Row(
            modifier = Modifier.padding(top = 60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.dish_icon),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                colorFilter = ColorFilter.tint(Color(0xFFC62828))
            )
            Text(
                text = stringResource(id = R.string.app_name),
                fontSize = 60.sp,
                color = Color(0xFFC62828),
                fontFamily = FontFamily(Font(R.font.dimibang)),
                modifier = Modifier
                    .padding(start = 10.dp)

            )
        }
        Spacer(modifier = Modifier.height(100.dp))

        // 입력 영역
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            if (warningText.isNotEmpty()) {
                Text(
                    text = warningText,
                    color = Color(0xFFC62828),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }

            OutlinedTextField(
                value = idInput,
                onValueChange = onIdChange,
                label = { Text(text = stringResource(id = R.string.login_id)) },
                placeholder = { Text("아이디를 입력하세요") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_person_24),
                        contentDescription = null
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = pwdInput,
                onValueChange = onPwdChange,
                label = { Text(text = stringResource(id = R.string.login_password)) },
                placeholder = { Text("비밀번호를 입력하세요") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_lock_24),
                        contentDescription = null
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC62828),
                    contentColor = Color.White
                )
            ) {
                Text(text = stringResource(id = R.string.login_button), fontSize = 18.sp)
            }

            TextButton(
                onClick = onSignUpClick,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.signup),
                    fontSize = 16.sp,
                    color = Color(0xFFC62828)
                )
            }
        }



    }
}
