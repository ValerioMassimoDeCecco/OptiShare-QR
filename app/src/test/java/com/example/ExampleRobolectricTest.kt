package com.example

import com.example.ui.theme.AppCustomizationState
import com.example.ui.theme.DesignStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun `default customization preserves material and liquid glass parameters`() {
        val state = AppCustomizationState()
        assertEquals(DesignStyle.MATERIAL, state.designStyle)
        assertEquals(16, state.glassCornerRadiusDp)
    }
}
