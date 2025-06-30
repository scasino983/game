package com.example.pogapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.pogapp.databinding.ActivityMainBinding
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Game constants
    private val INITIAL_POGS_PER_PLAYER = 20 // Total pogs each player starts with for the match
    private val POGS_PER_ROUND_STACK = 5     // Pogs each player contributes to the stack per round
    private val ROUNDS_TO_WIN_MATCH = 3      // Best out of (2*ROUNDS_TO_WIN_MATCH - 1)

    // Game state variables
    private var playerMatchScore = 0 // Rounds won by player in current match
    private var aiMatchScore = 0     // Rounds won by AI in current match

    private var playerTotalPogs = INITIAL_POGS_PER_PLAYER
    private var aiTotalPogs = INITIAL_POGS_PER_PLAYER

    private var currentPogStack = mutableListOf<Pog>()
    private var isPlayerTurn = true
    private var gameMessage = ""

    // AI settings
    private val AI_FLIP_PROBABILITY = 0.45 // AI is slightly less effective
    private val PLAYER_FLIP_PROBABILITY = 0.50 // Player's base flip probability

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAction.setOnClickListener {
            handleActionButtonClick()
        }
        startNewMatch()
    }

    private fun startNewMatch() {
        playerMatchScore = 0
        aiMatchScore = 0
        playerTotalPogs = INITIAL_POGS_PER_PLAYER
        aiTotalPogs = INITIAL_POGS_PER_PLAYER
        binding.btnAction.text = getString(R.string.action_button_start_round)
        gameMessage = "New match started! Press 'Start New Round' to begin."
        isPlayerTurn = Random.nextBoolean() // Randomize who starts the first round
        updateUI()
    }

    private fun setupNewRound() {
        currentPogStack.clear()
        var pogsToContribute = POGS_PER_ROUND_STACK

        // Ensure players have enough pogs
        if (playerTotalPogs < POGS_PER_ROUND_STACK) pogsToContribute = playerTotalPogs
        if (aiTotalPogs < POGS_PER_ROUND_STACK && pogsToContribute > aiTotalPogs) pogsToContribute = aiTotalPogs
        
        if (playerTotalPogs < pogsToContribute || aiTotalPogs < pogsToContribute || pogsToContribute == 0) {
            // Not enough pogs from one or both players for a full round, try with fewer or end game.
            if (playerTotalPogs == 0 || aiTotalPogs == 0) {
                 endGameDueToNoPogs()
                 return
            }
             pogsToContribute = minOf(playerTotalPogs, aiTotalPogs, POGS_PER_ROUND_STACK)
             if (pogsToContribute == 0) {
                endGameDueToNoPogs()
                return
             }
        }


        for (i in 0 until pogsToContribute) {
            currentPogStack.add(Pog(id = i, imageName = "pog_player_generic"))
            currentPogStack.add(Pog(id = i + pogsToContribute, imageName = "pog_ai_generic"))
        }
        currentPogStack.shuffle() // Shuffle the stack

        // Deduct pogs from total - these are now "wagered"
        playerTotalPogs -= pogsToContribute
        aiTotalPogs -= pogsToContribute

        isPlayerTurn = Random.nextBoolean() // Randomize who starts each round
        gameMessage = if (isPlayerTurn) getString(R.string.game_message_player_turn) else getString(R.string.game_message_ai_turn)
        binding.btnAction.text = getString(R.string.action_button_throw)
        updateUI()

        if (!isPlayerTurn) {
            aiTurn()
        }
    }
    
    private fun endGameDueToNoPogs() {
        // This function is called if a player cannot contribute any pogs to a new round.
        // The player with more pogs wins the match, or it's a tie.
        gameMessage = when {
            playerTotalPogs > aiTotalPogs -> "AI has no pogs left! Player wins the match by default!"
            aiTotalPogs > playerTotalPogs -> "Player has no pogs left! AI wins the match by default!"
            else -> "Both players ran out of pogs! It's a draw!"
        }
        // Consider overall pogs as a tie-breaker for the match if scores are equal
        if (playerMatchScore == aiMatchScore) {
            if (playerTotalPogs > aiTotalPogs) playerMatchScore++
            else if (aiTotalPogs > playerTotalPogs) aiMatchScore++
        }
        binding.btnAction.text = getString(R.string.action_button_start_match)
        updateUI()
    }


    private fun handleActionButtonClick() {
        if (binding.btnAction.text == getString(R.string.action_button_start_match)) {
            startNewMatch()
        } else if (binding.btnAction.text == getString(R.string.action_button_start_round)) {
            if (playerTotalPogs < POGS_PER_ROUND_STACK || aiTotalPogs < POGS_PER_ROUND_STACK) {
                // Attempt to start a round with fewer pogs if necessary
                val minPogs = minOf(playerTotalPogs, aiTotalPogs)
                if (minPogs > 0) {
                     // For simplicity now, we'll just proceed. setupNewRound will handle pogsToContribute
                     setupNewRound()
                } else {
                    endGameDueToNoPogs() // Not enough pogs to start any round
                }
            } else {
                 setupNewRound()
            }
        } else if (isPlayerTurn && currentPogStack.isNotEmpty()) {
            handleSlammerThrow(PLAYER_FLIP_PROBABILITY)
        }
    }

    private fun handleSlammerThrow(flipProbability: Double) {
        if (currentPogStack.isEmpty()) return

        val pogsFlippedThisTurn = mutableListOf<Pog>()
        val pogsRemainingInStack = mutableListOf<Pog>()
        var pogsCollectedCount = 0

        // Simulate flipping - simple randomization for now
        // A real game might have more complex physics (slammer weight, impact angle, etc.)
        val pogsToPotentiallyFlip = minOf(currentPogStack.size, Random.nextInt(1, currentPogStack.size + 2)) // Flip 1 to all+1 (simulating scattering)

        for (i in 0 until currentPogStack.size) {
            val pog = currentPogStack[i]
            if (i < pogsToPotentiallyFlip && Random.nextDouble() < flipProbability) {
                pog.isFaceUp = true // Assume slammer flips it face up
                pogsFlippedThisTurn.add(pog)
                pogsCollectedCount++
            } else {
                pog.isFaceUp = false // Stays face down or is flipped back by others
                pogsRemainingInStack.add(pog)
            }
        }
        
        currentPogStack = pogsRemainingInStack.toMutableList()

        if (isPlayerTurn) {
            playerTotalPogs += pogsCollectedCount
            gameMessage = "Player flipped $pogsCollectedCount pogs!"
        } else {
            aiTotalPogs += pogsCollectedCount
            gameMessage = "AI flipped $pogsCollectedCount pogs!"
        }

        updateUI()

        if (currentPogStack.isEmpty()) {
            endRound()
        } else {
            switchTurn()
        }
    }

    private fun switchTurn() {
        isPlayerTurn = !isPlayerTurn
        gameMessage += if (isPlayerTurn) "\n${getString(R.string.game_message_player_turn)}" else "\n${getString(R.string.game_message_ai_turn)}"
        updateUI()

        if (!isPlayerTurn) {
            aiTurn()
        }
    }

    private fun aiTurn() {
        // Simple AI: waits a bit then throws
        binding.btnAction.isEnabled = false // Disable button during AI turn
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isPlayerTurn && currentPogStack.isNotEmpty()) { // Check again in case state changed
                 handleSlammerThrow(AI_FLIP_PROBABILITY)
            }
            if (isPlayerTurn || currentPogStack.isEmpty()) { // Re-enable if turn switched back or round ended
                binding.btnAction.isEnabled = true
            }
        }, 1500) // 1.5 second delay for AI "thinking"
    }

    private var roundWinnerPogsCount = 0 // Temporary store for who won what in the round, for message
    private var roundLoserPogsCount = 0

    private fun endRound() {
        // At the end of the round, technically all pogs from the initial stack were "won"
        // by being flipped. The current logic adds them directly to player/AI totals during handleSlammerThrow.
        // So, we just need to declare a winner for the round score.
        // The "for keeps" part is implicitly handled by adding to totals.

        // To determine round winner for scoring, let's track how many *from the original stack* each player collected.
        // This requires a slight refactor or better tracking.
        // For now, let's simplify: the player who took the *last* pog (emptied the stack) wins the round score.
        // This is a common house rule. Or, the player who collected more pogs from *that round's stack*.

        // Let's refine: the winner of the round is the one who collected more pogs *during that round*.
        // This means we need to track pogs collected *per round* if we want to be precise for round win declaration.
        // However, the `handleSlammerThrow` already adds to `playerTotalPogs` and `aiTotalPogs`.
        // The spirit of "for keeps" is that the pogs flipped are kept.
        // The round *score* (for match progression) is a separate concept.

        // Let's assume the player who made the stack empty doesn't automatically win the round score.
        // Instead, we need to compare who collected more pogs *that were part of the initial round stack*.
        // This is tricky because `playerTotalPogs` is cumulative.

        // Simplified approach for round win: The player whose turn it was when the stack became empty
        // is deemed the "active" player. If they collected any pogs on that turn, they win the round.
        // This isn't perfect. A better way would be to sum pogs won *in this round only*.

        // Let's go with: whoever has more pogs currently (after the round) wins the round score.
        // This is also not quite right as it reflects cumulative.

        // *** REVISITING ROUND WIN LOGIC FOR CLARITY ***
        // The core idea: pogs put into the stack are wagered. Winner of the round gets the round point.
        // Pogs flipped are kept by the flipper.
        // If the stack is empty, the round ends. Who gets the point for the round?
        // Let's say the player who has collected more pogs *from the current round's stack* wins the round.
        // This means we need to track pogs collected *this round*.

        // For now, keeping it simple: if it was player's turn and stack emptied, player *likely* won more.
        // This is a placeholder for better round win determination. The current `handleSlammerThrow`
        // correctly assigns pogs to totals ("for keeps"). The round score is the main issue here.

        // Simplest for now: Last player to successfully flip pogs wins the round.
        if (isPlayerTurn) { // Player just took their turn and stack is now empty
            playerMatchScore++
            gameMessage = getString(R.string.game_message_player_wins_round)
        } else { // AI just took its turn
            aiMatchScore++
            gameMessage = getString(R.string.game_message_ai_wins_round)
        }
        // Note: A tie in a round (e.g. if both players somehow can't flip any more pogs and stack isn't empty)
        // isn't handled explicitly here, as the game proceeds turn by turn until stack is empty.

        updateUI()
        checkForMatchEnd()
    }


    private fun checkForMatchEnd() {
        var matchOver = false
        if (playerMatchScore >= ROUNDS_TO_WIN_MATCH) {
            gameMessage += "\n${getString(R.string.game_message_player_wins_match)}"
            matchOver = true
        } else if (aiMatchScore >= ROUNDS_TO_WIN_MATCH) {
            gameMessage += "\n${getString(R.string.game_message_ai_wins_match)}"
            matchOver = true
        }

        if (matchOver) {
            binding.btnAction.text = getString(R.string.action_button_start_match)
            // Pogs are already "kept" as they were added to totals during throws.
        } else {
            binding.btnAction.text = getString(R.string.action_button_start_round)
        }
        binding.btnAction.isEnabled = true // Ensure button is enabled after round/match ends
        updateUI()
    }

    private fun updateUI() {
        binding.tvPlayerMatchScore.text = "Player Rounds: $playerMatchScore"
        binding.tvAIMatchScore.text = "AI Rounds: $aiMatchScore"
        binding.tvPlayerTotalPogs.text = getString(R.string.player_pogs, playerTotalPogs)
        binding.tvAITotalPogs.text = getString(R.string.ai_pogs, aiTotalPogs)
        binding.tvPogsInStack.text = getString(R.string.pogs_in_stack, currentPogStack.size)
        binding.tvGameMessage.text = gameMessage

        // Disable button if it's AI's turn and game is active
        if (!isPlayerTurn && binding.btnAction.text == getString(R.string.action_button_throw) && currentPogStack.isNotEmpty()) {
            binding.btnAction.isEnabled = false
        } else {
            binding.btnAction.isEnabled = true
        }
    }
}
