package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    // ملاحظة: بعد إضافة دعم تعدد اللغات، القيمة الافتراضية (values/strings.xml
    // بلا مؤهل لغة) هي الإنجليزية، وهي ما يحمّله Robolectric هنا افتراضيًا
    // بلا تحديد qualifiers="ar" صريح — لذلك تُقارَن بالاسم الإنجليزي.
    assertEquals("Girls & Boys Group", appName)
  }
}
