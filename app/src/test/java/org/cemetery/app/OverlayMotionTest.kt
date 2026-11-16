package org.cemetery.app

import androidx.compose.animation.core.MutableTransitionState
import org.cemetery.app.ui.OverlayMotionState
import org.junit.Assert.*
import org.junit.Test

class OverlayMotionTest {
    @Test fun reducedMotionDismissesImmediatelyAndRejectsDuplicateActions(){
        val state=OverlayMotionState(MutableTransitionState(true),false)
        var calls=0
        state.exit{calls++};state.exit{calls+=100}
        assertEquals(1,calls)
        assertTrue(state.closing)
        assertNull(state.afterExit)
    }
    @Test fun animatedDismissalRetainsPanelUntilExitAndLatchesFirstAction(){
        val state=OverlayMotionState(MutableTransitionState(true),true)
        var cancelled=false;var deleted=false
        val cancel={cancelled=true}
        state.exit(cancel);state.exit{deleted=true}
        assertTrue(state.closing)
        assertFalse(state.visibility.targetState)
        assertTrue(state.visibility.currentState)
        assertFalse(cancelled);assertFalse(deleted)
        assertSame(cancel,state.afterExit)
    }
}
