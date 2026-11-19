package dev.jamescullimore.starjar.routinebuilder.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IconLabelsTest {
    @Test fun everySelectableIconHasBothAccessibleNames() {
        assertEquals(IconCatalog.ids, iconLabels.keys)
        iconLabels.forEach { (id, labels) ->
            assertTrue(labels.first.isNotBlank(), id)
            assertTrue(labels.second.isNotBlank(), id)
        }
        assertEquals("Geschirrspüler", iconLabel("Dishwasher", "de"))
        assertEquals("Dishwasher", iconLabel("Dishwasher", "fr"))
    }
}
