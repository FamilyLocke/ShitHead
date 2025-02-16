import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat.recreate


class GameLogic(val players: List<Player>,
                val gameState: GameState,
                private val context: Context) {

    var updateUICallback: (() -> Unit)? = null // UI-Update Callback
    private var isSettingOpenCards = true

    fun checkNextPhase(player: Player) {
        Log.d("GameLogic", "Prüfe Spielphase für ${player.name}")
        Log.d("GameLogic", "Handkarten: ${player.hand.size}, Ziehstapel: ${gameState.drawPile.size}")

        if (player.hand.isEmpty() && gameState.drawPile.isEmpty()) {
            if (player.openCards.isNotEmpty()) {
                gameState.currentPhase = GamePhase.OPEN_CARDS
                Log.d("GameLogic", "${player.name} kann nun offene Karten spielen.")
            } else if (player.hiddenCards.isNotEmpty()) {
                gameState.currentPhase = GamePhase.CLOSED_CARDS
                Log.d("GameLogic", "${player.name} kann nun verdeckte Karten spielen.")
            } else {
                Log.d("GameLogic", "${player.name} hat keine Karten mehr. Spiel beendet?")
            }
        }
    }


    // Spieler wählt eine Karte für die offenen Karten aus
    fun selectOpenCard(player: Player, card: Card) {
        if (isSettingOpenCards && player.selectedOpenCards.size < 3 && player.hand.contains(card)) {
            player.selectedOpenCards.add(card)
            player.hand.remove(card)

            // Prüfe, ob alle Spieler fertig sind
            if (players.all { it.selectedOpenCards.size == 3 }) {
                finalizeOpenCards()
                isSettingOpenCards = false
                startGame() // Starte das eigentliche Spiel
            }
        }
    }


    // Überträgt die ausgewählten Karten in die offenen Karten
    private fun finalizeOpenCards() {
        players.forEach { player ->
            player.openCards.addAll(player.selectedOpenCards)
            player.selectedOpenCards.clear()
        }
    }

    // Startet das Spiel erst nach der Auswahl der offenen Karten
    fun startGame() {
        if (gameState.drawPile.isEmpty()) {
            shuffleAndDeal()
            gameState.currentPlayer = players[0]
        }
    }

    // Überprüft, ob ein Spielzug gültig ist
    fun isValidMove(card: Card): Boolean {
        val topCard = gameState.discardPile.lastOrNull()
        val secondLastCard = if (gameState.discardPile.size > 1) gameState.discardPile[gameState.discardPile.size - 2] else null

        if (topCard == null) {
            println("Kein Top-Card -> Zug ist gültig")
            return true
        }

        println("Top-Card: ${topCard.value}, Second-Last: ${secondLastCard?.value ?: "None"}")

        // Regel 1: Eine 2 kann immer gespielt werden
        if (card.value == 2) {
            println("Regel: 2 darf immer gespielt werden")
            return true
        }

        // Regel 2: Eine 3 kann immer gespielt werden
        if (card.value == 3) {
            println("Regel: 3 darf immer gespielt werden")
            return true
        }

        // Regel 3: Karte muss größer/gleich als die oberste Karte sein (außer die oberste Karte ist eine 7,3)
        if (card.value >= topCard.value && topCard.value != 7 && topCard.value !=3) {
            println("Regel: Karte ist größer als die oberste und diese ist keine 7,3 -> Zug ist gültig")
            return true
        }

        // Regel 4: Wenn die oberste Karte eine 3 ist, orientiere dich an der vorletzten Karte
        if (topCard.value == 3 ) {
            println("Regel: Oberste Karte ist eine 3 -> Vorletzte Karte wird geprüft")
            if (secondLastCard?.value == null || (card.value >= secondLastCard?.value!!)) {
                println("Regel: Karte ist größer/gleich der vorletzten Karte -> Zug ist gültig")
                return true
            }
        }

        // Regel 5: Falls die oberste Karte eine 7 ist, muss die neue Karte kleiner oder gleich 7 sein
        if (topCard.value == 7 && card.value <= 7) {
            println("Regel: Oberste Karte ist eine 7 -> Neue Karte muss ≤ 7 sein -> Zug ist gültig")
            return true
        }

        // Wenn keine Regel zutrifft, ist der Zug ungültig
        println("Kein gültiger Zug")
        return false
    }




    // Führt einen Spielzug aus
    @SuppressLint("SuspiciousIndentation")
    fun playCard(player: Player, card: Card) {
        if (isValidMove(card)) {
            gameState.discardPile.add(card)
            handleSpecialCard(card)
            player.hand.remove(card)

           if (gameState.discardPile.isNotEmpty() && player.hand.size <= 2 ){
               while (player.hand.size <= 2)
               drawCardFromStack(player)
           }
            nextPlayer()

        }
    }

    fun playOpenCard(player: Player, card: Card) {
        if (isValidMove(card)) {
            gameState.discardPile.add(card)
            handleSpecialCard(card)
            player.openCards.remove(card)
            nextPlayer()
        }
    }

    fun playClosedCard(player: Player, card: Card) {
        if (isValidMove(card)) {
            gameState.discardPile.add(card)
            handleSpecialCard(card)
            player.hiddenCards.remove(card)
            nextPlayer()
        }else{

        }
    }

    fun drawCardFromStack(player: Player) {
        // Prüfen, ob der Ziehstapel Karten enthält
        if (gameState.drawPile.isNotEmpty()) {
            // Ziehe die oberste Karte vom Ziehstapel
            val drawnCard = gameState.drawPile.removeAt(gameState.drawPile.size - 1)
            // Füge die gezogene Karte zur Hand des Spielers hinzu
            player.hand.add(drawnCard)
            sortHandCards(player)
        } else {
            // Ziehstapel ist leer, zeige eine Nachricht an
            Toast.makeText(context, "Der Ziehstapel ist leer!", Toast.LENGTH_SHORT).show()
        }
    }

    // Behandelt Spezialkarten (2, 3, 7, 8, 10)
    private fun handleSpecialCard(card: Card) {
        when (card.value) {
            8 -> handleEight() // 8: Der nächste Spieler muss aussetzen
            14 -> handleBurn() // 14: Der Ablagestapel wird entfernt
        }
    }




    private fun handleEight() {
        val currentIndex = players.indexOf(gameState.currentPlayer)
        val nextIndex = (currentIndex + 1) % players.size
        gameState.currentPlayer = players[nextIndex] // Überspringe den nächsten Spieler
    }

    // 10: Der Ablagestapel wird entfernt
    private fun handleBurn() {
        gameState.discardPile.clear() // Leere den Ablagestapel
        gameState.currentPlayer = gameState.currentPlayer // Der Spieler ist nochmal dran
    }


    // Mischt und verteilt die Karten
    private fun shuffleAndDeal() {
        val deck = createDeck()
        deck.shuffle()
        players.forEach { player ->
            if (player.isBot == true){
                repeat(3) { player.hand.add(deck.removeAt(0)) }
                repeat(3) { player.hiddenCards.add(deck.removeAt(0)) }
                repeat(3) { player.openCards.add(deck.removeAt(0)) }

            }else {
                repeat(6) { player.hand.add(deck.removeAt(0)) }
                repeat(3) { player.hiddenCards.add(deck.removeAt(0)) }
            }
        }
        gameState.drawPile.addAll(deck)
    }

    // Erstellt ein Standard-Pokerset
    private fun createDeck(): MutableList<Card> {
        val suits = listOf(1, 2, 3, 4) // 1 = Herz, 2 = Karo, 3 = Kreuz, 4 = Pik
        val values = listOf(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)
        val deck = mutableListOf<Card>()

        for (suit in suits) {
            for (value in values) {
                val resourceName = "the${suit}of${value}" // z. B. "the1of14"
                val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)

                if (resourceId == 0) {
                    Log.e("createDeck", "Ressource nicht gefunden: $resourceName")
                }

                val card = Card(suit, value, resourceId)
                deck.add(card)
            }
        }
        return deck
    }

    fun getPlayersState(): String {
        val builder = StringBuilder()
        players.forEach { player ->
            builder.append("Spieler: ${player.name}\n")

            // Handkarten mit Details
            builder.append("  Handkarten (${player.hand.size}):\n")
            player.hand.forEachIndexed { index, card ->
                val cardName = "${card.suit} ${valueToString(card.value)}"
                builder.append("    ${index + 1}. $cardName\n")
            }

            // Offene Karten mit Details
            builder.append("  Offene Karten (${player.openCards.size}):\n")
            player.openCards.forEachIndexed { index, card ->
                val cardName = "${card.suit} ${valueToString(card.value)}"
                builder.append("    ${index + 1}. $cardName\n")
            }

            // Verdeckte Karten (nur Anzahl)
            builder.append("  Verdeckte Karten: ${player.hiddenCards.size}\n\n")

            builder.append("  Derzeitiger Spieler: ${gameState.currentPlayer}\n\n")

        }
        return builder.toString()
    }

    // Hilfsfunktion: Konvertiert Zahlenwerte in lesbare Kartenwerte (z. B. 11 -> "Jack")
    private fun valueToString(value: Int): String {
        return when (value) {
            11 -> "Queen"
            12 -> "King"
            13 -> "Ace"
            14 -> "BURN"
            else -> value.toString()
        }
    }

    fun playBotTurn(bot: Player) {

        do {
            Log.d("GameLogic", "Bot ${bot.name} ist am Zug...")
            checkNextPhase(bot)

            var hasPlayed = false // Flag, ob der Bot eine Karte gespielt hat

            // 1️⃣ HAND-KARTEN SPIELEN
            if (gameState.currentPhase == GamePhase.HAND_CARDS) {
                val playableCards = bot.hand.filter { isValidMove(it) }.sortedBy { it.value }

                if (playableCards.isNotEmpty()) {
                    val cardToPlay = playableCards.first()
                    playCard(bot, cardToPlay)
                    Log.d("GameLogic", "${bot.name} spielt ${cardToPlay.value} aus der Hand")
                    hasPlayed = true
                }
            }

            // 2️⃣ OFFENE KARTEN SPIELEN
            if (!hasPlayed && gameState.currentPhase == GamePhase.OPEN_CARDS) {
                val openPlayableCards = bot.openCards.filter { isValidMove(it) }.sortedBy { it.value }

                if (openPlayableCards.isNotEmpty()) {
                    val cardToPlay = openPlayableCards.first()
                    playOpenCard(bot, cardToPlay)
                    Log.d("GameLogic", "${bot.name} spielt offene Karte ${cardToPlay.value}")
                    hasPlayed = true
                }
            }

            // 3️⃣ VERDECKTE KARTEN SPIELEN (zufällige Karte)
            if (!hasPlayed && gameState.currentPhase == GamePhase.CLOSED_CARDS) {
                if (bot.hiddenCards.isNotEmpty()) {
                    val hiddenCard = bot.hiddenCards.random()
                    playClosedCard(bot, hiddenCard)
                    Log.d("GameLogic", "${bot.name} spielt eine zufällige verdeckte Karte")
                    hasPlayed = true
                }
            }

            // 4️⃣ FALLS DER BOT NICHT SPIELEN KANN → ABLAGESTAPEL AUFNEHMEN
            if (!canPlay(bot)) {
                Log.d("GameLogic", "${bot.name} kann nicht legen und nimmt den gesamten Ablagestapel auf!")
                bot.hand.addAll(gameState.discardPile)
                gameState.discardPile.clear()
                gameState.currentPhase = GamePhase.HAND_CARDS // Wechsel zurück zu Handkarten
                updateUICallback?.invoke()
                hasPlayed = true
                return // Bot beendet seinen Zug hier, `nextPlayer()` wird nicht aufgerufen!
            }

        }while (!hasPlayed)
        updateUICallback?.invoke()

    }


    fun sortHandCards(player: Player) {
        player.hand.sortBy { it.value } // Sortiere die Karten aufsteigend nach Wert
        Log.d("GameLogic", "${player.name}'s Handkarten wurden sortiert: ${player.hand.map { it.value }}")
    }


    fun nextPlayer() {
        val currentIndex = players.indexOf(gameState.currentPlayer)
        if (currentIndex == -1) {
            Log.e("GameLogic", "Fehler: Kein aktueller Spieler gesetzt!")
            return
        }
        Log.d("GameLogic", "Derzeitiger Spieler: ${gameState.currentPlayer?.name}")

        val nextIndex = (currentIndex + 1) % players.size
        gameState.currentPlayer = players[nextIndex]

        Log.d("GameLogic", "Nächster Spieler: ${gameState.currentPlayer?.name}")

        // Falls der neue Spieler ein Bot ist, spiele sofort seinen Zug
        val currentPlayer = gameState.currentPlayer
        if (currentPlayer != null && currentPlayer.isBot) {
            Handler(Looper.getMainLooper()).postDelayed({
                playBotTurn(currentPlayer)
            }, 1000) // 1 Sekunde Verzögerung für realistischeren Ablauf
        }
    }

    fun canPlay(player: Player): Boolean {
        val topCard = gameState.discardPile.lastOrNull() ?: return true // Falls kein Ablagestapel existiert, kann er immer legen

        // Prüfe, ob der Spieler eine Karte hat, die auf die oberste Karte passt
        return player.hand.any { card -> isValidMove(card) }
    }

}