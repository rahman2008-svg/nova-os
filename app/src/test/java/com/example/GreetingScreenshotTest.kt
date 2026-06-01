package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent { 
      MyApplicationTheme { 
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
              .background(Color(0xFF0E0E11))
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Text(
              text = "/// NOVA_OS",
              style = MaterialTheme.typography.displayMedium,
              color = Color(0xFFFF0830),
              fontWeight = FontWeight.Bold,
              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
              text = "NEXT_GEN CONCEPT DRIVER",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Designed for ultra-fluid operational layers.\nSimulated offline AI memory modules verified.",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.Gray,
              textAlign = TextAlign.Center,
              lineHeight = 18.sp
            )
          }
        }
      } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
