package com.example.pogapp

import android.os.Looper
import android.widget.Button
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.lang.reflect.Field
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
class MainActivityUnitTest {

    private lateinit var activity: MainActivity
    private lateinit var btnAction: Button
    private lateinit var tvPlayerMatchScore: TextView
    private lateinit var tvAIMatchScore: TextView
    private lateinit var tvPlayerTotalPogs: TextView
    private lateinit var tvAITotalPogs: TextView
    private lateinit var tvPogsInStack: TextView
    private lateinit var tvGameMessage: TextView

    // Reflection helpers to access private members
    private fun <T> getPrivateField(obj: Any, fieldName: String): T {
        val field: Field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(obj) as T
    }

    private fun <T> setPrivateField(obj: Any, fieldName: String, value: T) {
        val field: Field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(obj, value)
    }

    private fun callPrivateMethod(obj: Any, methodName: String, vararg args: Any?) {
        val method: Method = obj.javaClass.getDeclaredMethod(methodName, *args.map { it?.javaClass ?: Nothing::class.java }.toTypedArray())
        method.isAccessible = true
        method.invoke(obj, *args)
    }
    
    private fun callPrivateMethodReturning(obj: Any, methodName: String, vararg args: Any?): Any? {
        val argTypes = args.map {
            when (it) {
                is Double -> Double::class.javaPrimitiveType ?: Double::class.java
                is Int -> Int::class.javaPrimitiveType ?: Int::class.java
                is Boolean -> Boolean::class.javaPrimitiveType ?: Boolean::class.java
                // Add other primitive types as needed
                else -> it?.javaClass ?: Nothing::class.java
            }
        }.toTypedArray()
        val method: Method = obj.javaClass.getDeclaredMethod(methodName, *argTypes)
        method.isAccessible = true
        return method.invoke(obj, *args)
    }


    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(MainActivity::class.java).create().start().resume().get()
        btnAction = activity.findViewById(R.id.btnAction)
        tvPlayerMatchScore = activity.findViewById(R.id.tvPlayerMatchScore)
        tvAIMatchScore = activity.findViewById(R.id.tvAIMatchScore)
        tvPlayerTotalPogs = activity.findViewById(R.id.tvPlayerTotalPogs)
        tvAITotalPogs = activity.findViewById(R.id.tvAITotalPogs)
        tvPogsInStack = activity.findViewById(R.id.tvPogsInStack)
        tvGameMessage = activity.findViewById(R.id.tvGameMessage)
    }

    @Test
    fun testInitialState_AfterStartNewMatch() {
        callPrivateMethod(activity, "startNewMatch")
        assertEquals("Player Rounds: 0", tvPlayerMatchScore.text)
        assertEquals("AI Rounds: 0", tvAIMatchScore.text)
        val initialPogs = getPrivateField<Int>(activity, "INITIAL_POGS_PER_PLAYER")
        assertEquals(activity.getString(R.string.player_pogs, initialPogs), tvPlayerTotalPogs.text)
        assertEquals(activity.getString(R.string.ai_pogs, initialPogs), tvAITotalPogs.text)
        assertEquals(activity.getString(R.string.action_button_start_round), btnAction.text)
    }

    @Test
    fun testSetupNewRound_PopulatesStackAndDeductsPogs() {
        val initialPogs = getPrivateField<Int>(activity, "INITIAL_POGS_PER_PLAYER")
        val pogsPerRound = getPrivateField<Int>(activity, "POGS_PER_ROUND_STACK")

        callPrivateMethod(activity, "setupNewRound")

        val currentPogStack = getPrivateField<List<Pog>>(activity, "currentPogStack")
        assertEquals(pogsPerRound * 2, currentPogStack.size)
        assertEquals(activity.getString(R.string.pogs_in_stack, pogsPerRound * 2), tvPogsInStack.text)

        val playerTotalPogs = getPrivateField<Int>(activity, "playerTotalPogs")
        val aiTotalPogs = getPrivateField<Int>(activity, "aiTotalPogs")
        assertEquals(initialPogs - pogsPerRound, playerTotalPogs)
        assertEquals(initialPogs - pogsPerRound, aiTotalPogs)
        assertEquals(activity.getString(R.string.action_button_throw), btnAction.text)
    }
    
    @Test
    fun testHandleSlammerThrow_PlayerFlipsAllPogs() {
        // Setup round with a known small number of pogs
        setPrivateField(activity, "POGS_PER_ROUND_STACK", 1) // 1 pog from each player
        callPrivateMethod(activity, "setupNewRound") // Stack will have 2 pogs

        val initialPlayerPogs = getPrivateField<Int>(activity, "playerTotalPogs")
        setPrivateField(activity, "isPlayerTurn", true)

        // Mock random to ensure player flips all (by setting flipProbability to 1.0 for the call)
        // The actual method uses a field for probability, so we pass it directly.
        callPrivateMethodReturning(activity, "handleSlammerThrow", 1.0) // Force 100% flip rate

        val playerPogsAfter = getPrivateField<Int>(activity, "playerTotalPogs")
        val currentPogStack = getPrivateField<List<Pog>>(activity, "currentPogStack")

        assertEquals(0, currentPogStack.size) // Stack should be empty
        assertEquals(initialPlayerPogs + 2, playerPogsAfter) // Player keeps both pogs

        // Since stack is empty, round should end, player should win the round
        val playerMatchScore = getPrivateField<Int>(activity, "playerMatchScore")
        assertEquals(1, playerMatchScore)
        assertTrue(tvGameMessage.text.toString().contains(activity.getString(R.string.game_message_player_wins_round)))
        assertEquals(activity.getString(R.string.action_button_start_round), btnAction.text) // Next action
    }


    @Test
    fun testHandleSlammerThrow_AIFlipsSomePogs() {
        setPrivateField(activity, "POGS_PER_ROUND_STACK", 3) // 3 pogs each, 6 total
        callPrivateMethod(activity, "setupNewRound")
        val initialAIPogs = getPrivateField<Int>(activity, "aiTotalPogs")
        setPrivateField(activity, "isPlayerTurn", false)

        // For AI, use its defined probability (AI_FLIP_PROBABILITY = 0.45)
        // We can't guarantee exact flips, but we can check if pogs were processed.
        // To make it testable, let's assume AI flips at least one.
        // This requires a more complex setup with Robolectric's Random seeding or mocking.
        // For simplicity, let's check if AI total pogs can increase and stack decreases.
        // A more robust test would mock Random.nextDouble() to control flips.

        // We'll call handleSlammerThrow with a fixed probability for testing this scenario
        callPrivateMethodReturning(activity, "handleSlammerThrow", 0.5) // Test with 50%

        val aiPogsAfter = getPrivateField<Int>(activity, "aiTotalPogs")
        val stackAfter = getPrivateField<List<Pog>>(activity, "currentPogStack").size

        // With 6 pogs and 50% chance, statistically 3 should flip.
        // This is probabilistic, so exact numbers are hard without mocking Random.
        // We expect stack to decrease and AI pogs to potentially increase.
        assertTrue(stackAfter < 6)
        if (stackAfter < 6) { // If any pog was flipped
            assertTrue(aiPogsAfter > initialAIPogs)
        }
        // If stack didn't empty, it should still be AI's turn (or switch to player)
        val isPlayerTurnAfter = getPrivateField<Boolean>(activity, "isPlayerTurn")
        if (stackAfter > 0) {
             assertTrue(isPlayerTurnAfter) // Should switch to player
        }
    }


    @Test
    fun testFullMatch_PlayerWins() {
        val roundsToWin = getPrivateField<Int>(activity, "ROUNDS_TO_WIN_MATCH")
        setPrivateField(activity, "playerMatchScore", roundsToWin - 1)
        setPrivateField(activity, "aiMatchScore", 0)
        setPrivateField(activity, "isPlayerTurn", true) // Player's turn to win the last round

        // Simulate player winning the current round
        // Setup a minimal round
        setPrivateField(activity, "POGS_PER_ROUND_STACK", 1)
        callPrivateMethod(activity, "setupNewRound")
        setPrivateField(activity, "isPlayerTurn", true) // Ensure it's player's turn

        // Player flips all pogs to win the round
        callPrivateMethodReturning(activity, "handleSlammerThrow", 1.0) // 100% flip

        // Check match score and game message
        val playerMatchScore = getPrivateField<Int>(activity, "playerMatchScore")
        assertEquals(roundsToWin, playerMatchScore)
        assertTrue(tvGameMessage.text.toString().contains(activity.getString(R.string.game_message_player_wins_match)))
        assertEquals(activity.getString(R.string.action_button_start_match), btnAction.text)
    }

    @Test
    fun testFullMatch_AIWins() {
        val roundsToWin = getPrivateField<Int>(activity, "ROUNDS_TO_WIN_MATCH")
        setPrivateField(activity, "aiMatchScore", roundsToWin - 1)
        setPrivateField(activity, "playerMatchScore", 0)
        setPrivateField(activity, "isPlayerTurn", false) // AI's turn

        setPrivateField(activity, "POGS_PER_ROUND_STACK", 1)
        callPrivateMethod(activity, "setupNewRound")
        setPrivateField(activity, "isPlayerTurn", false) // Ensure AI's turn

        callPrivateMethodReturning(activity, "handleSlammerThrow", 1.0) // AI flips all

        val aiMatchScore = getPrivateField<Int>(activity, "aiMatchScore")
        assertEquals(roundsToWin, aiMatchScore)
        assertTrue(tvGameMessage.text.toString().contains(activity.getString(R.string.game_message_ai_wins_match)))
        assertEquals(activity.getString(R.string.action_button_start_match), btnAction.text)
    }
    
    @Test
    fun testEndGameDueToNoPogs_PlayerWinsMatch() {
        setPrivateField(activity, "playerTotalPogs", 10) // Player has pogs
        setPrivateField(activity, "aiTotalPogs", 0)     // AI has no pogs
        setPrivateField(activity, "playerMatchScore", 1) // Example scores
        setPrivateField(activity, "aiMatchScore", 1)

        callPrivateMethod(activity, "endGameDueToNoPogs")

        assertTrue(tvGameMessage.text.contains("AI has no pogs left! Player wins the match by default!"))
        // Player should win the match due to tie-breaker by pogs if scores were equal
        assertEquals(2, getPrivateField<Int>(activity, "playerMatchScore"))
        assertEquals(1, getPrivateField<Int>(activity, "aiMatchScore"))
        assertEquals(activity.getString(R.string.action_button_start_match), btnAction.text)
    }

    @Test
    fun testEndGameDueToNoPogs_AIWinsMatch() {
        setPrivateField(activity, "playerTotalPogs", 0)
        setPrivateField(activity, "aiTotalPogs", 10)
        setPrivateField(activity, "playerMatchScore", 1)
        setPrivateField(activity, "aiMatchScore", 1)

        callPrivateMethod(activity, "endGameDueToNoPogs")

        assertTrue(tvGameMessage.text.contains("Player has no pogs left! AI wins the match by default!"))
        assertEquals(1, getPrivateField<Int>(activity, "playerMatchScore"))
        assertEquals(2, getPrivateField<Int>(activity, "aiMatchScore")) // AI wins tie-breaker
        assertEquals(activity.getString(R.string.action_button_start_match), btnAction.text)
    }
    
    @Test
    fun testEndGameDueToNoPogs_Draw() {
        setPrivateField(activity, "playerTotalPogs", 0)
        setPrivateField(activity, "aiTotalPogs", 0)
        setPrivateField(activity, "playerMatchScore", 1)
        setPrivateField(activity, "aiMatchScore", 1)

        callPrivateMethod(activity, "endGameDueToNoPogs")

        assertTrue(tvGameMessage.text.contains("Both players ran out of pogs! It's a draw!"))
        // Scores remain tied as no one has more pogs for tie-breaker
        assertEquals(1, getPrivateField<Int>(activity, "playerMatchScore"))
        assertEquals(1, getPrivateField<Int>(activity, "aiMatchScore"))
        assertEquals(activity.getString(R.string.action_button_start_match), btnAction.text)
    }


    @Test
    fun testAITurn_ExecutesAndSwitchesBack() {
        setPrivateField(activity, "POGS_PER_ROUND_STACK", 1)
        callPrivateMethod(activity, "setupNewRound") // Stack has 2 pogs
        setPrivateField(activity, "isPlayerTurn", false) // Set to AI's turn

        val initialStackSize = getPrivateField<List<Pog>>(activity, "currentPogStack").size
        assertTrue(initialStackSize > 0)

        callPrivateMethod(activity, "aiTurn")
        Shadows.shadowOf(Looper.getMainLooper()).idle() // Execute pending Runnables on the main looper

        val stackSizeAfterAI = getPrivateField<List<Pog>>(activity, "currentPogStack").size
        val isPlayerTurnAfterAI = getPrivateField<Boolean>(activity, "isPlayerTurn")

        // AI should have taken a turn, potentially reducing stack. Then turn switches to player.
        assertTrue(stackSizeAfterAI < initialStackSize || stackSizeAfterAI == 0) // Stack reduced or emptied
        if (stackSizeAfterAI > 0) { // If round not over
            assertTrue(isPlayerTurnAfterAI) // Turn should be player's
            assertTrue(btnAction.isEnabled)
        } else { // Round ended
            // Check if round winner was declared, and button is for next round/match
            assertNotEquals(activity.getString(R.string.action_button_throw), btnAction.text)
        }
    }
}
